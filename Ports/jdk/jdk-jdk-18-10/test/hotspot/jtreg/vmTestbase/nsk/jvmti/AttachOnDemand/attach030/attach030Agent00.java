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
package nsk.jvmti.AttachOnDemand.attach030;

import nsk.share.TestBug;
import nsk.share.aod.AbstractJarAgent;
import java.lang.instrument.Instrumentation;

/*
 * First agent tries to redefine class loaded by the target application, then agent itself loads
 * class and redefines it
 */
public class attach030Agent00 extends AbstractJarAgent {

    private static final String classToRedefine1 = "nsk.jvmti.AttachOnDemand.attach030.ClassToRedefine01";

    private static final String classToRedefine2 = "nsk.jvmti.AttachOnDemand.attach030.ClassToRedefine02";

    protected void init(String[] args) {
        if (pathToNewByteCode() == null)
            throw new TestBug("Path to new byte code wasn't specified (" + pathToNewByteCodeOption + "=...)");
    }

    protected void agentActions() throws Throwable {
        boolean classWasFound = false;

        for (Class<?> klass : inst.getAllLoadedClasses()) {
            /*
             * When agent is running class 'classToRedefine1' should be
             * already loaded by the target application
             */
            if (klass.getName().equals(classToRedefine1)) {
                classWasFound = true;
                display("Trying to redefine class '" + classToRedefine1 + "'");
                redefineClass(klass);
                display("Class was redefined");
                break;
            }
        }

        if (!classWasFound) {
            setStatusFailed("Instrumentation.getAllLoadedClasses() didn't return class '" + classToRedefine1 + "'");
        }

        /*
         * Try to load and redefine 'classToRedefine2'
         */

        Class<?> klass = Class.forName(classToRedefine2);
        display("Trying to redefine class '" + classToRedefine2 + "'");
        redefineClass(klass);
        display("Class was redefined");

        final String expectedString = "ClassToRedefine02: Class is redefined";
        ClassToRedefine02 instance = new ClassToRedefine02();
        String string = instance.getString();
        display("ClassToRedefine02.getString(): " + string);
        if (!string.equals(expectedString)) {
            setStatusFailed("ClassToRedefine02.getString() returned unexpected value , expected is '" + expectedString + "'");
        }
    }

    public static void agentmain(String options, Instrumentation inst) {
        new attach030Agent00().runJarAgent(options, inst);
    }
}
