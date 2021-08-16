/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package com.sun.hotspot.tools.compiler;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a compilation scope in a compilation log. This class is a
 * hybrid: its instances can represent original scopes of methods being
 * compiled, but are also used to represent call sites in given methods.
 */
public class CallSite {

    /**
     * The index of the call in the caller. This will be 0 if this instance
     * represents a compilation root.
     */
    private int bci;

    /**
     * The method that is called at this call site. This will be {@code null}
     * if this instance represents a compilation root.
     */
    private Method method;

    /**
     * The invocation count for this call site.
     */
    private int count;

    /**
     * The receiver type of the call represented by this instance, if known.
     */
    private String receiver;

    /**
     * In case the {@linkplain receiver receiver type} of the call represented
     * by this instance is known, this is how often the type was encountered.
     */
    private int receiver_count;

    /**
     * The reason for a success or failure of an inlining operation at this
     * call site.
     */
    private String reason;

    /**
     * A list of all calls in this compilation scope.
     */
    private List<CallSite> calls;

    /**
     * Number of nodes in the graph at the end of parsing this compilation
     * scope.
     */
    private int endNodes;

    /**
     * Number of live nodes in the graph at the end of parsing this compilation
     * scope.
     */
    private int endLiveNodes;

    /**
     * Time in seconds since VM startup at which parsing this compilation scope
     * ended.
     */
    private double timeStamp;

    /**
     * The inline ID in case the call represented by this instance is inlined,
     * 0 otherwise.
     */
    private long inlineId;

    /**
     * List of uncommon traps in this compilation scope.
     */
    private List<UncommonTrap> traps;

    /**
     * The name of the intrinsic at this call site.
     */
    private String intrinsicName;

    /**
     * Default constructor: used to create an instance that represents the top
     * scope of a compilation.
     */
    CallSite() {}

    /**
     * Constructor to create an instance that represents an actual method call.
     */
    CallSite(int bci, Method m) {
        this.bci = bci;
        this.method = m;
    }

    /**
     * Add a call site to the compilation scope represented by this instance.
     */
    void add(CallSite site) {
        if (getCalls() == null) {
            calls = new ArrayList<>();
        }
        getCalls().add(site);
    }

    /**
     * Return the last of the {@linkplain #getCalls() call sites} in this
     * compilation scope.
     */
    CallSite last() {
        return getCalls().get(getCalls().size() - 1);
    }

