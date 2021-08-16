/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.ktab;

import sun.security.action.GetPropertyAction;
import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import sun.security.jgss.krb5.ServiceCreds;

/**
 * This class represents key table. The key table functions deal with storing
 * and retrieving service keys for use in authentication exchanges.
 *
 * A KeyTab object is always constructed, if the file specified does not
 * exist, it's still valid but empty. If there is an I/O error or file format
 * error, it's invalid.
 *
 * The class is immutable on the read side (the write side is only used by
 * the ktab tool).
 *
 * @author Yanni Zhang
 */
public class KeyTab implements KeyTabConstants {

    private static final boolean DEBUG = Krb5.DEBUG;
    private static String defaultTabName = null;

    // Attention: Currently there is no way to remove a keytab from this map,
    // this might lead to a memory leak.
    private static Map<String,KeyTab> map = new HashMap<>();

    // KeyTab file does not exist. Note: a missing keytab is still valid
    private boolean isMissing = false;

    // KeyTab file is invalid, possibly an I/O error or a file format error.
    private boolean isValid = true;

    private final String tabName;
    private long lastModified;
    private int kt_vno = KRB5_KT_VNO;

    private Vector<KeyTabEntry> entries = new Vector<>();

    /**
     * Constructs a KeyTab object.
     *
     * If there is any I/O error or format errot during the loading, the
     * isValid flag is set to false, and all half-read entries are dismissed.
     * @param filename path name for the keytab file, must not be null
     */
    private KeyTab(String filename) {
        tabName = filename;
        try {
            lastModified = new File(tabName).lastModified();
            try (KeyTabInputStream kis =
                    new KeyTabInputStream(new FileInputStream(filename))) {
                load(kis);
            }
        } catch (FileNotFoundException e) {
            entries.clear();
            isMissing = true;
        } catch (Exception ioe) {
            entries.clear();
            isValid = false;
        }
    }

    /**
     * Read a keytab file. Returns a new object and save it into cache when
     * new content (modified since last read) is available. If keytab file is
     * invalid, the old object will be returned. This is a safeguard for
     * partial-written keytab files or non-stable network. Please note that
     * a missing keytab is valid, which is equivalent to an empty keytab.
     *
     * @param s file name of keytab, must not be null
     * @return the keytab object, can be invalid, but never null.
     */
    private synchronized static KeyTab getInstance0(String s) {
        long lm = new File(s).lastModified();
        KeyTab old = map.get(s);
        if (old != null && old.isValid() && old.lastModified == lm) {
            return old;
        }
        KeyTab ktab = new KeyTab(s);
        if (ktab.isValid()) {               // A valid new keytab
            map.put(s, ktab);
            return ktab;
        } else if (old != null) {           // An existing old one
            return old;
        } else {
            return ktab;                    // first read is invalid
        }
    }

    /**
     * Gets a KeyTab object.
     * @param s the key tab file name.
     * @return the KeyTab object, never null.
     */
    public static KeyTab getInstance(String s) {
        if (s == null) {
            return getInstance();
        } else {
            return getInstance0(normalize(s));
        }
    }

    /**
     * Gets a KeyTab object.
     * @param file the key tab file.
     * @return the KeyTab object, never null.
     */
    public static KeyTab getInstance(File file) {
        if (file == null) {
            return getInstance();
        } else {
            return getInstance0(file.getPath());
        }
    }

    /**
     * Gets the default KeyTab object.
     * @return the KeyTab object, never null.
     */
    public static KeyTab getInstance() {
        return getInstance(getDefaultTabName());
    }

    public boolean isMissing() {
        return isMissing;
    }

    public boolean isValid() {
        return isValid;
    }

    /**
     * The location of keytab file will be read from the configuration file
     * If it is not specified, consider user.home as the keytab file's
     * default location.
     * @return never null
     */
    private static String getDefaultTabName() {
        if (defaultTabName != null) {
            return defaultTabName;
        } else {
            String kname = null;
            try {
                String keytab_names = Config.getInstance().get
                        ("libdefaults", "default_keytab_name");
                if (keytab_names != null) {
                    StringTokenizer st = new StringTokenizer(keytab_names, " ");
                    while (st.hasMoreTokens()) {
                        kname = normalize(st.nextToken());
                        if (new File(kname).exists()) {
                            break;
                        }
                    }
                }
            } catch (KrbException e) {
                kname = null;
            }

            if (kname == null) {
                String user_home = GetPropertyAction
                        .privilegedGetProperty("user.home");

                if (user_home == null) {
                    user_home = GetPropertyAction
                            .privilegedGetProperty("user.dir");
                }

                kname = user_home + File.separator  + "krb5.keytab";
            }
            defaultTabName = kname;
            return kname;
        }
    }

