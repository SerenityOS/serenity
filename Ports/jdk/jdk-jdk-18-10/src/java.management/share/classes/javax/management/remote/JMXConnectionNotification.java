/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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


package javax.management.remote;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * <p>Notification emitted when a client connection is opened or
 * closed or when notifications are lost.  These notifications are
 * sent by connector servers (instances of {@link JMXConnectorServer})
 * and by connector clients (instances of {@link JMXConnector}).  For
 * certain connectors, a session can consist of a sequence of
 * connections.  Connection-opened and connection-closed notifications
 * will be sent for each one.</p>
 *
 * <p>The notification type is one of the following:</p>
 *
 * <table class="striped">
 * <caption style="display:none">JMXConnectionNotification Types</caption>
 *
 * <thead style="text-align:left">
 * <tr>
 * <th scope="col">Type</th>
 * <th scope="col">Meaning</th>
 * </tr>
 * </thead>
 *
 * <tbody style="text-align:left">
 * <tr>
 * <th scope="row"><code>jmx.remote.connection.opened</code></th>
 * <td>A new client connection has been opened.</td>
 * </tr>
 *
 * <tr>
 * <th scope="row"><code>jmx.remote.connection.closed</code></th>
 * <td>A client connection has been closed.</td>
 * </tr>
 *
 * <tr>
 * <th scope="row"><code>jmx.remote.connection.failed</code></th>
 * <td>A client connection has failed unexpectedly.</td>
 * </tr>
 *
 * <tr>
 * <th scope="row"><code>jmx.remote.connection.notifs.lost</code></th>
 * <td>A client connection has potentially lost notifications.  This
 * notification only appears on the client side.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <p>The <code>timeStamp</code> of the notification is a time value
 * (consistent with {@link System#currentTimeMillis()}) indicating
 * when the notification was constructed.</p>
 *
 * @since 1.5
 */
public class JMXConnectionNotification extends Notification {

    private static final long serialVersionUID = -2331308725952627538L;

    /**
     * <p>Notification type string for a connection-opened notification.
     */
    public static final String OPENED = "jmx.remote.connection.opened";

    /**
     * <p>Notification type string for a connection-closed notification.
     */
    public static final String CLOSED = "jmx.remote.connection.closed";

    /**
     * <p>Notification type string for a connection-failed notification.
     */
    public static final String FAILED = "jmx.remote.connection.failed";

    /**
     * <p>Notification type string for a connection that has possibly
     * lost notifications.</p>
     */
    public static final String NOTIFS_LOST =
        "jmx.remote.connection.notifs.lost";

    /**
     * Constructs a new connection notification.  The {@link
     * #getSource() source} of the notification depends on whether it
     * is being sent by a connector server or a connector client:
     *
     * <ul>
     *
     * <li>For a connector server, if it is registered in an MBean
     * server, the source is the {@link ObjectName} under which it is
     * registered.  Otherwise, it is a reference to the connector
     * server object itself, an instance of a subclass of {@link
     * JMXConnectorServer}.
     *
     * <li>For a connector client, the source is a reference to the
     * connector client object, an instance of a class implementing
     * {@link JMXConnector}.
     *
     * </ul>
     *
     * @param type the type of the notification.  This is usually one
     * of the constants {@link #OPENED}, {@link #CLOSED}, {@link
     * #FAILED}, {@link #NOTIFS_LOST}.  It is not an error for it to
     * be a different string.
     *
     * @param source the connector server or client emitting the
     * notification.
     *
     * @param connectionId the ID of the connection within its
     * connector server.
     *
     * @param sequenceNumber a non-negative integer.  It is expected
     * but not required that this number will be greater than any
     * previous <code>sequenceNumber</code> in a notification from
     * this source.
     *
     * @param message an unspecified text message, typically containing
     * a human-readable description of the event.  Can be null.
     *
     * @param userData an object whose type and meaning is defined by
     * the connector server.  Can be null.
     *
     * @exception NullPointerException if <code>type</code>,
     * <code>source</code>, or <code>connectionId</code> is null.
     *
     * @exception IllegalArgumentException if
     * <code>sequenceNumber</code> is negative.
     */
    public JMXConnectionNotification(String type,
                                     Object source,
                                     String connectionId,
                                     long sequenceNumber,
                                     String message,
                                     Object userData) {
        /* We don't know whether the parent class (Notification) will
           throw an exception if the type or source is null, because
           JMX 1.2 doesn't specify that.  So we make sure it is not
           null, in case it would throw the wrong exception
           (e.g. IllegalArgumentException instead of
           NullPointerException).  Likewise for the sequence number.  */
        super((String) nonNull(type),
              nonNull(source),
              Math.max(0, sequenceNumber),
              System.currentTimeMillis(),
              message);
        if (type == null || source == null || connectionId == null)
            throw new NullPointerException("Illegal null argument");
        if (sequenceNumber < 0)
            throw new IllegalArgumentException("Negative sequence number");
        this.connectionId = connectionId;
        setUserData(userData);
    }

    private static Object nonNull(Object arg) {
        if (arg == null)
            return "";
        else
            return arg;
    }

    /**
     * <p>The connection ID to which this notification pertains.
     *
     * @return the connection ID.
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * @serial The connection ID to which this notification pertains.
     * @see #getConnectionId()
     **/
    private final String connectionId;
}
