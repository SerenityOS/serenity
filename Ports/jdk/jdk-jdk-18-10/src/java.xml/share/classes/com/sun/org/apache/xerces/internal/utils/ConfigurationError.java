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

package com.sun.org.apache.xerces.internal.utils;

/**
 * A configuration error. This was an internal class in ObjectFactory previously
 *
 * @LastModified: Oct 2017
 */
public final class ConfigurationError extends Error {
    private static final long serialVersionUID = 8095902236393167968L;

    //
    // Data
    //

    /** Exception. */
    private Exception exception;

    //
    // Constructors
    //

    /**
     * Construct a new instance with the specified detail string and
     * exception.
     */
    ConfigurationError(String msg, Exception x) {
        super(msg);
        this.exception = x;
    } // <init>(String,Exception)

    //
    // methods
    //

    /** Returns the exception associated to this error. */
    public Exception getException() {
        return exception;
    } // getException():Exception

} // class ConfigurationError
