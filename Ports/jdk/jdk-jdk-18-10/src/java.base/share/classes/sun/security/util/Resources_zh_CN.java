/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
public class Resources_zh_CN extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "\u65E0\u6548\u7684\u7A7A\u8F93\u5165"},
        {"actions.can.only.be.read.", "\u64CD\u4F5C\u53EA\u80FD\u4E3A '\u8BFB\u53D6'"},
        {"permission.name.name.syntax.invalid.",
                "\u6743\u9650\u540D\u79F0 [{0}] \u8BED\u6CD5\u65E0\u6548: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "\u8EAB\u4EFD\u8BC1\u660E\u7C7B\u540E\u9762\u672A\u8DDF\u968F\u4E3B\u7528\u6237\u7C7B\u53CA\u540D\u79F0"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "\u4E3B\u7528\u6237\u7C7B\u540E\u9762\u672A\u8DDF\u968F\u4E3B\u7528\u6237\u540D\u79F0"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "\u4E3B\u7528\u6237\u540D\u79F0\u5FC5\u987B\u653E\u5728\u5F15\u53F7\u5185"},
        {"Principal.Name.missing.end.quote",
                "\u4E3B\u7528\u6237\u540D\u79F0\u7F3A\u5C11\u53F3\u5F15\u53F7"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "\u5982\u679C\u4E3B\u7528\u6237\u540D\u79F0\u4E0D\u662F\u901A\u914D\u7B26 (*) \u503C, \u90A3\u4E48 PrivateCredentialPermission \u4E3B\u7528\u6237\u7C7B\u4E0D\u80FD\u662F\u901A\u914D\u7B26 (*) \u503C"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\t\u4E3B\u7528\u6237\u7C7B = {0}\n\t\u4E3B\u7528\u6237\u540D\u79F0 = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "\u63D0\u4F9B\u7684\u540D\u79F0\u4E3A\u7A7A\u503C"},
        {"provided.null.keyword.map", "\u63D0\u4F9B\u7684\u5173\u952E\u5B57\u6620\u5C04\u4E3A\u7A7A\u503C"},
        {"provided.null.OID.map", "\u63D0\u4F9B\u7684 OID \u6620\u5C04\u4E3A\u7A7A\u503C"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "\u63D0\u4F9B\u4E86\u65E0\u6548\u7684\u7A7A AccessControlContext"},
        {"invalid.null.action.provided", "\u63D0\u4F9B\u4E86\u65E0\u6548\u7684\u7A7A\u64CD\u4F5C"},
        {"invalid.null.Class.provided", "\u63D0\u4F9B\u4E86\u65E0\u6548\u7684\u7A7A\u7C7B"},
        {"Subject.", "\u4E3B\u4F53: \n"},
        {".Principal.", "\t\u4E3B\u7528\u6237: "},
        {".Public.Credential.", "\t\u516C\u5171\u8EAB\u4EFD\u8BC1\u660E: "},
        {".Private.Credentials.inaccessible.",
                "\t\u65E0\u6CD5\u8BBF\u95EE\u4E13\u7528\u8EAB\u4EFD\u8BC1\u660E\n"},
        {".Private.Credential.", "\t\u4E13\u7528\u8EAB\u4EFD\u8BC1\u660E: "},
        {".Private.Credential.inaccessible.",
                "\t\u65E0\u6CD5\u8BBF\u95EE\u4E13\u7528\u8EAB\u4EFD\u8BC1\u660E\n"},
        {"Subject.is.read.only", "\u4E3B\u4F53\u4E3A\u53EA\u8BFB"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "\u6B63\u5728\u5C1D\u8BD5\u5C06\u4E00\u4E2A\u975E java.security.Principal \u5B9E\u4F8B\u7684\u5BF9\u8C61\u6DFB\u52A0\u5230\u4E3B\u4F53\u7684\u4E3B\u7528\u6237\u96C6\u4E2D"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "\u6B63\u5728\u5C1D\u8BD5\u6DFB\u52A0\u4E00\u4E2A\u975E{0}\u5B9E\u4F8B\u7684\u5BF9\u8C61"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "\u65E0\u6548\u7A7A\u8F93\u5165: \u540D\u79F0"},
        {"No.LoginModules.configured.for.name",
         "\u6CA1\u6709\u4E3A{0}\u914D\u7F6E LoginModules"},
        {"invalid.null.Subject.provided", "\u63D0\u4F9B\u4E86\u65E0\u6548\u7684\u7A7A\u4E3B\u4F53"},
        {"invalid.null.CallbackHandler.provided",
                "\u63D0\u4F9B\u4E86\u65E0\u6548\u7684\u7A7A CallbackHandler"},
        {"null.subject.logout.called.before.login",
                "\u7A7A\u4E3B\u4F53 - \u5728\u767B\u5F55\u4E4B\u524D\u8C03\u7528\u4E86\u6CE8\u9500"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "\u65E0\u6CD5\u5B9E\u4F8B\u5316 LoginModule, {0}, \u56E0\u4E3A\u5B83\u672A\u63D0\u4F9B\u4E00\u4E2A\u65E0\u53C2\u6570\u6784\u9020\u5668"},
        {"unable.to.instantiate.LoginModule",
                "\u65E0\u6CD5\u5B9E\u4F8B\u5316 LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "\u65E0\u6CD5\u5B9E\u4F8B\u5316 LoginModule: "},
        {"unable.to.find.LoginModule.class.",
                "\u65E0\u6CD5\u627E\u5230 LoginModule \u7C7B: "},
        {"unable.to.access.LoginModule.",
                "\u65E0\u6CD5\u8BBF\u95EE LoginModule: "},
        {"Login.Failure.all.modules.ignored",
                "\u767B\u5F55\u5931\u8D25: \u5FFD\u7565\u6240\u6709\u6A21\u5757"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: \u89E3\u6790{0}\u65F6\u51FA\u9519:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: \u6DFB\u52A0\u6743\u9650{0}\u65F6\u51FA\u9519:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: \u6DFB\u52A0\u6761\u76EE\u65F6\u51FA\u9519:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "\u672A\u63D0\u4F9B\u522B\u540D ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "\u65E0\u6CD5\u5728\u522B\u540D {0} \u4E0A\u6267\u884C\u66FF\u4EE3"},
        {"substitution.value.prefix.unsupported",
                "\u66FF\u4EE3\u503C{0}\u4E0D\u53D7\u652F\u6301"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","\u7C7B\u578B\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "\u4E0D\u6307\u5B9A\u5BC6\u94A5\u5E93\u65F6\u65E0\u6CD5\u6307\u5B9A keystorePasswordURL"},
        {"expected.keystore.type", "\u5E94\u4E3A\u5BC6\u94A5\u5E93\u7C7B\u578B"},
        {"expected.keystore.provider", "\u5E94\u4E3A\u5BC6\u94A5\u5E93\u63D0\u4F9B\u65B9"},
        {"multiple.Codebase.expressions",
                "\u591A\u4E2A\u4EE3\u7801\u5E93\u8868\u8FBE\u5F0F"},
        {"multiple.SignedBy.expressions","\u591A\u4E2A SignedBy \u8868\u8FBE\u5F0F"},
        {"duplicate.keystore.domain.name","\u5BC6\u94A5\u5E93\u57DF\u540D\u91CD\u590D: {0}"},
        {"duplicate.keystore.name","\u5BC6\u94A5\u5E93\u540D\u79F0\u91CD\u590D: {0}"},
        {"SignedBy.has.empty.alias","SignedBy \u6709\u7A7A\u522B\u540D"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "\u6CA1\u6709\u901A\u914D\u7B26\u540D\u79F0, \u65E0\u6CD5\u4F7F\u7528\u901A\u914D\u7B26\u7C7B\u6307\u5B9A\u4E3B\u7528\u6237"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "\u5E94\u4E3A codeBase, SignedBy \u6216\u4E3B\u7528\u6237"},
        {"expected.permission.entry", "\u5E94\u4E3A\u6743\u9650\u6761\u76EE"},
        {"number.", "\u7F16\u53F7 "},
        {"expected.expect.read.end.of.file.",
                "\u5E94\u4E3A [{0}], \u8BFB\u53D6\u7684\u662F [\u6587\u4EF6\u7ED3\u5C3E]"},
        {"expected.read.end.of.file.",
                "\u5E94\u4E3A [;], \u8BFB\u53D6\u7684\u662F [\u6587\u4EF6\u7ED3\u5C3E]"},
        {"line.number.msg", "\u5217{0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "\u884C\u53F7 {0}: \u5E94\u4E3A [{1}], \u627E\u5230 [{2}]"},
        {"null.principalClass.or.principalName",
                "principalClass \u6216 principalName \u4E3A\u7A7A\u503C"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "PKCS11 \u6807\u8BB0 [{0}] \u53E3\u4EE4: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "\u65E0\u6CD5\u5B9E\u4F8B\u5316\u57FA\u4E8E\u4E3B\u9898\u7684\u7B56\u7565"}
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

