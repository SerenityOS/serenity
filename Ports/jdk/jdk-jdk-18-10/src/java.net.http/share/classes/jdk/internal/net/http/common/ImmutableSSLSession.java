/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http.common;

import java.security.Principal;
import java.util.List;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SNIServerName;

/**
 * All mutating methods throw UnsupportedOperationException
 */
public class ImmutableSSLSession implements SSLSession {
    private final SSLSession delegate;

    ImmutableSSLSession(SSLSession session) {
        this.delegate = session;
    }

    public byte[] getId() {
        return delegate.getId();
    }

    public SSLSessionContext getSessionContext() {
        return delegate.getSessionContext();
    }

    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    public long getLastAccessedTime() {
        return delegate.getLastAccessedTime();
    }

    public void invalidate() {
        throw new UnsupportedOperationException("session is not mutable");
    }

    public boolean isValid() {
        return delegate.isValid();
    }

    public void putValue(String name, Object value) {
        throw new UnsupportedOperationException("session is not mutable");
    }

    public Object getValue(String name) {
        return delegate.getValue(name);
    }

    public void removeValue(String name) {
        throw new UnsupportedOperationException("session is not mutable");
    }

    public String [] getValueNames() {
        return delegate.getValueNames();
    }

    public java.security.cert.Certificate [] getPeerCertificates()
            throws SSLPeerUnverifiedException {
        return delegate.getPeerCertificates();
    }

    public java.security.cert.Certificate [] getLocalCertificates() {
        return delegate.getLocalCertificates();
    }

    public Principal getPeerPrincipal()
            throws SSLPeerUnverifiedException {
        return delegate.getPeerPrincipal();
    }

    public Principal getLocalPrincipal() {
        return delegate.getLocalPrincipal();
    }

    public String getCipherSuite() {
        return delegate.getCipherSuite();
    }

    public String getProtocol() {
        return delegate.getProtocol();
    }

    public String getPeerHost() {
        return delegate.getPeerHost();
    }

    public int getPeerPort() {
        return delegate.getPeerPort();
    }

    public int getPacketBufferSize() {
        return delegate.getPacketBufferSize();
    }

    public int getApplicationBufferSize() {
        return delegate.getApplicationBufferSize();
    }
}
