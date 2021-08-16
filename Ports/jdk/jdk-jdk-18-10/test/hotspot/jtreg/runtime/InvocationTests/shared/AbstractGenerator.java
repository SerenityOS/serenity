/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package shared;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractGenerator {
    protected final boolean dumpClasses;
    protected final boolean executeTests;
    private static int testNum = 0;

    protected AbstractGenerator(String[] args) {
        List<String> params = new ArrayList<String>(Arrays.asList(args));

        if (params.contains("--help")) {
            Utils.printHelp();
            System.exit(0);
        }

        dumpClasses = params.contains("--dump");
        executeTests = !params.contains("--noexecute");

        params.remove("--dump");
        params.remove("--noexecute");

        Utils.init(params);
    }

    /*******************************************************************/
    public static void writeToFile(File dir, Map<String, byte[]> classes) {
        for (String name : classes.keySet()) {
            try {
                writeToFile(dir, name, classes.get(name));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*******************************************************************/
    public static void writeToFile(File dir, String fullName, byte[] classBytecode) {
        if (!dir.isDirectory()) {
            throw new RuntimeException("Invalid parameter: dir doesn't point to an existing directory");
        }

        File classFile =
            new File(
                    dir.getPath() + File.separator
                    + fullName.replaceAll("\\.", File.separator)
                    + ".class"
                    );

        classFile.getParentFile().mkdirs();

        try {
            FileOutputStream fos = new FileOutputStream(classFile);
            try {
                fos.write(classBytecode);
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean exec(Map<String, byte[]> classes, String description, String calleeClassName, String classNameC, String[] callSites) throws ClassNotFoundException {
        boolean isPassed = true;

        testNum++;

        String caseDescription = String.format("%4d| %s", testNum, description);

        // Create test executor for a single case
        classes.put(
                ExecutorGenerator.className
                , new ExecutorGenerator(
                        caseDescription
                        , calleeClassName
                        , classNameC
                    ).generateExecutor(callSites)
        );

        // Dump generated set to disk, if needed
        if (dumpClasses) {
            File dir = new File("classes" + File.separator + String.format("%04d", testNum));
            dir.mkdirs();
            writeToFile(dir, classes);
        }

        ByteArrayClassLoader loader = new ByteArrayClassLoader(classes);

        Class paramClass;
        Class targetClass;
        Checker checker;

        try {
            paramClass = loader.loadClass(calleeClassName);
            targetClass = loader.loadClass(classNameC);

            checker = getChecker(paramClass, targetClass);
        } catch (Throwable e) {
            String result = Checker.abbreviateResult(e.getClass().getName());

            System.out.printf(caseDescription);

            for (String site : callSites) {
                System.out.printf(" %7s", result);
            }

            System.out.println("");

            return true;
        }

        if (executeTests) {
            // Check runtime behavior
            Caller caller = new Caller(loader, checker, paramClass, targetClass);
            boolean printedCaseDes = false;
            for (String site : callSites) {
                String callResult = caller.call(site);

                if (!caller.isPassed()) {
                    isPassed = false;
                    if (!printedCaseDes) {
                        System.out.printf(caseDescription);
                        printedCaseDes = true;
                    }
                    System.out.printf(" %7s", callResult);
                }
            }
            if (!caller.isPassed()) {
                System.out.println(" |   FAILED");
            }
        } else {
            for (String site : callSites) {
                String result = checker.check(loader.loadClass(site));
                System.out.printf(" %7s", Checker.abbreviateResult(result));
            }
        }

        return isPassed;
    }

    protected abstract Checker getChecker(Class paramClass, Class targetClass);
}
