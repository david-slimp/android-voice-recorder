// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

public final class RecordingFiles extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    static Uri uri(File f){return new Uri.Builder().scheme("content").authority("com.rock808.voicerecorder.test.files").appendPath(f.getName()).build();}
    private File valid(Uri u) throws FileNotFoundException{
        if(u.getPathSegments().size()!=1)throw new FileNotFoundException();
        String n=u.getLastPathSegment();
        if(n==null||!n.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}\\.(m4a|wav|flac|mp3|ogg)(\\.bookmarks\\.csv)?"))throw new FileNotFoundException();
        File f=new File(RecordingService.directory(getContext()),n);
        if(!f.isFile())throw new FileNotFoundException();
        return f;
    }
    static String mime(String name){
        if(name==null)return "application/octet-stream";
        if(name.endsWith(".csv"))return "text/csv";
        if(name.endsWith(".wav"))return "audio/wav";
        if(name.endsWith(".mp3"))return "audio/mpeg";
        if(name.endsWith(".flac"))return "audio/flac";
        if(name.endsWith(".ogg"))return "audio/ogg";
        return "audio/mp4";
    }
    @Override public String getType(Uri u){return mime(u.getLastPathSegment());}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(valid(u),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri u,String[] p,String s,String[] args,String sort){
        try{File f=valid(u);String[] cols=p==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:p;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int j=0;j<cols.length;j++)row[j]=OpenableColumns.DISPLAY_NAME.equals(cols[j])?f.getName():OpenableColumns.SIZE.equals(cols[j])?f.length():null;c.addRow(row);return c;}catch(FileNotFoundException e){return null;}
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
