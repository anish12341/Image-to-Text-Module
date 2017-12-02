package com.example.android.imagetotextt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.android.imagetotextt.R;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import android.app.AlertDialog.Builder;

import static android.R.attr.data;
import static android.R.id.text1;
import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.example.android.imagetotextt.R.id.ocr;
import static com.example.android.imagetotextt.R.id.parent;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="MainActivity";
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    protected Button _button;
    protected ImageView _image;
    protected TextView _field;
    protected TextView _ocr,_crop;
    protected boolean _taken;
    private TessBaseAPI mTess;
    String datapath="";
    //Bitmap bitmap;
    Bitmap bitmap1,cropped;
    String filepath="",picturePath="",croppath="",p;
    Uri selectedImage;
    File file=null,filecrop=null;
    ExifInterface ei;
    Uri photoURI,photoURICrop;
    Context con=this;
    static final int REQUEST_TAKE_PHOTO = 1,REQUEST_CHOOSE_PHOTO=2,REQUEST_CROP=3;

    protected static final String PHOTO_TAKEN = "photo_taken";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         _ocr=(TextView)findViewById(ocr);
        _crop=(TextView)findViewById(R.id.crop);
        _image = ( ImageView ) findViewById( R.id.image );
        _button = ( Button ) findViewById( R.id.button );
        _button.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v){
                selectImage();
            }
        } );
        Log.d(TAG,Environment.getExternalStorageDirectory()+"/");
    }


    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mOpenCVCallBack);
    }


    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "png_" + timeStamp + "_";
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/DCIM/Camera/");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath= image.getAbsolutePath();
        return image;
    }




    protected void startCameraActivity()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            try {

                file = createImageFile();
            } catch (IOException ea) {
                Log.d(TAG, "Something went wrong");
            }


            if (file != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i( "MakeMachine", "resultCode: " + resultCode );
        if(resultCode==RESULT_OK){
            if(requestCode==1){
               onPhotoTaken();
            }
            else if(requestCode==2){
                selectedImage = data.getData();
               onPhotoChosen();
            }
            else{
                Bundle extra=data.getExtras();
                cropped=extra.getParcelable("data");
                onCrop();
            }
        }
    }

    protected void onPhotoTaken()
    {
        _taken = true;
        filepath=file.getAbsolutePath();
        photoURICrop = FileProvider.getUriForFile(this,
                "com.example.android.fileprovider",
                file);

        ContentValues values=new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, Settings.System.TIME_12_24);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(MediaStore.MediaColumns.DATA,filepath);

        con.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        _crop.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
/*
                con.grantUriPermission("com.android.camera",photoURICrop,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent cropIntent = new Intent(Intent.ACTION_EDIT);
                cropIntent.setDataAndType(photoURI,"image/*");

                cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                cropIntent.putExtra("crop",true);

                cropIntent.putExtra("aspectX", 1);
                cropIntent.putExtra("aspectY", 1);

                cropIntent.putExtra("outputX", 128);
                cropIntent.putExtra("outputY", 128);

                cropIntent.putExtra("return-data", true);

                cropIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURICrop);

                startActivityForResult(cropIntent, REQUEST_CROP);


               Intent editIntent = new Intent(Intent.ACTION_EDIT);
                editIntent.setDataAndType(photoURICrop, "image/*");
                editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                editIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(Intent.createChooser(editIntent, null),3);
*/
            }
        });



        /*BitmapFactory.Options option2=new BitmapFactory.Options();
        option2.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bitmap12=BitmapFactory.decodeFile(filepath,option2);
        try {
            ei= new ExifInterface(filepath);
        }catch(IOException e){}
        int orientation= ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);

        switch(orientation){

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotateImage(bitmap12,90,filepath);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotateImage(bitmap12,180,filepath);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotateImage(bitmap12,270,filepath);
                break;

            case ExifInterface.ORIENTATION_NORMAL:

                default:
                    break;
        }
        */
        setimg(filepath);

    }

   /* public static void rotateImage(Bitmap source, float angle,String pp) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap bitmap13=Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
        File file2=new File(pp);
        boolean deleted=file2.delete();
        String r=file2.getAbsolutePath();
        Log.d(TAG,r);


    }
    */

    protected void onPhotoChosen(){

        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(selectedImage,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        picturePath = cursor.getString(columnIndex);


        setimg(picturePath);

        cursor.close();

    }

    protected void onCrop(){
        try {
            filecrop = createImageFile();
        }catch(IOException e){}
        FileOutputStream out=null;
        try{
            out=new FileOutputStream(filecrop);
            cropped.compress(Bitmap.CompressFormat.PNG,100,out);
        }catch(FileNotFoundException f){}
        croppath=filecrop.getAbsolutePath();
        setimg(croppath);

    }


   protected void setimg(String picpath){

       p=picpath;
       BitmapFactory.Options option1=new BitmapFactory.Options();
       option1.inPreferredConfig=Bitmap.Config.ARGB_8888;
       bitmap1=BitmapFactory.decodeFile(picpath,option1);
       option1.inSampleSize=4;
       _image.setImageBitmap(bitmap1);
       _ocr.setOnClickListener(null);
       _ocr.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){
               Intent processingIntent = new Intent(MainActivity.this,ProcessingActivity.class);
               processingIntent.putExtra("FILEPATH",p);
               startActivity(processingIntent);
           }
       });
   }


    private void selectImage() {

        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo"))
                {
                   startCameraActivity();
                }
                else if (options[item].equals("Choose from Gallery"))
                {

                    try {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
                        }


                }catch (Exception e){
                e.printStackTrace();}
                }

                else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 2:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, REQUEST_CHOOSE_PHOTO);
                }
                else
                {
                   Log.d(TAG,"CANT USE GALLERY");
                }

                break;
        }
    }

}