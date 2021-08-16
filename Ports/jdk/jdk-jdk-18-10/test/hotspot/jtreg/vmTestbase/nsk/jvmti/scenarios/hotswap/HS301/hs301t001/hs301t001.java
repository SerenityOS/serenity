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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS301/hs301t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *     This testcase uses jvmti->RedefineClasses(... ), to change a static methods
 *     modifier to non-static (changes from a class method to instance method).
 *     Redefine should not allow to do that.
 *     The Test will creates an instance of a class, ./MyClass.java. After few steps,
 *     JvmtiEnv will try to redefine it to ./newclass00/MyClass.java.
 *     Method (DummyMethod) in newclass00/MyClass.java does not have static modifier
 *     where as loaded class (./MyClass.java) has.
 *     Because Redefine, will not accept method's modifiers changes, the test is supposed to fail
 *     to redefine the class.
 *     The testcase is said to pass, if jvmti->RedefineClasses(... ) fails.
 *
 *     The Test will creates an instance of a class (MyClass ,
 *     that makes the class as loaded). After few steps later, JvmtiEnv will
 *     redefine it to ./newclass00/MyClass.java .
 *
 *     One of the MyClass's method's static modifier will be droped.
 *     Because Redefine, will not accept method's modifiers changes,
 *     It will fail to redefine the class.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS301.hs301t001.hs301t001
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs301t001=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS301.hs301t001.hs301t001
 */

package nsk.jvmti.scenarios.hotswap.HS301.hs301t001;
import nsk.share.jvmti.RedefineAgent;

public class hs301t001 extends RedefineAgent {

    public hs301t001(String[] arg) {
        super(arg);
    }


    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs301t001 hsCase = new hs301t001(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {
        MyClass cls = new MyClass();
        boolean pass=false;
        if ( !redefine() ) {
            pass=true;
        }
        cls.doThis();
        if (!pass)  {
            log.println(" Error occured, error in redefineing (as expected).");
            log.println(" Case passed.");
        } else {
            log.println(" Successfully redefined, (which is not execpeted).");
            log.println(" Case failed.");
        }
        return pass;
    }

    public native boolean redefine();
}
