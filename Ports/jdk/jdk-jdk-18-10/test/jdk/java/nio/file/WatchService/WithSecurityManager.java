/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887
 * @summary Unit test for Watchable#register's permission checks
 * @modules jdk.unsupported
 * @build WithSecurityManager
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager denyAll.policy - fail
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager denyAll.policy tree fail
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirOnly.policy - pass
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirOnly.policy tree fail
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirAndOneLevel.policy - pass
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirAndOneLevel.policy tree fail
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirAndTree.policy - pass
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager grantDirAndTree.policy tree pass
 */

import java.nio.file.*;
import java.io.IOException;
import com.sun.nio.file.ExtendedWatchEventModifier;

public class WithSecurityManager {

    public static void main(String[] args) throws IOException {
        String policyFile = args[0];
        boolean recursive = args[1].equals("tree");
        boolean expectedToFail = args[2].equals("fail");

        // install security manager with the given policy file
        String testSrc = System.getProperty("test.src");
        if (testSrc == null)
            throw new RuntimeException("This test must be run by jtreg");
        Path dir = Paths.get(testSrc);
        System.setProperty("java.security.policy", dir.resolve(policyFile).toString());
        System.setSecurityManager(new SecurityManager());

        // initialize optional modifier
        WatchEvent.Modifier[] modifiers;
        if (recursive) {
            modifiers = new WatchEvent.Modifier[1];
            modifiers[0] = ExtendedWatchEventModifier.FILE_TREE;
        } else {
            modifiers = new WatchEvent.Modifier[0];
        }

        // attempt to register directory
        try {
            dir.register(dir.getFileSystem().newWatchService(),
                         new WatchEvent.Kind<?>[]{ StandardWatchEventKinds.ENTRY_CREATE },
                         modifiers);
            if (expectedToFail)
                throw new RuntimeException("SecurityException not thrown");
        } catch (SecurityException e) {
            if (!expectedToFail)
                throw e;
        } catch (UnsupportedOperationException e) {
            // FILE_TREE modifier only supported on some platforms
            if (!recursive)
                throw new RuntimeException(e);
            System.out.println("FILE_TREE option not supported");
        }
    }
}
