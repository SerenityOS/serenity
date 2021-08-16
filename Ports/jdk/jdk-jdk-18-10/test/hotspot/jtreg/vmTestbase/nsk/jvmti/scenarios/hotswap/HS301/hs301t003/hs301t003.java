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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS301/hs301t003.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description ::
 *    jvmti->RedefineClasses(... ) enables to change class file on runtime subject few constraints.
 *    This change inside the method is a simple and it causes behavioral changes.
 *    The test verifies, if the change is properly propagated or not. Method calls stacks would change for
 *    mutable to immutable objects. Technically, passing by value and reference will change.
 *    Here, method (DummyMethod())  defined in ./MyClass.java, causes change in state of MyClass's instance.
 *    This class would be redefined to ./newclass00/MyClass.java which doesn't change object state.
 *    The object instance passed to method should become 'call by value', because redefined class is immutable.
 *    The test is expected to pass, if the conversion is effective.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS301.hs301t003.hs301t003
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs301t003=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS301.hs301t003.hs301t003
 */

package nsk.jvmti.scenarios.hotswap.HS301.hs301t003;
import nsk.share.jvmti.RedefineAgent;

public class hs301t003 extends RedefineAgent {

    public hs301t003(String[] arg) {
        super(arg);
    }


    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs301t003 hsCase = new hs301t003(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {
        boolean  pass =true;
        try {
            int init_state=1000;
            // create mutableObjectInstnce.
            MyClass mutableObj = new MyClass(init_state);
            // Here is its  mutable object.
            invokeMethodsOnMutable(mutableObj);
            if (mutableObj.getCount() == init_state ) {
                pass = true ;
            } else {
                // no change in method state and
                pass = false;
            }
            // see the state of the object passed to method.
            // omgc
            System.out.println(" clas.getCount After redefine"+mutableObj.getCount());
            if (!pass ) {
                System.out.println(" Error occured, error in redefineing.");
            } else {
                System.out.println(" Successfully redefined.");
            }
        }
        catch(java.lang.VerifyError ve) {
            pass = false;
        }
        System.out.println(" PASS "+pass);
        return pass;
    }

    public void invokeMethodsOnMutable(MyClass mutableObj) {
        // Method invoked once.
        System.out.println("  invokeMethodsOnMutable ::   "+mutableObj.getCount());
        mutableObj = mutableObj.DummyMethod();
        // both the refereence and the state got changed.
        System.out.println("  invokeMethodsOnMutable ::  method invocation  "+mutableObj.getCount());
    }
}
