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

package sun.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.AccessException;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.server.ExportException;
import java.rmi.server.Operation;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.ServerRef;
import java.rmi.server.Skeleton;
import java.rmi.server.SkeletonNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import sun.rmi.runtime.Log;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.StreamRemoteCall;
import sun.rmi.transport.Target;
import sun.rmi.transport.tcp.TCPTransport;

/**
 * UnicastServerRef implements the remote reference layer server-side
 * behavior for remote objects exported with the "UnicastRef" reference
 * type.
 * If an {@link ObjectInputFilter ObjectInputFilter} is supplied it is
 * invoked during deserialization to filter the arguments,
 * otherwise the default filter of {@link ObjectInputStream ObjectInputStream}
 * applies.
 *
 * @author  Ann Wollrath
 * @author  Roger Riggs
 * @author  Peter Jones
 */
@SuppressWarnings("deprecation")
public class UnicastServerRef extends UnicastRef
    implements ServerRef, Dispatcher
{
    /** value of server call log property */
    @SuppressWarnings("removal")
    public static final boolean logCalls = AccessController.doPrivileged(
        (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("java.rmi.server.logCalls"));

    /** server call log */
    public static final Log callLog =
        Log.getLog("sun.rmi.server.call", "RMI", logCalls);

    // use serialVersionUID from JDK 1.2.2 for interoperability
    private static final long serialVersionUID = -7384275867073752268L;

    /** flag to enable writing exceptions to System.err */
    @SuppressWarnings("removal")
    private static final boolean wantExceptionLog =
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
            Boolean.getBoolean("sun.rmi.server.exceptionTrace"));

    private boolean forceStubUse = false;

    /**
     * flag to remove server-side stack traces before marshalling
     * exceptions thrown by remote invocations to this VM
     */
    @SuppressWarnings("removal")
    private static final boolean suppressStackTraces =
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
            Boolean.getBoolean("sun.rmi.server.suppressStackTraces"));

    /**
     * skeleton to dispatch remote calls through, for 1.1 stub protocol
     * (may be null if stub class only uses 1.2 stub protocol)
     */
    private transient Skeleton skel;

    // The ObjectInputFilter for checking the invocation arguments
    private final transient ObjectInputFilter filter;

    /** maps method hash to Method object for each remote method */
    private transient Map<Long,Method> hashToMethod_Map = null;

    /**
     * A weak hash map, mapping classes to hash maps that map method
     * hashes to method objects.
     **/
    private static final WeakClassHashMap<Map<Long,Method>> hashToMethod_Maps =
        new HashToMethod_Maps();

    /** cache of impl classes that have no corresponding skeleton class */
    private static final Map<Class<?>,?> withoutSkeletons =
        Collections.synchronizedMap(new WeakHashMap<Class<?>,Void>());

    /**
     * Create a new (empty) Unicast server remote reference.
     * The filter is null to defer to the  default ObjectInputStream filter, if any.
     */
    public UnicastServerRef() {
        this.filter = null;
    }

    /**
     * Construct a Unicast server remote reference for a specified
     * liveRef.
     * The filter is null to defer to the  default ObjectInputStream filter, if any.
     */
    public UnicastServerRef(LiveRef ref) {
        super(ref);
        this.filter = null;
    }

    /**
     * Construct a Unicast server remote reference for a specified
     * liveRef and filter.
     */
    public UnicastServerRef(LiveRef ref, ObjectInputFilter filter) {
        super(ref);
        this.filter = filter;
    }

    /**
     * Construct a Unicast server remote reference to be exported
     * on the specified port.
     */
    public UnicastServerRef(int port) {
        super(new LiveRef(port));
        this.filter = null;
    }

    /**
     * Constructs a UnicastServerRef to be exported on an
     * anonymous port (i.e., 0) and that uses a pregenerated stub class
     * (NOT a dynamic proxy instance) if 'forceStubUse' is 'true'.
     *
     * This constructor is only called by the method
     * UnicastRemoteObject.exportObject(Remote) passing 'true' for
     * 'forceStubUse'.  The UnicastRemoteObject.exportObject(Remote) method
     * returns RemoteStub, so it must ensure that the stub for the
     * exported object is an instance of a pregenerated stub class that
     * extends RemoteStub (instead of an instance of a dynamic proxy class
     * which is not an instance of RemoteStub).
     **/
    public UnicastServerRef(boolean forceStubUse) {
        this(0);
        this.forceStubUse = forceStubUse;
    }

    /**
     * With the addition of support for dynamic proxies as stubs, this
     * method is obsolete because it returns RemoteStub instead of the more
     * general Remote.  It should not be called.  It sets the
     * 'forceStubUse' flag to true so that the stub for the exported object
     * is forced to be an instance of the pregenerated stub class, which
     * extends RemoteStub.
     *
     * Export this object, create the skeleton and stubs for this
     * dispatcher.  Create a stub based on the type of the impl,
     * initialize it with the appropriate remote reference. Create the
     * target defined by the impl, dispatcher (this) and stub.
     * Export that target via the Ref.
     **/
    public RemoteStub exportObject(Remote impl, Object data)
        throws RemoteException
    {
        forceStubUse = true;
        return (RemoteStub) exportObject(impl, data, false);
    }

    /**
     * Export this object, create the skeleton and stubs for this
     * dispatcher.  Create a stub based on the type of the impl,
     * initialize it with the appropriate remote reference. Create the
     * target defined by the impl, dispatcher (this) and stub.
     * Export that target via the Ref.
     */
    public Remote exportObject(Remote impl, Object data,
                               boolean permanent)
        throws RemoteException
    {
        Class<?> implClass = impl.getClass();
        Remote stub;

        try {
            stub = Util.createProxy(implClass, getClientRef(), forceStubUse);
        } catch (IllegalArgumentException e) {
            throw new ExportException(
                "remote object implements illegal remote interface", e);
        }
        if (stub instanceof RemoteStub) {
            setSkeleton(impl);
        }

        Target target =
            new Target(impl, this, stub, ref.getObjID(), permanent);
        ref.exportObject(target);
        hashToMethod_Map = hashToMethod_Maps.get(implClass);
        return stub;
    }

    /**
     * Return the hostname of the current client.  When called from a
     * thread actively handling a remote method invocation the
     * hostname of the client is returned.
     * @exception ServerNotActiveException If called outside of servicing
     * a remote method invocation.
     */
    public String getClientHost() throws ServerNotActiveException {
        return TCPTransport.getClientHost();
    }

    /**
     * Discovers and sets the appropriate skeleton for the impl.
     */
    public void setSkeleton(Remote impl) throws RemoteException {
        if (!withoutSkeletons.containsKey(impl.getClass())) {
            try {
                skel = Util.createSkeleton(impl);
            } catch (SkeletonNotFoundException e) {
                /*
                 * Ignore exception for skeleton class not found, because a
                 * skeleton class is not necessary with the 1.2 stub protocol.
                 * Remember that this impl's class does not have a skeleton
                 * class so we don't waste time searching for it again.
                 */
                withoutSkeletons.put(impl.getClass(), null);
            }
        }
    }

    /**
     * Call to dispatch to the remote object (on the server side).
     * The up-call to the server and the marshalling of return result
     * (or exception) should be handled before returning from this
     * method.
     * @param obj the target remote object for the call
     * @param call the "remote call" from which operation and
     * method arguments can be obtained.
     * @exception IOException If unable to marshal return result or
     * release input or output streams
     */
    public void dispatch(Remote obj, RemoteCall call) throws IOException {
        // positive operation number in 1.1 stubs;
        // negative version number in 1.2 stubs and beyond...
        int num;
        long op;

        try {
            // read remote call header
            ObjectInput in;
            try {
                in = call.getInputStream();
                num = in.readInt();
            } catch (Exception readEx) {
                throw new UnmarshalException("error unmarshalling call header",
                                             readEx);
            }
            if (skel != null) {
                // If there is a skeleton, use it
                    oldDispatch(obj, call, num);
                    return;

            } else if (num >= 0){
                throw new UnmarshalException(
                        "skeleton class not found but required for client version");
            }
            try {
                op = in.readLong();
            } catch (Exception readEx) {
                throw new UnmarshalException("error unmarshalling call header",
                        readEx);
            }

            /*
             * Since only system classes (with null class loaders) will be on
             * the execution stack during parameter unmarshalling for the 1.2
             * stub protocol, tell the MarshalInputStream not to bother trying
             * to resolve classes using its superclasses's default method of
             * consulting the first non-null class loader on the stack.
             */
            MarshalInputStream marshalStream = (MarshalInputStream) in;
            marshalStream.skipDefaultResolveClass();

            Method method = hashToMethod_Map.get(op);
            if (method == null) {
                throw new UnmarshalException("unrecognized method hash: " +
                    "method not supported by remote object");
            }

            // if calls are being logged, write out object id and operation
            logCall(obj, method);

            // unmarshal parameters
            Class<?>[] types = method.getParameterTypes();
            Object[] params = new Object[types.length];

            try {
                unmarshalCustomCallData(in);
                // Unmarshal the parameters
                for (int i = 0; i < types.length; i++) {
                    params[i] = unmarshalValue(types[i], in);
                }

            } catch (AccessException aex) {
                // For compatibility, AccessException is not wrapped in UnmarshalException
                // disable saving any refs in the inputStream for GC
                ((StreamRemoteCall) call).discardPendingRefs();
                throw aex;
            } catch (java.io.IOException | ClassNotFoundException e) {
                // disable saving any refs in the inputStream for GC
                ((StreamRemoteCall) call).discardPendingRefs();
                throw new UnmarshalException(
                    "error unmarshalling arguments", e);
            } finally {
                call.releaseInputStream();
            }

            // make upcall on remote object
            Object result;
            try {
                result = method.invoke(obj, params);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }

            // marshal return value
            try {
                ObjectOutput out = call.getResultStream(true);
                Class<?> rtype = method.getReturnType();
                if (rtype != void.class) {
                    marshalValue(rtype, result, out);
                }
            } catch (IOException ex) {
                throw new MarshalException("error marshalling return", ex);
                /*
                 * This throw is problematic because when it is caught below,
                 * we attempt to marshal it back to the client, but at this
                 * point, a "normal return" has already been indicated,
                 * so marshalling an exception will corrupt the stream.
                 * This was the case with skeletons as well; there is no
                 * immediately obvious solution without a protocol change.
                 */
            }
        } catch (Throwable e) {
            Throwable origEx = e;
            logCallException(e);

            ObjectOutput out = call.getResultStream(false);
            if (e instanceof Error) {
                e = new ServerError(
                    "Error occurred in server thread", (Error) e);
            } else if (e instanceof RemoteException) {
                e = new ServerException(
                    "RemoteException occurred in server thread",
                    (Exception) e);
            }
            if (suppressStackTraces) {
                clearStackTraces(e);
            }
            out.writeObject(e);

            // AccessExceptions should cause Transport.serviceCall
            // to flag the connection as unusable.
            if (origEx instanceof AccessException) {
                throw new IOException("Connection is not reusable", origEx);
            }
        } finally {
            call.releaseInputStream(); // in case skeleton doesn't
            call.releaseOutputStream();
        }
    }

    /**
     * Sets a filter for invocation arguments, if a filter has been set.
     * Called by dispatch before the arguments are read.
     */
    @SuppressWarnings("removal")
    protected void unmarshalCustomCallData(ObjectInput in)
            throws IOException, ClassNotFoundException {
        if (filter != null &&
                in instanceof ObjectInputStream) {
            // Set the filter on the stream
            ObjectInputStream ois = (ObjectInputStream) in;

            AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
                ois.setObjectInputFilter(filter);
                return null;
            });
        }
    }

    /**
     * Handle server-side dispatch using the RMI 1.1 stub/skeleton
     * protocol, given a non-negative operation number or negative method hash
     * that has already been read from the call stream.
     * Exceptions are handled by the caller to be sent to the remote client.
     *
     * @param obj the target remote object for the call
     * @param call the "remote call" from which operation and
     * method arguments can be obtained.
     * @param op the operation number
     * @throws Exception if unable to marshal return result or
     * release input or output streams
     */
    private void oldDispatch(Remote obj, RemoteCall call, int op)
        throws Exception
    {
        long hash;              // hash for matching stub with skeleton

        // read remote call header
        ObjectInput in;
        in = call.getInputStream();
        try {
            Class<?> clazz = Class.forName("sun.rmi.transport.DGCImpl_Skel");
            if (clazz.isAssignableFrom(skel.getClass())) {
                ((MarshalInputStream)in).useCodebaseOnly();
            }
        } catch (ClassNotFoundException ignore) { }

        try {
            hash = in.readLong();
        } catch (Exception ioe) {
            throw new UnmarshalException("error unmarshalling call header", ioe);
        }

        // if calls are being logged, write out object id and operation
        Operation[] operations = skel.getOperations();
        logCall(obj, op >= 0 && op < operations.length ?  operations[op] : "op: " + op);
        unmarshalCustomCallData(in);
        // dispatch to skeleton for remote object
        skel.dispatch(obj, call, op, hash);
    }

    /**
     * Clear the stack trace of the given Throwable by replacing it with
     * an empty StackTraceElement array, and do the same for all of its
     * chained causative exceptions.
     */
    public static void clearStackTraces(Throwable t) {
        StackTraceElement[] empty = new StackTraceElement[0];
        while (t != null) {
            t.setStackTrace(empty);
            t = t.getCause();
        }
    }

    /**
     * Log the details of an incoming call.  The method parameter is either of
     * type java.lang.reflect.Method or java.rmi.server.Operation.
     */
    private void logCall(Remote obj, Object method) {
        if (callLog.isLoggable(Log.VERBOSE)) {
            String clientHost;
            try {
                clientHost = getClientHost();
            } catch (ServerNotActiveException snae) {
                clientHost = "(local)"; // shouldn't happen
            }
            callLog.log(Log.VERBOSE, "[" + clientHost + ": " +
                              obj.getClass().getName() +
                              ref.getObjID().toString() + ": " +
                              method + "]");
        }
    }

    /**
     * Log the exception detail of an incoming call.
     */
    private void logCallException(Throwable e) {
        // if calls are being logged, log them
        if (callLog.isLoggable(Log.BRIEF)) {
            String clientHost = "";
            try {
                clientHost = "[" + getClientHost() + "] ";
            } catch (ServerNotActiveException snae) {
            }
            callLog.log(Log.BRIEF, clientHost + "exception: ", e);
        }

        // write exceptions (only) to System.err if desired
        if (wantExceptionLog) {
            java.io.PrintStream log = System.err;
            synchronized (log) {
                log.println();
                log.println("Exception dispatching call to " +
                            ref.getObjID() + " in thread \"" +
                            Thread.currentThread().getName() +
                            "\" at " + (new Date()) + ":");
                e.printStackTrace(log);
            }
        }
    }

    /**
     * Returns the class of the ref type to be serialized.
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastServerRef";
    }

    /**
     * Return the client remote reference for this remoteRef.
     * In the case of a client RemoteRef "this" is the answer.
     * For a server remote reference, a client side one will have to
     * found or created.
     */
    protected RemoteRef getClientRef() {
        return new UnicastRef(ref);
    }

    /**
     * Write out external representation for remote ref.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
    }

    /**
     * Read in external representation for remote ref.
     * @exception ClassNotFoundException If the class for an object
     * being restored cannot be found.
     */
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        // object is re-exported elsewhere (e.g., by UnicastRemoteObject)
        ref = null;
        skel = null;
    }


    /**
     * A weak hash map, mapping classes to hash maps that map method
     * hashes to method objects.
     **/
    private static class HashToMethod_Maps
        extends WeakClassHashMap<Map<Long,Method>>
    {
        HashToMethod_Maps() {}

        @SuppressWarnings("removal")
        protected Map<Long,Method> computeValue(Class<?> remoteClass) {
            Map<Long,Method> map = new HashMap<>();
            for (Class<?> cl = remoteClass;
                 cl != null;
                 cl = cl.getSuperclass())
            {
                for (Class<?> intf : cl.getInterfaces()) {
                    if (Remote.class.isAssignableFrom(intf)) {
                        for (Method method : intf.getMethods()) {
                            final Method m = method;
                            /*
                             * Set this Method object to override language
                             * access checks so that the dispatcher can invoke
                             * methods from non-public remote interfaces.
                             */
                            AccessController.doPrivileged(
                                new PrivilegedAction<Void>() {
                                public Void run() {
                                    m.setAccessible(true);
                                    return null;
                                }
                            });
                            map.put(Util.computeMethodHash(m), m);
                        }
                    }
                }
            }
            return map;
        }
    }

}
