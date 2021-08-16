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

import java.io.IOException;
import java.io.Serializable;

import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;
import java.util.Objects;

/**
 * A class to represent a Uniform Resource Identifier (URI). This class
 * is designed to handle the parsing of URIs and provide access to
 * the various components (scheme, host, port, userinfo, path, query
 * string and fragment) that may constitute a URI.
 * <p>
 * Parsing of a URI specification is done according to the URI
 * syntax described in RFC 2396
 * <http://www.ietf.org/rfc/rfc2396.txt?number=2396>. Every URI consists
 * of a scheme, followed by a colon (':'), followed by a scheme-specific
 * part. For URIs that follow the "generic URI" syntax, the scheme-
 * specific part begins with two slashes ("//") and may be followed
 * by an authority segment (comprised of user information, host, and
 * port), path segment, query segment and fragment. Note that RFC 2396
 * no longer specifies the use of the parameters segment and excludes
 * the "user:password" syntax as part of the authority segment. If
 * "user:password" appears in a URI, the entire user/password string
 * is stored as userinfo.
 * <p>
 * For URIs that do not follow the "generic URI" syntax (e.g. mailto),
 * the entire scheme-specific part is treated as the "path" portion
 * of the URI.
 * <p>
 * Note that, unlike the java.net.URL class, this class does not provide
 * any built-in network access functionality nor does it provide any
 * scheme-specific functionality (for example, it does not know a
 * default port for a specific scheme). Rather, it only knows the
 * grammar and basic set of operations that can be applied to a URI.
 *
 *
 * @LastModified: Oct 2017
 */
public class URI implements Serializable
{
  private static final long serialVersionUID = 7096266377907081897L;

  /**
   * MalformedURIExceptions are thrown in the process of building a URI
   * or setting fields on a URI when an operation would result in an
   * invalid URI specification.
   *
   */
  public static class MalformedURIException extends IOException
  {
    private static final long serialVersionUID = -8498313684991136829L;

    /**
     * Constructs a <code>MalformedURIException</code> with no specified
     * detail message.
     */
    public MalformedURIException()
    {
      super();
    }

    /**
     * Constructs a <code>MalformedURIException</code> with the
     * specified detail message.
     *
     * @param p_msg the detail message.
     */
    public MalformedURIException(String p_msg)
    {
      super(p_msg);
    }
  }

  /** reserved characters */
  private static final String RESERVED_CHARACTERS = ";/?:@&=+$,";

  /**
   * URI punctuation mark characters - these, combined with
   *   alphanumerics, constitute the "unreserved" characters
   */
  private static final String MARK_CHARACTERS = "-_.!~*'() ";

  /** scheme can be composed of alphanumerics and these characters */
  private static final String SCHEME_CHARACTERS = "+-.";

  /**
   * userinfo can be composed of unreserved, escaped and these
   *   characters
   */
  private static final String USERINFO_CHARACTERS = ";:&=+$,";

  /** Stores the scheme (usually the protocol) for this URI.
   *  @serial */
  private String m_scheme = null;

  /** If specified, stores the userinfo for this URI; otherwise null.
   *  @serial */
  private String m_userinfo = null;

  /** If specified, stores the host for this URI; otherwise null.
   *  @serial */
  private String m_host = null;

  /** If specified, stores the port for this URI; otherwise -1.
   *  @serial */
  private int m_port = -1;

  /** If specified, stores the path for this URI; otherwise null.
   *  @serial */
  private String m_path = null;

  /**
   * If specified, stores the query string for this URI; otherwise
   *   null.
   * @serial
   */
  private String m_queryString = null;

  /** If specified, stores the fragment for this URI; otherwise null.
   *  @serial */
  private String m_fragment = null;

  /** Indicate whether in DEBUG mode          */
  private static boolean DEBUG = false;

  /**
   * Construct a new and uninitialized URI.
   */
  public URI(){}

  /**
   * Construct a new URI from another URI. All fields for this URI are
   * set equal to the fields of the URI passed in.
   *
   * @param p_other the URI to copy (cannot be null)
   */
  public URI(URI p_other)
  {
    initialize(p_other);
  }

  /**
   * Construct a new URI from a URI specification string. If the
   * specification follows the "generic URI" syntax, (two slashes
   * following the first colon), the specification will be parsed
   * accordingly - setting the scheme, userinfo, host,port, path, query
   * string and fragment fields as necessary. If the specification does
   * not follow the "generic URI" syntax, the specification is parsed
   * into a scheme and scheme-specific part (stored as the path) only.
   *
   * @param p_uriSpec the URI specification string (cannot be null or
   *                  empty)
   *
   * @throws MalformedURIException if p_uriSpec violates any syntax
   *                                   rules
   */
  public URI(String p_uriSpec) throws MalformedURIException
  {
    this((URI) null, p_uriSpec);
  }

  /**
   * Construct a new URI from a base URI and a URI specification string.
   * The URI specification string may be a relative URI.
   *
   * @param p_base the base URI (cannot be null if p_uriSpec is null or
   *               empty)
   * @param p_uriSpec the URI specification string (cannot be null or
   *                  empty if p_base is null)
   *
   * @throws MalformedURIException if p_uriSpec violates any syntax
   *                                  rules
   */
  public URI(URI p_base, String p_uriSpec) throws MalformedURIException
  {
    initialize(p_base, p_uriSpec);
  }

