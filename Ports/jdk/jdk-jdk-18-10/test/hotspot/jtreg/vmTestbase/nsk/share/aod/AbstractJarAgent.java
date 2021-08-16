/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import java.lang.instrument.*;
import java.io.*;
import nsk.share.*;

/*

AbstractJarAgent is base class for java agents used in AttachOnDemand tests
(tests against Attach API, API from package com.sun.tools.attach).

In all AttachOnDemand tests the same algorithm is used:
    - java application where agent is loaded to (target application) based on
     class nsk.share.aod.TargetApplicationWaitingAgents starts and waits when
     test agents will be loaded

    - special application (nsk.share.jvmti.aod.AgentsAttacher) loads test agents
    in the target application using Attach API

    - when agent is loaded it notifies target application about that and executes
    test-specific actions. When agent execution is completed it also notifies
    target application about that

    - when all test agents finish execution target application checks its status
    (passed or failed) and also finishes

Each java agent should have method 'agentmain' where only agent initialization should be done,
main agent's actions should be executed in special thread started from 'agentmain'.
Class AbstractJarAgent incapsulates actions common for all java agents: agent initialization,
starting thread executing agent's actions and communication with target application.

In most cases test agents should override only method 'agentActions' and its 'agentmain'
should contain only call of the method 'AbstractJarAgent.runJarAgent'.

Typical agent class looks like this:

public class agentExample extends AbstractJarAgent {

    protected void init(String[] args) {
        // parse agent's options and do test-specific initialization
    }

    protected void agentActions() {
        // do test specific actions
    }

    public static void agentmain(final String options, Instrumentation inst) {
        new agentExample().runJarAgent(options, inst);
    }
}
 */
abstract public class AbstractJarAgent {

    private boolean finishedSuccessfully = true;

    private Log log;

    protected void display(String message) {
        log.display(outputPrefix + message);
    }

    protected void complain(String message) {
        log.complain(outputPrefix + message);
    }

    protected void logThrowable(Throwable t) {
        t.printStackTrace(log.getOutStream());
    }

    /*
     * Instrumentation object passed to the 'agentmain' method
     */
    protected Instrumentation inst;

    private String name;

    private String outputPrefix;

    private String pathToNewByteCode;

    protected String pathToNewByteCode() {
        return pathToNewByteCode;
    }

    /*
     * Subclasses should report about test failures using this method
     */
    protected void setStatusFailed(String errorMessage) {
        finishedSuccessfully = false;
        complain("ERROR: " + errorMessage);
    }

    /*
     * Initialization method, called from agentmain before method agentActions is called
     * (it introduced for overriding in subclasses)
     */
    protected void init(String[] args) {
    }

    protected static class AgentOption {
        public String name;
        public String value;
        public AgentOption(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    protected AgentOption parseAgentArgument(String arg) {
        int index = arg.indexOf('=');
        if (index <= 0) {
            throw new TestBug("Invalid agent parameters format");
        }
        return new AgentOption(arg.substring(0, index), arg.substring(index + 1));
    }

    static protected final String agentNameOption = "-agentName";

    static protected final String pathToNewByteCodeOption = "-pathToNewByteCode";

    /*
     * Parse agent's options, initialize common parameters
     */
    private void defaultInit(String[] args) {
        for (int i = 0; i < args.length; i++) {
            AgentOption option = parseAgentArgument(args[i]);
            if (option.name.equals(agentNameOption)) {
                name = option.value;
                outputPrefix = name + ": ";
            } else if (option.name.equals(pathToNewByteCodeOption)) {
                pathToNewByteCode = option.value;
            }
        }

        if (name == null)
            throw new TestBug("Agent name wasn't specified");

        log = new Log(System.out, true);
    }

    /*
     * Special thread which is started from agentmain method and executing main
     * agent's actions. When agent completes execution AgentThread notifies
     * target application about that.
     */
    class AgentThread extends Thread {

        AgentThread() {
            super("Jar agent thread (agent: " + name + ")");
        }

        public void run() {
            try {
                agentActions();
            } catch (Throwable t) {
                setStatusFailed("Unexpected exception in the JarAgent: " + t);
                logThrowable(t);
            } finally {
                TargetApplicationWaitingAgents.agentFinished(name, finishedSuccessfully);
            }
        }
    }

    /*
     * This methods parses agent's options, initializes agent, notifies target application
     * that agent is started and starts thread executing main agent's actions.
     * Agents used in AttachOnDemand tests should call this method from its agentmain methods.
     */
    public final void runJarAgent(String options, Instrumentation inst) {
        if (options == null)
            throw new TestBug("Agent options weren't specified");

        this.inst = inst;

        String[] args = options.split(" ");

        // initialize common parameters
        defaultInit(args);

        // test-specific initialization
        init(args);

        // notify target application that agent was loaded and initialized
        TargetApplicationWaitingAgents.agentLoaded(name);

        // start special thread executing test-specific actions
        new AgentThread().start();
    }

    /*
     * Actions specific for test should be realized in this method.
     * This method is called from special thread started from agentmain method
     * after agent initialization
     */
    abstract protected void agentActions() throws Throwable;


    /*
     * Create ClassDefinition object for given class, path to the new class file
     * is specified in the parameter 'pathToByteCode'
     */
    protected static ClassDefinition createClassDefinition(Class<?> klass,
            String pathToByteCode) throws IOException {
        File classFile = new File(pathToByteCode + File.separator +
                klass.getName().replace(".", File.separator) +
                ".class");

        if (classFile.length() > Integer.MAX_VALUE)
            throw new Failure("Class file '" + classFile.getName() + " 'too large");

        byte data[] = new byte[(int)classFile.length()];

        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(classFile));
            in.readFully(data);
        } finally {
            if (in != null)
                in.close();
        }

        return new ClassDefinition(klass, data);
    }

    /*
     * Redefine given class using path to byte code specified with options -pathToNewByteCode
     */
    protected void redefineClass(Class<?> klass) throws Throwable {
        if (pathToNewByteCode() == null)
            throw new TestBug("Path to new class files wasn't specified");

        ClassDefinition newClassDef = createClassDefinition(klass, pathToNewByteCode());
        inst.redefineClasses(newClassDef);
    }
}
