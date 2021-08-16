/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.utils;

import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;
import java.util.Stack;
import java.util.StringTokenizer;
import org.w3c.dom.Element;

/**
 * Class to represent a qualified name: "The name of an internal XSLT object,
 * specifically a named template (see [7 Named Templates]), a mode (see [6.7 Modes]),
 * an attribute set (see [8.1.4 Named Attribute Sets]), a key (see [14.2 Keys]),
 * a locale (see [14.3 Number Formatting]), a variable or a parameter (see
 * [12 Variables and Parameters]) is specified as a QName. If it has a prefix,
 * then the prefix is expanded into a URI reference using the namespace declarations
 * in effect on the attribute in which the name occurs. The expanded name
 * consisting of the local part of the name and the possibly null URI reference
 * is used as the name of the object. The default namespace is not used for
 * unprefixed names."
 * @xsl.usage general
 * @LastModified: Oct 2017
 */
public class QName implements java.io.Serializable
{
    static final long serialVersionUID = 467434581652829920L;

  /**
   * The local name.
   * @serial
   */
  protected String _localName;

  /**
   * The namespace URI.
   * @serial
   */
  protected String _namespaceURI;

  /**
   * The namespace prefix.
   * @serial
   */
  protected String _prefix;

  /**
   * The XML namespace.
   */
  public static final String S_XMLNAMESPACEURI =
    "http://www.w3.org/XML/1998/namespace";

  /**
   * The cached hashcode, which is calculated at construction time.
   * @serial
   */
  private int m_hashCode;

  /**
   * Constructs an empty QName.
   * 20001019: Try making this public, to support Serializable? -- JKESS
   */
  public QName(){}

  /**
   * Constructs a new QName with the specified namespace URI and
   * local name.
   *
   * @param namespaceURI The namespace URI if known, or null
   * @param localName The local name
   */
  public QName(String namespaceURI, String localName)
  {
    this(namespaceURI, localName, false);
  }

  /**
   * Constructs a new QName with the specified namespace URI and
   * local name.
   *
   * @param namespaceURI The namespace URI if known, or null
   * @param localName The local name
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String namespaceURI, String localName, boolean validate)
  {

    // This check was already here.  So, for now, I will not add it to the validation
    // that is done when the validate parameter is true.
    if (localName == null)
      throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_NULL, null)); //"Argument 'localName' is null");

    if (validate)
    {
        if (!XML11Char.isXML11ValidNCName(localName))
        {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }
    }

    _namespaceURI = namespaceURI;
    _localName = localName;
    m_hashCode = toString().hashCode();
  }

  /**
   * Constructs a new QName with the specified namespace URI, prefix
   * and local name.
   *
   * @param namespaceURI The namespace URI if known, or null
   * @param prefix The namespace prefix is known, or null
   * @param localName The local name
   *
   */
  public QName(String namespaceURI, String prefix, String localName)
  {
     this(namespaceURI, prefix, localName, false);
  }

 /**
   * Constructs a new QName with the specified namespace URI, prefix
   * and local name.
   *
   * @param namespaceURI The namespace URI if known, or null
   * @param prefix The namespace prefix is known, or null
   * @param localName The local name
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String namespaceURI, String prefix, String localName, boolean validate)
  {

    // This check was already here.  So, for now, I will not add it to the validation
    // that is done when the validate parameter is true.
    if (localName == null)
      throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_NULL, null)); //"Argument 'localName' is null");

    if (validate)
    {
        if (!XML11Char.isXML11ValidNCName(localName))
        {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }

        if ((null != prefix) && (!XML11Char.isXML11ValidNCName(prefix)))
        {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_PREFIX_INVALID,null )); //"Argument 'prefix' not a valid NCName");
        }

    }
    _namespaceURI = namespaceURI;
    _prefix = prefix;
    _localName = localName;
    m_hashCode = toString().hashCode();
  }

  /**
   * Construct a QName from a string, without namespace resolution.  Good
   * for a few odd cases.
   *
   * @param localName Local part of qualified name
   *
   */
  public QName(String localName)
  {
    this(localName, false);
  }

