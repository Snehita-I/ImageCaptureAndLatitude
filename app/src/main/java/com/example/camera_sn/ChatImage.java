package com.example.camera_sn;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ChatImage extends AppCompatActivity {

    private StorageReference mStorageRef;
    private ImageView sendImageChatbtn;
    private String mImageUri;
    private Uri mainImageUri;
    private ImageView image;
    private ImageButton backButton;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private int STORAGE_PERMISSION_CODE=10;
    private GpsTracker gpsTracker;
    double latitude;
    double longitude;
    private String TAG = ChatImage.class.getSimpleName();

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_image);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mStorageRef = FirebaseStorage.getInstance().getReference("images");

        backButton = findViewById(R.id.backbutton);
        sendImageChatbtn = findViewById(R.id.sendMessageButton);
        image = findViewById(R.id.chosenImage);
        db = FirebaseFirestore.getInstance();

        //mAuth = FirebaseAuth.getInstance();
        //user = mAuth.getCurrentUser();

        Bundle extras = getIntent().getExtras();
        mImageUri = extras.getString("mImageUri");

        try {
            Picasso.get().load(String.valueOf(new URL(mImageUri))).into(image);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //image.setImageURI(tempMainImageUri);

        sendImageChatbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                //File file = new File(getBatchDirectoryName(), mDateFormat.format(new Date())+ ".jpg");
                //File file = new File("data/data//test.jpg");

                //file creation
                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/images");
                myDir.mkdirs();
                Random generator = new Random();
                int n = 10000;
                n = generator.nextInt(n);
                String fname = "Image-"+ n +".jpg";
                File file = new File (myDir, fname);
                if (file.exists ())
                    file.delete ();
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                final StorageReference imageRef = mStorageRef.child(System.currentTimeMillis() + "." + getFileExtension(Uri.parse(mImageUri)));
                UploadTask uploadTask = imageRef.putFile(Uri.parse(mImageUri));
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    Task<Uri> downloadUrl = imageRef.getDownloadUrl();
                    downloadUrl.addOnSuccessListener(uri -> {
                        Date d = new Date();
                        long timestamp = d.getTime();
                        Map<String, Object> docData = new HashMap<>();

                        docData.put("latitude", latitude);
                        docData.put("longitude", longitude);

                        db.collection("iku_earth_messages")
                                .add(docData)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {

                                        Toast.makeText(ChatImage.this, "Image info uploaded", Toast.LENGTH_LONG).show();

                                    }
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(ChatImage.this, e.toString(), Toast.LENGTH_LONG).show();
                                });
                    });
                });
            }
        });


    }


    private void uploadFile() throws FileNotFoundException {
        if (mImageUri != null) {
            Bitmap imageSelected = decodeUri(this, Uri.parse(mImageUri), 300);
            if (imageSelected != null)
                mainImageUri = getImageUri(this, imageSelected);
            if (mainImageUri != null) {
                final StorageReference imageRef = mStorageRef.child(System.currentTimeMillis() + "." + getFileExtension(mainImageUri));
                UploadTask uploadTask = imageRef.putFile(mainImageUri);
                //SaveImage(imageSelected);
            }

        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_LONG).show();
        }
    }
    private String getFileExtension(Uri uri) {
        if (uri != null) {
            ContentResolver cR = getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            return mime.getExtensionFromMimeType(cR.getType(uri));
        } else return null;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "images", null);

        if (path != null) return Uri.parse(path);
        else return null;
    }
    public static Bitmap decodeUri(Context c, Uri uri, final int requiredSize) throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o);

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;

        while (true) {
            if (width_tmp / 2 < requiredSize || height_tmp / 2 < requiredSize)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o2);
    }
    public void getLocation(View view){
        gpsTracker = new GpsTracker(ChatImage.this);
        if(gpsTracker.canGetLocation()){
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();

        }else{
            gpsTracker.showSettingsAlert();
        }
    }





}