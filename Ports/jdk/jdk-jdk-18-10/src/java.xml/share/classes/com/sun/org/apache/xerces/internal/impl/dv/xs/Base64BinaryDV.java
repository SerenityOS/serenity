/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.dv.xs;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.org.apache.xerces.internal.impl.dv.util.ByteListImpl;

/**
 * Represent the schema type "base64Binary"
 *
 * @xerces.internal
 *
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 * @author Sandy Gao, IBM
 *
 */
public class Base64BinaryDV extends TypeValidator {

    public short getAllowedFacets(){
        return (XSSimpleTypeDecl.FACET_LENGTH | XSSimpleTypeDecl.FACET_MINLENGTH | XSSimpleTypeDecl.FACET_MAXLENGTH | XSSimpleTypeDecl.FACET_PATTERN | XSSimpleTypeDecl.FACET_ENUMERATION | XSSimpleTypeDecl.FACET_WHITESPACE );
    }

    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        byte[] decoded = Base64.decode(content);
        if (decoded == null)
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "base64Binary"});

        return new XBase64(decoded);
    }

    // length of a binary type is the number of bytes
    public int getDataLength(Object value) {
        return ((XBase64)value).getLength();
    }

    /**
     * represent base64 data
     */
    private static final class XBase64 extends ByteListImpl {

        public XBase64(byte[] data) {
            super(data);
        }
        public synchronized String toString() {
            if (canonical == null) {
                canonical = Base64.encode(data);
            }
            return canonical;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof XBase64))
                return false;
            byte[] odata = ((XBase64)obj).data;
            int len = data.length;
            if (len != odata.length)
                return false;
            for (int i = 0; i < len; i++) {
                if (data[i] != odata[i])
                    return false;
            }
            return true;
        }

        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < data.length; ++i) {
                hash = hash * 37 + (((int) data[i]) & 0xff);
            }
            return hash;
        }
    }
} // class Base64BinaryDV
