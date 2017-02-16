package com.example.jeremy.auvuaui;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import com.github.niqdev.mjpeg.DisplayMode;
import android.view.WindowManager;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegInputStream;
import com.github.niqdev.mjpeg.MjpegView;
import rx.Observable;
import rx.functions.Action1;
import android.app.ProgressDialog;
import android.app.ProgressDialog;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.google.vr.sdk.widgets.video.VrVideoView.Options;
import android.opengl.*;






public class MainActivity extends Activity {
    //  ProgressDialog pDialog;
    //  MjpegView mjpegViewLeft;
    //  MjpegView mjpegViewRight;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_IS_PAUSED = "isPaused";
    private static final String STATE_PROGRESS_TIME = "progressTime";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;
    private int loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;
    private Uri fileUri;
    private Options videoOptions = new Options();
    private VideoLoaderTask backgroundVideoLoaderTask;
    protected VrVideoView videoWidgetView;
    private SeekBar seekBar;
    private ImageButton volumeToggle;
    private boolean isMuted;
    private boolean isPaused = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBarListener());

        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setEventListener(new ActivityEventListener());
        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;
        handleIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, this.hashCode() + ".onNewIntent()");
        setIntent(intent);
        handleIntent(intent);
    }

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }
    public boolean isMuted() {
        return isMuted;
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.i(TAG, "ACTION_VIEW Intent received");
            fileUri = intent.getData();
            if (fileUri == null) {
                Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
            } else {
                Log.i(TAG, "Using file " + fileUri.toString());
            }
            videoOptions.inputFormat = intent.getIntExtra("inputFormat", Options.FORMAT_DEFAULT);
            videoOptions.inputType = intent.getIntExtra("inputType", Options.TYPE_MONO);
        } else {
            Log.i(TAG, "Intent is not ACTION_VIEW. Using the default video.");
            fileUri = null;
        }

        if (backgroundVideoLoaderTask != null) {// Cancel any task from a previous intent sent to this activity.
            backgroundVideoLoaderTask.cancel(true);
        }
        backgroundVideoLoaderTask = new VideoLoaderTask();
        backgroundVideoLoaderTask.execute(Pair.create(fileUri, videoOptions));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        videoWidgetView.seekTo(progressTime);
        seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
        seekBar.setProgress((int) progressTime);

        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            videoWidgetView.pauseVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoWidgetView.pauseRendering();
        isPaused = true;
    }

    @Override
    protected void onResume() {

//    mjpegViewRight = (MjpegView) findViewById(R.id.imageRight);
//    mjpegViewLeft = (MjpegView) findViewById(R.id.imageLeft);
//
//    Observable<MjpegInputStream> mjpegObserverRight = Mjpeg.newInstance()
//            .open("", 5);
//
//    Observable<MjpegInputStream> mjpegObserverLeft = Mjpeg.newInstance()
//            .open("", 5);
//
//
//    mjpegObserverRight.subscribe(new Action1<MjpegInputStream>() {
//      @Override
//      public void call(MjpegInputStream mjpegInputStream) {
//        mjpegViewRight.setSource(mjpegInputStream);
//        mjpegViewRight.setDisplayMode(DisplayMode.BEST_FIT);
//        mjpegViewRight.showFps(true);
//      }
//    });
//    mjpegObserverLeft.subscribe(new Action1<MjpegInputStream>() {
//      @Override
//      public void call(MjpegInputStream mjpegInputStream) {
//        mjpegViewLeft.setSource(mjpegInputStream);
//        mjpegViewLeft.setDisplayMode(DisplayMode.BEST_FIT);
//        mjpegViewLeft.showFps(true);
//      }
//
//    });

        super.onResume();
        videoWidgetView.resumeRendering();

    }

    @Override
    protected void onDestroy() {
        videoWidgetView.shutdown();
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
            videoWidgetView.playVideo();
        } else {
            videoWidgetView.pauseVideo();
        }
        isPaused = !isPaused;

