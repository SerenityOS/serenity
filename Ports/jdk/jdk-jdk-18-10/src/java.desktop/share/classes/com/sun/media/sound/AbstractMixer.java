/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;

import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * Abstract Mixer.  Implements Mixer (with abstract methods) and specifies
 * some other common methods for use by our implementation.
 *
 * @author Kara Kytle
 */
//$$fb 2002-07-26: let AbstractMixer be an AbstractLine and NOT an AbstractDataLine!
abstract class AbstractMixer extends AbstractLine implements Mixer {

    //  STATIC VARIABLES
    protected static final int PCM  = 0;
    protected static final int ULAW = 1;
    protected static final int ALAW = 2;


    // IMMUTABLE PROPERTIES

    /**
     * Info object describing this mixer.
     */
    private final Mixer.Info mixerInfo;

    /**
     * source lines provided by this mixer
     */
    protected Line.Info[] sourceLineInfo;

    /**
     * target lines provided by this mixer
     */
    protected Line.Info[] targetLineInfo;

    /**
     * if any line of this mixer is started
     */
    private boolean started = false;

    /**
     * if this mixer had been opened manually with open()
     * If it was, then it won't be closed automatically,
     * only when close() is called manually.
     */
    private boolean manuallyOpened = false;

    // STATE VARIABLES

    /**
     * Source lines (ports) currently open.
     */
    private final Vector<Line> sourceLines = new Vector<>();

    /**
     * Target lines currently open.
     */
    private final Vector<Line> targetLines = new Vector<>();

    /**
     * Constructs a new AbstractMixer.
     * @param mixerInfo the mixer with which this line is associated
     * @param controls set of supported controls
     */
    protected AbstractMixer(Mixer.Info mixerInfo,
                            Control[] controls,
                            Line.Info[] sourceLineInfo,
                            Line.Info[] targetLineInfo) {

        // Line.Info, AbstractMixer, Control[]
        super(new Line.Info(Mixer.class), null, controls);

        // setup the line part
        this.mixer = this;
        if (controls == null) {
            controls = new Control[0];
        }

        // setup the mixer part
        this.mixerInfo = mixerInfo;
        this.sourceLineInfo = sourceLineInfo;
        this.targetLineInfo = targetLineInfo;
    }

    // MIXER METHODS

    @Override
    public final Mixer.Info getMixerInfo() {
        return mixerInfo;
    }

    @Override
    public final Line.Info[] getSourceLineInfo() {
        Line.Info[] localArray = new Line.Info[sourceLineInfo.length];
        System.arraycopy(sourceLineInfo, 0, localArray, 0, sourceLineInfo.length);
        return localArray;
    }

    @Override
    public final Line.Info[] getTargetLineInfo() {
        Line.Info[] localArray = new Line.Info[targetLineInfo.length];
        System.arraycopy(targetLineInfo, 0, localArray, 0, targetLineInfo.length);
        return localArray;
    }

    @Override
    public final Line.Info[] getSourceLineInfo(Line.Info info) {

        int i;
        Vector<Line.Info> vec = new Vector<>();

        for (i = 0; i < sourceLineInfo.length; i++) {

            if (info.matches(sourceLineInfo[i])) {
                vec.addElement(sourceLineInfo[i]);
            }
        }

        Line.Info[] returnedArray = new Line.Info[vec.size()];
        for (i = 0; i < returnedArray.length; i++) {
            returnedArray[i] = vec.elementAt(i);
        }

        return returnedArray;
    }

    @Override
    public final Line.Info[] getTargetLineInfo(Line.Info info) {

        int i;
        Vector<Line.Info> vec = new Vector<>();

        for (i = 0; i < targetLineInfo.length; i++) {

            if (info.matches(targetLineInfo[i])) {
                vec.addElement(targetLineInfo[i]);
            }
        }

        Line.Info[] returnedArray = new Line.Info[vec.size()];
        for (i = 0; i < returnedArray.length; i++) {
            returnedArray[i] = vec.elementAt(i);
        }

        return returnedArray;
    }

