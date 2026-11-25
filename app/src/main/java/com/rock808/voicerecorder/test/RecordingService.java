// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;

import android.app.*;
import android.content.*;
import android.media.MediaRecorder;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RecordingService extends Service {
    public static final String START="start", PAUSE="pause", RESUME="resume", STOP="stop", MARK="mark";
    static final int NOTIF=42;
    static final String CHANNEL="recording";
    static volatile String state="idle", message="Ready";
    static volatile long accumulated=0, started=0;
    static volatile int marks=0;
    static volatile File activeFile;
    static volatile String format="m4a";
    private WavCapture wav;
    private CompressedCapture compressed;
    private MediaRecorder recorder;
    private PowerManager.WakeLock wakeLock;
    private final ArrayList<Long> bookmarks=new ArrayList<>();
    static File directory(Context c) {
        File dir=c.getExternalFilesDir("recordings");
        if(dir==null) dir=new File(c.getFilesDir(),"recordings");
        if(!dir.exists()) dir.mkdirs();
        return dir;
    }
    static long elapsed() {
        return accumulated + (state.equals("recording") ? SystemClock.elapsedRealtime()-started : 0);
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Active recording",NotificationManager.IMPORTANCE_LOW));
    }
    private PendingIntent action(String a,int id){
        return PendingIntent.getService(this,id,new Intent(this,RecordingService.class).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification(){
        PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        boolean paused=state.equals("paused");
        b.setSmallIcon(R.drawable.ic_record).setContentTitle("DBS-VocRec • "+(paused?"Paused":"Recording")).setContentText("Tap to open. Recording stays local.").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
         .setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_SERVICE)
         .addAction(0,paused?"Resume":"Pause",action(paused?RESUME:PAUSE,2))
         .addAction(0,"Stop",action(STOP,3));
        if(!paused) b.setUsesChronometer(true).setWhen(System.currentTimeMillis()-elapsed()).setShowWhen(true);
        return b.build();
    }
    private void publish(){getSystemService(NotificationManager.class).notify(NOTIF,notification());}
    private void takeWake(){
        if(wakeLock==null){wakeLock=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"DBSVocRec:Recording");wakeLock.setReferenceCounted(false);}
        if(!wakeLock.isHeld()) wakeLock.acquire(12*60*60*1000L);
    }
    private void releaseWake(){if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i==null)return START_NOT_STICKY;
        String a=i.getAction();
        try {
            if(START.equals(a)&&state.equals("idle")) { String requested=i.getStringExtra("format");format=Arrays.asList("wav","flac","mp3","ogg").contains(requested)?requested:"m4a"; begin(); }
            else if(PAUSE.equals(a)&&state.equals("recording")) {if(wav!=null)wav.pause();else if(compressed!=null)compressed.pause();else recorder.pause();accumulated=elapsed();state="paused";releaseWake();publish();}
            else if(RESUME.equals(a)&&state.equals("paused")) {takeWake();if(wav!=null)wav.start();else if(compressed!=null)compressed.start();else recorder.resume();started=SystemClock.elapsedRealtime();state="recording";publish();}
            else if(MARK.equals(a)&&(!state.equals("idle"))){bookmarks.add(elapsed());marks=bookmarks.size();message="Bookmark "+marks+" added";}
            else if(STOP.equals(a)&&(!state.equals("idle"))) finish();
            else if(state.equals("idle")) stopSelf();
        } catch(Exception e){message="Recording error: "+e.getClass().getSimpleName();cleanup();stopForeground(true);stopSelf();}
        return START_NOT_STICKY;
    }
    private void begin() throws IOException {
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED) throw new SecurityException("Microphone permission");
        accumulated=0;started=SystemClock.elapsedRealtime();marks=0;bookmarks.clear();message="Recording";state="recording";
        if(Build.VERSION.SDK_INT>=29) startForeground(NOTIF,notification(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(NOTIF,notification());
        File folder=directory(this);
        if(folder.getUsableSpace()<10*1024*1024) throw new IOException("Insufficient storage");
        String stamp=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS",Locale.US).format(new Date());
        activeFile=new File(folder,stamp+".part."+format);
        if(format.equals("wav")){
            wav=new WavCapture(this,activeFile,()->new Handler(Looper.getMainLooper()).post(()->{message="PCM recording interrupted; partial file retained";cleanup();stopForeground(true);stopSelf();}));
            takeWake();wav.start();started=SystemClock.elapsedRealtime();publish();return;
        }
        if(!format.equals("m4a")){
            compressed=new CompressedCapture(this,activeFile,format,()->new Handler(Looper.getMainLooper()).post(()->{message="Encoding interrupted; partial file retained";cleanup();stopForeground(true);stopSelf();}));
            takeWake();compressed.start();started=SystemClock.elapsedRealtime();publish();return;
        }
        recorder=new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioChannels(1);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(96000);
        recorder.setOutputFile(activeFile.getPath());
        recorder.setOnErrorListener((r,what,extra)->{message="Recording interrupted; partial file retained";cleanup();stopForeground(true);stopSelf();});
        recorder.prepare();takeWake();recorder.start();started=SystemClock.elapsedRealtime();publish();
    }
    private void finish() throws IOException {
        long duration=elapsed();
        File part=activeFile;
        if(wav!=null){WavCapture capture=wav;wav=null;capture.finish();}else if(compressed!=null){CompressedCapture capture=compressed;compressed=null;capture.finish();}else{recorder.stop();recorder.release();recorder=null;}
        if(part==null||!part.exists()) throw new IOException("Missing recording");
        File saved=new File(part.getParentFile(),part.getName().replace(".part.","."));
        if(!part.renameTo(saved))throw new IOException("Couldn't finalize file");
        if(!bookmarks.isEmpty()){
            try(PrintWriter out=new PrintWriter(new File(saved.getParentFile(),saved.getName()+".bookmarks.csv"))){
                out.println("elapsed_ms");
                for(long m:bookmarks)out.println(m);
            }
        }
        message="Saved "+saved.getName();
        cleanup();accumulated=duration;stopForeground(true);stopSelf();
    }
    private void cleanup(){
        if(wav!=null){wav.abort();wav=null;}
        if(compressed!=null){compressed.abort();compressed=null;}
        if(recorder!=null){try{recorder.release();}catch(Exception ignored){}recorder=null;}
        releaseWake();state="idle";activeFile=null;marks=0;
    }
    @Override public void onDestroy(){cleanup();super.onDestroy();}
}
