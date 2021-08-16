/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.remote.rmi;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;
import java.util.Map;
import java.util.Collections;
import javax.security.auth.Subject;

import com.sun.jmx.remote.internal.rmi.RMIExporter;
import com.sun.jmx.remote.util.EnvHelp;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import sun.reflect.misc.ReflectUtil;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;
import sun.rmi.transport.LiveRef;

/**
 * <p>An {@link RMIServer} object that is exported through JRMP and that
 * creates client connections as RMI objects exported through JRMP.
 * User code does not usually reference this class directly.</p>
 *
 * @see RMIServerImpl
 *
 * @since 1.5
 */
public class RMIJRMPServerImpl extends RMIServerImpl {

    /**
     * <p>Creates a new {@link RMIServer} object that will be exported
     * on the given port using the given socket factories.</p>
     *
     * @param port the port on which this object and the {@link
     * RMIConnectionImpl} objects it creates will be exported.  Can be
     * zero, to indicate any available port.
     *
     * @param csf the client socket factory for the created RMI
     * objects.  Can be null.
     *
     * @param ssf the server socket factory for the created RMI
     * objects.  Can be null.
     *
     * @param env the environment map.  Can be null.
     *
     * @exception IOException if the {@link RMIServer} object
     * cannot be created.
     *
     * @exception IllegalArgumentException if <code>port</code> is
     * negative.
     */
    public RMIJRMPServerImpl(int port,
                             RMIClientSocketFactory csf,
                             RMIServerSocketFactory ssf,
                             Map<String,?> env)
            throws IOException {

        super(env);

        if (port < 0)
            throw new IllegalArgumentException("Negative port: " + port);

        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
        this.env = (env == null) ? Collections.<String, Object>emptyMap() : env;

        // This attribute was represented by RMIConnectorServer.CREDENTIALS_TYPES.
        // This attribute is superceded by
        // RMIConnectorServer.CREDENTIALS_FILTER_PATTERN.
        // Retaining this for backward compatibility.
        String[] credentialsTypes
                = (String[]) this.env.get("jmx.remote.rmi.server.credential.types");

        String credentialsFilter
                = (String) this.env.get(RMIConnectorServer.CREDENTIALS_FILTER_PATTERN);

        // It is impossible for both attributes to be specified
        if(credentialsTypes != null && credentialsFilter != null)
            throw new IllegalArgumentException("Cannot specify both \""
                    + "jmx.remote.rmi.server.credential.types" + "\" and \""
           + RMIConnectorServer.CREDENTIALS_FILTER_PATTERN + "\"");
        else if(credentialsFilter != null){
            cFilter = ObjectInputFilter.Config.createFilter(credentialsFilter);
            allowedTypes = null;
        }
        else if (credentialsTypes != null) {
            allowedTypes = Arrays.stream(credentialsTypes).filter(
                    s -> s!= null).collect(Collectors.toSet());
            allowedTypes.stream().forEach(ReflectUtil::checkPackageAccess);
            cFilter = this::newClientCheckInput;
        } else {
            allowedTypes = null;
            cFilter = null;
        }

        String userJmxFilter =
                (String) this.env.get(RMIConnectorServer.SERIAL_FILTER_PATTERN);
        if(userJmxFilter != null && !userJmxFilter.isEmpty())
            jmxRmiFilter = ObjectInputFilter.Config.createFilter(userJmxFilter);
        else
            jmxRmiFilter = null;
    }

    protected void export() throws IOException {
        export(this, cFilter);
    }

    private void export(Remote obj, ObjectInputFilter typeFilter) throws RemoteException {
        final RMIExporter exporter =
            (RMIExporter) env.get(RMIExporter.EXPORTER_ATTRIBUTE);
        final boolean daemon = EnvHelp.isServerDaemon(env);

        if (daemon && exporter != null) {
            throw new IllegalArgumentException("If "+EnvHelp.JMX_SERVER_DAEMON+
                    " is specified as true, "+RMIExporter.EXPORTER_ATTRIBUTE+
                    " cannot be used to specify an exporter!");
        }

        if (exporter != null) {
            exporter.exportObject(obj, port, csf, ssf, typeFilter);
        } else {
            if (csf == null && ssf == null) {
                new UnicastServerRef(new LiveRef(port), typeFilter).exportObject(obj, null, daemon);
            } else {
                new UnicastServerRef2(port, csf, ssf, typeFilter).exportObject(obj, null, daemon);
            }
        }
    }

