/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.isAbstract;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the isAbstract001 JDI test.
 */

public class isAbstract001a {

    static boolean verbose_mode = false;

    // Abstract classes must be extended by a class and that class must be
    // initialized, so that abstract classes could be returnedin debugger

    static class s_cls {}
    abstract static class abs_s_cls {}
    static class abs_s_cls_ext extends abs_s_cls {}
    abs_s_cls_ext abs_s_cls_ext_0 = new abs_s_cls_ext();
    static interface  s_interf {}
    static class s_interf_impl implements s_interf {}
    s_interf_impl s_interf_impl_0 = new s_interf_impl();

    s_cls s_cls_0=new s_cls();
    abs_s_cls abs_s_cls_0, abs_s_cls_1[]={abs_s_cls_0};
    s_interf s_interf_0, s_interf_1[]={s_interf_0};

    isAbstract001 a001_0=new isAbstract001();

    simple_cls simple_cls_0=new simple_cls();
    abstr_cls_ext abstr_cls_ext_0= new abstr_cls_ext();
    abstr_cls abstr_cls_0, abstr_cls_1[]={abstr_cls_0};
    interf_impl interf_impl_0= new interf_impl();
    interf interf_0, interf_1[]={interf_0};
    abstr_interf abstr_interf_0, abstr_interf_1[]={abstr_interf_0};
    abstr_interf_impl abstr_interf_impl_0= new abstr_interf_impl();

    private static void print_log_on_verbose(String message) {
        if ( verbose_mode ) {
            System.err.println(message);
        }
    }

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbose_mode = true;
                break;
            }
        }

        print_log_on_verbose("**> isAbstract001a: debugee started!");
        isAbstract001a isAbstract001a_obj = new isAbstract001a();
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        print_log_on_verbose("**> isAbstract001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> isAbstract001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> isAbstract001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> isAbstract001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> isAbstract001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

/** simple class */
class simple_cls {}

/** abstract class */
abstract class abstr_cls {}
class abstr_cls_ext extends abstr_cls {}

/** interface */
interface interf {}
class interf_impl implements interf {}

/** abstract interface */
abstract interface abstr_interf {}
class abstr_interf_impl implements abstr_interf {}
