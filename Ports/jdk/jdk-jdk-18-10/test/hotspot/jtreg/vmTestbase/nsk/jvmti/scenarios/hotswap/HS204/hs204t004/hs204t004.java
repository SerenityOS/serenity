/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS204/hs204t004.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * 1. Enable event ClassLoad.
 * 2. Load a class by a not-yet-loaded user-defined class loader.
 * 3. Upon ClassLoad occurrence for the class loader, set a breakpoint
 * in its method loadClass().
 * 4. Upon reaching the breakpoint, redefine the class loader with
 * the changed loadClass(). The obsolete loadClass() should be continued
 * to execute.
 * 5. Pop currently executed frame of loadClass().
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS204.hs204t004.hs204t004
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs204t004=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS204.hs204t004.hs204t004
 */

package nsk.jvmti.scenarios.hotswap.HS204.hs204t004;
import java.util.concurrent.atomic.AtomicBoolean;
import nsk.share.jvmti.RedefineAgent;

public class hs204t004 extends RedefineAgent {
    public hs204t004(String[] arg) {
        super(arg);
    }

        public static void main(String[] arg) {
                arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

                hs204t004 hsCase = new hs204t004(arg);
        System.exit(hsCase.runAgent());
        }
    public boolean  agentMethod() {
                MyThread mt = new MyThread();
                String name = mt.name;
                try {
                        mt.start();
                        while(!MyThread.resume.get()) ;
                        Thread.sleep(10000);
                        popFrame(mt);
                        mt.join();
                } catch(Exception exp) {
                        exp.printStackTrace();
                }
        boolean passed = false;
                if (name.equals(mt.name)) {
                        System.out.println(" Same Class..Failed.."+mt.name+"   "+name);
                } else if ( redefineAttempted() && isRedefined()) {
                        System.out.println( " Different Class Loader . Passed."+mt.name+"   "+name);
            passed = true;
                }
        return passed;
        }
        public static native boolean popFrame(Thread thread);
}

class MyThread extends Thread {
        public static AtomicBoolean resume = new AtomicBoolean(false);
        public String name="MyThread";

        public MyThread() {
                System.out.println(" Java..::");
        }

        public void run() {
                doThis();
        }

        public void doThis() {
                MyClassLoader ld = new MyClassLoader(ClassLoader.getSystemClassLoader());
                name = ld.name;
                try {
            // This class nsk.jvmti.s*. class will be loaded with the default system loader.
            // Because it falls in same package.
            // So ClassLoader will try to load from the same.
                        String class_name = "nsk.jvmti.scenarios.hotswap.HS204.hs204t004.TempClass";
                        System.out.println("... "+ld.name);
                        // Do the resume.set(true) after the above println. See JDK-8175509
                        resume.set(true);
                        Class cls = ld.loadClass(class_name);
                        TempClass tc = new TempClass(10);
                        tc = tc.doThis();
                        cls = Class.forName(class_name,true,ld);
                        System.out.println(" ClassLoader"+tc.toString());

                } catch(ClassNotFoundException cnfe) {
                        cnfe.printStackTrace();
                }
        }
}
