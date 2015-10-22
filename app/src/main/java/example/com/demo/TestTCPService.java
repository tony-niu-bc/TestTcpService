package example.com.demo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.os.Process;
import android.widget.RemoteViews;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestTCPService extends Service {
    private boolean bIsRunning = true; // true - running, false - exit

    private Socket clientSocket = null;

    // Android 中创建 (先)ObjectOutputStream、(后)ObjectInputStream 有先后
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private FileWriter fileWriter;
    private BufferedWriter bufWriter;

    private SimpleDateFormat sdfLogTime;

    public class TestBinder extends Binder
    {
        public TestTCPService getService()
        {
            return TestTCPService.this;
        }
    }

    public TestTCPService() {
    }

    private boolean checkNetWorkState() {
        ConnectivityManager con = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = con.getActiveNetworkInfo();// 获取可用的网络服务
        return ((info != null) && info.isAvailable());
    }

    private boolean initService(){
        boolean bIsSuccess = false;

        do {
            // 实例化对象并连接到服务器
            try {
                clientSocket = new Socket("10.1.200.16", 8080);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }

            // 获得Socket的输出流
            try {
                outStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            // 获得输入流
            try {
                inStream = clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            try {
                fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +
                                            File.separator + "TestTCPService.txt", true); // 追加不进行覆盖
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            bufWriter = new BufferedWriter(fileWriter);

            sdfLogTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 日志输出的时间格式

            bIsSuccess = true;

        } while (false);

        return bIsSuccess;
    }

    private void uninitService() {
        try {
            if (null != inStream) {
                inStream.close();
                inStream = null;
            }

            if (null != outStream) {
                outStream.close();
                outStream = null;
            }

            if (null != clientSocket) {
                clientSocket.close();
                clientSocket = null;
            }

            if (null != bufWriter) {
                bufWriter.close();
                bufWriter = null;
            }

            if (null != fileWriter) {
                fileWriter.close();
                fileWriter = null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean dataSendRecv() {
        boolean bIsSuccess = false;

        byte[] bufRecv = new byte[128];
        String strRecv = null;

        do {
            if ((null == outStream)
              ||(null == inStream)
              ||(null == bufWriter)) {
                break;
            }

            Date timeNow = new Date();
            String strNow = sdfLogTime.format(timeNow);
            String needWriteMessage = strNow + " " + "NetworkState: " + checkNetWorkState();

            Log.i("TestTCPService", "NetworkState: " + checkNetWorkState());

            try {
                bufWriter.write(needWriteMessage);
                bufWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            // 发送数据
            try {
                byte[] byteSend = new byte[]{0x00, 0x00, 0x00, 0x04, 65, 66, 67, 68};
                outStream.write(byteSend);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            // 读取输入数据（阻塞）
            try {
                inStream.read(bufRecv);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // 字符编码转换
            try {
                byte[] byteData = new byte[bufRecv.length - 4];
                System.arraycopy(bufRecv, 4, byteData, 0, bufRecv.length - 4);
                strRecv = new String(byteData, "utf-8").trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }

            Log.i("TestTCPService", "From server: " + strRecv);

            timeNow = new Date();
            strNow = sdfLogTime.format(timeNow);
            needWriteMessage = strNow + " " + strRecv;

            try {
                bufWriter.write(needWriteMessage);
                bufWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            bIsSuccess = true;

        } while(false);

        return bIsSuccess;
    }

    @Override
    public void onCreate() {
        Log.i("TestTCPService",
              "TestTCPService - onCreate - ProcessID - TID: " + Thread.currentThread().getId() +
              " PID: " + Process.myPid() +
              " TID: " + Process.myTid() +
              " UID: " + Process.myUid());

        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (!initService()) {
                        stopSelf();
                        break;
                    }

                    if (null != bufWriter) {
                        Date timeNow = new Date();
                        String strNow = sdfLogTime.format(timeNow);
                        String needWriteMessage = strNow + "onCreate" + " ProcessID =" + Thread.currentThread().getId() + " PID =" + Process.myPid() + "(" + "NetworkState: " + checkNetWorkState() + ")";

                        try {
                            bufWriter.write(needWriteMessage);
                            bufWriter.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    while (bIsRunning) {
                        synchronized (TestTCPService.this) {
                            try {
                                TestTCPService.this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                continue;
                            }

                            dataSendRecv();
                        }
                    }

                    uninitService();

                } while (false);
            }
        }).start();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("TestTCPService",
                "TestTCPService - onStartCommand -" + " intent = " + intent + " flags =" + flags + " startId =" + startId);

        if (null != bufWriter) {
            Date timeNow = new Date();
            String strNow = sdfLogTime.format(timeNow);
            String needWriteMessage = strNow + "onStartCommand" + " flags =" + flags + " startId =" + startId + "(" + "NetworkState: " + checkNetWorkState() + ")";

            try {
                bufWriter.write(needWriteMessage);
                bufWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized (this) {
                this.notify();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("todolist",
              "TestService - onBind - intent: " + intent +
              " ProcessID - TID: " + Thread.currentThread().getId() +
              " PID: " + Process.myPid() +
              " TID: " + Process.myTid() +
              " UID: " + Process.myUid());

        if (null != bufWriter) {
            Date timeNow = new Date();
            String strNow = sdfLogTime.format(timeNow);
            String needWriteMessage = strNow + "onBind" + "(" + "NetworkState: " + checkNetWorkState() + ")";

            try {
                bufWriter.write(needWriteMessage);
                bufWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

		return new TestBinder();
    }

    @Override
    public void onDestroy() {
        Log.i("TestTCPService", "TestTCPService - onDestroy");

        bIsRunning = false;

        super.onDestroy();
    }
}
