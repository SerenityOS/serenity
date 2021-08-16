/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.internal;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import javax.security.auth.Subject;


/**
 * <p>An identified listener.  A listener has an Integer id that is
 * unique per connector server.  It selects notifications based on the
 * ObjectName of the originator and an optional
 * NotificationFilter.</p>
 */
public class ClientListenerInfo {
    public ClientListenerInfo(Integer listenerID,
                              ObjectName name,
                              NotificationListener listener,
                              NotificationFilter filter,
                              Object handback,
                              Subject delegationSubject) {
        this.listenerID = listenerID;
        this.name = name;
        this.listener = listener;
        this.filter = filter;
        this.handback = handback;
        this.delegationSubject = delegationSubject;
    }

    public ObjectName getObjectName() {
        return name;
    }

    public Integer getListenerID() {
        return listenerID;
    }

    public NotificationFilter getNotificationFilter() {
        return filter;
    }

    public NotificationListener getListener() {
        return listener;
    }

    public Object getHandback() {
        return handback;
    }

    public Subject getDelegationSubject() {
        return delegationSubject;
    }


    public boolean sameAs(ObjectName name) {
        return (getObjectName().equals(name));
    }


    public boolean sameAs(ObjectName name, NotificationListener listener) {
        return ( getObjectName().equals(name) &&
                 getListener() == listener);
    }


    public boolean sameAs(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
        return ( getObjectName().equals(name) &&
                 getListener() == listener &&
                 getNotificationFilter() == filter &&
                 getHandback() == handback);
    }

    private final ObjectName name;
    private final Integer listenerID;
    private final NotificationFilter filter;

    private final NotificationListener listener;
    private final Object handback;
    private final Subject delegationSubject;
}
