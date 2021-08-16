/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028994
 * @author Staffan Larsen
 * @library /test/lib
 * @modules jdk.attach/sun.tools.attach
 *          jdk.management
 * @run main CheckOrigin
 */

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.sun.management.VMOption.Origin;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Map;
import jdk.test.lib.process.ProcessTools;
import sun.tools.attach.HotSpotVirtualMachine;

public class CheckOrigin {

    private static HotSpotDiagnosticMXBean mbean;

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            // start a process that has options set in a number of different ways

            File flagsFile = File.createTempFile("CheckOriginFlags", null);
            try (PrintWriter pw =
                   new PrintWriter(new FileWriter(flagsFile))) {
                pw.println("+PrintCodeCache");
            }

            ProcessBuilder pb = ProcessTools.
                createJavaProcessBuilder(
                    "--add-exports", "jdk.attach/sun.tools.attach=ALL-UNNAMED",
                    "-XX:+UseG1GC",  // this will cause MaxNewSize to be FLAG_SET_ERGO
                    "-XX:+UseCodeAging",
                    "-XX:+UseCerealGC",         // Should be ignored.
                    "-XX:Flags=" + flagsFile.getAbsolutePath(),
                    "-Djdk.attach.allowAttachSelf",
                    "-cp", System.getProperty("test.class.path"),
                    "CheckOrigin",
                    "-runtests");

            Map<String, String> env = pb.environment();
            env.put("_JAVA_OPTIONS", "-XX:+CheckJNICalls");
            // "UseGOneGC" should be ignored.
            env.put("JAVA_TOOL_OPTIONS", "-XX:+IgnoreUnrecognizedVMOptions "
                + "-XX:+PrintVMOptions -XX:+UseGOneGC");

            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            int exit = p.waitFor();
            System.out.println("sub process exit == " + exit);
            if (exit != 0) {
                throw new Exception("Unexpected exit code from subprocess == " + exit);
            }
        } else {
            mbean =
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);

            // set a few more options
            mbean.setVMOption("HeapDumpOnOutOfMemoryError", "true");
            setOptionUsingAttach("HeapDumpPath", "/a/sample/path");

            // check the origin field for all the options we set

            // Not set, so should be default
            checkOrigin("ManagementServer", Origin.DEFAULT);
            // Set on the command line
            checkOrigin("UseCodeAging", Origin.VM_CREATION);
            // Set in _JAVA_OPTIONS
            checkOrigin("CheckJNICalls", Origin.ENVIRON_VAR);
            // Set in JAVA_TOOL_OPTIONS
            checkOrigin("IgnoreUnrecognizedVMOptions", Origin.ENVIRON_VAR);
            checkOrigin("PrintVMOptions", Origin.ENVIRON_VAR);
            // Set in -XX:Flags file
            checkOrigin("PrintCodeCache", Origin.CONFIG_FILE);
            // Set through j.l.m
            checkOrigin("HeapDumpOnOutOfMemoryError", Origin.MANAGEMENT);
            // Should be set by the VM, when we set UseG1GC
            checkOrigin("MaxNewSize", Origin.ERGONOMIC);
            // Set using attach
            checkOrigin("HeapDumpPath", Origin.ATTACH_ON_DEMAND);
        }
    }

    private static void checkOrigin(String option, Origin origin) throws Exception
    {
        Origin o = mbean.getVMOption(option).getOrigin();
        if (!o.equals(origin)) {
            throw new Exception("Option '" + option + "' should have origin '" + origin + "' but had '" + o + "'");
        }
        System.out.println("Option '" + option + "' verified origin = '" + origin + "'");
    }

    // use attach to set a manageable vm option
    private static void setOptionUsingAttach(String option, String value) throws Exception {
        HotSpotVirtualMachine vm = (HotSpotVirtualMachine) VirtualMachine.attach(ProcessTools.getProcessId()+"");
        InputStream in = vm.setFlag(option, value);
        System.out.println("Result from setting '" + option + "' to '" + value + "' using attach:");
        drain(vm, in);
        System.out.println("-- end -- ");
    }

    // Read the stream from the target VM until EOF, print to output, then detach
    private static void drain(VirtualMachine vm, InputStream in) throws Exception {
        byte b[] = new byte[256];
        int n;
        do {
            n = in.read(b);
            if (n > 0) {
                String s = new String(b, 0, n, "UTF-8");
                System.out.print(s);
            }
        } while (n > 0);
        in.close();
        vm.detach();
    }

}
