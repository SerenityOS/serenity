/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.AttachOnDemand.attach045;

import nsk.share.CustomClassLoader;
import nsk.share.TestBug;
import nsk.share.aod.*;
import java.util.*;

/*
 * This target application provokes following events:
 * - ClassLoad
 * - ClassPrepare
 * - ThreadStart
 * - ThreadEnd
 * - VMObjectAlloc
 *
 * Note: number of generated events should be up-to-date with numbers expected by test agents
 */
public class attach045Target extends TargetApplicationWaitingAgents {

    static final String classPathOption = "classPath";

    List<Thread> threadsGeneratingEvents = new ArrayList<Thread>();

    static class ArgParser extends AODTargetArgParser {
        ArgParser(String[] args) {
            super(args);
        }

        protected boolean checkOption(String option, String value) {
            if (super.checkOption(option, value))
                return true;

            if (option.equals(classPathOption))
                return true;

            return false;
        }

        protected void checkOptions() {
            super.checkOptions();

            if (!options.containsKey(classPathOption))
                throw new TestBug("Classpath wasn't specified");
        }

        String getClasspath() {
            return options.getProperty(classPathOption);
        }
    }

    protected AODTargetArgParser createArgParser(String[] args) {
        return new ArgParser(args);
    }

    protected void init(String[] args) {
        for (int i = 0; i < 10; i++) {
            ClassEventsThread thread = new ClassEventsThread(50, ((ArgParser)argParser).getClasspath());
            thread.setName("attach045-ClassEventsThread-" + i);
            threadsGeneratingEvents.add(thread);
        }
    }

    static final String CLASS_TO_LOAD_NAME = "nsk.jvmti.AttachOnDemand.attach045.ClassToLoad";

    static class TestClassLoader extends CustomClassLoader {
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(CLASS_TO_LOAD_NAME))
                return findClass(name);
            else
                return super.loadClass(name);
        }
    }

    class ClassEventsThread extends Thread {
        int eventsNumber;
        String classPath;

        ClassEventsThread(int eventsNumber, String classPath) {
            this.eventsNumber = eventsNumber;
            this.classPath = classPath;
        }

        public void run() {
            try {
                for (int i = 0; i < eventsNumber; i++) {
                    /*
                     * Provoke ClassLoad/ClassPrepare events
                     */
                    TestClassLoader classLoader = new TestClassLoader();
                    classLoader.setClassPath(classPath);
                    Class<?> klass = Class.forName(CLASS_TO_LOAD_NAME, true, classLoader);

                    /*
                     * Provoke VMObjectAlloc event
                     */
                    klass.newInstance();
                }
            } catch (Throwable t) {
                setStatusFailed(t);
            }
        }
    }

    protected void targetApplicationActions() throws Throwable {
        for (Thread thread : threadsGeneratingEvents) {
            thread.start();
        }

        log.display("Starting to generate events");

        /*
         * Provoke ThreadStart/ThreadEnd events
         */
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    // do nothing
                }
            });
            thread.setName("attach045-ThreadStartEndEventsThread-" + i);
            thread.start();
            thread.join();
        }

        for (Thread thread : threadsGeneratingEvents) {
            thread.join();
        }

        log.display("All events were generated");
    }

    public static void main(String[] args) {
        new attach045Target().runTargetApplication(args);
    }
}
