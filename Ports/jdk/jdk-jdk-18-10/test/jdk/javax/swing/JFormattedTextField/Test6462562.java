/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6462562
   @summary Tests text input into JFormattedTextField
            with an InternationalFormatter
   @author Peter Zhelezniakov
   @run main Test6462562
*/

import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.Caret;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.NumberFormatter;


public class Test6462562
{
    static final String BACKSPACE = new String("backspace");
    static final String DELETE = new String("delete");

    boolean failed = false;

    void test() {
        testPercentFormat();
        testCurrencyFormat();
        testIntegerFormat();
        testDateFormat();

        if (failed) {
            throw new RuntimeException("Some testcases failed, see output above");
        }
        System.err.println("(-;  All testcases passed  ;-)");
    }

    TestFormattedTextField create(NumberFormat format) {
        format.setMaximumFractionDigits(0);
        NumberFormatter fmt = new NumberFormatter(format);
        return new TestFormattedTextField(fmt);
    }

    TestFormattedTextField create(DateFormat format) {
        DateFormatter fmt = new DateFormatter(format);
        return new TestFormattedTextField(fmt);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new Test6462562().test();
            }
        });
    }

    class TestFormattedTextField extends JFormattedTextField
    {
        final Action backspace;
        final Action delete;
        final Action insert;

        final ActionEvent dummyEvent;

        public TestFormattedTextField(InternationalFormatter fmt) {
            super(fmt);
            fmt.setAllowsInvalid(false);
            fmt.setOverwriteMode(true);

            backspace = getActionMap().get(DefaultEditorKit.deletePrevCharAction);
            delete = getActionMap().get(DefaultEditorKit.deleteNextCharAction);
            insert = getActionMap().get(DefaultEditorKit.insertContentAction);
            dummyEvent = new ActionEvent(this, 0, null);
        }

        public boolean test(int pos, int selectionLength, String todo, Object expectedResult) {
            Object v0 = getValue();

            Caret caret = getCaret();
            caret.setDot(pos);
            if (selectionLength > 0) {
                caret.moveDot(pos + selectionLength);
            }

            String desc = todo;
            if (todo == BACKSPACE) {
                backspace.actionPerformed(dummyEvent);
            } else if (todo == DELETE) {
                delete.actionPerformed(dummyEvent);
            } else {
                desc = "insert('" + todo + "')";
                insert.actionPerformed(new ActionEvent(this, 0, todo));
            }

            try {
                commitEdit();
            } catch (ParseException e) {
                e.printStackTrace();
                failed = true;
                return false;
            }

            Object v1 = getValue();
            if (! v1.equals(expectedResult)) {
                System.err.printf("Failure: value='%s', mark=%d, dot=%d, action=%s\n",
                        v0, pos, pos + selectionLength, desc);
                System.err.printf("   Result: '%s', expected: '%s'\n", v1, expectedResult);
                failed = true;
                return false;
            }
            return true;
        }
    }

    void testPercentFormat() {
        NumberFormat format = NumberFormat.getPercentInstance(Locale.US);
        TestFormattedTextField ftf = create(format);
        ftf.setValue(.34);

        System.err.println("Testing NumberFormat.getPercentInstance(Locale.US)");

        // test inserting individual characters
        ftf.test(0, 0, "1", .14);
        ftf.test(2, 0, "2", 1.42);
        ftf.test(1, 0, "0", 1.02);

        // test inserting several characters at once - e.g. from clipboard
        ftf.test(0, 0, "1024", 10.24);
        ftf.test(3, 0, "333", 103.33);
        ftf.test(6, 0, "77", 10333.77);
        ftf.test(4, 0, "99", 10399.77);
        ftf.test(6, 0, "00", 10390.07);

        // test inserting strings that contain some formatting
        ftf.test(0, 0, "2,2", 2290.07);
        ftf.test(2, 0, "2,2", 222.27);
        ftf.test(4, 0, "2,2", 222.22);
        ftf.test(6, 0, "33,33", 2222233.33);

        // test delete
        ftf.test(0, 0, DELETE, 222233.33);
        ftf.test(10, 0, DELETE, 222233.33);
        ftf.test(5, 0, DELETE, 22223.33);
        ftf.test(6, 0, DELETE, 2222.33);

        // test backspace
        ftf.test(0, 0, BACKSPACE, 2222.33);
        ftf.test(7, 0, BACKSPACE, 222.23);
        ftf.test(4, 0, BACKSPACE, 22.23);
        ftf.test(2, 0, BACKSPACE, 2.23);

        // test replacing selection
        ftf.test(0, 1, "555", 555.23);
        ftf.test(4, 2, "555", 5555.55);
        ftf.test(2, 3, "1", 551.55);
        ftf.test(3, 2, "6", 55.65);
        ftf.test(4, 2, "12", 556.12);
        ftf.test(3, 4, "0", 5.5);
        ftf.test(0, 3, "111222333444555", 1112223334445.55);

        // test deleting selection
        ftf.test(0, 2, DELETE, 12223334445.55);
        ftf.test(0, 3, BACKSPACE, 223334445.55);
        ftf.test(12, 2, DELETE, 2233344.45);
        ftf.test(9, 2, BACKSPACE, 22333.44);
        ftf.test(4, 3, DELETE, 223.44);
        ftf.test(1, 2, BACKSPACE, 23.44);
        ftf.test(3, 3, DELETE, .23);
        ftf.test(1, 2, BACKSPACE, .02);
    }

    void testCurrencyFormat() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        TestFormattedTextField ftf = create(format);
        ftf.setValue(56L);

        System.err.println("Testing NumberFormat.getCurrencyInstance(Locale.US)");

        // test inserting individual characters
        ftf.test(1, 0, "1", 16L);
        ftf.test(3, 0, "2", 162L);
        ftf.test(2, 0, "0", 102L);

        // test inserting several characters at once - e.g. from clipboard
        ftf.test(1, 0, "1024", 1024L);
        ftf.test(4, 0, "333", 10333L);
        ftf.test(7, 0, "77", 1033377L);
        ftf.test(5, 0, "99", 1039977L);
        ftf.test(7, 0, "00", 1039007L);

        // test inserting strings that contain some formatting
        ftf.test(1, 0, "2,2", 229007L);
        ftf.test(3, 0, "2,2", 22227L);
        ftf.test(4, 0, "2,2", 2222L);
        ftf.test(6, 0, "33,33", 22223333L);

        // test delete
        ftf.test(1, 0, DELETE, 2223333L);
        ftf.test(10, 0, DELETE, 2223333L);
        ftf.test(5, 0, DELETE, 222333L);
        ftf.test(5, 0, DELETE, 22233L);

        // test backspace
        ftf.test(1, 0, BACKSPACE, 22233L);
        ftf.test(7, 0, BACKSPACE, 2223L);
        ftf.test(4, 0, BACKSPACE, 223L);
        ftf.test(2, 0, BACKSPACE, 23L);

        // test replacing selection
        ftf.test(1, 1, "555", 5553L);
        ftf.test(4, 2, "555", 55555L);
        ftf.test(2, 3, "1", 5155L);
        ftf.test(3, 2, "6", 565L);
        ftf.test(1, 3, "111222333444555", 111222333444555L);

        // test deleting selection
        ftf.test(1, 2, DELETE, 1222333444555L);
        ftf.test(1, 3, BACKSPACE, 22333444555L);
        ftf.test(13, 2, DELETE, 223334445L);
        ftf.test(10, 2, BACKSPACE, 2233344L);
        ftf.test(4, 4, DELETE, 2244L);
        ftf.test(1, 4, BACKSPACE, 4L);
    }

    void testIntegerFormat() {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        TestFormattedTextField ftf = create(format);
        ftf.setValue(56L);

        System.err.println("Testing NumberFormat.getIntegerInstance(Locale.US)");

        // test inserting individual characters
        ftf.test(0, 0, "1", 16L);
        ftf.test(2, 0, "2", 162L);
        ftf.test(1, 0, "0", 102L);

        // test inserting several characters at once - e.g. from clipboard
        ftf.test(0, 0, "1024", 1024L);
        ftf.test(3, 0, "333", 10333L);
        ftf.test(6, 0, "77", 1033377L);
        ftf.test(4, 0, "99", 1039977L);
        ftf.test(6, 0, "00", 1039007L);

        // test inserting strings that contain some formatting
        ftf.test(0, 0, "2,2", 229007L);
        ftf.test(2, 0, "2,2", 22227L);
        ftf.test(3, 0, "2,2", 2222L);
        ftf.test(5, 0, "33,33", 22223333L);

        // test delete
        ftf.test(0, 0, DELETE, 2223333L);
        ftf.test(9, 0, DELETE, 2223333L);
        ftf.test(4, 0, DELETE, 222333L);
        ftf.test(4, 0, DELETE, 22233L);

        // test backspace
        ftf.test(0, 0, BACKSPACE, 22233L);
        ftf.test(6, 0, BACKSPACE, 2223L);
        ftf.test(2, 0, BACKSPACE, 223L);
        ftf.test(2, 0, BACKSPACE, 23L);

        // test replacing selection
        ftf.test(0, 1, "555", 5553L);
        ftf.test(3, 2, "555", 55555L);
        ftf.test(1, 3, "1", 5155L);
        ftf.test(2, 2, "6", 565L);
        ftf.test(0, 3, "111222333444555", 111222333444555L);

        // test deleting selection
        ftf.test(0, 2, DELETE, 1222333444555L);
        ftf.test(0, 3, BACKSPACE, 22333444555L);
        ftf.test(12, 2, DELETE, 223334445L);
        ftf.test(9, 2, BACKSPACE, 2233344L);
        ftf.test(3, 4, DELETE, 2244L);
        ftf.test(0, 4, BACKSPACE, 4L);
    }

    Date date(DateFormat format, String spec) {
        try {
            return format.parse(spec);
        } catch (ParseException e) {
            throw new Error("Error in test");
        }
    }

    void testDateFormat() {
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        TestFormattedTextField ftf = create(format);
        ftf.setValue(date(format, "12/05/2005"));

        System.err.println("Testing SimpleDateFormat(\"MM/dd/yyyy\", Locale.US)");

        // test inserting individual characters
        ftf.test(0, 0, "0", date(format, "02/05/2005"));
        ftf.test(4, 0, "4", date(format, "02/04/2005"));
        ftf.test(6, 0, "1", date(format, "02/04/1005"));
        ftf.test(9, 0, "9", date(format, "02/04/1009"));

        // test inserting several characters at once - e.g. from clipboard
        ftf.test(0, 0, "11", date(format, "11/04/1009"));
        ftf.test(3, 0, "23", date(format, "11/23/1009"));
        ftf.test(6, 0, "191", date(format, "11/23/1919"));

        // test delete
        ftf.test(0, 0, DELETE, date(format, "01/23/1919"));
        ftf.test(3, 0, DELETE, date(format, "01/03/1919"));
        ftf.test(10, 0, DELETE, date(format, "01/03/1919"));
        ftf.test(1, 0, DELETE, date(format, "12/03/1918"));
        ftf.test(4, 0, DELETE, date(format, "11/30/1918"));

        // test backspace
        ftf.test(0, 0, BACKSPACE, date(format, "11/30/1918"));
        ftf.test(1, 0, BACKSPACE, date(format, "01/30/1918"));
        ftf.test(4, 0, BACKSPACE, date(format, "12/31/1917"));
        ftf.test(10, 0, BACKSPACE, date(format, "12/31/0191"));
        ftf.test(3, 0, BACKSPACE, date(format, "01/31/0191"));
        ftf.test(5, 0, BACKSPACE, date(format, "01/03/0191"));

        // test replacing selection
        ftf.test(0, 1, "1", date(format, "11/03/0191"));
        ftf.test(3, 1, "2", date(format, "11/23/0191"));
        ftf.test(6, 2, "20", date(format, "11/23/2091"));

        // test deleting selection
        ftf.test(0, 1, BACKSPACE, date(format, "01/23/2091"));
        ftf.test(3, 1, DELETE, date(format, "01/03/2091"));
        ftf.test(6, 2, BACKSPACE, date(format, "01/03/0091"));
        ftf.test(8, 1, DELETE, date(format, "01/03/0001"));
    }
}
