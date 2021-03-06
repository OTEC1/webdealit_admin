package com.otec.webdealit.UI;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.fragment.app.Fragment;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.otec.webdealit.R;
import com.otec.webdealit.Retrofit_.Base_config;
import com.otec.webdealit.Retrofit_.Calls;
import com.otec.webdealit.Utils.Find;
import com.otec.webdealit.model.Auth;
import com.otec.webdealit.model.SendMovies;
import com.otec.webdealit.model.listOfmoviecategories;

import java.io.File;
import java.net.URISyntaxException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static com.otec.webdealit.Utils.Constants.PICK_IMAGE;
import static com.otec.webdealit.Utils.utils.message;


public class Video extends Fragment {


    ProgressBar spinnerProgressBar,mainProgressbar;
    VideoView videoPlayer;
    EditText videoTitle,yearReleased;
    ImageView thumbnail;
    Button chooseVideo,chooseThumbnail,upload,play,pause;
    Spinner spinner;
    ArrayAdapter adapter;
    Uri imgUri;
    TextView progressCount;
    AlertDialog alertDialog;
    View views;


    String TAG = "Video",categories="";
    Map<String, Object> containers;
    int spin_direction,thumbnail_orientation;
    boolean started_payload = false, image_or_video = false;


    @Override
    public void onResume() {
        super.onResume();
        CHECK();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  =  inflater.inflate(R.layout.fragment_video, container, false);
        API_MOVIES_LIST_CALL();
        Mapping(view);
        DIALOG();
        containers = new HashMap<>();



        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categories = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                categories="Select Categories";
            }
        });

        chooseVideo.setOnClickListener(r->{
            file_picker(2);
        });


        chooseThumbnail.setOnClickListener(w->{
            thumbnail_orientation = 0;
            spin_direction = 0;
            file_picker(1);
        });




        // thumbnail.animate().rotation(m).start();
        upload.setOnClickListener(r->{

               alertDialog.show();
               EditText edit = alertDialog.findViewById(R.id.writeUp);
               Button button = alertDialog.findViewById(R.id.upload);
               button.setOnClickListener(u->{
                   if(!started_payload) {
                       if (videoTitle.getText().toString().trim().isEmpty() || yearReleased.getText().toString().trim().isEmpty()
                               || categories.equals("Select Categories") || containers.size() <=1 || edit.getText().toString().trim().isEmpty())
                           message("Pls fill out all fields and add media files", getContext());
                       else {
                           if (imgUri != null) {
                               started_payload = true;
                               credentials(edit.getText().toString());
                               alertDialog.hide();
                           }else
                               message("Pls select a media file", getActivity());
                       }
                   }else
                       message("Pls wait upload in progress", getContext());
               });

        });


        videoPlayer.setOnClickListener(e->{

            if(videoPlayer.isPlaying()) {
                pause.setVisibility(View.VISIBLE);
                play.setVisibility(View.INVISIBLE);
            }else
                play.setVisibility(View.VISIBLE);


        });


        pause.setOnClickListener(r->{
            videoPlayer.pause();
            pause.setVisibility(View.INVISIBLE);
            play.setVisibility(View.VISIBLE);
        });


        play.setOnClickListener(u->{
            videoPlayer.start();
                play.setVisibility(View.INVISIBLE);
        });



        return view;
    }





    //Step 5
    private void credentials(String writeUp) {
        Calls call = Base_config.getConnection().create(Calls.class);
        Call<Auth> obj = call.getAuth();
        obj.enqueue(new Callback<Auth>() {
            @Override
            public void onResponse(Call<Auth> call, Response<Auth> response) {
                Map<String,Object> task = response.body().getList2();
                try {
                    send_data_to_s3(writeUp,task.get("p1").toString(), task.get("p2").toString(), task.get("p3").toString());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Auth> call, Throwable t) {
                message(t.getLocalizedMessage(), getContext());
            }
        });

    }


    private void CHECK() {

        if(containers!=null){
            if(containers.get("VID")!=null)
                videoPlayer.setVideoURI((Uri) containers.get("VID"));
            if(containers.get("IMG")!=null)
                thumbnail.setImageURI((Uri) containers.get("IMG"));
        }
    }


    private void DIALOG() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        views = layoutInflater.inflate(R.layout.dialog,null);
        AlertDialog.Builder albuild = new AlertDialog.Builder(getContext());
        albuild.setView(views);
        alertDialog = albuild.create();
    }


    private void API_MOVIES_LIST_CALL() {
        Calls call = Base_config.getConnection().create(Calls.class);
        Call<listOfmoviecategories> obj = call.getMovie_categories();
        obj.enqueue(new Callback<listOfmoviecategories>() {
            @Override
            public void onResponse(Call<listOfmoviecategories> call, Response<listOfmoviecategories> response) {

                assert response.body() != null;
                CarryOn(response.body().getList());
            }

            @Override
            public void onFailure(Call<listOfmoviecategories> call, Throwable t) {
                message(t.getLocalizedMessage(), getContext());
                Log.d(TAG,t.getMessage());
            }
        });
    }


    private void CarryOn(List<Object> body) {
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item,body);
        adapter.setDropDownViewResource(R.layout.text_pad);
        adapter.notifyDataSetChanged();
        spinner.setAdapter(adapter);
        spinnerProgressBar.setVisibility(View.INVISIBLE);
    }


    private void Mapping(View view) {

        videoPlayer = view.findViewById(R.id.videoPlayer);
        thumbnail = view.findViewById(R.id.thumbnail);
        videoTitle = view.findViewById(R.id.videoTitle);
        yearReleased = view.findViewById(R.id.yearreleased);
        spinner = view.findViewById(R.id.category);
        chooseVideo = view.findViewById(R.id.videoChooser);
        chooseThumbnail = view.findViewById(R.id.thumbnailChooser);
        upload = view.findViewById(R.id.uploader);
        spinnerProgressBar = view.findViewById(R.id.spinnerProgressBar);
        mainProgressbar = view.findViewById(R.id.mainProgressbar);
        progressCount = view.findViewById(R.id.progressCount);
        play = view.findViewById(R.id.play);
        pause = view.findViewById(R.id.pause);

    }


    private void send_data_to_s3(String writeUp, String p1, String p2, String p3) throws URISyntaxException {


        String fileName = videoTitle.getText().toString().replace(" ","".trim())+"_"+generate_name();
        List<Uri> path = new ArrayList<>();
        path.add((Uri) containers.get("IMG"));
        path.add((Uri) containers.get("VID"));



               for (int x = 0; x < path.size(); x++) {

                   String d = Find.get_file_selected_path(path.get(x), getActivity());
                   String format;

                   if(x == 0)
                       format ="Stream_Thumbnails/"+fileName+".png";
                   else
                       format = "Stream_Videouploads/"+fileName+".mp4";

                   AWSCredentials credentials = new BasicAWSCredentials(p1, p2);
                    AmazonS3 s3 = new AmazonS3Client(credentials);
                    Security.setProperty("networkaddress.cache.ttl", "60");
                    s3.setRegion(Region.getRegion(Regions.EU_WEST_3));
                    //s3.setObjectAcl("", ".png", CannedAccessControlList.PublicRead);
                    TransferUtility transferUtility = new TransferUtility(s3, getActivity());
                    assert d != null;
                    TransferObserver trans = transferUtility.upload(p3, format.trim(), new File(d));
                    trans.setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {

                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            float percentDone = ((float) bytesCurrent / (float) bytesTotal) * 100;
                            int percentDo = (int) percentDone;

                            progressCount.setText("Uploading... " + percentDo+"%");
                            if (percentDo == 100) {
                                progressCount.setText("Uploaded");
                                if(started_payload)
                                    send_payload(writeUp,fileName);
                                started_payload = false;
                                mainProgressbar.setVisibility(View.GONE);

                            }
                        }



                        @Override
                        public void onError(int id, Exception ex) {
                            message("S3= "+ ex.getLocalizedMessage(), getActivity());
                            mainProgressbar.setVisibility(View.GONE);
                            started_payload = false;
                            if(s3.doesObjectExist(p3,format))
                                   s3.deleteObject(p3,format);

                        }

                    });

             }
    }


    public int getCameraRotation(Context context, Uri uri, String path){
        int rotate = 0;
        try{
            context.getContentResolver().notifyChange(uri,null);
            File file = new File(path);

            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation  = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate =90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
            }

        }catch (Exception e){
            Log.d(TAG, "getCameraRotation: "+e.getLocalizedMessage());

        }
        return  rotate;
    }


    private void send_payload(String writeUp, String fileName) {
        if(thumbnail_orientation == 90)
            spin_direction = 90;
        else if(thumbnail_orientation == 270)
            spin_direction = -90;
        EchoToAPI(videoTitle.getText().toString(), Integer.parseInt(yearReleased.getText().toString()), categories, writeUp, fileName);

    }


    private  void EchoToAPI(String Mtitle, int year, String categories, String writeUp, String fileName){
        Calls sendMovies =  Base_config.getConnection().create(Calls.class);
        SendMovies send = new SendMovies(Mtitle.toLowerCase(),year,categories,writeUp,fileName,0,thumbnail_orientation,spin_direction,0);
        Call<SendMovies> sendMoviesCall =  sendMovies.sendMovie(send);
        sendMoviesCall.enqueue(new Callback<SendMovies>() {
            @Override
            public void onResponse(Call<SendMovies> call, Response<SendMovies> response) {
                 final String x;
                if(response.isSuccessful())
                    x ="Movie Added";
                else
                    x= "Error Occurred !";

                message(x,getContext());
            }

            @Override
            public void onFailure(Call<SendMovies> call, Throwable t) {
                message("Firebase= "+t.getLocalizedMessage(),getContext());

            }
        });
    }


    //Media selector  Custom ui
    public void file_picker(int v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        if (v == 1)
           intent.setDataAndType(imgUri,"image/*");
        else
            intent.setDataAndType(imgUri,"video/*");

        startActivityForResult(intent, PICK_IMAGE);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            assert data != null;
                imgUri = data.getData();
                assert imgUri != null;
                if (imgUri.toString().contains("image")) {
                    thumbnail.setImageURI(imgUri);
                    image_or_video = true;
                    progressCount.setText("");
                    containers.put("IMG",imgUri);
                    try {
                       thumbnail_orientation =  getCameraRotation(getContext(),imgUri,Find.get_file_selected_path(imgUri, getActivity()));
                       message(String.valueOf(getCameraRotation(getContext(),imgUri,Find.get_file_selected_path(imgUri, getActivity()))),getContext());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }

                }
                else
                    if (imgUri.toString().contains("video")) {
                        videoPlayer.setVideoURI(imgUri);
                        image_or_video = true;
                        progressCount.setText("");
                        containers.put("VID",imgUri);

                    }
               else
                  message("Pls Select an Image.", getActivity());
        }
    }


    public String generate_name() {
        long x = System.currentTimeMillis();
        long q = System.nanoTime();
        return String.valueOf(x).concat(String.valueOf(q));
    }

}