/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.fields.fields001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class fields001a {

    /* TEST DEPENDANT VARIABLES AND CONSTANTS */
    static final String PACKAGE_NAME = "nsk.jdb.fields.fields001";

    public static void main(String args[]) {
       fields001a _fields001a = new fields001a();
       System.exit(fields001.JCK_STATUS_BASE + _fields001a.runIt(args, System.out));
    }


    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        lastBreak();

        log.display("Debuggee PASSED");
        return fields001.PASSED;
    }

    static     int i_st;
    private    int i_pv;
    protected  int i_pt;
    public     int i_pb;
    final      int i_fn = 0;;
    transient  int i_tr;
    volatile   int i_vl;
    int []      i_a;
    int [][]    i_aa;
    int [][][]  i_aaa;

    static     Object o_st;
    private    Object o_pv;
    protected  Object o_pt;
    public     Object o_pb;
    final      Object o_fn = new Object();
    transient  Object o_tr;
    volatile   Object o_vl;
    Object []     o_a;
    Object [][]   o_aa;
    Object [][][] o_aaa;

    class Inner {
        private    int ii_pv;
        protected  int ii_pt;
        public     int ii_pb;
        final      int ii_fn = 0;;
        transient  int ii_tr;
        volatile   int ii_vl;
        int []      ii_a;
        int [][]    ii_aa;
        int [][][]  ii_aaa;

        private    Object oi_pv;
        protected  Object oi_pt;
        public     Object oi_pb;
        final      Object oi_fn = new Object();
        transient  Object oi_tr;
        volatile   Object oi_vl;
        Object []     oi_a;
        Object [][]   oi_aa;
        Object [][][] oi_aaa;
    }

    class Extender extends Inner {};

    Inner inner;
    Extender extender;

    public fields001a() {
        inner = new Inner();
        extender = new Extender();
    }
}
