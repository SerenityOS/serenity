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
public class AuthResources_fr extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // NT principals
        {"invalid.null.input.value", "entr\u00E9e NULL non valide : {0}"},
        {"NTDomainPrincipal.name", "NTDomainPrincipal : {0}"},
        {"NTNumericCredential.name", "NTNumericCredential : {0}"},
        {"Invalid.NTSid.value", "Valeur de NTSid non valide"},
        {"NTSid.name", "NTSid : {0}"},
        {"NTSidDomainPrincipal.name", "NTSidDomainPrincipal : {0}"},
        {"NTSidGroupPrincipal.name", "NTSidGroupPrincipal : {0}"},
        {"NTSidPrimaryGroupPrincipal.name", "NTSidPrimaryGroupPrincipal : {0}"},
        {"NTSidUserPrincipal.name", "NTSidUserPrincipal : {0}"},
        {"NTUserPrincipal.name", "NTUserPrincipal : {0}"},

        // UnixPrincipals
        {"UnixNumericGroupPrincipal.Primary.Group.name",
                "UnixNumericGroupPrincipal [groupe principal] : {0}"},
        {"UnixNumericGroupPrincipal.Supplementary.Group.name",
                "UnixNumericGroupPrincipal [groupe suppl\u00E9mentaire] : {0}"},
        {"UnixNumericUserPrincipal.name", "UnixNumericUserPrincipal : {0}"},
        {"UnixPrincipal.name", "UnixPrincipal : {0}"},

        // com.sun.security.auth.login.ConfigFile
        {"Unable.to.properly.expand.config", "Impossible de d\u00E9velopper {0} correctement"},
        {"extra.config.No.such.file.or.directory.",
                "{0} (fichier ou r\u00E9pertoire inexistant)"},
        {"Configuration.Error.No.such.file.or.directory",
                "Erreur de configuration :\n\tCe fichier ou r\u00E9pertoire n'existe pas"},
        {"Configuration.Error.Invalid.control.flag.flag",
                "Erreur de configuration :\n\tIndicateur de contr\u00F4le non valide, {0}"},
        {"Configuration.Error.Can.not.specify.multiple.entries.for.appName",
            "Erreur de configuration :\n\tImpossible de sp\u00E9cifier des entr\u00E9es multiples pour {0}"},
        {"Configuration.Error.expected.expect.read.end.of.file.",
                "Erreur de configuration :\n\tAttendu : [{0}], lu : [fin de fichier]"},
        {"Configuration.Error.Line.line.expected.expect.found.value.",
            "Erreur de configuration :\n\tLigne {0} : attendu [{1}], trouv\u00E9 [{2}]"},
        {"Configuration.Error.Line.line.expected.expect.",
            "Erreur de configuration :\n\tLigne {0} : attendu [{1}]"},
        {"Configuration.Error.Line.line.system.property.value.expanded.to.empty.value",
            "Erreur de configuration :\n\tLigne {0} : propri\u00E9t\u00E9 syst\u00E8me [{1}] d\u00E9velopp\u00E9e en valeur vide"},

        // com.sun.security.auth.module.JndiLoginModule
        {"username.","nom utilisateur : "},
        {"password.","mot de passe : "},

        // com.sun.security.auth.module.KeyStoreLoginModule
        {"Please.enter.keystore.information",
                "Entrez les informations du fichier de cl\u00E9s"},
        {"Keystore.alias.","Alias du fichier de cl\u00E9s : "},
        {"Keystore.password.","Mot de passe pour fichier de cl\u00E9s : "},
        {"Private.key.password.optional.",
            "Mot de passe de la cl\u00E9 priv\u00E9e (facultatif) : "},

        // com.sun.security.auth.module.Krb5LoginModule
        {"Kerberos.username.defUsername.",
                "Nom utilisateur Kerberos [{0}] : "},
        {"Kerberos.password.for.username.",
                "Mot de passe Kerberos pour {0} : "},
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
