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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

public class TransformerFactoryWrapper extends TransformerFactory {
    private TransformerFactory defaultImpl = TransformerFactory.newDefaultInstance();

    @Override
    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        return defaultImpl.newTransformer(source);
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return defaultImpl.newTransformer();
    }

    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        return defaultImpl.newTemplates(source);
    }

    @Override
    public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws TransformerConfigurationException {
        return defaultImpl.getAssociatedStylesheet(source, media, title, charset);
    }

    @Override
    public void setURIResolver(URIResolver resolver) {
        defaultImpl.setURIResolver(resolver);
    }

    @Override
    public URIResolver getURIResolver() {
        return defaultImpl.getURIResolver();
    }

    @Override
    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        defaultImpl.setFeature(name, value);
    }

    @Override
    public boolean getFeature(String name) {
        return defaultImpl.getFeature(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        defaultImpl.setAttribute(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return defaultImpl.getAttribute(name);
    }

    @Override
    public void setErrorListener(ErrorListener listener) {
        defaultImpl.setErrorListener(listener);
    }

    @Override
    public ErrorListener getErrorListener() {
        return defaultImpl.getErrorListener();
    }

}
