/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for jdk.internal.icu.text.StringPrep
 * @bug 4737170 8060097 8174270
 * @modules java.base/jdk.internal.icu.text
 *          java.base/sun.net.idn:open
 * @library .
 * @compile -XDignore.symbol.file TestStringPrep.java NFS4StringPrep.java
 *     TestData.java
 * @run main/othervm -ea TestStringPrep
 * @author Edward Wang
 */
/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/

import java.text.ParseException;
import java.io.InputStream;
import java.util.Locale;

import jdk.internal.icu.text.StringPrep;
import jdk.internal.icu.text.UCharacterIterator;

public class TestStringPrep {
    public static void main(String[] args) throws Exception {
        TestNFS4MixedPrep();
        TestCISPrep();
        TestCSPrep();
        TestNamePrepConformance();
    }
    /*
       There are several special identifiers ("who") which need to be
       understood universally, rather than in the context of a particular
       DNS domain.  Some of these identifiers cannot be understood when an
       NFS client accesses the server, but have meaning when a local process
       accesses the file.  The ability to display and modify these
       permissions is permitted over NFS, even if none of the access methods
       on the server understands the identifiers.

        Who                    Description
       _______________________________________________________________

       "OWNER"                The owner of the file.
       "GROUP"                The group associated with the file.
       "EVERYONE"             The world.
       "INTERACTIVE"          Accessed from an interactive terminal.
       "NETWORK"              Accessed via the network.
       "DIALUP"               Accessed as a dialup user to the server.
       "BATCH"                Accessed from a batch job.
       "ANONYMOUS"            Accessed without any authentication.
       "AUTHENTICATED"        Any authenticated user (opposite of
                              ANONYMOUS)
       "SERVICE"              Access from a system service.

       To avoid conflict, these special identifiers are distinguish by an
       appended "@" and should appear in the form "xxxx@" (note: no domain
       name after the "@").  For example: ANONYMOUS@.
    */
    private static String[] mixed_prep_data ={
        "OWNER@",
        "GROUP@",
        "EVERYONE@",
        "INTERACTIVE@",
        "NETWORK@",
        "DIALUP@",
        "BATCH@",
        "ANONYMOUS@",
        "AUTHENTICATED@",
        "\u0930\u094D\u092E\u094D\u0915\u094D\u0937\u0947\u0924\u094D@slip129-37-118-146.nc.us.ibm.net",
        "\u0936\u094d\u0930\u0940\u092e\u0926\u094d@saratoga.pe.utexas.edu",
        "\u092d\u0917\u0935\u0926\u094d\u0917\u0940\u0924\u093e@dial-120-45.ots.utexas.edu",
        "\u0905\u0927\u094d\u092f\u093e\u092f@woo-085.dorms.waller.net",
        "\u0905\u0930\u094d\u091c\u0941\u0928@hd30-049.hil.compuserve.com",
        "\u0935\u093f\u0937\u093e\u0926@pem203-31.pe.ttu.edu",
        "\u092f\u094b\u0917@56K-227.MaxTNT3.pdq.net",
        "\u0927\u0943\u0924\u0930\u093e\u0937\u094d\u091f\u094d\u0930@dial-36-2.ots.utexas.edu",
        "\u0909\u0935\u093E\u091A\u0943@slip129-37-23-152.ga.us.ibm.net",
        "\u0927\u0930\u094d\u092e\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947@ts45ip119.cadvision.com",
        "\u0915\u0941\u0930\u0941\u0915\u094d\u0937\u0947\u0924\u094d\u0930\u0947@sdn-ts-004txaustP05.dialsprint.net",
        "\u0938\u092e\u0935\u0947\u0924\u093e@bar-tnt1s66.erols.com",
        "\u092f\u0941\u092f\u0941\u0924\u094d\u0938\u0935\u0903@101.st-louis-15.mo.dial-access.att.net",
        "\u092e\u093e\u092e\u0915\u093e\u0903@h92-245.Arco.COM",
        "\u092a\u093e\u0923\u094d\u0921\u0935\u093e\u0936\u094d\u091a\u0948\u0935@dial-13-2.ots.utexas.edu",
        "\u0915\u093f\u092e\u0915\u0941\u0930\u094d\u0935\u0924@net-redynet29.datamarkets.com.ar",
        "\u0938\u0902\u091c\u0935@ccs-shiva28.reacciun.net.ve",
        "\u0c30\u0c18\u0c41\u0c30\u0c3e\u0c2e\u0c4d@7.houston-11.tx.dial-access.att.net",
        "\u0c35\u0c3f\u0c36\u0c4d\u0c35\u0c28\u0c3e\u0c27@ingw129-37-120-26.mo.us.ibm.net",
        "\u0c06\u0c28\u0c02\u0c26\u0c4d@dialup6.austintx.com",
        "\u0C35\u0C26\u0C4D\u0C26\u0C3F\u0C30\u0C3E\u0C1C\u0C41@dns2.tpao.gov.tr",
        "\u0c30\u0c3e\u0c1c\u0c40\u0c35\u0c4d@slip129-37-119-194.nc.us.ibm.net",
        "\u0c15\u0c36\u0c30\u0c2c\u0c3e\u0c26@cs7.dillons.co.uk.203.119.193.in-addr.arpa",
        "\u0c38\u0c02\u0c1c\u0c40\u0c35\u0c4d@swprd1.innovplace.saskatoon.sk.ca",
        "\u0c15\u0c36\u0c30\u0c2c\u0c3e\u0c26@bikini.bologna.maraut.it",
        "\u0c38\u0c02\u0c1c\u0c40\u0c2c\u0c4d@node91.subnet159-198-79.baxter.com",
        "\u0c38\u0c46\u0c28\u0c4d\u0c17\u0c41\u0c2a\u0c4d\u0c24@cust19.max5.new-york.ny.ms.uu.net",
        "\u0c05\u0c2e\u0c30\u0c47\u0c02\u0c26\u0c4d\u0c30@balexander.slip.andrew.cmu.edu",
        "\u0c39\u0c28\u0c41\u0c2e\u0c3e\u0c28\u0c41\u0c32@pool029.max2.denver.co.dynip.alter.net",
        "\u0c30\u0c35\u0c3f@cust49.max9.new-york.ny.ms.uu.net",
        "\u0c15\u0c41\u0c2e\u0c3e\u0c30\u0c4d@s61.abq-dialin2.hollyberry.com",
        "\u0c35\u0c3f\u0c36\u0c4d\u0c35\u0c28\u0c3e\u0c27@\u0917\u0928\u0947\u0936.sanjose.ibm.com",
        "\u0c06\u0c26\u0c3f\u0c24\u0c4d\u0c2f@www.\u00E0\u00B3\u00AF.com",
        "\u0C15\u0C02\u0C26\u0C4D\u0C30\u0C47\u0C17\u0C41\u0c32@www.\u00C2\u00A4.com",
        "\u0c36\u0c4d\u0c30\u0c40\u0C27\u0C30\u0C4D@www.\u00C2\u00A3.com",
        "\u0c15\u0c02\u0c1f\u0c2e\u0c36\u0c46\u0c1f\u0c4d\u0c1f\u0c3f@\u0025",
        "\u0c2e\u0c3e\u0c27\u0c35\u0c4d@\u005C\u005C",
        "\u0c26\u0c46\u0c36\u0c46\u0c1f\u0c4d\u0c1f\u0c3f@www.\u0021.com",
        "test@www.\u0024.com",
        "help@\u00C3\u00BC.com",
    };
    public static void TestNFS4MixedPrep(){
        for(int i=0; i< mixed_prep_data.length; i++){
            try{
                String src = mixed_prep_data[i];
                byte[] dest = NFS4StringPrep.mixed_prepare(src.getBytes("UTF-8"));
                String destString = new String(dest, "UTF-8");
                int destIndex = destString.indexOf('@');
                if(destIndex < 0){
                    fail("Delimiter @ disappeared from the output!");
                }
            }catch(Exception e){
                fail("mixed_prepare for string: " + mixed_prep_data[i] +" failed with " + e.toString());
            }
        }
        /* test the error condition */
        {
            String src = "OWNER@oss.software.ibm.com";
            try{
                byte[] dest = NFS4StringPrep.mixed_prepare(src.getBytes("UTF-8"));
                if(dest!=null){
                    fail("Did not get the expected exception");
                }
            }catch(Exception e){
                System.out.println("mixed_prepare for string: " + src +" passed with " + e.toString());
            }

         }
    }
    public static void TestCISPrep(){

        for(int i=0;i< (TestData.conformanceTestCases.length);i++){
            TestData.ConformanceTestCase testCase = TestData.conformanceTestCases[i];
            String src = testCase.input;
            Exception expected = testCase.expected;
            String expectedDest = testCase.output;
            try{
                byte[] dest =NFS4StringPrep.cis_prepare(src.getBytes("UTF-8"));
                String destString = new String(dest, "UTF-8");
                if(!expectedDest.equalsIgnoreCase(destString)){
                      fail("Did not get the expected output for nfs4_cis_prep at index " + i);
                }
            }catch(Exception e){
                if(!((expected instanceof ParseException) && (e instanceof ParseException))) {
                    fail("Did not get the expected exception");
                }
            }

        }
    }

