/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc;

import java.io.*;

/**
 * <tt>Generator</tt> creates classes that have huge number of fields
 * (about 65536). All files are generated into a directory that is passed
 * as a first argument to method <code>main</code> The classes are intended to
 * be used used by <code>gc/gctests/LargeObjects/large00*</code> tests.
 * <p>
 * The class generates:
 * <ul>
 * <li> 8 "simple" parent classes that have 65500 fields of 7 primitive
 *      types and type Object and have just one modifier;
 * <li> 8 "simple" child classes that extend correspondent parent class and
 *      have 46 fileds of the same type (total number of fields in the class
 *      is over the limitation 65535);
 * <li> 8 "simple" child classes that extend correspondent parent class and
 *      have 35 fileds of the same type (total number of fields in the class
 *      is under the limitation 65535);
 * <li> 5 "combined" parent classes that have 32616 fields of each primitive
 *      types, type Object, one and two dimensional arrays of each type with
 *      one modifier;
 * <li> 5 "combined" child classes that extend correspondent parent class and
 *      have 33030 fileds (total number of fields in the class is over the
 *      limitation 65535);
 * <li> 5 "combined" child classes that extend correspondent parent class and
 *      have 33018 fileds (total number of fields in the class is under the
 *      limitation 65535).
 * </ul>
 */
public class Generator {

    // All tested field types
    private final static String[] TYPES = {"byte", "int", "long", "short",
                                           "boolean", "double", "float",
                                           "Object"};

    // All tested field modifiers
    private final static String[] SIMPLE_MODIFIERS = {"static", "private",
                                                      "public", "protected",
                                                      "transient", "volatile",
                                                      "static", "public"};

    // All tested field modifiers
    private final static String[] COMBINED_MODIFIERS = {"static", "public",
                                                       "protected", "transient",
                                                       "volatile"};

    // Some constants that are used during generaion of each file
    private final static String EXT = ".java";
    private final static String PACKAGE = "package nsk.share.gc.newclass;\n";
    private final static String CLASS_BEGIN = "public class ";

    private final static String PARENT_SUFFIX = "parent";
    private final static String PARENT_FIELD_PREFIX = "p";
    private final static int SIMPLE_PARENT_FIELDS_NUM = 65500;
    private final static int COMBINED_PARENT_FIELDS_NUM = 32616;

    private final static String FIELD_NAME = "f";

    private final static String LCHILD_SUFFIX = "lchild";
    private final static int SIMPLE_LCHILD_FIELDS_NUM = 46;
    private final static int COMBINED_LCHILD_FIELDS_NUM = 33030;

    private final static String SCHILD_SUFFIX = "schild";
    private final static int SIMPLE_SCHILD_FIELDS_NUM = 35;
    private final static int COMBINED_SCHILD_FIELDS_NUM = 33018;

    /**
     * Default constructor.
     *
     */
    public Generator() {}

    /**
     * Generates classes.
     *
     * @param argv array of arguments passed to the class.
     *
     * @throws <tt>FileNotFoundException</tt> if directory that.is passed as
     * the first argument does not exist.
     *
     */
    public static void main(String[] argv) throws FileNotFoundException {

        // Generate classes that have fields of one type and with one
        // modifier
        for (int i = 0; i < SIMPLE_MODIFIERS.length; i++)
            generateSimple(System.out, argv[0], TYPES[i], SIMPLE_MODIFIERS[i]);

        // Generate classes that have fields of different types with one
        // modifier
        for (int i = 0; i < COMBINED_MODIFIERS.length; i++)
            generateCombined(System.out, argv[0], COMBINED_MODIFIERS[i]);
    }

