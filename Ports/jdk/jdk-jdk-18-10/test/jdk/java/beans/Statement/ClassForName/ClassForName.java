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
 */

import java.beans.Expression;
import java.beans.Statement;

/**
 * @test
 * @bug 8146313
 * @run main/othervm ClassForName
 * @run main/othervm/policy=java.policy -Djava.security.manager ClassForName
 */
public final class ClassForName {

    static boolean initialized;

    static final String[] classes = {
            "A.A", "java.lang.String", "ClassForName$Bean", "sun.awt.SunToolkit"
    };

    static final ClassLoader appl = new Object() {}.getClass().getClassLoader();

    static final ClassLoader[] loaders = {
            String.class.getClassLoader(), null, appl
    };

    static boolean[] inits = {false, true};

    public static void main(final String[] args) throws Exception {
        // Check that the Class.forName(name, boolean, classloader) is executed
        // when requested via JavaBeans
        simpleTest();

        // Check that the Class.forName and Expression returns the same classes
        for (final String cls : classes) {
            complexTest1Args(cls);
            for (final ClassLoader loader : loaders) {
                for (final boolean init : inits) {
                    complexTest3Args(cls, loader, init);
                }
            }
        }
    }

    private static void simpleTest() throws Exception {
        // load the class without initialization
        new Statement(Class.class, "forName", new Object[]{
                "ClassForName$Bean", false, Bean.class.getClassLoader()
        }).execute();
        if (initialized) {
            throw new RuntimeException("Should not be initialized");
        }

        // load the class and initialize it
        new Statement(Class.class, "forName", new Object[]{
                "ClassForName$Bean", true, Bean.class.getClassLoader()
        }).execute();
        if (!initialized) {
            throw new RuntimeException("Should be initialized");
        }
    }

    private static void complexTest1Args(final String cls) {
        // load via standard Class.forName();
        Class<?> classForName = null;
        try {
            classForName = Class.forName(cls);
        } catch (final Exception ignored) {
        }

        // load via Expression.execute()
        Class<?> classStatement = null;
        try {
            final Expression exp = new Expression(Class.class, "forName",
                                                  new Object[]{
                                                          cls
                                                  });
            exp.execute();
            classStatement = (Class<?>) exp.getValue();
        } catch (final Exception ignored) {
        }
        if (classForName != classStatement) {
            System.err.println(classForName);
            System.err.println(classStatement);
            throw new RuntimeException();
        }
    }

    private static void complexTest3Args(final String cls,
                                         final ClassLoader loader,
                                         final boolean init) {
        // load via standard Class.forName();
        Class<?> classForName = null;
        Class<?> excForName = null;
        try {
            classForName = Class.forName(cls, init, loader);
        } catch (final Exception e) {
            excForName = e.getClass();
        }

        // load via Expression.execute()
        Class<?> classStatement = null;
        Class<?> excStatement = null;
        try {
            final Expression exp = new Expression(Class.class, "forName",
                                                  new Object[]{
                                                          cls, init, loader
                                                  });
            exp.execute();
            classStatement = (Class<?>) exp.getValue();
        } catch (final Exception e) {
            excStatement = e.getClass();
        }
        if (classForName != classStatement) {
            System.err.println(classForName);
            System.err.println(classStatement);
            throw new RuntimeException();
        }
        if (excForName != excStatement) {
            System.err.println(excForName);
            System.err.println(excStatement);
            throw new RuntimeException();
        }
    }

    public static final class Bean {

        static {
            initialized = true;
        }
    }
}
