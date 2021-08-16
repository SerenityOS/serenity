/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessControlException;
import java.util.spi.ToolProvider;

/*
 * @test
 * @modules jdk.jlink
 * @build JLinkToolProviderTest
 * @run main/othervm/java.security.policy=toolprovider.policy JLinkToolProviderTest
 */
public class JLinkToolProviderTest {
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    private static void checkJlinkOptions(String... options) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        try {
            JLINK_TOOL.run(pw, pw, options);
            throw new AssertionError("SecurityException should have been thrown!");
        } catch (AccessControlException ace) {
            if (! ace.getPermission().getClass().getName().contains("JlinkPermission")) {
                throw new AssertionError("expected JlinkPermission check failure");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        checkJlinkOptions("--help");
        checkJlinkOptions("--list-plugins");
    }
}
