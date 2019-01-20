package com.mux.livedemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.mux.libcamera.CamcorderBase;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener
{
    private final static String TAG = MainActivity.class.getSimpleName ( );

    @Override
    protected void onCreate ( Bundle savedInstanceState )
    {
        super.onCreate ( savedInstanceState );
        Log.d ( TAG , "#-> onCreate ( )" );

        setContentView ( R.layout.activity_main );

        checkPermission ( );

        // ================== [RECORD] ==================
        Button btn = findViewById ( R.id.recoButton );
        btn.setOnClickListener ( new View.OnClickListener ( )
        {
            @Override
            public void onClick ( View v )
            {
                try
                {
                    camera.startRecord ( MainActivity.this ,
                            "{key}" ,
                            MainActivity.this );
                    Button btn = findViewById ( R.id.recoButton );
                    btn.setEnabled ( false );
                    btn = findViewById ( R.id.stopButton );
                    btn.setEnabled ( true );
                }
                catch ( Exception e )
                {
                    e.printStackTrace ( );
                }
            }
        } );

        // ================== [STOP] ==================
        btn = findViewById ( R.id.stopButton );
        btn.setOnClickListener ( new View.OnClickListener ( )
        {
            @Override
            public void onClick ( View v )
            {

                Log.d ( TAG , "#-> btnStop.onClick ( )" );

                camera.stopRecord ( MainActivity.this );

                Button btn = findViewById ( R.id.recoButton );
                btn.setEnabled ( true );
                btn = findViewById ( R.id.stopButton );
                btn.setEnabled ( false );

                Log.d ( TAG , "<-# btnStop.onClick ( )" );
            }
        } );
        btn.setEnabled ( false );

        // ================== [CAPTURE] ==================
        btn = findViewById ( R.id.captureButton );
        btn.setOnClickListener ( new View.OnClickListener ( )
        {
            @Override
            public void onClick ( View v )
            {
                Log.d ( TAG , "#-> btnCapture.onClick ( )" );
                camera.takeSnapshot ( );
                Log.d ( TAG , "<-# btnCapture.onClick ( )" );
            }
        } );

        Log.d ( TAG , "<-# onCreate ( )" );
    }

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA ,
            Manifest.permission.RECORD_AUDIO ,
            Manifest.permission.WRITE_EXTERNAL_STORAGE ,
    };

    public void checkPermission ( )
    {
        boolean needPermission = false;
        boolean needDescription = false;
        for ( final String permission : VIDEO_PERMISSIONS )
        {
            if ( ActivityCompat.checkSelfPermission ( this , permission )
                    != PackageManager.PERMISSION_GRANTED )
            {
                needPermission = true;
                if ( ActivityCompat.shouldShowRequestPermissionRationale ( this , permission ) )
                {
                    needDescription = true;
                }
            }
        }

        if ( needPermission )
        {
            if ( needDescription )
            {
                AlertDialog alertDialog = new AlertDialog.Builder ( this ).create ( );
                alertDialog.setTitle ( "Permission Detail" );
                alertDialog.setMessage ( "The app needs the permissions to operate properly" );
                alertDialog.setButton ( AlertDialog.BUTTON_POSITIVE , "OK" ,
                        new DialogInterface.OnClickListener ( )
                        {
                            public void onClick ( DialogInterface dialog , int which )
                            {
                                ActivityCompat.requestPermissions ( MainActivity.this ,
                                        VIDEO_PERMISSIONS ,
                                        REQUEST_VIDEO_PERMISSIONS );
                            }
                        } );
                alertDialog.setButton ( AlertDialog.BUTTON_NEGATIVE , "Cancel" ,
                        new DialogInterface.OnClickListener ( )
                        {
                            public void onClick ( DialogInterface dialog , int which )
                            {
                                dialog.dismiss ( );
                            }
                        } );
            }
            else
            {
                ActivityCompat.requestPermissions ( this , VIDEO_PERMISSIONS ,
                        REQUEST_VIDEO_PERMISSIONS );
            }
        }
        else
        {
            openCamera ( );
        }
    }

    CamcorderBase camera;

    private void openCamera ( )
    {
        Log.d ( TAG , "#-> openCamera ( ) " );

        try
        {
            camera = CamcorderBase.CreateCamera ( 2 , this , 0 ,
                    new CamcorderBase.OnCameraOpenListener ( )
                    {
                        @Override
                        public void onOpened ( boolean result )
                        {
                            if ( !result )
                            {
                                camera = null;
                            }
                        }
                    } );


            FrameLayout container = findViewById ( R.id.camera );

            View preview = camera.getPreview ( );

            preview.setLayoutParams ( new FrameLayout.LayoutParams (
                    FrameLayout.LayoutParams.MATCH_PARENT ,
                    FrameLayout.LayoutParams.MATCH_PARENT ) );

            container.addView ( preview );

            List< Size > sizes = camera.getSupportedCaptureSizes ( );

            camera.setCaptureSizeIndex ( 1 );
        }
        catch ( Exception e )
        {
            e.printStackTrace ( );
        }

        Log.d ( TAG , "<-# openCamera ( ) " );
    }

    @Override
    public void onResume ( )
    {
        super.onResume ( );
        if ( camera != null )
        {
            camera.pauseRecord ( false );
        }
    }

    @Override
    public void onPause ( )
    {
        if ( camera != null )
        {
            camera.pauseRecord ( true );
        }
        super.onPause ( );
    }

    @Override
    public void onDestroy ( )
    {
        if ( camera != null )
        {
            camera.release ( this );
        }
        super.onDestroy ( );
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode , @NonNull String[] permissions ,
                                             @NonNull int[] grantResults )
    {


        AlertDialog alertDialog = new AlertDialog.Builder ( this ).create ( );
        alertDialog.setTitle ( "Error" );
        alertDialog.setMessage ( "Permission grant failed" );


        alertDialog.setButton ( AlertDialog.BUTTON_POSITIVE , "OK" ,
                new DialogInterface.OnClickListener ( )
                {
                    public void onClick ( DialogInterface dialog , int which )
                    {
                        dialog.dismiss ( );
                    }
                } );


        if ( requestCode == REQUEST_VIDEO_PERMISSIONS )
        {
            boolean showError = false;

            if ( grantResults.length == VIDEO_PERMISSIONS.length )
            {
                for ( int result : grantResults )
                {
                    if ( result != PackageManager.PERMISSION_GRANTED )
                    {
                        showError = true;
                        break;
                    }
                }
            }
            else
            {
                showError = true;
            }
            if ( showError )
            {
                alertDialog.show ( );
            }
            else
            {
                openCamera ( );
            }
        }
        else
        {
            super.onRequestPermissionsResult ( requestCode , permissions , grantResults );
        }
    }

    @Override
    public void onRtmpConnecting ( String msg )
    {

    }

    @Override
    public void onRtmpConnected ( String msg )
    {

    }

    @Override
    public void onRtmpVideoStreaming ( )
    {

    }

    @Override
    public void onRtmpAudioStreaming ( )
    {

    }

    @Override
    public void onRtmpStopped ( )
    {

    }

    @Override
    public void onRtmpDisconnected ( )
    {

    }

    @Override
    public void onRtmpVideoFpsChanged ( double fps )
    {

    }

    @Override
    public void onRtmpVideoBitrateChanged ( double bitrate )
    {

    }

    @Override
    public void onRtmpAudioBitrateChanged ( double bitrate )
    {

    }

    @Override
    public void onRtmpSocketException ( SocketException e )
    {

    }

    @Override
    public void onRtmpIOException ( IOException e )
    {

    }

    @Override
    public void onRtmpIllegalArgumentException ( IllegalArgumentException e )
    {

    }

    @Override
    public void onRtmpIllegalStateException ( IllegalStateException e )
    {

    }
}