    /**
     * Return the last-but-one of the {@linkplain #getCalls() call sites} in
     * this compilation scope.
     */
    CallSite lastButOne() {
        return getCalls().get(getCalls().size() - 2);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getReason() == null) {
            sb.append("  @ " + getBci() + " " + getMethod());
        } else {
            sb.append("- @ " + getBci() + " " + getMethod() + " " + getReason());
        }
        sb.append("\n");
        if (getCalls() != null) {
            for (CallSite site : getCalls()) {
                sb.append(site);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void print(PrintStream stream) {
        print(stream, 0, true, false);
    }

    void emit(PrintStream stream, int indent) {
        for (int i = 0; i < indent; i++) {
            stream.print(' ');
        }
    }

    public void print(PrintStream stream, int indent, boolean printInlining, boolean printUncommonTraps) {
        emit(stream, indent);
        String m = getMethod().getHolder() + "::" + getMethod().getName();
        if (getReason() == null) {
            stream.print("  @ " + getBci() + " " + m + " (" + getMethod().getBytes() + " bytes)");
        } else {
            stream.print("  @ " + getBci() + " " + m + " " + getReason());
        }
        stream.print(getIntrinsicOrEmptyString());
        if (LogCompilation.compare == false) {
            // The timestamp is not useful for log comparison
            stream.printf(" (end time: %6.4f", getTimeStamp());
        }
        if (getEndNodes() > 0) {
            stream.printf(" nodes: %d live: %d", getEndNodes(), getEndLiveNodes());
        }
        stream.println(")");

        if (getReceiver() != null) {
            emit(stream, indent + 4);
            stream.println("type profile " + getMethod().getHolder() + " -> " + getReceiver() + " (" +
                    (getReceiverCount() * 100 / getCount()) + "%)");
        }
        if (printInlining && getCalls() != null) {
            for (CallSite site : getCalls()) {
                site.print(stream, indent + 2, printInlining, printUncommonTraps);
            }
        }
        if (printUncommonTraps && getTraps() != null) {
            for (UncommonTrap site : getTraps()) {
                site.print(stream, indent + 2);
            }
        }
    }

    public int getBci() {
        return bci;
    }

    public void setBci(int bci) {
        this.bci = bci;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public int getReceiverCount() {
        return receiver_count;
    }

    public void setReceiver_count(int receiver_count) {
        this.receiver_count = receiver_count;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<CallSite> getCalls() {
        return calls;
    }

    public List<UncommonTrap> getTraps() {
        return traps;
    }

    void add(UncommonTrap e) {
        if (traps == null) {
            traps = new ArrayList<UncommonTrap>();
        }
        traps.add(e);
    }

    void setEndNodes(int n) {
        endNodes = n;
    }

    public int getEndNodes() {
        return endNodes;
    }

    void setEndLiveNodes(int n) {
        endLiveNodes = n;
    }

    public int getEndLiveNodes() {
        return endLiveNodes;
    }

    void setTimeStamp(double time) {
        timeStamp = time;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    /**
     * Check whether this call site matches another. Every late inline call
     * site has a unique inline ID. If the call site we're looking for has one,
     * then use it; otherwise rely on method name and byte code index.
     */
    private boolean matches(CallSite other) {
        if (other.inlineId != 0) {
            return inlineId == other.inlineId;
        }
        return method.equals(other.method) && bci == other.bci;
    }

    /**
     * Locate a late inline call site: find, in this instance's
     * {@linkplain #calls call sites}, the one furthest down the given call
     * stack.
     *
     * Multiple chains of identical call sites with the same method name / bci
     * combination are possible, so we have to try them all until we find the
     * late inline call site that has a matching inline ID.
     *
     * @return a matching call site, or {@code null} if none was found.
     */
    public CallSite findCallSite(ArrayDeque<CallSite> sites) {
        if (calls == null) {
            return null;
        }
        CallSite site = sites.pop();
        for (CallSite c : calls) {
            if (c.matches(site)) {
                if (!sites.isEmpty()) {
                    CallSite res = c.findCallSite(sites);
                    if (res != null) {
                        sites.push(site);
                        return res;
                    }
                } else {
                    sites.push(site);
                    return c;
                }
            }
        }
        sites.push(site);
        return null;
    }

    /**
     * Locate a late inline call site in the tree spanned by all this instance's
     * {@linkplain #calls call sites}, and return the sequence of call sites
     * (scopes) leading to that late inline call site.
     */
    public ArrayDeque<CallSite> findCallSite2(CallSite site) {
        if (calls == null) {
            return null;
        }

        for (CallSite c : calls) {
            if (c.matches(site)) {
                ArrayDeque<CallSite> stack = new ArrayDeque<>();
                stack.push(c);
                return stack;
            } else {
                ArrayDeque<CallSite> stack = c.findCallSite2(site);
                if (stack != null) {
                    stack.push(c);
                    return stack;
                }
            }
        }
        return null;
    }

    public long getInlineId() {
        return inlineId;
    }

    public void setInlineId(long inlineId) {
        this.inlineId = inlineId;
    }

    public String getIntrinsicName() {
        return intrinsicName;
    }

    public void setIntrinsicName(String name) {
        this.intrinsicName = name;
    }

    public String getIntrinsicOrEmptyString() {
        if (intrinsicName != null) {
            return " (intrinsic: " + getIntrinsicName() + ")";
        }
        return "";
    }
}
