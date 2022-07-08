package ch.zli.hello.lazytimer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.Locale;
import java.util.Random;


/**
 * Author: Danial Vaezi
 * Date: 07:07:2022
 * :Desccription: Timer Application called Lazy Timer. Timer can be started and stopped with a shake of the phone and
 * you receive notifications when the timer is up
 *
 * Main Timer Logic written by "codinginflow" on github.
 * Source:
 * https://gist.github.com/codinginflow/ad9042bdaa712bdbc361a0b697179367
 *
 * Shake and sensor detection Logic inspired from:
 * https://demonuts.com/android-shake-detection/
 *
 * Notification Logic by "Cambo Tutorial" on Youtube.
 * https://www.youtube.com/watch?v=v1s36wmqP8M
 *
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * Declaring Attributes
     */

    private EditText mEditTextInput;
    private TextView mTextViewCountDown;
    private Button mButtonSet;
    private Button mButtonStartPause;
    private Button mButtonReset;


    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning = false;

    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;
    private long mEndTime;

    private long mShakeTimestamp;
    private static final int SHAKE_SLOP_TIME_MS = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mEditTextInput = findViewById(R.id.edit_text_input);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);

        mButtonSet = findViewById(R.id.button_set);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        mButtonReset = findViewById(R.id.button_reset);

        /**
         * OnClick Method for entering a custom time in the Timer field.
         * Checks for emtpy inputs and negative numbers.
         */

        mButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = mEditTextInput.getText().toString();
                if (input.length() == 0) {
                    Toast.makeText(MainActivity.this, "Field can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                long millisInput = Long.parseLong(input) * 60000;
                if (millisInput == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                    return;
                }

                setTime(millisInput);
                mEditTextInput.setText("");
            }
        });

        /**
         * OnClick Method that connects the button to the StartTimer and StopTimer Methods.
         */

        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });


        /**
         * OnClick Method that connects the button to the resetTimer method.
         */

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });


        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor sensorShake = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);



        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent != null) {
                    float x_accl = sensorEvent.values[0];
                    float y_accl = sensorEvent.values[1];
                    float z_accl = sensorEvent.values[2];


                    /**
                     * if statement checks sensors for movement/shaking
                     */

                    if (x_accl > 2 ||
                            x_accl < -2 ||
                            y_accl > 12 ||
                            y_accl < -12 ||
                            z_accl > 2 ||
                            z_accl < -2) {

                        final long now = System.currentTimeMillis();
                        // ignore shake events too close to each other (500ms)
                        if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                            return;
                        }

                        /**
                         * Starts/stops the timer after shaking
                         */

                        if(mTimerRunning) {
                            pauseTimer();

                        } else {
                            startTimer();

                        }
                        mShakeTimestamp = now;
                    } else {

                    }

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        sensorManager.registerListener(sensorEventListener, sensorShake, SensorManager.SENSOR_DELAY_FASTEST);


    }


    private void setTime(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
        closeKeyboard();
    }


    /**
     * Method for Starting the timer
     */

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                updateWatchInterface();
            }
        }.start();

        mTimerRunning = true;
        updateWatchInterface();
    }

    /**
     * Method for pausing the timer
     */

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateWatchInterface();
    }

    /**
     * Method for resetting the timer back to the previously entered time
     */

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateWatchInterface();
    }

    /**
     * Method for formatting and updating the digits in the view.
     * Example: Removes hour digit when timer is below 60 Minutes and Vice Versa.
     */

    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        if (seconds == 59 && hours == 0 && minutes == 0){
            createNotif(); //notifies user in the app when 1 minute is over/left on the timer

        }

        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
        }

        mTextViewCountDown.setText(timeLeftFormatted);
    }

    /**
     * Method resposible for the Interface and the Logic behind when and what buttons are visible.
     * Example: Updates the Start to the stop button when timer is running
     */

    private void updateWatchInterface() {
        if (mTimerRunning) {
            mEditTextInput.setVisibility(View.INVISIBLE);
            mButtonSet.setVisibility(View.INVISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonStartPause.setText("Pause");
        } else {
            mEditTextInput.setVisibility(View.VISIBLE);
            mButtonSet.setVisibility(View.VISIBLE);
            mButtonStartPause.setText("Start");

            if (mTimeLeftInMillis < 1000) {
                mButtonStartPause.setVisibility(View.INVISIBLE);
            } else {
                mButtonStartPause.setVisibility(View.VISIBLE);
            }

            if (mTimeLeftInMillis < mStartTimeInMillis) {
                mButtonReset.setVisibility(View.VISIBLE);
            } else {
                mButtonReset.setVisibility(View.INVISIBLE);
            }
        }
    }


    /**
     * Method for bringing up and hiding the numpad to enter time
     */

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Method for when Timer is stopped. Saves how much time is still left on the timer
     */

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /**
     * Method saving the data via SharedPrefs. so that timer can tun in background
     */

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 600000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        updateCountDownText();
        updateWatchInterface();



        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateWatchInterface();
            } else {
                startTimer();
            }
        }
    }

    /**
     * Method for creating and sending modern-looking "Hovering Notifications" in the app.
     */

    private void createNotif()
    {
        String id = "my_channel_id_01";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel =manager.getNotificationChannel(id);
            if(channel ==null)
            {
                channel = new NotificationChannel(id,"Channel Title", NotificationManager.IMPORTANCE_HIGH);
                //config nofication channel
                channel.setDescription("[Channel description]");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{100,1000,200,340});
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
        }
        Intent notificationIntent = new Intent(this,NoficationActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,id)
                .setSmallIcon(R.drawable.icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.bg))
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(BitmapFactory.decodeResource(getResources(),R.drawable.bg))
                        .bigLargeIcon(null))
                .setContentTitle("Timer Alert")
                .setContentText("Only 1 Minute left")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{100,1000,200,340})
                .setAutoCancel(false)
                .setTicker("Notification");
        builder.setContentIntent(contentIntent);
        NotificationManagerCompat m = NotificationManagerCompat.from(getApplicationContext());
        //id to generate new notification in list notifications menu
        m.notify(new Random().nextInt(),builder.build());

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}