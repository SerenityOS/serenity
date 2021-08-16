/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ReferenceType.defaultStratum.defaultStratum004;

import java.util.*;
import nsk.share.TestBug;
import nsk.share.jdi.sde.SDEDebuggee;

public class defaultStratum004a extends SDEDebuggee {
    public static void main(String[] args) {
        new defaultStratum004a().doTest(args);
    }

    // command:class_name class_name ...
    public static final String COMMAND_LOAD_TEST_CLASSES = "loadTestClasses";

    // command:class_name
    public static final String COMMAND_INSTANTIATE_TEST_CLASS = "instantiateTestClasses";

    List<Class> testClasses = new ArrayList<Class>();

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.startsWith(COMMAND_LOAD_TEST_CLASSES)) {
            String classNamesString[] = command.split(":");

            if ((classNamesString.length != 2) || (classNamesString[1].length() == 0))
                throw new TestBug("Invalid command format, class names was not specified");

            String classNames[] = classNamesString[1].split(" ");

            if (classNames.length == 0)
                throw new TestBug("Class names was not specified");

            loadTestClasses(classNames);

            return true;
        } else if (command.startsWith(COMMAND_INSTANTIATE_TEST_CLASS)) {
            String classNamesString[] = command.split(":");

            if ((classNamesString.length == 0) || (classNamesString[1].length() == 0))
                throw new TestBug("Class name was not specified");

            instantiateTestClass(classNamesString[1]);
            breakpointMethod();

            return true;
        }

        return false;
    }

    public void loadTestClasses(String classNames[]) {
        TestClassLoader classLoader = new TestClassLoader();
        classLoader.setClassPath(classpath);

        try {
            for (String className : classNames) {
                Class klass = classLoader.loadClass(className);
                testClasses.add(klass);

                log.display("Class '" + klass.getName() + "' was loaded");
            }
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());

            throw new TestBug("Unexpected exception: " + e);
        }
    }

    public void instantiateTestClass(String className) {
        for (Class klass : testClasses) {
            if (klass.getName().equals(className)) {
                try {
                    log.display("Create instance of '" + klass.getName() + "'");
                    klass.newInstance();
                } catch (Exception e) {
                    setSuccess(false);
                    log.complain("Unexpected exception: " + e);
                    e.printStackTrace(log.getOutStream());

                    throw new TestBug("Unexpected exception: " + e);
                }

                return;
            }
        }

        throw new TestBug("Class '" + className + "' was not loaded");
    }
}
