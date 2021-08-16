/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;
import java.util.List;

import javax.management.remote.JMXServiceURL ;
import javax.management.ObjectName;

@SqeDescriptorKey("INTERFACE ServerDelegateMBean")
public interface ServerDelegateMBean {
    @SqeDescriptorKey("ATTRIBUTE Address")
    public void addAddress(JMXServiceURL url);

    @SqeDescriptorKey("ATTRIBUTE Address")
    public List<JMXServiceURL> getAddresses();

    public String getPort();
    public void setPort(String p);

    public String getJavaVersion();

    public void sqeJmxwsCredentialsProviderCalled();
    public int getSqeJmxwsCredentialsProviderCallCount();

    public void setJmxwsCredentialsProviderUrl(String url);
    public String getJmxwsCredentialsProviderUrl();

    public void testJMXAuthenticatorCalled();
    public int getTestJMXAuthenticatorCallCount();

    public void setTestJMXAuthenticatorPrincipal(Principal principal);
    public String getTestJMXAuthenticatorPrincipalString();

    public void createStandardMBean(
            String implementationClassName,
            String interfaceClassName,
            boolean isMXBean,
            ObjectName name)
            throws Exception;

    public void createStandardMBean(
            String implementationClassName,
            boolean isMXBean,
            ObjectName name)
            throws Exception;
}
