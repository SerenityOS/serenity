/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4136620 4144590
   @summary Make sure that Attribute & subclasses are serialized and deserialized correctly
   @modules java.desktop
 */

import java.text.AttributedCharacterIterator.Attribute;
import java.awt.font.TextAttribute;
import java.io.*;

public class ReadResolve {

    public static void main(String[] args) throws Exception {
        testSerializationCycle(Attribute.LANGUAGE);
        testSerializationCycle(TextAttribute.INPUT_METHOD_HIGHLIGHT);

        boolean gotException = false;
        Attribute result = null;
        try {
            result = doSerializationCycle(FakeAttribute.LANGUAGE);
        } catch (Throwable e) {
            gotException = true;
        }
        if (!gotException) {
            throw new RuntimeException("Attribute should throw an exception when given a fake \"language\" attribute. Deserialized object: " + result);
        }
    }

    static Attribute doSerializationCycle(Attribute attribute) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream  oos  =  new  ObjectOutputStream(baos);
        oos.writeObject(attribute);
        oos.flush();

        byte[] data = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Attribute result = (Attribute) ois.readObject();

        return result;
    }

    static void testSerializationCycle(Attribute attribute) throws Exception {
        Attribute result = doSerializationCycle(attribute);
        if (result != attribute) {
            throw new RuntimeException("attribute changed identity during serialization/deserialization");
        }
    }

    private static class FakeAttribute extends Attribute {

        // This LANGUAGE attribute should never be confused with the
        // Attribute.LANGUAGE attribute. However, we don't override
        // readResolve here, so that deserialization goes
        // to Attribute. Attribute has to catch this problem and reject
        // the fake attribute.
        static final FakeAttribute LANGUAGE = new FakeAttribute("language");

        FakeAttribute(String name) {
            super(name);
        }
    }
}
