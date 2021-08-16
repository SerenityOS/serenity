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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS301/hs301t004.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *    jmvti->RedefineClases(... ) can make changes to class object during runtime, subject to few constraints and capabilities.
 *    This testcase will try to convert the immutable java object (class) to mutable.
 *    This test will create immutable object (./MyClass), which would be redefined
 *    with mutable class(./newclass00/MyClass.java). The test case is said to pass when
 *    the object becomes mutable.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS301.hs301t004.hs301t004
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs301t004=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS301.hs301t004.hs301t004
 */

package nsk.jvmti.scenarios.hotswap.HS301.hs301t004;
import nsk.share.jvmti.RedefineAgent;

public class hs301t004 extends RedefineAgent {

    public hs301t004(String[] arg) {
        super(arg);
    }


    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs301t004 hsCase = new hs301t004(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {
        boolean pass =true;
        final int init_state=100;
        MyClass immutableObject = new MyClass(init_state);
        invokeMethodsOnImmutable(immutableObject);
        if ( immutableObject.getCount() == init_state )  {
            pass = false;
        } else {
            pass = true;
        }
        System.out.println("getCount "+immutableObject.getCount()+
                " && initila state = "+init_state);
        if ( !pass ) {
            System.out.println(" Error occured, error in redefineing.");
        } else {
            System.out.println(" Successfully redefined.");
        }
        return pass;
    }

    public void invokeMethodsOnImmutable(MyClass immutableObject) {
        System.out.println(" Info : Entered invokeMethodsOnImmutable(... ) ");
        System.out.println("        state before call = "+immutableObject.getCount());
        immutableObject = immutableObject.DummyMethod();
        // trying to change the referece.
        immutableObject.DummyMethod();
        // trying to change the contents method call.
        System.out.println("        state After call = "+immutableObject.getCount());
    }
}
