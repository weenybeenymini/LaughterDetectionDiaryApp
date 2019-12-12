package com.example.laughterdetectiondiaryapp.background;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import com.example.laughterdetectiondiaryapp.DBHelper;
import com.example.laughterdetectiondiaryapp.R;
import com.example.laughterdetectiondiaryapp.activity.MainActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

public class RealService extends Service {
    private static final String LOG_TAG = "RecordingService";
    private Recorder recorder;

    private String originFileName = null;
    private String mFileName = null;
    private String mFilePath = null;
    private long mElapsedMillis = 0;

    private String tFileName = null;
    private String tFilePath = null;
    private long tElapsedMillis = 0;

    private long now = 0;

    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private boolean first = true;
    File f;
    private boolean isActive;
    private Thread mainThread;
    public static Intent serviceIntent = null;

    private DBHelper mDatabase;

    public RealService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIntent = intent;
        isActive = intent.getBooleanExtra("isActive", false);
        Log.d("나","서비스호출됬어");
        mDatabase = new DBHelper(getApplication());
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("aa hh:mm");
                boolean run = true;
                while (run) {
                    RecodingTime1();
                    Date date = new Date();
                    RecodingTime2();
                    //showToast(getApplication(), sdf.format(date));
                    sendNotification(sdf.format(date));
                }
            }
        });
        mainThread.start();
        return START_NOT_STICKY;
    }
    private void RecodingTime1(){
        long curTime = System.currentTimeMillis();
        startRecoding("smile1");
        while(true) {
            if (System.currentTimeMillis() - curTime > 1000 * 10) {
                break;
            }
        }
        stopRecoding();
    }
    private void RecodingTime2(){
        long curTime = System.currentTimeMillis();
        startRecoding("smile2");
        while(true) {
            if (System.currentTimeMillis() - curTime > 1000 * 10) {
                break;
            }
        }
        stopRecoding();
    }
    private void startRecoding(String name){
        Log.d("나","녹음");
        originFileName = name;
        setupRecorder();
        now = System.currentTimeMillis();
        recorder.startRecording();
        Log.d("나",mFileName + "이거 녹음했어");
    }
    private void stopRecoding(){
        Log.d("나","저장");
        try {
            recorder.stopRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mElapsedMillis = (System.currentTimeMillis() - now);
        tElapsedMillis = mElapsedMillis;
        tFileName = mFileName;
        tFilePath = mFilePath;
        Message msg = handler.obtainMessage();
        handler.sendMessage(msg);
        Log.d("나",tFileName + "이거저장해쏘");
    }

    final Handler handler = new Handler(){
        public void handleMessage(Message msg){
            // 원래 하려던 동작 (UI변경 작업 등)
            mDatabase.addRecording(tFileName, tFilePath, tElapsedMillis);
        }
    };

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
            mFilePath += "/LaughterRecorder/" + mFileName;
            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }
    public void setupRecorder() {
        setFileNameAndPath();
        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
                        animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                    }
                }), f);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //앱이 종료될 때 서비스를 종료하고 알람타이머를 실행해
        serviceIntent = null;
        setAlarmTimer();
        Thread.currentThread().interrupt();
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
    public void showToast(final Application application, final String msg) {
        Handler h = new Handler(application.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(application, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
    protected void setAlarmTimer() {
        //앱 종료 1초 후 알람 실행
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, AlarmRecever.class);
        intent.putExtra("isActive", isActive);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0,intent,0);
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
    }
    private void sendNotification(String messageBody) {
        Log.d("서비스리얼", messageBody);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);
        String channelId = "fcm_default_channel";//getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_media_play)//drawable.splash)
                        .setContentTitle("Service test")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,"Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
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