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

/*
 * @test
 * @bug 8149757 8144062 8196627
 * @summary Test that StandardJavaFileManager uses the correct version of a
 * class from a multi-release jar on classpath
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox
 * @run testng MultiReleaseJarAwareSJFM
 */

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class MultiReleaseJarAwareSJFM {
    private static final int CURRENT_VERSION = Runtime.version().major();

    private final String version8 =
            "package version;\n" +
            "\n" +
            "public class Version {\n" +
            "    public int getVersion() {\n" +
            "        return 8;\n" +
            "    }\n" +
            "}\n";

    private final String version9 =
            "package version;\n" +
            "\n" +
            "public class Version {\n" +
            "    public int getVersion() {\n" +
            "        int version = (new PackagePrivate()).getVersion();\n" +
            "        if (version == 9) return 9;\n" +
            "        return version;\n" +
            "    }\n" +
            "}\n";

    private final String packagePrivate =
            "package version;\n" +
            "\n" +
            "class PackagePrivate {\n" +
            "    int getVersion() {\n" +
            "        return 9;\n" +
            "    }\n" +
            "}\n";

    private final String versionCurrent =
            "package version;\n" +
            "\n" +
            "public class Version {\n" +
            "    public int getVersion() {\n" +
            "        return " + CURRENT_VERSION +";\n" +
            "    }\n" +
            "}\n";

    private final String manifest =
            "Manifest-Version: 1.0\n" +
            "Multi-Release: true\n";

    private final ToolBox tb = new ToolBox();

    private final JavaFileManager.Location jloc = new JavaFileManager.Location() {
        @Override
        public String getName() {
            return "Multi-Release Jar";
        }
        @Override
        public boolean isOutputLocation() {
            return false;
        }
    };

    @BeforeClass
    public void setup() throws Exception {
        tb.createDirectories("classes",
                "classes/META-INF/versions/9",
                "classes/META-INF/versions/" + CURRENT_VERSION);
        new JavacTask(tb)
                .outdir("classes")
                .sources(version8)
                .run();
        new JavacTask(tb)
                .outdir("classes/META-INF/versions/9")
                .sources(version9, packagePrivate)
                .run();
        new JavacTask(tb)
                .outdir("classes/META-INF/versions/" + CURRENT_VERSION)
                .sources(versionCurrent)
                .run();
        new JarTask(tb, "multi-release.jar")
                .manifest(manifest)
                .baseDir("classes")
                .files("version/Version.class",
                        "META-INF/versions/9/version/Version.class",
                        "META-INF/versions/9/version/PackagePrivate.class",
                        "META-INF/versions/" + CURRENT_VERSION + "/version/Version.class")
                .run();
    }

    @AfterClass
    public void teardown() throws Exception {
        tb.deleteFiles(
                "classes/META-INF/versions/" + CURRENT_VERSION + "/version/Version.class",
                "classes/META-INF/versions/" + CURRENT_VERSION + "/version",
                "classes/META-INF/versions/" + CURRENT_VERSION,
                "classes/META-INF/versions/9/version/Version.class",
                "classes/META-INF/versions/9/version/PackagePrivate.class",
                "classes/META-INF/versions/9/version",
                "classes/META-INF/versions/9",
                "classes/META-INF/versions",
                "classes/META-INF",
                "classes/version/Version.class",
                "classes/version",
                "classes",
                "multi-release.jar"
        );
    }

    @DataProvider(name = "versions")
    public Object[][] data() {
        return new Object[][] {
                {"", 8},
                {"8", 8},
                {"9", 9},
                {"runtime", Runtime.version().major()}
        };
    }

    @Test(dataProvider = "versions")
    public void test(String version, int expected) throws Throwable {
        StandardJavaFileManager jfm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
        jfm.setLocation(jloc, List.of(new File("multi-release.jar")));

        if (version.length() > 0) {
            jfm.handleOption("--multi-release", List.of(version).iterator());
        }

        CustomClassLoader cldr = new CustomClassLoader(jfm);
        Class<?> versionClass = cldr.loadClass("version.Version");
        MethodType mt = MethodType.methodType(int.class);
        MethodHandle mh = MethodHandles.lookup().findVirtual(versionClass, "getVersion", mt);
        int v = (int)mh.invoke(versionClass.newInstance());
        Assert.assertEquals(v, expected);

        jfm.close();
    }

    private class CustomClassLoader extends ClassLoader {
        private final JavaFileManager jfm;

        public CustomClassLoader(JavaFileManager jfm) {
            super(null);
            this.jfm = jfm;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            int n = name.lastIndexOf('.');
            String pkg = n == -1 ? "" : name.substring(0, n);
            String cls = name.substring(n + 1) + ".class";
            byte[] b;
            try {
                FileObject obj = jfm.getFileForInput(jloc, pkg, cls);
                try (InputStream is = obj.openInputStream()) {
                    b = is.readAllBytes();
                }
            } catch (IOException x) {
                throw new ClassNotFoundException(x.getMessage(), x);
            }
            return defineClass(name, b, 0, b.length);
        }
    }
}

