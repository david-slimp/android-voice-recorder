// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import android.media.*;
import java.io.*;

final class CompressedCapture {
    private final AudioRecord input;
    private final PcmSink sink;
    private final Runnable error;
    private final short[] buffer;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile Throwable failure;
    private Thread writer;
    CompressedCapture(android.content.Context context,File file,String format,Runnable error)throws IOException{
        if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission");
        this.error=error;
        int rate=format.equals("ogg")?48000:44100;
        int min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)throw new IOException("Unsupported PCM format");
        int size=Math.max(8192,min*2);
        input=new AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
        if(input.getState()!=AudioRecord.STATE_INITIALIZED){input.release();throw new IOException("Microphone unavailable");}
        buffer=new short[4096];
        try{sink=format.equals("flac")?new FlacSink(file):format.equals("mp3")?new Mp3Sink(file):new OggOpusSink(file);}
        catch(Exception e){input.release();throw new IOException("Encoder startup failed",e);}
    }
    @android.annotation.TargetApi(29)
    void observe(java.util.concurrent.Executor executor, AudioManager.AudioRecordingCallback callback) {
        input.registerAudioRecordingCallback(executor, callback);
    }
    void start(){
        if(running){paused=false;return;}
        if(failure!=null)throw new IllegalStateException("Previous audio write failed");
        input.startRecording();
        if(input.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("Microphone not recording");
        running=true;writer=new Thread(()->{
            try{
                while(running){
                    int n=input.read(buffer,0,buffer.length,AudioRecord.READ_BLOCKING);
                    if(n>0&&running&&!paused)sink.write(buffer,n);
                    else if(n<0&&running)throw new IOException("PCM read error "+n);
                }
            }catch(Throwable e){if(running){failure=e;running=false;error.run();}}
        },"compressed-writer");writer.start();
    }
    void pause(){paused=true;}
    private void stopCapture()throws IOException{
        running=false;
        if(input.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)input.stop();
        if(writer!=null){try{writer.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
            if(writer.isAlive())throw new IOException("Encoder writer did not stop");writer=null;}
    }
    void finish()throws IOException{try{stopCapture();sink.finish();if(failure!=null)throw new IOException("Encoder failed",failure);}finally{input.release();}}
    void abort(){try{stopCapture();input.release();sink.abort();}catch(Exception ignored){}}
}
