/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package xwp;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

public class XPathFactoryWrapper extends XPathFactory {
    private XPathFactory defaultImpl = XPathFactory.newDefaultInstance();

    @Override
    public boolean isObjectModelSupported(String objectModel) {
        return defaultImpl.isObjectModelSupported(objectModel);
    }

    @Override
    public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {
        defaultImpl.setFeature(name, value);
    }

    @Override
    public boolean getFeature(String name) throws XPathFactoryConfigurationException {
        return defaultImpl.getFeature(name);
    }

    @Override
    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        defaultImpl.setXPathVariableResolver(resolver);
    }

    @Override
    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        defaultImpl.setXPathFunctionResolver(resolver);
    }

    @Override
    public XPath newXPath() {
        return defaultImpl.newXPath();
    }

}
