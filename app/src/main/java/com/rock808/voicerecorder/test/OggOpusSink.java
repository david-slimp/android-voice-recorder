// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.concentus.*;

// RFC 7845 Opus in Ogg, mono 48 kHz, 20ms frames. One packet per page.
// Maintain one pending packet so the last page can carry EOS and sample-exact final granule.
final class OggOpusSink implements PcmSink {
    private static final int FRAME=960;
    private final OpusEncoder encoder;
    private final BufferedOutputStream out;
    private final int serial=new Random().nextInt(), preSkip;
    private final short[] pcm=new short[FRAME];
    private final byte[] encoded=new byte[1275];
    private byte[] pending;
    private int filled,seq;
    private long actual,packets;
    private boolean closed;
    OggOpusSink(File file)throws IOException{
        try{
            encoder=new OpusEncoder(48000,1,OpusApplication.OPUS_APPLICATION_AUDIO);
            encoder.setBitrate(64000);encoder.setComplexity(5);
            preSkip=encoder.getLookahead();
        }catch(OpusException e){throw new IOException(e);}
        out=new BufferedOutputStream(new FileOutputStream(file),8192);
        ByteBuffer head=ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        head.put(new byte[]{'O','p','u','s','H','e','a','d'}).put((byte)1).put((byte)1).putShort((short)preSkip).putInt(48000).putShort((short)0).put((byte)0);
        page(head.array(),0,2);
        byte[] vendor="DBS-VocRec / Concentus".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer tags=ByteBuffer.allocate(16+vendor.length).order(ByteOrder.LITTLE_ENDIAN);
        tags.put(new byte[]{'O','p','u','s','T','a','g','s'}).putInt(vendor.length).put(vendor).putInt(0);
        page(tags.array(),0,0);
    }
    private void page(byte[] data,long gp,int flags)throws IOException{
        int segs=data.length/255+1;
        ByteBuffer buf=ByteBuffer.allocate(27+segs+data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[]{'O','g','g','S'}).put((byte)0).put((byte)flags).putLong(gp).putInt(serial).putInt(seq++).putInt(0).put((byte)segs);
        int rem=data.length;
        for(int i=0;i<segs;i++){int n=Math.min(255,rem);buf.put((byte)n);rem-=n;}
        buf.put(data);
        byte[] page=buf.array();int crc=0;
        for(byte b:page){crc^=(b&255)<<24;for(int k=0;k<8;k++)crc=(crc<<1)^((crc&0x80000000)!=0?0x04c11db7:0);}
        buf.putInt(22,crc);out.write(page);
    }
    private void frame()throws IOException{
        try{
            int n=encoder.encode(pcm,0,FRAME,encoded,0,encoded.length);
            if(pending!=null)page(pending,packets*FRAME,0);
            pending=Arrays.copyOf(encoded,n);packets++;
        }catch(OpusException e){throw new IOException(e);}
        filled=0;
    }
    public void write(short[] samples,int count)throws IOException{
        actual+=count;
        int pos=0;
        while(pos<count){int n=Math.min(FRAME-filled,count-pos);System.arraycopy(samples,pos,pcm,filled,n);filled+=n;pos+=n;if(filled==FRAME)frame();}
    }
    public void finish()throws IOException{
        if(closed)return;closed=true;
        try{
            // Add enough zero padding to compensate encoder delay, then use an exact end granule.
            long end=actual+preSkip;
            while(packets*FRAME<end||pending==null){Arrays.fill(pcm,filled,FRAME,(short)0);frame();}
            page(pending,end,4);
        }finally{out.close();}
    }
    public void abort(){try{finish();}catch(Exception ignored){}}
}
