/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218960
 * @modules java.base/sun.util.locale.provider
 * @modules java.logging
 * @summary Check that no CONFIG messages are logged on instantiating
 *     SimpleDateFormat with the language-only locale.
 * @run main CheckLoggingFromLocaleProvider
 */
import java.text.*;
import java.util.*;
import java.util.logging.*;
import sun.util.locale.provider.*;

public class CheckLoggingFromLocaleProvider extends StreamHandler {

    @Override
    public boolean isLoggable(LogRecord lr) {
        if (lr.getLevel() == Level.CONFIG &&
            lr.getLoggerName().equals(
                LocaleServiceProviderPool.class.getCanonicalName())) {
            throw new RuntimeException("CONFIG message was logged in " +
                lr.getLoggerName() + ". Message: " + lr.getMessage());
        }
        return false;
    }

    public CheckLoggingFromLocaleProvider() {
        setLevel(Level.CONFIG);
        Logger l = LogManager.getLogManager().getLogger("");
        l.setLevel(Level.CONFIG);
        l.addHandler(this);

        new SimpleDateFormat("", Locale.ENGLISH);
    }

    public static void main(String[] args) {
        new CheckLoggingFromLocaleProvider();
    }
}
