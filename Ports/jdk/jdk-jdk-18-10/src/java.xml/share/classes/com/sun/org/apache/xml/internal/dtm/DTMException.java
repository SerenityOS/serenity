/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.dtm;

/**
 * This class specifies an exceptional condition that occurred
 * in the DTM module.
 */
public class DTMException extends RuntimeException {
    static final long serialVersionUID = -775576419181334734L;

    /**
     * Create a new DTMException.
     *
     * @param message The error or warning message.
     */
    public DTMException(String message) {
        super(message);
    }

    /**
     * Create a new DTMException wrapping an existing exception.
     *
     * @param e The exception to be wrapped.
     */
    public DTMException(Throwable e) {
        super(e);
    }

    /**
     * Wrap an existing exception in a DTMException.
     *
     * <p>This is used for throwing processor exceptions before
     * the processing has started.</p>
     *
     * @param message The error or warning message, or null to
     *                use the message from the embedded exception.
     * @param e Any exception
     */
    public DTMException(String message, Throwable e) {
        super(message, e);
    }
    }
