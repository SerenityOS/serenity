/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.share.jpda;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.test.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
/*
 * Class can be used as base debuggee class in jdi and jdwp tests.
 * Class contains common method for initializing log, pipe, vm, and several common auxiliary methods.  Subclass should implement parseCommand() and, if needed, doInit(parse command line parameters)
 * !!! Edit carefully, value of 'DEFAULT_BREAKPOINT_LINE' is hardcoded !!!
 */
public class AbstractDebuggeeTest {
    protected DebugeeArgumentHandler argHandler;

    protected IOPipe pipe;

    protected Log log;

    protected boolean callExit = true;

    private boolean success = true;

    protected void setSuccess(boolean value) {
        success = value;
    }

    public boolean getSuccess() {
        return success;
    }

    public final static int DEFAULT_BREAKPOINT_LINE = 63;

    public final static String DEFAULT_BREAKPOINT_METHOD_NAME = "breakpointMethod";

    public void breakpointMethod() {
        log.display("In breakpoint method: 'AbstractDebuggeeTest.breakpointMethod()'"); // DEFAULT_BREAKPOINT_LINE
    }

    protected Map<String, ClassUnloader> loadedClasses = new TreeMap<String, ClassUnloader>();

    public final static String COMMAND_FORCE_BREAKPOINT = "forceBreakpoint";

    //load class with given name with possibility to unload it
    static public final String COMMAND_LOAD_CLASS = "loadClass";

    // unload class with given name(it is possible for classes loaded via loadTestClass method)
    // command:className[:<unloadResult>], <unloadResult> - one of UNLOAD_RESULT_TRUE or UNLOAD_RESULT_FALSE
    static public final String COMMAND_UNLOAD_CLASS = "unloadClass";

    // Optional arguments of COMMAND_UNLOAD_CLASS
    // (is after unloading class should be really unloaded, default value is UNLOAD_RESULT_TRUE)
    static public final String UNLOAD_RESULT_TRUE = "unloadResultTrue";

    static public final String UNLOAD_RESULT_FALSE = "unloadResultFalse";

    static public final String COMMAND_CREATE_STATETESTTHREAD = "createStateTestThread";

    static public final String COMMAND_NEXTSTATE_STATETESTTHREAD = "stateTestThreadNextState";

    //force GC using AbstractDebuggeeTest.eatMemory()
    static public final String COMMAND_FORCE_GC = "forceGC";
    // GCcount is used to get information about GC activity during test
    static public final String COMMAND_GC_COUNT = "GCcount";
    private int lastGCCount;


    static public final String stateTestThreadName = "stateTestThread";

    static public final String stateTestThreadClassName = StateTestThread.class.getName();

    // path to classes intended for loading/unloading
    protected String classpath;

