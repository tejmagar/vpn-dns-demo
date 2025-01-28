package rahul.secretcodes.vvpn;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_VPN_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestNotificationPermission();
        // Trigger VPN permission check and service start
        prepareVpnService(this);
//        Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        startActivity(intent);

    }

    public void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
            }
        }
    }

    private void prepareVpnService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            // Request user permission to start VPN
            startActivityForResult(intent, REQUEST_VPN_PERMISSION);
        } else {
            // Permission already granted, start the VPN service
            startVpnService(context);
        }
    }

    private void startVpnService(Context context) {
        Intent serviceIntent = new Intent(context, MyVpnService.class);
        context.startForegroundService(serviceIntent);
        Toast.makeText(context, "VPN service started!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == RESULT_OK) {
                // Permission granted, start the VPN service
                startVpnService(this);
            } else {
                // Permission denied
                Toast.makeText(this, "VPN permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
