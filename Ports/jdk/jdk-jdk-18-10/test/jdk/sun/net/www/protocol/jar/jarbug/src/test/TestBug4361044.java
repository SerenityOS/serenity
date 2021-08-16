/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/*
 * ResourceBundle from jar not found if jar exists in path
 * which has symbol !
 *
 * 3 votes
 *
 * Note: Execute "real" test in separate vm instance so that any locks
 * held on files will be released when this separate vm exits and the
 * invoking vm can clean up if necessary.
 */
public class TestBug4361044 extends JarTest
{
    public void run(String[] args) throws Exception {
        File tmp = createTempDir();
        try {
            File dir = new File(tmp, "dir!name");
            dir.mkdir();
            File testFile = copyResource(dir, "jar1.jar");
            URL[] urls = new URL[1];
            urls[0] = new URL("jar:" + testFile.toURI().toURL() + "!/");
            URLClassLoader loader = new URLClassLoader(urls);
            loader.loadClass("jar1.LoadResourceBundle").newInstance();
        } finally {
            deleteRecursively(tmp);
        }
    }

    public static void main(String[] args) throws Exception {
        new TestBug4361044().run(args);
    }
}
