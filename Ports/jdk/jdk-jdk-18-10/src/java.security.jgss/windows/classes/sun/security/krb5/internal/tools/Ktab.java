/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.krb5.*;
import sun.security.krb5.internal.ktab.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import sun.security.krb5.internal.crypto.EType;
/**
 * This class can execute as a command-line tool to help the user manage
 * entries in the key table.
 * Available functions include list/add/update/delete service key(s).
 *
 * @author Yanni Zhang
 * @author Ram Marti
 */

public class Ktab {
    // KeyTabAdmin admin;
    KeyTab table;
    char action;
    String name;   // name and directory of key table
    String principal;
    boolean showEType;
    boolean showTime;
    int etype = -1;
    char[] password = null;

    boolean forced = false; // true if delete without prompt. Default false
    boolean append = false; // true if new keys are appended. Default false
    int vDel = -1;          // kvno to delete, -1 all, -2 old. Default -1
    int vAdd = -1;          // kvno to add. Default -1, means auto incremented

    /**
     * The main program that can be invoked at command line.
     * See {@link #printHelp} for usages.
     */
    public static void main(String[] args) {
        Ktab ktab = new Ktab();
        if ((args.length == 1) &&
            ((args[0].equalsIgnoreCase("-?")) ||
             (args[0].equalsIgnoreCase("-h")) ||
             (args[0].equalsIgnoreCase("--help")) ||
             // -help: legacy.
             (args[0].equalsIgnoreCase("-help")))) {
            ktab.printHelp();
            System.exit(0);
            return;
        } else if ((args == null) || (args.length == 0)) {
            ktab.action = 'l';
        } else {
            ktab.processArgs(args);
        }
        ktab.table = KeyTab.getInstance(ktab.name);
        if (ktab.table.isMissing() && ktab.action != 'a') {
            if (ktab.name == null) {
                System.out.println("No default key table exists.");
            } else {
                System.out.println("Key table " +
                        ktab.name + " does not exist.");
            }
            System.exit(-1);
        }
        if (!ktab.table.isValid()) {
            if (ktab.name == null) {
                System.out.println("The format of the default key table " +
                        " is incorrect.");
            } else {
                System.out.println("The format of key table " +
                        ktab.name + " is incorrect.");
            }
            System.exit(-1);
        }
        switch (ktab.action) {
        case 'l':
            ktab.listKt();
            break;
        case 'a':
            ktab.addEntry();
            break;
        case 'd':
            ktab.deleteEntry();
            break;
        default:
            ktab.error("A command must be provided");
        }
    }

