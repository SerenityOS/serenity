/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6190861
 * @summary Make sure to always load the default locale's bundle when
 * there's no bundle for the requested locale.
 */

import java.util.*;

public class Bug6190861 {

    public static void main(String[] args) {
        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("en", "US"));

            List localeList = new ArrayList();
            localeList.add(Locale.ENGLISH);
            localeList.add(Locale.KOREA);
            localeList.add(Locale.UK);
            localeList.add(new Locale("en", "CA"));
            localeList.add(Locale.ENGLISH);

            Iterator iter = localeList.iterator();
            while (iter.hasNext()){
                Locale currentLocale = (Locale) iter.next();
                System.out.println("\ncurrentLocale = "
                               + currentLocale.getDisplayName());

                ResourceBundle messages =
                    ResourceBundle.getBundle("Bug6190861Data",currentLocale);

                Locale messagesLocale = messages.getLocale();
                System.out.println("messagesLocale = "
                               + messagesLocale.getDisplayName());
                checkMessages(messages);
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    static void checkMessages(ResourceBundle messages) {
        String greetings = messages.getString("greetings");
        String inquiry = messages.getString("inquiry");
        String farewell = messages.getString("farewell");
        System.out.println(greetings);
        System.out.println(inquiry);
        System.out.println(farewell);
        if (!greetings.equals("Hiya.")) {
            throw new RuntimeException("got wrong resource bundle");
        }
    }
}
