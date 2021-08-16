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

package com.sun.org.apache.xerces.internal.xs.datatypes;

import com.sun.org.apache.xerces.internal.xs.XSException;
import java.util.List;

/**
 * <p>The <code>ByteList</code> is an immutable ordered collection of
 * <code>byte</code>.</p>
 *
 * @author Ankit Pasricha, IBM
 *
 * @LastModified: Oct 2017
 */
public interface ByteList extends List<Byte> {

    /**
     * The number of <code>byte</code>s in the list. The range of
     * valid child object indices is 0 to <code>length-1</code> inclusive.
     */
    public int getLength();

    /**
     * Checks if the <code>byte</code> <code>item</code> is a
     * member of this list.
     * @param item  <code>byte</code> whose presence in this list
     *   is to be tested.
     * @return  True if this list contains the <code>byte</code>
     *   <code>item</code>.
     */
    public boolean contains(byte item);

    /**
     * Returns the <code>index</code>th item in the collection. The index
     * starts at 0.
     * @param index  index into the collection.
     * @return  The <code>byte</code> at the <code>index</code>th
     *   position in the <code>ByteList</code>.
     * @exception XSException
     *   INDEX_SIZE_ERR: if <code>index</code> is greater than or equal to the
     *   number of objects in the list or less than zero.
     */
    public byte item(int index) throws XSException;

    /**
     * Construct and return a byte array for bytes contained in this list.
     */
    public byte[] toByteArray();
}
