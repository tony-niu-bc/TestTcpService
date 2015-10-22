package example.com.demo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class TestTCPReceiver extends BroadcastReceiver {
    public static String ACTION_POLL = "example.com.demo.action.POLL";

//    private TestTCPService testService = null;
//    private ServiceConnection conn = new ServiceConnection()
//    {
//
//        @Override
//        public void onServiceConnected(ComponentName name,
//                                       IBinder service) {
//            Log.i("TestTCPService", "TestTCPReceiver - onServiceConnected - ComponentName: " + name);
//			testService = ((TestTCPService.TestBinder)service).getService();
//            testService.dataSendRecv();
//            testService.unbindService(conn);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name)
//        {
//            Log.i("TestTCPService", "TestTCPReceiver - onServiceDisconnected - ComponentName: " + name);
//            testService = null;
//        }
//    };

    public TestTCPReceiver() {
    }

    private boolean bIsConnService = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        // BroadcastReceiver 只能 startService 不能 bindService
        Intent serviceIntent = new Intent(context, TestTCPService.class);
        context.startService(serviceIntent);

//        boolean bIsConnService = context.bindService(new Intent(context, TestTCPService.class),
//                                                     conn,
//                                                     Context.BIND_AUTO_CREATE |
//                                                     Context.BIND_ABOVE_CLIENT |
//                                                     Context.BIND_IMPORTANT);
//        Log.i("TestTCPService", "TestTCPReceiver - onServiceDisconnected - bIsConnService: " + bIsConnService);
    }
}