    // classloader loads only test classes from nsk.*
    public static class TestClassLoader extends CustomClassLoader {
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("nsk."))
                return findClass(name);
            else
                return super.loadClass(name);
        }
    }

    protected StressOptions stressOptions;
    protected Stresser stresser;

    // initialize test and remove unsupported by nsk.share.jdi.ArgumentHandler arguments
    // (ArgumentHandler constructor throws BadOption exception if command line contains unrecognized by ArgumentHandler options)
    // support -testClassPath parameter: path to find classes for custom classloader
    protected String[] doInit(String[] args) {
        stressOptions = new StressOptions(args);
        stresser = new Stresser(stressOptions);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testClassPath") && (i < args.length - 1)) {
                classpath = args[i + 1];
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    public void loadTestClass(String className) {
        if (classpath == null) {
            throw new TestBug("Debuggee requires 'testClassPath' parameter");
        }

        try {
            ClassUnloader classUnloader = new ClassUnloader();

            classUnloader.setClassLoader(new TestClassLoader());
            classUnloader.loadClass(className, classpath);
            loadedClasses.put(className, classUnloader);
        } catch (ClassNotFoundException e) {
            log.complain("Unexpected 'ClassNotFoundException' on loading of the requested class(" + className + ")");
            e.printStackTrace(log.getOutStream());
            throw new TestBug("Unexpected 'ClassNotFoundException' on loading of the requested class(" + className + ")");
        }
    }

    public static final int MAX_UNLOAD_ATTEMPS = 5;

    public void unloadTestClass(String className, boolean expectedUnloadingResult) {
        ClassUnloader classUnloader = loadedClasses.get(className);

        int unloadAttemps = 0;

        if (classUnloader != null) {
            boolean wasUnloaded = false;

            while (!wasUnloaded && (unloadAttemps++ < MAX_UNLOAD_ATTEMPS)) {
                wasUnloaded = classUnloader.unloadClass();
            }

            if (wasUnloaded)
                loadedClasses.remove(className);
            else {
                log.display("Class " + className + " was not unloaded");
            }

            if (wasUnloaded != expectedUnloadingResult) {
                setSuccess(false);

                if (wasUnloaded)
                    log.complain("Class " + className + " was unloaded!");
                else
                    log.complain("Class " + className + " wasn't unloaded!");
            }
        } else {
            log.complain("Invalid command 'unloadClass' is requested: class " + className + " was not loaded via ClassUnloader");
            throw new TestBug("Invalid command 'unloadClass' is requested: class " + className + " was not loaded via ClassUnloader");
        }
    }

    static public void sleep1sec() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private StateTestThread stateTestThread;

    public static final String COMMAND_QUIT = "quit";

    public static final String COMMAND_READY = "ready";

    private void createStateTestThread() {
        if (stateTestThread != null)
            throw new TestBug("StateTestThread already created");

        stateTestThread = new StateTestThread(stateTestThreadName);
    }

    private void stateTestThreadNextState() {
        if (stateTestThread == null)
            throw new TestBug("StateTestThread not created");

        stateTestThread.nextState();
    }

    public boolean parseCommand(String command) {
        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
            tokenizer.whitespaceChars(':', ':');
            tokenizer.wordChars('_', '_');
            tokenizer.wordChars('$', '$');
            tokenizer.wordChars('[', ']');

            if (command.equals(COMMAND_FORCE_GC)) {
                forceGC();
                lastGCCount = getCurrentGCCount();
                return true;
            } else if (command.equals(COMMAND_GC_COUNT)) {
                pipe.println(COMMAND_GC_COUNT + ":" + (getCurrentGCCount() - lastGCCount));
                return true;
            }   else if (command.equals(COMMAND_FORCE_BREAKPOINT)) {
                breakpointMethod();
                return true;
            } else if (command.equals(COMMAND_CREATE_STATETESTTHREAD)) {
                createStateTestThread();

                return true;
            } else if (command.equals(COMMAND_NEXTSTATE_STATETESTTHREAD)) {
                stateTestThreadNextState();

                return true;
            } else if (command.startsWith(COMMAND_LOAD_CLASS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                loadTestClass(className);

                return true;
            } else if (command.startsWith(COMMAND_UNLOAD_CLASS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                boolean expectedUnloadingResult = true;

                if (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
                    if (tokenizer.sval.equals(UNLOAD_RESULT_TRUE))
                        expectedUnloadingResult = true;
                    else if (tokenizer.sval.equals(UNLOAD_RESULT_FALSE))
                        expectedUnloadingResult = false;
                    else
                        throw new TestBug("Invalid command format: " + command);
                }

                unloadTestClass(className, expectedUnloadingResult);

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    protected DebugeeArgumentHandler createArgumentHandler(String args[]) {
        return new DebugeeArgumentHandler(args);
    }

    protected void init(String args[]) {
        argHandler = createArgumentHandler(doInit(args));
        pipe = argHandler.createDebugeeIOPipe();
        log = argHandler.createDebugeeLog();
        lastGCCount = getCurrentGCCount();
    }

    public void initDebuggee(DebugeeArgumentHandler argHandler, Log log, IOPipe pipe, String[] args, boolean callExit) {
        this.argHandler = argHandler;
        this.log = log;
        this.pipe = pipe;
        this.callExit = callExit;
        doInit(args);
    }

    public void doTest(String args[]) {
        init(args);
        doTest();
    }

    public void doTest() {
        do {
            log.display("Debuggee " + getClass().getName() + " : sending the command: " + AbstractDebuggeeTest.COMMAND_READY);
            pipe.println(AbstractDebuggeeTest.COMMAND_READY);

            String command = pipe.readln();
            log.display("Debuggee: received the command: " + command);

            if (command.equals(AbstractDebuggeeTest.COMMAND_QUIT)) {
                break;
            } else {
                try {
                    if (!parseCommand(command)) {
                        log.complain("TEST BUG: unknown debugger command: " + command);
                        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                    }
                } catch (Throwable t) {
                    log.complain("Unexpected exception in debuggee: " + t);
                    t.printStackTrace(log.getOutStream());
                    System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                }
            }
        } while (true);

        log.display("Debuggee: exiting");

        if (callExit) {
            if (success)
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
            else
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    public static void eatMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        int memoryChunk = (int) (maxMemory / 50);
        try {
            List<Object> list = new ArrayList<Object>();
            while (true) {
                list.add(new byte[memoryChunk]);
            }
        } catch (OutOfMemoryError e) {
            // expected exception
        }
    }

    public static int getCurrentGCCount() {
        int result = 0;
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : gcBeans) {
            result += bean.getCollectionCount();
        }
        return result;
    }

    public void forceGC() {
        eatMemory();
    }

    public void voidValueMethod() {
    }

    public void unexpectedException(Throwable t) {
        setSuccess(false);
        t.printStackTrace(log.getOutStream());
        log.complain("Unexpected exception: " + t);
    }

}
