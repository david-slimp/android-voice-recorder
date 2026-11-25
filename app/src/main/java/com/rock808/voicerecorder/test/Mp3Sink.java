// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import java.io.*;
import net.sourceforge.lame.mp3.*;

final class Mp3Sink implements PcmSink {
    private final Lame lame=new Lame();
    private final BufferedOutputStream out;
    private final byte[] encoded=new byte[24000];
    private final float[] pcm=new float[4096];
    private final float[] unused=new float[4096];
    private boolean closed;
    Mp3Sink(File file) throws IOException {
        LameGlobalFlags g=lame.getFlags();
        g.setInNumChannels(1);g.setInSampleRate(44100);g.setOutSampleRate(44100);
        g.setMode(MPEGMode.MONO);g.setBitRate(128);g.setQuality(5);
        lame.getId3().init(g);g.setWriteId3tagAutomatic(false);g.setFindReplayGain(false);
        if(lame.initParams()<0||g.getOutSampleRate()!=44100||g.getBitRate()!=128){lame.close();throw new IOException("MP3 parameters unsupported");}
        // No Java Sound API dependency. Stream encoded frames as they are produced.
        out=new BufferedOutputStream(new FileOutputStream(file),8192);
    }
    public void write(short[] samples,int count)throws IOException{
        for(int pos=0;pos<count;){
            int n=Math.min(pcm.length,count-pos);
            for(int j=0;j<n;j++)pcm[j]=(float)(samples[pos+j]<<16);
            int written=lame.encodeBuffer(pcm,unused,n,encoded);
            if(written<0)throw new IOException("MP3 encoder error "+written);
            out.write(encoded,0,written);pos+=n;
        }
    }
    public void finish()throws IOException{
        if(closed)return;closed=true;
        try {int n=lame.encodeFlush(encoded);if(n<0)throw new IOException("MP3 flush failed");out.write(encoded,0,n);}
        finally {try{lame.close();}finally{out.close();}}
    }
    public void abort(){try{finish();}catch(Exception ignored){}}
}
