/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6455351 8130181
 * @summary Make sure the Provider.info entries have the correct values
 * after going through serialization/deserialization.
 * @author Valerie Peng
 */

import java.io.*;
import java.security.*;
import java.util.*;

public class ProviderInfoCheck {

    public static void main(String[] args) throws Exception {
        Provider p = new SampleProvider();

        // Serialize and deserialize the above Provider object
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(p);
        oos.close();
        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Provider p2 = (Provider) ois.readObject();
        ois.close();

        checkProviderInfoEntries(p2);
    }

    private static void checkProviderInfoEntries(Provider p)
        throws Exception {
        String value = (String) p.get("Provider.id name");
        if (!SampleProvider.NAME.equalsIgnoreCase(value) ||
            !p.getName().equalsIgnoreCase(value)) {
            throw new Exception("Test Failed: incorrect name!");
        }
        value = (String) p.get("Provider.id info");
        if (!SampleProvider.INFO.equalsIgnoreCase(value) ||
            !p.getInfo().equalsIgnoreCase(value)) {
            throw new Exception("Test Failed: incorrect info!");
        }
        value = (String) p.get("Provider.id className");
        if (!p.getClass().getName().equalsIgnoreCase(value)) {
            throw new Exception("Test Failed: incorrect className!");
        }
        value = (String) p.get("Provider.id version");
        if (!SampleProvider.VERSION.equalsIgnoreCase(value) ||
            p.getVersionStr() != value) {
            throw new Exception("Test Failed: incorrect versionStr!");
        }
        System.out.println("Test Passed");
    }

    private static class SampleProvider extends Provider {
        static String NAME = "Sample";
        static String VERSION = "1.1";
        static String INFO = "Good for nothing";
        SampleProvider() {
            super(NAME, VERSION, INFO);
        }
    }
}
