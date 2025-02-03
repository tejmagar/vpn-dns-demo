package rahul.secretcodes.vvpn;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

public class MyVpnService extends VpnService {
    private static final String CHANNEL_ID = "dns_changer_service_channel";
    private static final String DNS_SERVER_1 = "157.245.97.90";
    private static final String DNS_SERVER_2 = "157.245.97.90";

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
            builder.addAddress("10.0.0.1", 32);
            builder.addDnsServer(DNS_SERVER_1);
            builder.addDnsServer(DNS_SERVER_2);
            builder.addRoute("10.0.0.1", 32);

            // Establish the VPN interface
            vpnInterface = builder.setSession("DnsChangerSession").establish();
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
