/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/*
 * @test
 * @modules java.management
 * @summary verify that overriddes on the command line affect *.display and *.format properties
 * @run main/othervm
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX
 *                   -Duser.country=X1
 *                   -Duser.script=X2
 *                   -Duser.variant=X3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.display=YY
 *                   -Duser.country=X1 -Duser.country.display=Y1
 *                   -Duser.script=X2 -Duser.script.display=Y2
 *                   -Duser.variant=X3 -Duser.variant.display=Y3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.display=YY -Duser.language.format=ZZ
 *                   -Duser.country=X1 -Duser.country.display=Y1 -Duser.country.format=Z1
 *                   -Duser.script=X2 -Duser.script.display=Y2 -Duser.script.format=Z2
 *                   -Duser.variant=X3 -Duser.variant.display=Y3 -Duser.variant.format=Z3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.format=ZZ
 *                   -Duser.country=X1 -Duser.country.format=Z1
 *                   -Duser.script=X2 -Duser.script.format=Z2
 *                   -Duser.variant=X3 -Duser.variant.format=Z3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.display=XX
 *                   -Duser.country=X1 -Duser.country.display=X1
 *                   -Duser.script=X2 -Duser.script.display=X2
 *                   -Duser.variant=X3 -Duser.variant.display=X3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.display=XX -Duser.language.format=XX
 *                   -Duser.country=X1 -Duser.country.display=X1 -Duser.country.format=X1
 *                   -Duser.script=X2 -Duser.script.display=X2 -Duser.script.format=X2
 *                   -Duser.variant=X3 -Duser.variant.display=X3 -Duser.variant.format=X3
 *          LocaleCmdOverrides
 * @run main/othervm -Duser.language=XX -Duser.language.format=X1
 *                   -Duser.country.format=X1
 *                   -Duser.script.format=X2
 *                   -Duser.variant.format=X3
 *          LocaleCmdOverrides
 */
public class LocaleCmdOverrides {

    // Language, country, script, variant

    public static void main(String[] args) {
        Map<String, String> props = commandLineDefines();
        System.out.printf("props: %s%n", props);
        test("user.language", props);
        test("user.country", props);
        test("user.script", props);
        test("user.variant", props);
    }

    /*
     * Check each of the properties for a given basename.
     */
    static void test(String baseName, Map<String, String> args) {
        validateArg(baseName,"",  args);
        validateArg(baseName,".display", args);
        validateArg(baseName,".format", args);
    }

    // If an argument is -D defined, the corresponding property must be equal
    static void validateArg(String name, String ext, Map<String, String> args) {
        String extName = name.concat(ext);
        String arg = args.get(extName);
        String prop = System.getProperty(extName);
        if (arg == null && prop == null) {
            System.out.printf("No values for %s%n", extName);
        } else {
            System.out.printf("validateArg %s: arg: %s, prop: %s%n", extName, arg, prop);
        }

        if (arg != null) {
            if (!Objects.equals(arg, prop)) {
                throw new RuntimeException(extName + ": -D value should match property: "
                        + arg + " != " + prop);
            }
        } else if (prop != null) {
            // no command line arg for extName and some value for prop
            // Check that if a property is not overridden then it is not equal to the base
            if (ext != null && !ext.isEmpty()) {
                String value = System.getProperty(name);
                if (Objects.equals(value, prop)) {
                    throw new RuntimeException(extName + " property should not be equals to "
                            + name + " property: " + prop);
                }
            }
        }
    }

    /**
     * Extract the -D arguments from the command line and return a map of key, value.
     * @return a map of key, values defined by -D on the command line.
     */
    static HashMap<String, String> commandLineDefines() {
        HashMap<String, String> props = new HashMap<>();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtime.getInputArguments();
        System.out.printf("args: %s%n", args);
        for (String arg : args) {
            if (arg.startsWith("-Duser.")) {
                String[] kv = arg.substring(2).split("=");
                switch (kv.length) {
                    case 1:
                        props.put(kv[0], "");
                        break;
                    case 2:
                        props.put(kv[0], kv[1]);
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal property syntax: " + arg);
                }
            }
        }
        return props;
    }
}
