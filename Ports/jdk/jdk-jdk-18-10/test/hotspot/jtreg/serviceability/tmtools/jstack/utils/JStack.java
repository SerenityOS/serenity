/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package utils;

import java.util.HashMap;

/**
 *
 * Represents stack of all threads + some extra information
 *
 */
public class JStack {

    private String date;
    private String vmVersion;
    private HashMap<String, ThreadStack> threads = new HashMap<String, ThreadStack>();
    private String jniGlobalReferences;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public void setVmVersion(String vmVersion) {
        this.vmVersion = vmVersion;
    }

    public HashMap<String, ThreadStack> getThreads() {
        return threads;
    }

    public void setThreads(HashMap<String, ThreadStack> threads) {
        this.threads = threads;
    }

    public void addThreadStack(String threadName, ThreadStack ts) {
        System.out.println("Adding thread stack for thread: " + threadName);
        threads.put(threadName, ts);
    }

    public String getJniGlobalReferences() {
        return jniGlobalReferences;
    }

    public void setJniGlobalReferences(String jniGlobalReferences) {
        this.jniGlobalReferences = jniGlobalReferences;
    }

    public ThreadStack getThreadStack(String threadName) {
        return threads.get(threadName);
    }

}
