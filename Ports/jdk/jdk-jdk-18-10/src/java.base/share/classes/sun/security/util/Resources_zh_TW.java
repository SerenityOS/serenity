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

package sun.security.util;

/**
 * This class represents the <code>ResourceBundle</code>
 * for javax.security.auth and sun.security.
 *
 */
public class Resources_zh_TW extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "\u7121\u6548\u7A7A\u503C\u8F38\u5165"},
        {"actions.can.only.be.read.", "\u52D5\u4F5C\u53EA\u80FD\u88AB\u300C\u8B80\u53D6\u300D"},
        {"permission.name.name.syntax.invalid.",
                "\u6B0A\u9650\u540D\u7A31 [{0}] \u662F\u7121\u6548\u7684\u8A9E\u6CD5: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "Credential \u985E\u5225\u5F8C\u9762\u4E0D\u662F Principal \u985E\u5225\u53CA\u540D\u7A31"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "Principal \u985E\u5225\u5F8C\u9762\u4E0D\u662F Principal \u540D\u7A31"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "Principal \u540D\u7A31\u5FC5\u9808\u4EE5\u5F15\u865F\u5708\u4F4F"},
        {"Principal.Name.missing.end.quote",
                "Principal \u540D\u7A31\u7F3A\u5C11\u4E0B\u5F15\u865F"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "\u5982\u679C Principal \u540D\u7A31\u4E0D\u662F\u4E00\u500B\u842C\u7528\u5B57\u5143 (*) \u503C\uFF0C\u90A3\u9EBC PrivateCredentialPermission Principal \u985E\u5225\u5C31\u4E0D\u80FD\u662F\u842C\u7528\u5B57\u5143 (*) \u503C"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\tPrincipal \u985E\u5225 = {0}\n\tPrincipal \u540D\u7A31 = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "\u63D0\u4F9B\u7A7A\u503C\u540D\u7A31"},
        {"provided.null.keyword.map", "\u63D0\u4F9B\u7A7A\u503C\u95DC\u9375\u5B57\u5C0D\u6620"},
        {"provided.null.OID.map", "\u63D0\u4F9B\u7A7A\u503C OID \u5C0D\u6620"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "\u63D0\u4F9B\u7121\u6548\u7684\u7A7A\u503C AccessControlContext"},
        {"invalid.null.action.provided", "\u63D0\u4F9B\u7121\u6548\u7684\u7A7A\u503C\u52D5\u4F5C"},
        {"invalid.null.Class.provided", "\u63D0\u4F9B\u7121\u6548\u7684\u7A7A\u503C\u985E\u5225"},
        {"Subject.", "\u4E3B\u984C:\n"},
        {".Principal.", "\tPrincipal: "},
        {".Public.Credential.", "\t\u516C\u7528\u8B49\u660E\u8CC7\u6599: "},
        {".Private.Credentials.inaccessible.",
                "\t\u79C1\u4EBA\u8B49\u660E\u8CC7\u6599\u7121\u6CD5\u5B58\u53D6\n"},
        {".Private.Credential.", "\t\u79C1\u4EBA\u8B49\u660E\u8CC7\u6599: "},
        {".Private.Credential.inaccessible.",
                "\t\u79C1\u4EBA\u8B49\u660E\u8CC7\u6599\u7121\u6CD5\u5B58\u53D6\n"},
        {"Subject.is.read.only", "\u4E3B\u984C\u70BA\u552F\u8B80"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "\u8A66\u5716\u65B0\u589E\u4E00\u500B\u975E java.security.Principal \u57F7\u884C\u8655\u7406\u7684\u7269\u4EF6\u81F3\u4E3B\u984C\u7684 Principal \u7FA4\u4E2D"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "\u8A66\u5716\u65B0\u589E\u4E00\u500B\u975E {0} \u57F7\u884C\u8655\u7406\u7684\u7269\u4EF6"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "\u7121\u6548\u7A7A\u503C\u8F38\u5165: \u540D\u7A31"},
        {"No.LoginModules.configured.for.name",
         "\u7121\u91DD\u5C0D {0} \u8A2D\u5B9A\u7684 LoginModules"},
        {"invalid.null.Subject.provided", "\u63D0\u4F9B\u7121\u6548\u7A7A\u503C\u4E3B\u984C"},
        {"invalid.null.CallbackHandler.provided",
                "\u63D0\u4F9B\u7121\u6548\u7A7A\u503C CallbackHandler"},
        {"null.subject.logout.called.before.login",
                "\u7A7A\u503C\u4E3B\u984C - \u5728\u767B\u5165\u4E4B\u524D\u5373\u547C\u53EB\u767B\u51FA"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "\u7121\u6CD5\u5275\u8A2D LoginModule\uFF0C{0}\uFF0C\u56E0\u70BA\u5B83\u4E26\u672A\u63D0\u4F9B\u975E\u5F15\u6578\u7684\u5EFA\u69CB\u5B50"},
        {"unable.to.instantiate.LoginModule",
                "\u7121\u6CD5\u5EFA\u7ACB LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "\u7121\u6CD5\u5EFA\u7ACB LoginModule: "},
        {"unable.to.find.LoginModule.class.",
                "\u627E\u4E0D\u5230 LoginModule \u985E\u5225: "},
        {"unable.to.access.LoginModule.",
                "\u7121\u6CD5\u5B58\u53D6 LoginModule: "},
        {"Login.Failure.all.modules.ignored",
                "\u767B\u5165\u5931\u6557: \u5FFD\u7565\u6240\u6709\u6A21\u7D44"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: \u5256\u6790\u932F\u8AA4 {0}: \n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: \u65B0\u589E\u6B0A\u9650\u932F\u8AA4 {0}: \n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: \u65B0\u589E\u9805\u76EE\u932F\u8AA4: \n\t{0}"},
        {"alias.name.not.provided.pe.name.", "\u672A\u63D0\u4F9B\u5225\u540D\u540D\u7A31 ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "\u7121\u6CD5\u5C0D\u5225\u540D\u57F7\u884C\u66FF\u63DB\uFF0C{0}"},
        {"substitution.value.prefix.unsupported",
                "\u4E0D\u652F\u63F4\u7684\u66FF\u63DB\u503C\uFF0C{0}"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","\u8F38\u5165\u4E0D\u80FD\u70BA\u7A7A\u503C"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "\u6307\u5B9A keystorePasswordURL \u9700\u8981\u540C\u6642\u6307\u5B9A\u91D1\u9470\u5132\u5B58\u5EAB"},
        {"expected.keystore.type", "\u9810\u671F\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u985E\u578B"},
        {"expected.keystore.provider", "\u9810\u671F\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u63D0\u4F9B\u8005"},
        {"multiple.Codebase.expressions",
                "\u591A\u91CD Codebase \u8868\u793A\u5F0F"},
        {"multiple.SignedBy.expressions","\u591A\u91CD SignedBy \u8868\u793A\u5F0F"},
        {"duplicate.keystore.domain.name","\u91CD\u8907\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u7DB2\u57DF\u540D\u7A31: {0}"},
        {"duplicate.keystore.name","\u91CD\u8907\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u540D\u7A31: {0}"},
        {"SignedBy.has.empty.alias","SignedBy \u6709\u7A7A\u5225\u540D"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "\u6C92\u6709\u842C\u7528\u5B57\u5143\u540D\u7A31\uFF0C\u7121\u6CD5\u6307\u5B9A\u542B\u6709\u842C\u7528\u5B57\u5143\u985E\u5225\u7684 Principal"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "\u9810\u671F\u7684 codeBase \u6216 SignedBy \u6216 Principal"},
        {"expected.permission.entry", "\u9810\u671F\u7684\u6B0A\u9650\u9805\u76EE"},
        {"number.", "\u865F\u78BC "},
        {"expected.expect.read.end.of.file.",
                "\u9810\u671F\u7684 [{0}], \u8B80\u53D6 [end of file]"},
        {"expected.read.end.of.file.",
                "\u9810\u671F\u7684 [;], \u8B80\u53D6 [end of file]"},
        {"line.number.msg", "\u884C {0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "\u884C {0}: \u9810\u671F\u7684 [{1}]\uFF0C\u767C\u73FE [{2}]"},
        {"null.principalClass.or.principalName",
                "\u7A7A\u503C principalClass \u6216 principalName"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "PKCS11 \u8A18\u865F [{0}] \u5BC6\u78BC: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "\u7121\u6CD5\u5EFA\u7ACB\u4E3B\u984C\u5F0F\u7684\u539F\u5247"}
    };


    /**
     * Returns the contents of this <code>ResourceBundle</code>.
     *
     * @return the contents of this <code>ResourceBundle</code>.
     */
    @Override
    public Object[][] getContents() {
        return contents;
    }
}

