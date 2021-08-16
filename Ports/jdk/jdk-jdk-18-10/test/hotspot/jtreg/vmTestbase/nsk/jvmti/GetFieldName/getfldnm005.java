/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetFieldName;

import java.io.*;
import java.util.*;

import nsk.share.*;

/**
 * This test checks that the JVMTI function <code>GetFieldName()</code>
 * returns generic signature information properly.<br>
 * Debuggee part of the test contains several tested fields. Some of
 * them are generic. Agent part obtains their signatures. Proper generic
 * signature should be returned for the generic fields, or NULL for
 * non-generic ones.
 */
public class getfldnm005 {
    static {
        try {
            System.loadLibrary("getfldnm005");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"getfldnm005\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    // dummy fields used for testing
    static getfldnm005 _getfldnm005St =
        new getfldnm005();
    getfldnm005b<String> _getfldnm005b =
        new getfldnm005b<String>();
    static getfldnm005b<String> _getfldnm005bSt =
        new getfldnm005b<String>();
    getfldnm005c<Boolean, Integer> _getfldnm005c =
        new getfldnm005c<Boolean, Integer>();
    static getfldnm005c<Boolean, Integer> _getfldnm005cSt =
        new getfldnm005c<Boolean, Integer>();
    getfldnm005e _getfldnm005e =
        new getfldnm005e();
    static getfldnm005e _getfldnm005eSt =
        new getfldnm005e();
    getfldnm005if<Object> _getfldnm005if =
        new getfldnm005d<Object>();
    static getfldnm005if<Object> _getfldnm005ifSt =
        new getfldnm005d<Object>();
    getfldnm005g<getfldnm005f> _getfldnm005g =
        new getfldnm005g<getfldnm005f>();
    static getfldnm005g<getfldnm005f> _getfldnm005gSt =
        new getfldnm005g<getfldnm005f>();
    getfldnm005g[] _getfldnm005gArr =
        new getfldnm005g[]{};

    native int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getfldnm005().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        return check();
    }
}

/*
 * Dummy classes used only for verifying generic signature information
 * in an agent.
 */

class getfldnm005b<L extends String> {}

class getfldnm005c<A, B extends Integer> {}

interface getfldnm005if<I> {}

class getfldnm005d<T> implements getfldnm005if<T> {}

class getfldnm005e {}

class getfldnm005f extends getfldnm005e implements getfldnm005if {}

class getfldnm005g<E extends getfldnm005e & getfldnm005if> {}
