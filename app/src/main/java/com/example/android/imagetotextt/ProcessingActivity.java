package com.example.android.imagetotextt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaCodec;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.bitmap;

public class ProcessingActivity extends AppCompatActivity {
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

    protected ImageView _image;
    protected TextView _text;
    protected TessBaseAPI mTess;
    protected String datapath = "";
    private static final String TAG="ProcessingActivity";
    private Mat rr=null,source=null,element=null,destination=null;
    Bitmap bitmap11,bit,b,bb;
    Pattern pattern1=Pattern.compile("[A-Z]{1}[a-z]+\\s[A-Z]{1}[a-z]+"), pattern2= Pattern.compile("[0-9]{4}\\s{1}[0-9]{4}\\s{1}[0-9]{4}"),pattern3=Pattern.compile("[A-Z]{1}[a-z]+\\s[A-Z]{1}[a-z]+\\s[A-Z]{1}[a-z]+"),pattern4=Pattern.compile("[A-Z]{1}[a-z]+"),pattern5=Pattern.compile("[0-9]{2}[/][0-9]{2}[/][0-9]{4}"),pattern6=Pattern.compile("[0-9]{4}");
    Matcher matcher1,matcher2,matcher3,matcher4,matcher5,matcher6,matcher7;
    Pix and;
    String Info="";
    int erosion_size=2,flag=1,note=0,mm,count=1,count1=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Bundle bundle = getIntent().getExtras();
        String newString = bundle.getString("FILEPATH");

//
        bitmap11 = BitmapFactory.decodeFile(newString);


        _image = (ImageView) findViewById(R.id.screenshot);

        _text = (TextView) findViewById(R.id.ocrtext);
        _text.setMovementMethod(new ScrollingMovementMethod());

        //IMAGE PROCESSING OPENCV
        bit = imageProcessing(newString);


        bb=Bitmap.createScaledBitmap(bit,785,827,false);
        _image.setImageBitmap(bb);
        // _image.setImageBitmap(bitmap11);

        datapath = getFilesDir() + "/tesseract/";


        //make sure training data has been copied
        checkFile(new File(datapath + "tessdata/"));
        //initialize Tesseract API
        String lang = "eng";
        mTess = new TessBaseAPI();
        mTess.init(datapath, lang);
        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM/,:-0123456789");
        String OCRresult = "",ocrcopy="";
        mTess.setImage(bb);
        OCRresult = mTess.getUTF8Text();
        ocrcopy=OCRresult;
        _text.setText(OCRresult);


