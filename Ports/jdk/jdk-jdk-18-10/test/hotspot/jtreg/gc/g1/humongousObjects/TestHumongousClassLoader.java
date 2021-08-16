/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects;

import gc.testlibrary.Helpers;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @test gc.g1.humongousObjects.TestHumongousClassLoader
 * @summary Checks that unreachable classes and unreachable humongous class loader are unloaded after GC
 * @requires vm.gc.G1
 * @requires vm.opt.G1HeapRegionSize == "null" | vm.opt.G1HeapRegionSize == "1M"
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.ClassUnloading  != false
 * @requires vm.opt.ClassUnloadingWithConcurrentMark  != false
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=240  -Xms256M -Xmx256M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                                gc.g1.humongousObjects.ClassLoaderGenerator 1
 *
 * @run main/othervm -Xms256M -Xmx256M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xlog:class+load,class+unload=debug:file=TestHumongousClassLoader_Full_GC.log
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousClassLoader FULL_GC
 *
 * @run main/othervm -Xms256M -Xmx256M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xlog:class+load,class+unload=debug:file=TestHumongousClassLoader_Full_GC_Mem_Pressure.log
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousClassLoader FULL_GC_MEMORY_PRESSURE
 *
 *@run main/othervm -Xms256M -Xmx256M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xlog:class+load,class+unload=debug:file=TestHumongousClassLoader_CMC.log
 *                   -XX:G1HeapRegionSize=1M -XX:MaxTenuringThreshold=1
 *                   gc.g1.humongousObjects.TestHumongousClassLoader CMC
 *
 */

public class TestHumongousClassLoader {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final String SAMPLE_CLASS_NAME_PREFIX = "SampleClassFiller";
    public static final String SIMPLE_CLASSLOADER_NAME = "SimpleClassLoader";
    public static final String HUMONGOUS_CLASSLOADER_NAME = "HumongousClassLoader";


    public static final String LOAD_CLASS_METHOD_PROTOTYPE =
            "    @Override\n"
                    + "    public Class loadClass(String fileName) throws ClassNotFoundException {\n"
                    + "        if (${ClassLoadFilter}) {\n"
                    + "            System.out.println(\"Loading class \" + fileName);\n"
                    + "            byte[] b = null;\n"
                    + "            try {\n"
                    + "                b = Files.readAllBytes(new File(fileName + \".class\").toPath());\n"
                    + "            } catch (IOException e) {\n"
                    + "                e.printStackTrace();\n"
                    + "            }\n"
                    + "            Class c = defineClass(fileName, b, 0, b.length);\n"
                    + "            resolveClass(c);\n"
                    + "            return c;\n"
                    + "        } else {\n"
                    + "            return super.loadClass(fileName);\n"
                    + "        }\n"
                    + "\n"
                    + "\n"
                    + "    }\n";

    public static final String CLASS_HEADER = "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.nio.file.Files;\n"
            + "import java.nio.file.Paths;\n";

    public static final String GENERIC_PROTOTYPE = "${ClassHeader}\n"
            + "public class ${ClassName} extends ${BaseClass}{\n"
            + "    ${ConstructorClause}\n"
            + "    ${Methods}\n"
            + "    ${Fields}\n"
            + "}\n";

    public static final String CONSTUCTOR_PROTOTYPE = "public ${ClassName}(ClassLoader parent) { super(parent);}\n";

    private enum GC {
        FULL_GC {
            @Override
            public void provoke() {
                System.gc();
            }
        },
        CMC {
            @Override
            public void provoke() {
                // We need 2 young gc to promote class loader to old gen
                // Otherwise it will not be unloaded after CMC
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.g1StartConcMarkCycle();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
            }
        },
        FULL_GC_MEMORY_PRESSURE {
            @Override
            public void provoke() {
                WHITE_BOX.fullGC();
            }
        };
        private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

        public abstract void provoke();
    }

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException {

        if (args.length != 1) {
            throw new Error("Test Bug: Expected GC type wasn't provided as command line argument");
        }
        GC gc = GC.valueOf(args[0]);

        Path wrkDir = Paths.get("");
        URL[] url = {wrkDir.toUri().toURL()};
        URLClassLoader urlLoader = new URLClassLoader(url);

        Class<?> simpleClassLoaderClass = urlLoader.loadClass(SIMPLE_CLASSLOADER_NAME);
        urlLoader.close();

        ClassLoader simpleClassLoader = (ClassLoader) simpleClassLoaderClass
                .getConstructor(java.lang.ClassLoader.class)
                .newInstance(TestHumongousClassLoader.class.getClassLoader());

        // Sanity check
        Asserts.assertEquals(WB.g1IsHumongous(simpleClassLoader), false,
                "Test Bug: simpleClassLoader is expected to be non-humongous but it's humongous");


        Class<?> humongousClassLoaderClass = simpleClassLoader.loadClass(HUMONGOUS_CLASSLOADER_NAME);

        ClassLoader humongousClassLoader = (ClassLoader) humongousClassLoaderClass
                .getConstructor(java.lang.ClassLoader.class)
                .newInstance(simpleClassLoader);

        // Sanity check
        Asserts.assertEquals(WB.g1IsHumongous(humongousClassLoader), true,
                "Test Bug: humongousClassLoader is expected to be humongous but it's non-humongous");

        //Asserts.assertEquals(1,0);

        Object[] loadedClasses = new Object[]{
                G1SampleClass.LARGEST_NON_HUMONGOUS.getCls(humongousClassLoader, wrkDir, SAMPLE_CLASS_NAME_PREFIX)
                        .newInstance(),
                G1SampleClass.SMALLEST_HUMONGOUS.getCls(humongousClassLoader, wrkDir, SAMPLE_CLASS_NAME_PREFIX)
                        .newInstance(),
                G1SampleClass.ONE_REGION_HUMONGOUS.getCls(humongousClassLoader, wrkDir, SAMPLE_CLASS_NAME_PREFIX)
                        .newInstance(),
                G1SampleClass.TWO_REGION_HUMONGOUS.getCls(humongousClassLoader, wrkDir, SAMPLE_CLASS_NAME_PREFIX)
                        .newInstance(),
        };

        // forgetting references to loaded classes
        for (int i = 0; i < loadedClasses.length; ++i) {
            loadedClasses[i] = null;
        }

        // forgetting referencies to classloaders
        humongousClassLoader = null;
        humongousClassLoaderClass = null;

        simpleClassLoader = null;
        simpleClassLoaderClass = null;

        gc.provoke();

        // Test checks
        Asserts.assertEquals(WB.isClassAlive(HUMONGOUS_CLASSLOADER_NAME), false,
                String.format("Classloader class %s is loaded after we forget all references to it",
                        HUMONGOUS_CLASSLOADER_NAME));

        for (G1SampleClass sampleClass : G1SampleClass.values()) {
            String className = Helpers.enumNameToClassName(sampleClass.name()) + "Class";
            Asserts.assertEquals(WB.isClassAlive(className), false,
                    String.format("Class %s is loaded after we forget all references to it", className));
        }
    }

}
