/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 4718897
  @summary tests that a Unicode string can be transferred between JVMs.
  @author das@sparc.spb.su area=datatransfer
  @library ../../regtesthelpers/process
  @build ProcessResults ProcessCommunicator
  @run main UnicodeTransferTest
*/

import java.awt.datatransfer.*;
import java.awt.*;
import java.text.Normalizer;

import test.java.awt.regtesthelpers.process.ProcessResults;
import test.java.awt.regtesthelpers.process.ProcessCommunicator;

public class UnicodeTransferTest {
    private static final Toolkit tk = Toolkit.getDefaultToolkit();
    private static final Clipboard clipboard = tk.getSystemClipboard();
    private static final Transferable t = new StringSelection(Util.getTestString());

    public static void main(String[] args) throws Exception {
        Util.setClipboardContents(clipboard, t, null);
        ProcessResults result = ProcessCommunicator.executeChildProcess(
                UnicodeTransferTestChild.class, new String[0]);
        verifyTestResults(result);
    }

    private static void verifyTestResults(ProcessResults processResults) {
        if (processResults.getExitValue() != 0) {
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED. See child stderr");
        }
        processResults.verifyStdErr(System.err);
        processResults.verifyProcessExitValue(System.err);
        processResults.printProcessStandartOutput(System.out);
    }

}

class Util {
    private static String testString = null;

    static {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i < 0x10000; i++) {
            // Skip surrogates.
            if (i < 0xD800 || (i > 0xDFFF && i < 0xFFF0)) {
                buf.append((char) i);
            } else {
                buf.append(0x20);
            }
        }
        // On OS X the unicode string is normalized but the clipboard,
        // so we need to use normalized strings as well to be able to
        // check the result
        testString = Normalizer.normalize(buf.toString(), Normalizer.Form.NFC);
    }

    public static String getTestString() {
        return testString;
    }

    public static void setClipboardContents(Clipboard cb,
                                            Transferable contents,
                                            ClipboardOwner owner) {

        boolean set = false;
        while (!set) {
            try {
                cb.setContents(contents, owner);
                set = true;
            } catch (IllegalStateException ise) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Transferable getClipboardContents(Clipboard cb,
                                                    Object requestor) {
        while (true) {
            try {
                return cb.getContents(requestor);
            } catch (IllegalStateException ise) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class UnicodeTransferTestChild {
    private static final Toolkit tk = Toolkit.getDefaultToolkit();
    private static final Clipboard clipboard = tk.getSystemClipboard();

    public static void main(String[] args) {
        Transferable t = Util.getClipboardContents(clipboard, null);

        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            Object o = null;
            try {
                o = t.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String testStr = Util.getTestString();

            if (!testStr.equals(o)) {
                if (o instanceof String) {
                    String s = (String)o;
                    if (s.length() != testStr.length()) {
                        System.err.println("Received length:" + s.length() +
                                " Expected length: " +
                                testStr.length());
                    } else {
                        for (int i = 0; i < s.length(); i++) {
                            char ch = s.charAt(i);
                            char expected = testStr.charAt(i);
                            if (ch != expected) {
                                System.err.println("i=" + i +
                                        " char=" +
                                        Integer.toHexString((int)ch) +
                                        " expected=" +
                                        Integer.toHexString(expected));
                            }
                        }
                    }
                } else {
                    System.err.println("Received object:" + o);
                }
                throw new RuntimeException("String doesn't match.");
            }
        } else {
            throw new RuntimeException("Clipboard content was not set");
        }
    }
}
