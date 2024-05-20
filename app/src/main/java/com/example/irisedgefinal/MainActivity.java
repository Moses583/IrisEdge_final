    package com.example.irisedgefinal;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

    public class MainActivity extends AppCompatActivity {
        private ImageView imgRecord;
        private ArrayList<String> locations;
        private TextView txtSpeech,txtFromPython;
        private Button ttsButton, sendData;
        private ExtendedFloatingActionButton button;
        private TextToSpeech toSpeech;
        String result;

        static final int REQUEST_VIDEO_CAPTURE = 1;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            initViews();

            locations = new ArrayList<>();
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, ImageDetectionActivity.class));
                }
            });
            imgRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    speech();
                }
            });
            sendData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new SendDataTask().execute(txtSpeech.getText().toString());
                }
            });
            ttsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTxtSpeech("Your origin is "+locations.get(0)+" and your destination is "+locations.get(1)+", Press open camera button to start navigation");
                    if (locations.isEmpty()){
                        Toast.makeText(MainActivity.this, "list empty", Toast.LENGTH_SHORT).show();
                    }else {
                        locations.clear();
                    }
                }
            });

        }
        public void setTxtSpeech(String text){
            toSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR){
                        toSpeech.setLanguage(Locale.UK);
                    }
                    toSpeech.speak(text,TextToSpeech.QUEUE_FLUSH, null);
                }
            });
        }
        public void speech(){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Start speaking");
            startActivityForResult(intent,100);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK){
                Toast.makeText(this, "taking video", Toast.LENGTH_SHORT).show();
            } else if (requestCode == 100 && resultCode == RESULT_OK) {
                if (data != null) {
                    String recorded = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                    txtSpeech.setText(recorded);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
        private class SendDataTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                String finalResult;String data = params[0];
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                        "{\n\t\"text\": \"" + data + "\"\n}");
                Request request = new Request.Builder().url("http://192.168.0.106:5000/process").post(body).addHeader("Content-Type", "application/json")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    finalResult = response.body().string();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return finalResult;
            }
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                txtFromPython.setText(result);
                locations.add(result);
                showToast();
            }
        }
        private void showToast(){
            for (int i = 0; i < locations.size(); i++) {
                Toast.makeText(MainActivity.this, locations.get(i), Toast.LENGTH_SHORT).show();
            }
        }
        private void startNavigation(){
            startActivity(new Intent(MainActivity.this, ImageDetectionActivity.class));
        }

        private void initViews() {
            button = findViewById(R.id.btnTakePicture);
            imgRecord = findViewById(R.id.imgRecord);
            txtSpeech = findViewById(R.id.txtSpeech);
            ttsButton = findViewById(R.id.btnTTS);
            txtFromPython = findViewById(R.id.txtFromPython);
            sendData = findViewById(R.id.btnSendData);
        }
}