/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.NoSuchObjectException;
import java.lang.reflect.Proxy;
import sun.rmi.server.Util;

/**
 * The <code>RemoteObject</code> class implements the
 * <code>java.lang.Object</code> behavior for remote objects.
 * <code>RemoteObject</code> provides the remote semantics of Object by
 * implementing methods for hashCode, equals, and toString.
 *
 * @author      Ann Wollrath
 * @author      Laird Dornin
 * @author      Peter Jones
 * @since       1.1
 */
public abstract class RemoteObject implements Remote, java.io.Serializable {

    /** The object's remote reference. */
    transient protected RemoteRef ref;

    /** indicate compatibility with JDK 1.1.x version of class */
    @java.io.Serial
    private static final long serialVersionUID = -3215090123894869218L;

    /**
     * Creates a remote object.
     */
    protected RemoteObject() {
        ref = null;
    }

    /**
     * Creates a remote object, initialized with the specified remote
     * reference.
     * @param newref remote reference
     */
    protected RemoteObject(RemoteRef newref) {
        ref = newref;
    }

    /**
     * Returns the remote reference for the remote object.
     *
     * <p>Note: The object returned from this method may be an instance of
     * an implementation-specific class.  The <code>RemoteObject</code>
     * class ensures serialization portability of its instances' remote
     * references through the behavior of its custom
     * <code>writeObject</code> and <code>readObject</code> methods.  An
     * instance of <code>RemoteRef</code> should not be serialized outside
     * of its <code>RemoteObject</code> wrapper instance or the result may
     * be unportable.
     *
     * @return remote reference for the remote object
     * @since 1.2
     */
    public RemoteRef getRef() {
        return ref;
    }

    /**
     * Returns the stub for the remote object <code>obj</code> passed
     * as a parameter. This operation is only valid <i>after</i>
     * the object has been exported.
     * @param obj the remote object whose stub is needed
     * @return the stub for the remote object, <code>obj</code>.
     * @throws NoSuchObjectException if the stub for the
     * remote object could not be found.
     * @since 1.2
     */
    @SuppressWarnings("deprecation")
    public static Remote toStub(Remote obj) throws NoSuchObjectException {
        if (obj instanceof RemoteStub ||
            (obj != null &&
             Proxy.isProxyClass(obj.getClass()) &&
             Proxy.getInvocationHandler(obj) instanceof
             RemoteObjectInvocationHandler))
        {
            return obj;
        } else {
            return sun.rmi.transport.ObjectTable.getStub(obj);
        }
    }

    /**
     * Returns a hashcode for a remote object.  Two remote object stubs
     * that refer to the same remote object will have the same hash code
     * (in order to support remote objects as keys in hash tables).
     *
     * @see             java.util.Hashtable
     */
    public int hashCode() {
        return (ref == null) ? super.hashCode() : ref.remoteHashCode();
    }

    /**
     * Compares two remote objects for equality.
     * Returns a boolean that indicates whether this remote object is
     * equivalent to the specified Object. This method is used when a
     * remote object is stored in a hashtable.
     * If the specified Object is not itself an instance of RemoteObject,
     * then this method delegates by returning the result of invoking the
     * <code>equals</code> method of its parameter with this remote object
     * as the argument.
     * @param   obj     the Object to compare with
     * @return  true if these Objects are equal; false otherwise.
     * @see             java.util.Hashtable
     */
    public boolean equals(Object obj) {
        if (obj instanceof RemoteObject) {
            if (ref == null) {
                return obj == this;
            } else {
                return ref.remoteEquals(((RemoteObject)obj).ref);
            }
        } else if (obj != null) {
            /*
             * Fix for 4099660: if object is not an instance of RemoteObject,
             * use the result of its equals method, to support symmetry is a
             * remote object implementation class that does not extend
             * RemoteObject wishes to support equality with its stub objects.
             */
            return obj.equals(this);
        } else {
            return false;
        }
    }

    /**
     * Returns a String that represents the value of this remote object.
     */
    public String toString() {
        String classname = Util.getUnqualifiedName(getClass());
        return (ref == null) ? classname :
            classname + "[" + ref.remoteToString() + "]";
    }

