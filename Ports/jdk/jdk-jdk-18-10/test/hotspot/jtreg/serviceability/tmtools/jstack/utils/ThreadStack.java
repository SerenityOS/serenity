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

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * Represents the stack of the thread
 *
 */
public class ThreadStack {

    private String threadType; // Thread / RealtimeThread / NoHeapRealtimeThread
    private String threadName;
    private String type; //daemon or not
    private String priority;
    private String tid;
    private String nid;

    /**
     * runnable or waiting on condition
     */
    private String status;
    private String pointerRange;

    /**
     * i.e. java.lang.Thread.State: WAITING (on object monitor)
     */
    private String extendedStatus;

    private LinkedList<MethodInfo> stack = new LinkedList<MethodInfo>();

    private LinkedList<LockInfo> lockOSList = new LinkedList<LockInfo>();

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPointerRange() {
        return pointerRange;
    }

    public void setPointerRange(String pointerRange) {
        this.pointerRange = pointerRange;
    }

    public String getExtendedStatus() {
        return extendedStatus;
    }

    public void setExtendedStatus(String extendedStatus) {
        this.extendedStatus = extendedStatus;
    }

    public LinkedList<MethodInfo> getStack() {
        return stack;
    }

    public LinkedList<LockInfo> getLockOSList() {
        return lockOSList;
    }

    public void setLockOSList(LinkedList<LockInfo> lockOSList) {
        this.lockOSList = lockOSList;
    }

    public void addMethod(MethodInfo mi) {
        stack.add(mi);
    }

    public boolean hasEqualStack(ThreadStack another) {
        boolean result = true;

        Iterator<MethodInfo> it1 = stack.iterator();
        Iterator<MethodInfo> it2 = another.stack.iterator();

        while (it1.hasNext() && it2.hasNext()) {

            MethodInfo mi1 = it1.next();
            MethodInfo mi2 = it2.next();

            if (mi1 == null && mi2 == null) {
                break;
            }

            boolean oneOfMethodInfoIsNull = mi1 == null && mi2 != null || mi1 != null && mi2 == null;

            if (oneOfMethodInfoIsNull || !mi1.equals(mi2)) {
                result = false;
            }
        }

        if (it1.hasNext() || it2.hasNext()) {
            Utils.log("stack sizes", String.valueOf(stack.size()), String.valueOf(another.stack.size()));
            result = false;
        }

        return result;
    }

    public String getThreadType() {
        return threadType;
    }

    public void setThreadType(String threadType) {
        this.threadType = threadType;
    }

}
