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
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal;

import java.io.ByteArrayOutputStream;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Derived from Apache sources and changed to use java.security.Signature
 * objects as input instead of
 * com.sun.org.apache.xml.internal.security.algorithms.SignatureAlgorithm objects.
 *
 */
public class SignerOutputStream extends ByteArrayOutputStream {
    private final Signature sig;

    public SignerOutputStream(Signature sig) {
        this.sig = sig;
    }

    @Override
    public void write(int arg0) {
        super.write(arg0);
        try {
            sig.update((byte)arg0);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] arg0, int arg1, int arg2) {
        super.write(arg0, arg1, arg2);
        try {
            sig.update(arg0, arg1, arg2);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
}
