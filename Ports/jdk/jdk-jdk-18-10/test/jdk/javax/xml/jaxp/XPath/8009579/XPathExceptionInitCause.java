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

/*
 * @test
 * @bug 8009579
 * @summary The initCause() incorrectly initialise the cause in
 * XPathException class when used with XPathException(String)
 * constructor.
 * @run main XPathExceptionInitCause
 * @author aleksej.efimov@oracle.com
 */

import javax.xml.xpath.XPathException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InvalidClassException;


public class XPathExceptionInitCause {

    /* This is a serial form of XPathException with two causes serialized
     * by JDK7 code:
     *
     *  ByteArrayOutputStream fser = new ByteArrayOutputStream();
     *  ObjectOutputStream oos = new ObjectOutputStream(fser);
     *  oos.writeObject(new XPathException(new Exception()).initCause(null));
     *  oos.close();
     */
    static final byte [] TWOCAUSES = {-84,-19,0,5,115,114,0,30,106,97,118,97,120,46,120,
        109,108,46,120,112,97,116,104,46,88,80,97,116,104,69,120,99,101,112,116,
        105,111,110,-26,-127,97,60,-120,119,127,28,2,0,1,76,0,5,99,97,117,115,101,
        116,0,21,76,106,97,118,97,47,108,97,110,103,47,84,104,114,111,119,97,98,
        108,101,59,120,114,0,19,106,97,118,97,46,108,97,110,103,46,69,120,99,101,
        112,116,105,111,110,-48,-3,31,62,26,59,28,-60,2,0,0,120,114,0,19,106,97,
        118,97,46,108,97,110,103,46,84,104,114,111,119,97,98,108,101,-43,-58,53,
        39,57,119,-72,-53,3,0,4,76,0,5,99,97,117,115,101,113,0,126,0,1,76,0,13,
        100,101,116,97,105,108,77,101,115,115,97,103,101,116,0,18,76,106,97,118,
        97,47,108,97,110,103,47,83,116,114,105,110,103,59,91,0,10,115,116,97,99,
        107,84,114,97,99,101,116,0,30,91,76,106,97,118,97,47,108,97,110,103,47,83,
        116,97,99,107,84,114,97,99,101,69,108,101,109,101,110,116,59,76,0,20,115,
        117,112,112,114,101,115,115,101,100,69,120,99,101,112,116,105,111,110,115,
        116,0,16,76,106,97,118,97,47,117,116,105,108,47,76,105,115,116,59,120,112,
        112,112,117,114,0,30,91,76,106,97,118,97,46,108,97,110,103,46,83,116,97,99,
        107,84,114,97,99,101,69,108,101,109,101,110,116,59,2,70,42,60,60,-3,34,57,
        2,0,0,120,112,0,0,0,1,115,114,0,27,106,97,118,97,46,108,97,110,103,46,83,
        116,97,99,107,84,114,97,99,101,69,108,101,109,101,110,116,97,9,-59,-102,
        38,54,-35,-123,2,0,4,73,0,10,108,105,110,101,78,117,109,98,101,114,76,0,
        14,100,101,99,108,97,114,105,110,103,67,108,97,115,115,113,0,126,0,4,76,
        0,8,102,105,108,101,78,97,109,101,113,0,126,0,4,76,0,10,109,101,116,104,
        111,100,78,97,109,101,113,0,126,0,4,120,112,0,0,0,31,116,0,23,88,80,97,116,
        104,69,120,99,101,112,116,105,111,110,83,101,114,105,97,108,105,122,101,
        116,0,28,88,80,97,116,104,69,120,99,101,112,116,105,111,110,83,101,114,105,
        97,108,105,122,101,46,106,97,118,97,116,0,4,109,97,105,110,115,114,0,38,
        106,97,118,97,46,117,116,105,108,46,67,111,108,108,101,99,116,105,111,110,
        115,36,85,110,109,111,100,105,102,105,97,98,108,101,76,105,115,116,-4,15,
        37,49,-75,-20,-114,16,2,0,1,76,0,4,108,105,115,116,113,0,126,0,6,120,114,
        0,44,106,97,118,97,46,117,116,105,108,46,67,111,108,108,101,99,116,105,111,
        110,115,36,85,110,109,111,100,105,102,105,97,98,108,101,67,111,108,108,101,
        99,116,105,111,110,25,66,0,-128,-53,94,-9,30,2,0,1,76,0,1,99,116,0,22,76,
        106,97,118,97,47,117,116,105,108,47,67,111,108,108,101,99,116,105,111,110,
        59,120,112,115,114,0,19,106,97,118,97,46,117,116,105,108,46,65,114,114,97,
        121,76,105,115,116,120,-127,-46,29,-103,-57,97,-99,3,0,1,73,0,4,115,105,
        122,101,120,112,0,0,0,0,119,4,0,0,0,0,120,113,0,126,0,20,120,115,113,0,126,
        0,2,113,0,126,0,21,112,117,113,0,126,0,8,0,0,0,1,115,113,0,126,0,10,0,0,0,
        31,113,0,126,0,12,113,0,126,0,13,113,0,126,0,14,113,0,126,0,18,120
    };

