/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework.driver;

import compiler.lib.ir_framework.IR;

import java.lang.reflect.Method;

/**
 * Helper class to store information about a method that needs to be IR matched.
 */
class IRMethod {
    private final Method method;
    private final int[] ruleIds;
    private final IR[] irAnnos;
    private final StringBuilder outputBuilder;
    private String output;
    private String idealOutput;
    private String optoAssemblyOutput;
    private boolean needsIdeal;
    private boolean needsOptoAssembly;

    public IRMethod(Method method, int[] ruleIds, IR[] irAnnos) {
        this.method = method;
        this.ruleIds = ruleIds;
        this.irAnnos = irAnnos;
        this.outputBuilder = new StringBuilder();
        this.output = "";
        this.idealOutput = "";
        this.optoAssemblyOutput = "";
    }

    public Method getMethod() {
        return method;
    }

    public int[] getRuleIds() {
        return ruleIds;
    }

    public IR getIrAnno(int idx) {
        return irAnnos[idx];
    }

    /**
     * The Ideal output comes always before the Opto Assembly output. We might parse multiple C2 compilations of this method.
     * Only keep the very last one by overriding 'output'.
     */
    public void setIdealOutput(String idealOutput) {
        outputBuilder.setLength(0);
        this.idealOutput = "PrintIdeal:" + System.lineSeparator() + idealOutput;
        outputBuilder.append(this.idealOutput);
    }

    /**
     * The Opto Assembly output comes after the Ideal output. Simply append to 'output'.
     */
    public void setOptoAssemblyOutput(String optoAssemblyOutput) {
        this.optoAssemblyOutput = "PrintOptoAssembly:" + System.lineSeparator() + optoAssemblyOutput;
        outputBuilder.append(System.lineSeparator()).append(System.lineSeparator()).append(this.optoAssemblyOutput);
        output = outputBuilder.toString();
    }

    public String getOutput() {
        return output;
    }

    public String getIdealOutput() {
        return idealOutput;
    }

    public String getOptoAssemblyOutput() {
        return optoAssemblyOutput;
    }

    public void needsAllOutput() {
        needsIdeal();
        needsOptoAssembly();
    }

    public void needsIdeal() {
        needsIdeal = true;
    }

    public boolean usesIdeal() {
        return needsIdeal;
    }

    public void needsOptoAssembly() {
        needsOptoAssembly = true;
    }

    public boolean usesOptoAssembly() {
        return needsOptoAssembly;
    }
}
