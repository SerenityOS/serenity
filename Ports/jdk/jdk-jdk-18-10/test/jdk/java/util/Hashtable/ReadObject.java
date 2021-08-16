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
 * @test
 * @bug 4652911
 * @summary test Hashtable readObject for invocation of overridable put method
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;

/**
 * Class that extends Hashtable to demonstrate bug when
 * subclass wraps the values put into the Hashtable.
 */
public class ReadObject extends Hashtable {
    /**
     * Wraps the values put into MyHashtable objects
     */
    class ValueWrapper implements Serializable {
        private Object mValue;

        ValueWrapper(Object value) {
            mValue = value;
        }

        Object getValue() {
            return mValue;
        }
    }

    public Object get(Object key) {
        ValueWrapper valueWrapper = (ValueWrapper)super.get(key);
        Object value = valueWrapper.getValue();
        if (value instanceof ValueWrapper)
            throw new RuntimeException("Hashtable.get bug");
        return value;
    }

    public Object put(Object key, Object value) {
        if (value instanceof ValueWrapper)
            throw new RuntimeException(
                "Hashtable.put bug: value is already wrapped");
        ValueWrapper valueWrapper = new ValueWrapper(value);
        super.put(key, valueWrapper);
        return value;
    }

    private static Object copyObject(Object oldObj) {
        Object newObj = null;
        try {
           //Create a stream in which to serialize the object.
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            //Serialize the object into the stream
            p.writeObject(oldObj);

            //Create an input stream from which to deserialize the object
            byte[] byteArray = ostream.toByteArray();
            ByteArrayInputStream istream = new ByteArrayInputStream(byteArray);
            ObjectInputStream q = new ObjectInputStream(istream);
            //Deserialize the object
            newObj = q.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return newObj;
    }

    public static void main(String[] args) {
        ReadObject myHashtable = new ReadObject();
        myHashtable.put("key", "value");
        ReadObject myHashtableCopy = (ReadObject)copyObject(myHashtable);
        String value = (String)myHashtableCopy.get("key");
    }
}
