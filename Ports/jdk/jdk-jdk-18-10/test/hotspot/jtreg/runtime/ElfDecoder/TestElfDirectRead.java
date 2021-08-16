/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8193373
 * @summary Test reading ELF info direct from underlaying file
 * @requires (os.family == "linux") & (os.arch != "ppc64")
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                     -XX:NativeMemoryTracking=detail TestElfDirectRead
 */

// This test intentionally disables caching of Elf sections during symbol lookup
// with WhiteBox.disableElfSectionCache(). On platforms which do not use function
// descriptors but use instead plain function pointers this slows down the lookup just a
// little bit, because all the symbols from an Elf file are still read consecutively
// after one 'fseek()' call. But on platforms with function descriptors like ppc64
// big-endian, we get two 'fseek()' calls for each symbol read from the Elf file
// because reading the file descriptor table is nested inside the loop which reads
// the symbols. This really trashes the I/O system and considerable slows down the
// test, so we need an extra long timeout setting.

/*
 * @test
 * @bug 8193373
 * @summary Test reading ELF info direct from underlaying file
 * @requires (os.family == "linux") & (os.arch == "ppc64")
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=600 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
                                 -XX:NativeMemoryTracking=detail TestElfDirectRead
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import sun.hotspot.WhiteBox;

public class TestElfDirectRead {
  public static void main(String args[]) throws Exception {
    WhiteBox wb = WhiteBox.getWhiteBox();
    wb.disableElfSectionCache();
    ProcessBuilder pb = new ProcessBuilder();
    OutputAnalyzer output;
    // Grab my own PID
    String pid = Long.toString(ProcessTools.getProcessId());

    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail"});
    output = new OutputAnalyzer(pb.start());
    // This is a pre-populated stack frame, should always exist if can decode
    output.shouldContain("MallocSiteTable::new_entry");
  }
}

