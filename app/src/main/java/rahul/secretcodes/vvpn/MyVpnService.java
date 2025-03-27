package rahul.secretcodes.vvpn;

import static android.system.OsConstants.AF_INET;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ProtocolFamily;
import java.nio.ByteBuffer;

public class MyVpnService extends VpnService {
    private static final String CHANNEL_ID = "dns_changer_service_channel";
    private static final String DNS_SERVER_1 = "8.8.8.8";
    private static final String DNS_SERVER_2 = "8.8.4.4";

    private ParcelFileDescriptor vpnInterface;

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create and display a notification to run as a foreground service
        createNotificationChannel();
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("DNS Changer Service")
                    .setContentText("Custom DNS is active.")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .build();
        }
        startForeground(1, notification);

        // Configure the VPN interface
        try {
            Builder builder = new Builder();
            builder.addAddress("10.0.0.1", 24);
            builder.addDnsServer(DNS_SERVER_1);
            builder.addDnsServer(DNS_SERVER_2);
            builder.addRoute("103.94.159.74", 32);
            builder.allowFamily(AF_INET);

            // Establish the VPN interface
            vpnInterface = builder.setSession("DnsChangerSession").establish();

            new Thread(() -> {
                try {
                    FileDescriptor fileDescriptor = vpnInterface.getFileDescriptor();
                    Stream stream = new Stream(fileDescriptor);
                    RustBridge.process(stream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "DNS Changer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
