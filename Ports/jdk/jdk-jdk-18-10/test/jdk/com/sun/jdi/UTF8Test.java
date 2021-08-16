/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5033550
 * @summary  JDWP back end uses modified UTF-8
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g UTF8Test.java
 * @run driver UTF8Test
 */

/*
  There is UTF-8 and there is modified UTF-8, which I will call M-UTF-8.
  The two differ in the representation of binary 0, and
  in some other more esoteric representations.
  See
      http://java.sun.com/developer/technicalArticles/Intl/Supplementary/#Modified_UTF-8
      http://java.sun.com/javase/6/docs/technotes/guides/jni/spec/types.html#wp16542

  All the following are observations of the treatment
  of binary 0.  In UTF-8, this represented as one byte:
      0x00

  while in modified UTF-8, it is represented as two bytes
      0xc0 0x80

  ** I haven't investigated if the other differences between UTF-8 and
     M-UTF-8 are handled in the same way.

 Here is how these our handled in our BE, JDWP, and FE:

 - Strings in .class files are M-UTF-8.

 - To get the value of a string object from the VM, our BE calls
      char * utf = JNI_FUNC_PTR(env,GetStringUTFChars)(env, string, NULL);
   which returns M-UTF-8.

- To create a string object in the VM, our BE VirtualMachine.createString() calls
      string = JNI_FUNC_PTR(env,NewStringUTF)(env, cstring);
      This function expects the string to be M-UTF-8
      BUG:  If the string came from JDWP, then it is actually UTF-8

- I haven't investigated strings in JVMTI.

- The JDWP spec says that strings are UTF-8.  The intro
  says this for all strings, and the createString command and
  the StringRefernce.value command say it explicitly.

- Our FE java writes strings to JDWP as UTF-8.

- BE function outStream_writeString uses strlen meaning
  it expects no 0 bytes, meaning that it expects M-UTF-8
  This function writes the byte length and then calls
  outStream.c::writeBytes which just writes the bytes to JDWP as is.

  BUG: If such a string came from the VM via JNI, it is actually
       M-UTF-8
  FIX:  - scan string to see if contains an M-UTF-8 char.
          if yes,
             - call String(bytes, 0, len, "UTF8")
               to get a java string.  Will this work -ie, the
               input is M-UTF-8 instead of real UTF-8
             - call some java method (NOT JNI which
               would just come back with M-UTF-8)
               on the String to get real UTF-8


- The JDWP StringReference.value command does reads a string
  from the BE out of the JDWP stream and does this to
  createe a Java String for it (see PacketStream.readString):
         String readString() {
          String ret;
          int len = readInt();

          try {
              ret = new String(pkt.data, inCursor, len, "UTF8");
          } catch(java.io.UnsupportedEncodingException e) {

  This String ctor converts _both- the M-UTF-8 0xc0 0x80
  and UTF-8 0x00  into a Java char containing 0x0000

  Does it do this for the other differences too?

Summary:
1.  JDWP says strings are UTF-8.
    We interpret this to mean standard UTF-8.

2.  JVMTI will be changed to match JNI saying that strings
    are M-UTF-8.

3.  The BE gets UTF-8 strings off JDWP and must convert them to
    M-UTF-8 before giving it to JVMTI or JNI.

4.  The BE gets M-UTF-8 strings from JNI and JVMTI and
    must convert them to UTF-8 when writing to JDWP.


 Here is how the supplementals are represented in java Strings.
 This from java.lang.Character doc:
    The Java 2 platform uses the UTF-16 representation in char arrays and
    in the String and StringBuffer classes. In this representation,
    supplementary characters are represented as a pair of char values,
    the first from the high-surrogates range, (\uD800-\uDBFF), the second
    from the low-surrogates range (\uDC00-\uDFFF).
  See utf8.txt


----

NSK Packet.java in the nsk/share/jdwp framework does this to write
a string to JDWP:
 public void addString(String value) {
        final int count = JDWP.TypeSize.INT + value.length();
        addInt(value.length());
        try {
            addBytes(value.getBytes("UTF-8"), 0, value.length());
        } catch (UnsupportedEncodingException e) {
            throw new Failure("Unsupported UTF-8 ecnoding while adding string value to JDWP packet:\n\t"
                                + e);
        }
    }
 ?? Does this get the standard UTF-8?  I would expect so.

and the readString method does this:
        for (int i = 0; i < len; i++)
            s[i] = getByte();

        try {
            return new String(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Failure("Unsupported UTF-8 ecnoding while extracting string value from JDWP packet:\n\t"
                                + e);
        }
Thus, this won't notice the modified UTF-8 coming in from JDWP .


*/

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.UnsupportedEncodingException;
import java.util.*;

    /********** target program **********/

