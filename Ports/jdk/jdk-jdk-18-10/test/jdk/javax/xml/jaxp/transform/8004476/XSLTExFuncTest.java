/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @test
 * @bug 8004476
 * @summary test XSLT extension functions
 * @run main/othervm -Djava.security.manager=allow XSLTExFuncTest
 */

import java.io.StringWriter;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

/**
 * test XSLT extension functions
 *
 * @author huizhe.wang@oracle.com
 */
public class XSLTExFuncTest extends TestBase {

    final static String ENABLE_EXTENSION_FUNCTIONS = "http://www.oracle.com/xml/jaxp/properties/enableExtensionFunctions";
    final static String CLASSNAME = "DocumentBuilderFactoryImpl";

    /**
     * Creates a new instance of StreamReader
     */
    public XSLTExFuncTest(String name) {
        super(name);
    }
    boolean hasSM;
    String xslFile, xslFileId;
    String xmlFile, xmlFileId;

    protected void setUp() {
        super.setUp();
        xmlFile = filepath + "/tokenize.xml";
        xslFile = filepath + "/tokenize.xsl";

        /**
         * On Windows platform it needs triple '/' for valid URL while double '/' is enough on Linux or Solaris.
         * Here use file:/// directly to make it work on Windows and it will not impact other platforms.
         */
        xslFileId = "file:///" + xslFile;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        XSLTExFuncTest test = new XSLTExFuncTest("OneTest");
        test.setUp();

        test.testExtFunc();
        test.testExtFuncNotAllowed();
        test.testEnableExtFunc();
        test.testTemplatesEnableExtFunc();
        test.tearDown();

    }

    /**
     * by default, extension function is enabled
     */
    public void testExtFunc() {
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            transform(factory);
            System.out.println("testExtFunc: OK");
        } catch (TransformerConfigurationException e) {
            fail(e.getMessage());
        } catch (TransformerException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Security is enabled, extension function not allowed
     */
    public void testExtFuncNotAllowed() {
        Policy p = new SimplePolicy(new AllPermission());
        Policy.setPolicy(p);
        System.setSecurityManager(new SecurityManager());
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            transform(factory);
        } catch (TransformerConfigurationException e) {
            fail(e.getMessage());
        } catch (TransformerException ex) {
            //expected since extension function is disallowed
            System.out.println("testExtFuncNotAllowed: OK");
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * Security is enabled, use new feature: enableExtensionFunctions
     */
    public void testEnableExtFunc() {
        Policy p = new SimplePolicy(new AllPermission());
        Policy.setPolicy(p);
        System.setSecurityManager(new SecurityManager());
        TransformerFactory factory = TransformerFactory.newInstance();

        /**
         * Use of the extension function 'http://exslt.org/strings:tokenize' is
         * not allowed when the secure processing feature is set to true.
         * Attempt to use the new property to enable extension function
         */
        boolean isExtensionSupported = enableExtensionFunction(factory);

        try {
            transform(factory);
            System.out.println("testEnableExt: OK");
        } catch (TransformerConfigurationException e) {
            fail(e.getMessage());
        } catch (TransformerException e) {
            fail(e.getMessage());
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * use Templates template = factory.newTemplates(new StreamSource( new
     * FileInputStream(xslFilename))); // Use the template to create a
     * transformer Transformer xformer = template.newTransformer();
     *
     * @param factory
     * @return
     */
    /**
     * Security is enabled, use new feature: enableExtensionFunctions Use the
     * template to create a transformer
     */
    public void testTemplatesEnableExtFunc() {
        Policy p = new SimplePolicy(new AllPermission());
        Policy.setPolicy(p);
        System.setSecurityManager(new SecurityManager());
        TransformerFactory factory = TransformerFactory.newInstance();

        /**
         * Use of the extension function 'http://exslt.org/strings:tokenize' is
         * not allowed when the secure processing feature is set to true.
         * Attempt to use the new property to enable extension function
         */
        boolean isExtensionSupported = enableExtensionFunction(factory);

        try {
            SAXSource xslSource = new SAXSource(new InputSource(xslFile));
            xslSource.setSystemId(xslFileId);
            Templates template = factory.newTemplates(xslSource);
            Transformer transformer = template.newTransformer();
            StringWriter stringResult = new StringWriter();
            Result result = new StreamResult(stringResult);
            transformer.transform(new SAXSource(new InputSource(xmlFile)), result);
            System.out.println("testTemplatesEnableExtFunc: OK");
        } catch (TransformerConfigurationException e) {
            fail(e.getMessage());
        } catch (TransformerException e) {
            fail(e.getMessage());
        } finally {
            System.setSecurityManager(null);
        }
    }

    boolean enableExtensionFunction(TransformerFactory factory) {
        boolean isSupported = true;
        try {
            factory.setFeature(ENABLE_EXTENSION_FUNCTIONS, true);
        } catch (TransformerConfigurationException ex) {
            isSupported = false;
        }
        return isSupported;
    }

    void transform(TransformerFactory factory) throws TransformerConfigurationException, TransformerException {
        SAXSource xslSource = new SAXSource(new InputSource(xslFile));
        xslSource.setSystemId(xslFileId);
        Transformer transformer = factory.newTransformer(xslSource);
        StringWriter stringResult = new StringWriter();
        Result result = new StreamResult(stringResult);
        transformer.transform(new SAXSource(new InputSource(xmlFile)), result);
    }

    class SimplePolicy extends Policy {

        private final Permissions perms;

        public SimplePolicy(Permission... permissions) {
            perms = new Permissions();
            for (Permission permission : permissions) {
                perms.add(permission);
            }
        }

        @Override
        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }

        @Override
        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p);
        }

        //for older jdk
        @Override
        public void refresh() {
        }
    }
}
