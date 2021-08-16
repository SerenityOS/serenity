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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS302/hs302t006.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS302.hs302t006.hs302t006
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs302t006=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS302.hs302t006.hs302t006
 */

package nsk.jvmti.scenarios.hotswap.HS302.hs302t006;

import nsk.share.jvmti.RedefineAgent;
import java.lang.reflect.*;
import nsk.jvmti.scenarios.hotswap.HS302.hs302t006r.MyClass;
public class hs302t006  extends RedefineAgent {

    public hs302t006(String[] arg) {
        super(arg);
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs302t006 hsCase = new hs302t006(arg);
        System.exit(hsCase.runAgent());
    }

    public boolean agentMethod() {
        boolean  pass=false;
        MyClass cls = new MyClass();
        Class klass = MyClass.class;
        try {
            Method[] methods = klass.getDeclaredMethods();
            for(Method method : methods) {
                if (method.getName().equals("setName")) {
                    System.out.println(" Modified "+method.getModifiers());
                    if ( (Modifier.PUBLIC &  method.getModifiers())==Modifier.PUBLIC ) {
                        System.out.println("...Public ..");
                        pass = true;
                    }
                }
            }
        }catch(Exception exp) {
            if ( isRedefined() ) {
                pass =true;
                log.println(" Passed ..");
            }
        }
        // If the execption is failed to throw.
        if ( redefineAttempted() && !isRedefined() ) {
            pass = true;
        }
        return pass;
    }
}