    /* This is a serial form of ordinary XPathException serialized by JDK7 code:
     *
     *  Throwable cause = new Throwable( "message 1" );
     *  XPathException xpathexcep = new XPathException( "message 2" );
     *  xpathexcep.initCause( cause );
     *  ByteArrayOutputStream fser = new ByteArrayOutputStream();
     *  ObjectOutputStream oos = new ObjectOutputStream(fser);
     *  oos.writeObject(xpathexcep);
     *  oos.close();
     */
    static final byte [] NORMALJDK7SER = {-84,-19,0,5,115,114,0,30,106,97,118,97,120,
        46,120,109,108,46,120,112,97,116,104,46,88,80,97,116,104,69,120,99,101,112,
        116,105,111,110,-26,-127,97,60,-120,119,127,28,2,0,1,76,0,5,99,97,117,115,
        101,116,0,21,76,106,97,118,97,47,108,97,110,103,47,84,104,114,111,119,97,
        98,108,101,59,120,114,0,19,106,97,118,97,46,108,97,110,103,46,69,120,99,
        101,112,116,105,111,110,-48,-3,31,62,26,59,28,-60,2,0,0,120,114,0,19,106,
        97,118,97,46,108,97,110,103,46,84,104,114,111,119,97,98,108,101,-43,-58,
        53,39,57,119,-72,-53,3,0,4,76,0,5,99,97,117,115,101,113,0,126,0,1,76,0,13,
        100,101,116,97,105,108,77,101,115,115,97,103,101,116,0,18,76,106,97,118,
        97,47,108,97,110,103,47,83,116,114,105,110,103,59,91,0,10,115,116,97,99,
        107,84,114,97,99,101,116,0,30,91,76,106,97,118,97,47,108,97,110,103,47,83,
        116,97,99,107,84,114,97,99,101,69,108,101,109,101,110,116,59,76,0,20,115,
        117,112,112,114,101,115,115,101,100,69,120,99,101,112,116,105,111,110,115,
        116,0,16,76,106,97,118,97,47,117,116,105,108,47,76,105,115,116,59,120,112,
        115,113,0,126,0,3,113,0,126,0,8,116,0,9,109,101,115,115,97,103,101,32,49,
        117,114,0,30,91,76,106,97,118,97,46,108,97,110,103,46,83,116,97,99,107,84,
        114,97,99,101,69,108,101,109,101,110,116,59,2,70,42,60,60,-3,34,57,2,0,0,
        120,112,0,0,0,1,115,114,0,27,106,97,118,97,46,108,97,110,103,46,83,116,97,
        99,107,84,114,97,99,101,69,108,101,109,101,110,116,97,9,-59,-102,38,54,-35,
        -123,2,0,4,73,0,10,108,105,110,101,78,117,109,98,101,114,76,0,14,100,101,
        99,108,97,114,105,110,103,67,108,97,115,115,113,0,126,0,4,76,0,8,102,105,
        108,101,78,97,109,101,113,0,126,0,4,76,0,10,109,101,116,104,111,100,78,97,
        109,101,113,0,126,0,4,120,112,0,0,0,19,116,0,23,88,80,97,116,104,69,120,
        99,101,112,116,105,111,110,83,101,114,105,97,108,105,122,101,116,0,28,88,
        80,97,116,104,69,120,99,101,112,116,105,111,110,83,101,114,105,97,108,105,
        122,101,46,106,97,118,97,116,0,4,109,97,105,110,115,114,0,38,106,97,118,
        97,46,117,116,105,108,46,67,111,108,108,101,99,116,105,111,110,115,36,85,
        110,109,111,100,105,102,105,97,98,108,101,76,105,115,116,-4,15,37,49,-75,
        -20,-114,16,2,0,1,76,0,4,108,105,115,116,113,0,126,0,6,120,114,0,44,106,
        97,118,97,46,117,116,105,108,46,67,111,108,108,101,99,116,105,111,110,115,
        36,85,110,109,111,100,105,102,105,97,98,108,101,67,111,108,108,101,99,116,
        105,111,110,25,66,0,-128,-53,94,-9,30,2,0,1,76,0,1,99,116,0,22,76,106,97,
        118,97,47,117,116,105,108,47,67,111,108,108,101,99,116,105,111,110,59,120,
        112,115,114,0,19,106,97,118,97,46,117,116,105,108,46,65,114,114,97,121,76,
        105,115,116,120,-127,-46,29,-103,-57,97,-99,3,0,1,73,0,4,115,105,122,101,
        120,112,0,0,0,0,119,4,0,0,0,0,120,113,0,126,0,22,120,116,0,9,109,101,115,
        115,97,103,101,32,50,117,113,0,126,0,10,0,0,0,1,115,113,0,126,0,12,0,0,0,
        20,113,0,126,0,14,113,0,126,0,15,113,0,126,0,16,113,0,126,0,20,120,112
    };

