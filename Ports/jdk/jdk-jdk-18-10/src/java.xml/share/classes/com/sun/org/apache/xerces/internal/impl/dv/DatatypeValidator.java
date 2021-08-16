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

package com.sun.org.apache.xerces.internal.impl.dv;

import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;

/**
 * The interface that a DTD datatype must implement. The implementation of this
 * interface must be thread-safe.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 */
public interface DatatypeValidator {

    /**
     * validate a given string against this DV
     *
     * @param content       the string value that needs to be validated
     * @param context       the validation context
     */
    public void validate(String content, ValidationContext context)
        throws InvalidDatatypeValueException;

}
