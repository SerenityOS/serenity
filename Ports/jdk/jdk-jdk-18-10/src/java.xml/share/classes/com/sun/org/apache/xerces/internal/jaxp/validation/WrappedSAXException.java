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

package com.sun.org.apache.xerces.internal.jaxp.validation;

import org.xml.sax.SAXException;

/**
 * Wraps {@link SAXException} and make it an unchecked exception.
 * <p>
 * Xerces XNI doesn't allow {@link SAXException} to be thrown.
 * So when the user-supplied error handler throws it,
 * it needs to be tunneled through Xerces.
 *
 * <p>
 * It is a bug if this exception "leaks" to the application.
 *
 * FIXME: use XNIException for this purpose. It's already doing this
 * kind of SAXException tunneling.
 *
 * @author
 *     Kohsuke Kawaguchi
 *
 * @LastModified: Oct 2017
 */
public class WrappedSAXException extends RuntimeException {
    private static final long serialVersionUID = -3201986204982729962L;

    public final SAXException exception;

    WrappedSAXException( SAXException e ) {
        this.exception = e;
    }
}
