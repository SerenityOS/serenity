/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 5009601 5010455 5005748
 * @summary enum constructors can be declared private
 * @author Joseph D. Darcy
 */

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

/*
 * Arguably, only the final and abstract should be held in
 * ExpectedModifiers; whether or not an enum should be static could be
 * inferred from getDeclaringClass and working versions of
 * getEnclosingMethod and getEnclosingConstructor.  I.e. if
 * getDeclaringClass, getEnclosingMethod, and getEnclosingConstructor
 * were all null, the enum is a top-level class and should not be
 * static; otherwise, it should be static.
 */

@ExpectedModifiers(Modifier.FINAL)
public enum EnumImplicitPrivateConstructor {
    RED(255, 0, 0),
    GREEN(0, 255, 0),
    BLUE(0, 0, 255);

    private int r, g, b;
    EnumImplicitPrivateConstructor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /*
     * Using reflection, Verify that
     * 1. all non-synthetic constructors of enum classes are marked as private.
     * 2. top-level enum classes are marked as static
     * 3. enum's are marked final and abstract as appropriate
     * 4. enum constructors *cannot* be invoked reflectively
     */
    public static void main(String argv[]) throws Exception {
        boolean passed = true;

        Collection<Class> classes = new LinkedHashSet<Class>();

        classes.add(Class.forName("EnumImplicitPrivateConstructor"));
        classes.add(Class.forName("EnumImplicitPrivateConstructor$AnotherEnum"));
        classes.add(Class.forName("EnumImplicitPrivateConstructor$YetAnotherEnum"));
        classes.add(Class.forName("EnumImplicitPrivateConstructor$OneMoreEnum"));

        // Add classes of specialized enum constants
        for(Enum e: YetAnotherEnum.values())
            classes.add(e.getClass());

        for(Class clazz: classes) {
            System.out.println("Testing class " + clazz);

            int classModifiers = clazz.getModifiers();

            // Why is this cast needed?
            ExpectedModifiers em = (ExpectedModifiers)clazz.getAnnotation(ExpectedModifiers.class);
            if (em != null) {
                System.out.println("\tTesting expected modifiers");
                int expected = em.value();

                if (expected != (classModifiers & (Modifier.ABSTRACT|Modifier.FINAL|Modifier.STATIC))) {
                    passed = false;
                    System.out.println("\tFAILED: Expected 0x" + Integer.toHexString(expected) +
                                       " got 0x" +Integer.toHexString(classModifiers));
                }
            }

            for(Constructor ctor: clazz.getDeclaredConstructors() ) {
                System.out.println("\tTesting constructor " + ctor);

                // We don't need no stinkin' access rules
                try {
                    ctor.setAccessible(true);
                } catch (java.security.AccessControlException ex) {
                }

                int modifiers = ctor.getModifiers();

                /*
                 * If clazz is for a specialized enum constant, the
                 * class will have the ENUM bit set but clazz.isEnum()
                 * will be false.  A constructor in such a class must
                 * be non-private to allow the parent class to call
                 * the constructor.  Therefore, only impose the
                 * private constructor check for genuine isEnum
                 * classes.
                 */
                if (clazz.isEnum()) {
                    if ((modifiers & Modifier.PRIVATE) == 0 &&
                        ! ctor.isSynthetic() ) {
                        passed = false;
                        System.out.println("\tFAILED: Constructor not marked private: modifiers 0x" +
                                           Integer.toHexString(modifiers));
                    }
                }

                try {
                    // Should get exception trying to invoke
                    Object o = null;
                    try {
                        o = ctor.newInstance("abc", 123);
                    } catch (IllegalAccessException ex) {
                    }

                    /*
                     * A better test would query the number (and type)
                     * of parameters and create an appropriate
                     * argument list since IllegalArgumentException can be
                     * thrown for just using the wrong number of arguments.
                     */

                    if (o != null) {
                        passed = false;
                        System.err.println("Error: Created new enum object!");
                        System.err.println(o.getClass());
                        System.err.println(o.toString());
                    }
                } catch (IllegalArgumentException iae) {}

            }
        }

        if (!passed)
            throw new RuntimeException("Error during testing.");
    }


    /*
     * Should be final and not abstract.
     */
    @ExpectedModifiers(Modifier.FINAL|Modifier.STATIC)
    enum AnotherEnum {
        YELLOW,
        CYAN,
        MAGENTA;
    }

    /*
     * Should be neither final nor abstract.
     */
    @ExpectedModifiers(Modifier.STATIC)
    enum YetAnotherEnum {
        GREEN {
            int value(){ return 1;}
        },

        ORANGE {
            int value(){ return 2;}
        },

        VIOLET {
            int value(){ return 3;}
        };

        int value(){ return 0;}
    }

    /*
     * Should be abstract and not final.
     */
    @ExpectedModifiers(Modifier.ABSTRACT|Modifier.STATIC)
    enum OneMoreEnum {
        SANGUINE {
            int value(){ return 1;}
        },

        VERDANT {
            int value(){ return 2;}
        },

        CERULEAN {
            int value(){ return 3;}
        };

        abstract int value();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedModifiers {
    int value();
}
