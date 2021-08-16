/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * build         @BUILD_TAG_PLACEHOLDER@
 *
 * @COPYRIGHT_MINI_LEGAL_NOTICE_PLACEHOLDER@
 */

import java.io.*;
import java.lang.reflect.*;

/*
  ClassLoader that knows how to fabricate exactly one class.  The name
  of the class is defined by the parameter singleClassName.  When
  asked to load a class, this loader first delegates to its parent as
  usual, then, if that doesn't find the class and if the name is the
  same as singleClassName, the class is fabricated.  It is a public
  class with no fields or methods and a single public no-arg
  constructor that simply calls its parent's no-arg constructor.  This
  means that the parent must have a public or protected no-arg
  constructor.
 */
public class SingleClassLoader extends ClassLoader {
    SingleClassLoader(String singleClassName, Class superclass,
                      ClassLoader parent) {
        super(parent);

        Constructor superConstr;
        try {
            superConstr = superclass.getDeclaredConstructor(new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Superclass must have no-arg " +
                                               "constructor");
        }
        int superConstrMods = superConstr.getModifiers();
        if ((superConstrMods & (Modifier.PUBLIC|Modifier.PROTECTED)) == 0) {
            final String msg =
                "Superclass no-arg constructor must be public or protected";
            throw new IllegalArgumentException(msg);
        }

        this.singleClassName = singleClassName;
        final Class c;
        try {
            c = makeClass(superclass);
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
        this.singleClass = c;
    }

    private Class makeClass(Class superclass) throws IOException {
        final String superName = superclass.getName();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        dout = new DataOutputStream(bout);

        String thisNameInternal = singleClassName.replace('.', '/');
        String superNameInternal = superName.replace('.', '/');

        dout.writeInt(0xcafebabe); // magic
        dout.writeInt(0x0003002d); // major 45 minor 3
        dout.writeShort(10);       // cpool count (incl virtual 0 element)

        cpoolIndex = 1;

        int thisNameConst = writeUTFConst(thisNameInternal);
        int thisClassConst = writeClassConst(thisNameConst);
        int superNameConst = writeUTFConst(superNameInternal);
        int superClassConst = writeClassConst(superNameConst);
        int initNameConst = writeUTFConst("<init>");
        int voidNoArgSigConst = writeUTFConst("()V");
        int codeNameConst = writeUTFConst("Code");
        int superConstructorNameAndTypeConst =
            writeNameAndTypeConst(initNameConst, voidNoArgSigConst);
        int superConstructorMethodRefConst =
            writeMethodRefConst(superClassConst,
                                superConstructorNameAndTypeConst);

        dout.writeShort(Modifier.PUBLIC | 0x20 /*SUPER*/);
        dout.writeShort(thisClassConst);
        dout.writeShort(superClassConst);
        dout.writeInt(0);   // n interfaces, n fields
        dout.writeShort(1); // n methods

        // <init> method
        dout.writeShort(Modifier.PUBLIC);
        dout.writeShort(initNameConst);
        dout.writeShort(voidNoArgSigConst);
        dout.writeShort(1); // attr count

        // Code attribute
        dout.writeShort(codeNameConst);
        dout.writeInt(17);   // len
        dout.writeShort(1);  // max stack
        dout.writeShort(1);  // max locals
        dout.writeInt(5);    // code len
        dout.writeByte(42);  // aload_0
        dout.writeByte(183); // invokespecial
        dout.writeShort(superConstructorMethodRefConst);
        dout.writeByte(177); // return

        dout.writeShort(0);  // 0 catches

        dout.writeShort(0);  // 0 method attrs

        dout.writeShort(0);  // 0 class attrs

        dout.close();
        byte[] classBytes = bout.toByteArray();

        dout = null;

        return
            defineClass(singleClassName, classBytes, 0, classBytes.length);
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        if (name.equals(singleClassName))
            return singleClass;
        else
            throw new ClassNotFoundException(name);
    }

    private int writeUTFConst(String s) throws IOException {
        dout.writeByte(1);
        dout.writeUTF(s);
        return cpoolIndex++;
    }

    private int writeClassConst(int nameIndex) throws IOException {
        dout.writeByte(7);
        dout.writeShort((short) nameIndex);
        return cpoolIndex++;
    }

    private int writeNameAndTypeConst(int nameIndex, int typeIndex)
            throws IOException {
        dout.writeByte(12);
        dout.writeShort((short) nameIndex);
        dout.writeShort((short) typeIndex);
        return cpoolIndex++;
    }

    private int writeMethodRefConst(int classIndex, int nameAndTypeIndex)
            throws IOException {
        dout.writeByte(10);
        dout.writeShort((short) classIndex);
        dout.writeShort((short) nameAndTypeIndex);
        return cpoolIndex++;
    }

    private final String singleClassName;
    private final Class singleClass;

    private DataOutputStream dout;
    private int cpoolIndex;
}
