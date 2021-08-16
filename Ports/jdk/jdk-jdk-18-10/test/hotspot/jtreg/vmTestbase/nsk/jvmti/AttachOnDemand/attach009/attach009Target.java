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
package nsk.jvmti.AttachOnDemand.attach009;

import nsk.share.aod.TargetApplicationWaitingAgents;

class FirstLoadedClass {

}

class ClassToLoad0 {

}

class ClassToLoad1 {

}

class ClassToLoad2 {

}

class LastLoadedClass {

}

public class attach009Target extends TargetApplicationWaitingAgents {

    private void loadClass(String className) {
        try {
            Class.forName(className, true, this.getClass().getClassLoader());
        } catch (Throwable t ){
            setStatusFailed("Unexpected exception during class loading: " + t);
            t.printStackTrace(log.getOutStream());
        }
    }

    class ClassLoadingThread extends Thread {

        String className;

        ClassLoadingThread(String className) {
            this.className = className;
        }

        public void run() {
            log.display(Thread.currentThread() + " is loading class '" + className + "'");
            loadClass(className);
        }
    }

    protected void targetApplicationActions() throws Throwable {
        /*
         * Test checks that ClassLoad event can be enabled only for one thread
         * (for target application thread executing method targetApplicationActions()).
         *
         * Method targetApplicationActions() does following:
         *      - loads class 'FirstLoadedClass', after this ClassLoad evens enabled only for the current thread
         *      - starts several threads loading classes (for these threads ClassLoad events shoudn't be generated)
         *      - loads class 'LastLoadedClass', after this test agent should finish work
         */

        ClassLoadingThread classLoadingThreads[] = new ClassLoadingThread[3];
        for (int i = 0; i < classLoadingThreads.length; i++) {
            classLoadingThreads[i] = new ClassLoadingThread("nsk.jvmti.AttachOnDemand.attach009.ClassToLoad" + i);
            classLoadingThreads[i].setName("ClassLoadingThread-" + i);
        }

        loadClass("nsk.jvmti.AttachOnDemand.attach009.FirstLoadedClass");

        for (ClassLoadingThread classLoadingThread : classLoadingThreads)
            classLoadingThread.start();

        for (ClassLoadingThread classLoadingThread : classLoadingThreads)
            classLoadingThread.join();

        loadClass("nsk.jvmti.AttachOnDemand.attach009.LastLoadedClass");
    }

    public static void main(String[] args) {
        new attach009Target().runTargetApplication(args);
    }
}
