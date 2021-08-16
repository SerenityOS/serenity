/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/*
 * @test
 * @bug 8073214 8075362
 * @summary Tests to verify that load() and store() throw NPEs as advertised.
 */
public class LoadAndStoreNPE
{
    public static void main(String[] args) throws Exception
    {
        int failures = 0;

        Properties props = new Properties();

        try {
            props.store((OutputStream)null, "comments");
            failures++;
        } catch (NullPointerException e) {
            // do nothing
        }

        try {
            props.store((Writer)null, "comments");
            failures++;
        } catch (NullPointerException e) {
            // do nothing
        }

        try {
            props.load((InputStream)null);
            failures++;
        } catch (NullPointerException e) {
            // do nothing
        }

        try {
            props.load((Reader)null);
            failures++;
        } catch (NullPointerException e) {
            // do nothing
        }

        if (failures != 0) {
            throw new RuntimeException("LoadAndStoreNPE failed with "
                + failures + " errors!");
        }
    }
}
