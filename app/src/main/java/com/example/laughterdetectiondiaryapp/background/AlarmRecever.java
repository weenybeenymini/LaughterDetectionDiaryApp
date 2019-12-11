package com.example.laughterdetectiondiaryapp.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmRecever extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("오잉", "위");
            //백그라운드로 실행하라고 해!
            Intent in = new Intent(context, RestartService.class);
            context.startForegroundService(in);
        } else {
            Log.d("오잉", "아래");
            //서비스시작해!! 알아서 백그라운드로 돌아가
            Intent in = new Intent(context, RealService.class);
            context.startService(in);
        }
    }

}