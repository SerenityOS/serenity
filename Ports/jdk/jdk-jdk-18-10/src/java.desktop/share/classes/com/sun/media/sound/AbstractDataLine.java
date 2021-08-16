/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * AbstractDataLine
 *
 * @author Kara Kytle
 */
abstract class AbstractDataLine extends AbstractLine implements DataLine {

    // DEFAULTS

    // default format
    private final AudioFormat defaultFormat;

    // default buffer size in bytes
    private final int defaultBufferSize;

    // the lock for synchronization
    protected final Object lock = new Object();

    // STATE

    // current format
    protected AudioFormat format;

    // current buffer size in bytes
    protected int bufferSize;

    private volatile boolean running;
    private volatile boolean started;
    private volatile boolean active;

    /**
     * Constructs a new AbstractLine.
     */
    protected AbstractDataLine(DataLine.Info info, AbstractMixer mixer, Control[] controls) {
        this(info, mixer, controls, null, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Constructs a new AbstractLine.
     */
    protected AbstractDataLine(DataLine.Info info, AbstractMixer mixer, Control[] controls, AudioFormat format, int bufferSize) {

        super(info, mixer, controls);

        // record the default values
        if (format != null) {
            defaultFormat = format;
        } else {
            // default CD-quality
            defaultFormat = new AudioFormat(44100.0f, 16, 2, true, Platform.isBigEndian());
        }
        if (bufferSize > 0) {
            defaultBufferSize = bufferSize;
        } else {
            // 0.5 seconds buffer
            defaultBufferSize = ((int) (defaultFormat.getFrameRate() / 2)) * defaultFormat.getFrameSize();
        }

        // set the initial values to the defaults
        this.format = defaultFormat;
        this.bufferSize = defaultBufferSize;
    }


    // DATA LINE METHODS

    public final void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        //$$fb 2001-10-09: Bug #4517739: avoiding deadlock by synchronizing to mixer !
        synchronized (mixer) {
            // if the line is not currently open, try to open it with this format and buffer size
            if (!isOpen()) {
                // make sure that the format is specified correctly
                // $$fb part of fix for 4679187: Clip.open() throws unexpected Exceptions
                Toolkit.isFullySpecifiedAudioFormat(format);
                // reserve mixer resources for this line
                //mixer.open(this, format, bufferSize);
                mixer.open(this);

                try {
                    // open the data line.  may throw LineUnavailableException.
                    implOpen(format, bufferSize);

                    // if we succeeded, set the open state to true and send events
                    setOpen(true);

                } catch (LineUnavailableException e) {
                    // release mixer resources for this line and then throw the exception
                    mixer.close(this);
                    throw e;
                }
            } else {
                // if the line is already open and the requested format differs from the
                // current settings, throw an IllegalStateException
                //$$fb 2002-04-02: fix for 4661602: Buffersize is checked when re-opening line
                if (!format.matches(getFormat())) {
                    throw new IllegalStateException("Line is already open with format " + getFormat() +
                                                    " and bufferSize " + getBufferSize());
                }
                //$$fb 2002-07-26: allow changing the buffersize of already open lines
                if (bufferSize > 0) {
                    setBufferSize(bufferSize);
                }
            }
        }
    }

    public final void open(AudioFormat format) throws LineUnavailableException {
        open(format, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * This implementation always returns 0.
     */
    @Override
    public int available() {
        return 0;
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void drain() {
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void flush() {
    }

    @Override
    public final void start() {
        //$$fb 2001-10-09: Bug #4517739: avoiding deadlock by synchronizing to mixer !
        synchronized(mixer) {

            // $$kk: 06.06.99: if not open, this doesn't work....???
            if (isOpen()) {

                if (!isStartedRunning()) {
                    mixer.start(this);
                    implStart();
                    running = true;
                }
            }
        }

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    @Override
    public final void stop() {

        //$$fb 2001-10-09: Bug #4517739: avoiding deadlock by synchronizing to mixer !
        synchronized(mixer) {
            // $$kk: 06.06.99: if not open, this doesn't work.
            if (isOpen()) {

                if (isStartedRunning()) {

                    implStop();
                    mixer.stop(this);

                    running = false;

                    // $$kk: 11.10.99: this is not exactly correct, but will probably work
                    if (started && (!isActive())) {
                        setStarted(false);
                    }
                }
            }
        }

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    // $$jb: 12.10.99: The official API for this is isRunning().
    // Per the denied RFE 4297981,
    // the change to isStarted() is technically an unapproved API change.
    // The 'started' variable is false when playback of data stops.
    // It is changed throughout the implementation with setStarted().
    // This state is what should be returned by isRunning() in the API.
    // Note that the 'running' variable is true between calls to
    // start() and stop().  This state is accessed now through the
    // isStartedRunning() method, defined below.  I have not changed
    // the variable names at this point, since 'running' is accessed
    // in MixerSourceLine and MixerClip, and I want to touch as little
    // code as possible to change isStarted() back to isRunning().

    @Override
    public final boolean isRunning() {
        return started;
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final long getMicrosecondPosition() {

        long microseconds = getLongFramePosition();
        if (microseconds != AudioSystem.NOT_SPECIFIED) {
            microseconds = Toolkit.frames2micros(getFormat(), microseconds);
        }
        return microseconds;
    }

    @Override
    public final AudioFormat getFormat() {
        return format;
    }

    @Override
    public final int getBufferSize() {
        return bufferSize;
    }

    /**
     * This implementation does NOT change the buffer size
     */
    public final int setBufferSize(int newSize) {
        return getBufferSize();
    }

    /**
     * This implementation returns AudioSystem.NOT_SPECIFIED.
     */
    @Override
    public final float getLevel() {
        return (float)AudioSystem.NOT_SPECIFIED;
    }

    // HELPER METHODS

    /**
     * running is true after start is called and before stop is called,
     * regardless of whether data is actually being presented.
     */
    // $$jb: 12.10.99: calling this method isRunning() conflicts with
    // the official API that was once called isStarted().  Since we
    // use this method throughout the implementation, I am renaming
    // it to isStartedRunning().  This is part of backing out the
    // change denied in RFE 4297981.

    final boolean isStartedRunning() {
        return running;
    }

    /**
     * This method sets the active state and generates
     * events if it changes.
     */
    final void setActive(boolean active) {
        //boolean sendEvents = false;
        //long position = getLongFramePosition();

        this.active = active;

        // $$kk: 11.19.99: take ACTIVE / INACTIVE / EOM events out;
        // putting them in is technically an API change.
        // do not generate ACTIVE / INACTIVE events for now
        // if (sendEvents) {
        //
        //      if (active) {
        //              sendEvents(new LineEvent(this, LineEvent.Type.ACTIVE, position));
        //      } else {
        //              sendEvents(new LineEvent(this, LineEvent.Type.INACTIVE, position));
        //      }
        //}
    }

    /**
     * This method sets the started state and generates
     * events if it changes.
     */
    final void setStarted(boolean started) {
        boolean sendEvents = false;
        long position = getLongFramePosition();

        if (this.started != started) {
            this.started = started;
            sendEvents = true;
        }

        if (sendEvents) {

            if (started) {
                sendEvents(new LineEvent(this, LineEvent.Type.START, position));
            } else {
                sendEvents(new LineEvent(this, LineEvent.Type.STOP, position));
            }
        }
    }

    /**
     * This method generates a STOP event and sets the started state to false.
     * It is here for historic reasons when an EOM event existed.
     */
    final void setEOM() {
        //$$fb 2002-04-21: sometimes, 2 STOP events are generated.
        // better use setStarted() to send STOP event.
        setStarted(false);
    }

    // OVERRIDES OF ABSTRACT LINE METHODS

    /**
     * Try to open the line with the current format and buffer size values.
     * If the line is not open, these will be the defaults.  If the
     * line is open, this should return quietly because the values
     * requested will match the current ones.
     */
    @Override
    public final void open() throws LineUnavailableException {
        // this may throw a LineUnavailableException.
        open(format, bufferSize);
    }

    /**
     * This should also stop the line.  The closed line should not be running or active.
     * After we close the line, we reset the format and buffer size to the defaults.
     */
    @Override
    public final void close() {
        //$$fb 2001-10-09: Bug #4517739: avoiding deadlock by synchronizing to mixer !
        synchronized (mixer) {
            if (isOpen()) {

                // stop
                stop();

                // set the open state to false and send events
                setOpen(false);

                // close resources for this line
                implClose();

                // release mixer resources for this line
                mixer.close(this);

                // reset format and buffer size to the defaults
                format = defaultFormat;
                bufferSize = defaultBufferSize;
            }
        }
    }

    abstract void implOpen(AudioFormat format, int bufferSize) throws LineUnavailableException;
    abstract void implClose();

    abstract void implStart();
    abstract void implStop();
}
