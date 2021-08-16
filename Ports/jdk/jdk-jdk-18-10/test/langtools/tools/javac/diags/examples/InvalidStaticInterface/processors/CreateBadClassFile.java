/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;

/* Create an invalid classfile with version 51.0 and a static method in an interface.*/
@SupportedAnnotationTypes("*")
public class CreateBadClassFile extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> elems, RoundEnvironment renv) {
        if (++round == 1) {
            ConstantPool cp = new ConstantPool(new CPInfo[] {
                new CONSTANT_Utf8_info(""),                     //0
                new CONSTANT_Utf8_info("Test"),                 //1
                new CONSTANT_Class_info(null, 1),               //2
                new CONSTANT_Utf8_info("java/lang/Object"),     //3
                new CONSTANT_Class_info(null, 3),               //4
                new CONSTANT_Utf8_info("test"),                 //5
                new CONSTANT_Utf8_info("()V"),                  //6
            });
            ClassFile cf = new ClassFile(0xCAFEBABE,
                          0,
                          51,
                          cp,
                          new AccessFlags(AccessFlags.ACC_ABSTRACT |
                                          AccessFlags.ACC_INTERFACE |
                                          AccessFlags.ACC_PUBLIC),
                          2,
                          4,
                          new int[0],
                          new Field[0],
                          new Method[] {
                              //creating static method in 51.0 classfile:
                              new Method(new AccessFlags(AccessFlags.ACC_PUBLIC |
                                                         AccessFlags.ACC_STATIC),
                                         5,
                                         new Descriptor(6),
                                         new Attributes(cp, new Attribute[0]))
                          },
                          new Attributes(cp, new Attribute[0]));
            try {
                JavaFileObject clazz = processingEnv.getFiler().createClassFile("Test");
                try (OutputStream out = clazz.openOutputStream()) {
                    new ClassWriter().write(cf, out);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    int round = 0;
}
