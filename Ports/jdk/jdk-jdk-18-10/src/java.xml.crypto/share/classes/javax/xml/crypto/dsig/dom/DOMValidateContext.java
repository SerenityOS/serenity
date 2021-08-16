/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: DOMValidateContext.java,v 1.8 2005/05/10 16:31:14 mullan Exp $
 */
package javax.xml.crypto.dsig.dom;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import java.security.Key;
import org.w3c.dom.Node;

/**
 * A DOM-specific {@link XMLValidateContext}. This class contains additional
 * methods to specify the location in a DOM tree where an {@link XMLSignature}
 * is to be unmarshalled and validated from.
 *
 * <p>Note that the behavior of an unmarshalled <code>XMLSignature</code>
 * is undefined if the contents of the underlying DOM tree are modified by the
 * caller after the <code>XMLSignature</code> is created.
 *
 * <p>Also, note that <code>DOMValidateContext</code> instances can contain
 * information and state specific to the XML signature structure it is
 * used with. The results are unpredictable if a
 * <code>DOMValidateContext</code> is used with different signature structures
 * (for example, you should not use the same <code>DOMValidateContext</code>
 * instance to validate two different {@link XMLSignature} objects).
 *
 * @implNote
 * By default, the JDK implementation enables a secure validation mode by
 * setting the <code>org.jcp.xml.dsig.secureValidation</code> property to
 * <code>Boolean.TRUE</code> (see the {@link #setProperty setProperty}
 * method). When enabled, validation of XML signatures are subject to
 * stricter checking of algorithms and other constraints as specified by the
 * <code>jdk.xml.dsig.secureValidationPolicy</code> security property.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignatureFactory#unmarshalXMLSignature(XMLValidateContext)
 */
public class DOMValidateContext extends DOMCryptoContext
    implements XMLValidateContext {

    private Node node;

    /**
     * Creates a <code>DOMValidateContext</code> containing the specified key
     * selector and node.
     *
     * @param ks a key selector for finding a validation key
     * @param node the node
     * @throws NullPointerException if <code>ks</code> or <code>node</code> is
     *    <code>null</code>
     */
    public DOMValidateContext(KeySelector ks, Node node) {
        if (ks == null) {
            throw new NullPointerException("key selector is null");
        }
        init(node, ks);
    }

    /**
     * Creates a <code>DOMValidateContext</code> containing the specified key
     * and node. The validating key will be stored in a
     * {@link KeySelector#singletonKeySelector singleton KeySelector} that
     * is returned when the {@link #getKeySelector getKeySelector}
     * method is called.
     *
     * @param validatingKey the validating key
     * @param node the node
     * @throws NullPointerException if <code>validatingKey</code> or
     *    <code>node</code> is <code>null</code>
     */
    public DOMValidateContext(Key validatingKey, Node node) {
        if (validatingKey == null) {
            throw new NullPointerException("validatingKey is null");
        }
        init(node, KeySelector.singletonKeySelector(validatingKey));
    }

    private void init(Node node, KeySelector ks) {
        if (node == null) {
            throw new NullPointerException("node is null");
        }

        this.node = node;
        super.setKeySelector(ks);
        super.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
    }

    /**
     * Sets the node.
     *
     * @param node the node
     * @throws NullPointerException if <code>node</code> is <code>null</code>
     * @see #getNode
     */
    public void setNode(Node node) {
        if (node == null) {
            throw new NullPointerException();
        }
        this.node = node;
    }

    /**
     * Returns the node.
     *
     * @return the node (never <code>null</code>)
     * @see #setNode(Node)
     */
    public Node getNode() {
        return node;
    }
}