    /**
     * Parses the command line arguments.
     */
    void processArgs(String[] args) {

        // Commands (should appear before options):
        //   -l
        //   -a <princ>
        //   -d <princ>
        // Options:
        //   -e <etype> (for -d)
        //   -e (for -l)
        //   -n <kvno>
        //   -k <keytab>
        //   -t
        //   -f
        //   -append
        // Optional extra arguments:
        //   password for -a
        //   [kvno|all|old] for -d

        boolean argAlreadyAppeared = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i].toLowerCase(Locale.US)) {

                    // Commands
                    case "-l":   // list
                        action = 'l';
                        break;
                    case "-a":   // add a new entry to keytab.
                        action = 'a';
                        if (++i >= args.length || args[i].startsWith("-")) {
                            error("A principal name must be specified after -a");
                        }
                        principal = args[i];
                        break;
                    case "-d":   // delete entries
                        action = 'd';
                        if (++i >= args.length || args[i].startsWith("-")) {
                            error("A principal name must be specified after -d");
                        }
                        principal = args[i];
                        break;

                        // Options
                    case "-e":
                        if (action == 'l') {    // list etypes
                            showEType = true;
                        } else if (action == 'd') { // delete etypes
                            if (++i >= args.length || args[i].startsWith("-")) {
                                error("An etype must be specified after -e");
                            }
                            try {
                                etype = Integer.parseInt(args[i]);
                                if (etype <= 0) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException nfe) {
                                error(args[i] + " is not a valid etype");
                            }
                        } else {
                            error(args[i] + " is not valid after -" + action);
                        }
                        break;
                    case "-n":   // kvno for -a
                        if (++i >= args.length || args[i].startsWith("-")) {
                            error("A KVNO must be specified after -n");
                        }
                        try {
                            vAdd = Integer.parseInt(args[i]);
                            if (vAdd < 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException nfe) {
                            error(args[i] + " is not a valid KVNO");
                        }
                        break;
                    case "-k":  // specify keytab to use
                        if (++i >= args.length || args[i].startsWith("-")) {
                            error("A keytab name must be specified after -k");
                        }
                        if (args[i].length() >= 5 &&
                            args[i].substring(0, 5).equalsIgnoreCase("FILE:")) {
                            name = args[i].substring(5);
                        } else {
                            name = args[i];
                        }
                        break;
                    case "-t":   // list timestamps
                        showTime = true;
                        break;
                    case "-f":   // force delete, no prompt
                        forced = true;
                        break;
                    case "-append": // -a, new keys append to file
                        append = true;
                        break;
                    default:
                        error("Unknown command: " + args[i]);
                        break;
                }
            } else {    // optional standalone arguments
                if (argAlreadyAppeared) {
                    error("Useless extra argument " + args[i]);
                }
                if (action == 'a') {
                    password = args[i].toCharArray();
                } else if (action == 'd') {
                    switch (args[i]) {
                        case "all": vDel = -1; break;
                        case "old": vDel = -2; break;
                        default: {
                            try {
                                vDel = Integer.parseInt(args[i]);
                                if (vDel < 0) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException nfe) {
                                error(args[i] + " is not a valid KVNO");
                            }
                        }
                    }
                } else {
                    error("Useless extra argument " + args[i]);
                }
                argAlreadyAppeared = true;
            }
        }
    }

    /**
     * Adds a service key to key table. If the specified key table does not
     * exist, the program will automatically generate
     * a new key table.
     */
    void addEntry() {
        PrincipalName pname = null;
        try {
            pname = new PrincipalName(principal);
        } catch (KrbException e) {
            System.err.println("Failed to add " + principal +
                               " to keytab.");
            e.printStackTrace();
            System.exit(-1);
        }
        if (password == null) {
            try {
                BufferedReader cis =
                    new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Password for " + pname.toString() + ":");
                System.out.flush();
                password = cis.readLine().toCharArray();
            } catch (IOException e) {
                System.err.println("Failed to read the password.");
                e.printStackTrace();
                System.exit(-1);
            }

        }
        try {
            // admin.addEntry(pname, password);
            table.addEntry(pname, password, vAdd, append);
            Arrays.fill(password, '0');  // clear password
            // admin.save();
            table.save();
            System.out.println("Done!");
            System.out.println("Service key for " + principal +
                               " is saved in " + table.tabName());

        } catch (KrbException e) {
            System.err.println("Failed to add " + principal + " to keytab.");
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("Failed to save new entry.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Lists key table name and entries in it.
     */
    void listKt() {
        System.out.println("Keytab name: " + table.tabName());
        KeyTabEntry[] entries = table.getEntries();
        if ((entries != null) && (entries.length > 0)) {
            String[][] output = new String[entries.length+1][showTime?3:2];
            int column = 0;
            output[0][column++] = "KVNO";
            if (showTime) output[0][column++] = "Timestamp";
            output[0][column++] = "Principal";
            for (int i = 0; i < entries.length; i++) {
                column = 0;
                output[i+1][column++] = entries[i].getKey().
                        getKeyVersionNumber().toString();
                if (showTime) output[i+1][column++] =
                        DateFormat.getDateTimeInstance(
                        DateFormat.SHORT, DateFormat.SHORT).format(
                        new Date(entries[i].getTimeStamp().getTime()));
                String princ = entries[i].getService().toString();
                if (showEType) {
                    int e = entries[i].getKey().getEType();
                    output[i+1][column++] = princ + " (" + e + ":" +
                            EType.toString(e) + ")";
                } else {
                    output[i+1][column++] = princ;
                }
            }
            int[] width = new int[column];
            for (int j=0; j<column; j++) {
                for (int i=0; i <= entries.length; i++) {
                    if (output[i][j].length() > width[j]) {
                        width[j] = output[i][j].length();
                    }
                }
                if (j != 0) width[j] = -width[j];
            }
            for (int j=0; j<column; j++) {
                System.out.printf("%" + width[j] + "s ", output[0][j]);
            }
            System.out.println();
            for (int j=0; j<column; j++) {
                for (int k=0; k<Math.abs(width[j]); k++) System.out.print("-");
                System.out.print(" ");
            }
            System.out.println();
            for (int i=0; i<entries.length; i++) {
                for (int j=0; j<column; j++) {
                    System.out.printf("%" + width[j] + "s ", output[i+1][j]);
                }
                System.out.println();
            }
        } else {
            System.out.println("0 entry.");
        }
    }

    /**
     * Deletes an entry from the key table.
     */
    void deleteEntry() {
        PrincipalName pname = null;
        try {
            pname = new PrincipalName(principal);
            if (!forced) {
                String answer;
                BufferedReader cis =
                    new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Are you sure you want to delete "+
                        "service key(s) for " + pname.toString() +
                        " (" + (etype==-1?"all etypes":("etype="+etype)) + ", " +
                        (vDel==-1?"all kvno":(vDel==-2?"old kvno":("kvno=" + vDel))) +
                        ") in " + table.tabName() + "? (Y/[N]): ");

                System.out.flush();
                answer = cis.readLine();
                if (answer.equalsIgnoreCase("Y") ||
                    answer.equalsIgnoreCase("Yes"));
                else {
                    // no error, the user did not want to delete the entry
                    System.exit(0);
                }
            }
        } catch (KrbException e) {
            System.err.println("Error occurred while deleting the entry. "+
                               "Deletion failed.");
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("Error occurred while deleting the entry. "+
                               " Deletion failed.");
            e.printStackTrace();
            System.exit(-1);
        }

        int count = table.deleteEntries(pname, etype, vDel);

        if (count == 0) {
            System.err.println("No matched entry in the keytab. " +
                               "Deletion fails.");
            System.exit(-1);
        } else {
            try {
                table.save();
            } catch (IOException e) {
                System.err.println("Error occurs while saving the keytab. " +
                                   "Deletion fails.");
                e.printStackTrace();
                System.exit(-1);
            }
            System.out.println("Done! " + count + " entries removed.");
        }
    }

    void error(String... errors) {
        for (String error: errors) {
            System.out.println("Error: " + error + ".");
        }
        printHelp();
        System.exit(-1);
    }
    /**
     * Prints out the help information.
     */
    void printHelp() {
        System.out.println("\nUsage: ktab <commands> <options>");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();
        System.out.println("-l [-e] [-t]\n"
                + "    list the keytab name and entries. -e with etype, -t with timestamp.");
        System.out.println("-a <principal name> [<password>] [-n <kvno>] [-append]\n"
                + "    add new key entries to the keytab for the given principal name with\n"
                + "    optional <password>. If a <kvno> is specified, new keys' Key Version\n"
                + "    Numbers equal to the value, otherwise, automatically incrementing\n"
                + "    the Key Version Numbers. If -append is specified, new keys are\n"
                + "    appended to the keytab, otherwise, old keys for the\n"
                + "    same principal are removed.");
        System.out.println("-d <principal name> [-f] [-e <etype>] [<kvno> | all | old]\n"
                + "    delete key entries from the keytab for the specified principal. If\n"
                + "    <kvno> is specified, delete keys whose Key Version Numbers match\n"
                + "    kvno. If \"all\" is specified, delete all keys. If \"old\" is specified,\n"
                + "    delete all keys except those with the highest kvno. Default action\n"
                + "    is \"all\". If <etype> is specified, only keys of this encryption type\n"
                + "    are deleted. <etype> should be specified as the numberic value etype\n"
                + "    defined in RFC 3961, section 8. A prompt to confirm the deletion is\n"
                + "    displayed unless -f is specified.");
        System.out.println();
        System.out.println("Common option(s):");
        System.out.println();
        System.out.println("-k <keytab name>\n"
                + "    specify keytab name and path with prefix FILE:");
    }
}
