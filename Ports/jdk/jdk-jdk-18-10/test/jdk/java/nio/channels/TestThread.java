/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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

/* Utility class for test threads
 *
 */

import java.io.*;


public abstract class TestThread
    extends Thread
{
    Exception failure = null;
    String name;
    protected final PrintStream log;
    Thread main;

    TestThread(String name, PrintStream log) {
        super("TestThread-" + name);
        this.name = name;
        this.log = log;
        this.main = Thread.currentThread();
        setDaemon(true);
    }

    TestThread(String name) {
        this(name, System.err);
    }

    abstract void go() throws Exception;

    public void run() {
        try {
            go();
        } catch (Exception x) {
            failure = x;
            main.interrupt();
        }
    }

    int finish(long timeout) {
        try {
            join(timeout);
        } catch (InterruptedException x) { }
        if (isAlive() && (failure == null))
            failure = new Exception(name + ": Timed out");
        if (failure != null) {
            failure.printStackTrace(log);
            return 0;
        }
        return 1;
    }

    void finishAndThrow(long timeout) throws Exception {
        try {
            join(timeout);
        } catch (InterruptedException x) { }
        if (failure != null)
            failure = new Exception(name + " threw an exception",
                                    failure);
        if (isAlive() && (failure == null))
            failure = new Exception(name + " timed out");
        if (failure != null)
            throw failure;
    }

    public String toString() {
        return name;
    }

}