    public static void TestCSPrep(){

        // Checking for bidi is turned off
        String src = "\uC138\uACC4\uC758\uBAA8\uB4E0\uC0AC\uB78C\uB4E4\uC774\u0644\u064A\u0647\uD55C\uAD6D\uC5B4\uB97C\uC774\uD574\uD55C\uB2E4\uBA74";
        try{
            NFS4StringPrep.cs_prepare(src.getBytes("UTF-8"), false);
        }catch(Exception e){
            fail("Got unexpected exception: " + e.toString());
        }

        // normalization is turned off
        try{
            src = "www.\u00E0\u00B3\u00AF.com";
            byte[] dest = NFS4StringPrep.cs_prepare(src.getBytes("UTF-8"), false);
            String destStr = new String(dest, "UTF-8");
            if(!src.equals(destStr)){
                fail("Did not get expected output. Expected: "+ prettify(src)+
                      " Got: " + prettify(destStr));
            }
        }catch(Exception e){
            fail("Got unexpected exception: " + e.toString());
        }

        // test case insensitive string
        try{
            src = "THISISATEST";
            byte[] dest = NFS4StringPrep.cs_prepare(src.getBytes("UTF-8"), false);
            String destStr = new String(dest, "UTF-8");
            if(!src.toLowerCase(Locale.ROOT).equals(destStr)){
                fail("Did not get expected output. Expected: "+ prettify(src)+
                      " Got: " + prettify(destStr));
            }
        }catch(Exception e){
            fail("Got unexpected exception: " + e.toString());
        }
        // test case sensitive string
        try{
            src = "THISISATEST";
            byte[] dest = NFS4StringPrep.cs_prepare(src.getBytes("UTF-8"), true);
            String destStr = new String(dest, "UTF-8");
            if(!src.equals(destStr)){
                fail("Did not get expected output. Expected: "+ prettify(src)+
                      " Got: " + prettify(destStr));
            }
        }catch(Exception e){
            fail("Got unexpected exception: " + e.toString());
        }
    }

