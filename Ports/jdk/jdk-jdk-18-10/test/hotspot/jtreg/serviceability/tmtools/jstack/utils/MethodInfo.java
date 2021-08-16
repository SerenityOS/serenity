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

import java.util.LinkedList;

/**
 *
 * Represents method info string
 *
 */
public class MethodInfo {

    private String name;
    private String compilationUnit;
    private String args;
    private String bci;
    private String line;
    private String frameType;

    private LinkedList<MonitorInfo> locks = new LinkedList<MonitorInfo>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompilationUnit() {
        return compilationUnit;
    }

    public void setCompilationUnit(String compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getBci() {
        return bci;
    }

    public void setBci(String bci) {
        this.bci = bci;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getFrameType() {
        return frameType;
    }

    public void setFrameType(String frameType) {
        this.frameType = frameType;
    }

    public LinkedList<MonitorInfo> getLocks() {
        return locks;
    }

    public void setLocks(LinkedList<MonitorInfo> locks) {
        this.locks = locks;
    }

    public boolean equals(MethodInfo another) {

        boolean result = true;

        if (!Utils.compareStrings(name, another.name)) {
            Utils.log("name", name, another.name);
            result = false;
        }

        if (!Utils.compareStrings(compilationUnit, another.compilationUnit)) {
            Utils.log("compilationUnit", compilationUnit, another.compilationUnit);
            result = false;
        }

        /*
         if (!Utils.compareStrings(args, another.args)) {
         Utils.log("args", args, another.args);
         result = false;
         }

         if (!Utils.compareStrings(bci, another.bci)) {
         Utils.log("bci", bci, another.bci);
         result = false;
         }

         if (!Utils.compareStrings(frameType, another.frameType)) {
         Utils.log("frameType", frameType, another.frameType);
         result = false;
         }
         */
        if (!Utils.compareStrings(line, another.line)) {
            Utils.log("line", line, another.line);
            result = false;
        }

        return result;
    }

}
