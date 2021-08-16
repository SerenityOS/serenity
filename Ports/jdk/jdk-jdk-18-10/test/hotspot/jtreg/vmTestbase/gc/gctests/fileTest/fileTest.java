/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/fileTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.fileTest.fileTest -Filename fileTest.java -iterations 500
 */

package gc.gctests.fileTest;

import java.io.*;
import java.util.*;
import jdk.test.lib.Utils;
import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.TestBug;
import nsk.share.TestFailure;

public class fileTest extends GCTestBase {
        private File [] fileArray;
        private FileInputStream [] fileInputArray;
        private final static int fileNumber = 10000;
        // The number of open file descriptors per process varies from one
        // system to another. lets expermiment with just 20 open fd's.
        private final static int inputStreamNumber = 20;
        private String fileName;

        public fileTest(String fileName) {
                this.fileName = fileName;
                fileArray = new File[fileNumber];
                fileInputArray = new FileInputStream[inputStreamNumber];
        }

        public void runIteration() throws IOException {
                for (int i = 0; i < fileNumber; ++i)
                        fileArray[i] = new File(Utils.TEST_SRC, fileName);
                for (int i = 0; i < inputStreamNumber; ++i)
                        fileInputArray[i] = new FileInputStream(Utils.TEST_SRC + File.separator + fileName);
                for (int i = 0; i < inputStreamNumber; ++i)
                        fileInputArray[i].close();
        }

        public void run() {
                try {
                        Stresser stresser = new Stresser(runParams.getStressOptions());
                        stresser.start(runParams.getIterations());
                        try {
                                while (stresser.iteration())
                                        runIteration();
                        } finally {
                                stresser.finish();
                        }
                } catch (IOException e) {
                        throw new TestFailure(e);
                }
        }

        public static void main(String args[]) {
                String fileName = null;
                for (int i = 0 ; i < args.length ; i++) {
                        if( args[i].equals("-Filename"))
                                fileName = args[++i];
                }
                if (fileName == null)
                        throw new TestBug("No -Filename option is specified");
                GC.runTest(new fileTest(fileName), args);
        }
}
