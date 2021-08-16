/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.transforms.params;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.transforms.TransformParam;
import com.sun.org.apache.xml.internal.security.utils.ElementProxy;
import com.sun.org.apache.xml.internal.security.utils.HelperNodeList;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements the parameters for the <A
 * HREF="http://www.w3.org/TR/xmldsig-filter2/">XPath Filter v2.0</A>.
 *
 * @see <A HREF="http://www.w3.org/TR/xmldsig-filter2/">XPath Filter v2.0 (TR)</A>
 */
public final class XPath2FilterContainer extends ElementProxy implements TransformParam {

    /** Field _ATT_FILTER */
    private static final String _ATT_FILTER = "Filter";

    /** Field _ATT_FILTER_VALUE_INTERSECT */
    private static final String _ATT_FILTER_VALUE_INTERSECT = "intersect";

    /** Field _ATT_FILTER_VALUE_SUBTRACT */
    private static final String _ATT_FILTER_VALUE_SUBTRACT = "subtract";

    /** Field _ATT_FILTER_VALUE_UNION */
    private static final String _ATT_FILTER_VALUE_UNION = "union";

    /** Field INTERSECT */
    public static final String INTERSECT =
        XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT;

    /** Field SUBTRACT */
    public static final String SUBTRACT =
        XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT;

    /** Field UNION */
    public static final String UNION =
        XPath2FilterContainer._ATT_FILTER_VALUE_UNION;

    /** Field _TAG_XPATH2 */
    public static final String _TAG_XPATH2 = "XPath";

    /** Field XPathFiler2NS */
    public static final String XPathFilter2NS =
        "http://www.w3.org/2002/06/xmldsig-filter2";

    /**
     * Constructor XPath2FilterContainer
     *
     */
    private XPath2FilterContainer() {
        // no instantiation
    }

    /**
     * Constructor XPath2FilterContainer
     *
     * @param doc
     * @param xpath2filter
     * @param filterType
     */
    private XPath2FilterContainer(Document doc, String xpath2filter, String filterType) {
        super(doc);

        setLocalAttribute(XPath2FilterContainer._ATT_FILTER, filterType);
        appendSelf(createText(xpath2filter));
    }

    /**
     * Constructor XPath2FilterContainer
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    private XPath2FilterContainer(Element element, String baseURI) throws XMLSecurityException {

        super(element, baseURI);

        String filterStr = getLocalAttribute(XPath2FilterContainer._ATT_FILTER);

        if (!filterStr.equals(XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT)
            && !filterStr.equals(XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT)
            && !filterStr.equals(XPath2FilterContainer._ATT_FILTER_VALUE_UNION)) {
            Object[] exArgs = { XPath2FilterContainer._ATT_FILTER, filterStr,
                                XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT
                                + ", "
                                + XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT
                                + " or "
                                + XPath2FilterContainer._ATT_FILTER_VALUE_UNION };

            throw new XMLSecurityException("attributeValueIllegal", exArgs);
        }
    }

    /**
     * Creates a new XPath2FilterContainer with the filter type "intersect".
     *
     * @param doc
     * @param xpath2filter
     * @return the filter.
     */
    public static XPath2FilterContainer newInstanceIntersect(
        Document doc, String xpath2filter
    ) {
        return new XPath2FilterContainer(
            doc, xpath2filter, XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT);
    }

    /**
     * Creates a new XPath2FilterContainer with the filter type "subtract".
     *
     * @param doc
     * @param xpath2filter
     * @return the filter.
     */
    public static XPath2FilterContainer newInstanceSubtract(Document doc, String xpath2filter) {
        return new XPath2FilterContainer(
            doc, xpath2filter, XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT);
    }

    /**
     * Creates a new XPath2FilterContainer with the filter type "union".
     *
     * @param doc
     * @param xpath2filter
     * @return the filter
     */
    public static XPath2FilterContainer newInstanceUnion(Document doc, String xpath2filter) {
        return new XPath2FilterContainer(
            doc, xpath2filter, XPath2FilterContainer._ATT_FILTER_VALUE_UNION);
    }

    /**
     * Method newInstances
     *
     * @param doc
     * @param params
     * @return the nodelist with the data
     */
    public static NodeList newInstances(Document doc, String[][] params) {
        HelperNodeList nl = new HelperNodeList();

        XMLUtils.addReturnToElement(doc, nl);

        for (int i = 0; i < params.length; i++) {
            String type = params[i][0];
            String xpath = params[i][1];

            if (!(type.equals(XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT)
                || type.equals(XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT)
                || type.equals(XPath2FilterContainer._ATT_FILTER_VALUE_UNION))) {
                throw new IllegalArgumentException("The type(" + i + ")=\"" + type
                                                   + "\" is illegal");
            }

            XPath2FilterContainer c = new XPath2FilterContainer(doc, xpath, type);

            nl.appendChild(c.getElement());
            XMLUtils.addReturnToElement(doc, nl);
        }

        return nl;
    }

    /**
     * Creates a XPath2FilterContainer from an existing Element; needed for verification.
     *
     * @param element
     * @param baseURI
     * @return the filter
     *
     * @throws XMLSecurityException
     */
    public static XPath2FilterContainer newInstance(
        Element element, String baseURI
    ) throws XMLSecurityException {
        return new XPath2FilterContainer(element, baseURI);
    }

    /**
     * Returns {@code true} if the {@code Filter} attribute has value "intersect".
     *
     * @return {@code true} if the {@code Filter} attribute has value "intersect".
     */
    public boolean isIntersect() {
        return getLocalAttribute(XPath2FilterContainer._ATT_FILTER
        ).equals(XPath2FilterContainer._ATT_FILTER_VALUE_INTERSECT);
    }

    /**
     * Returns {@code true} if the {@code Filter} attribute has value "subtract".
     *
     * @return {@code true} if the {@code Filter} attribute has value "subtract".
     */
    public boolean isSubtract() {
        return getLocalAttribute(XPath2FilterContainer._ATT_FILTER
        ).equals(XPath2FilterContainer._ATT_FILTER_VALUE_SUBTRACT);
    }

    /**
     * Returns {@code true} if the {@code Filter} attribute has value "union".
     *
     * @return {@code true} if the {@code Filter} attribute has value "union".
     */
    public boolean isUnion() {
        return getLocalAttribute(XPath2FilterContainer._ATT_FILTER
        ).equals(XPath2FilterContainer._ATT_FILTER_VALUE_UNION);
    }

    /**
     * Returns the XPath 2 Filter String
     *
     * @return the XPath 2 Filter String
     */
    public String getXPathFilterStr() {
        return this.getTextFromTextChild();
    }

    /**
     * Returns the first Text node which contains information from the XPath 2
     * Filter String. We must use this stupid hook to enable the here() function
     * to work.
     *
     * @return the first Text node which contains information from the XPath 2 Filter String
     */
    public Node getXPathFilterTextNode() {
        Node childNode = getElement().getFirstChild();
        while (childNode != null) {
            if (childNode.getNodeType() == Node.TEXT_NODE) {
                return childNode;
            }
            childNode = childNode.getNextSibling();
        }

        return null;
    }

    /**
     * Method getBaseLocalName
     *
     * @return the XPATH2 tag
     */
    public final String getBaseLocalName() {
        return XPath2FilterContainer._TAG_XPATH2;
    }

    /**
     * Method getBaseNamespace
     *
     * @return XPATH2 tag namespace
     */
    public final String getBaseNamespace() {
        return XPath2FilterContainer.XPathFilter2NS;
    }
}
