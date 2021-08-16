/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package gc.testlibrary;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public class Helpers {

    /**
     * Size of a long field in bytes
     */
    public static final int SIZE_OF_LONG = 8;

    // In case of 128 byte padding
    private static final int MAX_PADDING_SIZE = 128;

    /**
     * According class file format theoretical amount of fields in class is u2 which is (256 * 256 - 1).
     * Some service info takes place in constant pool and we really could make a class with lesser amount of fields.
     *
     * Since the exact value is not so important and I would like to avoid issues that may be caused by future changes/
     * different archs etc I selected (256 * 256 - 1024) for this constant.
     * The test works with other values too but the smaller the number the more classes we need to generate and it takes
     * more time
     */
    private static final int MAXIMUM_AMOUNT_OF_FIELDS_IN_CLASS = 256 * 256 - 1024;

    /**
     * Detects amount of extra bytes required to allocate a byte array.
     * Allocating a byte[n] array takes more then just n bytes in the heap.
     * Extra bytes are required to store object reference and the length.
     * This amount depends on bitness and other factors.
     *
     * @return byte[] memory overhead
     */
    public static int detectByteArrayAllocationOverhead() {

        WhiteBox whiteBox = WhiteBox.getWhiteBox();

        int zeroLengthByteArraySize = (int) whiteBox.getObjectSize(new byte[0]);

        // Since we do not know is there any padding in zeroLengthByteArraySize we cannot just take byte[0] size as overhead
        for (int i = 1; i < MAX_PADDING_SIZE + 1; ++i) {
            int realAllocationSize = (int) whiteBox.getObjectSize(new byte[i]);
            if (realAllocationSize != zeroLengthByteArraySize) {
                // It means we did not have any padding on previous step
                return zeroLengthByteArraySize - (i - 1);
            }
        }
        throw new Error("We cannot find byte[] memory overhead - should not reach here");
    }

    /**
     * Compiles a java class
     *
     * @param className class name
     * @param root      root directory - where .java and .class files will be put
     * @param source    class source
     * @throws IOException if cannot write file to specified directory
     */
    public static void compileClass(String className, Path root, String source) throws IOException {
        Path sourceFile = root.resolve(className + ".java");
        Files.write(sourceFile, source.getBytes());

        JDKToolLauncher jar = JDKToolLauncher.create("javac")
                .addToolArg("-d")
                .addToolArg(root.toAbsolutePath().toString())
                .addToolArg("-cp")
                .addToolArg(System.getProperty("java.class.path") + File.pathSeparator + root.toAbsolutePath())
                .addToolArg(sourceFile.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(jar.getCommand());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }

    /**
     * Generates class with specified name, which extends specified class, with specified constructor and specified
     * count of long fields
     * Generated class will looks like this:
     * public class ClassName extends SuperClass {
     * ClassName() {super();}
     * long f0;
     * ...
     * long fNNN;
     * <p>
     * }
     *
     * @param className   class name
     * @param superClass  super class. if null - no extends clause
     * @param constructor constructor. if null - no constructor
     * @param fieldCount  count of long fields
     * @return class text
     */
    public static String generate(String className, String superClass, String constructor, long fieldCount) {

        return new StringBuilder()
                .append(String.format("public class %s%s {\n", className, superClass == null ? ""
                        : " extends " + superClass))
                .append(constructor == null ? "" : constructor)
                .append(fieldsGenerator(fieldCount))
                .append("}\n")
                .toString();
    }

    /**
     * Generates specified amount of long fields
     * Result string will looks like this:
     * <p>
     * long f0;
     * ...
     * long fNNN;
     *
     * @param fieldCount count of long fields
     * @return generated fields
     */
    private static String fieldsGenerator(long fieldCount) {
        StringBuilder fieldsBuilder = new StringBuilder();

        for (int i = 0; i < fieldCount; ++i) {
            fieldsBuilder.append(String.format("long f%d;\n", i));
        }

        return fieldsBuilder.toString();
    }


    /**
     * Changes string from enum notation to class notation - i.e. "VERY_SMALL_CAT" to "VerySmallCat"
     *
     * @param enumName string in enum notation
     * @return string in class notation
     */
    public static String enumNameToClassName(String enumName) {
        if (enumName == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        boolean toLowerCase = false;
        for (int i = 0; i < enumName.length(); ++i) {
            if (enumName.charAt(i) == '_') {
                toLowerCase = false;
            } else {
                builder.append(toLowerCase ? String.valueOf(enumName.charAt(i)).toLowerCase() :
                        String.valueOf(enumName.charAt(i)));
                toLowerCase = true;
            }

        }
        return builder.toString();
    }

    /**
     * Generates and compiles class with instance of specified size and load it in specified class loader
     * Generated class will looks like this:
     * public class ClassName extends SuperClass {
     * long f0;
     * ...
     * long fNNN;
     * <p>
     * }
     *
     * @param classLoader  class loader
     * @param className    generated class name
     * @param instanceSize size of generated class' instance. Size should be aligned by 8 bytes
     * @param workDir      working dir where generated classes are put and compiled
     * @param prefix       prefix for service classes (ones we use to create chain of inheritance).
     *                     The names will be prefix_1, prefix_2,.., prefix_n
     * @return Class object of generated and compiled class loaded in specified class loader
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Class<?> generateCompileAndLoad(ClassLoader classLoader, String className, long instanceSize,
                                                  Path workDir, String prefix)
            throws IOException, ClassNotFoundException {

        generateByTemplateAndCompile(className, null, "public class ${ClassName} extends ${BaseClass} {\n${Fields}}\n",
                "", instanceSize, workDir, prefix);

        return classLoader.loadClass(className);
    }

    /**
     * Creates a class which instances will be approximately of the requested size.
     * This method produces a java source from a class template by substituting values instead of parameters.
     * Then the obtained source is compiled.
     * Generated class will looks like this:
     * classTemplate
     * constructorTemplate
     * long f0;
     * ...
     * long fNNN;
     * <p>
     * }
     *
     * @param className    generated class name
     * @param baseClass    base class
     * @param classTemplate class template - the first part of class. ${ClassName} and ${BaseClass} will be replaced
     *                      with values from className and baseClass,one entry of ${Fields} will be replaced with
     *                      generated long fields. Class template should look like this:
     *                      imports;
     *                      public class ${ClassName} extends ${BaseClass} {
     *                         public ${ClassName}  { some code here;}
     *                         some methods
     *                         ${Fields}
     *
     *                      }
     * @param constructorTemplate constructor template, ${ClassName} would be replaced on actual class name
     * @param instanceSize size of generated class' instance. Size should be aligned by 8 bytes
     * @param workDir      working dir where generated classes are put and compiled
     * @param prefix       prefix for service classes (ones we use to create chain of inheritance).
     *                     The names will be prefix_1, prefix_2,.., prefix_n
     * @return Class object of generated and compiled class loaded in specified class loader
     * @throws IOException if cannot write or read to workDir
     */
    public static void generateByTemplateAndCompile(String className, String baseClass, String classTemplate,
                                                    String constructorTemplate, long instanceSize, Path workDir,
                                                    String prefix) throws IOException {

        if (instanceSize % SIZE_OF_LONG != 0L) {
            throw new Error(String.format("Test bug: only sizes aligned by %d bytes are supported and %d was specified",
                    SIZE_OF_LONG, instanceSize));
        }

        int instanceSizeWithoutObjectHeaderInWords =
                (int) (instanceSize - WhiteBox.getWhiteBox().getObjectSize(new Object())) / SIZE_OF_LONG;

        if (instanceSizeWithoutObjectHeaderInWords <= 0) {
            throw new Error(String.format("Test bug: specified instance size is too small - %d."
                    + " Cannot generate any classes", instanceSize));
        }

        int sizeOfLastFile = instanceSizeWithoutObjectHeaderInWords % MAXIMUM_AMOUNT_OF_FIELDS_IN_CLASS;
        int generatedClassesCount = instanceSizeWithoutObjectHeaderInWords / MAXIMUM_AMOUNT_OF_FIELDS_IN_CLASS;

        // Do all the classes have the maximum number of fields?
        int fieldsInLastClassCount;

        if (sizeOfLastFile == 0) {
            fieldsInLastClassCount = MAXIMUM_AMOUNT_OF_FIELDS_IN_CLASS;
        } else {
            generatedClassesCount++;
            fieldsInLastClassCount = sizeOfLastFile;
        }

        // first (generatedClassesCount - 1) classes are just fillers - just long fields and constructor
        for (int i = 0; i < generatedClassesCount - 1; i++) {
            String clsName = prefix + i;

            Helpers.compileClass(clsName, workDir,
                    Helpers.generate(
                            clsName,
                            // first generated class extends base class
                            (i == 0 ? baseClass : prefix + (i - 1)),
                            constructorTemplate.replace("${ClassName}", clsName),
                            MAXIMUM_AMOUNT_OF_FIELDS_IN_CLASS));
        }

        // generating last class - the one with specified className
        Helpers.compileClass(className, workDir,
                classTemplate.replaceAll("\\$\\{ClassName\\}", className)
                        // if no fillers were generated (generatedClassesCount == 1)
                        // the last class should extends baseClass
                        // otherwise it should extend last generated filler class which name is
                        // prefix + (generatedClassesCount - 2)
                        // generatedClassesCount is always not smaller than 1
                        .replace("${BaseClass}",
                                generatedClassesCount == 1 ? baseClass :
                                        prefix + (generatedClassesCount - 2))
                        .replace("${Fields}", fieldsGenerator(fieldsInLastClassCount))
        );
    }

    /**
     * Waits until Concurent Mark Cycle finishes
     *
     * @param wb        Whitebox instance
     * @param sleepTime sleep time
     */
    public static void waitTillCMCFinished(WhiteBox wb, int sleepTime) {
        while (wb.g1InConcurrentMark()) {
            if (sleepTime > -1) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    System.out.println("Got InterruptedException while waiting for ConcMarkCycle to finish");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * @return a number formatter instance which prints numbers in a human
     * readable form, like 9_223_372_036_854_775_807.
     */
    public static NumberFormat numberFormatter() {
        DecimalFormat df = new DecimalFormat();
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setGroupingSeparator('_');
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
        return df;
    }
}
