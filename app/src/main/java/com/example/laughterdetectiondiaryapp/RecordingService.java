package com.example.laughterdetectiondiaryapp;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

/**
 * Created by Daniel on 12/28/2014.
 */
public class RecordingService extends Service {

    private static final String LOG_TAG = "RecordingService";

    private Recorder recorder;


    private String originFileName = null;

    private String mFileName = null;
    private String mFilePath = null;

    private long now = 0;
    private long mElapsedMillis = 0;
    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private boolean first = true;

    File f;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        originFileName = intent.getStringExtra("name");

        if(first){
            setupRecorder();
            first= false;
        }

        now = System.currentTimeMillis();
        recorder.startRecording();

        return START_STICKY;
    }
    // 서비스가 호출될 때 마다 실행

    @Override
    public void onDestroy() {
        if (recorder != null) {
            mstopRecording();
        }
        first = true;

        Intent intent = new Intent("custom-event-name");
        intent.putExtra("name", mFileName);
        intent.putExtra("path", mFilePath);
        intent.putExtra("em", mElapsedMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onDestroy();
    }
    // 서비스가 종료될 때 실행

    public void setupRecorder() {
        setFileNameAndPath();

        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override
                    public void onAudioChunkPulled(AudioChunk audioChunk) {
                        animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                    }
                }), f);
    }

    public void setFileNameAndPath() {
        int count = 0;

        now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd_hhmmss");
        String getTime = sdf.format(date);

        do{
            count++;

            if(count==1){
                mFileName = originFileName + "_" + getTime + ".wav";
            }else{
                mFileName = originFileName + "_" + getTime + count + ".wav";
            }
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }

    public void mstopRecording() {
        try {
            recorder.stopRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mElapsedMillis = (System.currentTimeMillis() - now);
    }

    private void animateVoice(final float maxPeak) {
        //animate().scaleX(1 + maxPeak).scaleY(1 + maxPeak).setDuration(10).start();
    }

    private PullableSource mic() {
        return new PullableSource.Default(
                new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 16000
                )
        );
    }
}