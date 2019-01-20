package com.mux.libcamera.camera2;

import android.app.Activity;
import android.content.Context;
import android.media.MediaCodecInfo;
import android.util.Log;
import android.util.Size;
import android.view.View;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.mux.libcamera.CamcorderBase;
import com.mux.libcamera.SinkRtmp;
import com.mux.libcamera.encoders.Encoder;
import com.mux.libcamera.encoders.EncoderAudioAAC;
import com.mux.libcamera.encoders.EncoderVideoH264;

import java.io.IOException;

public class Camcorder extends CamcorderBase
{
    private final static String TAG = Camcorder.class.getSimpleName ( );

    private CameraTextureView cameraPreview;

    public Camcorder ( Context ctx , String cameraId , OnCameraOpenListener listener )
    {
        Log.d ( TAG , "#-> Camcorder ( cameraId" + cameraId + " )" );

        cameraPreview = new CameraTextureView ( ctx , cameraId , listener );

        Log.d ( TAG , "#-> Camcorder ( cameraId" + cameraId + " )" );
    }

    @Override
    public void release ( Activity ctx )
    {
        super.release ( ctx );
        cameraPreview.release ( );
    }

    @Override
    public View getPreview ( )
    {
        Log.d ( TAG , "    getPreview ( )" );
        return cameraPreview;
    }

    private Encoder.ISink mSink;

    @Override
    public void startRecord ( Activity activity , String streamKey , RtmpHandler.RtmpListener listener ) throws IOException
    {
        super.startRecord ( activity , streamKey , listener );
        Log.d ( TAG , "#-> startRecord ( streamKey" + streamKey + " )" );

        Size capturedSize = new Size ( 1280 , 720 )/*supportedCaptureSizes.get ( captureSizeIndex )*/;

        videoEncoder = new EncoderVideoH264 ( capturedSize , true );

        audioEncoder = new EncoderAudioAAC ( EncoderAudioAAC.SupportedSampleRate[ 7 ] ,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC ,
                EncoderAudioAAC.SupportBitRate[ 2 ] );

        mSink = new SinkRtmp ( "rtmp://a.rtmp.youtube.com/live2/" + streamKey , capturedSize , listener );

        videoEncoder.setSink ( mSink );
        audioEncoder.setSink ( mSink );

        audioEncoder.start ( );

        cameraPreview.startRecording ( videoEncoder );

        Log.d ( TAG , "<-# startRecord ( streamKey" + streamKey + " )" );
    }

    @Override
    public void stopRecord ( Activity activity )
    {
        super.stopRecord ( activity );
        Log.d ( TAG , "#-> stopRecord ( )" );
        cameraPreview.stop ( );
        audioEncoder.stop ( );
        mSink.close ( );
        Log.d ( TAG , "<-# stopRecord ( )" );
    }

    @Override
    public void takeSnapshot ( )
    {

    }

}
