/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8163969
 * @summary Test interface initialization states and when certain interfaces are initialized
 * in the presence of initialization errors.
 * @run main InterfaceInitializationStates
 */

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class InterfaceInitializationStates {

    static List<Class<?>> cInitOrder = new ArrayList<>();

    // K interface with a default method has an initialization error
    interface K {
        boolean v = InterfaceInitializationStates.out(K.class);
        static final Object CONST = InterfaceInitializationStates.someMethod();
        default int method() { return 2; }
    }

    // I is initialized when CONST is used, and doesn't trigger initialization of K,
    // I also doesn't get an initialization error just because K has an initialization error.
    interface I extends K {
        boolean v = InterfaceInitializationStates.out(I.class);
        static final Object CONST = InterfaceInitializationStates.someMethod();
    }

    // L can be fully initialized even though it extends an interface that has an
    // initialization error
    interface L extends K {
        boolean v = InterfaceInitializationStates.out(L.class);
        default void lx() {}
        static void func() {
            System.out.println("Calling function on interface with bad super interface.");
        }
    }

    // Another interface needing initialization.
    // Initialization of this interface does not occur with ClassLIM because K throws
    // an initialization error, so the interface initialization is abandoned
    interface M {
        boolean v = InterfaceInitializationStates.out(M.class);
        default void mx() {}
    }

    static class ClassLIM implements L, I, M {
        boolean v = InterfaceInitializationStates.out(ClassLIM.class);
        int callMethodInK() { return method(); }
        static {
            // Since interface initialization of K fails, this should never be called
            System.out.println("Initializing C, but L is still good");
            L.func();
        }
    }

    // Finally initialize M
    static class ClassM implements M {
        boolean v = InterfaceInitializationStates.out(ClassM.class);
    }

    // Iunlinked is testing initialization like interface I, except interface I is linked when
    // ClassLIM is linked.
    // Iunlinked is not linked already when K gets an initialization error.  Linking Iunlinked
    // should succeed because it does not depend on the initialization state of K for linking.
    interface Iunlinked extends K {
        boolean v = InterfaceInitializationStates.out(Iunlinked.class);
    }

    // More tests.  What happens if we use K for parameters and return types?
    // K is a symbolic reference in the constant pool and the initialization error only
    // matters when it's used.
    interface Iparams {
        boolean v = InterfaceInitializationStates.out(Iparams.class);
        K the_k = null;
        K m(K k); // abstract
        default K method() { return new K(){}; }
    }

    static class ClassIparams implements Iparams {
        boolean v = InterfaceInitializationStates.out(ClassIparams.class);
        public K m(K k) { return k; }
    }

    public static void main(java.lang.String[] unused) {
        // The rule this tests is the last sentence of JLS 12.4.1:

        // When a class is initialized, its superclasses are initialized (if they have not
        // been previously initialized), as well as any superinterfaces (s8.1.5) that declare any
        // default methods (s9.4.3) (if they have not been previously initialized). Initialization
        // of an interface does not, of itself, cause initialization of any of its superinterfaces.

        // Trigger initialization.
        // Now L is fully_initialized even though K should
        // throw an error during initialization.
        boolean v = L.v;
        L.func();

        try {
            ClassLIM c  = new ClassLIM();  // is K initialized, with a perfectly good L in the middle
            // was bug: this used to succeed and be able to callMethodInK().
            throw new RuntimeException("FAIL exception not thrown for class");
        } catch (ExceptionInInitializerError e) {
            System.out.println("ExceptionInInitializerError thrown as expected");
        }

        // Test that K already has initialization error so gets ClassNotFoundException because
        // initialization was attempted with ClassLIM.
        try {
            Class.forName("InterfaceInitializationStates$K", true, InterfaceInitializationStates.class.getClassLoader());
            throw new RuntimeException("FAIL exception not thrown for forName(K)");
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException should not be thrown");
        } catch(NoClassDefFoundError e) {
            System.out.println("NoClassDefFoundError thrown as expected");
        }

        new ClassM();

        // Initialize I, which doesn't cause K (super interface) to be initialized.
        // Since the initialization of I does _not_ cause K to be initialized, it does
        // not get NoClassDefFoundError because K is erroneous.
        // But the initialization of I throws RuntimeException, so we expect
        // ExceptionInInitializerError.
        try {
            Object ii = I.CONST;
            throw new RuntimeException("FAIL exception not thrown for I's initialization");
        } catch (ExceptionInInitializerError e) {
            System.out.println("ExceptionInInitializerError as expected");
        }

        // Initialize Iunlinked. No exception should be thrown even if K
        // (its super interface) is in initialization_error state.
        boolean bb = Iunlinked.v;

        // This should be okay
        boolean value = Iparams.v;
        System.out.println("value is " + value);

        ClassIparams p = new ClassIparams();
        try {
            // Now we get an error because K got an initialization_error
            K kk = p.method();
            throw new RuntimeException("FAIL exception not thrown for calling method for K");
        } catch(NoClassDefFoundError e) {
            System.out.println("NoClassDefFoundError thrown as expected");
        }

         // Check expected class initialization order
        List<Class<?>> expectedCInitOrder = Arrays.asList(L.class, K.class, M.class, ClassM.class,
                                                          I.class, Iunlinked.class, Iparams.class,
                                                          ClassIparams.class);
        if (!cInitOrder.equals(expectedCInitOrder)) {
            throw new RuntimeException(
                String.format("Class initialization array %s not equal to expected array %s",
                              cInitOrder, expectedCInitOrder));
        }
    }

    static boolean out(Class c) {
        System.out.println("#: initializing " + c.getName());
        cInitOrder.add(c);
        return true;
    }
    static Object someMethod() {
        throw new RuntimeException();
    }
}
