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
public class AuthResources_de extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // NT principals
        {"invalid.null.input.value", "Ung\u00FCltige Nulleingabe: {0}"},
        {"NTDomainPrincipal.name", "NTDomainPrincipal: {0}"},
        {"NTNumericCredential.name", "NTNumericCredential: {0}"},
        {"Invalid.NTSid.value", "Ung\u00FCltiger NTSid-Wert"},
        {"NTSid.name", "NTSid: {0}"},
        {"NTSidDomainPrincipal.name", "NTSidDomainPrincipal: {0}"},
        {"NTSidGroupPrincipal.name", "NTSidGroupPrincipal: {0}"},
        {"NTSidPrimaryGroupPrincipal.name", "NTSidPrimaryGroupPrincipal: {0}"},
        {"NTSidUserPrincipal.name", "NTSidUserPrincipal: {0}"},
        {"NTUserPrincipal.name", "NTUserPrincipal: {0}"},

        // UnixPrincipals
        {"UnixNumericGroupPrincipal.Primary.Group.name",
                "UnixNumericGroupPrincipal [Prim\u00E4rgruppe]: {0}"},
        {"UnixNumericGroupPrincipal.Supplementary.Group.name",
                "UnixNumericGroupPrincipal [Zusatzgruppe]: {0}"},
        {"UnixNumericUserPrincipal.name", "UnixNumericUserPrincipal: {0}"},
        {"UnixPrincipal.name", "UnixPrincipal: {0}"},

        // com.sun.security.auth.login.ConfigFile
        {"Unable.to.properly.expand.config", "{0} kann nicht ordnungsgem\u00E4\u00DF erweitert werden"},
        {"extra.config.No.such.file.or.directory.",
                "{0} (Datei oder Verzeichnis nicht vorhanden)"},
        {"Configuration.Error.No.such.file.or.directory",
                "Konfigurationsfehler:\n\tDatei oder Verzeichnis nicht vorhanden"},
        {"Configuration.Error.Invalid.control.flag.flag",
                "Konfigurationsfehler:\n\tUng\u00FCltiges Steuerkennzeichen {0}"},
        {"Configuration.Error.Can.not.specify.multiple.entries.for.appName",
            "Konfigurationsfehler:\n\tEs k\u00F6nnen nicht mehrere Angaben f\u00FCr {0} gemacht werden."},
        {"Configuration.Error.expected.expect.read.end.of.file.",
                "Konfigurationsfehler:\n\t[{0}] erwartet, [Dateiende] gelesen"},
        {"Configuration.Error.Line.line.expected.expect.found.value.",
            "Konfigurationsfehler:\n\tZeile {0}: [{1}] erwartet, [{2}] gefunden"},
        {"Configuration.Error.Line.line.expected.expect.",
            "Konfigurationsfehler:\n\tZeile {0}: [{1}] erwartet"},
        {"Configuration.Error.Line.line.system.property.value.expanded.to.empty.value",
            "Konfigurationsfehler:\n\tZeile {0}: Systemeigenschaft [{1}] auf leeren Wert erweitert"},

        // com.sun.security.auth.module.JndiLoginModule
        {"username.","Benutzername: "},
        {"password.","Kennwort: "},

        // com.sun.security.auth.module.KeyStoreLoginModule
        {"Please.enter.keystore.information",
                "Geben Sie die Keystore-Informationen ein"},
        {"Keystore.alias.","Keystore-Alias: "},
        {"Keystore.password.","Keystore-Kennwort: "},
        {"Private.key.password.optional.",
            "Private Key-Kennwort (optional): "},

        // com.sun.security.auth.module.Krb5LoginModule
        {"Kerberos.username.defUsername.",
                "Kerberos-Benutzername [{0}]: "},
        {"Kerberos.password.for.username.",
                "Kerberos-Kennwort f\u00FCr {0}: "},
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
