/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8049365
 * @summary Tests the JDI and JDWP update for modules
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ModulesTest.java
 * @run driver ModulesTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class ModulesTarg {

    public static void main(String[] args){
        System.out.println("Goodbye from ModulesTarg!");
    }
}

    /********** test program **********/

public class ModulesTest extends TestScaffold {
    static final String FailPrefix = "ModulesTest: failed: ";

    private static ModuleReference bootUnnamedModule = null;
    private static ModuleReference appUnnamedModule  = null;
    private static ModuleReference extUnnamedModule  = null;

    private ReferenceType targetClass;
    private List<ModuleReference> modules;


    ModulesTest (String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new ModulesTest(args).startTests();
    }

    /********** test core **********/

    private boolean reftypeSanityCheck(ModuleReference module, ReferenceType type) {
        ModuleReference other = type.module();
        if (other == null) {
            testFailed = true;
            println(FailPrefix + "a ModuleReference should never be null #1");
            return false;
        }
        // Sanity checks: make sure there is no crash or exception
        String otherName = other.name();
        return true;
    }

    private void checkLoaderDefinedClasses(ModuleReference module, ClassLoaderReference loader) {
        String moduleName = module.name();
        List<ReferenceType> definedClasses = loader.definedClasses();
        boolean origModuleWasObserved = false;

        for (ReferenceType type: definedClasses) {
            ClassLoaderReference otherLoader = type.classLoader();
            if (!loader.equals(otherLoader)) {
                testFailed = true;
                println(FailPrefix + "all classes defined by a ClassLoader" +
                        " should refer to the defining ClassLoader");
                return;
            }
            if (!reftypeSanityCheck(module, type)) {
                return;
            }
        }
    }

    private void checkLoaderVisibleClasses(ModuleReference module, ClassLoaderReference loader) {
        String moduleName = module.name();
        List<ReferenceType> visibleClasses = loader.visibleClasses();

        for (ReferenceType type: visibleClasses) {
            if (!type.isPrepared()) {
                continue; // Safety: skip unprepared classes
            }
            if (!reftypeSanityCheck(module, type)) {
                return;
            }
        }
    }

    // Check any ClassLoader except the bootsrtap ClassLoader
    private void checkClassLoader(ModuleReference module, ClassLoaderReference loader) {
        checkLoaderDefinedClasses(module, loader);
        checkLoaderVisibleClasses(module, loader);
    }

    // Sanity checks to make sure there are no crashes or exceptions.
    private void checkModule(ModuleReference module, ModuleReference other, int checkIdx) {
        if (module == null) {
            testFailed = true;
            println(FailPrefix + "a ModuleReference should never be null #2");
            return;
        }
        String name = module.name();
        println("\n--- Check #" + checkIdx);
        println("    module name: " + name);

        ClassLoaderReference loader = module.classLoader();
        println("    loader: " + loader);

        if (loader != null) {
            checkClassLoader(module, loader);
            String classloaderName = loader.toString();
            if (classloaderName.contains("AppClassLoader") && name == null) {
                if (appUnnamedModule != null) {
                    testFailed = true;
                    println(FailPrefix + "multiple unnamed modules in AppClassLoader");
                }
                appUnnamedModule = module;
            }
            if (classloaderName.contains("PlatformClassLoader") && name == null) {
                if (extUnnamedModule != null) {
                    testFailed = true;
                    println(FailPrefix + "multiple unnamed modules in PlatformClassLoader");
                }
                extUnnamedModule = module;
            }
        } else if (name == null) {
            if (bootUnnamedModule != null) {
                testFailed = true;
                println(FailPrefix + "multiple unnamed modules in BootClassLoader");
            }
            bootUnnamedModule = module;
        }
    }

    // Check that the java.lang.String class was loaded by the java.base module.
    private void checkBaseModule() {
        List<ReferenceType> clist = vm().classesByName("java.lang.String");
        if (clist.size() != 1) {
            testFailed = true;
            println(FailPrefix + "just one java.lang.String class is expected" +
                    "but found multiple class instances: " + clist.size());
            return;
        }
        ModuleReference module = clist.get(0).module();
        if (module == null) {
            testFailed = true;
            println(FailPrefix + "a ModuleReference should never be null #3");
        }
        if (module.name().compareTo("java.base") != 0) {
            testFailed = true;
            println(FailPrefix + "java.lang.String must belong to java.base module");
        }
    }

    // Check that the unnamed modules of the bootsrtap, application
    // and platform class loaders were observed.
    private void checkUnnamedModules() {
        if (bootUnnamedModule == null) {
            testFailed = true;
            println(FailPrefix + "unnamed module of BootClassLoader was not observed");
        }
        if (appUnnamedModule == null) {
            testFailed = true;
            println(FailPrefix + "unnamed module of AppClassLoader was not observed");
        }
        if (extUnnamedModule == null) {
            testFailed = true;
            println(FailPrefix + "unnamed module of PlatformClassLoader was not observed");
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main() to determine targetClass
         */
        BreakpointEvent bpe = startToMain("ModulesTarg");
        targetClass = bpe.location().declaringType();

        if (!vm().canGetModuleInfo()) {
            testFailed = true;
            println(FailPrefix + "vm().canGetModuleInfo() returned false");
        }
        ModuleReference other = targetClass.module();
        modules = vm().allModules();

        int checkIdx = 0;

        for (ModuleReference module : modules) {
            checkModule(module, other, checkIdx++);
            other = module;
        }

        checkBaseModule();
        checkUnnamedModules();

        /*
         * resume the target until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ModulesTest: passed");
        } else {
            throw new Exception("ModulesTest: some checks failed");
        }
    }
}
