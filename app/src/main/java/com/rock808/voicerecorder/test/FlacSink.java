// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import android.media.*;
import java.io.*;
import java.nio.*;

final class FlacSink implements PcmSink {
    private final MediaCodec codec;
    private final RandomAccessFile out;
    private final MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
    private final byte[] chunk=new byte[32768];
    private long samples;
    private boolean header,closed;
    FlacSink(File file)throws IOException{
        out=new RandomAccessFile(file,"rw");out.setLength(0);
        MediaCodec c=null;
        try{
            c=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_FLAC);
            MediaFormat f=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_FLAC,44100,1);
            f.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
            f.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL,3);
            c.configure(f,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);c.start();
        }catch(Exception e){if(c!=null)c.release();out.close();throw new IOException("FLAC encoder unavailable",e);}
        codec=c;
    }
    private void header(ByteBuffer b)throws IOException{
        if(header||b==null||b.remaining()<4)return;
        ByteBuffer dup=b.duplicate();
        if(dup.get()!='f'||dup.get()!='L'||dup.get()!='a'||dup.get()!='C')throw new IOException("Invalid FLAC codec header");
        if(b.remaining()<42)throw new IOException("Incomplete FLAC STREAMINFO");
        copy(b);header=true;
    }
    private void copy(ByteBuffer b)throws IOException{
        while(b.hasRemaining()){int n=Math.min(b.remaining(),chunk.length);b.get(chunk,0,n);out.write(chunk,0,n);}
    }
    private boolean drain(long timeout)throws IOException{
        int idx=codec.dequeueOutputBuffer(info,timeout);
        if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            ByteBuffer csd=codec.getOutputFormat().getByteBuffer("csd-0");
            if(csd!=null)header(csd.duplicate());
            return false;
        }
        if(idx<0)return false;
        try{
            ByteBuffer b=codec.getOutputBuffer(idx);
            if(b!=null&&info.size>0){
                b.position(info.offset);b.limit(info.offset+info.size);
                if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)header(b);
                else{if(!header)throw new IOException("FLAC data before STREAMINFO");copy(b);}
            }
            return (info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
        }finally{codec.releaseOutputBuffer(idx,false);}
    }
    private int input()throws IOException{
        long deadline=System.nanoTime()+5_000_000_000L;
        int i;
        while((i=codec.dequeueInputBuffer(10000))<0){drain(10000);if(System.nanoTime()>deadline)throw new IOException("FLAC input timed out");}
        return i;
    }
    public void write(short[] pcm,int count)throws IOException{
        for(int pos=0;pos<count;){
            int i=input();ByteBuffer b=codec.getInputBuffer(i);if(b==null)throw new IOException("Missing FLAC input buffer");
            b.clear();b.order(ByteOrder.LITTLE_ENDIAN);int n=Math.min(count-pos,b.remaining()/2);
            if(n<1)throw new IOException("Empty FLAC buffer");
            for(int j=0;j<n;j++)b.putShort(pcm[pos+j]);
            codec.queueInputBuffer(i,0,n*2,samples*1000000L/44100,0);samples+=n;pos+=n;
            drain(0);
        }
    }
    public void finish()throws IOException{
        if(closed)return;closed=true;
        try {
            int i=input();codec.queueInputBuffer(i,0,0,samples*1000000L/44100,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            long deadline=System.nanoTime()+5_000_000_000L;
            while(!drain(10000))if(System.nanoTime()>deadline)throw new IOException("FLAC output timed out");
            if(!header)throw new IOException("Missing FLAC stream header");
            // STREAMINFO total samples (36 bits), retain sample rate/channel/depth.
            out.seek(18);long spec=out.readLong();out.seek(18);out.writeLong((spec&~0xfffffffffL)|(samples&0xfffffffffL));
        }finally{try{codec.stop();}finally{try{codec.release();}finally{out.close();}}}
    }
    public void abort(){try{finish();}catch(Exception ignored){}}
}
