/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @library /java/text/testlib
 * @summary test MessageFormat
 */
/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/


import java.util.*;
import java.io.*;
import java.text.*;

public class MessageTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        new MessageTest().run(args);
    }


   public void TestMSGPatternTest() {
        Object[] testArgs = {
             1D, 3456D,
            "Disk", new Date(10000000000L)};

        String[] testCases = {
           "Quotes '', '{', 'a' {0} '{0}'",
           "Quotes '', '{', 'a' {0,number} '{0}'",
           "'{'1,number,'#',##} {1,number,'#',##}",
           "There are {1} files on {2} at {3}",
           "On {2}, there are {1} files, with {0,number,currency}.",
           "'{1,number,percent}', {1,number,percent}, ",
           "'{1,date,full}', {1,date,full}, ",
           "'{3,date,full}', {3,date,full}, ",
           "'{1,number,#,##}' {1,number,#,##}",
        };

        for (int i = 0; i < testCases.length; ++i) {
            Locale save = Locale.getDefault();
            try {
                Locale.setDefault(Locale.US);
                logln("");
                logln( i + " Pat in:  " + testCases[i]);
                MessageFormat form = new MessageFormat(testCases[i]);
                logln( i + " Pat out: " + form.toPattern());
                String result = form.format(testArgs);
                logln( i + " Result:  " + result);
                Object[] values = form.parse(result);
                for (int j = 0; j < testArgs.length; ++j) {
                    Object testArg = testArgs[j];
                    Object value = null;
                    if (j < values.length) {
                        value = values[j];
                    }
                    if ((testArg == null && value != null)
                        || (testArg != null && !testArg.equals(value))) {
                       logln( i + " " + j + " old: " + testArg);
                       logln( i + " " + j + " new: " + value);
                    }
                }
            }
            catch(java.text.ParseException pe ) {
                throw new RuntimeException("Error: MessageFormat.parse throws ParseException");
            }
            finally{
                Locale.setDefault(save);
            }
        }
    }
}
