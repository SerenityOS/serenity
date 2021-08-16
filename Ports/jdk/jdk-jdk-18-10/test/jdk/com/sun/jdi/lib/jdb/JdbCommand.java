/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package lib.jdb;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents list of commands of <code>jdb</code> from JDK1.4:
 *
 *   run [class [args]]        -- start execution of application's main class
 *
 *   threads [threadgroup]     -- list threads
 *   thread <thread id>        -- set default thread
 *   suspend [thread id(s)]    -- suspend threads (default: all)
 *   resume [thread id(s)]     -- resume threads (default: all)
 *   where [thread id] | all   -- dump a thread's stack
 *   wherei [thread id] | all  -- dump a thread's stack, with pc info
 *   up [n frames]             -- move up a thread's stack
 *   down [n frames]           -- move down a thread's stack
 *   kill <thread> <expr>      -- kill a thread with the given exception object
 *   interrupt <thread>        -- interrupt a thread
 *
 *   print <expr>              -- print value of expression
 *   dump <expr>               -- print all object information
 *   eval <expr>               -- evaluate expression (same as print)
 *   set <lvalue> = <expr>     -- assign new value to field/variable/array element
 *   locals                    -- print all local variables in current stack frame
 *
 *   classes                   -- list currently known classes
 *   class <class id>          -- show details of named class
 *   methods <class id>        -- list a class's methods
 *   fields <class id>         -- list a class's fields
 *
 *   threadgroups              -- list threadgroups
 *   threadgroup <name>        -- set current threadgroup
 *
 *   stop in <class id>.<method>[(argument_type,...)]
 *                             -- set a breakpoint in a method
 *   stop at <class id>:<line> -- set a breakpoint at a line
 *   clear <class id>.<method>[(argument_type,...)]
 *                             -- clear a breakpoint in a method
 *   clear <class id>:<line>   -- clear a breakpoint at a line
 *   clear                     -- list breakpoints
 *   catch <class id>          -- break when specified exception thrown
 *   ignore <class id>         -- cancel 'catch'  for the specified exception
 *   watch [access|all] <class id>.<field name>
 *                             -- watch access/modifications to a field
 *   unwatch [access|all] <class id>.<field name>
 *                             -- discontinue watching access/modifications to a field
 *   trace methods [thread]    -- trace method entry and exit
 *   untrace methods [thread]  -- stop tracing method entry and exit
 *   step                      -- execute current line
 *   step up                   -- execute until the current method returns to its caller
 *   stepi                     -- execute current instruction
 *   next                      -- step one line (step OVER calls)
 *   cont                      -- continue execution from breakpoint
 *
 *   list [line number|method] -- print source code
 *   use (or sourcepath) [source file path]
 *                             -- display or change the source path
 *   exclude [class id ... | "none"]
 *                             -- do not report step or method events for specified classes
 *   classpath                 -- print classpath info from target VM
 *
 *   monitor <command>         -- execute command each time the program stops
 *   monitor                   -- list monitors
 *   unmonitor <monitor#>      -- delete a monitor
 *   read <filename>           -- read and execute a command file
 *
 *   lock <expr>               -- print lock info for an object
 *   threadlocks [thread id]   -- print lock info for a thread
 *
 *   pop                       -- pop the stack through and including the current frame
 *   reenter                   -- same as pop, but current frame is reentered
 *   redefine <class id> <class file name>
 *                             -- redefine the code for a class
 *
 *   disablegc <expr>          -- prevent garbage collection of an object
 *   enablegc <expr>           -- permit garbage collection of an object
 *
 *   !!                        -- repeat last command
 *   <n> <command>             -- repeat command n times
 *   help (or ?)               -- list commands
 *   version                   -- print version information
 *   exit (or quit)            -- exit debugger
 *
 *   <class id>: full class name with package qualifiers or a
 *   pattern with a leading or trailing wildcard ('*').
 *   <thread id>: thread number as reported in the 'threads' command
 *   <expr>: a Java(tm) Programming Language expression.
 *   Most common syntax is supported.
 *
 *   Startup commands can be placed in either "jdb.ini" or ".jdbrc"
 *   in user.home or user.dir
 */
public class JdbCommand {
    final String cmd;
    boolean allowExit = false;
    // Default pattern to wait for command to complete
    Pattern waitForPattern = Jdb.PROMPT_REGEXP;

    public JdbCommand(String cmd) {
        this.cmd = cmd;
    }

    public JdbCommand allowExit() {
        allowExit = true;
        return this;
    }

    public JdbCommand waitForPrompt(String pattern, boolean isMultiline) {
        waitForPattern = Pattern.compile(pattern, isMultiline ? Pattern.MULTILINE : 0);
        return this;
    }


