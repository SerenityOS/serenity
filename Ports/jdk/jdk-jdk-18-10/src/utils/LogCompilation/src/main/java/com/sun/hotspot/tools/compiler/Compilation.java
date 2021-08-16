/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;

/**
 * One particular compilation, represented in the compilation log file as a
 * {@code task} element.
 */
public class Compilation implements LogEvent {

    /**
     * The compilation ID.
     */
    private int id;

    /**
     * Whether this is a compilation for on-stack replacement (OSR).
     */
    private boolean osr;

    /**
     * The method being compiled.
     */
    private Method method;

    /**
     * The {@linkplain CallSite scope} of this compilation. This is created as
     * an empty {@link CallSite} instance, to be filled with data (and
     * meaning) later on.
     */
    private CallSite call = new CallSite();

    /**
     * In case a {@code late_inline} event occurs during the compilation, this
     * field holds the information about it.
     */
    private CallSite lateInlineCall = new CallSite();

    /**
     * The bytecode instruction index for on-stack replacement compilations; -1
     * if this is not an OSR compilation.
     */
    private int bci;

    /**
     * The method under compilation's invocation count.
     */
    private String icount;

    /**
     * The method under compilation's backedge count.
     */
    private String bcount;

    /**
     * Additional information for special compilations (e.g., adapters).
     */
    private String special;

    /**
     * The compilation level for this task.
     */
    private long level;

    /**
     * Start time stamp.
     */
    private double start;

    /**
     * End time stamp.
     */
    private double end;

    /**
     * Trip count of the register allocator.
     */
    private int attempts;

    /**
     * The compilation result (a native method).
     */
    private NMethod nmethod;

    /**
     * The phases through which this compilation goes.
     */
    private ArrayList<Phase> phases = new ArrayList<>(4);

    /**
     * In case this compilation fails, the reason for that.
     */
    private String failureReason;

    Compilation(int id) {
        this.id = id;
    }

    void reset() {
        call = new CallSite();
        lateInlineCall = new CallSite();
        phases = new ArrayList<>(4);
    }

    /**
     * Get a compilation phase by name, or {@code null}.
     *
     * @param s the name of the phase to retrieve in this compilation.
     *
     * @return a compilation phase, or {@code null} if no phase with the given
     *         name is found.
     */
    Phase getPhase(String s) {
        for (Phase p : getPhases()) {
            if (p.getName().equals(s)) {
                return p;
            }
        }
        return null;
    }

    double getRegallocTime() {
        return getPhase("regalloc").getElapsedTime();
    }

    public double getStart() {
        return start;
    }

    public String getCompiler() {
        assert getNMethod() != null  || getFailureReason() != null : "Null nmethod for Compilation:" + getId() + " " + getMethod();
        if (getNMethod() != null) {
            getNMethod().getCompiler();
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getId());
        sb.append(" ");
        sb.append(getCompiler());
        sb.append(" ");
        sb.append(getMethod());
        sb.append(" ");
        sb.append(getIcount());
        sb.append("+");
        sb.append(getBcount());
        sb.append("\n");
        if (getCall() != null && getCall().getCalls() != null) {
            for (CallSite site : getCall().getCalls()) {
                sb.append(site);
                sb.append("\n");
            }
        }
        if (getLateInlineCall().getCalls() != null) {
            sb.append("late inline:\n");
            for (CallSite site : getLateInlineCall().getCalls()) {
                sb.append(site);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void printShort(PrintStream stream) {
        if (getMethod() == null) {
            stream.println(getSpecial());
        } else {
            int bc = isOsr() ? getBCI() : -1;
            stream.print(getId() + getMethod().decodeFlags(bc) + " " + getCompiler() + " " + getMethod().format(bc));
        }
    }

    public void print(PrintStream stream, boolean printID) {
        print(stream, 0, printID, true, false);
    }

    public void print(PrintStream stream, boolean printID, boolean printInlining) {
        print(stream, 0, printID, printInlining, false);
    }

    public void print(PrintStream stream, boolean printID, boolean printInlining, boolean printUncommonTraps) {
        print(stream, 0, printID, printInlining, printUncommonTraps);
    }

    public void print(PrintStream stream, int indent, boolean printID, boolean printInlining, boolean printUncommonTraps) {
        if (getMethod() == null) {
            stream.println(getSpecial());
        } else {
            if (printID) {
                stream.print(getId());
                // Print the comp level next to the id as with +PrintCompilation
                if (nmethod != null && nmethod.getLevel() != 0) {
                    stream.print(" " + nmethod.getLevel());
                }
            }

            String codeSize = "";
            if (nmethod != null) {
                long nmethodSize = nmethod.getInstSize();
                if (nmethodSize > 0) {
                    codeSize = "(code size: " + nmethodSize + ")";
                }
            }

            int bc = isOsr() ? getBCI() : -1;
            stream.print(getMethod().decodeFlags(bc) + " " + getCompiler() + " " + getMethod().format(bc) + codeSize);
            stream.println();
            if (getFailureReason() != null) {
                stream.println("COMPILE SKIPPED: " + getFailureReason() + " (not retryable)");
            }
            if (printInlining && call.getCalls() != null) {
                for (CallSite site : call.getCalls()) {
                    site.print(stream, indent + 2, printInlining, printUncommonTraps);
                }
            }
            if (printUncommonTraps && call.getTraps() != null) {
                for (UncommonTrap site : call.getTraps()) {
                    site.print(stream, indent + 2);
                }
            }
            if (printInlining && lateInlineCall.getCalls() != null) {
                stream.println("late inline:");
                for (CallSite site : lateInlineCall.getCalls()) {
                    site.print(stream, indent + 2, printInlining, printUncommonTraps);
                }
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOsr() {
        return osr;
    }

    public void setOsr(boolean osr) {
        this.osr = osr;
    }

    public int getBCI() {
        return bci;
    }

    public void setBCI(int osrBci) {
        this.bci = osrBci;
    }

    public String getIcount() {
        return icount;
    }

    public void setICount(String icount) {
        this.icount = icount;
    }

    public String getBcount() {
        return bcount;
    }

    public void setBCount(String bcount) {
        this.bcount = bcount;
    }

    public String getSpecial() {
        return special;
    }

    public void setSpecial(String special) {
        this.special = special;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public NMethod getNMethod() {
        return nmethod;
    }

    public void setNMethod(NMethod NMethod) {
        this.nmethod = NMethod;
    }

    public ArrayList<Phase> getPhases() {
        return phases;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Set the method under compilation. If it is already set, ignore the
     * argument to avoid changing the method by post-parse inlining info.
     *
     * @param method the method under compilation. May be ignored.
     */
    public void setMethod(Method method) {
        if (getMethod() == null) {
            this.method = method;
        }
    }

    public CallSite getCall() {
        return call;
    }

    public CallSite getLateInlineCall() {
        return lateInlineCall;
    }

    public double getElapsedTime() {
        return end - start;
    }

    public Compilation getCompilation() {
        return this;
    }

    /**
     * @return the level
     */
    public long getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(long level) {
        this.level = level;
        if (getMethod() != null) {
            getMethod().setLevel(level);
        }
    }
}
