/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8232097
 * @summary Basic sanity for creation of SCTP channels
 * @modules jdk.sctp
 * @run main/othervm SctpSanity 1
 * @run main/othervm SctpSanity 2
 * @run main/othervm SctpSanity 3
 */

import java.io.IOException;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpMultiChannel;
import com.sun.nio.sctp.SctpServerChannel;
import static java.lang.System.out;

/**
 * Tests creation of SCTP channels. The channels should either be created
 * or not. The latter throwing an UnsupportedOperationException. No other
 * behavior is acceptable. Minimally, exercises the JDK's native library
 * loading on operating systems that provide an implementation, even if
 * the system-level support is not configured.
 */
public class SctpSanity {

    public static void main(String... args) throws IOException {
        switch (Integer.valueOf(args[0])) {
            case 1: testSctpChannel();        break;
            case 2: testSctpServerChannel();  break;
            case 3: testSctpMultiChannel();   break;
            default: throw new AssertionError("should not reach here");
        }
    }

    static void testSctpChannel() throws IOException {
        try (SctpChannel channel = SctpChannel.open()) {
            out.println("created SctpChannel:" + channel);
        } catch (UnsupportedOperationException uoe) {
            // ok - the platform does not support SCTP
            out.println("ok, caught:" + uoe);
        }
    }

    static void testSctpServerChannel() throws IOException {
        try (SctpServerChannel channel = SctpServerChannel.open()) {
            out.println("created SctpServerChannel:" + channel);
        } catch (UnsupportedOperationException uoe) {
            // ok - the platform does not support SCTP
            out.println("ok, caught:" + uoe);
        }
    }

    static void testSctpMultiChannel() throws IOException {
        try (SctpMultiChannel channel = SctpMultiChannel.open()) {
            out.println("created SctpMultiChannel:" + channel);
        } catch (UnsupportedOperationException uoe) {
            // ok - the platform does not support SCTP
            out.println("ok, caught:" + uoe);
        }
    }
}

