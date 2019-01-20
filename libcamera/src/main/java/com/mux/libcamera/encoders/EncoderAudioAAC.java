package com.mux.libcamera.encoders;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EncoderAudioAAC extends Encoder {

    private final static String TAG = EncoderAudioAAC.class.getSimpleName();

    public final static int[] SupportedSampleRate = {
            96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000,
            7350,
    };

    public final static int[] SupportBitRate = {
            128000,
            64000,
            48000,
            32000,
            16000,
    };

    private AudioRecord audioRecord;

    public EncoderAudioAAC(int sampleRate, int profile, int bitRate) throws IOException {

        Log.e(TAG, "#-> EncoderAudioAAC ( sampleRate=" + sampleRate + " profile=" + profile + " bitRate=" + bitRate + ") ");

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                getPcmBufferSize(sampleRate));

        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);


        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {

            MediaCodecInfo curInfo = MediaCodecList.getCodecInfoAt(i);

            if (!curInfo.isEncoder())
                continue;

            String[] types = curInfo.getSupportedTypes();

            Log.e(TAG, "    EncoderAudioAAC ( ) codec=[" + curInfo.getName() + "] ");

            int j = 0;
            for (j = 0; j < types.length; j++) {
                Log.e(TAG, "                  " + types[j]);
                try {
                    MediaCodecInfo.CodecCapabilities cap = curInfo.getCapabilitiesForType(types[j]);
                    for (int x = 0; x < cap.profileLevels.length; x++) {
                        Log.e(TAG, "                    profile=" + cap.profileLevels[x].profile + " level=" + cap.profileLevels[x].level);
                    }
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }

            }

        }


        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        //mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);

        String encoderName = mcl.findEncoderForFormat(mediaFormat);

        Log.e(TAG, "    EncoderAudioAAC ( encoderName=" + encoderName + ") ");

        mediaCodec = MediaCodec.createByCodecName(encoderName);

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        bufferFormat = mediaCodec.getOutputFormat();

        Log.e(TAG, "<-# EncoderAudioAAC ( )");

    }

    private int getPcmBufferSize(int sampleRate) {
        int pcmBufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT) + 8191;
        return pcmBufSize - (pcmBufSize % 8192);
    }

    private boolean keepRunning = true;
    private MediaFormat bufferFormat;

    public void start() {
        audioRecord.startRecording();
        super.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                byte[] pcmBuffer = new byte[4096];
                while (keepRunning) {
                    int size = audioRecord.read(pcmBuffer, 0, pcmBuffer.length);
                    if (!pauseEncoding && size > 0) {
                        onSample(pcmBuffer, size);
                    }
                }
            }
        }).start();
    }

    @Override
    public void onSample(byte[] pcmBuffer, int size) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputBufferId = mediaCodec.dequeueInputBuffer(25000);
        if (inputBufferId >= 0) {
            ByteBuffer inputBuffers = mediaCodec.getInputBuffer(inputBufferId);
            if (inputBuffers != null) {
                inputBuffers.put(pcmBuffer, 0, size);
                mediaCodec.queueInputBuffer(inputBufferId, 0, size,
                        System.nanoTime() / 1000, 0);
            }
        }

        int outputBufferId = mediaCodec.dequeueOutputBuffer(info, 25000);
        if (outputBufferId >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(
                    outputBufferId);
            if (outputBuffer != null && sink != null) {
                sink.onSample(outputBuffer, bufferFormat, info);
            }
            mediaCodec.releaseOutputBuffer(outputBufferId, false);
        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // Subsequent data will conform to new format.
            bufferFormat = mediaCodec.getOutputFormat();
        }
    }

    public void stop() {
        keepRunning = false;
        audioRecord.stop();
        super.stop();
    }
}
