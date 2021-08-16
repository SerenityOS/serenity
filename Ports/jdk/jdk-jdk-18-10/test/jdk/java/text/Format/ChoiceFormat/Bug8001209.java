/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8001209
 * @summary Confirm that the values set by setChoices() are not mutable.
 */
import java.text.*;

public class Bug8001209 {

    public static void main(String[] args) throws Exception {
        boolean err = false;

        // Borrow an example in API doc
        double[] limits = {1,2,3,4,5,6,7};
        String[] dayOfWeekNames = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        ChoiceFormat form = new ChoiceFormat(limits, dayOfWeekNames);
        ParsePosition status = new ParsePosition(0);

        StringBuilder before = new StringBuilder();
        for (double i = 1.0; i <= 7.0; ++i) {
            status.setIndex(0);
            String s = form.format(i);
            before.append(" ");
            before.append(s);
            before.append(form.parse(form.format(i),status));
        }
        String original = before.toString();

        double[] newLimits = form.getLimits();
        String[] newFormats = (String[])form.getFormats();
        newFormats[6] = "Doyoubi";
        StringBuilder after = new StringBuilder();
        for (double i = 1.0; i <= 7.0; ++i) {
            status.setIndex(0);
            String s = form.format(i);
            after.append(" ");
            after.append(s);
            after.append(form.parse(form.format(i),status));
        }
        if (!original.equals(after.toString())) {
            err = true;
            System.err.println("  Expected:" + before
                               + "\n  Got:     " + after);
        }

        dayOfWeekNames[6] = "Saturday";
        after = new StringBuilder();
        for (double i = 1.0; i <= 7.0; ++i) {
            status.setIndex(0);
            String s = form.format(i);
            after.append(" ");
            after.append(s);
            after.append(form.parse(form.format(i),status));
        }
        if (!original.equals(after.toString())) {
            err = true;
            System.err.println("  Expected:" + before
                               + "\n  Got:     " + after);
        }

        if (err) {
            throw new RuntimeException("Failed.");
        } else {
            System.out.println("Passed.");
        }
    }
}
