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

package nsk.jdi.ReferenceType.methodsByName_ss;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the methbyname_ss001 JDI test.
 */

public class methbyname_ss001a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.methodsByName_ss.";
//        package_prefix = "";    //  for DEBUG without package
    static String checked_class_name = package_prefix + "methbyname_ss001aClassForCheck";


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

        print_log_on_verbose("**> methbyname_ss001a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        Class checked_class_classobj = null;
        try {
            checked_class_classobj =
                Class.forName(checked_class_name, true, methbyname_ss001a.class.getClassLoader());
            print_log_on_verbose
                ("--> methbyname_ss001a: checked class loaded:" + checked_class_name);
        }
        catch ( Throwable thrown ) {  // ClassNotFoundException
//            System.err.println
//                ("**> methbyname_ss001a: load class: Throwable thrown = " + thrown.toString());
            print_log_on_verbose
                ("--> methbyname_ss001a: checked class NOT loaded: " + checked_class_name);
            // Debuuger finds this fact itself
        }

        print_log_on_verbose("**> methbyname_ss001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> methbyname_ss001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> methbyname_ss001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> methbyname_ss001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> methbyname_ss001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

abstract class methbyname_ss001aClassForCheck extends methbyname_ss001aSuperClassForCheck
    implements methbyname_ss001aInterfaceForCheck {

    // constructor
    public void ClassForCheck() {
    }

    // static methods without params
    static void s_void_method() {}
    static boolean s_boolean_method() {return true;}
    static byte    s_byte_method() {return (byte)1;}
    static char    s_char_method() {return (char)1;}
    static double  s_double_method() {return (double)100.99;}
    static float   s_float_method() {return (float)100.88;}
    static int     s_int_method() {return 100;}
    static long    s_long_method() {return 100;}
    static String  s_string_method() {return "return";}
    static Object  s_object_method() {return new Object();}
    static long[]  s_prim_array_method() {return new long[100];}
    static Object[]  s_ref_array_method() {return new Object[100];}

    static void   s_super_hidden_void_method() {}
    static int    s_super_hidden_prim_method() {return 100;}
    static Object s_super_hidden_ref_method() {return new Object();}

    // static methods with params
    static void s_void_par_method(boolean z) {}
    static boolean s_boolean_par_method(boolean z) {return true;}
    static byte    s_byte_par_method(byte b) {return (byte)1;}
    static char    s_char_par_method(char ch) {return (char)1;}
    static double  s_double_par_method(double d) {return (double)100.99;}
    static float   s_float_par_method(float f) {return (float)100.88;}
    static int     s_int_par_method(int i) {return 100;}
    static long    s_long_par_method(long l) {return 100;}
    static String  s_string_par_method(String s) {return "return";}
    static Object  s_object_par_method(Object obj) {return new Object();}
    static long[]  s_prim_array_par_method(long[] la) {return new long[100];}
    static Object[]  s_ref_array_par_method(Object[] obja) {return new Object[100];}

    static void   s_super_hidden_void_par_method(int i) {}
    static int    s_super_hidden_prim_par_method(int i) {return 100;}
    static Object s_super_hidden_ref_par_method(Object obj) {return new Object();}

    // other static methods
    native static Object  s_native_method(Object obj);
    static synchronized Object s_synchr_method(Object obj) {return new Object();}
    final static Object  s_final_method(Object obj) {return new Object();}
    private static Object  s_private_method(Object obj) {return new Object();}
    protected static Object  s_protected_method(Object obj) {return new Object();}
    public static Object  s_public_method(Object obj) {return new Object();}

    // instance methods without params
    void i_void_method() {}
    boolean i_boolean_method() {return true;}
    byte    i_byte_method() {return (byte)1;}
    char    i_char_method() {return (char)1;}
    double  i_double_method() {return (double)100.99;}
    float   i_float_method() {return (float)100.88;}
    int     i_int_method() {return 100;}
    long    i_long_method() {return 100;}
    String  i_string_method() {return "return";}
    Object  i_object_method() {return new Object();}
    long[]  i_prim_array_method() {return new long[100];}
    Object[]  i_ref_array_method() {return new Object[100];}

    void   i_super_overridden_void_method() {}
    int    i_super_overridden_prim_method() {return 100;}
    Object i_super_overridden_ref_method() {return new Object();}

    public void   i_interf_overridden_void_method() {}
    public int    i_interf_overridden_prim_method() {return 100;}
    public Object i_interf_overridden_ref_method() {return new Object();}

    // instance methods with params
    void i_void_par_method(boolean z) {}
    boolean i_boolean_par_method(boolean z) {return true;}
    byte    i_byte_par_method(byte b) {return (byte)1;}
    char    i_char_par_method(char ch) {return (char)1;}
    double  i_double_par_method(double d) {return (double)100.99;}
    float   i_float_par_method(float f) {return (float)100.88;}
    int     i_int_par_method(int i) {return 100;}
    long    i_long_par_method(long l) {return 100;}
    String  i_string_par_method(String s) {return "return";}
    Object  i_object_par_method(Object obj) {return new Object();}
    long[]  i_prim_array_par_method(long[] la) {return new long[100];}
    Object[]  i_ref_array_par_method(Object[] obja) {return new Object[100];}

    void   i_super_overridden_void_par_method(int i) {}
    int    i_super_overridden_prim_par_method(int i) {return 100;}
    Object i_super_overridden_ref_par_method(Object obj) {return new Object();}

    public void   i_interf_overridden_void_par_method(int i) {}
    public int    i_interf_overridden_prim_par_method(int i) {return 100;}
    public Object i_interf_overridden_ref_par_method(Object obj) {return new Object();}

    // other instance methods
    abstract Object  i_abstract_method(Object obj);
    native Object  i_native_method(Object obj);
    synchronized Object  i_synchr_method(Object obj) {return new Object();}
    final Object  i_final_method(Object obj) {return new Object();}
    private Object  i_private_method(Object obj) {return new Object();}
    protected Object  i_protected_method(Object obj) {return new Object();}
    public Object  i_public_method(Object obj) {return new Object();}


    // static initializer
    static {}

    // overloaded static methods
    static void  s_overloaded_method() {}
    static String  s_overloaded_method(String s) {return "string";}
    static Object  s_overloaded_method(Object obj) {return new Object();}
    static Object  s_overloaded_method(long l, String s) {return new Object();}

    static Object  s_super_overloaded_method(long l, String s) {return new Object();}

    // overloaded instance methods
    void  i_overloaded_method() {}
    long  i_overloaded_method(Object obj) {return (long)1;}
    String  i_overloaded_method(String s) {return "string";}
    Object  i_overloaded_method(long l, String s) {return new Object();}

    Object  i_super_overloaded_method(long l, String s) {return new Object();}
    Object  i_interf_overloaded_method(long l, String s) {return new Object();}


}

abstract class methbyname_ss001aSuperClassForCheck  {

    static void    s_super_void_method(long l) {}
    static long    s_super_prim_method(long l) {return 100;}
    static Object  s_super_ref_method(Object obj) {return new Object();}

    void    i_super_void_method(long l) {}
    long    i_super_prim_method(long l) {return 100;}
    Object  i_super_ref_method(Object obj) {return new Object();}

    static void   s_super_hidden_void_method() {}
    static int    s_super_hidden_prim_method() {return 100;}
    static Object s_super_hidden_ref_method() {return new Object();}

    static void   s_super_hidden_void_par_method(int i) {}
    static int    s_super_hidden_prim_par_method(int i) {return 100;}
    static Object s_super_hidden_ref_par_method(Object obj) {return new Object();}

    void   i_super_overridden_void_method() {}
    int    i_super_overridden_prim_method() {return 100;}
    Object i_super_overridden_ref_method() {return new Object();}

    void   i_super_overridden_void_par_method(int i) {}
    int    i_super_overridden_prim_par_method(int i) {return 100;}
    Object i_super_overridden_ref_par_method(Object obj) {return new Object();}

    public Object i_multiple_inherited_method(Object obj) {return new Object();}

    static Object  s_super_overloaded_method(long l) {return new Object();}
    Object  i_super_overloaded_method(long l) {return new Object();}

}

interface methbyname_ss001aInterfaceForCheck {

    void    i_interf_void_method(long l);
    long    i_interf_prim_method(long l);
    Object  i_interf_ref_method(Object obj);

    void   i_interf_overridden_void_method();
    int    i_interf_overridden_prim_method();
    Object i_interf_overridden_ref_method();

    void   i_interf_overridden_void_par_method(int i);
    int    i_interf_overridden_prim_par_method(int i);
    Object i_interf_overridden_ref_par_method(Object obj);

    public Object i_multiple_inherited_method(Object obj);

    Object  i_interf_overloaded_method(long l);

}