    /**
     * Normalizes some common keytab name formats into the bare file name.
     * For example, FILE:/etc/krb5.keytab to /etc/krb5.keytab
     * @param name never null
     * @return never null
     */
    // This method is used in this class and Krb5LoginModule
    public static String normalize(String name) {
        String kname;
        if ((name.length() >= 5) &&
            (name.substring(0, 5).equalsIgnoreCase("FILE:"))) {
            kname = name.substring(5);
        } else if ((name.length() >= 9) &&
                (name.substring(0, 9).equalsIgnoreCase("ANY:FILE:"))) {
            // this format found in MIT's krb5.ini.
            kname = name.substring(9);
        } else if ((name.length() >= 7) &&
                (name.substring(0, 7).equalsIgnoreCase("SRVTAB:"))) {
            // this format found in MIT's krb5.ini.
            kname = name.substring(7);
        } else
            kname = name;
        return kname;
    }

    private void load(KeyTabInputStream kis)
        throws IOException, RealmException {

        entries.clear();
        kt_vno = kis.readVersion();
        if (kt_vno == KRB5_KT_VNO_1) {
            kis.setNativeByteOrder();
        }
        int entryLength = 0;
        KeyTabEntry entry;
        while (kis.available() > 0) {
            entryLength = kis.readEntryLength();
            entry = kis.readEntry(entryLength, kt_vno);
            if (DEBUG) {
                System.out.println(">>> KeyTab: load() entry length: " +
                        entryLength + "; type: " +
                        (entry != null? entry.keyType : 0));
            }
            if (entry != null)
                entries.addElement(entry);
        }
    }

    /**
     * Returns a principal name in this keytab. Used by
     * {@link ServiceCreds#getKKeys()}.
     */
    public PrincipalName getOneName() {
        int size = entries.size();
        return size > 0 ? entries.elementAt(size-1).service : null;
    }

    /**
     * Reads all keys for a service from the keytab file that have
     * etypes that have been configured for use.
     * @param service the PrincipalName of the requested service
     * @return an array containing all the service keys, never null
     */
    public EncryptionKey[] readServiceKeys(PrincipalName service) {
        KeyTabEntry entry;
        EncryptionKey key;
        int size = entries.size();
        ArrayList<EncryptionKey> keys = new ArrayList<>(size);
        if (DEBUG) {
            System.out.println("Looking for keys for: " + service);
        }
        for (int i = size-1; i >= 0; i--) {
            entry = entries.elementAt(i);
            if (entry.service.match(service)) {
                if (EType.isSupported(entry.keyType)) {
                    key = new EncryptionKey(entry.keyblock,
                                        entry.keyType,
                                        entry.keyVersion);
                    keys.add(key);
                    if (DEBUG) {
                        System.out.println("Added key: " + entry.keyType +
                            ", version: " + entry.keyVersion);
                    }
                } else if (DEBUG) {
                    System.out.println("Found unsupported keytype (" +
                        entry.keyType + ") for " + service);
                }
            }
        }
        size = keys.size();
        EncryptionKey[] retVal = keys.toArray(new EncryptionKey[size]);

        // Sort the keys by kvno. Sometimes we must choose a single key (say,
        // generate encrypted timestamp in AS-REQ). A key with a higher KVNO
        // sounds like a newer one.
        Arrays.sort(retVal, new Comparator<EncryptionKey>() {
            @Override
            public int compare(EncryptionKey o1, EncryptionKey o2) {
                return o2.getKeyVersionNumber().intValue()
                        - o1.getKeyVersionNumber().intValue();
            }
        });

        return retVal;
    }



