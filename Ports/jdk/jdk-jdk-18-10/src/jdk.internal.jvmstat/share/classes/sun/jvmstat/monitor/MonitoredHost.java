/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.monitor;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import sun.jvmstat.monitor.event.HostListener;

/**
 * An abstraction for a host that contains instrumented Java Virtual
 * Machines. The class provides abstract factory methods for creating
 * concrete instances of this class and factory methods for creating
 * {@link MonitoredVm} instances. Concrete implementations of this class
 * provide methods for managing the communications protocols and provide
 * for event notification.
 *
 * @author Brian Doherty
 * @since 1.5
 *
 * @see HostIdentifier
 * @see VmIdentifier
 * @see MonitoredVm
 * @see HostListener
 */
public abstract class MonitoredHost {
    private static Map<HostIdentifier, MonitoredHost> monitoredHosts =
                new HashMap<HostIdentifier, MonitoredHost>();

    /*
     * The default optimized local protocol override mechanism. The value
     * of this property is used to construct the default package name
     * for the default optimized local protocol as follows:
     *        <IMPL_PACKAGE>.monitor.<LOCAL_PROTOCOL>
     * This property is not expected to be set under normal circumstances.
     */
    private static final String LOCAL_PROTOCOL_PROP_NAME =
            "sun.jvmstat.monitor.local";
    private static final String LOCAL_PROTOCOL =
            System.getProperty(LOCAL_PROTOCOL_PROP_NAME, "local");

    /*
     * The default remote protocol override mechanism. The value of
     * this property is used to construct the default package name
     * for the default remote protocol protocol as follows:
     *        <IMPL_PACKAGE>.monitor.protocol.<REMOTE_PROTOCOL>
     * This property is not expected to be set under normal circumstances.
     */
    private static final String REMOTE_PROTOCOL_PROP_NAME =
            "sun.jvmstat.monitor.remote";
    private static final String REMOTE_PROTOCOL =
            System.getProperty(REMOTE_PROTOCOL_PROP_NAME, "rmi");

    /**
     * The HostIdentifier for this MonitoredHost instance.
     */
    protected HostIdentifier hostId;

    /**
     * The polling interval, in milliseconds, for this MonitoredHost instance.
     */
    protected int interval;

    /**
     * The last Exception encountered while polling this MonitoredHost.
     */
    protected Exception lastException;

    /**
     * Factory method to construct MonitoredHost instances to manage
     * connections to the host indicated by {@code hostIdString}
     *
     * @param hostIdString a String representation of a {@link HostIdentifier}
     * @return MonitoredHost - the MonitoredHost instance for communicating
     *                         with the indicated host using the protocol
     *                         specified in hostIdString.
     * @throws MonitorException  Thrown if monitoring errors occur.
     * @throws URISyntaxException Thrown when the hostIdString is poorly
     *                            formed. This exception may get encapsulated
     *                            into MonitorException in a future revision.
     */
    public static MonitoredHost getMonitoredHost(String hostIdString)
                  throws MonitorException, URISyntaxException {
        HostIdentifier hostId = new HostIdentifier(hostIdString);
        return getMonitoredHost(hostId);
    }

    /**
     * Factory method to construct a MonitoredHost instance to manage the
     * connection to the Java Virtual Machine indicated by {@code vmid}.
     *
     * This method provide a convenient short cut for attaching to a specific
     * instrumented Java Virtual Machine. The information in the VmIdentifier
     * is used to construct a corresponding HostIdentifier, which in turn is
     * used to create the MonitoredHost instance.
     *
     * @param vmid The identifier for the target Java Virtual Machine.
     * @return MonitoredHost - The MonitoredHost object needed to attach to
     *                         the target Java Virtual Machine.
     *
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public static MonitoredHost getMonitoredHost(VmIdentifier vmid)
                 throws MonitorException {
        // use the VmIdentifier to construct the corresponding HostIdentifier
        HostIdentifier hostId = new HostIdentifier(vmid);
        return getMonitoredHost(hostId);
    }


    /*
     * Load the MonitoredHostServices
     */
    private static ServiceLoader<MonitoredHostService> monitoredHostServiceLoader =
        ServiceLoader.load(MonitoredHostService.class, MonitoredHostService.class.getClassLoader());

