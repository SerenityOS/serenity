/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share.server;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.lang.management.*;
import nsk.share.*;
import nsk.monitoring.share.*;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

public class ServerMXBean {
        protected MBeanServer mbeanServer;
        protected ObjectName objectName;

        public ServerMXBean(MBeanServer mbeanServer, String name) {
                this.mbeanServer = mbeanServer;
                try {
                        this.objectName = new ObjectName(name);
                } catch (Exception e) {
                        throw Monitoring.convertException(e);
                }
        }

        public ServerMXBean(MBeanServer mbeanServer, ObjectName objectName) {
                this.mbeanServer = mbeanServer;
                this.objectName = objectName;
        }

        protected RuntimeException convertException(Exception e) {
                //e.printStackTrace(logger.getOutStream());
                return new Failure(e);
        }

        /**
         * Retrieves the <code>int</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected int getIntAttribute(String name) {
                try {
                        Integer i = (Integer) mbeanServer.getAttribute(objectName, name);
                        return i.intValue();
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>long</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected long getLongAttribute(String name) {
                try {
                        Long l = (Long) mbeanServer.getAttribute(objectName, name);
                        return l.longValue();
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Sets the value of the specified <code>long</code> attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected void setLongAttribute(String name, long value) {
                Attribute attribute = new Attribute(name, Long.valueOf(value));
                try {
                        mbeanServer.setAttribute(objectName, attribute);
                } catch (Exception e) {
                        throw convertException(e);
                }
        }


        /**
         * Sets the <code>boolean</code> value to the specified attribute from
         * MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         * @param value value of the attribute.
         */
        protected void setBooleanAttribute(String name, boolean value) {
                Attribute attribute = new Attribute(name, Boolean.valueOf(value));
                try {
                        mbeanServer.setAttribute(objectName, attribute);
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>boolean</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected boolean getBooleanAttribute(String name) {
                try {
                        Boolean b = (Boolean) mbeanServer.getAttribute(objectName, name);
                        return b.booleanValue();
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>String</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected String getStringAttribute(String name) {
                try {
                        String s = (String) mbeanServer.getAttribute(objectName, name);
                        return s;
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>String</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected String[] getStringArrayAttribute(String name) {
                try {
                        String[] s = (String[]) mbeanServer.getAttribute(objectName, name);
                        return s;
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>MemoryUsage</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected MemoryUsage getMemoryUsageAttribute(String name) {
                try {
                        Object data = mbeanServer.getAttribute(objectName, name);
                        if (data instanceof MemoryUsage)
                                return (MemoryUsage) data;
                        return MemoryUsage.from((CompositeData) data);
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        /**
         * Retrieves the <code>MemoryType</code> value of the specified attribute
         * from MBeanServer.
         *
         * @param object MBean's <code>ObjectName</code>
         * @param name name of the attribute.
         *
         * @return value of the attribute.
         */
        protected MemoryType getMemoryTypeAttribute(String name) {
                try {
                        Object data = mbeanServer.getAttribute(objectName, name);
                        return (MemoryType) data;
                } catch (Exception e) {
                        throw convertException(e);
                }
        }

        protected<T> T[] convertArray(Object o, Class<T[]> cl) {
                if (cl.isInstance(o))
                        return (T[]) o;
                else {
                        CompositeData[] data = (CompositeData[]) o;
                        Class<?> ccl = cl.getComponentType();
                        T[] t = (T[]) Array.newInstance(ccl, data.length);
                        for (int i = 0; i < t.length; ++i)
                                t[i] = (T) convertObject(data[i], ccl);
                        return t;
                }
        }

        protected<T> T convertObject(Object o, Class<T> cl) {
                if (cl.isInstance(o))
                        return (T) o;
                else {
                        try {
                                Method method = cl.getMethod("from", CompositeData.class);
                                return (T) method.invoke(null, o);
                        } catch (NoSuchMethodException e) {
                                throw Monitoring.convertException(e);
                        } catch (IllegalAccessException e) {
                                throw Monitoring.convertException(e);
                        } catch (InvocationTargetException e) {
                                throw Monitoring.convertException(e);
                        }
                }
        }

        protected void invokeVoidMethod(String name) {
                invokeMethod(name, new Object[0], null);
        }

        protected Object invokeMethod(String name, Object[] params, String[] signature) {
                try {
                        return mbeanServer.invoke(objectName, name, params, signature);
                } catch (InstanceNotFoundException e) {
                        throw Monitoring.convertException(e);
                } catch (MBeanException e) {
                        throw Monitoring.convertException(e);
                } catch (ReflectionException e) {
                        throw Monitoring.convertException(e);
                }
        }

        public ObjectName getObjectName() {
                return null;
        }
}
