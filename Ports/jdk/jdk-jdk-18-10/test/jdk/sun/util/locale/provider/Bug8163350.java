/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163350
 * @modules java.base/sun.util.locale.provider
 * @summary Test FALLBACK provider is not in adapter preference list when
 * COMPAT is specified with System Property java.locale.providers.
 * @run main/othervm  -Djava.locale.providers=COMPAT Bug8163350
 */
import java.util.List;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleProviderAdapter.Type;

public class Bug8163350 {

    public static void main(String[] args) {
        List<Type> preferenceList = LocaleProviderAdapter.getAdapterPreference();
        if (preferenceList.contains(Type.FALLBACK)) {
            throw new RuntimeException("FALLBACK Provider should not be returned in "
                    + "adapter list " + preferenceList);
        }
    }
}
