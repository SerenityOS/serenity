/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.tools;

import java.net.InetAddress;
import java.util.List;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.ccache.*;
import sun.security.krb5.internal.ktab.*;
import sun.security.krb5.internal.crypto.EType;

/**
 * This class can execute as a command-line tool to list entries in
 * credential cache and key tab.
 *
 * @author Yanni Zhang
 * @author Ram Marti
 */
public class Klist {
    Object target;
    // for credentials cache, options are 'f', 'e', 'a' and 'n';
    // for  keytab, optionsare 't' and 'K' and 'e'
    char[] options = new char[4];
    String name;       // the name of credentials cache and keytable.
    char action;       // actions would be 'c' for credentials cache
    // and 'k' for keytable.
    private static boolean DEBUG = Krb5.DEBUG;

    /**
     * The main program that can be invoked at command line.
     * <br>Usage: klist
     * [[-c] [-f] [-e] [-a [-n]]] [-k [-t] [-K]] [name]
     * -c specifies that credential cache is to be listed
     * -k specifies that key tab is to be listed
     * name name of the credentials cache or keytab
     * <br>available options for credential caches:
     * <ul>
     * <li><b>-f</b>  shows credentials flags
     * <li><b>-e</b>  shows the encryption type
     * <li><b>-a</b>  shows addresses
     * <li><b>-n</b>  do not reverse-resolve addresses
     * </ul>
     * available options for keytabs:
     * <ul>
     * <li><b>-t</b> shows keytab entry timestamps
     * <li><b>-K</b> shows keytab entry DES keys
     * </ul>
     */
    public static void main(String[] args) {
        Klist klist = new Klist();
        if ((args == null) || (args.length == 0)) {
            klist.action = 'c'; // default will list default credentials cache.
        } else {
            klist.processArgs(args);
        }
        switch (klist.action) {
        case 'c':
            if (klist.name == null) {
                klist.target = CredentialsCache.getInstance();
                klist.name = CredentialsCache.cacheName();
            } else
                klist.target = CredentialsCache.getInstance(klist.name);

            if (klist.target != null)  {
                klist.displayCache();
            } else {
                klist.displayMessage("Credentials cache");
                System.exit(-1);
            }
            break;
        case 'k':
            KeyTab ktab = KeyTab.getInstance(klist.name);
            if (ktab.isMissing()) {
                System.out.println("KeyTab " + klist.name + " not found.");
                System.exit(-1);
            } else if (!ktab.isValid()) {
                System.out.println("KeyTab " + klist.name
                        + " format not supported.");
                System.exit(-1);
            }
            klist.target = ktab;
            klist.name = ktab.tabName();
            klist.displayTab();
            break;
        default:
            if (klist.name != null) {
                klist.printHelp();
                System.exit(-1);
            } else {
                klist.target = CredentialsCache.getInstance();
                klist.name = CredentialsCache.cacheName();
                if (klist.target != null) {
                    klist.displayCache();
                } else {
                    klist.displayMessage("Credentials cache");
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * Parses the command line arguments.
     */
    void processArgs(String[] args) {
        Character arg;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-?") ||
                args[i].equals("-h") ||
                args[i].equals("--help")) {
                printHelp();
                System.exit(0);
            }
            if ((args[i].length() >= 2) && (args[i].startsWith("-"))) {
                arg = Character.valueOf(args[i].charAt(1));
                switch (arg.charValue()) {
                case 'c':
                    action = 'c';
                    break;
                case 'k':
                    action = 'k';
                    break;
                case 'a':
                    options[2] = 'a';
                    break;
                case 'n':
                    options[3] = 'n';
                    break;
                case 'f':
                    options[1] = 'f';
                    break;
                case 'e':
                    options[0] = 'e';
                    break;
                case 'K':
                    options[1] = 'K';
                    break;
                case 't':
                    options[2] = 't';
                    break;
                default:
                    printHelp();
                    System.exit(-1);
                }

            } else {
                if (!args[i].startsWith("-") && (i == args.length - 1)) {
                    // the argument is the last one.
                    name = args[i];
                    arg = null;
                } else {
                    printHelp(); // incorrect input format.
                    System.exit(-1);
                }
            }
        }
    }

    void displayTab() {
        KeyTab table = (KeyTab)target;
        KeyTabEntry[] entries = table.getEntries();
        if (entries.length == 0) {
            System.out.println("\nKey tab: " + name +
                               ", " + " 0 entries found.\n");
        } else {
            if (entries.length == 1)
                System.out.println("\nKey tab: " + name +
                                   ", " + entries.length + " entry found.\n");
            else
                System.out.println("\nKey tab: " + name + ", " +
                                   entries.length + " entries found.\n");
            for (int i = 0; i < entries.length; i++) {
                System.out.println("[" + (i + 1) + "] " +
                                   "Service principal: "  +
                                   entries[i].getService().toString());
                System.out.println("\t KVNO: " +
                                   entries[i].getKey().getKeyVersionNumber());
                if (options[0] == 'e') {
                    EncryptionKey key = entries[i].getKey();
                    System.out.println("\t Key type: " +
                                       key.getEType());
                }
                if (options[1] == 'K') {
                    EncryptionKey key = entries[i].getKey();
                    System.out.println("\t Key: " +
                                       entries[i].getKeyString());
                }
                if (options[2] == 't') {
                    System.out.println("\t Time stamp: " +
                            format(entries[i].getTimeStamp()));
                }
            }
        }
    }

    void displayCache() {
        CredentialsCache cache = (CredentialsCache)target;
        sun.security.krb5.internal.ccache.Credentials[] creds =
            cache.getCredsList();
        if (creds == null) {
            System.out.println ("No credentials available in the cache " +
                                name);
            System.exit(-1);
        }
        System.out.println("\nCredentials cache: " +  name);
        String defaultPrincipal = cache.getPrimaryPrincipal().toString();
        int num = creds.length;

        if (num == 1)
            System.out.println("\nDefault principal: " +
                               defaultPrincipal + ", " +
                               creds.length + " entry found.\n");
        else
            System.out.println("\nDefault principal: " +
                               defaultPrincipal + ", " +
                               creds.length + " entries found.\n");
        if (creds != null) {
            for (int i = 0; i < creds.length; i++) {
                try {
                    String starttime;
                    String endtime;
                    String renewTill;
                    String servicePrincipal;
                    PrincipalName servicePrincipal2;
                    String clientPrincipal;
                    if (creds[i].getStartTime() != null) {
                        starttime = format(creds[i].getStartTime());
                    } else {
                        starttime = format(creds[i].getAuthTime());
                    }
                    endtime = format(creds[i].getEndTime());
                    servicePrincipal =
                        creds[i].getServicePrincipal().toString();
                    System.out.println("[" + (i + 1) + "] " +
                                       " Service Principal:  " +
                                       servicePrincipal);
                    servicePrincipal2 =
                            creds[i].getServicePrincipal2();
                    if (servicePrincipal2 != null) {
                        System.out.println("     Second Service:     "
                                + servicePrincipal2);
                    }
                    clientPrincipal =
                            creds[i].getClientPrincipal().toString();
                    if (!clientPrincipal.equals(defaultPrincipal)) {
                        System.out.println("     Client Principal:   " +
                                clientPrincipal);
                    }
                    System.out.println("     Valid starting:     " + starttime);
                    System.out.println("     Expires:            " + endtime);
                    if (creds[i].getRenewTill() != null) {
                        renewTill = format(creds[i].getRenewTill());
                        System.out.println(
                                "     Renew until:        " + renewTill);
                    }
                    if (options[0] == 'e') {
                        String eskey = EType.toString(creds[i].getEType());
                        String etkt = EType.toString(creds[i].getTktEType());
                        if (creds[i].getTktEType2() == 0) {
                            System.out.println("     EType (skey, tkt):  "
                                    + eskey + ", " + etkt);
                        } else {
                            String etkt2 = EType.toString(creds[i].getTktEType2());
                            System.out.println("     EType (skey, tkts): "
                                    + eskey + ", " + etkt
                                    + ", " + etkt2);
                        }
                    }
                    if (options[1] == 'f') {
                        System.out.println("     Flags:              " +
                                           creds[i].getTicketFlags().toString());
                    }
                    if (options[2] == 'a') {
                        boolean first = true;
                        InetAddress[] caddr
                                = creds[i].setKrbCreds().getClientAddresses();
                        if (caddr != null) {
                            for (InetAddress ia: caddr) {
                                String out;
                                if (options[3] == 'n') {
                                    out = ia.getHostAddress();
                                } else {
                                    out = ia.getCanonicalHostName();
                                }
                                System.out.println("     " +
                                        (first?"Addresses:":"          ") +
                                        "       " + out);
                                first = false;
                            }
                        } else {
                            System.out.println("     [No host addresses info]");
                        }
                    }
                } catch (RealmException e) {
                    System.out.println("Error reading principal from "+
                                       "the entry.");
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                    System.exit(-1);
                }
            }
        } else {
            System.out.println("\nNo entries found.");
        }

        List<CredentialsCache.ConfigEntry> configEntries
                = cache.getConfigEntries();
        if (configEntries != null && !configEntries.isEmpty()) {
            System.out.println("\nConfig entries:");
            for (CredentialsCache.ConfigEntry e : configEntries) {
                System.out.println("     " + e);
            }
        }
    }

    void displayMessage(String target) {
        if (name == null) {
            System.out.println("Default " + target + " not found.");
        } else {
            System.out.println(target + " " + name + " not found.");
        }
    }
    /**
     * Reformats the date from the form -
     *     dow mon dd hh:mm:ss zzz yyyy to mon/dd/yyyy hh:mm
     * where dow is the day of the week, mon is the month,
     * dd is the day of the month, hh is the hour of
     * the day, mm is the minute within the hour,
     * ss is the second within the minute, zzz is the time zone,
     * and yyyy is the year.
     * @param date the string form of Date object.
     */
    private String format(KerberosTime kt) {
        String date = kt.toDate().toString();
        return (date.substring(4, 7) + " " + date.substring(8, 10) +
                ", " + date.substring(24)
                + " " + date.substring(11, 19));
    }
    /**
     * Prints out the help information.
     */
    void printHelp() {
        System.out.println("\nUsage: klist " +
                           "[[-c] [-f] [-e] [-a [-n]]] [-k [-t] [-K]] [name]");
        System.out.println("   name\t name of credentials cache or " +
                           " keytab with the prefix. File-based cache or "
                           + "keytab's prefix is FILE:.");
        System.out.println("   -c specifies that credential cache is to be " +
                           "listed");
        System.out.println("   -k specifies that key tab is to be listed");
        System.out.println("   options for credentials caches:");
        System.out.println("\t-f \t shows credentials flags");
        System.out.println("\t-e \t shows the encryption type");
        System.out.println("\t-a \t shows addresses");
        System.out.println("\t  -n \t   do not reverse-resolve addresses");
        System.out.println("   options for keytabs:");
        System.out.println("\t-t \t shows keytab entry timestamps");
        System.out.println("\t-K \t shows keytab entry key value");
        System.out.println("\t-e \t shows keytab entry key type");
    }
}