    public static JdbCommand run(String ... params) {
        return new JdbCommand("run " + Arrays.stream(params).collect(Collectors.joining(" ")));
    }
    public static JdbCommand cont() {
        return new JdbCommand("cont");
    }
    public static JdbCommand dump(String what) {
        return new JdbCommand("dump " + what);
    }
    public static JdbCommand quit() {
        // the command suppose jdb terminates
        return new JdbCommand("quit").allowExit();
    }
    public static JdbCommand stopAt(String targetClass, int lineNum) {
        return new JdbCommand("stop at " + targetClass + ":" + lineNum);
    }
    public static JdbCommand stopThreadAt(String targetClass, int lineNum) {
        return new JdbCommand("stop thread at " + targetClass + ":" + lineNum);
    }
    public static JdbCommand stopGoAt(String targetClass, int lineNum) {
        return new JdbCommand("stop go at " + targetClass + ":" + lineNum);
    }
    public static JdbCommand stopIn(String targetClass, String methodName) {
        return new JdbCommand("stop in " + targetClass + "." + methodName);
    }
    public static JdbCommand stopInThreadid(String targetClass, String methodName, String threadid) {
        return new JdbCommand("stop " + threadid + " in " + targetClass + "." + methodName);
    }
    public static JdbCommand thread(int threadNumber) {
        return new JdbCommand("thread " + threadNumber);
    }
    // clear <class id>:<line>   -- clear a breakpoint at a line
    public static JdbCommand clear(String targetClass, int lineNum) {
        return new JdbCommand("clear " + targetClass + ":" + lineNum);
    }

    // exception type used by catch/ignore
    public enum ExType{
        uncaught,
        caught,
        all
    }
    public static JdbCommand catch_(String classId) {
        return new JdbCommand("catch " + classId);
    }
    public static JdbCommand catch_(ExType eType, String classId) {
        return catch_(eType.toString() + " " + classId);
    }
    public static JdbCommand ignore(String classId) {
        return new JdbCommand("ignore " + classId);
    }
    public static JdbCommand ignore(ExType eType, String classId) {
        return ignore(eType.toString() + " " + classId);
    }

    public static JdbCommand step() {
        return new JdbCommand("step");
    }
    public static JdbCommand stepUp() {
        return new JdbCommand("step up");
    }
    public static JdbCommand next() {
        return new JdbCommand("next");
    }

    // where [thread id] | all
    public static JdbCommand where(String threadId) {
        return new JdbCommand("where " + threadId);
    }
    public static JdbCommand eval(String expr) {
        return new JdbCommand("eval " + expr);
    }
    public static JdbCommand print(String expr) {
        return new JdbCommand("print " + expr);
    }

    public static JdbCommand locals() {
        return new JdbCommand("locals");
    }

    public static JdbCommand set(String lvalue, String expr) {
        return new JdbCommand("set " + lvalue + " = " + expr);
    }

    public static JdbCommand lock(String expr) {
        return new JdbCommand("lock " + expr);
    }

    public static JdbCommand methods(String classId) {
        return new JdbCommand("methods " + classId);
    }

    public static JdbCommand threads() {
        return new JdbCommand("threads");
    }

    // trace [go] methods [thread]
    //                           -- trace method entries and exits.
    //                           -- All threads are suspended unless 'go' is specified
    // trace [go] method exit | exits [thread]
    //                           -- trace the current method's exit, or all methods' exits
    //                           -- All threads are suspended unless 'go' is specified
    // untrace [methods]         -- stop tracing method entrys and/or exits
    public static JdbCommand trace(boolean go, String  mode, Integer threadId) {
        return new JdbCommand(" trace"
                + (go ? " go" : "")
                + (mode != null ? " " + mode : "")
                + (threadId != null ? " " + threadId.toString() : ""));
    }
    // prints trace status
    public static JdbCommand trace() {
        return trace(false, null, null);
    }
    public static JdbCommand traceMethods(boolean go, Integer threadId) {
        return trace(go, "methods", threadId);
    }
    public static JdbCommand traceMethodExit(boolean go, Integer threadId) {
        return trace(go, "method exit", threadId);
    }
    public static JdbCommand traceMethodExits(boolean go, Integer threadId) {
        return trace(go, "method exits", threadId);
    }
    public static JdbCommand untrace() {
        return new JdbCommand("untrace");
    }

    // watch [access|all] <class id>.<field name>
    public static JdbCommand watch(String classId, String fieldName) {
        return new JdbCommand("watch " + classId + "." + fieldName);
    }

    public static JdbCommand pop() {
        return new JdbCommand("pop");
    }

    public static JdbCommand redefine(String classId, String classFileName) {
        return new JdbCommand("redefine " + classId + " " + classFileName);
    }
}
