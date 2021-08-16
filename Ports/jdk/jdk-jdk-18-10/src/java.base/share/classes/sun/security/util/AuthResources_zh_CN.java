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
public class AuthResources_zh_CN extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // NT principals
        {"invalid.null.input.value", "\u65E0\u6548\u7684\u7A7A\u8F93\u5165: {0}"},
        {"NTDomainPrincipal.name", "NTDomainPrincipal: {0}"},
        {"NTNumericCredential.name", "NTNumericCredential: {0}"},
        {"Invalid.NTSid.value", "\u65E0\u6548\u7684 NTSid \u503C"},
        {"NTSid.name", "NTSid: {0}"},
        {"NTSidDomainPrincipal.name", "NTSidDomainPrincipal: {0}"},
        {"NTSidGroupPrincipal.name", "NTSidGroupPrincipal: {0}"},
        {"NTSidPrimaryGroupPrincipal.name", "NTSidPrimaryGroupPrincipal: {0}"},
        {"NTSidUserPrincipal.name", "NTSidUserPrincipal: {0}"},
        {"NTUserPrincipal.name", "NTUserPrincipal: {0}"},

        // UnixPrincipals
        {"UnixNumericGroupPrincipal.Primary.Group.name",
                "UnixNumericGroupPrincipal [\u4E3B\u7EC4]: {0}"},
        {"UnixNumericGroupPrincipal.Supplementary.Group.name",
                "UnixNumericGroupPrincipal [\u8865\u5145\u7EC4]: {0}"},
        {"UnixNumericUserPrincipal.name", "UnixNumericUserPrincipal: {0}"},
        {"UnixPrincipal.name", "UnixPrincipal: {0}"},

        // com.sun.security.auth.login.ConfigFile
        {"Unable.to.properly.expand.config", "\u65E0\u6CD5\u6B63\u786E\u6269\u5C55{0}"},
        {"extra.config.No.such.file.or.directory.",
                "{0} (\u6CA1\u6709\u8FD9\u6837\u7684\u6587\u4EF6\u6216\u76EE\u5F55)"},
        {"Configuration.Error.No.such.file.or.directory",
                "\u914D\u7F6E\u9519\u8BEF:\n\t\u6CA1\u6709\u6B64\u6587\u4EF6\u6216\u76EE\u5F55"},
        {"Configuration.Error.Invalid.control.flag.flag",
                "\u914D\u7F6E\u9519\u8BEF: \n\t\u65E0\u6548\u7684\u63A7\u5236\u6807\u8BB0, {0}"},
        {"Configuration.Error.Can.not.specify.multiple.entries.for.appName",
            "\u914D\u7F6E\u9519\u8BEF:\n\t\u65E0\u6CD5\u6307\u5B9A{0}\u7684\u591A\u4E2A\u6761\u76EE"},
        {"Configuration.Error.expected.expect.read.end.of.file.",
                "\u914D\u7F6E\u9519\u8BEF: \n\t\u5E94\u4E3A [{0}], \u8BFB\u53D6\u7684\u662F [\u6587\u4EF6\u7ED3\u5C3E]"},
        {"Configuration.Error.Line.line.expected.expect.found.value.",
            "\u914D\u7F6E\u9519\u8BEF: \n\t\u884C {0}: \u5E94\u4E3A [{1}], \u627E\u5230 [{2}]"},
        {"Configuration.Error.Line.line.expected.expect.",
            "\u914D\u7F6E\u9519\u8BEF: \n\t\u884C {0}: \u5E94\u4E3A [{1}]"},
        {"Configuration.Error.Line.line.system.property.value.expanded.to.empty.value",
            "\u914D\u7F6E\u9519\u8BEF: \n\t\u884C {0}: \u7CFB\u7EDF\u5C5E\u6027 [{1}] \u6269\u5C55\u5230\u7A7A\u503C"},

        // com.sun.security.auth.module.JndiLoginModule
        {"username.","\u7528\u6237\u540D: "},
        {"password.","\u53E3\u4EE4: "},

        // com.sun.security.auth.module.KeyStoreLoginModule
        {"Please.enter.keystore.information",
                "\u8BF7\u8F93\u5165\u5BC6\u94A5\u5E93\u4FE1\u606F"},
        {"Keystore.alias.","\u5BC6\u94A5\u5E93\u522B\u540D: "},
        {"Keystore.password.","\u5BC6\u94A5\u5E93\u53E3\u4EE4: "},
        {"Private.key.password.optional.",
            "\u79C1\u6709\u5BC6\u94A5\u53E3\u4EE4 (\u53EF\u9009): "},

        // com.sun.security.auth.module.Krb5LoginModule
        {"Kerberos.username.defUsername.",
                "Kerberos \u7528\u6237\u540D [{0}]: "},
        {"Kerberos.password.for.username.",
                "{0}\u7684 Kerberos \u53E3\u4EE4: "},
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
