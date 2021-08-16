/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8075545 8210406
 * @summary Check whether RuntimePermission("localeServiceProvider") is
 *          handled correctly.
 * @library providersrc/foobarutils
 *          providersrc/barprovider
 *          providersrc/fooprovider
 * @build com.foobar.Utils
 *        com.bar.*
 *        com.foo.*
 * @run main/othervm PermissionTest
 * @run main/othervm/fail/java.security.policy=dummy.policy
 *                        -Djava.security.manager
 *                        -Djava.locale.providers=JRE,SPI
 *                        PermissionTest
 * @run main/othervm/java.security.policy=localeServiceProvider.policy
 *                   -Djava.security.manager
 *                   -Djava.locale.providers=JRE,SPI
 *                   PermissionTest
 */

import com.bar.CalendarDataProviderImpl;
import com.bar.CalendarNameProviderImpl;
import com.bar.CurrencyNameProviderImpl;
import com.bar.CurrencyNameProviderImpl2;
import com.bar.GenericTimeZoneNameProviderImpl;
import com.bar.LocaleNameProviderImpl;
import com.bar.TimeZoneNameProviderImpl;
import com.foo.BreakIteratorProviderImpl;
import com.foo.CollatorProviderImpl;
import com.foo.DateFormatProviderImpl;
import com.foo.DateFormatSymbolsProviderImpl;
import com.foo.DecimalFormatSymbolsProviderImpl;
import com.foo.NumberFormatProviderImpl;

public class PermissionTest{

    //  Make sure provider impls can be instantiated under a security manager.ZZ
    BreakIteratorProviderImpl breakIP = new BreakIteratorProviderImpl();
    CollatorProviderImpl collatorP = new CollatorProviderImpl();
    DateFormatProviderImpl dateFP = new DateFormatProviderImpl();
    DateFormatSymbolsProviderImpl dateFSP = new DateFormatSymbolsProviderImpl();
    DecimalFormatSymbolsProviderImpl decimalFSP = new DecimalFormatSymbolsProviderImpl();
    NumberFormatProviderImpl numberFP = new NumberFormatProviderImpl();
    CurrencyNameProviderImpl currencyNP = new CurrencyNameProviderImpl();
    CurrencyNameProviderImpl2 currencyNP2 = new CurrencyNameProviderImpl2();
    LocaleNameProviderImpl localeNP = new LocaleNameProviderImpl();
    TimeZoneNameProviderImpl tzNP = new TimeZoneNameProviderImpl();
    GenericTimeZoneNameProviderImpl tzGenNP = new GenericTimeZoneNameProviderImpl();
    CalendarDataProviderImpl calDataP = new CalendarDataProviderImpl();
    CalendarNameProviderImpl calNameP = new CalendarNameProviderImpl();

    public static void main(String[] s) {
        new PermissionTest();
    }
}
