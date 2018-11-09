package com.example.irpei.myapplication;


import android.Manifest;
import android.app.Activity;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private List<GridViewItem> gridItems;
    private List<GridViewItem> toLoad;
    private MyGridAdapter adp;
    private GridView gridView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(Environment.getExternalStorageDirectory().toString());
        setContentView(R.layout.activity_main);
        toLoad = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

        }
        else
        {
            setGridAdapter(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
        }

    }
    @Override
    protected void onStart() {
        super.onStart();

        Button imp = (Button)findViewById(R.id.button);
        imp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), 1);
            }
        });

        Button delete = (Button)findViewById(R.id.delete);
        delete.setOnClickListener((view) -> {
                String f = toLoad.get(0).getPath();
                /*
                    This is the part that does the deleting and updating
                 */
                adp.remove(toLoad.get(0)); //Removes item from adp's data
                new File(f).delete();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(f)))); //Tells device about delete
                adp.notifyDataSetChanged(); // This line updates the GridView
                /*
                    End of area that does deleting and updating
                 */
                toLoad = new ArrayList<>();
                for(int i = 0; i < gridItems.size(); i++)
                {
                    GridViewItem g = gridItems.get(i);
                    if(g.isSelected()) {
                        g.setSelected(!g.isSelected());
                        adp.getView(i, null, null);
                    }
                }
        });
    }
    /**
     * This will create our GridViewItems and set the adapter
     *
     * @param path
     *            The directory in which to search for images
     */
    private void setGridAdapter(String path) {
        // Create a new grid adapter
        gridItems = createGridItems(path);
         adp = new MyGridAdapter(this, gridItems);

        // Set the grid adapter
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(adp);

        // Set the onClickListener
        gridView.setOnItemClickListener(this);
    }


    /**
        @param directoryPath - directory we would like to make gridView using
     */
    private List<GridViewItem> createGridItems(String directoryPath) {
        List<GridViewItem> items = new ArrayList<GridViewItem>();

        // List all the items within the folder.
        File[] files = new File(directoryPath).listFiles();

        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            // Add the directories containing images or sub-directories
            if (file.isDirectory()
                    && file.listFiles(new ImageFileFilter()).length > 0) {
                items.add(new GridViewItem(file.getAbsolutePath(), true, null, false));
            }
            // Add the images
            else {
                Bitmap image = BitmapHelper.decodeBitmapFromFile(file.getAbsolutePath(),
                        50,
                        50);

                items.add(new GridViewItem(file.getAbsolutePath(), false, image, false));
            }
        }

        return items;
    }


    /**
     * Checks the file to see if it has a compatible extension.
     */
    private boolean isImageFile(String filePath) {
        if (filePath.endsWith(".jpg") || filePath.endsWith(".png"))
        // Add other formats as desired
        {
            return true;
        }
        return false;
    }


    @Override
    public void
    onItemClick(AdapterView<?> parent, View view, int position, long id) {


        if (gridItems.get(position).isDirectory()) {
            setGridAdapter(gridItems.get(position).getPath());
        }
        else {
            GridViewItem g = gridItems.get(position);
            g.setSelected(!g.isSelected());
            adp.getView(position, view, null);
            toLoad.add(g);
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1 && resultCode == Activity.RESULT_OK)
        {
            if(data.getClipData() != null) // multiple pictures
            {
                for(int i = 0; i < data.getClipData().getItemCount(); i++)
                {
                    Uri u = data.getClipData().getItemAt(i).getUri();
                    String path = getPath(u);
                    File f = new File(path);
                    copy(f, new File(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/" + f.getName()));
                    GridViewItem g = new GridViewItem(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/"+ f.getName(), false, BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/" + f.getName()), false);
                    gridItems.add(g);
                    adp.add(g);
                }
            }
            //else if(data.getData() != null) // single picture
        }

    }

    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }

    public void copy(File src, File dest)
    {
        Bitmap image = BitmapFactory.decodeFile(src.getAbsolutePath());

        try {
            FileOutputStream out = new FileOutputStream(dest.getAbsolutePath());
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            String path = dest.getAbsolutePath();
            MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * This can be used to filter files.
     */
    private class ImageFileFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            else if (isImageFile(file.getAbsolutePath())) {
                return true;
            }
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setGridAdapter("/storage/self/primary/DCIM/Camera");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}