    @Override
    public final boolean isLineSupported(Line.Info info) {

        int i;

        for (i = 0; i < sourceLineInfo.length; i++) {

            if (info.matches(sourceLineInfo[i])) {
                return true;
            }
        }

        for (i = 0; i < targetLineInfo.length; i++) {

            if (info.matches(targetLineInfo[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public abstract Line getLine(Line.Info info) throws LineUnavailableException;

    @Override
    public abstract int getMaxLines(Line.Info info);

    protected abstract void implOpen() throws LineUnavailableException;
    protected abstract void implStart();
    protected abstract void implStop();
    protected abstract void implClose();

    @Override
    public final Line[] getSourceLines() {

        Line[] localLines;

        synchronized(sourceLines) {

            localLines = new Line[sourceLines.size()];

            for (int i = 0; i < localLines.length; i++) {
                localLines[i] = sourceLines.elementAt(i);
            }
        }

        return localLines;
    }

    @Override
    public final Line[] getTargetLines() {

        Line[] localLines;

        synchronized(targetLines) {

            localLines = new Line[targetLines.size()];

            for (int i = 0; i < localLines.length; i++) {
                localLines[i] = targetLines.elementAt(i);
            }
        }

        return localLines;
    }

    /**
     * Default implementation always throws an exception.
     */
    @Override
    public final void synchronize(Line[] lines, boolean maintainSync) {
        throw new IllegalArgumentException("Synchronization not supported by this mixer.");
    }

    /**
     * Default implementation always throws an exception.
     */
    @Override
    public final void unsynchronize(Line[] lines) {
        throw new IllegalArgumentException("Synchronization not supported by this mixer.");
    }

    /**
     * Default implementation always returns false.
     */
    @Override
    public final boolean isSynchronizationSupported(Line[] lines,
                                                    boolean maintainSync) {
        return false;
    }

    // OVERRIDES OF ABSTRACT DATA LINE METHODS

    /**
     * This implementation tries to open the mixer with its current format and buffer size settings.
     */
    @Override
    public final synchronized void open() throws LineUnavailableException {
        open(true);
    }

    /**
     * This implementation tries to open the mixer with its current format and buffer size settings.
     */
    final synchronized void open(boolean manual) throws LineUnavailableException {
        if (!isOpen()) {
            implOpen();
            // if the mixer is not currently open, set open to true and send event
            setOpen(true);
            if (manual) {
                manuallyOpened = true;
            }
        }
    }

    // METHOD FOR INTERNAL IMPLEMENTATION USE

    /**
     * The default implementation of this method just determines whether
     * this line is a source or target line, calls open(no-arg) on the
     * mixer, and adds the line to the appropriate vector.
     * The mixer may be opened at a format different than the line's
     * format if it is a DataLine.
     */
    final synchronized void open(Line line) throws LineUnavailableException {
        // $$kk: 06.11.99: ignore ourselves for now
        if (this.equals(line)) {
            return;
        }

        // source line?
        if (isSourceLine(line.getLineInfo())) {
            if (! sourceLines.contains(line) ) {
                // call the no-arg open method for the mixer; it should open at its
                // default format if it is not open yet
                open(false);

                // we opened successfully! add the line to the list
                sourceLines.addElement(line);
            }
        } else {
            // target line?
            if(isTargetLine(line.getLineInfo())) {
                if (! targetLines.contains(line) ) {
                    // call the no-arg open method for the mixer; it should open at its
                    // default format if it is not open yet
                    open(false);

                    // we opened successfully!  add the line to the list
                    targetLines.addElement(line);
                }
            } else {
                if (Printer.err) Printer.err("Unknown line received for AbstractMixer.open(Line): " + line);
            }
        }
    }

    /**
     * Removes this line from the list of open source lines and
     * open target lines, if it exists in either.
     * If the list is now empty, closes the mixer.
     */
    final synchronized void close(Line line) {
        // $$kk: 06.11.99: ignore ourselves for now
        if (this.equals(line)) {
            return;
        }

        sourceLines.removeElement(line);
        targetLines.removeElement(line);

        if (sourceLines.isEmpty() && targetLines.isEmpty() && !manuallyOpened) {
            close();
        }
    }

    /**
     * Close all lines and then close this mixer.
     */
    @Override
    public final synchronized void close() {
        if (isOpen()) {
            // close all source lines
            Line[] localLines = getSourceLines();
            for (int i = 0; i<localLines.length; i++) {
                localLines[i].close();
            }

            // close all target lines
            localLines = getTargetLines();
            for (int i = 0; i<localLines.length; i++) {
                localLines[i].close();
            }

            implClose();

            // set the open state to false and send events
            setOpen(false);
        }
        manuallyOpened = false;
    }

    /**
     * Starts the mixer.
     */
    final synchronized void start(Line line) {
        // $$kk: 06.11.99: ignore ourselves for now
        if (this.equals(line)) {
            return;
        }

        // we just start the mixer regardless of anything else here.
        if (!started) {
            implStart();
            started = true;
        }
    }

    /**
     * Stops the mixer if this was the last running line.
     */
    final synchronized void stop(Line line) {
        // $$kk: 06.11.99: ignore ourselves for now
        if (this.equals(line)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Vector<Line> localSourceLines = (Vector<Line>)sourceLines.clone();
        for (int i = 0; i < localSourceLines.size(); i++) {

            // if any other open line is running, return

            // this covers clips and source data lines
            if (localSourceLines.elementAt(i) instanceof AbstractDataLine) {
                AbstractDataLine sourceLine = (AbstractDataLine)localSourceLines.elementAt(i);
                if ( sourceLine.isStartedRunning() && (!sourceLine.equals(line)) ) {
                    return;
                }
            }
        }

        @SuppressWarnings("unchecked")
        Vector<Line> localTargetLines = (Vector<Line>)targetLines.clone();
        for (int i = 0; i < localTargetLines.size(); i++) {

            // if any other open line is running, return
            // this covers target data lines
            if (localTargetLines.elementAt(i) instanceof AbstractDataLine) {
                AbstractDataLine targetLine = (AbstractDataLine)localTargetLines.elementAt(i);
                if ( targetLine.isStartedRunning() && (!targetLine.equals(line)) ) {
                    return;
                }
            }
        }

        // otherwise, stop
        started = false;
        implStop();
    }

    /**
     * Determines whether this is a source line for this mixer.
     * Right now this just checks whether it's supported, but should
     * check whether it actually belongs to this mixer....
     */
    final boolean isSourceLine(Line.Info info) {

        for (int i = 0; i < sourceLineInfo.length; i++) {
            if (info.matches(sourceLineInfo[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether this is a target line for this mixer.
     * Right now this just checks whether it's supported, but should
     * check whether it actually belongs to this mixer....
     */
    final boolean isTargetLine(Line.Info info) {

        for (int i = 0; i < targetLineInfo.length; i++) {
            if (info.matches(targetLineInfo[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first complete Line.Info object it finds that
     * matches the one specified, or null if no matching Line.Info
     * object is found.
     */
    final Line.Info getLineInfo(Line.Info info) {
        if (info == null) {
            return null;
        }
        // $$kk: 05.31.99: need to change this so that
        // the format and buffer size get set in the
        // returned info object for data lines??
        for (int i = 0; i < sourceLineInfo.length; i++) {
            if (info.matches(sourceLineInfo[i])) {
                return sourceLineInfo[i];
            }
        }

        for (int i = 0; i < targetLineInfo.length; i++) {
            if (info.matches(targetLineInfo[i])) {
                return targetLineInfo[i];
            }
        }
        return null;
    }
}
