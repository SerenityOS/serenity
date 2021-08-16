/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * SwingTest is a utility class for writing regression tests
 * that require interacting with the UI.
 * It uses reflection to invoke all public methods without parameters.
 * All static methods are starting on the current thread.
 * Other methods including constructor are starting on the EDT.
 * Between each method invocation all pending events are processed.
 * The methods are sorted by name and invoked in that order.
 * Failure of the test is signaled by any method throwing an exception.
 * If no methods throw an exception the test is assumed to have passed.
 *
 * @author Sergey A. Malenkov
 */
final class SwingTest implements Runnable {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void start(Class<?> type) throws Throwable {
        new SwingTest(type).start();
    }

    private final Class<?> type;
    private final Iterator<Method> methods;

    private JFrame frame;
    private Object object;
    private Method method;
    private Throwable error;

    private SwingTest(Class<?> type) {
        Set<Method> methods = new TreeSet<Method>(new Comparator<Method>() {
            public int compare(Method first, Method second) {
                return first.getName().compareTo(second.getName());
            }
        });
        for (Method method : type.getMethods()) {
            if (method.getDeclaringClass().equals(type)) {
                if (method.getReturnType().equals(void.class)) {
                    if (0 == method.getParameterTypes().length) {
                        methods.add(method);
                    }
                }
            }
        }
        this.type = type;
        this.methods = methods.iterator();
    }

    public void run() {
        try {
            if (this.object == null) {
                System.out.println(this.type);
                this.frame = new JFrame(this.type.getSimpleName());
                this.frame.setSize(WIDTH, HEIGHT);
                this.frame.setLocationRelativeTo(null);
                this.object = this.type.getConstructor(this.frame.getClass()).newInstance(this.frame);
                this.frame.setVisible(true);
            }
            else if (this.method != null) {
                System.out.println(this.method);
                this.method.invoke(this.object);
            }
            else {
                System.out.println((this.error == null) ? "PASSED" : "FAILED"); // NON-NLS: debug
                this.frame.dispose();
                this.frame = null;
            }
        }
        catch (NoSuchMethodException exception) {
            this.error = exception;
        }
        catch (SecurityException exception) {
            this.error = exception;
        }
        catch (IllegalAccessException exception) {
            this.error = exception;
        }
        catch (IllegalArgumentException exception) {
            this.error = exception;
        }
        catch (InstantiationException exception) {
            this.error = exception;
        }
        catch (InvocationTargetException exception) {
            this.error = exception.getTargetException();
        }
        System.out.flush();
        this.method = this.methods.hasNext() && (this.error == null)
                ? this.methods.next()
                : null;
    }

    private void start() throws Throwable {
        do {
            if ((this.method != null) && Modifier.isStatic(this.method.getModifiers())) {
                run(); // invoke static method on the current thread
            }
            else {
                SwingUtilities.invokeLater(this); // invoke on the event dispatch thread
            }
            java.awt.Robot robot = new java.awt.Robot();
            robot.waitForIdle();
        }
        while (this.frame != null);
        if (this.error != null) {
            throw this.error;
        }
    }
}
