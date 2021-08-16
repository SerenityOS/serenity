/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xpath.internal.jaxp;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;

import javax.xml.namespace.NamespaceContext;

/**
 * <meta name="usage" content="general"/>
 * This class implements a Default PrefixResolver which
 * can be used to perform prefix-to-namespace lookup
 * for the XPath object.
 * This class delegates the resolution to the passed NamespaceContext
 */
public class JAXPPrefixResolver implements PrefixResolver
{

    private NamespaceContext namespaceContext;


    public JAXPPrefixResolver ( NamespaceContext nsContext ) {
        this.namespaceContext = nsContext;
    }


    public String getNamespaceForPrefix( String prefix ) {
        return namespaceContext.getNamespaceURI( prefix );
    }

    /**
     * Return the base identifier.
     *
     * @return null
     */
    public String getBaseIdentifier() {
        return null;
    }

    /**
     * @see PrefixResolver#handlesNullPrefixes()
     */
    public boolean handlesNullPrefixes() {
        return false;
    }


    /**
     * The URI for the XML namespace.
     * (Duplicate of that found in com.sun.org.apache.xpath.internal.XPathContext).
     */

    public static final String S_XMLNAMESPACEURI =
        "http://www.w3.org/XML/1998/namespace";


    /**
     * Given a prefix and a Context Node, get the corresponding namespace.
     * Warning: This will not work correctly if namespaceContext
     * is an attribute node.
     * @param prefix Prefix to resolve.
     * @param namespaceContext Node from which to start searching for a
     * xmlns attribute that binds a prefix to a namespace.
     * @return Namespace that prefix resolves to, or null if prefix
     * is not bound.
     */
    public String getNamespaceForPrefix(String prefix,
                                      org.w3c.dom.Node namespaceContext) {
        Node parent = namespaceContext;
        String namespace = null;

        if (prefix.equals("xml")) {
            namespace = S_XMLNAMESPACEURI;
        } else {
            int type;

            while ((null != parent) && (null == namespace)
                && (((type = parent.getNodeType()) == Node.ELEMENT_NODE)
                    || (type == Node.ENTITY_REFERENCE_NODE))) {

                if (type == Node.ELEMENT_NODE) {
                    NamedNodeMap nnm = parent.getAttributes();

                    for (int i = 0; i < nnm.getLength(); i++) {
                        Node attr = nnm.item(i);
                        String aname = attr.getNodeName();
                        boolean isPrefix = aname.startsWith("xmlns:");

                        if (isPrefix || aname.equals("xmlns")) {
                            int index = aname.indexOf(':');
                            String p =isPrefix ?aname.substring(index + 1) :"";

                            if (p.equals(prefix)) {
                                namespace = attr.getNodeValue();
                                break;
                            }
                        }
                    }
                }

                parent = parent.getParentNode();
            }
        }
        return namespace;
    }

}
