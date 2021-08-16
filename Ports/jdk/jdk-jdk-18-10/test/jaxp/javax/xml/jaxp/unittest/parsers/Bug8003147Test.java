/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003147
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @compile -g Bug8003147TestClass.java
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug8003147Test
 * @run testng/othervm parsers.Bug8003147Test
 * @summary Test port fix for BCEL bug 39695.
 */

package parsers;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.FileOutputStream;
import java.io.FilePermission;

import jaxp.library.JAXPTestUtilities;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.ConstantClass;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
import com.sun.org.apache.bcel.internal.generic.InstructionList;

@Listeners({ jaxp.library.FilePolicy.class, jaxp.library.InternalAPIPolicy.class })
public class Bug8003147Test {

    @Test
    public void test() throws Exception {
        // Note: Because BCEL library is always behind java version, to make sure
        // JavaClass can parse the class file, create a separate
        // Bug8003147TestClass.java, which only uses basic features.
        JAXPTestUtilities.tryRunWithTmpPermission(() -> {
            String classfile = getSystemProperty("test.classes") + "/parsers/Bug8003147TestClass.class";
            JavaClass jc = new ClassParser(classfile).parse();

            // rename class
            ConstantPool cp = jc.getConstantPool();
            int cpIndex = ((ConstantClass) cp.getConstant(jc.getClassNameIndex())).getNameIndex();
            cp.setConstant(cpIndex, new ConstantUtf8("parsers/Bug8003147TestClassPrime"));
            ClassGen gen = new ClassGen(jc);
            Method[] methods = jc.getMethods();
            int index;
            for (index = 0; index < methods.length; index++) {
                if (methods[index].getName().equals("doSomething")) {
                    break;
                }
            }
            Method m = methods[index];
            MethodGen mg = new MethodGen(m, gen.getClassName(), gen.getConstantPool());

            // @bug 8064516, not currently used directly by JAXP, but we may need
            // to modify preexisting methods in the future.
            InstructionFactory f = new InstructionFactory(gen);
            InstructionList il = mg.getInstructionList();
            InstructionList newInst = new InstructionList();
            newInst.append(f.createPrintln("Hello Sekai!"));
            il.insert(newInst);
            mg.setMaxStack();

            gen.replaceMethod(m, mg.getMethod());
            String path = classfile.replace("Bug8003147TestClass", "Bug8003147TestClassPrime");
            gen.getJavaClass().dump(new FileOutputStream(path));

            try {
                Class.forName("parsers.Bug8003147TestClassPrime");
            } catch (ClassFormatError cfe) {
                cfe.printStackTrace();
                Assert.fail("modified version of class does not pass verification");
            }
        }, new FilePermission(getSystemProperty("test.classes") + "/-", "read,write"));
    }
}
