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
 * @summary test Collation, Monkey style
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

import java.io.IOException;
import java.util.Random;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.CollationKey;

public class MonkeyTest extends CollatorTest
{
    public static void main(String[] args) throws Exception {
        new MonkeyTest().run(args);
    }

    public void report(String s, String t, int result, int revResult)
    {
        if (result == -1)
        {
            if (revResult != 1)
                errln(" --> Test Failed");
        }
        else if (result == 1)
        {
            if (revResult != -1)
                errln(" --> Test Failed");
        }
        else if (result == 0)
        {
            if (revResult != 0)
                errln(" --> Test Failed");
        }
    }

    public void TestCollationKey()
    {
        String source = "-abcdefghijklmnopqrstuvwxyz#&^$@";
        Random r = new Random(3);
        int s = checkValue(r.nextInt() % source.length());
        int t = checkValue(r.nextInt() % source.length());
        int slen = checkValue((r.nextInt() - source.length()) % source.length());
        int tlen = checkValue((r.nextInt() - source.length()) % source.length());
        String subs = source.substring((s > slen ? slen : s), (s >= slen ? s : slen));
        String subt = source.substring((t > tlen ? tlen : t), (t >= tlen ? t : tlen));
        myCollator.setStrength(Collator.TERTIARY);
        CollationKey CollationKey1 = myCollator.getCollationKey(subs);
        CollationKey CollationKey2 = myCollator.getCollationKey(subt);
        int result = CollationKey1.compareTo(CollationKey2);  // Tertiary
        int revResult = CollationKey2.compareTo(CollationKey1);  // Tertiary
        report(("CollationKey(" + subs + ")"), ("CollationKey(" + subt + ")"), result, revResult);
        myCollator.setStrength(Collator.SECONDARY);
        CollationKey1 = myCollator.getCollationKey(subs);
        CollationKey2 = myCollator.getCollationKey(subt);
        result = CollationKey1.compareTo(CollationKey2);  // Secondary
        revResult = CollationKey2.compareTo(CollationKey1);   // Secondary
        report(("CollationKey(" + subs + ")") , ("CollationKey(" + subt + ")"), result, revResult);
        myCollator.setStrength(Collator.PRIMARY);
        CollationKey1 = myCollator.getCollationKey(subs);
        CollationKey2 = myCollator.getCollationKey(subt);
        result = CollationKey1.compareTo(CollationKey2);  // Primary
        revResult = CollationKey2.compareTo(CollationKey1);   // Primary
        report(("CollationKey(" + subs + ")"), ("CollationKey(" + subt + ")"), result, revResult);
        String addOne = subs + "\uE000";
        CollationKey1 = myCollator.getCollationKey(subs);
        CollationKey2 = myCollator.getCollationKey(addOne);
        result = CollationKey1.compareTo(CollationKey2);
        if (result != -1)
            errln("CollationKey(" + subs + ")" + ".LT." + "CollationKey(" + addOne + ") Failed.");
        result = CollationKey2.compareTo(CollationKey1);
        if (result != 1)
            errln("CollationKey(" + addOne + ")" + ".GT." + "CollationKey(" + subs + ") Failed.");
    }
    private static int checkValue(int value)
    {
        value *= (value > 0) ? 1 : -1;
        return value;
    }
    public void TestCompare()
    {
        String source = "-abcdefghijklmnopqrstuvwxyz#&^$@";
        Random r = new Random(3);
        int s = checkValue(r.nextInt() % source.length());
        int t = checkValue(r.nextInt() % source.length());
        int slen = checkValue((r.nextInt() - source.length()) % source.length());
        int tlen = checkValue((r.nextInt() - source.length()) % source.length());
        String subs = source.substring((s > slen ? slen : s), (s >= slen ? s : slen));
        String subt = source.substring((t > tlen ? tlen : t), (t >= tlen ? t : tlen));
        myCollator.setStrength(Collator.TERTIARY);
        int result = myCollator.compare(subs, subt);  // Tertiary
        int revResult = myCollator.compare(subt, subs);  // Tertiary
        report(subs, subt, result, revResult);
        myCollator.setStrength(Collator.SECONDARY);
        result = myCollator.compare(subs, subt);  // Secondary
        revResult = myCollator.compare(subt, subs);  // Secondary
        report(subs, subt, result, revResult);
        myCollator.setStrength(Collator.PRIMARY);
        result = myCollator.compare(subs, subt);  // Primary
        revResult = myCollator.compare(subt, subs);  // Primary
        report(subs, subt, result, revResult);
        String addOne = subs + "\uE000";
        result = myCollator.compare(subs, addOne);
        if (result != -1)
            errln("Test : " + subs + " .LT. " + addOne + " Failed.");
        result = myCollator.compare(addOne, subs);
        if (result != 1)
            errln("Test : " + addOne + " .GE. " + subs + " Failed.");
    }
    private static Collator myCollator = Collator.getInstance();
}
