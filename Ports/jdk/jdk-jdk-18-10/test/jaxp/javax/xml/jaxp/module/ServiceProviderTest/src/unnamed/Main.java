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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.lang.module.ModuleDescriptor.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.xml.sax.helpers.XMLReaderFactory;

public class Main {
    /*
     * @param args, the names of provider modules, which have been loaded
     */
    public static void main(String[] args) throws Exception {
        Module xml = ModuleLayer.boot().findModule("java.xml").get();

        Set<String> allServices = new HashSet<>(Arrays.asList(expectedAllServices));
        if (!allServices.equals(xml.getDescriptor().uses()))
            throw new AssertionError("Expect xml module uses: " + allServices + " But actually uses: "
                    + xml.getDescriptor().uses());

        long violationCount = Stream.of(args)
                .map(xmlProviderName -> ModuleLayer.boot().findModule(xmlProviderName).get())
                .mapToLong(
                        // services provided by the implementation in provider module
                        provider -> provider.getDescriptor().provides().stream()
                                .map(Provides::service)
                                .filter(serviceName -> {
                                    allServices.remove(serviceName); // remove service provided by
                                                                     // customized module from allServices
                                    return !belongToModule(serviceName, instantiateXMLService(serviceName), provider);
                                }).count())
                .sum();

        // the remaining services should be provided by the default implementation
        violationCount += allServices.stream()
                .filter(serviceName -> !belongToModule(serviceName, instantiateXMLService(serviceName), xml))
                .count();

        if (violationCount > 0)
            throw new AssertionError(violationCount + " services are not provided by expected module");
    }

    /*
     * instantiate a xml factory by reflection e.g.
     * DocumentBuilderFactory.newInstance()
     */
    private static Object instantiateXMLService(String serviceName) {
        try {
            if (serviceName.equals("org.xml.sax.XMLReader"))
                return XMLReaderFactory.createXMLReader();
            else if (serviceName.equals("javax.xml.validation.SchemaFactory"))
                return Class.forName(serviceName).getMethod("newInstance", String.class)
                        .invoke(null, W3C_XML_SCHEMA_NS_URI);
            else
                return Class.forName(serviceName).getMethod("newInstance").invoke(null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    /*
     * verify which module provides the xml factory
     */
    private static boolean belongToModule(String factoryName, Object factory, Module expected) {
        Module actual = factory.getClass().getModule();
        if (!actual.equals(expected)) {
            System.err.println("Expect " + factoryName + " is provided by " + expected
                    + ", but actual implementation " + factory.getClass() + " is provided by " + actual);
            return false;
        } else {
            System.out.println(factory.getClass() + " is provided by " + expected);
            return true;
        }
    }

    /*
     * This list equals the declarations in java.xml module-info.java
     */
    private static final String[] expectedAllServices = { "javax.xml.datatype.DatatypeFactory",
            "javax.xml.parsers.DocumentBuilderFactory", "javax.xml.parsers.SAXParserFactory",
            "javax.xml.stream.XMLEventFactory", "javax.xml.stream.XMLInputFactory",
            "javax.xml.stream.XMLOutputFactory", "javax.xml.transform.TransformerFactory",
            "javax.xml.validation.SchemaFactory", "javax.xml.xpath.XPathFactory",
            "org.xml.sax.XMLReader"};

}
