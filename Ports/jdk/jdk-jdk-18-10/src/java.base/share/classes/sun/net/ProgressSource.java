/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
package sun.net;

import java.net.URL;

/**
 * ProgressSource represents the source of progress changes.
 *
 * @author Stanley Man-Kit Ho
 */
public class ProgressSource
{
    public enum State { NEW, CONNECTED, UPDATE, DELETE };

    // URL
    private URL url;
    // URL method
    private String method;
    // Content type
    private String contentType;
    // bytes read
    private long progress = 0;
    // last bytes read
    private long lastProgress = 0;
    //bytes expected
    private long expected = -1;
    // the last thing to happen with this source
    private State state;
    // connect flag
    private boolean connected = false;
    // threshold for notification
    private int threshold = 8192;
    // progress monitor
    private ProgressMonitor progressMonitor;

    /**
     * Construct progress source object.
     */
    public ProgressSource(URL url, String method) {
        this(url, method, -1);
    }

    /**
     * Construct progress source object.
     */
    public ProgressSource(URL url, String method, long expected)  {
        this.url = url;
        this.method = method;
        this.contentType = "content/unknown";
        this.progress = 0;
        this.lastProgress = 0;
        this.expected = expected;
        this.state = State.NEW;
        this.progressMonitor = ProgressMonitor.getDefault();
        this.threshold = progressMonitor.getProgressUpdateThreshold();
    }

    public boolean connected() {
        if (!connected) {
            connected = true;
            state = State.CONNECTED;
            return false;
        }
        return true;
    }

    /**
     * Close progress source.
     */
    public void close() {
        state = State.DELETE;
    }

    /**
     * Return URL of progress source.
     */
    public URL getURL() {
        return url;
    }

    /**
     * Return method of URL.
     */
    public String getMethod()  {
        return method;
    }

    /**
     * Return content type of URL.
     */
    public String getContentType()  {
        return contentType;
    }

    // Change content type
    public void setContentType(String ct)  {
        contentType = ct;
    }

    /**
     * Return current progress.
     */
    public long getProgress()  {
        return progress;
    }

    /**
     * Return expected maximum progress; -1 if expected is unknown.
     */
    public long getExpected() {
        return expected;
    }

    /**
     * Return state.
     */
    public State getState() {
        return state;
    }

    /**
     * Begin progress tracking.
     */
    public void beginTracking() {
        progressMonitor.registerSource(this);
    }

    /**
     * Finish progress tracking.
     */
    public void finishTracking() {
        progressMonitor.unregisterSource(this);
    }

    /**
     * Update progress.
     */
    public void updateProgress(long latestProgress, long expectedProgress) {
        lastProgress = progress;
        progress = latestProgress;
        expected = expectedProgress;

        if (connected() == false)
            state = State.CONNECTED;
        else
            state = State.UPDATE;

        // The threshold effectively divides the progress into
        // different set of ranges:
        //
        //      Range 0: 0..threshold-1,
        //      Range 1: threshold .. 2*threshold-1
        //      ....
        //      Range n: n*threshold .. (n+1)*threshold-1
        //
        // To determine which range the progress belongs to, it
        // would be calculated as follow:
        //
        //      range number = progress / threshold
        //
        // Notification should only be triggered when the current
        // progress and the last progress are in different ranges,
        // i.e. they have different range numbers.
        //
        // Using this range scheme, notification will be generated
        // only once when the progress reaches each range.
        //
        if (lastProgress / threshold != progress / threshold)   {
            progressMonitor.updateProgress(this);
        }

        // Detect read overrun
        if (expected != -1) {
            if (progress >= expected && progress != 0)
                close();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString()    {
        return getClass().getName() + "[url=" + url + ", method=" + method + ", state=" + state
            + ", content-type=" + contentType + ", progress=" + progress + ", expected=" + expected + "]";
    }
}
