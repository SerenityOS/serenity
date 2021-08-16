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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS301/hs301t002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *     This testcase uses jvmti->RedefineClasses(... ), RedefineClasses(... ) should fail
 *     to change method modifiers.
 *     The Test will creates an instance of a class ./MyClass.java. After few steps, JVMTIEnv will attempt
 *     to redefine it to ./newclass00/MyClass.java. One of the MyClass's method in ./newclass00/MyClass.java
 *     has a static modifier.  Because Redefine doesn't accept method's modifiers changes, the test is
 *     supposed to fail to redefine the class.
 *     The testcase is said to pass, if jvmti->RedefineClasses(... ) call fails.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS301.hs301t002.hs301t002
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs301t002=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS301.hs301t002.hs301t002
 */

package nsk.jvmti.scenarios.hotswap.HS301.hs301t002;
import nsk.share.jvmti.RedefineAgent;

public class hs301t002 extends RedefineAgent {
    public hs301t002(String[] arg) {
        super(arg);
    }


    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs301t002 hsCase = new hs301t002(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {

        MyClass cls = new MyClass();
        boolean  pass=true;
        if ( redefine() ) {
            System.out.println(" #Error hs301t002.java Line=34 : Is MyClass is redefined  ??? "
                    +" ( not expected to get redefined ).");
            pass=false;
            return pass;
            // we can move off from here.
        }
        // Could be useful for debugging.
        if ( !redefineAttempted())  {
            // it failed to redefine and it have n't
            // even attempted to do, so its here. so one
            // simple error could be jni->FindClass(... ).
            // if so check, classpath, classloader context, in which this method was called.
            System.out.println(" #Error : Please refer (t)log files "+
                    "error occured before attempting redefine, could be in jni->FindClass.");
            pass = false;
            return pass;
        }
        // lets see if the method is redefined, to static method count will be different.
        cls.doThis();
        // Doing one more check with counts,
        // counts do change for if it was actually redefined or picked a wrong class.
        if ( cls.size  != MyClass.count ) {
            System.out.println(" #Error hs301t002.java Line=56 : Is MyClass is redefined  ??? "
                                +" ( not expected to get redefined ).");
            System.out.println(" #Info  : nsk.share.jvmti.RedefineAgent.isRedefined="
                                +isRedefined());
            System.out.println(" #Info  : nsk.share.jvmti.RedefineAgent.redefineAttempted="
                                +redefineAttempted());
            pass=false;
            // interesting case, its not redefined and attempted to do.
            // this info shows it as redefined, Some one have to look in details.
            return pass;
        }
        // Here redefineAttempted, failed to redefine
        // (programaticaly cls.size  == cls.count ).
        if ( isRedefined() ) {
            System.out.println(" #Error hs301t002.java Line=71 : Is MyClass is redefined  ??? "
                    +" ( not expected to get redefined ).");
            System.out.println(" #Info  : nsk.share.jvmti.RedefineAgent.isRedefined=true.");
            System.out.println(" #Info  : nsk.share.jvmti.RedefineAgent.redefineAttempted=true.");
            pass = false;
            return pass;
        }
        // pass == true.
        // we have left this method on all false setting cases.
        System.out.println(" #Info : redefineAttempted() = true && "+
                "isRedefined()=false && cls.size  == cls.count.");
        System.out.println(" #Info : hs301t002  passed.");
        System.out.println(" #TEST PASSED.");
        return pass;
    }
    // The parameter was not used in code, so its not useful even.
    public native boolean redefine();
}
