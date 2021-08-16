/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import handlers.jar.Handler;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.util.JarBuilder;
/*
 * @test
 * @bug 8174151
 * @summary Test for java.protocol.handler.pkgs with jar protocol handler
 * @library /test/lib/
 * @build jdk.test.lib.util.JarBuilder
 * @compile handlers/jar/Handler.java
 * @run main/othervm -Djava.protocol.handler.pkgs=handlers JarHandlerPkgPrefix
 */
public class JarHandlerPkgPrefix {

    static void createJar(Path p) throws Exception {
        JarBuilder jb = new JarBuilder(p.toString());
        jb.addEntry("resource.txt", "CONTENTS".getBytes());
        jb.build();
    }

    public static void main(String[] args) throws Exception {
        Path jarPath = Paths.get(System.getProperty("user.dir", "."), "test.jar");
        try {
            createJar(jarPath);

            URL j = new URL("jar:file:" + jarPath.toString() + "!/");
            URLClassLoader ucl = new URLClassLoader(new URL[]{j});
            ucl.findResource("resource.txt");

            URL r = new URL("jar:file:" + jarPath.toString() + "!/resource.txt");
            if (!Handler.URLS.contains(r))
                throw new AssertionError("jar: URL handler not invoked");
        }
        finally {
            Files.delete(jarPath);
        }
    }
}

