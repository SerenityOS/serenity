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
package com.sun.org.apache.xml.internal.security.signature;

import java.util.Collections;
import java.util.List;

/**
 * Holds the result of a Reference validation.
 */
public class VerifiedReference {

    private final boolean valid;
    private final String uri;
    private final List<VerifiedReference> manifestReferences;

    /**
     * @param valid Whether this Reference was successfully validated or not
     * @param uri The URI of this Reference
     * @param manifestReferences If this reference is a reference to a Manifest, this holds the list
     * of verified referenes associated with this Manifest
     */
    public VerifiedReference(boolean valid, String uri, List<VerifiedReference> manifestReferences) {
        this.valid = valid;
        this.uri = uri;
        if (manifestReferences != null) {
            this.manifestReferences = manifestReferences;
        } else {
            this.manifestReferences = Collections.emptyList();
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getUri() {
        return uri;
    }

    public List<VerifiedReference> getManifestReferences() {
        return Collections.unmodifiableList(manifestReferences);
    }
}