  /**
   * Construct a QName from a string, without namespace resolution.  Good
   * for a few odd cases.
   *
   * @param localName Local part of qualified name
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String localName, boolean validate)
  {

    // This check was already here.  So, for now, I will not add it to the validation
    // that is done when the validate parameter is true.
    if (localName == null)
      throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_NULL, null)); //"Argument 'localName' is null");

    if (validate)
    {
        if (!XML11Char.isXML11ValidNCName(localName))
        {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }
    }
    _namespaceURI = null;
    _localName = localName;
    m_hashCode = toString().hashCode();
  }

  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace stack. The default namespace is
   * not resolved.
   *
   * @param qname Qualified name to resolve
   * @param namespaces Namespace stack to use to resolve namespace
   */
  public QName(String qname, Stack<NameSpace> namespaces)
  {
    this(qname, namespaces, false);
  }

  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace stack. The default namespace is
   * not resolved.
   *
   * @param qname Qualified name to resolve
   * @param namespaces Namespace stack to use to resolve namespace
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String qname, Stack<NameSpace> namespaces, boolean validate)
  {

    String namespace = null;
    String prefix = null;
    int indexOfNSSep = qname.indexOf(':');

    if (indexOfNSSep > 0)
    {
      prefix = qname.substring(0, indexOfNSSep);

      if (prefix.equals("xml"))
      {
        namespace = S_XMLNAMESPACEURI;
      }
      // Do we want this?
      else if (prefix.equals("xmlns"))
      {
        return;
      }
      else
      {
        int depth = namespaces.size();

        for (int i = depth - 1; i >= 0; i--)
        {
          NameSpace ns = namespaces.get(i);

          while (null != ns)
          {
            if ((null != ns.m_prefix) && prefix.equals(ns.m_prefix))
            {
              namespace = ns.m_uri;
              i = -1;

              break;
            }

            ns = ns.m_next;
          }
        }
      }

      if (null == namespace)
      {
        throw new RuntimeException(
          XMLMessages.createXMLMessage(
            XMLErrorResources.ER_PREFIX_MUST_RESOLVE,
            new Object[]{ prefix }));  //"Prefix must resolve to a namespace: "+prefix);
      }
    }

    _localName = (indexOfNSSep < 0)
                 ? qname : qname.substring(indexOfNSSep + 1);

    if (validate)
    {
        if ((_localName == null) || (!XML11Char.isXML11ValidNCName(_localName)))
        {
           throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }
    }
    _namespaceURI = namespace;
    _prefix = prefix;
    m_hashCode = toString().hashCode();
  }

  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace context and prefix resolver.
   * The default namespace is not resolved.
   *
   * @param qname Qualified name to resolve
   * @param namespaceContext Namespace Context to use
   * @param resolver Prefix resolver for this context
   */
  public QName(String qname, Element namespaceContext,
               PrefixResolver resolver)
  {
      this(qname, namespaceContext, resolver, false);
  }

  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace context and prefix resolver.
   * The default namespace is not resolved.
   *
   * @param qname Qualified name to resolve
   * @param namespaceContext Namespace Context to use
   * @param resolver Prefix resolver for this context
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String qname, Element namespaceContext,
               PrefixResolver resolver, boolean validate)
  {

    _namespaceURI = null;

    int indexOfNSSep = qname.indexOf(':');

    if (indexOfNSSep > 0)
    {
      if (null != namespaceContext)
      {
        String prefix = qname.substring(0, indexOfNSSep);

        _prefix = prefix;

        if (prefix.equals("xml"))
        {
          _namespaceURI = S_XMLNAMESPACEURI;
        }

        // Do we want this?
        else if (prefix.equals("xmlns"))
        {
          return;
        }
        else
        {
          _namespaceURI = resolver.getNamespaceForPrefix(prefix,
                  namespaceContext);
        }

        if (null == _namespaceURI)
        {
          throw new RuntimeException(
            XMLMessages.createXMLMessage(
              XMLErrorResources.ER_PREFIX_MUST_RESOLVE,
              new Object[]{ prefix }));  //"Prefix must resolve to a namespace: "+prefix);
        }
      }
      else
      {

        // TODO: error or warning...
      }
    }

    _localName = (indexOfNSSep < 0)
                 ? qname : qname.substring(indexOfNSSep + 1);

    if (validate)
    {
        if ((_localName == null) || (!XML11Char.isXML11ValidNCName(_localName)))
        {
           throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }
    }

    m_hashCode = toString().hashCode();
  }


  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace stack. The default namespace is
   * not resolved.
   *
   * @param qname Qualified name to resolve
   * @param resolver Prefix resolver for this context
   */
  public QName(String qname, PrefixResolver resolver)
  {
    this(qname, resolver, false);
  }

  /**
   * Construct a QName from a string, resolving the prefix
   * using the given namespace stack. The default namespace is
   * not resolved.
   *
   * @param qname Qualified name to resolve
   * @param resolver Prefix resolver for this context
   * @param validate If true the new QName will be validated and an IllegalArgumentException will
   *                 be thrown if it is invalid.
   */
  public QName(String qname, PrefixResolver resolver, boolean validate)
  {

        String prefix = null;
    _namespaceURI = null;

    int indexOfNSSep = qname.indexOf(':');

    if (indexOfNSSep > 0)
    {
      prefix = qname.substring(0, indexOfNSSep);

      if (prefix.equals("xml"))
      {
        _namespaceURI = S_XMLNAMESPACEURI;
      }
      else
      {
        _namespaceURI = resolver.getNamespaceForPrefix(prefix);
      }

      if (null == _namespaceURI)
      {
        throw new RuntimeException(
          XMLMessages.createXMLMessage(
            XMLErrorResources.ER_PREFIX_MUST_RESOLVE,
            new Object[]{ prefix }));  //"Prefix must resolve to a namespace: "+prefix);
      }
      _localName = qname.substring(indexOfNSSep + 1);
    }
    else if (indexOfNSSep == 0)
    {
      throw new RuntimeException(
         XMLMessages.createXMLMessage(
           XMLErrorResources.ER_NAME_CANT_START_WITH_COLON,
           null));
    }
    else
    {
      _localName = qname;
    }

    if (validate)
    {
        if ((_localName == null) || (!XML11Char.isXML11ValidNCName(_localName)))
        {
           throw new IllegalArgumentException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_ARG_LOCALNAME_INVALID,null )); //"Argument 'localName' not a valid NCName");
        }
    }


    m_hashCode = toString().hashCode();
    _prefix = prefix;
  }

  /**
   * Returns the namespace URI. Returns null if the namespace URI
   * is not known.
   *
   * @return The namespace URI, or null
   */
  public String getNamespaceURI()
  {
    return _namespaceURI;
  }

  /**
   * Returns the namespace prefix. Returns null if the namespace
   * prefix is not known.
   *
   * @return The namespace prefix, or null
   */
  public String getPrefix()
  {
    return _prefix;
  }

  /**
   * Returns the local part of the qualified name.
   *
   * @return The local part of the qualified name
   */
  public String getLocalName()
  {
    return _localName;
  }

  /**
   * Return the string representation of the qualified name, using the
   * prefix if available, or the '{ns}foo' notation if not. Performs
   * string concatenation, so beware of performance issues.
   *
   * @return the string representation of the namespace
   */
  public String toString()
  {

    return _prefix != null
           ? (_prefix + ":" + _localName)
           : (_namespaceURI != null
              ? ("{"+_namespaceURI + "}" + _localName) : _localName);
  }

  /**
   * Return the string representation of the qualified name using the
   * the '{ns}foo' notation. Performs
   * string concatenation, so beware of performance issues.
   *
   * @return the string representation of the namespace
   */
  public String toNamespacedString()
  {

    return (_namespaceURI != null
              ? ("{"+_namespaceURI + "}" + _localName) : _localName);
  }


  /**
   * Get the namespace of the qualified name.
   *
   * @return the namespace URI of the qualified name
   */
  public String getNamespace()
  {
    return getNamespaceURI();
  }

  /**
   * Get the local part of the qualified name.
   *
   * @return the local part of the qualified name
   */
  public String getLocalPart()
  {
    return getLocalName();
  }

  /**
   * Return the cached hashcode of the qualified name.
   *
   * @return the cached hashcode of the qualified name
   */
  public int hashCode()
  {
    return m_hashCode;
  }

  /**
   * Override equals and agree that we're equal if
   * the passed object is a string and it matches
   * the name of the arg.
   *
   * @param ns Namespace URI to compare to
   * @param localPart Local part of qualified name to compare to
   *
   * @return True if the local name and uri match
   */
  public boolean equals(String ns, String localPart)
  {

    String thisnamespace = getNamespaceURI();

    return getLocalName().equals(localPart)
           && (((null != thisnamespace) && (null != ns))
               ? thisnamespace.equals(ns)
               : ((null == thisnamespace) && (null == ns)));
  }

  /**
   * Override equals and agree that we're equal if
   * the passed object is a QName and it matches
   * the name of the arg.
   *
   * @return True if the qualified names are equal
   */
  public boolean equals(Object object)
  {

    if (object == this)
      return true;

    if (object instanceof QName) {
      QName qname = (QName) object;
      String thisnamespace = getNamespaceURI();
      String thatnamespace = qname.getNamespaceURI();

      return getLocalName().equals(qname.getLocalName())
             && (((null != thisnamespace) && (null != thatnamespace))
                 ? thisnamespace.equals(thatnamespace)
                 : ((null == thisnamespace) && (null == thatnamespace)));
    }
    else
      return false;
  }

  /**
   * Given a string, create and return a QName object
   *
   *
   * @param name String to use to create QName
   *
   * @return a QName object
   */
  public static QName getQNameFromString(String name)
  {

    StringTokenizer tokenizer = new StringTokenizer(name, "{}", false);
    QName qname;
    String s1 = tokenizer.nextToken();
    String s2 = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

    if (null == s2)
      qname = new QName(null, s1);
    else
      qname = new QName(s1, s2);

    return qname;
  }

  /**
   * This function tells if a raw attribute name is a
   * xmlns attribute.
   *
   * @param attRawName Raw name of attribute
   *
   * @return True if the attribute starts with or is equal to xmlns
   */
  public static boolean isXMLNSDecl(String attRawName)
  {

    return (attRawName.startsWith("xmlns")
            && (attRawName.equals("xmlns")
                || attRawName.startsWith("xmlns:")));
  }

  /**
   * This function tells if a raw attribute name is a
   * xmlns attribute.
   *
   * @param attRawName Raw name of attribute
   *
   * @return Prefix of attribute
   */
  public static String getPrefixFromXMLNSDecl(String attRawName)
  {

    int index = attRawName.indexOf(':');

    return (index >= 0) ? attRawName.substring(index + 1) : "";
  }

  /**
   * Returns the local name of the given node.
   *
   * @param qname Input name
   *
   * @return Local part of the name if prefixed, or the given name if not
   */
  public static String getLocalPart(String qname)
  {

    int index = qname.indexOf(':');

    return (index < 0) ? qname : qname.substring(index + 1);
  }

  /**
   * Returns the local name of the given node.
   *
   * @param qname Input name
   *
   * @return Prefix of name or empty string if none there
   */
  public static String getPrefixPart(String qname)
  {

    int index = qname.indexOf(':');

    return (index >= 0) ? qname.substring(0, index) : "";
  }
}
