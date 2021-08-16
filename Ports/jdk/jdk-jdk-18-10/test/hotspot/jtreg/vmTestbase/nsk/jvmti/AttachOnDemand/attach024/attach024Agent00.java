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
package nsk.jvmti.AttachOnDemand.attach024;

import nsk.share.TestBug;
import nsk.share.aod.AbstractJarAgent;
import java.lang.instrument.Instrumentation;

public class attach024Agent00 extends AbstractJarAgent {

    public static final String MODIFIED_TO_STRING = "attach024: Modified version";

    private static final String TESTED_CLASS_NAME = "java.util.TooManyListenersException";

    protected void agentActions() {
        /*
         * Check that TooManyListenersException isn't loaded, otherwise test checks
         * doesn't make sense
         */
        for (Class<?> klass : inst.getAllLoadedClasses()) {
            if (klass.getName().equals(TESTED_CLASS_NAME)) {
                throw new TestBug("TooManyListenersException already loaded");
            }
        }

        checkTooManyListenersException();
    }

    void checkTooManyListenersException() {
        java.util.TooManyListenersException e = new java.util.TooManyListenersException("Test");
        display("TooManyListenersException.toString(): " + e.toString());
        if (e.toString().equals(MODIFIED_TO_STRING)) {
            setStatusFailed("Class TooManyListenersException was erroneously loaded from agent's jar");
        }
    }

    public static void agentmain(String options, Instrumentation inst) {
        new attach024Agent00().runJarAgent(options, inst);
    }
}
