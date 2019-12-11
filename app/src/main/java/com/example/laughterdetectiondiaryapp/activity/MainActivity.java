package com.example.laughterdetectiondiaryapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.example.laughterdetectiondiaryapp.R;
import com.example.laughterdetectiondiaryapp.adapters.FileViewerAdapter;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.threeten.bp.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity
        implements OnDateSelectedListener, OnMonthChangedListener{

    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FileViewerFragment";

    private FileViewerAdapter mFileViewerAdapter;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");

    MaterialCalendarView widget;
    TextView textView;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        observer.startWatching();

        widget = (MaterialCalendarView)findViewById(R.id.calendarView);
        textView = (TextView)findViewById(R.id.textView);
        recyclerView =(RecyclerView) findViewById(R.id.recyclerView);

        widget.setOnDateChangedListener(this);
        widget.setOnMonthChangedListener(this);

        //Setup initial text
        textView.setText("No Selection");

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