    // Generate classes that have fields of one type and with one modifier
    private static void generateSimple(PrintStream out, String dir, String type,
                                 String modifier) throws FileNotFoundException {

        // Generate parent class
        String parentName = modifier + "_" + type + "_" + PARENT_SUFFIX;
        try (PrintWriter pw = getPrintWriter(dir, parentName + EXT)) {

            pw.println(PACKAGE);
            pw.println(CLASS_BEGIN + parentName + " {");

            // Even if "private" modifier is tested, parent class must have "public"
            // fields, so that child class could have access to them
            String m = modifier;
            if (modifier.equals("private"))
                m = "public";
            for (int i = 0; i < SIMPLE_PARENT_FIELDS_NUM; i++)
                pw.println(m + " " + type + " "
                         + PARENT_FIELD_PREFIX + FIELD_NAME + (i + 1) + ";");
            pw.println("public Object objP = null;");
            pw.println("}");
        }

        // Generate two children that extend the parent
        generateSimpleChild(modifier + "_" + type + "_" + LCHILD_SUFFIX,
                            type, modifier, dir, parentName,
                            SIMPLE_LCHILD_FIELDS_NUM);
        generateSimpleChild(modifier + "_" + type + "_" + SCHILD_SUFFIX,
                            type, modifier, dir, parentName,
                            SIMPLE_SCHILD_FIELDS_NUM);
    }

    // Generate a child that extends specified parent class
    private static void generateSimpleChild(String className, String type,
                                            String modifier, String dir,
                                            String parentName, int num)
                                                  throws FileNotFoundException {
        try (PrintWriter pw = getPrintWriter(dir, className + EXT)) {

            pw.println(PACKAGE);
            pw.println(CLASS_BEGIN + className + " extends " + parentName + " {");
            for (int i = 0; i < num; i++)
                pw.println(modifier + " " + type + " " + FIELD_NAME + (i + 1)
                         + ";");
            pw.println("public Object objC = null;");
            pw.println("}");
        }
    }

    // Generate classes that have fields of different types with one modifier
    private static void generateCombined(PrintStream out, String dir,
                                 String modifier) throws FileNotFoundException {

        // Generate parent class
        String parentName = modifier + "_combination_" + PARENT_SUFFIX;
        try (PrintWriter pw = getPrintWriter(dir, parentName + EXT)){

            pw.println(PACKAGE);
            pw.println(CLASS_BEGIN + parentName + " {");
            String pattern = PARENT_FIELD_PREFIX + FIELD_NAME;
            for (int i = 0; i < COMBINED_PARENT_FIELDS_NUM; ) {
                for (int j = 0; j < TYPES.length; j++) {
                    pw.println(modifier + " " + TYPES[j] + " "
                             + pattern + (i + 1) + ", "
                             + pattern + (i + 2) + "[], "
                             + pattern + (i + 3) + "[][];");
                    i = i + 3;
                }
            }
            pw.println("public Object objP = null;");
            pw.println("}");
        }

        // Generate two children that extend the parent
        generateCombinedChild(modifier + "_combination_" + LCHILD_SUFFIX,
                              modifier, dir, parentName,
                              COMBINED_LCHILD_FIELDS_NUM);
        generateCombinedChild(modifier + "_combination_" + SCHILD_SUFFIX,
                              modifier, dir, parentName,
                              COMBINED_SCHILD_FIELDS_NUM);
    }

    // Generate a child that extends specified parent class
    private static void generateCombinedChild(String className, String modifier,
                                              String dir, String parentName,
                                              int num)
                                                  throws FileNotFoundException {
        try (PrintWriter pw = getPrintWriter(dir, className + EXT)) {

            pw.println(PACKAGE);
            pw.println(CLASS_BEGIN + className + " extends " + parentName + " {");
            for (int i = 0; i < num; )
                for (int j = 0; j < TYPES.length; j++) {
                    pw.println(modifier + " " + TYPES[j] + " "
                             + FIELD_NAME + (i + 1) + ", "
                             + FIELD_NAME + (i + 2) + "[], "
                             + FIELD_NAME + (i + 3) + "[][];");
                    i = i + 3;
                    if (i >= num)
                        break;
                }
            pw.println("public Object objC = null;");
            pw.println("}");
        }
    }

    // Create a new PrintWriter
    private static PrintWriter getPrintWriter(String dir, String name)
                                                  throws FileNotFoundException {
        FileOutputStream stream = new FileOutputStream(new File(dir, name));
        return new PrintWriter(stream, true);
    }
}
