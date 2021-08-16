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

import java.util.List;

/**
 * Just a set of constants
 */
public class Utils {
    public static final String TARGET_METHOD_NAME = "m";
    public static int version = 50;

    public static boolean isACC_SUPER = false;

    public static void init(List<String> args) {
        for (String param : args) {
            String name = "classfile_version";
            String pattern = "--"+name+"=";
            if (param.startsWith(pattern)) {
                String value = param.substring(pattern.length());
                int majorVersion = 50;
                int minorVersion = 0;

                try {
                    String[] versions = value.split(":");
                    if (versions.length > 2) {
                        throw new RuntimeException(String.format("Unknown %s value: %s", name, value));
                    }

                    try {
                        majorVersion = Integer.parseInt(versions[0]);
                        if (versions.length > 1) {
                            minorVersion = Integer.parseInt(versions[1]);
                        }
                    } catch(Exception e) {
                        throw new RuntimeException(String.format("Can't parse %s value: '%s'", name, value));
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: "+e.getMessage());
                }

                version = majorVersion + (minorVersion << 16);

                System.out.printf("INFO: Class file version: major: %d; minor: %d\n", majorVersion, minorVersion);

                if (majorVersion < 49 && !args.contains("--no_acc_super")) {
                    isACC_SUPER = true;
                    System.out.printf("INFO: Enabling ACC_SUPER flag for major: %d\nTo disable it, specify --no_acc_super option.\n", majorVersion, minorVersion);
                }
            } else if (param.equals("--no_acc_super")){
                System.out.println("INFO: ACC_SUPER flag is disabled");
                isACC_SUPER = false;
            } else if (param.equals("--acc_super")){
                isACC_SUPER = true;
            } else {
                System.out.println("ERROR: Unknown option: "+param);
                printHelp();
                System.exit(1);
            }
        }
    }

    public static void printHelp() {
        System.out.println(
                 "Supported parameters:\n"
               + "\t--classfile_version=major_version[:minor_version]\n"
               + "\t\t- specify class file version for generated classes\n"
               + "\t--no_acc_super\n"
               + "\t\t- don't add ACC_SUPER flag into generated classes\n"
               + "\t--acc_super\n"
               + "\t\t- force ACC_SUPER flag in generated classes\n"
               + "\t--dump\n"
               + "\t\t- dump generated classes\n"
               + "\t--noexecute\n"
               + "\t\t- print only expected results, don't execute tests\n"
        );
    }

    /*******************************************************************/
    public static String getInternalName(String s) {
        return s.replaceAll("\\.", "/");
    }

}