//    mjpegViewRight.stopPlayback();
//    mjpegViewLeft.stopPlayback();
    }

    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                videoWidgetView.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) { }
    }

    private class ActivityEventListener extends VrVideoEventListener  {

        @Override
        public void onLoadSuccess() {
            Log.i(TAG, "Sucessfully loaded video " + videoWidgetView.getDuration());
            loadVideoStatus = LOAD_VIDEO_STATUS_SUCCESS;
            seekBar.setMax((int) videoWidgetView.getDuration());

        }

        @Override
        public void onLoadError(String errorMessage) {
            // An error here is normally due to being unable to decode the video format.
            loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
            Toast.makeText(
                    MainActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "Error loading video: " + errorMessage);
        }

        @Override
        public void onClick() {
            togglePause();
        }

        @Override
        public void onNewFrame() {

            seekBar.setProgress((int) videoWidgetView.getCurrentPosition());
        }
        @Override
        public void onCompletion() {
            videoWidgetView.seekTo(0);
        }
    }
    public class HeadTransform {
        private static final float GIMBAL_LOCK_EPSILON = 0.01f;
        private final float[] mHeadView;

        public HeadTransform() {
            super();
            Matrix.setIdentityM(this.mHeadView = new float[16], 0);
        }

        float[] getHeadView() {
            return this.mHeadView;
        }

        public void getHeadView(final float[] headView, final int offset) {
            if (offset + 16 > headView.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            System.arraycopy(this.mHeadView, 0, headView, offset, 16);
        }

        public void getForwardVector(final float[] forward, final int offset) {
            if (offset + 3 > forward.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            for (int i = 0; i < 3; ++i) {
                forward[i + offset] = -this.mHeadView[8 + i];
            }
        }

        public void getUpVector(final float[] up, final int offset) {
            if (offset + 3 > up.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            for (int i = 0; i < 3; ++i) {
                up[i + offset] = this.mHeadView[4 + i];
            }
        }

        public void getRightVector(final float[] right, final int offset) {
            if (offset + 3 > right.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            for (int i = 0; i < 3; ++i) {
                right[i + offset] = this.mHeadView[i];
            }
        }

        public void getQuaternion(final float[] quaternion, final int offset) {
            if (offset + 4 > quaternion.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            final float[] m = this.mHeadView;
            final float t = m[0] + m[5] + m[10];
            float w;
            float x;
            float y;
            float z;
            if (t >= 0.0f) {
                float s = (float)Math.sqrt(t + 1.0f);
                w = 0.5f * s;
                s = 0.5f / s;
                x = (m[9] - m[6]) * s;
                y = (m[2] - m[8]) * s;
                z = (m[4] - m[1]) * s;
            }
            else if (m[0] > m[5] && m[0] > m[10]) {
                float s = (float)Math.sqrt(1.0f + m[0] - m[5] - m[10]);
                x = s * 0.5f;
                s = 0.5f / s;
                y = (m[4] + m[1]) * s;
                z = (m[2] + m[8]) * s;
                w = (m[9] - m[6]) * s;
            }
            else if (m[5] > m[10]) {
                float s = (float)Math.sqrt(1.0f + m[5] - m[0] - m[10]);
                y = s * 0.5f;
                s = 0.5f / s;
                x = (m[4] + m[1]) * s;
                z = (m[9] + m[6]) * s;
                w = (m[2] - m[8]) * s;
            }
            else {
                float s = (float)Math.sqrt(1.0f + m[10] - m[0] - m[5]);
                z = s * 0.5f;
                s = 0.5f / s;
                x = (m[2] + m[8]) * s;
                y = (m[9] + m[6]) * s;
                w = (m[4] - m[1]) * s;
            }
            quaternion[offset + 0] = x;
            quaternion[offset + 1] = y;
            quaternion[offset + 2] = z;
            quaternion[offset + 3] = w;

        }

        public void getEulerAngles(final float[] eulerAngles, final int offset) {

            if (offset + 3 > eulerAngles.length) {
                throw new IllegalArgumentException("Not enough space to write the result");
            }
            final float pitch = (float)Math.asin(this.mHeadView[6]);
            float yaw;
            float roll;
            if ((float)Math.sqrt(1.0f - this.mHeadView[6] * this.mHeadView[6]) >= GIMBAL_LOCK_EPSILON) {
                yaw = (float)Math.atan2(-this.mHeadView[2], this.mHeadView[10]);
                roll = (float)Math.atan2(-this.mHeadView[4], this.mHeadView[5]);
            }
            else {
                yaw = 0.0f;
                roll = (float)Math.atan2(this.mHeadView[1], this.mHeadView[0]);
            }
            eulerAngles[offset + 0] = -pitch;
            eulerAngles[offset + 1] = -yaw;
            eulerAngles[offset + 2] = -roll;

        }
    }

    class VideoLoaderTask extends AsyncTask<Pair<Uri, Options>, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {
            try {
                if (fileInformation == null || fileInformation.length < 1
                        || fileInformation[0] == null || fileInformation[0].first == null) {
                    // No intent was specified, so we default to playing the local stereo-over-under video.
                    Options options = new Options();
                    options.inputType = Options.TYPE_STEREO_OVER_UNDER;
                    videoWidgetView.loadVideoFromAsset("movie.mp4", options);
                }
                //Loads video from file location
                else {
                    videoWidgetView.loadVideo(fileInformation[0].first, fileInformation[0].second);
                }
            } catch (IOException e) {
                // An error here is normally due to being unable to locate the file.
                loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
                // Since this is a background thread, we need to switch to the main thread to show a toast.
                videoWidgetView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast
                                .makeText(MainActivity.this, "Error opening file. ", Toast.LENGTH_LONG)
                                .show();
                    }
                });
                Log.e(TAG, "Could not open video: " + e);
            }

            return true;
        }
    }
}