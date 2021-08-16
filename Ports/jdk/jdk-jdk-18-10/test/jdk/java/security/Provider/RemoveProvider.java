/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4190873 7054918 8130181
 * @library ../testlibrary
 * @summary Make sure provider instance can be removed from list of registered
 * providers, and "entrySet", "keySet", and "values" methods don't loop
 * indefinitely.
 */
import java.security.*;
import java.util.*;

public class RemoveProvider {

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {

        // Add provider 1
        Provider p1 = new MyProvider("name1","1","");
        Security.addProvider(p1);

        // Add provider 2
        Provider p2 = new MyProvider("name2","1","");
        Security.addProvider(p2);

        // List all providers
        System.out.println("// List all providers");
        Provider[] provs = Security.getProviders();
        for (int i=0; i<provs.length; i++)
            System.out.println(provs[i].toString());

        // Remove one provider and list all remaining providers
        System.out.println("");
        System.out.println("// Remove one provider");
        Security.removeProvider("name1");
        provs = Security.getProviders();
        for (int i=0; i<provs.length; i++)
            System.out.println(provs[i].toString());

        // Iterate over the entrySet
        System.out.println("");
        System.out.println("// Iterate over entrySet");
        Map.Entry me = null;
        Set es = p1.entrySet();
        Iterator i = es.iterator();
        while (i.hasNext()) {
            me = (Map.Entry)i.next();
            System.out.println("Key: " + (String)me.getKey());
            System.out.println("Value: " + (String)me.getValue());
        }
        // Try to modify the backing Map, and catch the expected exception
        try {
            me.setValue("name1.mac");
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
            System.out.println("Expected exception caught");
        }

        // Iterate over the keySet, and try to remove one (should fail)
        System.out.println("");
        System.out.println("// Iterate over keySet");
        Object o = null;
        Set ks = p1.keySet();
        i = ks.iterator();
        while (i.hasNext()) {
            o = i.next();
            System.out.println((String)o);
        }
        try {
            ks.remove(o);
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
        }

        // Iterate over the map values
        System.out.println("");
        System.out.println("// Iterate over values");
        Collection c = p1.values();
        i = c.iterator();
        while (i.hasNext()) {
            System.out.println((String)i.next());
        }

        // Modify provider and make sure changes are reflected in
        // existing entrySet (i.e., make sure entrySet is "live").
        // First, add entry to provider.
        System.out.println("");
        System.out.println("// Add 'Cipher' entry to provider");
        p1.put("Cipher", "name1.des");
        //  entrySet
        i = es.iterator();
        boolean found = false;
        while (i.hasNext()) {
            me = (Map.Entry)i.next();
            System.out.println("Key: " + (String)me.getKey());
            System.out.println("Value: " + (String)me.getValue());
            if (((String)me.getKey()).equals("Cipher"))
                found = true;
        }
        if (!found)
            throw new Exception("EntrySet not live");
        // keySet
        i = ks.iterator();
        while (i.hasNext()) {
            o = i.next();
            System.out.println((String)o);
        }
        // collection
        i = c.iterator();
        while (i.hasNext()) {
            System.out.println((String)i.next());
        }

        // Remove entry from provider
        System.out.println("");
        System.out.println("// Remove 'Digest' entry from provider");
        p1.remove("Digest");
        // entrySet
        i = es.iterator();
        while (i.hasNext()) {
            me = (Map.Entry)i.next();
            System.out.println("Key: " + (String)me.getKey());
            System.out.println("Value: " + (String)me.getValue());
        }
        // keySet
        i = ks.iterator();
        while (i.hasNext()) {
            o = i.next();
            System.out.println((String)o);
        }
        // collection
        i = c.iterator();
        while (i.hasNext()) {
            System.out.println((String)i.next());
        }

        // Retrieve new entrySet and make sure the backing Map cannot be
        // mofified
        es = p1.entrySet();
        i = es.iterator();
        while (i.hasNext()) {
            me = (Map.Entry)i.next();
            System.out.println("Key: " + (String)me.getKey());
            System.out.println("Value: " + (String)me.getValue());
        }
        try {
            me.setValue("name1.mac");
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
            System.out.println("Expected exception caught");
        }
    }
}

class MyProvider extends Provider {
    public MyProvider(String name, String version, String info) {
        super(name, version, info);
        put("Signature", name+".signature");
        put("Digest", name+".digest");
    }
}