  /**
   * Construct a new URI that does not follow the generic URI syntax.
   * Only the scheme and scheme-specific part (stored as the path) are
   * initialized.
   *
   * @param p_scheme the URI scheme (cannot be null or empty)
   * @param p_schemeSpecificPart the scheme-specific part (cannot be
   *                             null or empty)
   *
   * @throws MalformedURIException if p_scheme violates any
   *                                  syntax rules
   */
  public URI(String p_scheme, String p_schemeSpecificPart)
          throws MalformedURIException
  {

    if (p_scheme == null || p_scheme.trim().length() == 0)
    {
      throw new MalformedURIException(
        "Cannot construct URI with null/empty scheme!");
    }

    if (p_schemeSpecificPart == null
            || p_schemeSpecificPart.trim().length() == 0)
    {
      throw new MalformedURIException(
        "Cannot construct URI with null/empty scheme-specific part!");
    }

    setScheme(p_scheme);
    setPath(p_schemeSpecificPart);
  }

  /**
   * Construct a new URI that follows the generic URI syntax from its
   * component parts. Each component is validated for syntax and some
   * basic semantic checks are performed as well.  See the individual
   * setter methods for specifics.
   *
   * @param p_scheme the URI scheme (cannot be null or empty)
   * @param p_host the hostname or IPv4 address for the URI
   * @param p_path the URI path - if the path contains '?' or '#',
   *               then the query string and/or fragment will be
   *               set from the path; however, if the query and
   *               fragment are specified both in the path and as
   *               separate parameters, an exception is thrown
   * @param p_queryString the URI query string (cannot be specified
   *                      if path is null)
   * @param p_fragment the URI fragment (cannot be specified if path
   *                   is null)
   *
   * @throws MalformedURIException if any of the parameters violates
   *                                  syntax rules or semantic rules
   */
  public URI(String p_scheme, String p_host, String p_path, String p_queryString, String p_fragment)
          throws MalformedURIException
  {
    this(p_scheme, null, p_host, -1, p_path, p_queryString, p_fragment);
  }

