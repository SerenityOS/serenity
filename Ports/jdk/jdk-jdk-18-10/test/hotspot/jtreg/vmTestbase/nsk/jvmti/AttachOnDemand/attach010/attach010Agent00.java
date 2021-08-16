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
package nsk.jvmti.AttachOnDemand.attach010;

import nsk.share.aod.AbstractJarAgent;
import java.io.*;
import java.lang.instrument.Instrumentation;

public class attach010Agent00 extends AbstractJarAgent {

    private static final String outStreamFileName = "AttachOnDemand.attach010.out";

    private static final String errStreamFileName = "AttachOnDemand.attach010.err";

    private static final String inStreamFileName = "AttachOnDemand.attach010.in";

    protected void agentActions() throws Throwable {
        PrintStream newOutStream = new PrintStream(outStreamFileName);
        PrintStream newErrStream = new PrintStream(errStreamFileName);

        System.setOut(newOutStream);
        System.out.println("Print to the new System.out");

        System.setErr(newErrStream);
        System.err.println("Print to the new System.err");

        /*
         * Check input stream
         */

        final int valueToWrite = 100;

        PrintStream inputFileStream = new PrintStream(inStreamFileName);
        try {
            inputFileStream.println(valueToWrite);
        } finally {
            inputFileStream.close();
        }

        FileInputStream newInputStream = new FileInputStream(inStreamFileName);
        System.setIn(newInputStream);

        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(System.in));
        int readValue = Integer.parseInt(inputStreamReader.readLine());

        if (readValue != valueToWrite) {
            setStatusFailed("Unexpected value was read from input stream: " + readValue + ", expected value is " + valueToWrite);
        }
    }

    public static void agentmain(String options, Instrumentation inst) {
        new attach010Agent00().runJarAgent(options, inst);
    }
}
