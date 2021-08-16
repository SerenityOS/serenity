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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS104/hs104t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * T001: Hotswap class(es) in synchronous manner within an JVMTI event
 * when all fields, methods, attributes and the constant pool are changed
 * in new class(es). The VM works in default mode.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS104.hs104t001.hs104t001
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs104t001=pathToNewByteCode=./bin,-waittime=5,package=nsk,samples=100,mode=compiled
 *      nsk.jvmti.scenarios.hotswap.HS104.hs104t001.hs104t001
 */

package nsk.jvmti.scenarios.hotswap.HS104.hs104t001;

import nsk.share.jvmti.RedefineAgent;

public class hs104t001 extends RedefineAgent {
    public hs104t001(String[] arg) {
        super(arg);
    }

        public static void main(String[] arg) {
                arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

                hs104t001 hsCase = new hs104t001(arg);
        System.exit(hsCase.runAgent());
        }

        public boolean  agentMethod() {
                boolean state=false;
                int size=100;
                MyClass cla = new MyClass("In Side..", size);
                cla.doThis();
                System.out.println("..."+cla.getState());
                if (size != cla.getState()) {
                        System.out.println(" State.. Passed "+state);
                        state = true;
                } else {
                        System.out.println(" State.. Failed.."+state);
                }
                return state;
        }
}
