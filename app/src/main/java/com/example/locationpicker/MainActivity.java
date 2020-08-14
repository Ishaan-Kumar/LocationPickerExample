package com.example.locationpicker;

import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_SEND_LOCATION = 100;
    private ImageView attach;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        attach = findViewById(R.id.attach);
        attach.setOnClickListener(v -> doShareContent());

    }


    private void doShareContent() {
        showChatOptionPopUp();
    }

    private void showChatOptionPopUp() {
        try {
            final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.MyAlertDialogStyle);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.pop_up_chat_options);

            ImageView sendLocation = dialog
                    .findViewById(R.id.chat_send_location);

            sendLocation.setOnClickListener(v -> {
                dialog.cancel();
                try {
                    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (lm != null) {
                        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            getLocation();
                        } else {
                            showSettingsAlert();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            });

            WindowManager.LayoutParams wmlp = dialog.getWindow()
                    .getAttributes();
            wmlp.gravity = Gravity.BOTTOM;
            wmlp.width = ViewGroup.LayoutParams.FILL_PARENT;
            wmlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            wmlp.x = 62; // x position
            wmlp.y = 62; // y position

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to show chat options", Toast.LENGTH_SHORT).show();
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);

        alertDialog.setTitle("SETTINGS");
        alertDialog.setMessage("Enable Location Provider! Go to settings menu?");
        alertDialog.setPositiveButton("Settings",
                (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    this.startActivity(intent);
                });
        alertDialog.setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    public void getLocation() {
        startActivityForResult(new Intent(this, ChooseLocation.class), REQUEST_CODE_SEND_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SEND_LOCATION) {
            Log.d(TAG, "onActivityResult: REQUEST_CODE_LOCATION");
            if (resultCode == RESULT_OK) {
                if (data.getExtras() != null) {
                    double locationLat = data.getExtras().getDouble(ChooseLocation.LOCATION_LAT);
                    double locationLong = data.getExtras().getDouble(ChooseLocation.LOCATION_LONG);
                    sendMessage(locationLat, locationLong);
                }
            } else {
                Log.d(TAG, "onActivityResult: Location Sending aborted by user");
            }
        }
    }

    private void sendMessage(double locationLat, double locationLong) {
        Log.d(TAG, "sendMessage() called with: locationLat = [" + locationLat + "], locationLong = [" + locationLong + "]");
        Toast.makeText(this, "Lat: " + locationLat + "\nLong: " + locationLong, Toast.LENGTH_LONG).show();
    }

}