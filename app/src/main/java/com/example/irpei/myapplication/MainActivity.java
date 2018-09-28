package com.example.irpei.myapplication;


import android.app.Activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    List<GridViewItem> gridItems;
    List<String> toLoad;
    MyGridAdapter adp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button but = (Button)findViewById(R.id.button);


        setContentView(R.layout.activity_main);
        toLoad = new ArrayList<>();
        setGridAdapter("/storage/self/primary/DCIM/Camera");
    }
    public void buttonClick(View view)
    {

        for(String s: toLoad)
        {
            Bitmap image = BitmapHelper.decodeBitmapFromFile(s,
                    50,
                    50);
            File f = new File(s);
            FileOutputStream out = null;
            try{

                out = new FileOutputStream("/storage/self/primary/DCIM/Test" + f.getName());
                image.compress(Bitmap.CompressFormat.PNG, 100, out);
                MediaScannerConnection.scanFile(this, new String[]{"/storage/self/primary/DCIM/Test" + f.getName()}, null, null);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            finally {
                if(out != null)
                {
                    try {
                        out.close();
                        for(int i = 0; i < gridItems.size(); i++)
                        {
                            RelativeLayout v = (RelativeLayout)(adp.getView(i, null, null));
                            ViewGroup a = (ViewGroup)v;
                            ImageView img = (ImageView)(a.getChildAt(0));
                            img.setColorFilter(null);
                        }
                        System.out.println("Done");
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

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
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(adp);

        // Set the onClickListener
        gridView.setOnItemClickListener(this);
    }


    /**
     * Go through the specified directory, and create items to display in our
     * GridView
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
            if(g.isSelected())
            {
                g.setSelected(false);
                ViewGroup a = (ViewGroup)view;
                ImageView i = (ImageView)(a.getChildAt(0));
                i.setColorFilter(null);
                if(toLoad.contains(g.getPath()))
                {
                    toLoad.remove(g.getPath());
                }
            }
            else
            {
                g.setSelected(true);
                toLoad.add(g.getPath());
                System.out.println(g.getPath());
                ViewGroup a = (ViewGroup)view;
                ImageView i = (ImageView)(a.getChildAt(0));
                i.setColorFilter(Color.argb(150,200,200,200));


            }
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
}