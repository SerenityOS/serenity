/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4498848
 * @summary Sound causes crashes on Linux (part 3)
 */
public class ClipLinuxCrash2 implements LineListener{
    Clip clip;
    int stopOccured;
    static final Object lock = new Object();

    public static long bytes2Ms(long bytes, AudioFormat format) {
        return (long) (bytes/format.getFrameRate()*1000/format.getFrameSize());
    }

    static int staticLen=1000;
    static boolean addLen=true;

    ClipLinuxCrash2() {
    }

    public void update(LineEvent e) {
        if (e.getType() == LineEvent.Type.STOP) {
            stopOccured++;
            out("  Test program: receives STOP event for clip="+clip.toString()+" no."+stopOccured);
            out("  Test program: Calling close() in event dispatcher thread");
            clip.close();
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        else if (e.getType() == LineEvent.Type.CLOSE) {
            out("  Test program: receives CLOSE event for "+clip.toString());
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        else if (e.getType() == LineEvent.Type.START) {
            out("  Test program: receives START event for "+clip.toString());
        }
        else if (e.getType() == LineEvent.Type.OPEN) {
            out("  Test program: receives OPEN event for "+clip.toString());
        }
    }

    public long start() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);

        if (addLen) {
            staticLen+=(int) (staticLen/5)+1000;
        } else {
            staticLen-=(int) (staticLen/5)+1000;
        }
        if (staticLen>8*44100*4) {
            staticLen = 8*44100*4;
            addLen=!addLen;
        }
        if (staticLen<1000) {
            staticLen = 1000;
            addLen=!addLen;
        }
        int len = staticLen;
        len -= (len % 4);
        out("  Test program: preparing to play back "+len+" bytes == "+bytes2Ms(len, format)+"ms audio...");

        byte[] fakedata=new byte[len];
        InputStream is = new ByteArrayInputStream(fakedata);
        AudioInputStream ais = new AudioInputStream(is, format, fakedata.length/format.getFrameSize());

        DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
        clip = (Clip) AudioSystem.getLine(info);
        clip.addLineListener(this);

        out("  Test program: opening clip="+((clip==null)?"null":clip.toString()));
        clip.open(ais);
        ais.close();
        out("  Test program: starting clip="+((clip==null)?"null":clip.toString()));
        clip.start();
        return bytes2Ms(fakedata.length, format);
    }

    public static void main(String[] args) throws Exception {
        if (!isSoundcardInstalled()) {
            return;
        }

        try {
            int COUNT=10;
            out();
            out("4498848 Sound causes crashes on Linux");
            if (args.length>0) {
                COUNT=Integer.parseInt(args[0]);
            }
            for (int i=0; i<COUNT; i++) {
                out("trial "+(i+1)+"/"+COUNT);
                ClipLinuxCrash2 t = new ClipLinuxCrash2();
                t.start();
                int waitTime = 300+(1300*(i % 2)); // every 2nd time wait 1600, rather than 300ms.
                out("  Test program: waiting for "+waitTime+" ms for audio playback to stop...");
                Thread.sleep(waitTime);
                out("  Test program: calling close() from main thread");
                t.clip.close();
                // let the subsystem enough time to actually close the soundcard
                //out("  Test program: waiting for 2 seconds...");
                //Thread.sleep(2000);
                //out();
            }
            out("  Test program: waiting for 1 second...");
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            out("  Test program: waiting for 1 second");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {}
            // do not fail if no audio device installed - bug 4742021
            if (!(e instanceof LineUnavailableException)) {
                throw e;
            }
        }
        out("Test passed");
    }

    static void out() {
        out("");
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
    }

    /**
     * Returns true if at least one soundcard is correctly installed
     * on the system.
     */
    public static boolean isSoundcardInstalled() {
        boolean result = false;
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers.length > 0) {
                result = AudioSystem.getSourceDataLine(null) != null;
            }
        } catch (Exception e) {
            System.err.println("Exception occured: "+e);
        }
        if (!result) {
            System.err.println("Soundcard does not exist or sound drivers not installed!");
            System.err.println("This test requires sound drivers for execution.");
        }
        return result;
    }

}
