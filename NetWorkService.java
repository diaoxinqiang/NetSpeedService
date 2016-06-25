

import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class NetWorkService extends Service {

    private Handler handler = new Handler();
    private Timer timer;
    private long rxtxTotal = 0;
    private boolean isNetBad = false;
    private int time;
    private double rxtxSpeed = 1.0f;
    private DecimalFormat showFloatFormat = new DecimalFormat("0.00");
    private Intent receiverIntent;
    public final static String NET_SPEED_RECEIVER_ACTION = "com.ridgepm.network_speed_action";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new RefreshTask(), 0L, (long) 1000);
        }

        receiverIntent = new Intent();
        receiverIntent.setAction(NET_SPEED_RECEIVER_ACTION);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service被终止的同时也停止定时器继续运行
        timer.cancel();
        timer = null;
        Log.i("NetworkSpeedService", "ondestroy");
    }

    class RefreshTask extends TimerTask {
        boolean isFirst = true;

        @Override
        public void run() {
            isNetBad = false;
            long tempSum = TrafficStats.getTotalRxBytes()
                    + TrafficStats.getTotalTxBytes();
            if (isFirst) {
                rxtxTotal = tempSum;
                isFirst = false;
            }
            long rxtxLast = tempSum - rxtxTotal;
            double tempSpeed = rxtxLast * 1000 / 1000;
            rxtxTotal = tempSum;
            if ((tempSpeed / 1024d) < 20 && (rxtxSpeed / 1024d) < 20) {
                time += 1;
            } else {
                time = 0;
            }
            rxtxSpeed = tempSpeed;
            Log.i("NetworkSpeedService", showFloatFormat.format(tempSpeed / 1024d) + "kb/s");
            if (time >= 5) {//连续四次检测网速都小于10kb/s  断定网速很差.
                isNetBad = true;
                Log.i("NetworkSpeedService", "网速差 " + isNetBad);
                time = 0; //重新检测
            }
            if (isNetBad) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        receiverIntent.putExtra("is_slow_net_speed", isNetBad);
                        sendBroadcast(receiverIntent);
//                        NetWorkService.this.stopSelf();
                    }
                });

            }
        }

    }
}
