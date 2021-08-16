/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole.inspector;

import java.io.IOException;
import javax.management.*;
import javax.swing.Icon;
import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.MBeansTab;
import sun.tools.jconsole.ProxyClient.SnapshotMBeanServerConnection;

public class XMBean {

    private final MBeansTab mbeansTab;
    private final ObjectName objectName;
    private Icon icon;
    private String text;
    private Boolean broadcaster;
    private final Object broadcasterLock = new Object();
    private MBeanInfo mbeanInfo;
    private final Object mbeanInfoLock = new Object();

    public XMBean(ObjectName objectName, MBeansTab mbeansTab) {
        this.mbeansTab = mbeansTab;
        this.objectName = objectName;
        text = objectName.getKeyProperty("name");
        if (text == null) {
            text = objectName.getDomain();
        }
        if (MBeanServerDelegate.DELEGATE_NAME.equals(objectName)) {
            icon = IconManager.MBEANSERVERDELEGATE;
        } else {
            icon = IconManager.MBEAN;
        }
    }

    MBeanServerConnection getMBeanServerConnection() {
        return mbeansTab.getMBeanServerConnection();
    }

    SnapshotMBeanServerConnection getSnapshotMBeanServerConnection() {
        return mbeansTab.getSnapshotMBeanServerConnection();
    }

    public Boolean isBroadcaster() {
        synchronized (broadcasterLock) {
            if (broadcaster == null) {
                try {
                    broadcaster = getMBeanServerConnection().isInstanceOf(
                            getObjectName(),
                            "javax.management.NotificationBroadcaster");
                } catch (Exception e) {
                    if (JConsole.isDebug()) {
                        System.err.println("Couldn't check if MBean [" +
                                objectName + "] is a notification broadcaster");
                        e.printStackTrace();
                    }
                    return false;
                }
            }
            return broadcaster;
        }
    }

    public Object invoke(String operationName) throws Exception {
        Object result = getMBeanServerConnection().invoke(
                getObjectName(), operationName, new Object[0], new String[0]);
        return result;
    }

    public Object invoke(String operationName, Object params[], String sig[])
            throws Exception {
        Object result = getMBeanServerConnection().invoke(
                getObjectName(), operationName, params, sig);
        return result;
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InstanceNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, IOException {
        getMBeanServerConnection().setAttribute(getObjectName(), attribute);
    }

    public Object getAttribute(String attributeName)
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        return getSnapshotMBeanServerConnection().getAttribute(
                getObjectName(), attributeName);
    }

    public AttributeList getAttributes(String attributeNames[])
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        return getSnapshotMBeanServerConnection().getAttributes(
                getObjectName(), attributeNames);
    }

    public AttributeList getAttributes(MBeanAttributeInfo attributeNames[])
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException {
        String attributeString[] = new String[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            attributeString[i] = attributeNames[i].getName();
        }
        return getAttributes(attributeString);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public MBeanInfo getMBeanInfo() throws InstanceNotFoundException,
            IntrospectionException, ReflectionException, IOException {
        synchronized (mbeanInfoLock) {
            if (mbeanInfo == null) {
                mbeanInfo = getMBeanServerConnection().getMBeanInfo(objectName);
            }
            return mbeanInfo;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof XMBean)) {
            return false;
        }
        XMBean that = (XMBean) obj;
        return getObjectName().equals(that.getObjectName());
    }

    @Override
    public int hashCode() {
        return (objectName == null ? 0 : objectName.hashCode());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return getText();
    }
}
