/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.media.sound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

// IDEA:
// Use java.util.concurrent.Semaphore,
// java.util.concurrent.locks.ReentrantLock and other new classes/methods
// to improve this class's thread safety.

/**
 * A Mixer which provides direct access to audio devices.
 *
 * @author Florian Bomers
 */
final class DirectAudioDevice extends AbstractMixer {

    private static final int CLIP_BUFFER_TIME = 1000; // in milliseconds

    private static final int DEFAULT_LINE_BUFFER_TIME = 500; // in milliseconds

    DirectAudioDevice(DirectAudioDeviceProvider.DirectAudioDeviceInfo portMixerInfo) {
        // pass in Line.Info, mixer, controls
        super(portMixerInfo,              // Mixer.Info
              null,                       // Control[]
              null,                       // Line.Info[] sourceLineInfo
              null);                      // Line.Info[] targetLineInfo
        // source lines
        DirectDLI srcLineInfo = createDataLineInfo(true);
        if (srcLineInfo != null) {
            sourceLineInfo = new Line.Info[2];
            // SourcedataLine
            sourceLineInfo[0] = srcLineInfo;
            // Clip
            sourceLineInfo[1] = new DirectDLI(Clip.class, srcLineInfo.getFormats(),
                                              srcLineInfo.getHardwareFormats(),
                                              32, // arbitrary minimum buffer size
                                              AudioSystem.NOT_SPECIFIED);
        } else {
            sourceLineInfo = new Line.Info[0];
        }

        // TargetDataLine
        DataLine.Info dstLineInfo = createDataLineInfo(false);
        if (dstLineInfo != null) {
            targetLineInfo = new Line.Info[1];
            targetLineInfo[0] = dstLineInfo;
        } else {
            targetLineInfo = new Line.Info[0];
        }
    }

