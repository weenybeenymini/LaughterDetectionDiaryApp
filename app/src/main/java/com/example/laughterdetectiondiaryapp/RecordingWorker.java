package com.example.laughterdetectiondiaryapp;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Worker;
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
public class RecordingWorker extends Worker {
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

    @NonNull
    @Override
    public WorkerResult doWork() {
        Log.d("시작", "시작");
        /*
        originFileName = "";
        if(first){
            setupRecorder();
            first= false;
        }
        now = System.currentTimeMillis();
        Log.d("시작", Long.toString(now));
        recorder.startRecording();
        Log.d("시작", "녹음시작");
        //원래 ondestroy쪽
        if (recorder != null) {
            mstopRecording();
        }
        first = true;
        Log.d("시작", "끝났어");

         */
        return WorkerResult.SUCCESS;
    }
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