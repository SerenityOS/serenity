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
 * @bug 4627316 6743526
 * @summary Test option to limit direct memory allocation
 * @requires (os.arch == "x86_64") | (os.arch == "amd64")
 * @library /test/lib
 *
 * @summary Test: memory is properly limited using multiple buffers
 * @run main/othervm -XX:MaxDirectMemorySize=10 LimitDirectMemory true 10 1
 * @run main/othervm -XX:MaxDirectMemorySize=1k LimitDirectMemory true 1k 100
 * @run main/othervm -XX:MaxDirectMemorySize=10m LimitDirectMemory true 10m 10m
 *
 * @summary Test: We can increase the amount of available memory
 * @run main/othervm -XX:MaxDirectMemorySize=65M LimitDirectMemory false 64M 65M
 *
 * @summary Test: Exactly the default amount of memory is available
 * @run main/othervm LimitDirectMemory false 10 1
 * @run main/othervm -Xmx64m LimitDirectMemory false 0 DEFAULT
 * @run main/othervm -Xmx64m LimitDirectMemory true 0 DEFAULT+1
 *
 * @summary Test: We should be able to eliminate direct memory allocation entirely
 * @run main/othervm -XX:MaxDirectMemorySize=0 LimitDirectMemory true 0 1
 *
 * @summary Test: Setting the system property should not work so we should be able
 *                to allocate the default amount
 * @run main/othervm -Dsun.nio.MaxDirectMemorySize=1K -Xmx64m
 *                   LimitDirectMemory false DEFAULT-1 DEFAULT/2
 */

import java.nio.ByteBuffer;
import java.util.Properties;

public class LimitDirectMemory {
    private static final int K = 1024;

    public static void main(String [] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: "
                    + "java LimitDirectMemory"
                    + " <OOME_expected(true|false)>"
                    + " <fill_direct_memory>"
                    + " <size_per_buffer>");
        }
        boolean throwp = parseThrow(args[0]);
        int size = parseSize(args[1]);
        int incr = (args.length > 2 ? parseSize(args[2]) : size);

        Properties p = System.getProperties();
        if (p.getProperty("sun.nio.MaxDirectMemorySize") != null)
            throw new RuntimeException("sun.nio.MaxDirectMemorySize defined");

        ByteBuffer [] b = new ByteBuffer[K];

        // Fill up most/all of the direct memory
        int i = 0;
        while (size >= incr) {
            b[i++] = ByteBuffer.allocateDirect(incr);
            size -= incr;
        }

        if (throwp) {
            try {
                b[i] = ByteBuffer.allocateDirect(incr);
                throw new RuntimeException("OutOfMemoryError not thrown: "
                                           + incr);
            } catch (OutOfMemoryError e) {
                e.printStackTrace(System.out);
                System.out.println("OK - Error thrown as expected ");
            }
        } else {
            b[i] = ByteBuffer.allocateDirect(incr);
            System.out.println("OK - Error not thrown");
        }
    }

    private static boolean parseThrow(String s) {
        if (s.equals("true"))  return true;
        if (s.equals("false")) return false;
        throw new RuntimeException("Unrecognized expectation: " + s);
    }

    private static int parseSize(String size) throws Exception {

        if (size.equals("DEFAULT"))
            return (int)Runtime.getRuntime().maxMemory();
        if (size.equals("DEFAULT+1"))
            return (int)Runtime.getRuntime().maxMemory() + 1;
        if (size.equals("DEFAULT+1M"))
            return (int)Runtime.getRuntime().maxMemory() + (1 << 20);
        if (size.equals("DEFAULT-1"))
            return (int)Runtime.getRuntime().maxMemory() - 1;
        if (size.equals("DEFAULT/2"))
            return (int)Runtime.getRuntime().maxMemory() / 2;

        int idx = 0, len = size.length();


        for (int i = 0; i < len; i++) {
            if (Character.isDigit(size.charAt(i))) idx++;
            else break;
        }

        if (idx == 0)
            throw new RuntimeException("No digits detected: " + size);

        int result = Integer.parseInt(size.substring(0, idx));

        if (idx < len) {
            for (int i = idx; i < len; i++) {
                switch(size.charAt(i)) {
                case 'T': case 't': result *= K; // fall through
                case 'G': case 'g': result *= K; // fall through
                case 'M': case 'm': result *= K; // fall through
                case 'K': case 'k': result *= K;
                    break;
                default:
                    throw new RuntimeException("Unrecognized size: " + size);
                }
            }
        }
        return result;
    }
}
