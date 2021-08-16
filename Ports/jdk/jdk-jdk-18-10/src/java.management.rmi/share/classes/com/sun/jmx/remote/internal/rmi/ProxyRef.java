/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.internal.rmi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;


@SuppressWarnings({"deprecation",
                   "serial"}) // Externalizable class w/o no-arg c'tor
public class ProxyRef implements RemoteRef {
    private static final long serialVersionUID = -6503061366316814723L;

    public ProxyRef(RemoteRef ref) {
        this.ref = ref;
    }

    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        ref.readExternal(in);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        ref.writeExternal(out);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void invoke(java.rmi.server.RemoteCall call) throws Exception {
        ref.invoke(call);
    }

    public Object invoke(Remote obj, Method method, Object[] params,
                         long opnum) throws Exception {
        return ref.invoke(obj, method, params, opnum);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void done(java.rmi.server.RemoteCall call) throws RemoteException {
        ref.done(call);
    }

    public String getRefClass(ObjectOutput out) {
        return ref.getRefClass(out);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public java.rmi.server.RemoteCall newCall(RemoteObject obj,
            java.rmi.server.Operation[] op, int opnum,
                              long hash) throws RemoteException {
        return ref.newCall(obj, op, opnum, hash);
    }

    public boolean remoteEquals(RemoteRef obj) {
        return ref.remoteEquals(obj);
    }

    public int remoteHashCode() {
        return ref.remoteHashCode();
    }

    public String remoteToString() {
        return ref.remoteToString();
    }

    protected RemoteRef ref;
}