    private DirectDLI createDataLineInfo(boolean isSource) {
        Vector<AudioFormat> formats = new Vector<>();
        AudioFormat[] hardwareFormatArray = null;
        AudioFormat[] formatArray = null;

        synchronized(formats) {
            nGetFormats(getMixerIndex(), getDeviceID(),
                        isSource /* true:SourceDataLine/Clip, false:TargetDataLine */,
                        formats);
            if (formats.size() > 0) {
                int size = formats.size();
                int formatArraySize = size;
                hardwareFormatArray = new AudioFormat[size];
                for (int i = 0; i < size; i++) {
                    AudioFormat format = formats.elementAt(i);
                    hardwareFormatArray[i] = format;
                    int bits = format.getSampleSizeInBits();
                    boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
                    boolean isUnsigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
                    if ((isSigned || isUnsigned)) {
                        // will insert a magically converted format here
                        formatArraySize++;
                    }
                }
                formatArray = new AudioFormat[formatArraySize];
                int formatArrayIndex = 0;
                for (int i = 0; i < size; i++) {
                    AudioFormat format = hardwareFormatArray[i];
                    formatArray[formatArrayIndex++] = format;
                    int bits = format.getSampleSizeInBits();
                    boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
                    boolean isUnsigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
                    // add convenience formats (automatic conversion)
                    if (bits == 8) {
                        // add the other signed'ness for 8-bit
                        if (isSigned) {
                            formatArray[formatArrayIndex++] =
                                new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                    format.getSampleRate(), bits, format.getChannels(),
                                    format.getFrameSize(), format.getSampleRate(),
                                    format.isBigEndian());
                        }
                        else if (isUnsigned) {
                            formatArray[formatArrayIndex++] =
                                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                    format.getSampleRate(), bits, format.getChannels(),
                                    format.getFrameSize(), format.getSampleRate(),
                                    format.isBigEndian());
                        }
                    } else if (bits > 8 && (isSigned || isUnsigned)) {
                        // add the other endian'ness for more than 8-bit
                        formatArray[formatArrayIndex++] =
                            new AudioFormat(format.getEncoding(),
                                              format.getSampleRate(), bits,
                                              format.getChannels(),
                                              format.getFrameSize(),
                                              format.getSampleRate(),
                                              !format.isBigEndian());
                    }
                    //System.out.println("Adding "+v.get(v.size()-1));
                }
            }
        }
        // todo: find out more about the buffer size ?
        if (formatArray != null) {
            return new DirectDLI(isSource?SourceDataLine.class:TargetDataLine.class,
                                 formatArray, hardwareFormatArray,
                                 32, // arbitrary minimum buffer size
                                 AudioSystem.NOT_SPECIFIED);
        }
        return null;
    }

    // ABSTRACT MIXER: ABSTRACT METHOD IMPLEMENTATIONS

    @Override
    public Line getLine(Line.Info info) throws LineUnavailableException {
        Line.Info fullInfo = getLineInfo(info);
        if (fullInfo == null) {
            throw new IllegalArgumentException("Line unsupported: " + info);
        }
        if (fullInfo instanceof DataLine.Info) {

            DataLine.Info dataLineInfo = (DataLine.Info)fullInfo;
            AudioFormat lineFormat;
            int lineBufferSize = AudioSystem.NOT_SPECIFIED;

            // if a format is specified by the info class passed in, use it.
            // otherwise use a format from fullInfo.

            AudioFormat[] supportedFormats = null;

            if (info instanceof DataLine.Info) {
                supportedFormats = ((DataLine.Info)info).getFormats();
                lineBufferSize = ((DataLine.Info)info).getMaxBufferSize();
            }

            if ((supportedFormats == null) || (supportedFormats.length == 0)) {
                // use the default format
                lineFormat = null;
            } else {
                // use the last format specified in the line.info object passed
                // in by the app
                lineFormat = supportedFormats[supportedFormats.length-1];

                // if something is not specified, use default format
                if (!Toolkit.isFullySpecifiedPCMFormat(lineFormat)) {
                    lineFormat = null;
                }
            }

            if (dataLineInfo.getLineClass().isAssignableFrom(DirectSDL.class)) {
                return new DirectSDL(dataLineInfo, lineFormat, lineBufferSize, this);
            }
            if (dataLineInfo.getLineClass().isAssignableFrom(DirectClip.class)) {
                return new DirectClip(dataLineInfo, lineFormat, lineBufferSize, this);
            }
            if (dataLineInfo.getLineClass().isAssignableFrom(DirectTDL.class)) {
                return new DirectTDL(dataLineInfo, lineFormat, lineBufferSize, this);
            }
        }
        throw new IllegalArgumentException("Line unsupported: " + info);
    }

    @Override
    public int getMaxLines(Line.Info info) {
        Line.Info fullInfo = getLineInfo(info);

        // if it's not supported at all, return 0.
        if (fullInfo == null) {
            return 0;
        }

        if (fullInfo instanceof DataLine.Info) {
            // DirectAudioDevices should mix !
            return getMaxSimulLines();
        }

        return 0;
    }

    @Override
    protected void implOpen() throws LineUnavailableException {
    }

    @Override
    protected void implClose() {
    }

    @Override
    protected void implStart() {
    }

    @Override
    protected void implStop() {
    }

    int getMixerIndex() {
        return ((DirectAudioDeviceProvider.DirectAudioDeviceInfo) getMixerInfo()).getIndex();
    }

    int getDeviceID() {
        return ((DirectAudioDeviceProvider.DirectAudioDeviceInfo) getMixerInfo()).getDeviceID();
    }

    int getMaxSimulLines() {
        return ((DirectAudioDeviceProvider.DirectAudioDeviceInfo) getMixerInfo()).getMaxSimulLines();
    }

    private static void addFormat(Vector<AudioFormat> v, int bits, int frameSizeInBytes, int channels, float sampleRate,
                                  int encoding, boolean signed, boolean bigEndian) {
        AudioFormat.Encoding enc = null;
        switch (encoding) {
        case PCM:
            enc = signed?AudioFormat.Encoding.PCM_SIGNED:AudioFormat.Encoding.PCM_UNSIGNED;
            break;
        case ULAW:
            enc = AudioFormat.Encoding.ULAW;
            if (bits != 8) {
                if (Printer.err) Printer.err("DirectAudioDevice.addFormat called with ULAW, but bitsPerSample="+bits);
                bits = 8; frameSizeInBytes = channels;
            }
            break;
        case ALAW:
            enc = AudioFormat.Encoding.ALAW;
            if (bits != 8) {
                if (Printer.err) Printer.err("DirectAudioDevice.addFormat called with ALAW, but bitsPerSample="+bits);
                bits = 8; frameSizeInBytes = channels;
            }
            break;
        }
        if (enc==null) {
            if (Printer.err) Printer.err("DirectAudioDevice.addFormat called with unknown encoding: "+encoding);
            return;
        }
        if (frameSizeInBytes <= 0) {
            if (channels > 0) {
                frameSizeInBytes = ((bits + 7) / 8) * channels;
            } else {
                frameSizeInBytes = AudioSystem.NOT_SPECIFIED;
            }
        }
        v.add(new AudioFormat(enc, sampleRate, bits, channels, frameSizeInBytes, sampleRate, bigEndian));
    }

    protected static AudioFormat getSignOrEndianChangedFormat(AudioFormat format) {
        boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean isUnsigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
        if (format.getSampleSizeInBits() > 8 && isSigned) {
            // if this is PCM_SIGNED and 16-bit or higher, then try with endian-ness magic
            return new AudioFormat(format.getEncoding(),
                                   format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(),
                                   format.getFrameSize(), format.getFrameRate(), !format.isBigEndian());
        }
        else if (format.getSampleSizeInBits() == 8 && (isSigned || isUnsigned)) {
            // if this is PCM and 8-bit, then try with signed-ness magic
            return new AudioFormat(isSigned?AudioFormat.Encoding.PCM_UNSIGNED:AudioFormat.Encoding.PCM_SIGNED,
                                   format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(),
                                   format.getFrameSize(), format.getFrameRate(), format.isBigEndian());
        }
        return null;
    }

    /**
     * Private inner class for the DataLine.Info objects
     * adds a little magic for the isFormatSupported so
     * that the automagic conversion of endianness and sign
     * does not show up in the formats array.
     * I.e. the formats array contains only the formats
     * that are really supported by the hardware,
     * but isFormatSupported() also returns true
     * for formats with wrong endianness.
     */
    private static final class DirectDLI extends DataLine.Info {
        final AudioFormat[] hardwareFormats;

        private DirectDLI(Class<?> clazz, AudioFormat[] formatArray,
                          AudioFormat[] hardwareFormatArray,
                          int minBuffer, int maxBuffer) {
            super(clazz, formatArray, minBuffer, maxBuffer);
            this.hardwareFormats = hardwareFormatArray;
        }

        public boolean isFormatSupportedInHardware(AudioFormat format) {
            if (format == null) return false;
            for (int i = 0; i < hardwareFormats.length; i++) {
                if (format.matches(hardwareFormats[i])) {
                    return true;
                }
            }
            return false;
        }

        /*public boolean isFormatSupported(AudioFormat format) {
         *   return isFormatSupportedInHardware(format)
         *      || isFormatSupportedInHardware(getSignOrEndianChangedFormat(format));
         *}
         */

         private AudioFormat[] getHardwareFormats() {
             return hardwareFormats;
         }
    }

    /**
     * Private inner class as base class for direct lines.
     */
    private static class DirectDL extends AbstractDataLine implements EventDispatcher.LineMonitor {
        protected final int mixerIndex;
        protected final int deviceID;
        protected long id;
        protected int waitTime;
        protected volatile boolean flushing = false;
        protected final boolean isSource;         // true for SourceDataLine, false for TargetDataLine
        protected volatile long bytePosition;
        protected volatile boolean doIO = false;     // true in between start() and stop() calls
        protected volatile boolean stoppedWritten = false; // true if a write occurred in stopped state
        protected volatile boolean drained = false; // set to true when drain function returns, set to false in write()
        protected boolean monitoring = false;

        // if native needs to manually swap samples/convert sign, this
        // is set to the framesize
        protected int softwareConversionSize = 0;
        protected AudioFormat hardwareFormat;

        private final Gain gainControl = new Gain();
        private final Mute muteControl = new Mute();
        private final Balance balanceControl = new Balance();
        private final Pan panControl = new Pan();
        private float leftGain, rightGain;
        protected volatile boolean noService = false; // do not run the nService method

        // Guards all native calls.
        protected final Object lockNative = new Object();

        protected DirectDL(DataLine.Info info,
                           DirectAudioDevice mixer,
                           AudioFormat format,
                           int bufferSize,
                           int mixerIndex,
                           int deviceID,
                           boolean isSource) {
            super(info, mixer, null, format, bufferSize);
            this.mixerIndex = mixerIndex;
            this.deviceID = deviceID;
            this.waitTime = 10; // 10 milliseconds default wait time
            this.isSource = isSource;

        }

        @Override
        void implOpen(AudioFormat format, int bufferSize) throws LineUnavailableException {
            // $$fb part of fix for 4679187: Clip.open() throws unexpected Exceptions
            Toolkit.isFullySpecifiedAudioFormat(format);

            // check for record permission
            if (!isSource) {
                JSSecurityManager.checkRecordPermission();
            }
            int encoding = PCM;
            if (format.getEncoding().equals(AudioFormat.Encoding.ULAW)) {
                encoding = ULAW;
            }
            else if (format.getEncoding().equals(AudioFormat.Encoding.ALAW)) {
                encoding = ALAW;
            }

            if (bufferSize <= AudioSystem.NOT_SPECIFIED) {
                bufferSize = (int) Toolkit.millis2bytes(format, DEFAULT_LINE_BUFFER_TIME);
            }

            DirectDLI ddli = null;
            if (info instanceof DirectDLI) {
                ddli = (DirectDLI) info;
            }

            /* set up controls */
            if (isSource) {
                if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                    && !format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                    // no controls for non-PCM formats */
                    controls = new Control[0];
                }
                else if (format.getChannels() > 2
                         || format.getSampleSizeInBits() > 16) {
                    // no support for more than 2 channels or more than 16 bits
                    controls = new Control[0];
                } else {
                    if (format.getChannels() == 1) {
                        controls = new Control[2];
                    } else {
                        controls = new Control[4];
                        controls[2] = balanceControl;
                        /* to keep compatibility with apps that rely on
                         * MixerSourceLine's PanControl
                         */
                        controls[3] = panControl;
                    }
                    controls[0] = gainControl;
                    controls[1] = muteControl;
                }
            }
            hardwareFormat = format;

            /* some magic to account for not-supported endianness or signed-ness */
            softwareConversionSize = 0;
            if (ddli != null && !ddli.isFormatSupportedInHardware(format)) {
                AudioFormat newFormat = getSignOrEndianChangedFormat(format);
                if (ddli.isFormatSupportedInHardware(newFormat)) {
                    // apparently, the new format can be used.
                    hardwareFormat = newFormat;
                    // So do endian/sign conversion in software
                    softwareConversionSize = format.getFrameSize() / format.getChannels();
                }
            }

            // align buffer to full frames
            bufferSize = ( bufferSize / format.getFrameSize()) * format.getFrameSize();

            id = nOpen(mixerIndex, deviceID, isSource,
                    encoding,
                    hardwareFormat.getSampleRate(),
                    hardwareFormat.getSampleSizeInBits(),
                    hardwareFormat.getFrameSize(),
                    hardwareFormat.getChannels(),
                    hardwareFormat.getEncoding().equals(
                        AudioFormat.Encoding.PCM_SIGNED),
                    hardwareFormat.isBigEndian(),
                    bufferSize);

            if (id == 0) {
                // TODO: nicer error messages...
                throw new LineUnavailableException(
                        "line with format "+format+" not supported.");
            }

            this.bufferSize = nGetBufferSize(id, isSource);
            if (this.bufferSize < 1) {
                // this is an error!
                this.bufferSize = bufferSize;
            }
            this.format = format;
            // wait time = 1/4 of buffer time
            waitTime = (int) Toolkit.bytes2millis(format, this.bufferSize) / 4;
            if (waitTime < 10) {
                waitTime = 1;
            }
            else if (waitTime > 1000) {
                // we have seen large buffer sizes!
                // never wait for more than a second
                waitTime = 1000;
            }
            bytePosition = 0;
            stoppedWritten = false;
            doIO = false;
            calcVolume();
        }

        @Override
        void implStart() {
            // check for record permission
            if (!isSource) {
                JSSecurityManager.checkRecordPermission();
            }

            synchronized (lockNative)
            {
                nStart(id, isSource);
            }
            // check for monitoring/servicing
            monitoring = requiresServicing();
            if (monitoring) {
                getEventDispatcher().addLineMonitor(this);
            }

            synchronized(lock) {
                doIO = true;
                // need to set Active and Started
                // note: the current API always requires that
                //       Started and Active are set at the same time...
                if (isSource && stoppedWritten) {
                    setStarted(true);
                    setActive(true);
                }
            }
        }

        @Override
        void implStop() {
            // check for record permission
            if (!isSource) {
                JSSecurityManager.checkRecordPermission();
            }

            if (monitoring) {
                getEventDispatcher().removeLineMonitor(this);
                monitoring = false;
            }
            synchronized (lockNative) {
                nStop(id, isSource);
            }
            // wake up any waiting threads
            synchronized(lock) {
                // need to set doIO to false before notifying the
                // read/write thread, that's why isStartedRunning()
                // cannot be used
                doIO = false;
                setActive(false);
                setStarted(false);
                lock.notifyAll();
            }
            stoppedWritten = false;
        }

        @Override
        void implClose() {
            // check for record permission
            if (!isSource) {
                JSSecurityManager.checkRecordPermission();
            }

            // be sure to remove this monitor
            if (monitoring) {
                getEventDispatcher().removeLineMonitor(this);
                monitoring = false;
            }

            doIO = false;
            long oldID = id;
            id = 0;
            synchronized (lockNative) {
                nClose(oldID, isSource);
            }
            bytePosition = 0;
            softwareConversionSize = 0;
        }

        @Override
        public int available() {
            if (id == 0) {
                return 0;
            }
            int a;
            synchronized (lockNative) {
                a = nAvailable(id, isSource);
            }
            return a;
        }

        @Override
        public void drain() {
            noService = true;
            // additional safeguard against draining forever
            // this occurred on Solaris 8 x86, probably due to a bug
            // in the audio driver
            int counter = 0;
            long startPos = getLongFramePosition();
            boolean posChanged = false;
            while (!drained) {
                synchronized (lockNative) {
                    if ((id == 0) || (!doIO) || !nIsStillDraining(id, isSource))
                        break;
                }
                // check every now and then for a new position
                if ((counter % 5) == 4) {
                    long thisFramePos = getLongFramePosition();
                    posChanged = posChanged | (thisFramePos != startPos);
                    if ((counter % 50) > 45) {
                        // when some time elapsed, check that the frame position
                        // really changed
                        if (!posChanged) {
                            if (Printer.err) Printer.err("Native reports isDraining, but frame position does not increase!");
                            break;
                        }
                        posChanged = false;
                        startPos = thisFramePos;
                    }
                }
                counter++;
                synchronized(lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException ie) {}
                }
            }

            if (doIO && id != 0) {
                drained = true;
            }
            noService = false;
        }

        @Override
        public void flush() {
            if (id != 0) {
                // first stop ongoing read/write method
                flushing = true;
                synchronized(lock) {
                    lock.notifyAll();
                }
                synchronized (lockNative) {
                    if (id != 0) {
                        // then flush native buffers
                        nFlush(id, isSource);
                    }
                }
                drained = true;
            }
        }

        // replacement for getFramePosition (see AbstractDataLine)
        @Override
        public long getLongFramePosition() {
            long pos;
            synchronized (lockNative) {
                pos = nGetBytePosition(id, isSource, bytePosition);
            }
            // hack because ALSA sometimes reports wrong framepos
            if (pos < 0) {
                pos = 0;
            }
            return (pos / getFormat().getFrameSize());
        }

        /*
         * write() belongs into SourceDataLine and Clip,
         * so define it here and make it accessible by
         * declaring the respective interfaces with DirectSDL and DirectClip
         */
        public int write(byte[] b, int off, int len) {
            flushing = false;
            if (len == 0) {
                return 0;
            }
            if (len < 0) {
                throw new IllegalArgumentException("illegal len: "+len);
            }
            if (len % getFormat().getFrameSize() != 0) {
                throw new IllegalArgumentException("illegal request to write "
                                                   +"non-integral number of frames ("
                                                   +len+" bytes, "
                                                   +"frameSize = "+getFormat().getFrameSize()+" bytes)");
            }
            if (off < 0) {
                throw new ArrayIndexOutOfBoundsException(off);
            }
            if ((long)off + (long)len > (long)b.length) {
                throw new ArrayIndexOutOfBoundsException(b.length);
            }
            synchronized(lock) {
                if (!isActive() && doIO) {
                    // this is not exactly correct... would be nicer
                    // if the native sub system sent a callback when IO really
                    // starts
                    setActive(true);
                    setStarted(true);
                }
            }
            int written = 0;
            while (!flushing) {
                int thisWritten;
                synchronized (lockNative) {
                    thisWritten = nWrite(id, b, off, len,
                            softwareConversionSize,
                            leftGain, rightGain);
                    if (thisWritten < 0) {
                        // error in native layer
                        break;
                    }
                    bytePosition += thisWritten;
                    if (thisWritten > 0) {
                        drained = false;
                    }
                }
                len -= thisWritten;
                written += thisWritten;
                if (doIO && len > 0) {
                    off += thisWritten;
                    synchronized (lock) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException ie) {}
                    }
                } else {
                    break;
                }
            }
            if (written > 0 && !doIO) {
                stoppedWritten = true;
            }
            return written;
        }

        protected boolean requiresServicing() {
            return nRequiresServicing(id, isSource);
        }

        // called from event dispatcher for lines that need servicing
        @Override
        public void checkLine() {
            synchronized (lockNative) {
                if (monitoring
                        && doIO
                        && id != 0
                        && !flushing
                        && !noService) {
                    nService(id, isSource);
                }
            }
        }

        private void calcVolume() {
            if (getFormat() == null) {
                return;
            }
            if (muteControl.getValue()) {
                leftGain = 0.0f;
                rightGain = 0.0f;
                return;
            }
            float gain = gainControl.getLinearGain();
            if (getFormat().getChannels() == 1) {
                // trivial case: only use gain
                leftGain = gain;
                rightGain = gain;
            } else {
                // need to combine gain and balance
                float bal = balanceControl.getValue();
                if (bal < 0.0f) {
                    // left
                    leftGain = gain;
                    rightGain = gain * (bal + 1.0f);
                } else {
                    leftGain = gain * (1.0f - bal);
                    rightGain = gain;
                }
            }
        }

        /////////////////// CONTROLS /////////////////////////////

        protected final class Gain extends FloatControl {

            private float linearGain = 1.0f;

            private Gain() {

                super(FloatControl.Type.MASTER_GAIN,
                      Toolkit.linearToDB(0.0f),
                      Toolkit.linearToDB(2.0f),
                      Math.abs(Toolkit.linearToDB(1.0f)-Toolkit.linearToDB(0.0f))/128.0f,
                      -1,
                      0.0f,
                      "dB", "Minimum", "", "Maximum");
            }

            @Override
            public void setValue(float newValue) {
                // adjust value within range ?? spec says IllegalArgumentException
                //newValue = Math.min(newValue, getMaximum());
                //newValue = Math.max(newValue, getMinimum());

                float newLinearGain = Toolkit.dBToLinear(newValue);
                super.setValue(Toolkit.linearToDB(newLinearGain));
                // if no exception, commit to our new gain
                linearGain = newLinearGain;
                calcVolume();
            }

            float getLinearGain() {
                return linearGain;
            }
        } // class Gain

        private final class Mute extends BooleanControl {

            private Mute() {
                super(BooleanControl.Type.MUTE, false, "True", "False");
            }

            @Override
            public void setValue(boolean newValue) {
                super.setValue(newValue);
                calcVolume();
            }
        }  // class Mute

        private final class Balance extends FloatControl {

            private Balance() {
                super(FloatControl.Type.BALANCE, -1.0f, 1.0f, (1.0f / 128.0f), -1, 0.0f,
                      "", "Left", "Center", "Right");
            }

            @Override
            public void setValue(float newValue) {
                setValueImpl(newValue);
                panControl.setValueImpl(newValue);
                calcVolume();
            }

            void setValueImpl(float newValue) {
                super.setValue(newValue);
            }

        } // class Balance

        private final class Pan extends FloatControl {

            private Pan() {
                super(FloatControl.Type.PAN, -1.0f, 1.0f, (1.0f / 128.0f), -1, 0.0f,
                      "", "Left", "Center", "Right");
            }

            @Override
            public void setValue(float newValue) {
                setValueImpl(newValue);
                balanceControl.setValueImpl(newValue);
                calcVolume();
            }
            void setValueImpl(float newValue) {
                super.setValue(newValue);
            }
        } // class Pan
    } // class DirectDL

    /**
     * Private inner class representing a SourceDataLine.
     */
    private static final class DirectSDL extends DirectDL
            implements SourceDataLine {

        private DirectSDL(DataLine.Info info,
                          AudioFormat format,
                          int bufferSize,
                          DirectAudioDevice mixer) {
            super(info, mixer, format, bufferSize, mixer.getMixerIndex(), mixer.getDeviceID(), true);
        }
    }

    /**
     * Private inner class representing a TargetDataLine.
     */
    private static final class DirectTDL extends DirectDL
            implements TargetDataLine {

        private DirectTDL(DataLine.Info info,
                          AudioFormat format,
                          int bufferSize,
                          DirectAudioDevice mixer) {
            super(info, mixer, format, bufferSize, mixer.getMixerIndex(), mixer.getDeviceID(), false);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            flushing = false;
            if (len == 0) {
                return 0;
            }
            if (len < 0) {
                throw new IllegalArgumentException("illegal len: "+len);
            }
            if (len % getFormat().getFrameSize() != 0) {
                throw new IllegalArgumentException("illegal request to read "
                                                   +"non-integral number of frames ("
                                                   +len+" bytes, "
                                                   +"frameSize = "+getFormat().getFrameSize()+" bytes)");
            }
            if (off < 0) {
                throw new ArrayIndexOutOfBoundsException(off);
            }
            if ((long)off + (long)len > (long)b.length) {
                throw new ArrayIndexOutOfBoundsException(b.length);
            }
            synchronized(lock) {
                if (!isActive() && doIO) {
                    // this is not exactly correct... would be nicer
                    // if the native sub system sent a callback when IO really
                    // starts
                    setActive(true);
                    setStarted(true);
                }
            }
            int read = 0;
            while (doIO && !flushing) {
                int thisRead;
                synchronized (lockNative) {
                    thisRead = nRead(id, b, off, len, softwareConversionSize);
                    if (thisRead < 0) {
                        // error in native layer
                        break;
                    }
                    bytePosition += thisRead;
                    if (thisRead > 0) {
                        drained = false;
                    }
                }
                len -= thisRead;
                read += thisRead;
                if (len > 0) {
                    off += thisRead;
                    synchronized(lock) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException ie) {}
                    }
                } else {
                    break;
                }
            }
            if (flushing) {
                read = 0;
            }
            return read;
        }

    }

    /**
     * Private inner class representing a Clip
     * This clip is realized in software only
     */
    private static final class DirectClip extends DirectDL
            implements Clip, Runnable, AutoClosingClip {

        private volatile Thread thread;
        private volatile byte[] audioData = null;
        private volatile int frameSize;         // size of one frame in bytes
        private volatile int m_lengthInFrames;
        private volatile int loopCount;
        private volatile int clipBytePosition;   // index in the audioData array at current playback
        private volatile int newFramePosition;   // set in setFramePosition()
        private volatile int loopStartFrame;
        private volatile int loopEndFrame;      // the last sample included in the loop

        // auto closing clip support
        private boolean autoclosing = false;

        private DirectClip(DataLine.Info info,
                           AudioFormat format,
                           int bufferSize,
                           DirectAudioDevice mixer) {
            super(info, mixer, format, bufferSize, mixer.getMixerIndex(), mixer.getDeviceID(), true);
        }

        // CLIP METHODS

        @Override
        public void open(AudioFormat format, byte[] data, int offset, int bufferSize)
            throws LineUnavailableException {

            // $$fb part of fix for 4679187: Clip.open() throws unexpected Exceptions
            Toolkit.isFullySpecifiedAudioFormat(format);
            Toolkit.validateBuffer(format.getFrameSize(), bufferSize);

            byte[] newData = new byte[bufferSize];
            System.arraycopy(data, offset, newData, 0, bufferSize);
            open(format, newData, bufferSize / format.getFrameSize());
        }

        // this method does not copy the data array
        private void open(AudioFormat format, byte[] data, int frameLength)
            throws LineUnavailableException {

            // $$fb part of fix for 4679187: Clip.open() throws unexpected Exceptions
            Toolkit.isFullySpecifiedAudioFormat(format);

            synchronized (mixer) {
                if (isOpen()) {
                    throw new IllegalStateException("Clip is already open with format " + getFormat() +
                                                    " and frame lengh of " + getFrameLength());
                } else {
                    // if the line is not currently open, try to open it with this format and buffer size
                    this.audioData = data;
                    this.frameSize = format.getFrameSize();
                    this.m_lengthInFrames = frameLength;
                    // initialize loop selection with full range
                    bytePosition = 0;
                    clipBytePosition = 0;
                    newFramePosition = -1; // means: do not set to a new readFramePos
                    loopStartFrame = 0;
                    loopEndFrame = frameLength - 1;
                    loopCount = 0; // means: play the clip irrespective of loop points from beginning to end

                    try {
                        // use DirectDL's open method to open it
                        open(format, (int) Toolkit.millis2bytes(format, CLIP_BUFFER_TIME)); // one second buffer
                    } catch (LineUnavailableException lue) {
                        audioData = null;
                        throw lue;
                    } catch (IllegalArgumentException iae) {
                        audioData = null;
                        throw iae;
                    }

                    // if we got this far, we can instanciate the thread
                    int priority = Thread.NORM_PRIORITY
                        + (Thread.MAX_PRIORITY - Thread.NORM_PRIORITY) / 3;
                    thread = JSSecurityManager.createThread(this,
                                                            "Direct Clip", // name
                                                            true,     // daemon
                                                            priority, // priority
                                                            false);  // doStart
                    // cannot start in createThread, because the thread
                    // uses the "thread" variable as indicator if it should
                    // continue to run
                    thread.start();
                }
            }
            if (isAutoClosing()) {
                getEventDispatcher().autoClosingClipOpened(this);
            }
        }

        @Override
        public void open(AudioInputStream stream) throws LineUnavailableException, IOException {

            // $$fb part of fix for 4679187: Clip.open() throws unexpected Exceptions
            Toolkit.isFullySpecifiedAudioFormat(stream.getFormat());

            synchronized (mixer) {
                byte[] streamData = null;

                if (isOpen()) {
                    throw new IllegalStateException("Clip is already open with format " + getFormat() +
                                                    " and frame lengh of " + getFrameLength());
                }
                int lengthInFrames = (int)stream.getFrameLength();
                int bytesRead = 0;
                int frameSize = stream.getFormat().getFrameSize();
                if (lengthInFrames != AudioSystem.NOT_SPECIFIED) {
                    // read the data from the stream into an array in one fell swoop.
                    int arraysize = lengthInFrames * frameSize;
                    if (arraysize < 0) {
                        throw new IllegalArgumentException("Audio data < 0");
                    }
                    try {
                        streamData = new byte[arraysize];
                    } catch (OutOfMemoryError e) {
                        throw new IOException("Audio data is too big");
                    }
                    int bytesRemaining = arraysize;
                    int thisRead = 0;
                    while (bytesRemaining > 0 && thisRead >= 0) {
                        thisRead = stream.read(streamData, bytesRead, bytesRemaining);
                        if (thisRead > 0) {
                            bytesRead += thisRead;
                            bytesRemaining -= thisRead;
                        }
                        else if (thisRead == 0) {
                            Thread.yield();
                        }
                    }
                } else {
                    // read data from the stream until we reach the end of the stream
                    // we use a slightly modified version of ByteArrayOutputStream
                    // to get direct access to the byte array (we don't want a new array
                    // to be allocated)
                    int maxReadLimit = Math.max(16384, frameSize);
                    DirectBAOS dbaos  = new DirectBAOS();
                    byte[] tmp;
                    try {
                        tmp = new byte[maxReadLimit];
                    } catch (OutOfMemoryError e) {
                        throw new IOException("Audio data is too big");
                    }
                    int thisRead = 0;
                    while (thisRead >= 0) {
                        thisRead = stream.read(tmp, 0, tmp.length);
                        if (thisRead > 0) {
                            dbaos.write(tmp, 0, thisRead);
                            bytesRead += thisRead;
                        }
                        else if (thisRead == 0) {
                            Thread.yield();
                        }
                    } // while
                    streamData = dbaos.getInternalBuffer();
                }
                lengthInFrames = bytesRead / frameSize;

                // now try to open the device
                open(stream.getFormat(), streamData, lengthInFrames);
            } // synchronized
        }

        @Override
        public int getFrameLength() {
            return m_lengthInFrames;
        }

        @Override
        public long getMicrosecondLength() {
            return Toolkit.frames2micros(getFormat(), getFrameLength());
        }

        @Override
        public void setFramePosition(int frames) {
            if (frames < 0) {
                frames = 0;
            }
            else if (frames >= getFrameLength()) {
                frames = getFrameLength();
            }
            if (doIO) {
                newFramePosition = frames;
            } else {
                clipBytePosition = frames * frameSize;
                newFramePosition = -1;
            }
            // fix for failing test050
            // $$fb although getFramePosition should return the number of rendered
            // frames, it is intuitive that setFramePosition will modify that
            // value.
            bytePosition = frames * frameSize;

            // cease currently playing buffer
            flush();

            // set new native position (if necessary)
            // this must come after the flush!
            synchronized (lockNative) {
                nSetBytePosition(id, isSource, frames * frameSize);
            }
        }

        // replacement for getFramePosition (see AbstractDataLine)
        @Override
        public long getLongFramePosition() {
            /* $$fb
             * this would be intuitive, but the definition of getFramePosition
             * is the number of frames rendered since opening the device...
             * That also means that setFramePosition() means something very
             * different from getFramePosition() for Clip.
             */
            // take into account the case that a new position was set...
            //if (!doIO && newFramePosition >= 0) {
            //return newFramePosition;
            //}
            return super.getLongFramePosition();
        }

        @Override
        public void setMicrosecondPosition(long microseconds) {
            long frames = Toolkit.micros2frames(getFormat(), microseconds);
            setFramePosition((int) frames);
        }

        @Override
        public void setLoopPoints(int start, int end) {
            if (start < 0 || start >= getFrameLength()) {
                throw new IllegalArgumentException("illegal value for start: "+start);
            }
            if (end >= getFrameLength()) {
                throw new IllegalArgumentException("illegal value for end: "+end);
            }

            if (end == -1) {
                end = getFrameLength() - 1;
                if (end < 0) {
                    end = 0;
                }
            }

            // if the end position is less than the start position, throw IllegalArgumentException
            if (end < start) {
                throw new IllegalArgumentException("End position " + end + "  preceeds start position " + start);
            }

            // slight race condition with the run() method, but not a big problem
            loopStartFrame = start;
            loopEndFrame = end;
        }

        @Override
        public void loop(int count) {
            // note: when count reaches 0, it means that the entire clip
            // will be played, i.e. it will play past the loop end point
            loopCount = count;
            start();
        }

        @Override
        void implOpen(AudioFormat format, int bufferSize) throws LineUnavailableException {
            // only if audioData wasn't set in a calling open(format, byte[], frameSize)
            // this call is allowed.
            if (audioData == null) {
                throw new IllegalArgumentException("illegal call to open() in interface Clip");
            }
            super.implOpen(format, bufferSize);
        }

        @Override
        void implClose() {
            // dispose of thread
            Thread oldThread = thread;
            thread = null;
            doIO = false;
            if (oldThread != null) {
                // wake up the thread if it's in wait()
                synchronized(lock) {
                    lock.notifyAll();
                }
                // wait for the thread to terminate itself,
                // but max. 2 seconds. Must not be synchronized!
                try {
                    oldThread.join(2000);
                } catch (InterruptedException ie) {}
            }
            super.implClose();
            // remove audioData reference and hand it over to gc
            audioData = null;
            newFramePosition = -1;

            // remove this instance from the list of auto closing clips
            getEventDispatcher().autoClosingClipClosed(this);
        }

        @Override
        void implStart() {
            super.implStart();
        }

        @Override
        void implStop() {
            super.implStop();
            // reset loopCount field so that playback will be normal with
            // next call to start()
            loopCount = 0;
        }

        // main playback loop
        @Override
        public void run() {
            Thread curThread = Thread.currentThread();
            while (thread == curThread) {
                // doIO is volatile, but we could check it, then get
                // pre-empted while another thread changes doIO and notifies,
                // before we wait (so we sleep in wait forever).
                synchronized(lock) {
                    while (!doIO && thread == curThread) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                while (doIO && thread == curThread) {
                    if (newFramePosition >= 0) {
                        clipBytePosition = newFramePosition * frameSize;
                        newFramePosition = -1;
                    }
                    int endFrame = getFrameLength() - 1;
                    if (loopCount > 0 || loopCount == LOOP_CONTINUOUSLY) {
                        endFrame = loopEndFrame;
                    }
                    long framePos = (clipBytePosition / frameSize);
                    int toWriteFrames = (int) (endFrame - framePos + 1);
                    int toWriteBytes = toWriteFrames * frameSize;
                    if (toWriteBytes > getBufferSize()) {
                        toWriteBytes = Toolkit.align(getBufferSize(), frameSize);
                    }
                    int written = write(audioData, clipBytePosition, toWriteBytes); // increases bytePosition
                    clipBytePosition += written;
                    // make sure nobody called setFramePosition, or stop() during the write() call
                    if (doIO && newFramePosition < 0 && written >= 0) {
                        framePos = clipBytePosition / frameSize;
                        // since endFrame is the last frame to be played,
                        // framePos is after endFrame when all frames, including framePos,
                        // are played.
                        if (framePos > endFrame) {
                            // at end of playback. If looping is on, loop back to the beginning.
                            if (loopCount > 0 || loopCount == LOOP_CONTINUOUSLY) {
                                if (loopCount != LOOP_CONTINUOUSLY) {
                                    loopCount--;
                                }
                                newFramePosition = loopStartFrame;
                            } else {
                                // no looping, stop playback
                                drain();
                                stop();
                            }
                        }
                    }
                }
            }
        }

        // AUTO CLOSING CLIP SUPPORT

        /* $$mp 2003-10-01
           The following two methods are common between this class and
           MixerClip. They should be moved to a base class, together
           with the instance variable 'autoclosing'. */

        @Override
        public boolean isAutoClosing() {
            return autoclosing;
        }

        @Override
        public void setAutoClosing(boolean value) {
            if (value != autoclosing) {
                if (isOpen()) {
                    if (value) {
                        getEventDispatcher().autoClosingClipOpened(this);
                    } else {
                        getEventDispatcher().autoClosingClipClosed(this);
                    }
                }
                autoclosing = value;
            }
        }

        @Override
        protected boolean requiresServicing() {
            // no need for servicing for Clips
            return false;
        }

    } // DirectClip

    /*
     * private inner class representing a ByteArrayOutputStream
     * which allows retrieval of the internal array
     */
    private static class DirectBAOS extends ByteArrayOutputStream {
        DirectBAOS() {
            super();
        }

        public byte[] getInternalBuffer() {
            return buf;
        }

    } // class DirectBAOS

    @SuppressWarnings("rawtypes")
    private static native void nGetFormats(int mixerIndex, int deviceID,
                                           boolean isSource, Vector formats);

    private static native long nOpen(int mixerIndex, int deviceID, boolean isSource,
                                     int encoding,
                                     float sampleRate,
                                     int sampleSizeInBits,
                                     int frameSize,
                                     int channels,
                                     boolean signed,
                                     boolean bigEndian,
                                     int bufferSize) throws LineUnavailableException;
    private static native void nStart(long id, boolean isSource);
    private static native void nStop(long id, boolean isSource);
    private static native void nClose(long id, boolean isSource);
    private static native int nWrite(long id, byte[] b, int off, int len, int conversionSize,
                                     float volLeft, float volRight);
    private static native int nRead(long id, byte[] b, int off, int len, int conversionSize);
    private static native int nGetBufferSize(long id, boolean isSource);
    private static native boolean nIsStillDraining(long id, boolean isSource);
    private static native void nFlush(long id, boolean isSource);
    private static native int nAvailable(long id, boolean isSource);
    // javaPos is number of bytes read/written in Java layer
    private static native long nGetBytePosition(long id, boolean isSource, long javaPos);
    private static native void nSetBytePosition(long id, boolean isSource, long pos);

    // returns if the native implementation needs regular calls to nService()
    private static native boolean nRequiresServicing(long id, boolean isSource);
    // called in irregular intervals
    private static native void nService(long id, boolean isSource);
}
