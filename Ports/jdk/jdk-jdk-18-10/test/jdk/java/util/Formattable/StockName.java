/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4965770 4992540 5030716
 */

import java.nio.CharBuffer;
import java.util.Formatter;
import java.util.Formattable;
import java.util.Locale;
import static java.util.FormattableFlags.*;

public class StockName implements Formattable {
    private String symbol, companyName, frenchCompanyName;

    public StockName(String symbol, String companyName,
                     String frenchCompanyName)
    {
        this.symbol = symbol;
        this.companyName = companyName;
        this.frenchCompanyName = frenchCompanyName;
    }

    public void formatTo(Formatter fmt, int f, int width, int precision) {
        StringBuilder sb = new StringBuilder();

        // decide form of name
        String name = companyName;
        if (fmt.locale().equals(Locale.FRANCE))
            name = frenchCompanyName;
        boolean alternate = (f & ALTERNATE) == ALTERNATE;
        boolean usesymbol = alternate || (precision != -1 && precision < 10);
        String out = (usesymbol ? symbol : name);

        // apply precision
        if (precision == -1 || out.length() < precision) {
            // write it all
            sb.append(out);
        } else {
            sb.append(out.substring(0, precision - 1)).append('*');
        }

        // apply width and justification
        int len = sb.length();
        if (len < width)
            for (int i = 0; i < width - len; i++)
                if ((f & LEFT_JUSTIFY) == LEFT_JUSTIFY)
                    sb.append(' ');
                else
                    sb.insert(0, ' ');

        fmt.format(sb.toString());
    }

    public String toString() {
        return String.format("%s - %s", symbol, companyName);
    }

    public static void main(String [] args) {
        StockName sn = new StockName("HUGE", "Huge Fruit, Inc.",
                                     "Fruit Titanesque, Inc.");
        CharBuffer cb = CharBuffer.allocate(128);
        Formatter fmt = new Formatter(cb);

        fmt.format("%s", sn);            //   -> "Huge Fruit, Inc."
        test(cb, "Huge Fruit, Inc.");

        fmt.format("%s", sn.toString()); //   -> "HUGE - Huge Fruit, Inc."
        test(cb, "HUGE - Huge Fruit, Inc.");

        fmt.format("%#s", sn);           //   -> "HUGE"
        test(cb, "HUGE");

        fmt.format("%-10.8s", sn);       //   -> "HUGE      "
        test(cb, "HUGE      ");

        fmt.format("%.12s", sn);         //   -> "Huge Fruit,*"
        test(cb, "Huge Fruit,*");

        fmt.format(Locale.FRANCE, "%25s", sn);
                                         //   -> "   Fruit Titanesque, Inc."
        test(cb, "   Fruit Titanesque, Inc.");
    }

    private static void test(CharBuffer cb, String exp) {
        cb.limit(cb.position());
        cb.rewind();
        if (!cb.toString().equals(exp))
            throw new RuntimeException("expect: '" + exp + "'; got: '"
                                       + cb.toString() + "'");
        cb.clear();
    }
}