    /**
     * Searches for the service entry in the keytab file.
     * The etype of the key must be one that has been configured
     * to be used.
     * @param service the PrincipalName of the requested service.
     * @return true if the entry is found, otherwise, return false.
     */
    public boolean findServiceEntry(PrincipalName service) {
        KeyTabEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.elementAt(i);
            if (entry.service.match(service)) {
                if (EType.isSupported(entry.keyType)) {
                    return true;
                } else if (DEBUG) {
                    System.out.println("Found unsupported keytype (" +
                        entry.keyType + ") for " + service);
                }
            }
        }
        return false;
    }

    public String tabName() {
        return tabName;
    }

    /////////////////// THE WRITE SIDE ///////////////////////
    /////////////// only used by ktab tool //////////////////

    /**
     * Adds a new entry in the key table.
     * @param service the service which will have a new entry in the key table.
     * @param psswd the password which generates the key.
     * @param kvno the kvno to use, -1 means automatic increasing
     * @param append false if entries with old kvno would be removed.
     * Note: if kvno is not -1, entries with the same kvno are always removed
     */
    public void addEntry(PrincipalName service, char[] psswd,
            int kvno, boolean append) throws KrbException {
        addEntry(service, service.getSalt(), psswd, kvno, append);
    }

    // Called by KDC test
    public void addEntry(PrincipalName service, String salt, char[] psswd,
            int kvno, boolean append) throws KrbException {

        EncryptionKey[] encKeys = EncryptionKey.acquireSecretKeys(
            psswd, salt);

        // There should be only one maximum KVNO value for all etypes, so that
        // all added keys can have the same KVNO.

        int maxKvno = 0;    // only useful when kvno == -1
        for (int i = entries.size()-1; i >= 0; i--) {
            KeyTabEntry e = entries.get(i);
            if (e.service.match(service)) {
                if (e.keyVersion > maxKvno) {
                    maxKvno = e.keyVersion;
                }
                if (!append || e.keyVersion == kvno) {
                    entries.removeElementAt(i);
                }
            }
        }
        if (kvno == -1) {
            kvno = maxKvno + 1;
        }

        for (int i = 0; encKeys != null && i < encKeys.length; i++) {
            int keyType = encKeys[i].getEType();
            byte[] keyValue = encKeys[i].getBytes();

            KeyTabEntry newEntry = new KeyTabEntry(service,
                            service.getRealm(),
                            new KerberosTime(System.currentTimeMillis()),
                                               kvno, keyType, keyValue);
            entries.addElement(newEntry);
        }
    }

    /**
     * Gets the list of service entries in key table.
     * @return array of <code>KeyTabEntry</code>.
     */
    public KeyTabEntry[] getEntries() {
        KeyTabEntry[] kentries = new KeyTabEntry[entries.size()];
        for (int i = 0; i < kentries.length; i++) {
            kentries[i] = entries.elementAt(i);
        }
        return kentries;
    }

    /**
     * Creates a new default key table.
     */
    public synchronized static KeyTab create()
        throws IOException, RealmException {
        String dname = getDefaultTabName();
        return create(dname);
    }

    /**
     * Creates a new default key table.
     */
    public synchronized static KeyTab create(String name)
        throws IOException, RealmException {

        try (KeyTabOutputStream kos =
                new KeyTabOutputStream(new FileOutputStream(name))) {
            kos.writeVersion(KRB5_KT_VNO);
        }
        return new KeyTab(name);
    }

    /**
     * Saves the file at the directory.
     */
    public synchronized void save() throws IOException {
        try (KeyTabOutputStream kos =
                new KeyTabOutputStream(new FileOutputStream(tabName))) {
            kos.writeVersion(kt_vno);
            for (int i = 0; i < entries.size(); i++) {
                kos.writeEntry(entries.elementAt(i));
            }
        }
    }

    /**
     * Removes entries from the key table.
     * @param service the service <code>PrincipalName</code>.
     * @param etype the etype to match, remove all if -1
     * @param kvno what kvno to remove, -1 for all, -2 for old
     * @return the number of entries deleted
     */
    public int deleteEntries(PrincipalName service, int etype, int kvno) {
        int count = 0;

        // Remember the highest KVNO for each etype. Used for kvno == -2
        Map<Integer,Integer> highest = new HashMap<>();

        for (int i = entries.size()-1; i >= 0; i--) {
            KeyTabEntry e = entries.get(i);
            if (service.match(e.getService())) {
                if (etype == -1 || e.keyType == etype) {
                    if (kvno == -2) {
                        // Two rounds for kvno == -2. In the first round (here),
                        // only find out highest KVNO for each etype
                        if (highest.containsKey(e.keyType)) {
                            int n = highest.get(e.keyType);
                            if (e.keyVersion > n) {
                                highest.put(e.keyType, e.keyVersion);
                            }
                        } else {
                            highest.put(e.keyType, e.keyVersion);
                        }
                    } else if (kvno == -1 || e.keyVersion == kvno) {
                        entries.removeElementAt(i);
                        count++;
                    }
                }
            }
        }

        // Second round for kvno == -2, remove old entries
        if (kvno == -2) {
            for (int i = entries.size()-1; i >= 0; i--) {
                KeyTabEntry e = entries.get(i);
                if (service.match(e.getService())) {
                    if (etype == -1 || e.keyType == etype) {
                        int n = highest.get(e.keyType);
                        if (e.keyVersion != n) {
                            entries.removeElementAt(i);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Creates key table file version.
     * @param file the key table file.
     * @exception IOException
     */
    public synchronized void createVersion(File file) throws IOException {
        try (KeyTabOutputStream kos =
                new KeyTabOutputStream(new FileOutputStream(file))) {
            kos.write16(KRB5_KT_VNO);
        }
    }
}
