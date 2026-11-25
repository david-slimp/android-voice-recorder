// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.media.MediaPlayer;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private TextView title,timer,status,info;
    private View marker;
    private Button start,pause,stop,bookmark,library,formatButton;
    private String selectedFormat="m4a";
    private static final String[] FORMATS={"m4a","wav","flac","mp3","ogg"};
    private static final String[] LABELS={"AAC / M4A (96 kbps)","WAV (lossless PCM)","FLAC (lossless)","MP3 (128 kbps)","Ogg Opus (64 kbps)"};
    private LinearLayout layout;
    private Handler handler=new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private boolean showingLibrary;
    private String screen="recorder";
    private final Runnable tick=new Runnable(){public void run(){if(!showingLibrary){update();if(!RecordingService.state.equals("idle"))handler.postDelayed(this,1000);}}};
    private int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(dp(4),dp(3),dp(4),dp(3));return t;}
    private Button button(String s,View.OnClickListener action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setOnClickListener(action);return b;}
    private void command(String a){Intent i=new Intent(this,RecordingService.class).setAction(a).putExtra("format",selectedFormat);
        if(RecordingService.START.equals(a)&&Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        handler.removeCallbacks(tick);handler.postDelayed(tick,350);
    }
    @Override public void onCreate(Bundle state){super.onCreate(state);selectedFormat=getPreferences(MODE_PRIVATE).getString("format","m4a");getWindow().setStatusBarColor(Color.rgb(16,18,22));getWindow().setNavigationBarColor(Color.rgb(16,18,22));showRecorder();}
    private void base(){
        layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(12),dp(12),dp(12),dp(12));layout.setBackgroundColor(Color.rgb(16,18,22));layout.setGravity(Gravity.CENTER_HORIZONTAL);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(layout);setContentView(scroll);
    }
    private TextView header(String heading){
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton menu=new ImageButton(this);menu.setImageResource(R.drawable.ic_menu);menu.setContentDescription("Open menu");
        menu.setPadding(dp(12),dp(12),dp(12),dp(12));
        android.util.TypedValue bg=new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,bg,true);
        if(bg.resourceId!=0)menu.setBackgroundResource(bg.resourceId);
        bar.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView headingView=label(heading,22);
        bar.addView(headingView,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(new View(this),new LinearLayout.LayoutParams(dp(48),dp(48)));
        layout.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        menu.setOnClickListener(v->{
            PopupMenu popup=new PopupMenu(this,menu);
            popup.getMenu().add(0,1,0,"Recorder");
            popup.getMenu().add(0,2,1,"About");
            popup.setOnMenuItemClickListener(item->{
                releasePlayer();
                if(item.getItemId()==1){showRecorder();return true;}
                if(item.getItemId()==2){showAbout();return true;}
                return false;
            });
            popup.show();
        });
        return headingView;
    }
    private String version(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(PackageManager.NameNotFoundException ignored){return "Unknown";}
    }
    private void showAbout(){
        handler.removeCallbacks(tick);releasePlayer();showingLibrary=true;screen="about";base();header("About");
        layout.addView(label("DBS-VocRec",24));
        layout.addView(label("Version "+version(),16));
        layout.addView(label("Created by David Slimp / rock808",16));
        layout.addView(label("A lightweight, offline-first audio recorder for Android. Record in AAC, WAV, FLAC, MP3 or Ogg/Opus, add timestamp bookmarks, and share your own files.",15));
        layout.addView(label("No ads. No tracking. No internet permission. Recording starts only when you choose Record, with a visible ongoing notification.",15));
        layout.addView(label("Copyright © 2026 David Slimp.\nGNU AGPL-3.0 — own application code.\nThird-party codecs retain their respective licenses.",14));
        layout.addView(button("GNU AGPL-3.0 license",v->showLicense("AGPL-3.0.txt","GNU Affero GPL v3")));
        layout.addView(button("Java LAME license",v->showLicense("java-lame-LGPL.txt","Java LAME — LGPL")));
        layout.addView(button("GNU GPL v3 (LGPL terms)",v->showLicense("GPL-3.0.txt","GNU GPL v3")));
        layout.addView(button("Concentus license",v->showLicense("concentus-BSD.txt","Concentus — BSD")));
        layout.addView(button("Back to recorder",v->showRecorder()));
    }
    private void showLicense(String name,String heading){
        screen="license";showingLibrary=true;base();header(heading);
        try(InputStream in=getAssets().open("licenses/"+name);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)buffer.write(b,0,n);
            TextView body=label(buffer.toString("UTF-8"),13);body.setGravity(Gravity.START);body.setTextIsSelectable(true);layout.addView(body);
        }catch(IOException e){layout.addView(label("License text unavailable.",16));}
        layout.addView(button("Back to About",v->showAbout()));
    }
    private void showRecorder(){
        showingLibrary=false;screen="recorder";base();title=header("DBS-VocRec");
        timer=label("00:00:00",32);layout.addView(timer);
        LinearLayout stateRow=new LinearLayout(this);stateRow.setGravity(Gravity.CENTER);
        marker=new View(this);
        android.graphics.drawable.GradientDrawable dot=new android.graphics.drawable.GradientDrawable();dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);dot.setColor(Color.rgb(255,45,65));marker.setBackground(dot);
        LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(dp(8),dp(8));dotParams.rightMargin=dp(5);stateRow.addView(marker,dotParams);
        status=label("Ready",16);stateRow.addView(status);layout.addView(stateRow);
        info=label("",13);layout.addView(info);
        formatButton=button("Format",v->new AlertDialog.Builder(this).setTitle("Recording format").setSingleChoiceItems(LABELS,Math.max(0,Arrays.asList(FORMATS).indexOf(selectedFormat)),(d,idx)->{selectedFormat=FORMATS[idx];getPreferences(MODE_PRIVATE).edit().putString("format",selectedFormat).apply();d.dismiss();update();}).setNegativeButton(android.R.string.cancel,null).show());layout.addView(formatButton,new LinearLayout.LayoutParams(-1,dp(44)));
        start=button("Record",v->requestStart());layout.addView(start,new LinearLayout.LayoutParams(-1,dp(52)));
        pause=button("Pause",v->command(RecordingService.state.equals("paused")?RecordingService.RESUME:RecordingService.PAUSE));
        stop=button("Stop & save",v->command(RecordingService.STOP));
        LinearLayout controls=new LinearLayout(this);controls.addView(pause,new LinearLayout.LayoutParams(0,dp(52),1));controls.addView(stop,new LinearLayout.LayoutParams(0,dp(52),1));layout.addView(controls);
        bookmark=button("Bookmark",v->command(RecordingService.MARK));layout.addView(bookmark,new LinearLayout.LayoutParams(-1,dp(52)));
        library=button("Recordings",v->showLibrary());layout.addView(library,new LinearLayout.LayoutParams(-1,dp(52)));
        update();
        if(!RecordingService.state.equals("idle")){handler.removeCallbacks(tick);handler.postDelayed(tick,1000);}
    }
    private void requestStart(){
        if(!RecordingService.state.equals("idle"))return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},10);return;}
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}
        command(RecordingService.START);
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)requestStart();else Toast.makeText(this,"Recording requires microphone and visible notification permission.",Toast.LENGTH_LONG).show();}
    private String time(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
    private void update(){
        if(showingLibrary)return;
        boolean active=!RecordingService.state.equals("idle"), paused=RecordingService.state.equals("paused");
        marker.setVisibility(active&&!paused?View.VISIBLE:View.INVISIBLE);
        timer.setText(time(RecordingService.elapsed()));
        status.setText(active?(paused?"Paused":"Recording"):RecordingService.message.startsWith("Saved ")? "Saved":RecordingService.message);
        File f=RecordingService.activeFile;
        long bytes=f==null?0:f.length();
        String audioFormat=active?RecordingService.format:selectedFormat;
        formatButton.setText("Format: "+LABELS[Math.max(0,Arrays.asList(FORMATS).indexOf(audioFormat))]);
        formatButton.setEnabled(!active);
        info.setText(String.format(Locale.US,"%s • Mono\n%.2f MB • %d bookmarks\n%.1f GB free",audioFormat.equals("wav")||audioFormat.equals("flac")?"44.1 kHz / 16-bit":audioFormat.equals("mp3")?"44.1 kHz / 128 kbps":audioFormat.equals("ogg")?"48 kHz / 64 kbps":"96 kbps",bytes/1e6,RecordingService.marks,RecordingService.directory(this).getUsableSpace()/1e9));
        start.setEnabled(!active);pause.setEnabled(active);pause.setText(paused?"Resume":"Pause");stop.setEnabled(active);bookmark.setEnabled(active);library.setEnabled(!active);
    }
    private void showLibrary(){
        showingLibrary=true;screen="library";base();header("Recordings");layout.addView(button("Back to recorder",v->{releasePlayer();showRecorder();}));
        File[] files=RecordingService.directory(this).listFiles(f->(f.getName().matches(".*\\.(m4a|wav|flac|mp3|ogg)"))&&!f.getName().contains(".part."));
        if(files==null||files.length==0){layout.addView(label("No saved recordings yet.",18));return;}
        Arrays.sort(files,Comparator.comparing(File::getName).reversed());
        for(File f:files){
            layout.addView(label(f.getName()+"\n"+String.format(Locale.US,"%.2f MB",f.length()/1e6),16));
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(button("Play",v->play(f)),new LinearLayout.LayoutParams(0,dp(52),1));
            row.addView(button("Stop playback",v->releasePlayer()),new LinearLayout.LayoutParams(0,dp(52),1));
            row.addView(button("Share",v->share(f)),new LinearLayout.LayoutParams(0,dp(52),1));layout.addView(row);
            File marks=new File(f.getParentFile(),f.getName()+".bookmarks.csv");
            if(marks.isFile()) layout.addView(button("Share bookmarks (CSV)",v->share(marks)));
        }
    }
    private void play(File f){releasePlayer();try{player=new MediaPlayer();player.setDataSource(f.getPath());player.setOnCompletionListener(p->releasePlayer());player.prepare();player.start();}catch(Exception e){releasePlayer();Toast.makeText(this,"Can't play file",Toast.LENGTH_SHORT).show();}}
    private void releasePlayer(){if(player!=null){player.release();player=null;}}
    private void share(File f){android.net.Uri uri=RecordingFiles.uri(f);Intent i=new Intent(Intent.ACTION_SEND).setType(RecordingFiles.mime(f.getName())).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri(f.getName(),uri));startActivity(Intent.createChooser(i,"Share recording"));}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){handler.removeCallbacks(tick);releasePlayer();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);releasePlayer();super.onDestroy();}
    @Override public void onBackPressed(){if(screen.equals("license")){showAbout();}else if(showingLibrary){releasePlayer();showRecorder();}else super.onBackPressed();}
}
