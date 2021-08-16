/*
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
 * @bug 8160564
 * @summary check the implementation of VersionProps.versionNumbers()
 * @modules java.base/java.lang:open
 * @run main VersionProps
 * @author Volker Simonis
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class VersionProps {

    final static String[] validVersions = {
        "1", "1.2", "1.2.3", "1.2.3.4", "1.0.0.1",
        "1.10000.1", "1.0.2.0.0.3.0.0.0.4.5.0.0.6",
        "1000001", "1.2.3.4.5.6.7.8.9.0.9.8.7.6.5.4.3.2.1" };

    @SuppressWarnings("rawtypes")
    final static List[] validLists = {
        Arrays.asList(1),
        Arrays.asList(1, 2),
        Arrays.asList(1, 2, 3),
        Arrays.asList(1, 2, 3, 4),
        Arrays.asList(1, 0, 0, 1),
        Arrays.asList(1, 10000, 1),
        Arrays.asList(1, 0, 2, 0, 0, 3, 0, 0, 0, 4, 5, 0, 0, 6),
        Arrays.asList(1000001),
        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 9, 8, 7, 6, 5, 4, 3, 2, 1) };

    final static String[] invalidVersions = {
        "01", "0.1.2", "1.02.3", "1.2.03.4", "1.0.0.1.0",
        "1.0.1.0.0", "1.00.1", "1.0.1.00", "1.10000." };

    public static void main(String[] args) throws Exception {
        Class<?> versionProps = Class.forName("java.lang.VersionProps");
        Method parseVersionNumbers =
            versionProps.getDeclaredMethod("parseVersionNumbers", String.class);
        parseVersionNumbers.setAccessible(true);

        for (int i = 0; i < validVersions.length; i++) {
            @SuppressWarnings("unchecked")
            List<Integer> li =
                (List<Integer>)parseVersionNumbers.invoke(null, validVersions[i]);
            System.out.println(li);
            if (!validLists[i].equals(li))
                throw new Exception(li + " != " + validLists[i]);
            li = Runtime.Version.parse(validVersions[i]).version();
            if (!validLists[i].equals(li))
                throw new Exception(li + " != " + validLists[i]);
        }

        for (int i = 0; i < invalidVersions.length; i++) {
            try {
                List<Integer> li =
                        (List<Integer>)parseVersionNumbers.invoke(null, invalidVersions[i]);
                throw new Exception(invalidVersions[i] +
                        " not recognized as invalid by VersionProps.parseVersionNumbers()");
            } catch (InvocationTargetException ex) {
                if (ex.getCause() instanceof IllegalArgumentException) {
                    System.out.println("OK - caught bad version string " +
                            invalidVersions[i]);
                } else {
                    throw ex;
                }
            }

            try {
                List<Integer> li = Runtime.Version.parse(invalidVersions[i]).version();
                throw new Exception(invalidVersions[i] +
                        " not recognized as invalid by Runtime.Version.parse()");
            } catch (IllegalArgumentException ex) {
                continue;
            }
        }
    }
}
