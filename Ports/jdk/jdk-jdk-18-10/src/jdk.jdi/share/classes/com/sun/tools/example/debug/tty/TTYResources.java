/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

/**
 * <p> This class represents the <code>ResourceBundle</code>
 * for the following package(s):
 *
 * <ol>
 * <li> com.sun.tools.example.debug.tty
 * </ol>
 *
 */
public class TTYResources extends java.util.ListResourceBundle {


    /**
     * Returns the contents of this <code>ResourceBundle</code>.
     *
     * <p>
     *
     * @return the contents of this <code>ResourceBundle</code>.
     */
    @Override
    public Object[][] getContents() {
        Object[][] temp = new Object[][] {
        // NOTE: The value strings in this file containing "{0}" are
        //       processed by the java.text.MessageFormat class.  Any
        //       single quotes appearing in these strings need to be
        //       doubled up.
        //
        // LOCALIZE THIS
        {"** classes list **", "** classes list **\n{0}"},
        {"** fields list **", "** fields list **\n{0}"},
        {"** methods list **", "** methods list **\n{0}"},
        {"*** Reading commands from", "*** Reading commands from {0}"},
        {"All threads resumed.", "All threads resumed."},
        {"All threads suspended.", "All threads suspended."},
        {"Argument is not defined for connector:", "Argument {0} is not defined for connector: {1}"},
        {"Arguments match no method", "Arguments match no method"},
        {"Array:", "Array: {0}"},
        {"Array element is not a method", "Array element is not a method"},
        {"Array index must be a integer type", "Array index must be a integer type"},
        {"base directory:", "base directory: {0}"},
        {"Breakpoint hit:", "Breakpoint hit: "},
        {"breakpoint", "breakpoint {0}"},
        {"Breakpoints set:", "Breakpoints set:"},
        {"Breakpoints can be located only in classes.", "Breakpoints can be located only in classes.  {0} is an interface or array."},
        {"Can only trace", "Can only trace 'methods' or 'method exit' or 'method exits'"},
        {"cannot redefine existing connection", "{0} cannot redefine existing connection"},
        {"Cannot assign to a method invocation", "Cannot assign to a method invocation"},
        {"Cannot specify command line with connector:", "Cannot specify command line with connector: {0}"},
        {"Cannot specify target vm arguments with connector:", "Cannot specify target VM arguments with connector: {0}"},
        {"Class containing field must be specified.", "Class containing field must be specified."},
        {"Class:", "Class: {0}"},
        {"Classic VM no longer supported.", "Classic VM no longer supported."},
        {"classpath:", "classpath: {0}"},
        {"colon mark", ":"},
        {"colon space", ": "},
        {"Command is not supported on the target VM", "Command ''{0}'' is not supported on the target VM"},
        {"Command is not supported on a read-only VM connection", "Command ''{0}'' is not supported on a read-only VM connection"},
        {"Command not valid until the VM is started with the run command", "Command ''{0}'' is not valid until the VM is started with the ''run'' command"},
        {"Condition must be boolean", "Condition must be boolean"},
        {"Connector and Transport name", "  Connector: {0}  Transport: {1}"},
        {"Connector argument nodefault", "    Argument: {0} (no default)"},
        {"Connector argument default", "    Argument: {0} Default value: {1}"},
        {"Connector description", "    description: {0}"},
        {"Connector required argument nodefault", "    Required Argument: {0} (no default)"},
        {"Connector required argument default", "    Required Argument: {0} Default value: {1}"},
        {"Connectors available", "Available connectors are:"},
        {"Constant is not a method", "Constant is not a method"},
        {"Could not open:", "Could not open: {0}"},
        {"Current method is native", "Current method is native"},
        {"Current thread died. Execution continuing...", "Current thread {0} died. Execution continuing..."},
        {"Current thread isnt suspended.", "Current thread isn't suspended."},
        {"Current thread not set.", "Current thread not set."},
        {"dbgtrace flag value must be an integer:", "dbgtrace flag value must be an integer: {0}"},
        {"dbgtrace command value must be an integer:", "dbgtrace command value must be an integer: {0}"},
        {"Deferring.", "Deferring {0}.\nIt will be set after the class is loaded."},
        {"End of stack.", "End of stack."},
        {"Error popping frame", "Error popping frame - {0}"},
        {"Error reading file", "Error reading ''{0}'' - {1}"},
        {"Error redefining class to file", "Error redefining {0} to {1} - {2}"},
        {"exceptionSpec all", "all {0}"},
        {"exceptionSpec caught", "caught {0}"},
        {"exceptionSpec uncaught", "uncaught {0}"},
        {"Exception in expression:", "Exception in expression: {0}"},
        {"Exception occurred caught", "Exception occurred: {0} (to be caught at: {1})"},
        {"Exception occurred uncaught", "Exception occurred: {0} (uncaught)"},
        {"Exceptions caught:", "Break when these exceptions occur:"},
        {"Expected at, in, or an integer <thread_id>:", "Expected \"at\", \"in\", or an integer <thread_id>: {0}"},
        {"expr is null", "{0} = null"},
        {"expr is value", "{0} = {1}"},
        {"expr is value <collected>", "  {0} = {1} <collected>"},
        {"Expression cannot be void", "Expression cannot be void"},
        {"Expression must evaluate to an object", "Expression must evaluate to an object"},
        {"extends:", "extends: {0}"},
        {"Extra tokens after breakpoint location", "Extra tokens after breakpoint location"},
        {"Failed reading output", "Failed reading output of child java interpreter."},
        {"Fatal error", "Fatal error:"},
        {"Field access encountered before after", "Field ({0}) is {1}, will be {2}: "},
        {"Field access encountered", "Field ({0}) access encountered: "},
        {"Field to unwatch not specified", "Field to unwatch not specified."},
        {"Field to watch not specified", "Field to watch not specified."},
        {"GC Disabled for", "GC Disabled for {0}:"},
        {"GC Enabled for", "GC Enabled for {0}:"},
        {"grouping begin character", "{"},
        {"grouping end character", "}"},
        {"Illegal Argument Exception", "Illegal Argument Exception"},
        {"Illegal connector argument", "Illegal connector argument: {0}"},
        {"implementor:", "implementor: {0}"},
        {"implements:", "implements: {0}"},
        {"Initializing progname", "Initializing {0} ..."},
        {"Input stream closed.", "Input stream closed."},
        {"Interface:", "Interface: {0}"},
        {"Internal debugger error.", "Internal debugger error."},
        {"Internal error: null ThreadInfo created", "Internal error: null ThreadInfo created"},
        {"Internal error; unable to set", "Internal error; unable to set {0}"},
        {"Internal exception during operation:", "Internal exception during operation:\n    {0}"},
        {"Internal exception:", "Internal exception:"},
        {"Invalid argument type name", "Invalid argument type name"},
        {"Invalid assignment syntax", "Invalid assignment syntax"},
        {"Invalid command syntax", "Invalid command syntax"},
        {"Invalid connect type", "Invalid connect type"},
        {"Invalid consecutive invocations", "Invalid consecutive invocations"},
        {"Invalid exception object", "Invalid exception object"},
        {"Invalid line number specified", "Invalid line number specified"},
        {"Invalid <method_name> specification:", "Invalid <method_name> specification: {0}"},
        {"Invalid option on class command", "Invalid option on class command"},
        {"invalid option", "invalid option: {0}"},
        {"Invalid thread status.", "Invalid thread status."},
        {"Invalid <thread_id>:", "Invalid <thread_id>: {0}"},
        {"Invalid transport name:", "Invalid transport name: {0}"},
        {"Invalid <class>.<method_name> specification", "Invalid <class>.<method_name> specification"},
        {"I/O exception occurred:", "I/O Exception occurred: {0}"},
        {"is an ambiguous method name in", "\"{0}\" is an ambiguous method name in \"{1}\""},
        {"is an invalid line number for",  "{0,number,integer} is an invalid line number for {1}"},
        {"is not a valid class name", "\"{0}\" is not a valid class name."},
        {"is not a valid field name", "\"{0}\" is not a valid field name."},
        {"is not a valid id or class name", "\"{0}\" is not a valid id or class name."},
        {"is not a valid line number or method name for", "\"{0}\" is not a valid line number or method name for class \"{1}\""},
        {"is not a valid method name", "\"{0}\" is not a valid method name."},
        {"is not a valid thread id", "\"{0}\" is not a valid thread id."},
        {"is not a valid threadgroup name", "\"{0}\" is not a valid threadgroup name."},
        {"jdb prompt with no current thread", "> "},
        {"jdb prompt thread name and current stack frame", "{0}[{1,number,integer}] "},
        {"killed", "{0} killed"},
        {"killing thread:", "killing thread: {0}"},
        {"Line number information not available for", "Source line numbers not available for this location."},
        {"line number", ":{0,number,integer}"},
        {"list field typename and name", "{0} {1}\n"},
        {"list field typename and name inherited", "{0} {1} (inherited from {2})\n"},
        {"list field typename and name hidden", "{0} {1} (hidden)\n"},
        {"Listening at address:", "Listening at address: {0}"},
        {"Local variable information not available.", "Local variable information not available.  Compile with -g to generate variable information"},
        {"Local variables:", "Local variables:"},
        {"<location unavailable>", "<location unavailable>"},
        {"location", "\"thread={0}\", {1}"},
        {"locationString", "{0}.{1}(), line={2,number,integer} bci={3,number,integer}"},
        {"Main class and arguments must be specified", "Main class and arguments must be specified"},
        {"Method arguments:", "Method arguments:"},
        {"Method entered:", "Method entered: "},
        {"Method exited:",  "Method exited"},
        {"Method exitedValue:", "Method exited: return value = {0}, "},
        {"Method is overloaded; specify arguments", "Method {0} is overloaded; specify arguments"},
        {"minus version", "This is {0} version {1,number,integer}.{2,number,integer} (Java SE version {3})"},
        {"Missing at or in", "Missing \"at\" or \"in\""},
        {"Monitor information for thread", "Monitor information for thread {0}:"},
        {"Monitor information for expr", "Monitor information for {0} ({1}):"},
        {"More than one class named", "More than one class named: ''{0}''"},
        {"native method", "native method"},
        {"nested:", "nested: {0}"},
        {"No attach address specified.", "No attach address specified."},
        {"No breakpoints set.", "No breakpoints set."},
        {"No class named", "No class named ''{0}''"},
        {"No class specified.", "No class specified."},
        {"No classpath specified.", "No classpath specified."},
        {"No code at line", "No code at line {0,number,integer} in {1}"},
        {"No connect specification.", "No connect specification."},
        {"No connector named:", "No connector named: {0}"},
        {"No current thread", "No current thread"},
        {"No default thread specified:", "No default thread specified: use the \"thread\" command first."},
        {"No exception object specified.", "No exception object specified."},
        {"No exceptions caught.", "No exceptions caught."},
        {"No expression specified.", "No expression specified."},
        {"No field in", "No field {0} in {1}"},
        {"No frames on the current call stack", "No frames on the current call stack"},
        {"No linenumber information for", "No linenumber information for {0}.  Try compiling with debugging on."},
        {"No local variables", "No local variables"},
        {"No method in", "No method {0} in {1}"},
        {"No method specified.", "No method specified."},
        {"No monitor numbered:", "No monitor numbered: {0}"},
        {"No monitors owned", "  No monitors owned"},
        {"No object specified.", "No object specified."},
        {"No objects specified.", "No objects specified."},
        {"No save index specified.", "No save index specified."},
        {"No saved values", "No saved values"},
        {"No source information available for:", "No source information available for: {0}"},
        {"No sourcedebugextension specified", "No SourceDebugExtension specified"},
        {"No sourcepath specified.", "No sourcepath specified."},
        {"No thread specified.", "No thread specified."},
        {"No VM connected", "No VM connected"},
        {"No waiters", "  No waiters"},
        {"not a class", "{0} is not a class"},
        {"Not a monitor number:", "Not a monitor number: ''{0}''"},
        {"not found (try the full name)", "{0} not found (try the full name)"},
        {"Not found:", "Not found: {0}"},
        {"not found", "{0} not found"},
        {"Not owned", "  Not owned"},
        {"Not waiting for a monitor", "  Not waiting for a monitor"},
        {"Nothing suspended.", "Nothing suspended."},
        {"object description and id", "({0}){1}"},
        {"Operation is not supported on the target VM", "Operation is not supported on the target VM"},
        {"operation not yet supported", "operation not yet supported"},
        {"Owned by:", "  Owned by: {0}, entry count: {1,number,integer}"},
        {"Owned monitor:", "  Owned monitor: {0}"},
        {"Parse exception:", "Parse Exception: {0}"},
        {"printclearcommandusage", "Usage clear <class>:<line_number> or\n      clear <class>.<method_name>[(argument_type,...)]"},
        {"printstopcommandusage",
         "Usage: stop [go|thread] [<thread_id>] <at|in> <location>\n" +
         "  If \"go\" is specified, immediately resume after stopping\n" +
         "  If \"thread\" is specified, only suspend the thread we stop in\n" +
         "  If neither \"go\" nor \"thread\" are specified, suspend all threads\n" +
         "  If an integer <thread_id> is specified, only stop in the specified thread\n" +
         "  \"at\" and \"in\" have the same meaning\n" +
         "  <location> can either be a line number or a method:\n" +
         "    <class_id>:<line_number>\n" +
         "    <class_id>.<method>[(argument_type,...)]"
        },
        {"Removed:", "Removed: {0}"},
        {"Requested stack frame is no longer active:", "Requested stack frame is no longer active: {0,number,integer}"},
        {"run <args> command is valid only with launched VMs", "'run <args>' command is valid only with launched VMs"},
        {"run", "run {0}"},
        {"saved", "{0} saved"},
        {"Set deferred", "Set deferred {0}"},
        {"Set", "Set {0}"},
        {"Source file not found:", "Source file not found: {0}"},
        {"source line number and line", "{0,number,integer}    {1}"},
        {"source line number current line and line", "{0,number,integer} => {1}"},
        {"sourcedebugextension", "SourceDebugExtension -- {0}"},
        {"Specify class and method", "Specify class and method"},
        {"Specify classes to redefine", "Specify classes to redefine"},
        {"Specify file name for class", "Specify file name for class {0}"},
        {"stack frame dump with pc", "  [{0,number,integer}] {1}.{2} ({3}), pc = {4}"},
        {"stack frame dump", "  [{0,number,integer}] {1}.{2} ({3})"},
        {"Step completed:", "Step completed: "},
        {"Stopping due to deferred breakpoint errors.", "Stopping due to deferred breakpoint errors.\n"},
        {"subclass:", "subclass: {0}"},
        {"subinterface:", "subinterface: {0}"},
        {"tab", "\t{0}"},
        {"Target VM failed to initialize.", "Target VM failed to initialize."},
        {"The application exited", "The application exited"},
        {"The application has been disconnected", "The application has been disconnected"},
        {"The gc command is no longer necessary.", "The 'gc' command is no longer necessary.\n" +
         "All objects are garbage collected as usual. Use 'enablegc' and 'disablegc'\n" +
         "commands to control garbage collection of individual objects."},
        {"The load command is no longer supported.", "The 'load' command is no longer supported."},
        {"The memory command is no longer supported.", "The 'memory' command is no longer supported."},
        {"The VM does not use paths", "The VM does not use paths"},
        {"Thread is not running (no stack).", "Thread is not running (no stack)."},
        {"Thread number not specified.", "Thread number not specified."},
        {"Thread:", "{0}:"},
        {"Thread Group:", "Group {0}:"},
        {"Thread description name unknownStatus BP",  "  {0} {1} unknown (at breakpoint)"},
        {"Thread description name unknownStatus",     "  {0} {1} unknown"},
        {"Thread description name zombieStatus BP",   "  {0} {1} zombie (at breakpoint)"},
        {"Thread description name zombieStatus",      "  {0} {1} zombie"},
        {"Thread description name runningStatus BP",  "  {0} {1} running (at breakpoint)"},
        {"Thread description name runningStatus",     "  {0} {1} running"},
        {"Thread description name sleepingStatus BP", "  {0} {1} sleeping (at breakpoint)"},
        {"Thread description name sleepingStatus",    "  {0} {1} sleeping"},
        {"Thread description name waitingStatus BP",  "  {0} {1} waiting in a monitor (at breakpoint)"},
        {"Thread description name waitingStatus",     "  {0} {1} waiting in a monitor"},
        {"Thread description name condWaitstatus BP", "  {0} {1} cond. waiting (at breakpoint)"},
        {"Thread description name condWaitstatus",    "  {0} {1} cond. waiting"},
        {"Thread has been resumed", "Thread has been resumed"},
        {"Thread not suspended", "Thread not suspended"},
        {"thread group number description name", "{0,number,integer}. {1} {2}"},
        {"Threadgroup name not specified.", "Threadgroup name not specified."},
        {"<thread_id> option not valid until the VM is started with the run command",
         "<thread_id> option not valid until the VM is started with the run command"},
        {"Threads must be suspended", "Threads must be suspended"},
        {"trace method exit in effect for", "trace method exit in effect for {0}"},
        {"trace method exits in effect", "trace method exits in effect"},
        {"trace methods in effect", "trace methods in effect"},
        {"trace go method exit in effect for", "trace go method exit in effect for {0}"},
        {"trace go method exits in effect", "trace go method exits in effect"},
        {"trace go methods in effect", "trace go methods in effect"},
        {"trace not in effect", "trace not in effect"},
        {"Unable to attach to target VM.", "Unable to attach to target VM."},
        {"Unable to display process output:", "Unable to display process output: {0}"},
        {"Unable to launch target VM.", "Unable to launch target VM."},
        {"Unable to set deferred", "Unable to set deferred {0} : {1}"},
        {"Unable to set main class and arguments", "Unable to set main class and arguments"},
        {"Unable to set", "Unable to set {0} : {1}"},
        {"Unexpected event type", "Unexpected event type: {0}"},
        {"unknown", "unknown"},
        {"Unmonitoring", "Unmonitoring {0} "},
        {"Unrecognized command.  Try help...", "Unrecognized command: ''{0}''.  Try help..."},
        {"Usage: catch exception", "Usage: catch [uncaught|caught|all] <class id>|<class pattern>"},
        {"Usage: ignore exception", "Usage: ignore [uncaught|caught|all] <class id>|<class pattern>"},
        {"Usage: down [n frames]", "Usage: down [n frames]"},
        {"Usage: kill <thread id> <throwable>", "Usage: kill <thread id> <throwable>"},
        {"Usage: read <command-filename>", "Usage: read <command-filename>"},
        {"Usage: unmonitor <monitor#>", "Usage: unmonitor <monitor#>"},
        {"Usage: up [n frames]", "Usage: up [n frames]"},
        {"Use java minus X to see", "Use 'java -X' to see the available non-standard options"},
        {"VM already running. use cont to continue after events.", "VM already running. Use 'cont' to continue after events."},
        {"VM Started:", "VM Started: "},
        {"vmstartexception", "VM start exception: {0}"},
        {"Waiting for monitor:", "   Waiting for monitor: {0}"},
        {"Waiting thread:", " Waiting thread: {0}"},
        {"watch accesses of", "watch accesses of {0}.{1}"},
        {"watch modification of", "watch modification of {0}.{1}"},
        {"zz help text",
             "** command list **\n" +
             "connectors                -- list available connectors and transports in this VM\n" +
             "\n" +
             "run [class [args]]        -- start execution of application's main class\n" +
             "\n" +
             "threads [threadgroup]     -- list threads\n" +
             "thread <thread id>        -- set default thread\n" +
             "suspend [thread id(s)]    -- suspend threads (default: all)\n" +
             "resume [thread id(s)]     -- resume threads (default: all)\n" +
             "where [<thread id> | all] -- dump a thread's stack\n" +
             "wherei [<thread id> | all]-- dump a thread's stack, with pc info\n" +
             "up [n frames]             -- move up a thread's stack\n" +
             "down [n frames]           -- move down a thread's stack\n" +
             "kill <thread id> <expr>   -- kill a thread with the given exception object\n" +
             "interrupt <thread id>     -- interrupt a thread\n" +
             "\n" +
             "print <expr>              -- print value of expression\n" +
             "dump <expr>               -- print all object information\n" +
             "eval <expr>               -- evaluate expression (same as print)\n" +
             "set <lvalue> = <expr>     -- assign new value to field/variable/array element\n" +
             "locals                    -- print all local variables in current stack frame\n" +
             "\n" +
             "classes                   -- list currently known classes\n" +
             "class <class id>          -- show details of named class\n" +
             "methods <class id>        -- list a class's methods\n" +
             "fields <class id>         -- list a class's fields\n" +
             "\n" +
             "threadgroups              -- list threadgroups\n" +
             "threadgroup <name>        -- set current threadgroup\n" +
             "\n" +
             "stop [go|thread] [<thread_id>] <at|in> <location>\n" +
             "                          -- set a breakpoint\n" +
             "                          -- if no options are given, the current list of breakpoints is printed\n" +
             "                          -- if \"go\" is specified, immediately resume after stopping\n" +
             "                          -- if \"thread\" is specified, only suspend the thread we stop in\n" +
             "                          -- if neither \"go\" nor \"thread\" are specified, suspend all threads\n" +
             "                          -- if an integer <thread_id> is specified, only stop in the specified thread\n" +
             "                          -- \"at\" and \"in\" have the same meaning\n" +
             "                          -- <location> can either be a line number or a method:\n" +
             "                          --   <class_id>:<line_number>\n" +
             "                          --   <class_id>.<method>[(argument_type,...)]\n" +
             "clear <class id>.<method>[(argument_type,...)]\n" +
             "                          -- clear a breakpoint in a method\n" +
             "clear <class id>:<line>   -- clear a breakpoint at a line\n" +
             "clear                     -- list breakpoints\n" +
             "catch [uncaught|caught|all] <class id>|<class pattern>\n" +
             "                          -- break when specified exception occurs\n" +
             "ignore [uncaught|caught|all] <class id>|<class pattern>\n" +
             "                          -- cancel 'catch' for the specified exception\n" +
             "watch [access|all] <class id>.<field name>\n" +
             "                          -- watch access/modifications to a field\n" +
             "unwatch [access|all] <class id>.<field name>\n" +
             "                          -- discontinue watching access/modifications to a field\n" +
             "trace [go] methods [thread]\n" +
             "                          -- trace method entries and exits.\n" +
             "                          -- All threads are suspended unless 'go' is specified\n" +
             "trace [go] method exit | exits [thread]\n" +
             "                          -- trace the current method's exit, or all methods' exits\n" +
             "                          -- All threads are suspended unless 'go' is specified\n" +
             "untrace [methods]         -- stop tracing method entrys and/or exits\n" +
             "step                      -- execute current line\n" +
             "step up                   -- execute until the current method returns to its caller\n" +
             "stepi                     -- execute current instruction\n" +
             "next                      -- step one line (step OVER calls)\n" +
             "cont                      -- continue execution from breakpoint\n" +
             "\n" +
             "list [line number|method] -- print source code\n" +
             "use (or sourcepath) [source file path]\n" +
             "                          -- display or change the source path\n" +
             "exclude [<class pattern>, ... | \"none\"]\n" +
             "                          -- do not report step or method events for specified classes\n" +
             "classpath                 -- print classpath info from target VM\n" +
             "\n" +
             "monitor <command>         -- execute command each time the program stops\n" +
             "monitor                   -- list monitors\n" +
             "unmonitor <monitor#>      -- delete a monitor\n" +
             "read <filename>           -- read and execute a command file\n" +
             "\n" +
             "lock <expr>               -- print lock info for an object\n" +
             "threadlocks [thread id]   -- print lock info for a thread\n" +
             "\n" +
             "pop                       -- pop the stack through and including the current frame\n" +
             "reenter                   -- same as pop, but current frame is reentered\n" +
             "redefine <class id> <class file name>\n" +
             "                          -- redefine the code for a class\n" +
             "\n" +
             "disablegc <expr>          -- prevent garbage collection of an object\n" +
             "enablegc <expr>           -- permit garbage collection of an object\n" +
             "\n" +
             "!!                        -- repeat last command\n" +
             "<n> <command>             -- repeat command n times\n" +
             "# <command>               -- discard (no-op)\n" +
             "help (or ?)               -- list commands\n" +
             "dbgtrace [flag]           -- same as dbgtrace command line option\n" +
             "version                   -- print version information\n" +
             "exit (or quit)            -- exit debugger\n" +
             "\n" +
             "<class id>: a full class name with package qualifiers\n" +
             "<class pattern>: a class name with a leading or trailing wildcard ('*')\n" +
             "<thread id>: thread number as reported in the 'threads' command\n" +
             "<expr>: a Java(TM) Programming Language expression.\n" +
             "Most common syntax is supported.\n" +
             "\n" +
             "Startup commands can be placed in either \"jdb.ini\" or \".jdbrc\"\n" +
             "in user.home or user.dir"},
        {"zz usage text",
             "Usage: {0} <options> <class> <arguments>\n" +
             "\n" +
             "where options include:\n" +
             "    -? -h --help -help print this help message and exit\n" +
             "    -sourcepath <directories separated by \"{1}\">\n" +
             "                      directories in which to look for source files\n" +
             "    -attach <address>\n" +
             "                      attach to a running VM at the specified address using standard connector\n" +
             "    -listen <address>\n" +
             "                      wait for a running VM to connect at the specified address using standard connector\n" +
             "    -listenany\n" +
             "                      wait for a running VM to connect at any available address using standard connector\n" +
             "    -launch\n" +
             "                      launch VM immediately instead of waiting for ''run'' command\n" +
             "    -listconnectors   list the connectors available in this VM\n" +
             "    -connect <connector-name>:<name1>=<value1>,...\n" +
             "                      connect to target VM using named connector with listed argument values\n" +
             "    -dbgtrace [flags] print info for debugging {0}\n" +
             "    -tclient          run the application in the HotSpot(TM) Client Compiler\n" +
             "    -tserver          run the application in the HotSpot(TM) Server Compiler\n" +
             "\n" +
             "options forwarded to debuggee process:\n" +
             "    -v -verbose[:class|gc|jni]\n" +
             "                      turn on verbose mode\n" +
             "    -D<name>=<value>  set a system property\n" +
             "    -classpath <directories separated by \"{1}\">\n" +
             "                      list directories in which to look for classes\n" +
             "    -X<option>        non-standard target VM option\n" +
             "\n" +
             "<class> is the name of the class to begin debugging\n" +
             "<arguments> are the arguments passed to the main() method of <class>\n" +
             "\n" +
             "For command help type ''help'' at {0} prompt"},
        // END OF MATERIAL TO LOCALIZE
        };

        return temp;
    }
}
