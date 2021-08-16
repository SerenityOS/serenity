/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Accessible.modifiers;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the modifiers001 JDI test.
 */

public class modifiers001a {

    static boolean verbose_mode = false;

    // Classes must be loaded and linked, so all fields must be
    // initialized
    Boolean   Z0 = Boolean.valueOf(false);
    Byte      B0 = Byte.valueOf((byte)1);
    Character C0 = Character.valueOf('c');
    Double    D0 = Double.valueOf(1);
    Float     F0 = Float.valueOf(1);
    Integer   I0 = Integer.valueOf(1);
    Long      L0 = Long.valueOf(1);
    String    S0 = new String("s");
    Object    O0 = new Object();

    modifiers001 m001_0=new modifiers001();
    modifiers001a m001a_0, m001a_1[] = {m001a_0};

    final     static class  fin_s_cls {}
    abstract  static class  abs_s_cls {}
              static class  abs_s_cls_ext extends abs_s_cls {}
    static interface  s_interf {}
    static class s_interf_impl implements s_interf {}

    // Interfaces and abstract classes must be loaded and linked, so classes
    // that implement interfaces and extend abstract classes must be
    // initialized
    fin_s_cls fin_s_cls_0 = new fin_s_cls();
    abs_s_cls_ext abs_s_cls_ext_0 = new abs_s_cls_ext();
    abs_s_cls abs_s_cls_0, abs_s_cls_1[] = {abs_s_cls_0};
    s_interf_impl s_interf_impl_0 = new s_interf_impl();
    s_interf s_interf_0, s_interf_1[] = {s_interf_0};

    simple_class m_simpleclass_0 = new simple_class();
    abstract_class_ext m_absclass_ext_0 = new abstract_class_ext();
    abstract_class m_absclass_0, m_absclass_1[] = {m_absclass_0};
    final_class m_finclass_0 = new final_class();
    interf_impl m_interf_impl_0 = new interf_impl();
    interf m_interf_0, m_interf_1[] = {m_interf_0};

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

        print_log_on_verbose("**> modifiers001a: debugee started!");
        modifiers001a obj = new modifiers001a();
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        print_log_on_verbose("**> modifiers001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> modifiers001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> modifiers001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> modifiers001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> modifiers001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

/** simple class */
class simple_class {}

/** abstract class */
abstract class abstract_class {}

/** Class that extends abstract class */
class abstract_class_ext extends abstract_class {}

/** final class */
final class final_class {}

/** simple interface */
interface interf {}

/** Class that implements interface */
class interf_impl implements interf {}
