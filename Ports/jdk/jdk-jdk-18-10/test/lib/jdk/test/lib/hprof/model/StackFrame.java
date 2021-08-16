/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

/**
 *
 * @author      Bill Foote
 */


/**
 * Represents a stack frame.
 */

public class StackFrame {

    //
    // Values for the lineNumber data member.  These are the same
    // as the values used in the JDK 1.2 heap dump file.
    //
    public final static int LINE_NUMBER_UNKNOWN = -1;
    public final static int LINE_NUMBER_COMPILED = -2;
    public final static int LINE_NUMBER_NATIVE = -3;

    private String methodName;
    private String methodSignature;
    private String className;
    private String sourceFileName;
    private int lineNumber;

    public StackFrame(String methodName, String methodSignature,
                      String className, String sourceFileName, int lineNumber) {
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.className = className;
        this.sourceFileName = sourceFileName;
        this.lineNumber = lineNumber;
    }

    public void resolve(Snapshot snapshot) {
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getClassName() {
        return className;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getLineNumber() {
        switch(lineNumber) {
            case LINE_NUMBER_UNKNOWN:
                return "(unknown)";
            case LINE_NUMBER_COMPILED:
                return "(compiled method)";
            case LINE_NUMBER_NATIVE:
                return "(native method)";
            default:
                return Integer.toString(lineNumber, 10);
        }
    }
}