/*
 * The debuggee has a few Strings the debugger reads via JDI
 */
class UTF8Targ {
    static String[] vals = new String[] {"xx\u0000yy",           // standard UTF-8 0
                                         "xx\ud800\udc00yy",     // first supplementary
                                         "xx\udbff\udfffyy"      // last supplementary
                                         // d800 = 1101 1000 0000 0000   dc00 = 1101 1100 0000 0000
                                         // dbff = 1101 1011 1111 1111   dfff = 1101 1111 1111 1111
    };

    static String aField;

    public static void main(String[] args){
        System.out.println("Howdy!");
        gus();
        System.out.println("Goodbye from UTF8Targ!");
    }
    static void gus() {
    }
}

    /********** test program **********/

public class UTF8Test extends TestScaffold {
    ClassType targetClass;
    ThreadReference mainThread;
    Field targetField;
    UTF8Test (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new UTF8Test(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("UTF8Targ");
        targetClass = (ClassType)bpe.location().declaringType();
        targetField = targetClass.fieldByName("aField");

        ArrayReference targetVals = (ArrayReference)targetClass.getValue(targetClass.fieldByName("vals"));

        /* For each string in the debuggee's 'val' array, verify that we can
         * read that value via JDI.
         */

        for (int ii = 0; ii < UTF8Targ.vals.length; ii++) {
            StringReference val = (StringReference)targetVals.getValue(ii);
            String valStr = val.value();

            /*
             * Verify that we can read a value correctly.
             * We read it via JDI, and access it directly from the static
             * var in the debuggee class.
             */
            if (!valStr.equals(UTF8Targ.vals[ii]) ||
                valStr.length() != UTF8Targ.vals[ii].length()) {
                failure("     FAILED: Expected /" + printIt(UTF8Targ.vals[ii]) +
                        "/, but got /" + printIt(valStr) + "/, length = " + valStr.length());
            }
        }

        /* Test 'all' unicode chars - send them to the debuggee via JDI
         * and then read them back.
         */
        doFancyVersion();

        resumeTo("UTF8Targ", "gus", "()V");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ee) {
        }


        /*
         * resume the target listening for events
         */

        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("UTF8Test: passed");
        } else {
            throw new Exception("UTF8Test: failed");
        }
    }

    /**
     * For each unicode value, send a string containing
     * it to the debuggee via JDI, read it back via JDI, and see if
     * we get the same value.
     */
    void doFancyVersion() throws Exception {
        // This does 4 chars at a time just to save time.
        for (int ii = Character.MIN_CODE_POINT;
             ii < Character.MIN_SUPPLEMENTARY_CODE_POINT;
             ii += 4) {
            // Skip the surrogates
            if (ii == Character.MIN_SURROGATE) {
                ii = Character.MAX_SURROGATE - 3;
                break;
            }
            doFancyTest(ii, ii + 1, ii + 2, ii + 3);
        }

        // Do the supplemental chars.
        for (int ii = Character.MIN_SUPPLEMENTARY_CODE_POINT;
             ii <= Character.MAX_CODE_POINT;
             ii += 2000) {
            // Too many of these so just do a few
            doFancyTest(ii, ii + 1, ii + 2, ii + 3);
        }

    }

    void doFancyTest(int ... args) throws Exception {
        String ss = new String(args, 0, 4);
        targetClass.setValue(targetField, vm().mirrorOf(ss));

        StringReference returnedVal = (StringReference)targetClass.getValue(targetField);
        String returnedStr = returnedVal.value();

        if (!ss.equals(returnedStr)) {
            failure("Set: FAILED: Expected /" + printIt(ss) +
                    "/, but got /" + printIt(returnedStr) + "/, length = " + returnedStr.length());
        }
    }

    /**
     * Return a String containing binary representations of
     * the chars in a String.
     */
     String printIt(String arg) {
        char[] carray = arg.toCharArray();
        StringBuffer bb = new StringBuffer(arg.length() * 5);
        for (int ii = 0; ii < arg.length(); ii++) {
            int ccc = arg.charAt(ii);
            bb.append(String.format("%1$04x ", ccc));
        }
        return bb.toString();
    }

    String printIt1(String arg) {
        byte[] barray = null;
        try {
             barray = arg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ee) {
        }
        StringBuffer bb = new StringBuffer(barray.length * 3);
        for (int ii = 0; ii < barray.length; ii++) {
            bb.append(String.format("%1$02x ", barray[ii]));
        }
        return bb.toString();
    }

}
