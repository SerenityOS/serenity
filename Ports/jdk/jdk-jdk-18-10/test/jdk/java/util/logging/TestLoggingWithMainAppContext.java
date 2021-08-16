/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8019853 8023258
 * @summary Test that the default user context is used when in the main
 *          application context. This test must not be run in same VM or agent
 *          VM mode: it would not test the intended behavior.
 * @modules java.desktop
 *          java.logging
 * @run main/othervm -Djava.security.manager=allow TestLoggingWithMainAppContext
 */
public class TestLoggingWithMainAppContext {

    public static void main(String[] args) throws IOException {
        System.out.println("Creating loggers.");

        // These loggers will be created in the default user context.
        final Logger foo1 = Logger.getLogger( "foo" );
        final Logger bar1 = Logger.getLogger( "foo.bar" );
        if (bar1.getParent() != foo1) {
            throw new RuntimeException("Parent logger of bar1 "+bar1+" is not "+foo1);
        }
        System.out.println("bar1.getParent() is the same as foo1");

        // Set a security manager
        System.setSecurityManager(new SecurityManager());
        System.out.println("Now running with security manager");

        // Triggers the creation of the main AppContext
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[] { 0, 1 });
        ImageIO.read(is); // triggers calls to system loggers & creation of main AppContext

        // verify that we're still using the default user context
        final Logger bar2 = Logger.getLogger( "foo.bar" );
        if (bar1 != bar2) {
            throw new RuntimeException("bar2 "+bar2+" is not the same as bar1 "+bar1);
        }
        System.out.println("bar2 is the same as bar1");
        if (bar2.getParent() != foo1) {
            throw new RuntimeException("Parent logger of bar2 "+bar2+" is not foo1 "+foo1);
        }
        System.out.println("bar2.getParent() is the same as foo1");
        final Logger foo2 = Logger.getLogger("foo");
        if (foo1 != foo2) {
            throw new RuntimeException("foo2 "+foo2+" is not the same as foo1 "+foo1);
        }
        System.out.println("foo2 is the same as foo1");

        System.out.println("Test passed.");
    }
}