    /**
     * Factory method to construct a MonitoredHost instance to manage the
     * connection to the host indicated by {@code hostId}.
     *
     * @param hostId the identifier for the target host.
     * @return MonitoredHost - The MonitoredHost object needed to attach to
     *                         the target host.
     *
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public static MonitoredHost getMonitoredHost(HostIdentifier hostId)
                  throws MonitorException {
        MonitoredHost mh = null;

        synchronized(monitoredHosts) {
            mh = monitoredHosts.get(hostId);
            if (mh != null) {
                if (mh.isErrored()) {
                    monitoredHosts.remove(hostId);
                } else {
                    return mh;
                }
            }
        }

        hostId = resolveHostId(hostId);

        for (MonitoredHostService mhs : monitoredHostServiceLoader) {
            if (mhs.getScheme().equals(hostId.getScheme())) {
                mh = mhs.getMonitoredHost(hostId);
            }
        }

        if (mh == null) {
            throw new IllegalArgumentException("Could not find MonitoredHost for scheme: " + hostId.getScheme());
        }

        synchronized(monitoredHosts) {
            monitoredHosts.put(mh.hostId, mh);
        }

        return mh;
    }

    /**
     * Method to resolve unspecified components of the given HostIdentifier
     * by constructing a new HostIdentifier that replaces the unspecified
     * components with the default values.
     *
     * @param hostId the unresolved HostIdentifier.
     * @return HostIdentifier - a resolved HostIdentifier.
     *
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    protected static HostIdentifier resolveHostId(HostIdentifier hostId)
                     throws MonitorException {
        String hostname = hostId.getHost();
        String scheme = hostId.getScheme();
        StringBuilder sb = new StringBuilder();

        assert hostname != null;

        if (scheme == null) {
            if (hostname.compareTo("localhost") == 0) {
                scheme = LOCAL_PROTOCOL;
            } else {
                scheme = REMOTE_PROTOCOL;
            }
        }

        sb.append(scheme).append(":").append(hostId.getSchemeSpecificPart());

        String frag = hostId.getFragment();
        if (frag != null) {
            sb.append("#").append(frag);
        }

        try {
            return new HostIdentifier(sb.toString());
        } catch (URISyntaxException e) {
            // programming error - HostIdentifier was valid.
            assert false;
            throw new IllegalArgumentException("Malformed URI created: "
                                               + sb.toString());
        }
    }

    /**
     * Return the resolved HostIdentifier for this MonitoredHost.
     *
     * @return HostIdentifier - the resolved HostIdentifier.
     */
    public HostIdentifier getHostIdentifier() {
        return hostId;
    }

    /* ---- Methods to support polled MonitoredHost Implementations ----- */

    /**
     * Set the polling interval for this MonitoredHost.
     *
     * @param interval the polling interval, in milliseconds
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * Get the polling interval.
     *
     * @return int - the polling interval in milliseconds for this MonitoredHost
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Set the last exception encountered while polling this MonitoredHost.
     *
     * @param lastException the last exception encountered;
     */
    public void setLastException(Exception lastException) {
        this.lastException = lastException;
    }

    /**
     * Get the last exception encountered while polling this MonitoredHost.
     *
     * @return Exception - the last exception occurred while polling this
     *                     MonitoredHost, or {@code null} if no exception
     *                     has occurred or the exception has been cleared,
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Clear the last exception.
     */
    public void clearLastException() {
        lastException = null;
    }

    /**
     * Test if this MonitoredHost is in the errored state. If this method
     * returns true, then the Exception returned by getLastException()
     * indicates the Exception that caused the error condition.
     *
     * @return boolean - true if the MonitoredHost instance has experienced
     *                   an error, or false if it hasn't or if any past
     *                   error has been cleared.
     */
    public boolean isErrored() {
        return lastException != null;
    }

    /**
     * Get the MonitoredVm for the given Java Virtual Machine. The default
     * sampling interval is used for the MonitoredVm instance.
     *
     * @param id the VmIdentifier specifying the target Java Virtual Machine.
     * @return MonitoredVm - the MonitoredVm instance for the target Java
     *                       Virtual Machine.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract MonitoredVm getMonitoredVm(VmIdentifier id)
                                throws MonitorException;

    /**
     * Get the MonitoredVm for the given Java Virtual Machine. The sampling
     * interval is set to the given interval.
     *
     * @param id the VmIdentifier specifying the target Java Virtual Machine.
     * @param interval the sampling interval for the target Java Virtual Machine.
     * @return MonitoredVm - the MonitoredVm instance for the target Java
     *                       Virtual Machine.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract MonitoredVm getMonitoredVm(VmIdentifier id, int interval)
                                throws MonitorException;

    /**
     * Detach from the indicated MonitoredVm.
     *
     * @param vm the monitored Java Virtual Machine.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract void detach(MonitoredVm vm) throws MonitorException;

    /**
     * Add a HostListener. The given listener is added to the list
     * of HostListener objects to be notified of MonitoredHost related events.
     *
     * @param listener the HostListener to add.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract void addHostListener(HostListener listener)
                         throws MonitorException;

    /**
     * Remove a HostListener. The given listener is removed from the list
     * of HostListener objects to be notified of MonitoredHost related events.
     *
     * @param listener the HostListener to add.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract void removeHostListener(HostListener listener)
                         throws MonitorException;

    /**
     * Return the current set of active Java Virtual Machines for this
     * MonitoredHost. The returned Set contains {@link Integer} instances
     * holding the local virtual machine identifier, or <em>lvmid</em>
     * for each instrumented Java Virtual Machine currently available.
     *
     * @return Set - the current set of active Java Virtual Machines associated
     *               with this MonitoredHost, or the empty set of none.
     * @throws MonitorException Thrown if monitoring errors occur.
     */
    public abstract Set<Integer> activeVms() throws MonitorException;
}
