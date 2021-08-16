/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.ccache.*;
import java.io.IOException;
import java.util.Arrays;
import sun.security.util.Password;
import javax.security.auth.kerberos.KeyTab;

/**
 * Kinit tool for obtaining Kerberos v5 tickets.
 *
 * @author Yanni Zhang
 * @author Ram Marti
 */
public class Kinit {

    private KinitOptions options;
    private static final boolean DEBUG = Krb5.DEBUG;

    /**
     * The main method is used to accept user command line input for ticket
     * request. Read {@link KinitOptions#printHelp} for usages or call
     *    java sun.security.krb5.internal.tools.Kinit -help
     * to bring up help menu.
     * <p>
     * We currently support only file-based credentials cache to
     * store the tickets obtained from the KDC.
     * By default, for all Unix platforms a cache file named
     * {@code /tmp/krb5cc_<uid>} will be generated. The {@code <uid>} is the
     * numeric user identifier.
     * For all other platforms, a cache file named
     * {@code <USER_HOME>/krb5cc_<USER_NAME>} would be generated.
     * <p>
     * {@code <USER_HOME>} is obtained from {@code java.lang.System}
     * property <i>user.home</i>.
     * {@code <USER_NAME>} is obtained from {@code java.lang.System}
     * property <i>user.name</i>.
     * If {@code <USER_HOME>} is null the cache file would be stored in
     * the current directory that the program is running from.
     * {@code <USER_NAME>} is operating system's login username.
     * It could be different from user's principal name.
     * <p>
     * For instance, on Windows NT, it could be
     * {@code c:\winnt\profiles\duke\krb5cc_duke}, in
     * which duke is the {@code <USER_NAME>} and {@code c:\winnt\profile\duke} is the
     * {@code <USER_HOME>}.
     * <p>
     * A single user could have multiple principal names,
     * but the primary principal of the credentials cache could only be one,
     * which means one cache file could only store tickets for one
     * specific user principal. If the user switches
     * the principal name at the next Kinit, the cache file generated for the
     * new ticket would overwrite the old cache file by default.
     * To avoid overwriting, you need to specify
     * a different cache file name when you request a
     * new ticket.
     * <p>
     * You can specify the location of the cache file by using the -c option
     */

    public static void main(String[] args) {
        try {
            Kinit self = new Kinit(args);
        }
        catch (Exception e) {
            String msg = null;
            if (e instanceof KrbException) {
                msg = ((KrbException)e).krbErrorMessage() + " " +
                    ((KrbException)e).returnCodeMessage();
            } else  {
                msg = e.getMessage();
            }
            if (msg != null) {
                System.err.println("Exception: " + msg);
            } else {
                System.out.println("Exception: " + e);
            }
            e.printStackTrace();
            System.exit(-1);
        }
        return;
    }

    /**
     * Constructs a new Kinit object.
     * @param args array of ticket request options.
     * Avaiable options are: -f, -p, -c, principal, password.
     * @exception IOException if an I/O error occurs.
     * @exception RealmException if the Realm could not be instantiated.
     * @exception KrbException if error occurs during Kerberos operation.
     */
    private Kinit(String[] args)
        throws IOException, RealmException, KrbException {
        if (args == null || args.length == 0) {
            options = new KinitOptions();
        } else {
            options = new KinitOptions(args);
        }
        switch (options.action) {
            case 1:
                acquire();
                break;
            case 2:
                renew();
                break;
            default:
                throw new KrbException("kinit does not support action "
                        + options.action);
        }
    }

    private void renew()
            throws IOException, RealmException, KrbException {

        PrincipalName principal = options.getPrincipal();
        String realm = principal.getRealmAsString();
        CredentialsCache cache = CredentialsCache.getInstance(options.cachename);

        if (cache == null) {
            throw new IOException("Unable to find existing cache file " +
                    options.cachename);
        }
        sun.security.krb5.internal.ccache.Credentials credentials =
                cache.getCreds(PrincipalName.tgsService(realm, realm));

        credentials = credentials.setKrbCreds()
                .renew()
                .toCCacheCreds();

        cache = CredentialsCache.create(principal, options.cachename);
        if (cache == null) {
            throw new IOException("Unable to create the cache file " +
                    options.cachename);
        }
        cache.update(credentials);
        cache.save();
    }

    private void acquire()
            throws IOException, RealmException, KrbException {

        String princName = null;
        PrincipalName principal = options.getPrincipal();
        if (principal != null) {
            princName = principal.toString();
        }
        KrbAsReqBuilder builder;
        if (DEBUG) {
            System.out.println("Principal is " + principal);
        }
        char[] psswd = options.password;
        boolean useKeytab = options.useKeytabFile();
        if (!useKeytab) {
            if (princName == null) {
                throw new IllegalArgumentException
                    (" Can not obtain principal name");
            }
            if (psswd == null) {
                System.out.print("Password for " + princName + ":");
                System.out.flush();
                psswd = Password.readPassword(System.in);
                if (DEBUG) {
                    System.out.println(">>> Kinit console input " +
                        new String(psswd));
                }
            }
            builder = new KrbAsReqBuilder(principal, psswd);
        } else {
            if (DEBUG) {
                System.out.println(">>> Kinit using keytab");
            }
            if (princName == null) {
                throw new IllegalArgumentException
                    ("Principal name must be specified.");
            }
            String ktabName = options.keytabFileName();
            if (ktabName != null) {
                if (DEBUG) {
                    System.out.println(
                                       ">>> Kinit keytab file name: " + ktabName);
                }
            }

            builder = new KrbAsReqBuilder(principal, ktabName == null
                    ? KeyTab.getInstance()
                    : KeyTab.getInstance(new File(ktabName)));
        }

        KDCOptions opt = new KDCOptions();
        setOptions(KDCOptions.FORWARDABLE, options.forwardable, opt);
        setOptions(KDCOptions.PROXIABLE, options.proxiable, opt);
        builder.setOptions(opt);
        String realm = options.getKDCRealm();
        if (realm == null) {
            realm = Config.getInstance().getDefaultRealm();
        }

        if (DEBUG) {
            System.out.println(">>> Kinit realm name is " + realm);
        }

        PrincipalName sname = PrincipalName.tgsService(realm, realm);
        builder.setTarget(sname);

        if (DEBUG) {
            System.out.println(">>> Creating KrbAsReq");
        }

        if (options.getAddressOption())
            builder.setAddresses(HostAddresses.getLocalAddresses());

        builder.setTill(options.lifetime);
        builder.setRTime(options.renewable_lifetime);

        builder.action();

        sun.security.krb5.internal.ccache.Credentials credentials =
            builder.getCCreds();
        builder.destroy();

        // we always create a new cache and store the ticket we get
        CredentialsCache cache =
            CredentialsCache.create(principal, options.cachename);
        if (cache == null) {
           throw new IOException("Unable to create the cache file " +
                                 options.cachename);
        }
        cache.update(credentials);
        cache.save();

        if (options.password == null) {
            // Assume we're running interactively
            System.out.println("New ticket is stored in cache file " +
                               options.cachename);
         } else {
             Arrays.fill(options.password, '0');
         }

        // clear the password
        if (psswd != null) {
            Arrays.fill(psswd, '0');
        }
        options = null; // release reference to options
    }

    private static void setOptions(int flag, int option, KDCOptions opt) {
        switch (option) {
        case 0:
            break;
        case -1:
            opt.set(flag, false);
            break;
        case 1:
            opt.set(flag, true);
        }
    }
}
