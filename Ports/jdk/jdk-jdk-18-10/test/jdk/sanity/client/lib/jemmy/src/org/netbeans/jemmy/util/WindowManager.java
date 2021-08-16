/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.util;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.util.Vector;

import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.WindowWaiter;
import org.netbeans.jemmy.operators.WindowOperator;

/**
 * Class allows to make periodical window jobs like error window closing.
 *
 * @see WindowJob
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class WindowManager implements Timeoutable, Outputable {

    /**
     * Default value for WindowManager.TimeDelta timeout.
     */
    private static long TIME_DELTA = 1000;

    private static WindowManager manager;

    private Vector<JobThread> jobs;
    private Timeouts timeouts;
    private TestOut output;

    private WindowManager() {
        super();
        setTimeouts(JemmyProperties.getCurrentTimeouts());
        setOutput(JemmyProperties.getCurrentOutput());
        jobs = new Vector<>();
    }

    /**
     * Adds job to list.
     *
     * @param job a job to perform.
     */
    public static void addJob(WindowJob<?, Window> job) {
        manager.add(job);
    }

    /**
     * Removes job from list.
     *
     * @param job a job to remove.
     */
    public static void removeJob(WindowJob<?, ?> job) {
        manager.remove(job);
    }

    public static void performJob(WindowJob<?, Window> job) {
        while (manager.performJobOnce(job)) {
        }
    }

    static {
        Timeouts.initDefault("WindowManager.TimeDelta", TIME_DELTA);
        manager = new WindowManager();
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut output) {
        this.output = output;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Adds job to list.
     *
     * @param job a job to perform.
     */
    public void add(WindowJob<?, Window> job) {
        output.printLine("Starting job \""
                + job.getDescription()
                + "\"");
        synchronized (jobs) {
            JobThread thread = new JobThread(job);
            jobs.add(thread);
            thread.start();
        }
    }

    /**
     * Removes job from list.
     *
     * @param job a job to remove.
     */
    public void remove(WindowJob<?, ?> job) {
        output.printLine("Killing job \""
                + job.getDescription()
                + "\"");
        synchronized (jobs) {
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get(i).job == job) {
                    jobs.get(i).needStop = true;
                    jobs.remove(i);
                    break;
                }
            }
        }
    }

    private boolean performJobOnce(WindowJob<?, Window> job) {
        Window win = WindowWaiter.getWindow(job);
        if (win != null) {
            job.launch(win);
            return true;
        } else {
            return false;
        }
    }

    public static class ModalDialogChoosingJob implements WindowJob<Void, Window> {

        @Override
        public boolean checkComponent(Component comp) {
            return (comp instanceof Dialog
                    && ((Dialog) comp).isModal());
        }

        @Override
        public Void launch(Window obj) {
            new WindowOperator(obj).requestCloseAndThenHide();
            return null;
        }

        @Override
        public String getDescription() {
            return "A job of closing modal dialogs";
        }

        @Override
        public String toString() {
            return "ModalDialogChoosingJob{description = " + getDescription() + '}';
        }
    }

    private static class JobThread extends Thread {

        WindowJob<?, Window> job;
        volatile boolean needStop = false;

        public JobThread(WindowJob<?, Window> job) {
            this.job = job;
        }

        private boolean getNS() {
            return needStop;
        }

        @Override
        public void run() {
            while (!getNS()) {
                manager.performJobOnce(job);
                manager.timeouts.sleep("WindowManager.TimeDelta");
            }
        }
    }

}
