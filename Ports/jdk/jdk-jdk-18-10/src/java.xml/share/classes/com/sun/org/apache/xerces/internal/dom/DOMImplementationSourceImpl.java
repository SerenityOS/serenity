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

package com.sun.org.apache.xerces.internal.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMImplementationList;
import org.w3c.dom.DOMImplementationSource;

/**
 * Supply one the right implementation, based upon requested features. Each
 * implemented <code>DOMImplementationSource</code> object is listed in the
 * binding-specific list of available sources so that its
 * <code>DOMImplementation</code> objects are made available.
 *
 * <p>See also the
 * <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#DOMImplementationSource'>
 * Document Object Model (DOM) Level 3 Core Specification</a>.
 *
 * @xerces.internal
 *
 * @LastModified: Oct 2017
 */
public class DOMImplementationSourceImpl
    implements DOMImplementationSource {

    /**
     * A method to request a DOM implementation.
     * @param features A string that specifies which features are required.
     *   This is a space separated list in which each feature is specified
     *   by its name optionally followed by a space and a version number.
     *   This is something like: "XML 1.0 Traversal Events 2.0"
     * @return An implementation that has the desired features, or
     *   <code>null</code> if this source has none.
     */
    public DOMImplementation getDOMImplementation(String features) {
        // first check whether the CoreDOMImplementation would do
        DOMImplementation impl =
            CoreDOMImplementationImpl.getDOMImplementation();
        if (testImpl(impl, features)) {
            return impl;
        }
        // if not try the DOMImplementation
        impl = DOMImplementationImpl.getDOMImplementation();
        if (testImpl(impl, features)) {
            return impl;
        }

        return null;
    }

    /**
     * A method to request a list of DOM implementations that support the
     * specified features and versions, as specified in .
     * @param features A string that specifies which features and versions
     *   are required. This is a space separated list in which each feature
     *   is specified by its name optionally followed by a space and a
     *   version number. This is something like: "XML 3.0 Traversal +Events
     *   2.0"
     * @return A list of DOM implementations that support the desired
     *   features.
     */
    public DOMImplementationList getDOMImplementationList(String features) {
        // first check whether the CoreDOMImplementation would do
        DOMImplementation impl = CoreDOMImplementationImpl.getDOMImplementation();
        final List<DOMImplementation> implementations = new ArrayList<>();
        if (testImpl(impl, features)) {
            implementations.add(impl);
        }
        impl = DOMImplementationImpl.getDOMImplementation();
        if (testImpl(impl, features)) {
            implementations.add(impl);
        }

        return new DOMImplementationListImpl(implementations);
    }

    boolean testImpl(DOMImplementation impl, String features) {

        StringTokenizer st = new StringTokenizer(features);
        String feature = null;
        String version = null;

        if (st.hasMoreTokens()) {
           feature = st.nextToken();
        }
        while (feature != null) {
           boolean isVersion = false;
           if (st.hasMoreTokens()) {
               char c;
               version = st.nextToken();
               c = version.charAt(0);
               switch (c) {
               case '0': case '1': case '2': case '3': case '4':
               case '5': case '6': case '7': case '8': case '9':
                   isVersion = true;
               }
           } else {
               version = null;
           }
           if (isVersion) {
               if (!impl.hasFeature(feature, version)) {
                   return false;
               }
               if (st.hasMoreTokens()) {
                   feature = st.nextToken();
               } else {
                   feature = null;
               }
           } else {
               if (!impl.hasFeature(feature, null)) {
                   return false;
               }
               feature = version;
           }
        }
        return true;
    }
}
