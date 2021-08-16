/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdb;

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
    public static final String ls = System.getProperty("line.separator");

    public static final String _catch       = "catch ";
    public static final String _class       = "class ";
    public static final String classes      = "classes" + ls;
    public static final String classpath    = "classpath" + ls;
    public static final String clear        = "clear ";
    public static final String cont         = "cont" + ls;
    public static final String disablegc    = "disablegc ";
    public static final String down         = "down ";
    public static final String dump         = "dump ";
    public static final String enablegc     = "enablegc";
    public static final String eval         = "eval ";
    public static final String exit         = "exit" + ls;
    public static final String exclude      = "exclude ";
    public static final String fields       = "fields ";
    public static final String help         = "help" + ls;
    public static final String ignore       = "ignore ";
    public static final String interrupt    = "interrupt ";
    public static final String kill         = "kill ";
    public static final String list         = "list ";
    public static final String locals       = "locals" + ls;
    public static final String lock         = "lock ";
    public static final String methods      = "methods ";
    public static final String monitor      = "monitor ";
    public static final String next         = "next" + ls;
    public static final String pop          = "pop" + ls;
    public static final String print        = "print ";
    public static final String quit         = "quit" + ls;
    public static final String read         = "read ";
    public static final String redefine     = "redefine ";
    public static final String reenter      = "reenter" + ls;
    public static final String resume       = "resume ";
    public static final String run          = "run ";
    public static final String set          = "set ";
    public static final String step         = "step" + ls;
    public static final String stepi        = "stepi" + ls;
    public static final String step_up      = "step up" + ls;
    public static final String stop_in      = "stop in ";
    public static final String stop_at      = "stop at ";
    public static final String suspend      = "suspend ";
    public static final String thread       = "thread ";
    public static final String threads      = "threads ";
    public static final String threadgroup  = "threadgroup ";
    public static final String threadgroups = "threadgroups" + ls;
    public static final String threadlocks  = "threadlocks ";
    public static final String trace        = "trace ";
    public static final String watch        = "watch ";
    public static final String where        = "where ";
    public static final String where_all    = "where all" + ls;
    public static final String wherei       = "wherei ";
    public static final String wherei_all   = "wherei all" + ls;
    public static final String unmonitor    = "unmonitor ";
    public static final String untrace      = "untrace ";
    public static final String unwatch      = "unwatch ";
    public static final String up           = "up ";
    public static final String use          = "use ";
    public static final String version      = "version" + ls;
}
