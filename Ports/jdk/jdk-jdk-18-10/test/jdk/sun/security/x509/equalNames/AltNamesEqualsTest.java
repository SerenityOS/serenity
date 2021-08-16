/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Make sure names that are equal are treated as such.
 * @bug 4273559 8242151
 * @author Yassir Elley
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */

import sun.security.x509.*;
import sun.security.util.ObjectIdentifier;

public class AltNamesEqualsTest{

    private final static String baseDNSName = "bob.example.com";
    private final static String baseDNSNameSame = "BOB.EXAMPLE.COM";
    private final static String baseDNSNameDiff = "fred.example.com";

    private final static String baseRFCName = "bob@example.com";
    private final static String baseRFCNameSame = "BOB@EXAMPLE.COM";
    private final static String baseRFCNameDiff = "fred@example.com";

    private final static String baseURIName = "http://bob.example.com/bob.html";
    private final static String baseURINameSame ="HTTP://BOB.EXAMPLE.COM/bob.html";
    private final static String baseURINameDiff ="http://bob.example.com/BOB.html";

    private final static String baseOIDName = "1.2.3.4";
    private final static String baseOIDNameDiff = "1.2.3.5";

    private final static String baseIPName = "1.2.3.4";
    private final static String baseIPNameDiff = "1.2.3.5";

    private DNSName dnsName, dnsNameSame, dnsNameDiff;
    private RFC822Name rfcName, rfcNameSame, rfcNameDiff;
    private URIName uriName, uriNameSame, uriNameDiff;
    private OIDName oidName, oidNameSame, oidNameDiff;
    private IPAddressName ipName, ipNameSame, ipNameDiff;

    public static void main(String [] args) throws Exception {
        AltNamesEqualsTest test = new AltNamesEqualsTest();
        test.run();
    }

    private void run() throws Exception {
        initializeNames();
        testNames("DNSNames", dnsName, dnsNameSame, dnsNameDiff);
        testNames("RFC822Names", rfcName, rfcNameSame, rfcNameDiff);
        testNames("URINames", uriName, uriNameSame, uriNameDiff);
        testNames("OIDNames", oidName, oidNameSame, oidNameDiff);
        testNames("IPAddressNames", ipName, ipNameSame, ipNameDiff);
    }

    private void initializeNames() throws Exception {
        dnsName = new DNSName(baseDNSName);
        dnsNameSame = new DNSName(baseDNSNameSame);
        dnsNameDiff = new DNSName(baseDNSNameDiff);

        rfcName = new RFC822Name(baseRFCName);
        rfcNameSame = new RFC822Name(baseRFCNameSame);
        rfcNameDiff = new RFC822Name(baseRFCNameDiff);

        uriName = new URIName(baseURIName);
        uriNameSame = new URIName(baseURINameSame);
        uriNameDiff = new URIName(baseURINameDiff);

        oidName = stringToOIDName(baseOIDName);
        oidNameSame = stringToOIDName(baseOIDName);
        oidNameDiff = stringToOIDName(baseOIDNameDiff);

        ipName = stringToIPName(baseIPName);
        ipNameSame = stringToIPName(baseIPName);
        ipNameDiff = stringToIPName(baseIPNameDiff);
    }

    private void testNames(String nameType, GeneralNameInterface name,
                           GeneralNameInterface sameName,
                           GeneralNameInterface diffName)
        throws Exception
    {
        if (!name.equals(sameName)){
            throw new Exception("Equal " + nameType + " are being " +
                                "considered unequal");
        }
        if (name.equals(diffName)){
            throw new Exception("Unequal " + nameType + " are being " +
                                "considered equal");
        }
    }

    private OIDName stringToOIDName(String name)
        throws Exception
    {
        OIDName oidName = null;
        ObjectIdentifier oid = ObjectIdentifier.of(name);
        oidName = new OIDName(oid);
        return oidName;
    }

    private IPAddressName stringToIPName(String name)
        throws Exception
    {
        IPAddressName ipAddr = null;

        //Convert to byte array
        int ch = '.';
        int start = 0;
        int end = 0;
        byte components [];
        int  componentLen;

        // Calculate length of IP address
        componentLen = 0;
        while ((end = name.indexOf(ch,start)) != -1) {
            start = end + 1;
            componentLen += 1;
        }
        componentLen += 1;
        if (componentLen != 4)
            throw new Exception("Invalid IP address: need four components");
        components = new byte[componentLen];

        start = 0;
        int i = 0;
        String comp = null;
        Integer compVal = new Integer(0);
        while ((end = name.indexOf(ch,start)) != -1) {
            comp = name.substring(start,end);
            compVal = Integer.valueOf(comp);
            if (compVal.intValue() < 0 || compVal.intValue() > 255)
                throw new Exception("Invalid IP address: component value " +
                                    "not between 0-255");
            components[i++] = compVal.byteValue();
            start = end + 1;
        }
        comp = name.substring(start);
        components[i] = Integer.valueOf(comp).byteValue();
        ipAddr = new IPAddressName(components);
        return ipAddr;
    }
}
