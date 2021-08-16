/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.metaspace;

import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * This class provides access to the input arguments to the VM.
 */
public class InputArguments {
    private static final List<String> args;

    static {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        args = runtimeMxBean.getInputArguments();
    }

    /**
     * Returns true if {@code arg} is an input argument to the VM.
     *
     * This is useful for checking boolean flags such as -XX:+UseSerialGC or
     * -XX:-UsePerfData.
     *
     * @param arg The name of the argument.
     * @return {@code true} if the given argument is an input argument,
     *         otherwise {@code false}.
     */
    public static boolean contains(String arg) {
        return args.contains(arg);
    }

    /**
     * Returns true if {@code prefix} is the start of an input argument to the
     * VM.
     *
     * This is useful for checking if flags describing a quantity, such as
     * -XX:+MaxMetaspaceSize=100m, is set without having to know the quantity.
     * To check if the flag -XX:MaxMetaspaceSize is set, use
     * {@code InputArguments.containsPrefix("-XX:MaxMetaspaceSize")}.
     *
     * @param prefix The start of the argument.
     * @return {@code true} if the given argument is the start of an input
     *         argument, otherwise {@code false}.
     */
    public static boolean containsPrefix(String prefix) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
