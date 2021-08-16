/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share;

import javax.management.*;

/**
 * The <code>CustomMBeanServerBuilder</code> class represents a builder that
 * creates {@link CustomMBeanServer <tt>CustomMBeanServer</tt>} which is
 * implemeted by NSK J2SE SQE Team to test Monitoring and Management API
 * (JSR-174).
 *
 * <p>To instantiate <tt>CustomMBeanServer</tt>, the
 * <b>javax.management.builder.initial</b> system property must contain
 * <code>"nsk.monitoring.share.CustomMBeanServerBuilder"</code> string.
 */
public class CustomMBeanServerBuilder extends MBeanServerBuilder {

    // NSK default domain
    private static final String DEFAULT_DOMAIN = "nsk.defaultDomain";

    /**
     * Public default constructor.
     */
    public CustomMBeanServerBuilder() {
        super();
    }

    /**
     * Creates a new <code>CustomMBeanServer</code> object.
     *
     * @param defaultDomain Default domain of the new MBean server.
     * @param outer A pointer to the MBean server object that must be passed
     *        to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration
     *        <code>MBeanRegistration</code>} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated with
     *        the new MBeanServer. The new MBeanServer must register this
     *        MBean in its MBean repository.
     *
     * @return A new <code>CustomMBeanServer</code> instance.
     *
     * @see javax.management.MBeanServerBuilder#newMBeanServer
     */
    public MBeanServer newMBeanServer(String defaultDomain,
                                  MBeanServer outer,
                                  MBeanServerDelegate delegate) {
        if (defaultDomain == null || defaultDomain.length() == 0)
            return new CustomMBeanServer(DEFAULT_DOMAIN);
        else
            return new CustomMBeanServer(defaultDomain);
    }

    /**
     * Creates an instance of the {@link javax.management.MBeanServerDelegate
     * <code>MBeanServerDelegate</code>} class.
     *
     * @return A new <code>MBeanServerDelegate</code> object.
     *
     * @see javax.management.MBeanServerBuilder#newMBeanServerDelegate
     */
    public MBeanServerDelegate newMBeanServerDelegate() {
        return new MBeanServerDelegate();
    }
}
