/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: XSLTTransformParameterSpec.java,v 1.4 2005/05/10 16:40:18 mullan Exp $
 */
package javax.xml.crypto.dsig.spec;

import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.XMLStructure;

/**
 * Parameters for the <a href="http://www.w3.org/TR/1999/REC-xslt-19991116">
 * XSLT Transform Algorithm</a>.
 * The parameters include a namespace-qualified stylesheet element.
 *
 * <p>An <code>XSLTTransformParameterSpec</code> is instantiated with a
 * mechanism-dependent (ex: DOM) stylesheet element. For example:
 * <pre>
 *   DOMStructure stylesheet = new DOMStructure(element)
 *   XSLTTransformParameterSpec spec = new XSLTransformParameterSpec(stylesheet);
 * </pre>
 * where <code>element</code> is an {@link org.w3c.dom.Element} containing
 * the namespace-qualified stylesheet element.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see Transform
 */
public final class XSLTTransformParameterSpec implements TransformParameterSpec{
    private XMLStructure stylesheet;

    /**
     * Creates an <code>XSLTTransformParameterSpec</code> with the specified
     * stylesheet.
     *
     * @param stylesheet the XSLT stylesheet to be used
     * @throws NullPointerException if <code>stylesheet</code> is
     *    <code>null</code>
     */
    public XSLTTransformParameterSpec(XMLStructure stylesheet) {
        if (stylesheet == null) {
            throw new NullPointerException();
        }
        this.stylesheet = stylesheet;
    }

    /**
     * Returns the stylesheet.
     *
     * @return the stylesheet
     */
    public XMLStructure getStylesheet() {
        return stylesheet;
    }
}
