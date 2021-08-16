/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test of diagnostic command VM.class_hierarchy
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng ClassHierarchyTest
 */

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassHierarchyTest {

    // $> jcmd DcmdTestClass VM.class_hierarchy  DcmdTestClass | grep DcmdTestClass\$\$Lambda
    // |--DcmdTestClass$$Lambda$1/4081552/0xa529fbb0

    // > VM.class_hierarchy DcmdBaseClass
    // java.lang.Object/null
    // |--DcmdBaseClass/0xa4abcd48

    // > VM.class_hierarchy DcmdBaseClass -s
    // java.lang.Object/null
    // |--DcmdBaseClass/0xa4abcd48
    // |  |--DcmdTestClass/0xa4abcd48

    // > VM.class_hierarchy DcmdBaseClass -i -s
    // java.lang.Object/null
    // |--DcmdBaseClass/0xa4abcd48
    // |  implements Intf2/0xa4abcd48 (declared intf)
    // |  implements Intf1/0xa4abcd48 (inherited intf)
    // |  |--DcmdTestClass/0xa4abcd48
    // |  |  implements Intf1/0xa4abcd48 (inherited intf)
    // |  |  implements Intf2/0xa4abcd48 (inherited intf)

    static Pattern expected_lambda_line =
        Pattern.compile("\\|--DcmdTestClass\\$\\$Lambda.*");

    static Pattern expected_lines[] = {
        Pattern.compile("java.lang.Object/null"),
        Pattern.compile("\\|--DcmdBaseClass/0x(\\p{XDigit}*)"),
        Pattern.compile("\\|  implements Intf2/0x(\\p{XDigit}*) \\(declared intf\\)"),
        Pattern.compile("\\|  implements Intf1/0x(\\p{XDigit}*) \\(inherited intf\\)"),
        Pattern.compile("\\|  \\|--DcmdTestClass/0x(\\p{XDigit}*)"),
        Pattern.compile("\\|  \\|  implements Intf1/0x(\\p{XDigit}*) \\(inherited intf\\)"),
        Pattern.compile("\\|  \\|  implements Intf2/0x(\\p{XDigit}*) \\(inherited intf\\)")
    };

    public void run(CommandExecutor executor) throws ClassNotFoundException {
        OutputAnalyzer output;
        Iterator<String> lines;
        int i;

        // Load our test class whose hierarchy we will print.
        Class<?> c = Class.forName("DcmdTestClass");

        // Verify the presence of the lamba anonymous class
        output = executor.execute("VM.class_hierarchy");
        lines = output.asLines().iterator();
        Boolean foundMatch = false;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lambda_line.matcher(line);
            if (m.matches()) {
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch) {
            Assert.fail("Failed to find lamda class");
        }

        // Verify the output for the simple hierachry of just DcmdBaseClass.
        output = executor.execute("VM.class_hierarchy DcmdBaseClass");
        lines = output.asLines().iterator();
        i = 0;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            // Should only be two lines of output in this form.
            if (i == 2) break;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }

        // Verify the output for the full hierarchy of DcmdBaseClass, but without interfaces.
        output = executor.execute("VM.class_hierarchy DcmdBaseClass -s");
        lines = output.asLines().iterator();
        i = 0;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            // "implements" lines should not be in this output.
            if (i == 2 || i == 4) i += 2;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }

        // Verify the output for the full hierarchy of DcmdBaseClass, including interfaces.
        output = executor.execute("VM.class_hierarchy DcmdBaseClass -i -s");
        lines = output.asLines().iterator();
        i = 0;
        String classLoaderAddr = null;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            if (i == 2) {
                // Fetch the ClassLoader address, which should be the same in
                // subsequent lines.
                classLoaderAddr = m.group(1);
                System.out.println(classLoaderAddr);
            } else if (i > 2) {
                if (!classLoaderAddr.equals(m.group(1))) {
                    Assert.fail("Classloader address didn't match on line #"
                                        + i + ": " + line);
                }
            }
            if (i == expected_lines.length) break;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }
    }

    @Test
    public void jmx() throws ClassNotFoundException {
        run(new JMXExecutor());
    }
}

interface Intf1 {
}

interface Intf2 extends Intf1 {
}

class DcmdBaseClass implements Intf2 {
}

class DcmdTestClass extends DcmdBaseClass {
    static {
        // Force creation of anonymous class (for the lambdaform).
        Runnable r = () -> System.out.println("Hello");
        r.run();
    }
}
