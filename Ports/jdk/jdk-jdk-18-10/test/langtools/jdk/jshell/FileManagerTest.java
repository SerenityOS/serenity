/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8173845
 * @summary test custom file managers
 * @build KullaTesting TestingInputStream
 * @run testng FileManagerTest
 */


import java.io.File;
import java.io.IOException;

import java.util.Set;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

@Test
public class FileManagerTest extends KullaTesting {

    boolean encountered;

    class MyFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
            implements StandardJavaFileManager {

        protected MyFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location,
                String packageName,
                Set<Kind> kinds,
                boolean recurse)
                throws IOException {
            //System.out.printf("list(%s, %s, %s, %b)\n",
            //        location, packageName, kinds, recurse);
            if (packageName.equals("java.lang.reflect")) {
                encountered = true;
            }
            return fileManager.list(location, packageName, kinds, recurse);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
            return fileManager.getJavaFileObjectsFromFiles(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
            return fileManager.getJavaFileObjects(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
            return fileManager.getJavaFileObjectsFromStrings(names);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
            return fileManager.getJavaFileObjects(names);
        }

        @Override
        public void setLocation(Location location, Iterable<? extends File> files) throws IOException {
            fileManager.setLocation(location, files);
        }

        @Override
        public Iterable<? extends File> getLocation(Location location) {
            return fileManager.getLocation(location);
        }

    }

    @BeforeMethod
    @Override
    public void setUp() {
        setUp(b -> b.fileManager(fm -> new MyFileManager(fm)));
    }

    public void testSnippetMemberAssignment() {
        assertEval("java.lang.reflect.Array.get(new String[1], 0) == null");
        assertTrue(encountered, "java.lang.reflect not encountered");
    }

}