    public static void TestNamePrepConformance() throws Exception {
        InputStream stream = StringPrep.class.getModule()
                                             .getResourceAsStream("sun/net/idn/uidna.spp");
        StringPrep namePrep = new StringPrep(stream);
        stream.close();
        int i;
        for(i=0; i<TestData.conformanceTestCases.length;i++){
            TestData.ConformanceTestCase testCase = TestData.conformanceTestCases[i];
            try{
                UCharacterIterator iter = UCharacterIterator.getInstance(testCase.input);
                StringBuffer output = namePrep.prepare(iter, testCase.flags);
                if(testCase.output !=null && output!=null && !testCase.output.equals(output.toString())){
                    fail("Did not get the expected output. Expected: " + prettify(testCase.output)+
                            " Got: "+ prettify(output.toString()) );
                }
            } catch(ParseException ex) {
                if (testCase.expected == null) {
                    fail("get the unexpected exception for source: " +testCase.input +" Got:  "+ ex.toString());
                }
            }
        }
        System.out.println("Nameprep test count: " + i);
    }

    private static void fail(String msg) {
        throw new RuntimeException(msg);
    }

    private static String prettify(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char ch =s.charAt(i);
            if(ch > 0x7f){
                result.append("\\u");
                result.append(hex(ch));
            }else{
                result.append(ch);
            }

        }
        return result.toString();
    }

    private static String hex(char ch) {
        StringBuffer result = new StringBuffer();
        String foo = Integer.toString(ch,16).toUpperCase(Locale.ROOT);
        for (int i = foo.length(); i < 4; ++i) {
            result.append('0');
        }
        return result + foo;
    }
}
