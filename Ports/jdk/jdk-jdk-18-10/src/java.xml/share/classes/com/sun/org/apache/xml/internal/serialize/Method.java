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


package com.sun.org.apache.xml.internal.serialize;


/**
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 * @see OutputFormat
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 */
@Deprecated
public final class Method
{


    /**
     * The output method for XML documents.
     */
    public static final String XML = "xml";


    /**
     * The output method for HTML documents.
     */
    public static final String HTML = "html";


    /**
     * The output method for HTML documents as XHTML.
     */
    public static final String XHTML = "xhtml";


    /**
     * The output method for text documents.
     */
    public static final String TEXT = "text";


    /**
     * The output method for FO documents as PDF.
     */
    public static final String FOP = "fop";


}
