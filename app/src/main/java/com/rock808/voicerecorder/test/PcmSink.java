// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 David Slimp
package com.rock808.voicerecorder.test;
import java.io.IOException;
interface PcmSink {
    void write(short[] samples,int count) throws IOException;
    void finish() throws IOException;
    void abort();
}
