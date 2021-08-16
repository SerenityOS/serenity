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
public class Resources_ko extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "\uB110 \uC785\uB825\uAC12\uC774 \uBD80\uC801\uD569\uD569\uB2C8\uB2E4."},
        {"actions.can.only.be.read.", "\uC791\uC5C5\uC740 '\uC77D\uAE30' \uC804\uC6A9\uC785\uB2C8\uB2E4."},
        {"permission.name.name.syntax.invalid.",
                "\uAD8C\uD55C \uC774\uB984 [{0}] \uAD6C\uBB38\uC774 \uBD80\uC801\uD569\uD568: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "\uC778\uC99D\uC11C \uD074\uB798\uC2A4 \uB2E4\uC74C\uC5D0 \uC8FC\uCCB4 \uD074\uB798\uC2A4\uC640 \uC774\uB984\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "\uC8FC\uCCB4 \uD074\uB798\uC2A4 \uB2E4\uC74C\uC5D0 \uC8FC\uCCB4 \uC774\uB984\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "\uC8FC\uCCB4 \uC774\uB984\uC740 \uB530\uC634\uD45C\uB85C \uBB36\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Principal.Name.missing.end.quote",
                "\uC8FC\uCCB4 \uC774\uB984\uC5D0 \uB2EB\uB294 \uB530\uC634\uD45C\uAC00 \uB204\uB77D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "\uC8FC\uCCB4 \uC774\uB984\uC774 \uC640\uC77C\uB4DC \uCE74\uB4DC \uBB38\uC790(*) \uAC12\uC774 \uC544\uB2CC \uACBD\uC6B0 PrivateCredentialPermission \uC8FC\uCCB4 \uD074\uB798\uC2A4\uB294 \uC640\uC77C\uB4DC \uCE74\uB4DC \uBB38\uC790(*) \uAC12\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\t\uC8FC\uCCB4 \uD074\uB798\uC2A4 = {0}\n\t\uC8FC\uCCB4 \uC774\uB984 = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "\uB110 \uC774\uB984\uC744 \uC81C\uACF5\uD588\uC2B5\uB2C8\uB2E4."},
        {"provided.null.keyword.map", "\uB110 \uD0A4\uC6CC\uB4DC \uB9F5\uC744 \uC81C\uACF5\uD588\uC2B5\uB2C8\uB2E4."},
        {"provided.null.OID.map", "\uB110 OID \uB9F5\uC744 \uC81C\uACF5\uD588\uC2B5\uB2C8\uB2E4."},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "\uBD80\uC801\uD569\uD55C \uB110 AccessControlContext\uAC00 \uC81C\uACF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"invalid.null.action.provided", "\uBD80\uC801\uD569\uD55C \uB110 \uC791\uC5C5\uC774 \uC81C\uACF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"invalid.null.Class.provided", "\uBD80\uC801\uD569\uD55C \uB110 \uD074\uB798\uC2A4\uAC00 \uC81C\uACF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Subject.", "\uC81C\uBAA9:\n"},
        {".Principal.", "\\\uC8FC\uCCB4: "},
        {".Public.Credential.", "\t\uACF5\uC6A9 \uC778\uC99D\uC11C: "},
        {".Private.Credentials.inaccessible.",
                "\t\uC804\uC6A9 \uC778\uC99D\uC11C\uC5D0 \uC561\uC138\uC2A4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.\n"},
        {".Private.Credential.", "\t\uC804\uC6A9 \uC778\uC99D\uC11C: "},
        {".Private.Credential.inaccessible.",
                "\t\uC804\uC6A9 \uC778\uC99D\uC11C\uC5D0 \uC561\uC138\uC2A4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.\n"},
        {"Subject.is.read.only", "\uC81C\uBAA9\uC774 \uC77D\uAE30 \uC804\uC6A9\uC785\uB2C8\uB2E4."},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "java.security.Principal\uC758 \uC778\uC2A4\uD134\uC2A4\uAC00 \uC544\uB2CC \uAC1D\uCCB4\uB97C \uC81C\uBAA9\uC758 \uC8FC\uCCB4 \uC9D1\uD569\uC5D0 \uCD94\uAC00\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "{0}\uC758 \uC778\uC2A4\uD134\uC2A4\uAC00 \uC544\uB2CC \uAC1D\uCCB4\uB97C \uCD94\uAC00\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "\uBD80\uC801\uD569\uD55C \uB110 \uC785\uB825\uAC12: \uC774\uB984"},
        {"No.LoginModules.configured.for.name",
         "{0}\uC5D0 \uB300\uD574 \uAD6C\uC131\uB41C LoginModules\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"invalid.null.Subject.provided", "\uBD80\uC801\uD569\uD55C \uB110 \uC81C\uBAA9\uC774 \uC81C\uACF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"invalid.null.CallbackHandler.provided",
                "\uBD80\uC801\uD569\uD55C \uB110 CallbackHandler\uAC00 \uC81C\uACF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"null.subject.logout.called.before.login",
                "\uB110 \uC81C\uBAA9 - \uB85C\uADF8\uC778 \uC804\uC5D0 \uB85C\uADF8\uC544\uC6C3\uC774 \uD638\uCD9C\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "\uC778\uC218\uAC00 \uC5C6\uB294 \uC0DD\uC131\uC790\uB97C \uC81C\uACF5\uD558\uC9C0 \uC54A\uC544 LoginModule {0}\uC744(\uB97C) \uC778\uC2A4\uD134\uC2A4\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"unable.to.instantiate.LoginModule",
                "LoginModule\uC744 \uC778\uC2A4\uD134\uC2A4\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"unable.to.instantiate.LoginModule.",
                "LoginModule\uC744 \uC778\uC2A4\uD134\uC2A4\uD654\uD560 \uC218 \uC5C6\uC74C: "},
        {"unable.to.find.LoginModule.class.",
                "LoginModule \uD074\uB798\uC2A4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: "},
        {"unable.to.access.LoginModule.",
                "LoginModule\uC5D0 \uC561\uC138\uC2A4\uD560 \uC218 \uC5C6\uC74C: "},
        {"Login.Failure.all.modules.ignored",
                "\uB85C\uADF8\uC778 \uC2E4\uD328: \uBAA8\uB4E0 \uBAA8\uB4C8\uC774 \uBB34\uC2DC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: {0}\uC758 \uAD6C\uBB38\uC744 \uBD84\uC11D\uD558\uB294 \uC911 \uC624\uB958 \uBC1C\uC0DD:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: {0} \uAD8C\uD55C\uC744 \uCD94\uAC00\uD558\uB294 \uC911 \uC624\uB958 \uBC1C\uC0DD:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: \uD56D\uBAA9\uC744 \uCD94\uAC00\uD558\uB294 \uC911 \uC624\uB958 \uBC1C\uC0DD:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "\uBCC4\uCE6D \uC774\uB984\uC774 \uC81C\uACF5\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4({0})."},
        {"unable.to.perform.substitution.on.alias.suffix",
                "{0} \uBCC4\uCE6D\uC744 \uB300\uCCB4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"substitution.value.prefix.unsupported",
                "\uB300\uCCB4 \uAC12 {0}\uC740(\uB294) \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","\uC720\uD615\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "\uD0A4 \uC800\uC7A5\uC18C\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uACE0 keystorePasswordURL\uC744 \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"expected.keystore.type", "\uD0A4 \uC800\uC7A5\uC18C \uC720\uD615\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},
        {"expected.keystore.provider", "\uD0A4 \uC800\uC7A5\uC18C \uC81C\uACF5\uC790\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},
        {"multiple.Codebase.expressions",
                "Codebase \uD45C\uD604\uC2DD\uC774 \uC5EC\uB7EC \uAC1C\uC785\uB2C8\uB2E4."},
        {"multiple.SignedBy.expressions","SignedBy \uD45C\uD604\uC2DD\uC774 \uC5EC\uB7EC \uAC1C\uC785\uB2C8\uB2E4."},
        {"duplicate.keystore.domain.name","\uC911\uBCF5\uB41C \uD0A4 \uC800\uC7A5\uC18C \uB3C4\uBA54\uC778 \uC774\uB984: {0}"},
        {"duplicate.keystore.name","\uC911\uBCF5\uB41C \uD0A4 \uC800\uC7A5\uC18C \uC774\uB984: {0}"},
        {"SignedBy.has.empty.alias","SignedBy\uC758 \uBCC4\uCE6D\uC774 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "\uC640\uC77C\uB4DC \uCE74\uB4DC \uBB38\uC790 \uC774\uB984 \uC5C6\uC774 \uC640\uC77C\uB4DC \uCE74\uB4DC \uBB38\uC790 \uD074\uB798\uC2A4\uB97C \uC0AC\uC6A9\uD558\uB294 \uC8FC\uCCB4\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "codeBase, SignedBy \uB610\uB294 \uC8FC\uCCB4\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},
        {"expected.permission.entry", "\uAD8C\uD55C \uD56D\uBAA9\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},
        {"number.", "\uC22B\uC790 "},
        {"expected.expect.read.end.of.file.",
                "[{0}]\uC774(\uAC00) \uD544\uC694\uD558\uC9C0\uB9CC [\uD30C\uC77C\uC758 \uB05D]\uAE4C\uC9C0 \uC77D\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"expected.read.end.of.file.",
                "[;]\uC774 \uD544\uC694\uD558\uC9C0\uB9CC [\uD30C\uC77C\uC758 \uB05D]\uAE4C\uC9C0 \uC77D\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"line.number.msg", "{0} \uD589: {1}"},
        {"line.number.expected.expect.found.actual.",
                "{0} \uD589: [{1}]\uC774(\uAC00) \uD544\uC694\uD558\uC9C0\uB9CC [{2}]\uC774(\uAC00) \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"null.principalClass.or.principalName",
                "principalClass \uB610\uB294 principalName\uC774 \uB110\uC785\uB2C8\uB2E4."},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "PKCS11 \uD1A0\uD070 [{0}] \uBE44\uBC00\uBC88\uD638: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "\uC81C\uBAA9 \uAE30\uBC18 \uC815\uCC45\uC744 \uC778\uC2A4\uD134\uC2A4\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."}
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