    private void unexport(Remote obj, boolean force)
            throws NoSuchObjectException {
        RMIExporter exporter =
            (RMIExporter) env.get(RMIExporter.EXPORTER_ATTRIBUTE);
        if (exporter == null)
            UnicastRemoteObject.unexportObject(obj, force);
        else
            exporter.unexportObject(obj, force);
    }

    protected String getProtocol() {
        return "rmi";
    }

    /**
     * <p>Returns a serializable stub for this {@link RMIServer} object.</p>
     *
     * @return a serializable stub.
     *
     * @exception IOException if the stub cannot be obtained - e.g the
     *            RMIJRMPServerImpl has not been exported yet.
     */
    public Remote toStub() throws IOException {
        return RemoteObject.toStub(this);
    }

    /**
     * <p>Creates a new client connection as an RMI object exported
     * through JRMP. The port and socket factories for the new
     * {@link RMIConnection} object are the ones supplied
     * to the <code>RMIJRMPServerImpl</code> constructor.</p>
     *
     * @param connectionId the ID of the new connection. Every
     * connection opened by this connector server will have a
     * different id.  The behavior is unspecified if this parameter is
     * null.
     *
     * @param subject the authenticated subject.  Can be null.
     *
     * @return the newly-created <code>RMIConnection</code>.
     *
     * @exception IOException if the new {@link RMIConnection}
     * object cannot be created or exported.
     */
    protected RMIConnection makeClient(String connectionId, Subject subject)
            throws IOException {

        if (connectionId == null)
            throw new NullPointerException("Null connectionId");

        RMIConnection client =
            new RMIConnectionImpl(this, connectionId, getDefaultClassLoader(),
                                  subject, env);
        export(client, jmxRmiFilter);
        return client;
    }

    protected void closeClient(RMIConnection client) throws IOException {
        unexport(client, true);
    }

    /**
     * <p>Called by {@link #close()} to close the connector server by
     * unexporting this object.  After returning from this method, the
     * connector server must not accept any new connections.</p>
     *
     * @exception IOException if the attempt to close the connector
     * server failed.
     */
    protected void closeServer() throws IOException {
        unexport(this, true);
    }

    /**
     * Check that a type in the remote invocation of {@link RMIServerImpl#newClient}
     * is one of the {@code allowedTypes}.
     *
     * @param clazz       the class; may be null
     * @param size        the size for arrays, otherwise is 0
     * @param nObjectRefs the current number of object references
     * @param depth       the current depth
     * @param streamBytes the current number of bytes consumed
     * @return {@code ObjectInputFilter.Status.ALLOWED} if the class is allowed,
     *          otherwise {@code ObjectInputFilter.Status.REJECTED}
     */
    ObjectInputFilter.Status newClientCheckInput(ObjectInputFilter.FilterInfo filterInfo) {
        ObjectInputFilter.Status status = ObjectInputFilter.Status.UNDECIDED;
        if (allowedTypes != null && filterInfo.serialClass() != null) {
            // If enabled, check type
            String type = filterInfo.serialClass().getName();
            if (allowedTypes.contains(type))
                status = ObjectInputFilter.Status.ALLOWED;
            else
                status = ObjectInputFilter.Status.REJECTED;
        }
        return status;
    }

    private final int port;
    private final RMIClientSocketFactory csf;
    private final RMIServerSocketFactory ssf;
    private final Map<String, ?> env;
    private final Set<String> allowedTypes;
    private final ObjectInputFilter jmxRmiFilter;
    private final ObjectInputFilter cFilter;
}
