/*
 * Copyright (c) 2001, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

public final class ReferenceRegistryStub
    extends java.rmi.server.RemoteStub
    implements java.rmi.registry.Registry, java.rmi.Remote
{
    private static final java.rmi.server.Operation[] operations = {
        new java.rmi.server.Operation("void bind(java.lang.String, java.rmi.Remote)"),
        new java.rmi.server.Operation("java.lang.String list()[]"),
        new java.rmi.server.Operation("java.rmi.Remote lookup(java.lang.String)"),
        new java.rmi.server.Operation("void rebind(java.lang.String, java.rmi.Remote)"),
        new java.rmi.server.Operation("void unbind(java.lang.String)")
    };

    private static final long interfaceHash = 4905912898345647071L;

    // constructors
    public ReferenceRegistryStub() {
        super();
    }
    public ReferenceRegistryStub(java.rmi.server.RemoteRef ref) {
        super(ref);
    }

    // methods from remote interfaces

    // implementation of bind(String, Remote)
    public void bind(java.lang.String $param_String_1, java.rmi.Remote $param_Remote_2)
        throws java.rmi.AccessException, java.rmi.AlreadyBoundException, java.rmi.RemoteException
    {
        try {
            java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 0, interfaceHash);
            try {
                java.io.ObjectOutput out = call.getOutputStream();
                out.writeObject($param_String_1);
                out.writeObject($param_Remote_2);
            } catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling arguments", e);
            }
            ref.invoke(call);
            ref.done(call);
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.rmi.AlreadyBoundException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }

    // implementation of list()
    public java.lang.String[] list()
        throws java.rmi.AccessException, java.rmi.RemoteException
    {
        try {
            java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 1, interfaceHash);
            ref.invoke(call);
            java.lang.String[] $result;
            try {
                java.io.ObjectInput in = call.getInputStream();
                $result = (java.lang.String[]) in.readObject();
            } catch (java.io.IOException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling return", e);
            } catch (java.lang.ClassNotFoundException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling return", e);
            } finally {
                ref.done(call);
            }
            return $result;
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }

    // implementation of lookup(String)
    public java.rmi.Remote lookup(java.lang.String $param_String_1)
        throws java.rmi.AccessException, java.rmi.NotBoundException, java.rmi.RemoteException
    {
        try {
            java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 2, interfaceHash);
            try {
                java.io.ObjectOutput out = call.getOutputStream();
                out.writeObject($param_String_1);
            } catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling arguments", e);
            }
            ref.invoke(call);
            java.rmi.Remote $result;
            try {
                java.io.ObjectInput in = call.getInputStream();
                $result = (java.rmi.Remote) in.readObject();
            } catch (java.io.IOException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling return", e);
            } catch (java.lang.ClassNotFoundException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling return", e);
            } finally {
                ref.done(call);
            }
            return $result;
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.rmi.NotBoundException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }

    // implementation of rebind(String, Remote)
    public void rebind(java.lang.String $param_String_1, java.rmi.Remote $param_Remote_2)
        throws java.rmi.AccessException, java.rmi.RemoteException
    {
        try {
            java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 3, interfaceHash);
            try {
                java.io.ObjectOutput out = call.getOutputStream();
                out.writeObject($param_String_1);
                out.writeObject($param_Remote_2);
            } catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling arguments", e);
            }
            ref.invoke(call);
            ref.done(call);
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }

    // implementation of unbind(String)
    public void unbind(java.lang.String $param_String_1)
        throws java.rmi.AccessException, java.rmi.NotBoundException, java.rmi.RemoteException
    {
        try {
            java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 4, interfaceHash);
            try {
                java.io.ObjectOutput out = call.getOutputStream();
                out.writeObject($param_String_1);
            } catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling arguments", e);
            }
            ref.invoke(call);
            ref.done(call);
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.rmi.NotBoundException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }
}