  /**
   * Construct a new URI that follows the generic URI syntax from its
   * component parts. Each component is validated for syntax and some
   * basic semantic checks are performed as well.  See the individual
   * setter methods for specifics.
   *
   * @param p_scheme the URI scheme (cannot be null or empty)
   * @param p_userinfo the URI userinfo (cannot be specified if host
   *                   is null)
   * @param p_host the hostname or IPv4 address for the URI
   * @param p_port the URI port (may be -1 for "unspecified"; cannot
   *               be specified if host is null)
   * @param p_path the URI path - if the path contains '?' or '#',
   *               then the query string and/or fragment will be
   *               set from the path; however, if the query and
   *               fragment are specified both in the path and as
   *               separate parameters, an exception is thrown
   * @param p_queryString the URI query string (cannot be specified
   *                      if path is null)
   * @param p_fragment the URI fragment (cannot be specified if path
   *                   is null)
   *
   * @throws MalformedURIException if any of the parameters violates
   *                                  syntax rules or semantic rules
   */
  public URI(String p_scheme, String p_userinfo, String p_host, int p_port, String p_path, String p_queryString, String p_fragment)
          throws MalformedURIException
  {

    if (p_scheme == null || p_scheme.trim().length() == 0)
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_SCHEME_REQUIRED, null)); //"Scheme is required!");
    }

    if (p_host == null)
    {
      if (p_userinfo != null)
      {
        throw new MalformedURIException(
          XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_USERINFO_IF_NO_HOST, null)); //"Userinfo may not be specified if host is not specified!");
      }

      if (p_port != -1)
      {
        throw new MalformedURIException(
          XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_PORT_IF_NO_HOST, null)); //"Port may not be specified if host is not specified!");
      }
    }

    if (p_path != null)
    {
      if (p_path.indexOf('?') != -1 && p_queryString != null)
      {
        throw new MalformedURIException(
          XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_QUERY_STRING_IN_PATH, null)); //"Query string cannot be specified in path and query string!");
      }

      if (p_path.indexOf('#') != -1 && p_fragment != null)
      {
        throw new MalformedURIException(
          XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_FRAGMENT_STRING_IN_PATH, null)); //"Fragment cannot be specified in both the path and fragment!");
      }
    }

    setScheme(p_scheme);
    setHost(p_host);
    setPort(p_port);
    setUserinfo(p_userinfo);
    setPath(p_path);
    setQueryString(p_queryString);
    setFragment(p_fragment);
  }

  /**
   * Initialize all fields of this URI from another URI.
   *
   * @param p_other the URI to copy (cannot be null)
   */
  private void initialize(URI p_other)
  {

    m_scheme = p_other.getScheme();
    m_userinfo = p_other.getUserinfo();
    m_host = p_other.getHost();
    m_port = p_other.getPort();
    m_path = p_other.getPath();
    m_queryString = p_other.getQueryString();
    m_fragment = p_other.getFragment();
  }

  /**
   * Initializes this URI from a base URI and a URI specification string.
   * See RFC 2396 Section 4 and Appendix B for specifications on parsing
   * the URI and Section 5 for specifications on resolving relative URIs
   * and relative paths.
   *
   * @param p_base the base URI (may be null if p_uriSpec is an absolute
   *               URI)
   * @param p_uriSpec the URI spec string which may be an absolute or
   *                  relative URI (can only be null/empty if p_base
   *                  is not null)
   *
   * @throws MalformedURIException if p_base is null and p_uriSpec
   *                                  is not an absolute URI or if
   *                                  p_uriSpec violates syntax rules
   */
  private void initialize(URI p_base, String p_uriSpec)
          throws MalformedURIException
  {

    if (p_base == null
            && (p_uriSpec == null || p_uriSpec.trim().length() == 0))
    {
      throw new MalformedURIException(
        XMLMessages.createXMLMessage(XMLErrorResources.ER_CANNOT_INIT_URI_EMPTY_PARMS, null)); //"Cannot initialize URI with empty parameters.");
    }

    // just make a copy of the base if spec is empty
    if (p_uriSpec == null || p_uriSpec.trim().length() == 0)
    {
      initialize(p_base);

      return;
    }

    String uriSpec = p_uriSpec.trim();
    int uriSpecLen = uriSpec.length();
    int index = 0;

    // check for scheme
    int colonIndex = uriSpec.indexOf(':');
    if (colonIndex < 0)
    {
      if (p_base == null)
      {
        throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_SCHEME_IN_URI, new Object[]{uriSpec})); //"No scheme found in URI: "+uriSpec);
      }
    }
    else
    {
      initializeScheme(uriSpec);
      uriSpec = uriSpec.substring(colonIndex+1);
      // This is a fix for XALANJ-2059.
      if(m_scheme != null && p_base != null)
      {
        // a) If <uriSpec> starts with a slash (/), it means <uriSpec> is absolute
        //    and p_base can be ignored.
        //    For example,
        //    uriSpec = file:/myDIR/myXSLFile.xsl
        //    p_base = file:/myWork/
        //
        //    Here, uriSpec has absolute path after scheme file and :
        //    Hence p_base can be ignored.
        //
        // b) Similarily, according to RFC 2396, uri is resolved for <uriSpec> relative to <p_base>
        //    if scheme in <uriSpec> is same as scheme in <p_base>, else p_base can be ignored.
        //
        // c) if <p_base> is not hierarchical, it can be ignored.
        //
        if(uriSpec.startsWith("/") || !m_scheme.equals(p_base.m_scheme) || !p_base.getSchemeSpecificPart().startsWith("/"))
        {
          p_base = null;
        }
      }
      // Fix for XALANJ-2059
      uriSpecLen = uriSpec.length();
    }

    // two slashes means generic URI syntax, so we get the authority
    if (((index + 1) < uriSpecLen)
            && (uriSpec.substring(index).startsWith("//")))
    {
      index += 2;

      int startPos = index;

      // get authority - everything up to path, query or fragment
      char testChar = '\0';

      while (index < uriSpecLen)
      {
        testChar = uriSpec.charAt(index);

        if (testChar == '/' || testChar == '?' || testChar == '#')
        {
          break;
        }

        index++;
      }

      // if we found authority, parse it out, otherwise we set the
      // host to empty string
      if (index > startPos)
      {
        initializeAuthority(uriSpec.substring(startPos, index));
      }
      else
      {
        m_host = "";
      }
    }

    initializePath(uriSpec.substring(index));

    // Resolve relative URI to base URI - see RFC 2396 Section 5.2
    // In some cases, it might make more sense to throw an exception
    // (when scheme is specified is the string spec and the base URI
    // is also specified, for example), but we're just following the
    // RFC specifications
    if (p_base != null)
    {

      // check to see if this is the current doc - RFC 2396 5.2 #2
      // note that this is slightly different from the RFC spec in that
      // we don't include the check for query string being null
      // - this handles cases where the urispec is just a query
      // string or a fragment (e.g. "?y" or "#s") -
      // see <http://www.ics.uci.edu/~fielding/url/test1.html> which
      // identified this as a bug in the RFC
      if (m_path.length() == 0 && m_scheme == null && m_host == null)
      {
        m_scheme = p_base.getScheme();
        m_userinfo = p_base.getUserinfo();
        m_host = p_base.getHost();
        m_port = p_base.getPort();
        m_path = p_base.getPath();

        if (m_queryString == null)
        {
          m_queryString = p_base.getQueryString();
        }

        return;
      }

      // check for scheme - RFC 2396 5.2 #3
      // if we found a scheme, it means absolute URI, so we're done
      if (m_scheme == null)
      {
        m_scheme = p_base.getScheme();
      }

      // check for authority - RFC 2396 5.2 #4
      // if we found a host, then we've got a network path, so we're done
      if (m_host == null)
      {
        m_userinfo = p_base.getUserinfo();
        m_host = p_base.getHost();
        m_port = p_base.getPort();
      }
      else
      {
        return;
      }

      // check for absolute path - RFC 2396 5.2 #5
      if (m_path.length() > 0 && m_path.startsWith("/"))
      {
        return;
      }

      // if we get to this point, we need to resolve relative path
      // RFC 2396 5.2 #6
      String path = "";
      String basePath = p_base.getPath();

      // 6a - get all but the last segment of the base URI path
      if (basePath != null)
      {
        int lastSlash = basePath.lastIndexOf('/');

        if (lastSlash != -1)
        {
          path = basePath.substring(0, lastSlash + 1);
        }
      }

      // 6b - append the relative URI path
      path = path.concat(m_path);

      // 6c - remove all "./" where "." is a complete path segment
      index = -1;

      while ((index = path.indexOf("/./")) != -1)
      {
        path = path.substring(0, index + 1).concat(path.substring(index + 3));
      }

      // 6d - remove "." if path ends with "." as a complete path segment
      if (path.endsWith("/."))
      {
        path = path.substring(0, path.length() - 1);
      }

      // 6e - remove all "<segment>/../" where "<segment>" is a complete
      // path segment not equal to ".."
      index = -1;

      int segIndex = -1;
      String tempString = null;

      while ((index = path.indexOf("/../")) > 0)
      {
        tempString = path.substring(0, path.indexOf("/../"));
        segIndex = tempString.lastIndexOf('/');

        if (segIndex != -1)
        {
          if (!tempString.substring(segIndex++).equals(".."))
          {
            path = path.substring(0, segIndex).concat(path.substring(index
                    + 4));
          }
        }
      }

      // 6f - remove ending "<segment>/.." where "<segment>" is a
      // complete path segment
      if (path.endsWith("/.."))
      {
        tempString = path.substring(0, path.length() - 3);
        segIndex = tempString.lastIndexOf('/');

        if (segIndex != -1)
        {
          path = path.substring(0, segIndex + 1);
        }
      }

      m_path = path;
    }
  }

  /**
   * Initialize the scheme for this URI from a URI string spec.
   *
   * @param p_uriSpec the URI specification (cannot be null)
   *
   * @throws MalformedURIException if URI does not have a conformant
   *                                  scheme
   */
  private void initializeScheme(String p_uriSpec) throws MalformedURIException
  {

    int uriSpecLen = p_uriSpec.length();
    int index = 0;
    String scheme = null;
    char testChar = '\0';

    while (index < uriSpecLen)
    {
      testChar = p_uriSpec.charAt(index);

      if (testChar == ':' || testChar == '/' || testChar == '?'
              || testChar == '#')
      {
        break;
      }

      index++;
    }

    scheme = p_uriSpec.substring(0, index);

    if (scheme.length() == 0)
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_SCHEME_INURI, null)); //"No scheme found in URI.");
    }
    else
    {
      setScheme(scheme);
    }
  }

  /**
   * Initialize the authority (userinfo, host and port) for this
   * URI from a URI string spec.
   *
   * @param p_uriSpec the URI specification (cannot be null)
   *
   * @throws MalformedURIException if p_uriSpec violates syntax rules
   */
  private void initializeAuthority(String p_uriSpec)
          throws MalformedURIException
  {

    int index = 0;
    int start = 0;
    int end = p_uriSpec.length();
    char testChar = '\0';
    String userinfo = null;

    // userinfo is everything up @
    if (p_uriSpec.indexOf('@', start) != -1)
    {
      while (index < end)
      {
        testChar = p_uriSpec.charAt(index);

        if (testChar == '@')
        {
          break;
        }

        index++;
      }

      userinfo = p_uriSpec.substring(start, index);

      index++;
    }

    // host is everything up to ':'
    String host = null;

    start = index;

    while (index < end)
    {
      testChar = p_uriSpec.charAt(index);

      if (testChar == ':')
      {
        break;
      }

      index++;
    }

    host = p_uriSpec.substring(start, index);

    int port = -1;

    if (host.length() > 0)
    {

      // port
      if (testChar == ':')
      {
        index++;

        start = index;

        while (index < end)
        {
          index++;
        }

        String portStr = p_uriSpec.substring(start, index);

        if (portStr.length() > 0)
        {
          for (int i = 0; i < portStr.length(); i++)
          {
            if (!isDigit(portStr.charAt(i)))
            {
              throw new MalformedURIException(
                portStr + " is invalid. Port should only contain digits!");
            }
          }

          try
          {
            port = Integer.parseInt(portStr);
          }
          catch (NumberFormatException nfe)
          {

            // can't happen
          }
        }
      }
    }

    setHost(host);
    setPort(port);
    setUserinfo(userinfo);
  }

  /**
   * Initialize the path for this URI from a URI string spec.
   *
   * @param p_uriSpec the URI specification (cannot be null)
   *
   * @throws MalformedURIException if p_uriSpec violates syntax rules
   */
  private void initializePath(String p_uriSpec) throws MalformedURIException
  {

    if (p_uriSpec == null)
    {
      throw new MalformedURIException(
        "Cannot initialize path from null string!");
    }

    int index = 0;
    int start = 0;
    int end = p_uriSpec.length();
    char testChar = '\0';

    // path - everything up to query string or fragment
    while (index < end)
    {
      testChar = p_uriSpec.charAt(index);

      if (testChar == '?' || testChar == '#')
      {
        break;
      }

      // check for valid escape sequence
      if (testChar == '%')
      {
        if (index + 2 >= end ||!isHex(p_uriSpec.charAt(index + 1))
                ||!isHex(p_uriSpec.charAt(index + 2)))
        {
          throw new MalformedURIException(
            XMLMessages.createXMLMessage(XMLErrorResources.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE, null)); //"Path contains invalid escape sequence!");
        }
      }
      else if (!isReservedCharacter(testChar)
               &&!isUnreservedCharacter(testChar))
      {
        if ('\\' != testChar)
          throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_PATH_INVALID_CHAR, new Object[]{String.valueOf(testChar)})); //"Path contains invalid character: "
                                          //+ testChar);
      }

      index++;
    }

    m_path = p_uriSpec.substring(start, index);

    // query - starts with ? and up to fragment or end
    if (testChar == '?')
    {
      index++;

      start = index;

      while (index < end)
      {
        testChar = p_uriSpec.charAt(index);

        if (testChar == '#')
        {
          break;
        }

        if (testChar == '%')
        {
          if (index + 2 >= end ||!isHex(p_uriSpec.charAt(index + 1))
                  ||!isHex(p_uriSpec.charAt(index + 2)))
          {
            throw new MalformedURIException(
              "Query string contains invalid escape sequence!");
          }
        }
        else if (!isReservedCharacter(testChar)
                 &&!isUnreservedCharacter(testChar))
        {
          throw new MalformedURIException(
            "Query string contains invalid character:" + testChar);
        }

        index++;
      }

      m_queryString = p_uriSpec.substring(start, index);
    }

    // fragment - starts with #
    if (testChar == '#')
    {
      index++;

      start = index;

      while (index < end)
      {
        testChar = p_uriSpec.charAt(index);

        if (testChar == '%')
        {
          if (index + 2 >= end ||!isHex(p_uriSpec.charAt(index + 1))
                  ||!isHex(p_uriSpec.charAt(index + 2)))
          {
            throw new MalformedURIException(
              "Fragment contains invalid escape sequence!");
          }
        }
        else if (!isReservedCharacter(testChar)
                 &&!isUnreservedCharacter(testChar))
        {
          throw new MalformedURIException(
            "Fragment contains invalid character:" + testChar);
        }

        index++;
      }

      m_fragment = p_uriSpec.substring(start, index);
    }
  }

  /**
   * Get the scheme for this URI.
   *
   * @return the scheme for this URI
   */
  public String getScheme()
  {
    return m_scheme;
  }

  /**
   * Get the scheme-specific part for this URI (everything following the
   * scheme and the first colon). See RFC 2396 Section 5.2 for spec.
   *
   * @return the scheme-specific part for this URI
   */
  public String getSchemeSpecificPart()
  {

    final StringBuilder schemespec = new StringBuilder();

    if (m_userinfo != null || m_host != null || m_port != -1)
    {
      schemespec.append("//");
    }

    if (m_userinfo != null)
    {
      schemespec.append(m_userinfo);
      schemespec.append('@');
    }

    if (m_host != null)
    {
      schemespec.append(m_host);
    }

    if (m_port != -1)
    {
      schemespec.append(':');
      schemespec.append(m_port);
    }

    if (m_path != null)
    {
      schemespec.append((m_path));
    }

    if (m_queryString != null)
    {
      schemespec.append('?');
      schemespec.append(m_queryString);
    }

    if (m_fragment != null)
    {
      schemespec.append('#');
      schemespec.append(m_fragment);
    }

    return schemespec.toString();
  }

  /**
   * Get the userinfo for this URI.
   *
   * @return the userinfo for this URI (null if not specified).
   */
  public String getUserinfo()
  {
    return m_userinfo;
  }

  /**
   * Get the host for this URI.
   *
   * @return the host for this URI (null if not specified).
   */
  public String getHost()
  {
    return m_host;
  }

  /**
   * Get the port for this URI.
   *
   * @return the port for this URI (-1 if not specified).
   */
  public int getPort()
  {
    return m_port;
  }

  /**
   * Get the path for this URI (optionally with the query string and
   * fragment).
   *
   * @param p_includeQueryString if true (and query string is not null),
   *                             then a "?" followed by the query string
   *                             will be appended
   * @param p_includeFragment if true (and fragment is not null),
   *                             then a "#" followed by the fragment
   *                             will be appended
   *
   * @return the path for this URI possibly including the query string
   *         and fragment
   */
  public String getPath(boolean p_includeQueryString,
                        boolean p_includeFragment)
  {

    final StringBuilder pathString = new StringBuilder(m_path);

    if (p_includeQueryString && m_queryString != null)
    {
      pathString.append('?');
      pathString.append(m_queryString);
    }

    if (p_includeFragment && m_fragment != null)
    {
      pathString.append('#');
      pathString.append(m_fragment);
    }

    return pathString.toString();
  }

  /**
   * Get the path for this URI. Note that the value returned is the path
   * only and does not include the query string or fragment.
   *
   * @return the path for this URI.
   */
  public String getPath()
  {
    return m_path;
  }

  /**
   * Get the query string for this URI.
   *
   * @return the query string for this URI. Null is returned if there
   *         was no "?" in the URI spec, empty string if there was a
   *         "?" but no query string following it.
   */
  public String getQueryString()
  {
    return m_queryString;
  }

  /**
   * Get the fragment for this URI.
   *
   * @return the fragment for this URI. Null is returned if there
   *         was no "#" in the URI spec, empty string if there was a
   *         "#" but no fragment following it.
   */
  public String getFragment()
  {
    return m_fragment;
  }

  /**
   * Set the scheme for this URI. The scheme is converted to lowercase
   * before it is set.
   *
   * @param p_scheme the scheme for this URI (cannot be null)
   *
   * @throws MalformedURIException if p_scheme is not a conformant
   *                                  scheme name
   */
  public void setScheme(String p_scheme) throws MalformedURIException
  {

    if (p_scheme == null)
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_SCHEME_FROM_NULL_STRING, null)); //"Cannot set scheme from null string!");
    }

    if (!isConformantSchemeName(p_scheme))
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_SCHEME_NOT_CONFORMANT, null)); //"The scheme is not conformant.");
    }

    m_scheme = p_scheme.toLowerCase();
  }

  /**
   * Set the userinfo for this URI. If a non-null value is passed in and
   * the host value is null, then an exception is thrown.
   *
   * @param p_userinfo the userinfo for this URI
   *
   * @throws MalformedURIException if p_userinfo contains invalid
   *                                  characters
   */
  public void setUserinfo(String p_userinfo) throws MalformedURIException
  {

    if (p_userinfo == null)
    {
      m_userinfo = null;
    }
    else
    {
      if (m_host == null)
      {
        throw new MalformedURIException(
          "Userinfo cannot be set when host is null!");
      }

      // userinfo can contain alphanumerics, mark characters, escaped
      // and ';',':','&','=','+','$',','
      int index = 0;
      int end = p_userinfo.length();
      char testChar = '\0';

      while (index < end)
      {
        testChar = p_userinfo.charAt(index);

        if (testChar == '%')
        {
          if (index + 2 >= end ||!isHex(p_userinfo.charAt(index + 1))
                  ||!isHex(p_userinfo.charAt(index + 2)))
          {
            throw new MalformedURIException(
              "Userinfo contains invalid escape sequence!");
          }
        }
        else if (!isUnreservedCharacter(testChar)
                 && USERINFO_CHARACTERS.indexOf(testChar) == -1)
        {
          throw new MalformedURIException(
            "Userinfo contains invalid character:" + testChar);
        }

        index++;
      }
    }

    m_userinfo = p_userinfo;
  }

  /**
   * Set the host for this URI. If null is passed in, the userinfo
   * field is also set to null and the port is set to -1.
   *
   * @param p_host the host for this URI
   *
   * @throws MalformedURIException if p_host is not a valid IP
   *                                  address or DNS hostname.
   */
  public void setHost(String p_host) throws MalformedURIException
  {

    if (p_host == null || p_host.trim().length() == 0)
    {
      m_host = p_host;
      m_userinfo = null;
      m_port = -1;
    }
    else if (!isWellFormedAddress(p_host))
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_HOST_ADDRESS_NOT_WELLFORMED, null)); //"Host is not a well formed address!");
    }

    m_host = p_host;
  }

  /**
   * Set the port for this URI. -1 is used to indicate that the port is
   * not specified, otherwise valid port numbers are  between 0 and 65535.
   * If a valid port number is passed in and the host field is null,
   * an exception is thrown.
   *
   * @param p_port the port number for this URI
   *
   * @throws MalformedURIException if p_port is not -1 and not a
   *                                  valid port number
   */
  public void setPort(int p_port) throws MalformedURIException
  {

    if (p_port >= 0 && p_port <= 65535)
    {
      if (m_host == null)
      {
        throw new MalformedURIException(
          XMLMessages.createXMLMessage(XMLErrorResources.ER_PORT_WHEN_HOST_NULL, null)); //"Port cannot be set when host is null!");
      }
    }
    else if (p_port != -1)
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_INVALID_PORT, null)); //"Invalid port number!");
    }

    m_port = p_port;
  }

  /**
   * Set the path for this URI. If the supplied path is null, then the
   * query string and fragment are set to null as well. If the supplied
   * path includes a query string and/or fragment, these fields will be
   * parsed and set as well. Note that, for URIs following the "generic
   * URI" syntax, the path specified should start with a slash.
   * For URIs that do not follow the generic URI syntax, this method
   * sets the scheme-specific part.
   *
   * @param p_path the path for this URI (may be null)
   *
   * @throws MalformedURIException if p_path contains invalid
   *                                  characters
   */
  public void setPath(String p_path) throws MalformedURIException
  {

    if (p_path == null)
    {
      m_path = null;
      m_queryString = null;
      m_fragment = null;
    }
    else
    {
      initializePath(p_path);
    }
  }

  /**
   * Append to the end of the path of this URI. If the current path does
   * not end in a slash and the path to be appended does not begin with
   * a slash, a slash will be appended to the current path before the
   * new segment is added. Also, if the current path ends in a slash
   * and the new segment begins with a slash, the extra slash will be
   * removed before the new segment is appended.
   *
   * @param p_addToPath the new segment to be added to the current path
   *
   * @throws MalformedURIException if p_addToPath contains syntax
   *                                  errors
   */
  public void appendPath(String p_addToPath) throws MalformedURIException
  {

    if (p_addToPath == null || p_addToPath.trim().length() == 0)
    {
      return;
    }

    if (!isURIString(p_addToPath))
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_PATH_INVALID_CHAR, new Object[]{p_addToPath})); //"Path contains invalid character!");
    }

    if (m_path == null || m_path.trim().length() == 0)
    {
      if (p_addToPath.startsWith("/"))
      {
        m_path = p_addToPath;
      }
      else
      {
        m_path = "/" + p_addToPath;
      }
    }
    else if (m_path.endsWith("/"))
    {
      if (p_addToPath.startsWith("/"))
      {
        m_path = m_path.concat(p_addToPath.substring(1));
      }
      else
      {
        m_path = m_path.concat(p_addToPath);
      }
    }
    else
    {
      if (p_addToPath.startsWith("/"))
      {
        m_path = m_path.concat(p_addToPath);
      }
      else
      {
        m_path = m_path.concat("/" + p_addToPath);
      }
    }
  }

  /**
   * Set the query string for this URI. A non-null value is valid only
   * if this is an URI conforming to the generic URI syntax and
   * the path value is not null.
   *
   * @param p_queryString the query string for this URI
   *
   * @throws MalformedURIException if p_queryString is not null and this
   *                                  URI does not conform to the generic
   *                                  URI syntax or if the path is null
   */
  public void setQueryString(String p_queryString)
          throws MalformedURIException
  {

    if (p_queryString == null)
    {
      m_queryString = null;
    }
    else if (!isGenericURI())
    {
      throw new MalformedURIException(
        "Query string can only be set for a generic URI!");
    }
    else if (getPath() == null)
    {
      throw new MalformedURIException(
        "Query string cannot be set when path is null!");
    }
    else if (!isURIString(p_queryString))
    {
      throw new MalformedURIException(
        "Query string contains invalid character!");
    }
    else
    {
      m_queryString = p_queryString;
    }
  }

  /**
   * Set the fragment for this URI. A non-null value is valid only
   * if this is a URI conforming to the generic URI syntax and
   * the path value is not null.
   *
   * @param p_fragment the fragment for this URI
   *
   * @throws MalformedURIException if p_fragment is not null and this
   *                                  URI does not conform to the generic
   *                                  URI syntax or if the path is null
   */
  public void setFragment(String p_fragment) throws MalformedURIException
  {

    if (p_fragment == null)
    {
      m_fragment = null;
    }
    else if (!isGenericURI())
    {
      throw new MalformedURIException(
        XMLMessages.createXMLMessage(XMLErrorResources.ER_FRAG_FOR_GENERIC_URI, null)); //"Fragment can only be set for a generic URI!");
    }
    else if (getPath() == null)
    {
      throw new MalformedURIException(
        XMLMessages.createXMLMessage(XMLErrorResources.ER_FRAG_WHEN_PATH_NULL, null)); //"Fragment cannot be set when path is null!");
    }
    else if (!isURIString(p_fragment))
    {
      throw new MalformedURIException(XMLMessages.createXMLMessage(XMLErrorResources.ER_FRAG_INVALID_CHAR, null)); //"Fragment contains invalid character!");
    }
    else
    {
      m_fragment = p_fragment;
    }
  }

  /**
   * Determines if the passed-in Object is equivalent to this URI.
   *
   * @param p_test the Object to test for equality.
   *
   * @return true if p_test is a URI with all values equal to this
   *         URI, false otherwise
   */
  @Override
  public boolean equals(Object p_test)
  {

    if (p_test instanceof URI)
    {
      URI testURI = (URI) p_test;

      if (((m_scheme == null && testURI.m_scheme == null) || (m_scheme != null && testURI.m_scheme != null && m_scheme.equals(
              testURI.m_scheme))) && ((m_userinfo == null && testURI.m_userinfo == null) || (m_userinfo != null && testURI.m_userinfo != null && m_userinfo.equals(
              testURI.m_userinfo))) && ((m_host == null && testURI.m_host == null) || (m_host != null && testURI.m_host != null && m_host.equals(
              testURI.m_host))) && m_port == testURI.m_port && ((m_path == null && testURI.m_path == null) || (m_path != null && testURI.m_path != null && m_path.equals(
              testURI.m_path))) && ((m_queryString == null && testURI.m_queryString == null) || (m_queryString != null && testURI.m_queryString != null && m_queryString.equals(
              testURI.m_queryString))) && ((m_fragment == null && testURI.m_fragment == null) || (m_fragment != null && testURI.m_fragment != null && m_fragment.equals(
              testURI.m_fragment))))
      {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + Objects.hashCode(this.m_scheme);
    hash = 59 * hash + Objects.hashCode(this.m_userinfo);
    hash = 59 * hash + Objects.hashCode(this.m_host);
    hash = 59 * hash + this.m_port;
    hash = 59 * hash + Objects.hashCode(this.m_path);
    hash = 59 * hash + Objects.hashCode(this.m_queryString);
    hash = 59 * hash + Objects.hashCode(this.m_fragment);
    return hash;
  }

  /**
   * Get the URI as a string specification. See RFC 2396 Section 5.2.
   *
   * @return the URI string specification
   */
  @Override
  public String toString()
  {

    final StringBuilder uriSpecString = new StringBuilder();

    if (m_scheme != null)
    {
      uriSpecString.append(m_scheme);
      uriSpecString.append(':');
    }

    uriSpecString.append(getSchemeSpecificPart());

    return uriSpecString.toString();
  }

  /**
   * Get the indicator as to whether this URI uses the "generic URI"
   * syntax.
   *
   * @return true if this URI uses the "generic URI" syntax, false
   *         otherwise
   */
  public boolean isGenericURI()
  {

    // presence of the host (whether valid or empty) means
    // double-slashes which means generic uri
    return (m_host != null);
  }

  /**
   * Determine whether a scheme conforms to the rules for a scheme name.
   * A scheme is conformant if it starts with an alphanumeric, and
   * contains only alphanumerics, '+','-' and '.'.
   *
   *
   * @param p_scheme The sheme name to check
   * @return true if the scheme is conformant, false otherwise
   */
  public static boolean isConformantSchemeName(String p_scheme)
  {

    if (p_scheme == null || p_scheme.trim().length() == 0)
    {
      return false;
    }

    if (!isAlpha(p_scheme.charAt(0)))
    {
      return false;
    }

    char testChar;

    for (int i = 1; i < p_scheme.length(); i++)
    {
      testChar = p_scheme.charAt(i);

      if (!isAlphanum(testChar) && SCHEME_CHARACTERS.indexOf(testChar) == -1)
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Determine whether a string is syntactically capable of representing
   * a valid IPv4 address or the domain name of a network host. A valid
   * IPv4 address consists of four decimal digit groups separated by a
   * '.'. A hostname consists of domain labels (each of which must
   * begin and end with an alphanumeric but may contain '-') separated
   * & by a '.'. See RFC 2396 Section 3.2.2.
   *
   *
   * @param p_address The address string to check
   * @return true if the string is a syntactically valid IPv4 address
   *              or hostname
   */
  public static boolean isWellFormedAddress(String p_address)
  {

    if (p_address == null)
    {
      return false;
    }

    String address = p_address.trim();
    int addrLength = address.length();

    if (addrLength == 0 || addrLength > 255)
    {
      return false;
    }

    if (address.startsWith(".") || address.startsWith("-"))
    {
      return false;
    }

    // rightmost domain label starting with digit indicates IP address
    // since top level domain label can only start with an alpha
    // see RFC 2396 Section 3.2.2
    int index = address.lastIndexOf('.');

    if (address.endsWith("."))
    {
      index = address.substring(0, index).lastIndexOf('.');
    }

    if (index + 1 < addrLength && isDigit(p_address.charAt(index + 1)))
    {
      char testChar;
      int numDots = 0;

      // make sure that 1) we see only digits and dot separators, 2) that
      // any dot separator is preceded and followed by a digit and
      // 3) that we find 3 dots
      for (int i = 0; i < addrLength; i++)
      {
        testChar = address.charAt(i);

        if (testChar == '.')
        {
          if (!isDigit(address.charAt(i - 1))
                  || (i + 1 < addrLength &&!isDigit(address.charAt(i + 1))))
          {
            return false;
          }

          numDots++;
        }
        else if (!isDigit(testChar))
        {
          return false;
        }
      }

      if (numDots != 3)
      {
        return false;
      }
    }
    else
    {

      // domain labels can contain alphanumerics and '-"
      // but must start and end with an alphanumeric
      char testChar;

      for (int i = 0; i < addrLength; i++)
      {
        testChar = address.charAt(i);

        if (testChar == '.')
        {
          if (!isAlphanum(address.charAt(i - 1)))
          {
            return false;
          }

          if (i + 1 < addrLength &&!isAlphanum(address.charAt(i + 1)))
          {
            return false;
          }
        }
        else if (!isAlphanum(testChar) && testChar != '-')
        {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Determine whether a char is a digit.
   *
   *
   * @param p_char the character to check
   * @return true if the char is betweeen '0' and '9', false otherwise
   */
  private static boolean isDigit(char p_char)
  {
    return p_char >= '0' && p_char <= '9';
  }

  /**
   * Determine whether a character is a hexadecimal character.
   *
   *
   * @param p_char the character to check
   * @return true if the char is betweeen '0' and '9', 'a' and 'f'
   *         or 'A' and 'F', false otherwise
   */
  private static boolean isHex(char p_char)
  {
    return (isDigit(p_char) || (p_char >= 'a' && p_char <= 'f')
            || (p_char >= 'A' && p_char <= 'F'));
  }

  /**
   * Determine whether a char is an alphabetic character: a-z or A-Z
   *
   *
   * @param p_char the character to check
   * @return true if the char is alphabetic, false otherwise
   */
  private static boolean isAlpha(char p_char)
  {
    return ((p_char >= 'a' && p_char <= 'z')
            || (p_char >= 'A' && p_char <= 'Z'));
  }

  /**
   * Determine whether a char is an alphanumeric: 0-9, a-z or A-Z
   *
   *
   * @param p_char the character to check
   * @return true if the char is alphanumeric, false otherwise
   */
  private static boolean isAlphanum(char p_char)
  {
    return (isAlpha(p_char) || isDigit(p_char));
  }

  /**
   * Determine whether a character is a reserved character:
   * ';', '/', '?', ':', '@', '&', '=', '+', '$' or ','
   *
   *
   * @param p_char the character to check
   * @return true if the string contains any reserved characters
   */
  private static boolean isReservedCharacter(char p_char)
  {
    return RESERVED_CHARACTERS.indexOf(p_char) != -1;
  }

  /**
   * Determine whether a char is an unreserved character.
   *
   *
   * @param p_char the character to check
   * @return true if the char is unreserved, false otherwise
   */
  private static boolean isUnreservedCharacter(char p_char)
  {
    return (isAlphanum(p_char) || MARK_CHARACTERS.indexOf(p_char) != -1);
  }

  /**
   * Determine whether a given string contains only URI characters (also
   * called "uric" in RFC 2396). uric consist of all reserved
   * characters, unreserved characters and escaped characters.
   *
   *
   * @param p_uric URI string
   * @return true if the string is comprised of uric, false otherwise
   */
  private static boolean isURIString(String p_uric)
  {

    if (p_uric == null)
    {
      return false;
    }

    int end = p_uric.length();
    char testChar = '\0';

    for (int i = 0; i < end; i++)
    {
      testChar = p_uric.charAt(i);

      if (testChar == '%')
      {
        if (i + 2 >= end ||!isHex(p_uric.charAt(i + 1))
                ||!isHex(p_uric.charAt(i + 2)))
        {
          return false;
        }
        else
        {
          i += 2;

          continue;
        }
      }

      if (isReservedCharacter(testChar) || isUnreservedCharacter(testChar))
      {
        continue;
      }
      else
      {
        return false;
      }
    }

    return true;
  }
}
