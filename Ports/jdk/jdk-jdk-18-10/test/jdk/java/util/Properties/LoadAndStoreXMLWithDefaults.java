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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * @test
 * @bug 8016344
 * @summary checks that Properties.storeToXML only stores properties locally
 *          defined on the Properties object, excluding those that are inherited.
 * @author danielfuchs
 */
public class LoadAndStoreXMLWithDefaults {

    static String writeToXML(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.storeToXML(baos, "Test 8016344");
        return baos.toString();
    }

    static Properties loadFromXML(String xml, Properties defaults) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Properties props = new Properties(defaults);
        props.loadFromXML(bais);
        return props;
    }

    static enum Objects { OBJ1, OBJ2, OBJ3 };

    public static void main(String[] args) throws IOException {
        Properties p1 = new Properties();
        p1.setProperty("p1.prop", "prop1-p1");
        p1.setProperty("p1.and.p2.prop", "prop2-p1");
        p1.setProperty("p1.and.p2.and.p3.prop", "prop3-p1");
        Properties p2 = new Properties(p1);
        p2.setProperty("p2.prop", "prop4-p2");
        p2.setProperty("p1.and.p2.prop", "prop5-p2");
        p2.setProperty("p1.and.p2.and.p3.prop", "prop6-p2");
        p2.setProperty("p2.and.p3.prop", "prop7-p2");
        Properties p3 = new Properties(p2);
        p3.setProperty("p3.prop", "prop8-p3");
        p3.setProperty("p1.and.p2.and.p3.prop", "prop9-p3");
        p3.setProperty("p2.and.p3.prop", "prop10-p3");

        Properties P1 = loadFromXML(writeToXML(p1), null);
        Properties P2 = loadFromXML(writeToXML(p2), P1);
        Properties P3 = loadFromXML(writeToXML(p3), P2);

        testResults(p1, P1, p2, P2, p3, P3);
    }

    public static void testResults(Properties... pps) {
        for (int i=0 ; i < pps.length ; i += 2) {
            if (!pps[i].equals(pps[i+1])) {
                System.err.println("P" + (i/2+1)
                        + " Reloaded properties differ from original");
                System.err.println("\toriginal: " + pps[i]);
                System.err.println("\treloaded: " + pps[i+1]);
                throw new RuntimeException("P" + (i/2+1)
                        + " Reloaded properties differ from original");
            }
            if (!pps[i].keySet().equals(pps[i+1].keySet())) {
                System.err.println("P" + (i/2+1)
                        + " Reloaded property names differ from original");
                System.err.println("\toriginal: " + pps[i].keySet());
                System.err.println("\treloaded: " + pps[i+1].keySet());
                throw new RuntimeException("P" + (i/2+1)
                        + " Reloaded property names differ from original");
            }
            if (!pps[i].stringPropertyNames().equals(pps[i+1].stringPropertyNames())) {
                System.err.println("P" + (i/2+1)
                        + " Reloaded string property names differ from original");
                System.err.println("\toriginal: " + pps[i].stringPropertyNames());
                System.err.println("\treloaded: " + pps[i+1].stringPropertyNames());
                throw new RuntimeException("P" + (i/2+1)
                        + " Reloaded string property names differ from original");
            }
        }
    }

}