        if (Pattern.matches(".*[0-9]{4}\\s{1}[0-9]{4}\\s{1}[0-9]{4}.*", OCRresult.replace("\n", ""))) {

            matcher3 = pattern3.matcher(OCRresult);
            while (matcher3.find()) {
                Info = Info + "Name:" + matcher3.group() + "\n";
                flag = 0;
            }

            if (flag == 1) {
                matcher1 = pattern1.matcher(OCRresult);
                while (matcher1.find()) {
                    Info = Info + "Name:" + matcher1.group() + "\n";
                }
            }
            matcher2 = pattern2.matcher(OCRresult);
            while (matcher2.find()) {
                Info = Info + "Adhar card no.:" + matcher2.group() + "\n";
            }
            if (OCRresult.toLowerCase().contains("female")) {
                note=1;
                Info = Info + "Gender:Female\n";
            } else if (OCRresult.toLowerCase().contains("male")) {
                Info = Info + "Gender:Male\n";
            }

            if (OCRresult.toLowerCase().contains("year")) {
                int p = OCRresult.toLowerCase().indexOf("year");
                if(note==1){
                     mm=OCRresult.toLowerCase().indexOf("female");
                }
                else{
                    mm=OCRresult.toLowerCase().indexOf("male");
                }


                String ss=OCRresult.substring(p,mm);
                matcher7=pattern6.matcher(ss);

                while(matcher7.find()&&count<2){
                    count+=1;
                    Info=Info+"Year Of birth:"+matcher7.group()+"\n";

                }

            }else {
                matcher6=pattern5.matcher(OCRresult);
                while (matcher6.find()){
                    Info=Info+"Date of Birth:"+matcher6.group()+"\n";
                }
            }


        } else {


            if (OCRresult.toLowerCase().contains("fath")) {
                int q = OCRresult.toLowerCase().indexOf("name");
                int r = OCRresult.toLowerCase().indexOf("fath");
                int s = OCRresult.toLowerCase().indexOf("sex");
                String ss = OCRresult.substring(q, r);
                String st=OCRresult.substring(r+3,s);
                matcher5=pattern4.matcher(st);
                matcher4=pattern1.matcher(ss);
                while(matcher4.find()){
                    Info=Info+"Name:"+matcher4.group()+"\n";
                }
                while(matcher5.find()&&count1<2){
                    if(!matcher5.group().toLowerCase().equals("name")) {
                        count1+=1;
                        Info = Info + "Father's Name:" + matcher5.group() + "\n";
                    }
                }
            }
            else if(OCRresult.toLowerCase().contains("husba")){
                int q = OCRresult.toLowerCase().indexOf("name");
                int r = OCRresult.toLowerCase().indexOf("husba");
                int s=OCRresult.toLowerCase().indexOf("sex");
                String ss=OCRresult.substring(q,r);
                String st=OCRresult.substring(r+4,s);
                matcher5=pattern4.matcher(st);
                matcher4=pattern1.matcher(ss);
                while(matcher4.find()){
                    Info=Info+"Name:"+matcher4.group()+"\n";
                }
                while(matcher5.find()&&count1<2){
                    if(!matcher5.group().toLowerCase().equals("name")) {
                        count1+=1;
                        Info = Info + "Husband's Name:" + matcher5.group() + "\n";
                    }
                }
            }
            if (OCRresult.toLowerCase().contains("female")) {
                Info = Info + "Gender:Female\n";
                //OCRresult = OCRresult.replace("ale","");
            } else if (OCRresult.toLowerCase().contains("male")) {
                Info = Info + "Gender:Male\n";
                //OCRresult = OCRresult.replace("Female","");
            }

            matcher6=pattern5.matcher(OCRresult);
            while (matcher6.find()){
                Info=Info+"Date of Birth:"+matcher6.group()+"\n";
            }

            int gj=OCRresult.indexOf("GJ"),gg;
            String eid="GJ/",op="";
            int count=0,coi=0;
            gg=gj+1;
            op=Character.toString(OCRresult.charAt(gg));
             Log.d(TAG,op);

            while(!op.equals("/")&&!op.equals("1")&&!op.equals("l")&&!op.equals("I")){
                    gg+=1;
                    op=Character.toString(OCRresult.charAt(gg));
                    Log.d(TAG,op);
            }
            int cc=gg-1;
            while (count < 2) {
                if(Character.isDigit(OCRresult.charAt(cc))) {
                    eid = eid + OCRresult.charAt(cc);
                    count+=1;
                }
                cc+=1;
            }
            eid=eid+"/";
            count=0;
            op=Character.toString(OCRresult.charAt(cc));
            while(!op.equals("/")&&!op.equals("1")&&!op.equals("l")&&!op.equals("I")){
                cc+=1;
                op=Character.toString(OCRresult.charAt(cc));
                Log.d(TAG,op);
            }
            int cd=cc+1;
            while(count<=2){
                if(Character.isDigit(OCRresult.charAt(cd))){
                    eid=eid+OCRresult.charAt(cd);
                    count+=1;
                }
                cd+=1;
            }
            eid=eid+"/";
            count=0;
            op=Character.toString(OCRresult.charAt(cd));
            while(!op.equals("/")&&!op.equals("1")&&!op.equals("l")&&!op.equals("I")){
                cd+=1;
                op=Character.toString(OCRresult.charAt(cd));
                Log.d(TAG,op);
            }
            int ce=cd+1;
            while(count<=5){
                if(Character.isDigit(OCRresult.charAt(ce))){
                    eid=eid+OCRresult.charAt(ce);
                    count+=1;
                }
                ce+=1;
            }
            Info=Info+"Eelection ID no."+eid;


        }

       String f="PROCESSED TEXT\n\n"+"-----------------------------------------\n\n"+Info+"\n\n"+"--------------------------------------\n\n"+"\nRAWTEXT:::::::::::::::::::::::::::::::::\n\n"+ocrcopy;

       _text.setText(f);

    }


    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mOpenCVCallBack);
    }


    protected Bitmap imageProcessing(String path){
        source= Imgcodecs.imread(path);
        destination=new Mat(source.rows(),source.cols(),source.type());
        Imgproc.cvtColor(source,destination, Imgproc.COLOR_BGR2GRAY);
        destination.convertTo(destination,-1,1,50);
        b=Bitmap.createBitmap(destination.cols(),destination.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination,b);
        return b;

    }


    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

