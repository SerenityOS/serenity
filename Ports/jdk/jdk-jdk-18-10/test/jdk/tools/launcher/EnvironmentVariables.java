/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * see TestSpecialArgs.java
 * bug 7131021
 * summary Checks for environment variables set by the launcher
 * author anthony.petrov@oracle.com: area=launcher
 */

public class EnvironmentVariables {
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new RuntimeException("ERROR: two command line arguments expected");
        }

        String name = args[0];
        String expect = args[1];
        String key = null;

        if (!name.endsWith("*")) {
            key = name;
        } else {
            name = name.split("\\*")[0];

            for (String s : System.getenv().keySet()) {
                if (s.startsWith(name)) {
                    if (key == null) {
                        key = s;
                    } else {
                        System.err.println("WARNING: more variables match: " + s);
                    }
                }
            }

            if (key == null) {
                throw new RuntimeException("ERROR: unable to find a match for: " + name);
            }
        }

        System.err.println("Will check the variable named: '" + key +
                "' expecting the value: '" + expect + "'");

        if (!System.getenv().containsKey(key)) {
            throw new RuntimeException("ERROR: the variable '" + key +
                    "' is not present in the environment");
        }

        if (!expect.equals(System.getenv().get(key))) {
            throw new RuntimeException("ERROR: expected: '" + expect +
                    "', got: '" + System.getenv().get(key) + "'");
        }
        for (String x : args) {
            System.err.print(x + " ");
        }
        System.err.println("-----> Passed!");
    }
}