    //Serialize XPathException
    static byte [] pickleXPE(XPathException xpe) throws IOException {
        ByteArrayOutputStream bos =  new ByteArrayOutputStream();
        ObjectOutputStream xpeos = new ObjectOutputStream(bos);
        xpeos.writeObject(xpe);
        xpeos.close();
        return bos.toByteArray();
    }

    //Deserialize XPathException with byte array as serial data source
    static XPathException unpickleXPE(byte [] ser)
            throws IOException, ClassNotFoundException {
        XPathException xpe;
        ByteArrayInputStream bis = new ByteArrayInputStream(ser);
        ObjectInputStream xpeis = new ObjectInputStream(bis);
        xpe = (XPathException) xpeis.readObject();
        xpeis.close();
        return xpe;
    }

    public static void main(String[] args) throws Exception {
        Throwable cause = new Throwable("message 1");
        XPathException xpathexcep = new XPathException("message 2");

        //Test XPE initCause() method
        xpathexcep.initCause(cause);
        System.out.println("getCause() result: '" + xpathexcep.getCause()
                + "' Cause itself: '" + cause + "'");
        if (!xpathexcep.getCause().toString().equals(cause.toString())) {
            throw new Exception("Incorrect cause is set by initCause()");
        }

        //Test serialization/deserialization of initialized XPE
        byte [] xpeserial;
        XPathException xpedeser;
        xpeserial = pickleXPE(xpathexcep);
        xpedeser = unpickleXPE(xpeserial);
        System.out.println("Serialized XPE: message='" + xpathexcep.getMessage()
                + "' cause='" + xpathexcep.getCause().toString() + "'");
        System.out.println("Deserialized XPE: message='" + xpedeser.getMessage()
                + "' cause='" + xpedeser.getCause().toString()+"'");
        if(xpedeser.getCause() == null ||
                !xpedeser.getCause().toString().equals(cause.toString()) ||
                !xpedeser.getMessage().toString().equals("message 2") )
            throw new Exception("XPathException incorrectly serialized/deserialized");

        //Test serialization/deserialization of uninitialized cause in XPE
        XPathException xpeuninit = new XPathException("uninitialized cause");
        xpeserial = pickleXPE(xpeuninit);
        xpedeser = unpickleXPE(xpeserial);
        System.out.println("Serialized XPE: message='" + xpeuninit.getMessage()
                + "' cause='" + xpeuninit.getCause()+"'");
        System.out.println("Deserialized XPE: message='" + xpedeser.getMessage()
                + "' cause='" + xpedeser.getCause()+"'");
        if(xpedeser.getCause() != null ||
                !xpedeser.getMessage().toString().equals("uninitialized cause") )
            throw new Exception("XPathException incorrectly serialized/deserialized");

        //Test deserialization of normal XPathException serialized by JDK7
        XPathException xpejdk7 = unpickleXPE(NORMALJDK7SER);
        if(xpejdk7 == null || xpejdk7.getCause() == null ||
                !xpejdk7.getMessage().equals("message 2") ||
                !xpejdk7.getCause().getMessage().equals("message 1"))
            throw new Exception("XpathException serialized by JDK7 was "
                    + "incorrectly deserialized.");

        //Test deserialization of XPathException with two causes from JDK7.
        //The serialization are done for the following XPathException object:
        // new XPathException(new Exception()).initCause(null)
        try {
            xpejdk7 = unpickleXPE(TWOCAUSES);
            throw new Exception("Expected InvalidClassException but it wasn't"
                    + " observed");
        } catch(InvalidClassException e) {
            System.out.println("InvalidClassException caught as expected.");
        }

    }
}