    /**
     * <code>writeObject</code> for custom serialization.
     *
     * <p>This method writes this object's serialized form for this class
     * as follows:
     *
     * <p>The {@link RemoteRef#getRefClass(java.io.ObjectOutput) getRefClass}
     * method is invoked on this object's <code>ref</code> field
     * to obtain its external ref type name.
     * If the value returned by <code>getRefClass</code> was
     * a non-<code>null</code> string of length greater than zero,
     * the <code>writeUTF</code> method is invoked on <code>out</code>
     * with the value returned by <code>getRefClass</code>, and then
     * the <code>writeExternal</code> method is invoked on
     * this object's <code>ref</code> field passing <code>out</code>
     * as the argument; otherwise,
     * the <code>writeUTF</code> method is invoked on <code>out</code>
     * with a zero-length string (<code>""</code>), and then
     * the <code>writeObject</code> method is invoked on <code>out</code>
     * passing this object's <code>ref</code> field as the argument.
     *
     * @serialData
     *
     * The serialized data for this class comprises a string (written with
     * <code>ObjectOutput.writeUTF</code>) that is either the external
     * ref type name of the contained <code>RemoteRef</code> instance
     * (the <code>ref</code> field) or a zero-length string, followed by
     * either the external form of the <code>ref</code> field as written by
     * its <code>writeExternal</code> method if the string was of non-zero
     * length, or the serialized form of the <code>ref</code> field as
     * written by passing it to the serialization stream's
     * <code>writeObject</code> if the string was of zero length.
     *
     * <p>If this object is an instance of
     * {@link RemoteStub} or {@link RemoteObjectInvocationHandler}
     * that was returned from any of
     * the <code>UnicastRemoteObject.exportObject</code> methods
     * and custom socket factories are not used,
     * the external ref type name is <code>"UnicastRef"</code>.
     *
     * If this object is an instance of
     * <code>RemoteStub</code> or <code>RemoteObjectInvocationHandler</code>
     * that was returned from any of
     * the <code>UnicastRemoteObject.exportObject</code> methods
     * and custom socket factories are used,
     * the external ref type name is <code>"UnicastRef2"</code>.
     *
     * If this object is an instance of
     * <code>RemoteStub</code> or <code>RemoteObjectInvocationHandler</code>
     * that was returned from
     * the <code>RemoteObject.toStub</code> method (and the argument passed
     * to <code>toStub</code> was not itself a <code>RemoteStub</code>),
     * the external ref type name is a function of how the remote object
     * passed to <code>toStub</code> was exported, as described above.
     *
     * If this object is an instance of
     * <code>RemoteStub</code> or <code>RemoteObjectInvocationHandler</code>
     * that was originally created via deserialization,
     * the external ref type name is the same as that which was read
     * when this object was deserialized.
     *
     * <p>If this object is an instance of
     * <code>java.rmi.server.UnicastRemoteObject</code> that does not
     * use custom socket factories,
     * the external ref type name is <code>"UnicastServerRef"</code>.
     *
     * If this object is an instance of
     * <code>UnicastRemoteObject</code> that does
     * use custom socket factories,
     * the external ref type name is <code>"UnicastServerRef2"</code>.
     *
     * <p>Following is the data that must be written by the
     * <code>writeExternal</code> method and read by the
     * <code>readExternal</code> method of <code>RemoteRef</code>
     * implementation classes that correspond to the each of the
     * defined external ref type names:
     *
     * <p>For <code>"UnicastRef"</code>:
     *
     * <ul>
     *
     * <li>the hostname of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeUTF(String)}
     *
     * <li>the port of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeInt(int)}
     *
     * <li>the data written as a result of calling
     * {link java.rmi.server.ObjID#write(java.io.ObjectOutput)}
     * on the <code>ObjID</code> instance contained in the reference
     *
     * <li>the boolean value <code>false</code>,
     * written by {@link java.io.ObjectOutput#writeBoolean(boolean)}
     *
     * </ul>
     *
     * <p>For <code>"UnicastRef2"</code> with a
     * <code>null</code> client socket factory:
     *
     * <ul>
     *
     * <li>the byte value <code>0x00</code>
     * (indicating <code>null</code> client socket factory),
     * written by {@link java.io.ObjectOutput#writeByte(int)}
     *
     * <li>the hostname of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeUTF(String)}
     *
     * <li>the port of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeInt(int)}
     *
     * <li>the data written as a result of calling
     * {link java.rmi.server.ObjID#write(java.io.ObjectOutput)}
     * on the <code>ObjID</code> instance contained in the reference
     *
     * <li>the boolean value <code>false</code>,
     * written by {@link java.io.ObjectOutput#writeBoolean(boolean)}
     *
     * </ul>
     *
     * <p>For <code>"UnicastRef2"</code> with a
     * non-<code>null</code> client socket factory:
     *
     * <ul>
     *
     * <li>the byte value <code>0x01</code>
     * (indicating non-<code>null</code> client socket factory),
     * written by {@link java.io.ObjectOutput#writeByte(int)}
     *
     * <li>the hostname of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeUTF(String)}
     *
     * <li>the port of the referenced remote object,
     * written by {@link java.io.ObjectOutput#writeInt(int)}
     *
     * <li>a client socket factory (object of type
     * <code>java.rmi.server.RMIClientSocketFactory</code>),
     * written by passing it to an invocation of
     * <code>writeObject</code> on the stream instance
     *
     * <li>the data written as a result of calling
     * {link java.rmi.server.ObjID#write(java.io.ObjectOutput)}
     * on the <code>ObjID</code> instance contained in the reference
     *
     * <li>the boolean value <code>false</code>,
     * written by {@link java.io.ObjectOutput#writeBoolean(boolean)}
     *
     * </ul>
     *
     * <p>For <code>"UnicastServerRef"</code> and
     * <code>"UnicastServerRef2"</code>, no data is written by the
     * <code>writeExternal</code> method or read by the
     * <code>readExternal</code> method.
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        if (ref == null) {
            throw new java.rmi.MarshalException("Invalid remote object");
        } else {
            String refClassName = ref.getRefClass(out);
            if (refClassName == null || refClassName.length() == 0) {
                /*
                 * No reference class name specified, so serialize
                 * remote reference.
                 */
                out.writeUTF("");
                out.writeObject(ref);
            } else {
                /*
                 * Built-in reference class specified, so delegate
                 * to reference to write out its external form.
                 */
                out.writeUTF(refClassName);
                ref.writeExternal(out);
            }
        }
    }

    /**
     * <code>readObject</code> for custom serialization.
     *
     * <p>This method reads this object's serialized form for this class
     * as follows:
     *
     * <p>The <code>readUTF</code> method is invoked on <code>in</code>
     * to read the external ref type name for the <code>RemoteRef</code>
     * instance to be filled in to this object's <code>ref</code> field.
     * If the string returned by <code>readUTF</code> has length zero,
     * the <code>readObject</code> method is invoked on <code>in</code>,
     * and than the value returned by <code>readObject</code> is cast to
     * <code>RemoteRef</code> and this object's <code>ref</code> field is
     * set to that value.
     * Otherwise, this object's <code>ref</code> field is set to a
     * <code>RemoteRef</code> instance that is created of an
     * implementation-specific class corresponding to the external ref
     * type name returned by <code>readUTF</code>, and then
     * the <code>readExternal</code> method is invoked on
     * this object's <code>ref</code> field.
     *
     * <p>If the external ref type name is
     * <code>"UnicastRef"</code>, <code>"UnicastServerRef"</code>,
     * <code>"UnicastRef2"</code>, or <code>"UnicastServerRef2"</code>,
     * a corresponding
     * implementation-specific class must be found, and its
     * <code>readExternal</code> method must read the serial data
     * for that external ref type name as specified to be written
     * in the <b>serialData</b> documentation for this class.
     * If the external ref type name is any other string (of non-zero
     * length), a <code>ClassNotFoundException</code> will be thrown,
     * unless the implementation provides an implementation-specific
     * class corresponding to that external ref type name, in which
     * case this object's <code>ref</code> field will be set to an
     * instance of that implementation-specific class.
     *
     * @param  in the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        String refClassName = in.readUTF();
        if (refClassName == null || refClassName.length() == 0) {
            /*
             * No reference class name specified, so construct
             * remote reference from its serialized form.
             */
            ref = (RemoteRef) in.readObject();
        } else {
            /*
             * Built-in reference class specified, so delegate to
             * internal reference class to initialize its fields from
             * its external form.
             */
            String internalRefClassName =
                RemoteRef.packagePrefix + "." + refClassName;
            Class<?> refClass = Class.forName(internalRefClassName);
            try {
                @SuppressWarnings("deprecation")
                Object tmp = refClass.newInstance();
                ref = (RemoteRef) tmp;

                /*
                 * If this step fails, assume we found an internal
                 * class that is not meant to be a serializable ref
                 * type.
                 */
            } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
                throw new ClassNotFoundException(internalRefClassName, e);
            }
            ref.readExternal(in);
        }
    }
}
