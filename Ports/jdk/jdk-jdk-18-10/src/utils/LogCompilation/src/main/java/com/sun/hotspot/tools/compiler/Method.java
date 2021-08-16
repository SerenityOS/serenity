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

import java.util.Arrays;

import static com.sun.hotspot.tools.compiler.Constants.*;

/**
 * Representation of a Java method in a compilation log.
 */
public class Method {

    /**
     * The name of the class holding the method.
     */
    private String holder;

    /**
     * The method's name.
     */
    private String name;

    /**
     * The return type of the method, as a fully qualified (source-level) class
     * or primitive type name.
     */
    private String returnType;

    /**
     * The method's signature, in internal form.
     */
    private String signature;

    /**
     * The length of the method's byte code.
     */
    private String bytes;

    /**
     * The number of times this method was invoked in the interpreter.
     */
    private String iicount;

    /**
     * The method's flags, in the form of a {@code String} representing the
     * {@code int} encoding them.
     */
    private String flags;

    /**
     * The name of the compiler performing this compilation.
     */
    private String compiler;

    /**
     * The nmethod's compilation level.
     */
    private long level;

    /**
     * Decode the {@link flags} numerical string to a format for console
     * output. The result does not honour all possible flags but includes
     * information about OSR compilation.
     *
     * @param osr_bci the byte code index at which an OSR compilation takes
     * place, or -1 if the compilation is not an OSR one.
     */
    String decodeFlags(int osr_bci) {
        int f = Integer.parseInt(getFlags());
        char[] c = new char[4];
        Arrays.fill(c, ' ');
        if (osr_bci >= 0) {
            c[0] = '%';
        }
        if ((f & JVM_ACC_SYNCHRONIZED) != 0) {
            c[1] = 's';
        }
        return new String(c);
    }

    /**
     * Format this method for console output.
     *
     * @param osr_bci the byte code index at which OSR takes place, or -1 if no
     * OSR compilation is going on.
     */
    String format(int osr_bci) {
        if (osr_bci >= 0) {
            return getHolder() + "::" + getName() + " @ " + osr_bci + " (" + getBytes() + " bytes)";
        } else {
            return getHolder() + "::" + getName() + " (" + getBytes() + " bytes)";
        }
    }

    @Override
    public String toString() {
        return getHolder() + "::" + getName() + " (" + getBytes() + " bytes)";
    }

    public String getFullName() {
        return getHolder().replace('/', '.') + "." + getName() + signature;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature.replace('/', '.');
    }

    public String getArguments() {
        return signature.substring(0, signature.indexOf(')') + 1);
    }

    public String getBytes() {
        return bytes;
    }

    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    public String getIICount() {
        return iicount;
    }

    public void setIICount(String iicount) {
        this.iicount = iicount;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Method) {
            Method other = (Method) o;
            return holder.equals(other.holder) && name.equals(other.name) && signature.equals(other.signature);
        }
        return false;
    }

    public int hashCode() {
        return holder.hashCode() ^ name.hashCode();
    }

    /**
     * @return the compiler
     */
    public String getCompiler() {
        return compiler;
    }

    /**
     * @param compiler the compiler to set
     */
    public void setCompiler(String compiler) {
        this.compiler = compiler;
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
        assert this.level == 0 || this.level == level;
        this.level = level;
    }
}
