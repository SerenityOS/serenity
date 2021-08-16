/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestG1RemSetFlags
 * @requires vm.gc.G1
 * @summary Verify that the remembered set flags are updated as expected
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @library /test/lib
 * @library /
 * @run driver gc.arguments.TestG1RemSetFlags
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestG1RemSetFlags {

  private static void checkG1RemSetFlags(String[] flags, int exitValue) throws Exception {
    ArrayList<String> flagList = new ArrayList<String>();
    flagList.addAll(Arrays.asList(flags));
    flagList.add("-XX:+UseG1GC");
    flagList.add("-XX:+PrintFlagsFinal");
    flagList.add("-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(flagList);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(exitValue);
  }

  public static void main(String args[]) throws Exception {
    checkG1RemSetFlags(new String[] { "-XX:+UnlockExperimentalVMOptions", "-XX:G1RemSetHowlNumBuckets=8", "-XX:G1RemSetHowlMaxNumBuckets=8"  },  0);
    checkG1RemSetFlags(new String[] { "-XX:+UnlockExperimentalVMOptions", "-XX:G1RemSetHowlNumBuckets=8", "-XX:G1RemSetHowlMaxNumBuckets=16"  },  0);
    checkG1RemSetFlags(new String[] { "-XX:+UnlockExperimentalVMOptions", "-XX:G1RemSetHowlNumBuckets=16", "-XX:G1RemSetHowlMaxNumBuckets=8"  },  1);
    checkG1RemSetFlags(new String[] { "-XX:+UnlockExperimentalVMOptions", "-XX:G1RemSetHowlNumBuckets=7"  },  1);
    checkG1RemSetFlags(new String[] { "-XX:+UnlockExperimentalVMOptions", "-XX:G1RemSetHowlMaxNumBuckets=7"  },  1);
  }
}
