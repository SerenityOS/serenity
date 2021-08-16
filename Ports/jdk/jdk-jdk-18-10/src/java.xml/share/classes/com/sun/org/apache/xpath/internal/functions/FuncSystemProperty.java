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

package com.sun.org.apache.xpath.internal.functions;

import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.objects.XString;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Properties;
import jdk.xml.internal.SecuritySupport;

/**
 * Execute the SystemProperty() function.
 * @xsl.usage advanced
 * @LastModified: Sep 2017
 */
public class FuncSystemProperty extends FunctionOneArg
{
    static final long serialVersionUID = 3694874980992204867L;
  /**
   * The path/filename of the property file: XSLTInfo.properties
   * Maintenance note: see also
   * com.sun.org.apache.xalan.internal.processor.TransformerFactoryImpl.XSLT_PROPERTIES
   */
  static final String XSLT_PROPERTIES =
            "com/sun/org/apache/xalan/internal/res/XSLTInfo.properties";

  /**
   * Execute the function.  The function must return
   * a valid object.
   * @param xctxt The current execution context.
   * @return A valid XObject.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {

    String fullName = m_arg0.execute(xctxt).str();
    int indexOfNSSep = fullName.indexOf(':');
    String result;
    String propName = "";

    // List of properties where the name of the
    // property argument is to be looked for.
    Properties xsltInfo = new Properties();

    loadPropertyFile(xsltInfo);

    if (indexOfNSSep > 0)
    {
      String prefix = (indexOfNSSep >= 0)
                      ? fullName.substring(0, indexOfNSSep) : "";
      String namespace;

      namespace = xctxt.getNamespaceContext().getNamespaceForPrefix(prefix);
      propName = (indexOfNSSep < 0)
                 ? fullName : fullName.substring(indexOfNSSep + 1);

      if (namespace.startsWith("http://www.w3.org/XSL/Transform")
              || namespace.equals("http://www.w3.org/1999/XSL/Transform"))
      {
        result = xsltInfo.getProperty(propName);

        if (null == result)
        {
          warn(xctxt, XPATHErrorResources.WG_PROPERTY_NOT_SUPPORTED,
               new Object[]{ fullName });  //"XSL Property not supported: "+fullName);

          return XString.EMPTYSTRING;
        }
      }
      else
      {
        warn(xctxt, XPATHErrorResources.WG_DONT_DO_ANYTHING_WITH_NS,
             new Object[]{ namespace,
                           fullName });  //"Don't currently do anything with namespace "+namespace+" in property: "+fullName);

        try
        {
          result = SecuritySupport.getSystemProperty(propName);

          if (null == result)
          {

            // result = System.getenv(propName);
            return XString.EMPTYSTRING;
          }
        }
        catch (SecurityException se)
        {
          warn(xctxt, XPATHErrorResources.WG_SECURITY_EXCEPTION,
               new Object[]{ fullName });  //"SecurityException when trying to access XSL system property: "+fullName);

          return XString.EMPTYSTRING;
        }
      }
    }
    else
    {
      try
      {
        result = SecuritySupport.getSystemProperty(fullName);

        if (null == result)
        {

          // result = System.getenv(fullName);
          return XString.EMPTYSTRING;
        }
      }
      catch (SecurityException se)
      {
        warn(xctxt, XPATHErrorResources.WG_SECURITY_EXCEPTION,
             new Object[]{ fullName });  //"SecurityException when trying to access XSL system property: "+fullName);

        return XString.EMPTYSTRING;
      }
    }

    if (propName.equals("version") && result.length() > 0)
    {
      try
      {
        // Needs to return the version number of the spec we conform to.
        return new XString("1.0");
      }
      catch (Exception ex)
      {
        return new XString(result);
      }
    }
    else
      return new XString(result);
  }

  /**
   * Retrieve a property bundle from XSLT_PROPERTIES
   *
   * @param target The target property bag the file will be placed into.
   */
  private void loadPropertyFile(Properties target)
  {
    try
    {
      // Use SecuritySupport class to provide privileged access to property file
      InputStream is = SecuritySupport.getResourceAsStream(XSLT_PROPERTIES);

      // get a buffered version
      try (BufferedInputStream bis = new BufferedInputStream(is)) {
          target.load(bis);  // and load up the property bag from this
      }
    }
    catch (Exception ex)
    {
      // ex.printStackTrace();
      throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(ex);
    }
  }
}
