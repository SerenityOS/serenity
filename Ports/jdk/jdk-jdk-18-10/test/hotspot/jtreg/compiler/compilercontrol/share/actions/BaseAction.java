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

package compiler.compilercontrol.share.actions;

import compiler.compilercontrol.share.pool.PoolHelper;
import compiler.compilercontrol.share.scenario.State;
import jdk.test.lib.util.Pair;
import jdk.test.lib.process.ProcessTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Executable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class BaseAction {
    private static final List<Pair<Executable, Callable<?>>> METHODS;
    private static final Map<String, Executable> METHODS_NAMES;

    static {
        METHODS = new PoolHelper().getAllMethods();
        METHODS_NAMES = METHODS.stream().collect(Collectors.toMap(
                pair -> pair.first.toGenericString(),
                pair -> pair.first));
    }

    public static void main(String[] args) {
        new BaseAction().communicate(args);
    }

    /*
     * args[0] is a port to connect
     * args[1] is an optional parameter that shows that the state map should be
     *         passed
     */
    protected void communicate(String[] args) {
        if (args.length < 1) {
            throw new Error("TESTBUG: requires port as parameter: "
                    + Arrays.toString(args));
        }
        boolean getStates = false;
        if (args.length == 2) {
            if ("states".equals(args[1])) {
                getStates = true;
            } else {
                throw new Error("TESTBUG: incorrect argument: "+ args[1]);
            }
        }
        long pid;
        try {
            pid = ProcessTools.getProcessId();
        } catch (Exception e) {
            throw new Error("Could not determine own pid", e);
        }
        int port = Integer.parseInt(args[0]);
        System.out.println("INFO: Client connection port = " + port);
        List<String> lines;
        try (
                Socket socket = new Socket(InetAddress.getLocalHost(), port);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()))) {
            // send own pid to execute jcmd if needed
            out.println(String.valueOf(pid));
            out.flush();
            if (getStates) {
                lines = in.lines().collect(Collectors.toList());
                check(decodeMap(lines));
            } else {
                in.readLine();
            }
        } catch (IOException e) {
            throw new Error("Error on performing network operation", e);
        }
    }

    private Map<Executable, State> decodeMap(List<String> lines) {
        if (lines == null || lines.size() == 0) {
            throw new Error("TESTBUG: unexpected lines list");
        }
        Map<Executable, State> stateMap = new HashMap<>();
        int startIndex = 0;
        ListIterator<String> iterator = lines.listIterator();
        while (iterator.hasNext()) {
            int index = iterator.nextIndex();
            String next = iterator.next();
            switch (next) {
                case "{" :
                    startIndex = index;
                    break;
                case "}" :
                    // method name goes after {
                    Executable executable = METHODS_NAMES.get(lines.get(
                            ++startIndex));
                    // state description starts after method
                    State state = State.fromString(lines.subList(++startIndex,
                            index).toArray(new String[index - startIndex]));
                    stateMap.put(executable, state);
                    break;
            }
        }
        return stateMap;
    }

    protected void check(Map<Executable, State> methodStates) {
        // Check each method from the pool
        METHODS.forEach(pair -> {
            Executable x = pair.first;
            CompileAction.checkCompiled(x, methodStates.get(x));
        });
    }
}
