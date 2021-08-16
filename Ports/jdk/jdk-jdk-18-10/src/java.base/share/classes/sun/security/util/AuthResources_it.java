/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

/**
 * <p> This class represents the <code>ResourceBundle</code>
 * for the following packages:
 *
 * <ol>
 * <li> com.sun.security.auth
 * <li> com.sun.security.auth.login
 * </ol>
 *
 */
public class AuthResources_it extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // NT principals
        {"invalid.null.input.value", "input nullo non valido: {0}"},
        {"NTDomainPrincipal.name", "NTDomainPrincipal: {0}"},
        {"NTNumericCredential.name", "NTNumericCredential: {0}"},
        {"Invalid.NTSid.value", "Valore NTSid non valido"},
        {"NTSid.name", "NTSid: {0}"},
        {"NTSidDomainPrincipal.name", "NTSidDomainPrincipal: {0}"},
        {"NTSidGroupPrincipal.name", "NTSidGroupPrincipal: {0}"},
        {"NTSidPrimaryGroupPrincipal.name", "NTSidPrimaryGroupPrincipal: {0}"},
        {"NTSidUserPrincipal.name", "NTSidUserPrincipal: {0}"},
        {"NTUserPrincipal.name", "NTUserPrincipal: {0}"},

        // UnixPrincipals
        {"UnixNumericGroupPrincipal.Primary.Group.name",
                "UnixNumericGroupPrincipal [gruppo primario]: {0}"},
        {"UnixNumericGroupPrincipal.Supplementary.Group.name",
                "UnixNumericGroupPrincipal [gruppo supplementare]: {0}"},
        {"UnixNumericUserPrincipal.name", "UnixNumericUserPrincipal: {0}"},
        {"UnixPrincipal.name", "UnixPrincipal: {0}"},

        // com.sun.security.auth.login.ConfigFile
        {"Unable.to.properly.expand.config", "Impossibile espandere correttamente {0}"},
        {"extra.config.No.such.file.or.directory.",
                "{0} (file o directory inesistente)"},
        {"Configuration.Error.No.such.file.or.directory",
                "Errore di configurazione:\n\tFile o directory inesistente"},
        {"Configuration.Error.Invalid.control.flag.flag",
                "Errore di configurazione:\n\tflag di controllo non valido, {0}"},
        {"Configuration.Error.Can.not.specify.multiple.entries.for.appName",
            "Errore di configurazione:\n\timpossibile specificare pi\u00F9 valori per {0}"},
        {"Configuration.Error.expected.expect.read.end.of.file.",
                "Errore di configurazione:\n\tprevisto [{0}], letto [end of file]"},
        {"Configuration.Error.Line.line.expected.expect.found.value.",
            "Errore di configurazione:\n\triga {0}: previsto [{1}], trovato [{2}]"},
        {"Configuration.Error.Line.line.expected.expect.",
            "Errore di configurazione:\n\triga {0}: previsto [{1}]"},
        {"Configuration.Error.Line.line.system.property.value.expanded.to.empty.value",
            "Errore di configurazione:\n\triga {0}: propriet\u00E0 di sistema [{1}] espansa a valore vuoto"},

        // com.sun.security.auth.module.JndiLoginModule
        {"username.","Nome utente: "},
        {"password.","Password: "},

        // com.sun.security.auth.module.KeyStoreLoginModule
        {"Please.enter.keystore.information",
                "Immettere le informazioni per il keystore"},
        {"Keystore.alias.","Alias keystore: "},
        {"Keystore.password.","Password keystore: "},
        {"Private.key.password.optional.",
            "Password chiave privata (opzionale): "},

        // com.sun.security.auth.module.Krb5LoginModule
        {"Kerberos.username.defUsername.",
                "Nome utente Kerberos [{0}]: "},
        {"Kerberos.password.for.username.",
                "Password Kerberos per {0}: "},
    };

    /**
     * Returns the contents of this <code>ResourceBundle</code>.
     *
     * <p>
     *
     * @return the contents of this <code>ResourceBundle</code>.
     */
    public Object[][] getContents() {
        return contents;
    }
}
