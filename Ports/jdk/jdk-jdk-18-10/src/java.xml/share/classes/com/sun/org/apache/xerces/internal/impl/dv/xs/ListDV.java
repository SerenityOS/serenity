/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.org.apache.xerces.internal.xs.datatypes.ObjectList;
import java.util.AbstractList;

/**
 * Represent the schema list types
 *
 * @xerces.internal
 *
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 * @author Sandy Gao, IBM
 *
 * @LastModified: Oct 2017
 */
public class ListDV extends TypeValidator{

    public short getAllowedFacets(){
          return (XSSimpleTypeDecl.FACET_LENGTH | XSSimpleTypeDecl.FACET_MINLENGTH |
                  XSSimpleTypeDecl.FACET_MAXLENGTH | XSSimpleTypeDecl.FACET_PATTERN |
                  XSSimpleTypeDecl.FACET_ENUMERATION | XSSimpleTypeDecl.FACET_WHITESPACE );
    }

    // this method should never be called: XSSimpleTypeDecl is responsible for
    // calling the item type for the convertion
    public Object getActualValue(String content, ValidationContext context)
            throws InvalidDatatypeValueException{
        return content;
    }

    // length of a list type is the number of items in the list
    public int getDataLength(Object value) {
        return ((ListData)value).getLength();
    }

    final static class ListData extends AbstractList<Object> implements ObjectList {
        final Object[] data;
        private String canonical;
        public ListData(Object[] data) {
            this.data = data;
        }
        public synchronized String toString() {
            if (canonical == null) {
                int len = data.length;
                StringBuffer buf = new StringBuffer();
                if (len > 0) {
                    buf.append(data[0].toString());
                }
                for (int i = 1; i < len; i++) {
                    buf.append(' ');
                    buf.append(data[i].toString());
                }
                canonical = buf.toString();
            }
            return canonical;
        }
        public int getLength() {
            return data.length;
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof ListData))
                return false;
            Object[] odata = ((ListData)obj).data;

            int count = data.length;
            if (count != odata.length)
                return false;

            for (int i = 0 ; i < count ; i++) {
                if (!data[i].equals(odata[i]))
                    return false;
            }//end of loop

            //everything went fine.
            return true;
        }

        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < data.length; ++i) {
                hash ^= data[i].hashCode();
            }
            return hash;
        }

        public boolean contains(Object item) {
            for (int i = 0;i < data.length; i++) {
                if (item == data[i]) {
                    return true;
                }
            }
            return false;
        }

        public Object item(int index) {
            if (index < 0 || index >= data.length) {
                return null;
            }
            return data[index];
        }

        /*
         * List methods
         */

        public Object get(int index) {
            if (index >= 0 && index < data.length) {
                return data[index];
            }
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        public int size() {
            return getLength();
        }
    }
} // class ListDV
