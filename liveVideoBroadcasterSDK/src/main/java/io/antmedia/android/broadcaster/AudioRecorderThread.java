package io.antmedia.android.broadcaster;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Message;
import android.util.Log;

import java.util.Arrays;

import io.antmedia.android.broadcaster.encoder.AudioHandler;

/**
 * Created by mekya on 28/03/2017.
 */

class AudioRecorderThread extends Thread {

    private static final String TAG = AudioRecorderThread.class.getSimpleName();
    private final int mSampleRate;
    private final long startTime;
    private boolean isAudioEnabled;
    private volatile boolean stopThread = false;

    private android.media.AudioRecord audioRecord;
    private AudioHandler audioHandler;

    public AudioRecorderThread(int sampleRate, long recordStartTime, AudioHandler audioHandler,boolean isAudioEnabled) {
        this.mSampleRate = sampleRate;
        this.startTime = recordStartTime;
        this.audioHandler = audioHandler;
        this.isAudioEnabled = isAudioEnabled;
    }


    @Override
    public void run() {
        //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = android.media.AudioRecord
                .getMinBufferSize(mSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
        byte[][] audioData;
        int bufferReadResult;

        audioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        // divide byte buffersize to 2 to make it short buffer
        audioData = new byte[1000][bufferSize];

        audioRecord.startRecording();

        int i = 0;
        byte[] data;
        while ((bufferReadResult = audioRecord.read(audioData[i], 0, audioData[i].length)) > 0) {
                data = audioData[i];
                Message msg = Message.obtain(audioHandler, AudioHandler.RECORD_AUDIO,isAudioEnabled?data:new byte[data.length]);
                msg.arg1 = bufferReadResult;
                msg.arg2 = (int) (System.currentTimeMillis() - startTime);
                audioHandler.sendMessage(msg);
            i++;
            if (i == 1000) {
                i = 0;
            }
            if (stopThread) {
                break;
            }
        }

        Log.d(TAG, "AudioThread Finished, release audioRecord");

    }

    public void  setAudioEnabled(boolean audioEnabled){
        isAudioEnabled=audioEnabled;
    }

    public void stopAudioRecording() {

        if (audioRecord != null && audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
            stopThread = true;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
}
