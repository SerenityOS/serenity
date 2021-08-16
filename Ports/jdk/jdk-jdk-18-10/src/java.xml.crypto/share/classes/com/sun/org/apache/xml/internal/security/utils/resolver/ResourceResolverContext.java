/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.utils.resolver;

import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Attr;

public class ResourceResolverContext {

    private final Map<String, String> properties;

    public final String uriToResolve;

    public final boolean secureValidation;

    public final String baseUri;

    public final Attr attr;

    public ResourceResolverContext(Attr attr, String baseUri, boolean secureValidation) {
        this(attr, baseUri, secureValidation, Collections.emptyMap());
    }

    public ResourceResolverContext(Attr attr, String baseUri, boolean secureValidation, Map<String, String> properties) {
        this.attr = attr;
        this.baseUri = baseUri;
        this.secureValidation = secureValidation;
        this.uriToResolve = attr != null ? attr.getValue() : null;
        this.properties = Collections.unmodifiableMap(properties != null ? properties : Collections.emptyMap());
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
