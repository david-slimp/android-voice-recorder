// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import android.media.*;
import java.io.*;

final class WavCapture {
    static final int RATE=44100;
    private final AudioRecord input;
    private final RandomAccessFile output;
    private final Runnable onError;
    private final int bufferSize;
    private volatile boolean running;
    private volatile boolean paused;
    private Thread writer;
    private volatile Throwable failure;
    private long bytes;
    WavCapture(android.content.Context context,File file,Runnable onError) throws IOException {
        this.onError=onError;
        if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission");
        int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)throw new IOException("Unsupported PCM format");
        bufferSize=Math.max(8192,min*2);
        input=new AudioRecord(MediaRecorder.AudioSource.MIC,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSize);
        if(input.getState()!=AudioRecord.STATE_INITIALIZED){input.release();throw new IOException("Microphone unavailable");}
        try { output=new RandomAccessFile(file,"rw");output.setLength(0);header(0); }
        catch(IOException e){input.release();throw e;}
    }
    private void le(int n,int count)throws IOException{for(int i=0;i<count;i++)output.write((n >>> (i*8))&255);}
    private void header(long len)throws IOException{
        output.seek(0);output.writeBytes("RIFF");le((int)(36+len),4);output.writeBytes("WAVEfmt ");le(16,4);le(1,2);le(1,2);le(RATE,4);le(RATE*2,4);le(2,2);le(16,2);output.writeBytes("data");le((int)len,4);output.seek(44+len);
    }
    @android.annotation.TargetApi(29)
    void observe(java.util.concurrent.Executor executor, AudioManager.AudioRecordingCallback callback) {
        input.registerAudioRecordingCallback(executor, callback);
    }
    void start(){
        if(running){paused=false;return;}
        if(failure!=null)throw new IllegalStateException("Previous PCM write failed");
        input.startRecording();
        if(input.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("Microphone not recording");
        running=true;writer=new Thread(()->{
            byte[] buffer=new byte[bufferSize];
            try{
                while(running){
                    int n=input.read(buffer,0,buffer.length,AudioRecord.READ_BLOCKING);
                    if(n>0&&running&&!paused){
                        if(bytes+n>0xfffffff0L-36)throw new IOException("WAV size limit reached");
                        output.write(buffer,0,n);bytes+=n;
                    }else if(n<0&&running)throw new IOException("PCM read error "+n);
                }
            }catch(Throwable e){if(running){failure=e;running=false;onError.run();}}
        },"wav-writer");writer.start();
    }
    void pause() {paused=true;}
    private void stopCapture() throws IOException {
        running=false;
        if(input.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING) input.stop();
        if(writer!=null){try{writer.join(2000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}if(writer.isAlive())throw new IOException("PCM writer did not stop");writer=null;}
        header(bytes);
    }
    void finish() throws IOException {try{stopCapture();}finally{input.release();output.close();}}
    void abort(){try{finish();}catch(Exception ignored){}}
}
