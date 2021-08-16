/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for finding the command on the current system
 */
public class UnixCommands {

    public static final boolean isUnix = ! System.getProperty("os.name").startsWith("Windows");
    public static final boolean isLinux = System.getProperty("os.name").startsWith("Linux");

    private static final String[] paths = {"/bin", "/usr/bin"};

    private static Map<String,String> nameToCommand = new HashMap<>(16);

    /**
     * Throws Error unless every listed command is available on the system
     */
    public static void ensureCommandsAvailable(String... commands) {
        for (String command : commands) {
            if (findCommand(command) == null) {
                throw new Error("Command '" + command + "' not found; bailing out");
            }
        }
    }

    /**
     * If the path to the command could be found, returns the command with the full path.
     * Otherwise, returns null.
     */
    public static String cat()   { return findCommand("cat"); }
    public static String sh()    { return findCommand("sh"); }
    public static String kill()  { return findCommand("kill"); }
    public static String sleep() { return findCommand("sleep"); }
    public static String tee()   { return findCommand("tee"); }
    public static String echo()  { return findCommand("echo"); }

    public static String findCommand(String name) {
        if (nameToCommand.containsKey(name)) {
            return nameToCommand.get(name);
        }
        String command = findCommand0(name);
        nameToCommand.put(name, command);
        return command;
    }

    private static String findCommand0(String name) {
        for (String path : paths) {
            File file = new File(path, name);
            if (file.canExecute()) {
                return file.getPath();
            }
        }
        return null;
    }
}
