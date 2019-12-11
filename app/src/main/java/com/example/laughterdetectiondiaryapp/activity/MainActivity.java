package com.example.laughterdetectiondiaryapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.PowerManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.laughterdetectiondiaryapp.R;
import com.example.laughterdetectiondiaryapp.RecordingWorker;
import com.example.laughterdetectiondiaryapp.adapters.FileViewerAdapter;
import com.example.laughterdetectiondiaryapp.background.RealService;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements OnDateSelectedListener, OnMonthChangedListener{

    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FileViewerFragment";

    private FileViewerAdapter mFileViewerAdapter;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");

    MaterialCalendarView widget;
    TextView textView;
    RecyclerView recyclerView;
    Switch switchView;

    private Intent serviceIntent;
    private boolean isActivate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        observer.startWatching();

        widget = (MaterialCalendarView)findViewById(R.id.calendarView);
        textView = (TextView)findViewById(R.id.textView);
        recyclerView =(RecyclerView) findViewById(R.id.recyclerView);
        switchView =(Switch) findViewById(R.id.activation_switch);

        //서비스 제공 유무 판단
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isActivate = isChecked;
                if (isActivate == true){
                    Toast.makeText(MainActivity.this, "켜짐", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "꺼짐", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //달력처리
        widget.setOnDateChangedListener(this);
        widget.setOnMonthChangedListener(this);

        textView.setText("No Selection");

        //파일 리사이클러뷰 처리
        recyclerView.setLayoutManager(new LinearLayoutManager(this)) ;

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        //newest to oldest order (database stores from oldest to newest)
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        recyclerView.setLayoutManager(llm);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mFileViewerAdapter = new FileViewerAdapter(this, llm);
        recyclerView.setAdapter(mFileViewerAdapter);

        //백그라운드 서비스 처리
        /*
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }

        if (RealService.serviceIntent==null) {
            serviceIntent = new Intent(this, RealService.class);
            startService(serviceIntent);
        } else {
            serviceIntent = RealService.serviceIntent;//getInstance().getApplication();
            Toast.makeText(getApplicationContext(), "already", Toast.LENGTH_LONG).show();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent!=null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }

    @Override
    public void onDateSelected(
            @NonNull MaterialCalendarView widget,
            @NonNull CalendarDay date,
            boolean selected) {
        textView.setText(selected ? date.getDate().toString() : "No Selection");
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        //getSupportActionBar().setTitle((CharSequence) date.getDate().toString());
    }

    FileObserver observer =
            new FileObserver(android.os.Environment.getExternalStorageDirectory().toString()
                    + "/SoundRecorder") {
                // set up a file observer to watch this directory on sd card
                @Override
                public void onEvent(int event, String file) {
                    if(event == FileObserver.DELETE){
                        // user deletes a recording file out of the app

                        String filePath = android.os.Environment.getExternalStorageDirectory().toString()
                                + "/SoundRecorder" + file + "]";

                        Log.d(LOG_TAG, "File deleted ["
                                + android.os.Environment.getExternalStorageDirectory().toString()
                                + "/SoundRecorder" + file + "]");

                        // remove file from database and recyclerview
                        mFileViewerAdapter.removeOutOfApp(filePath);
                    }
                }
            };
}
