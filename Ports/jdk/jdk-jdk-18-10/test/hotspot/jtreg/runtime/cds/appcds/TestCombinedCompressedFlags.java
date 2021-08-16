/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8232069
 * @summary Testing different combination of CompressedOops and CompressedClassPointers
 * @requires vm.cds
 * @requires vm.gc == "null"
 * @requires vm.bits == 64
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/Hello.java
 * @modules java.base/jdk.internal.misc
 * @run driver TestCombinedCompressedFlags
 */

import jdk.test.lib.process.OutputAnalyzer;
import java.util.List;
import java.util.ArrayList;

public class TestCombinedCompressedFlags {
    public static String HELLO_STRING = "Hello World";
    public static String EXEC_ABNORMAL_MSG = "Unable to use shared archive.";
    public static final int PASS = 0;
    public static final int FAIL = 1;

    static class ConfArg {
        public boolean useCompressedOops;            // UseCompressedOops
        public boolean useCompressedClassPointers;   // UseCompressedClassPointers
        public String  msg;
        public int code;
        public ConfArg(boolean useCompressedOops, boolean useCompressedClassPointers, String msg, int code) {
            this.useCompressedOops = useCompressedOops;
            this.useCompressedClassPointers = useCompressedClassPointers;
            this.msg  = msg;
            this.code = code;
        }
    }

    static class RunArg {
        public ConfArg dumpArg;
        public List<ConfArg> execArgs;
        public RunArg(ConfArg arg) {
            dumpArg = arg;
            initExecArgs();
        }
        private void initExecArgs() {
           /* The combinations have four cases.
            *          UseCompressedOops   UseCompressedClassPointers  Result
            *    1.
            *    dump: on                  on
            *    test: on                  on                          Pass
            *          on                  off                         Fail
            *          off                 on                          Fail
            *          off                 off                         Fail
            *    2.
            *    dump: on                  off
            *    test: on                  off                         Pass
            *          on                  on                          Fail
            *          off                 on                          Pass
            *          off                 off                         Fail
            *    3.
            *    dump: off                 on
            *    test: off                 on                          Pass
            *          on                  on                          Fail
            *          on                  off                         Fail
            *    4.
            *    dump: off                 off
            *    test: off                 off                         Pass
            *          on                  on                          Fail
            *          on                  off                         Fail
            **/
            execArgs = new ArrayList<ConfArg>();
            if (dumpArg.useCompressedOops && dumpArg.useCompressedClassPointers) {
                execArgs
                    .add(new ConfArg(true, true, HELLO_STRING, PASS));
                execArgs
                    .add(new ConfArg(true, false, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(false, true, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(false, false, EXEC_ABNORMAL_MSG, FAIL));

            }  else if(dumpArg.useCompressedOops && !dumpArg.useCompressedClassPointers) {
                execArgs
                    .add(new ConfArg(true, false, HELLO_STRING, PASS));
                execArgs
                    .add(new ConfArg(true, true, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(false, true, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(false, false, EXEC_ABNORMAL_MSG, FAIL));

            } else if (!dumpArg.useCompressedOops && dumpArg.useCompressedClassPointers) {
                execArgs
                    .add(new ConfArg(false, true, HELLO_STRING, PASS));
                execArgs
                    .add(new ConfArg(true, true, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(true, false, EXEC_ABNORMAL_MSG, FAIL));
            } else if (!dumpArg.useCompressedOops && !dumpArg.useCompressedClassPointers) {
                execArgs
                    .add(new ConfArg(false, false, HELLO_STRING, PASS));
                execArgs
                    .add(new ConfArg(true, true, EXEC_ABNORMAL_MSG, FAIL));
                execArgs
                    .add(new ConfArg(true, false, EXEC_ABNORMAL_MSG, FAIL));
            }
        }
    }

    public static String getCompressedOopsArg(boolean on) {
        if (on) return "-XX:+UseCompressedOops";
        else    return "-XX:-UseCompressedOops";
    }

    public static String getCompressedClassPointersArg(boolean on) {
        if (on) return "-XX:+UseCompressedClassPointers";
        else    return "-XX:-UseCompressedClassPointers";
    }

    public static List<RunArg> runList;

    public static void configureRunArgs() {
        runList = new ArrayList<RunArg>();
        runList
            .add(new RunArg(new ConfArg(true, true, null, PASS)));
        runList
            .add(new RunArg(new ConfArg(true, false, null, PASS)));
        runList
            .add(new RunArg(new ConfArg(false, true, null, PASS)));
        runList
            .add(new RunArg(new ConfArg(false, false, null, PASS)));
    }

    public static void main(String[] args) throws Exception {
        String helloJar = JarBuilder.build("hello", "Hello");
        configureRunArgs();
        OutputAnalyzer out;
        for (RunArg t: runList) {
            out = TestCommon
                .dump(helloJar,
                      new String[] {"Hello"},
                      getCompressedOopsArg(t.dumpArg.useCompressedOops),
                      getCompressedClassPointersArg(t.dumpArg.useCompressedClassPointers),
                      "-Xlog:cds",
                      "-XX:NativeMemoryTracking=detail");
            out.shouldContain("Dumping shared data to file:");
            out.shouldHaveExitValue(0);

            for (ConfArg c : t.execArgs) {
                out = TestCommon.exec(helloJar,
                                      "-cp",
                                      helloJar,
                                      "-Xlog:cds",
                                      "-XX:NativeMemoryTracking=detail",
                                      getCompressedOopsArg(c.useCompressedOops),
                                      getCompressedClassPointersArg(c.useCompressedClassPointers),
                                      "Hello");
                out.shouldContain(c.msg);
                out.shouldHaveExitValue(c.code);
            }
        }
    }
}
