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
public class Resources extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "invalid null input(s)"},
        {"actions.can.only.be.read.", "actions can only be 'read'"},
        {"permission.name.name.syntax.invalid.",
                "permission name [{0}] syntax invalid: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "Credential Class not followed by a Principal Class and Name"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "Principal Class not followed by a Principal Name"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "Principal Name must be surrounded by quotes"},
        {"Principal.Name.missing.end.quote",
                "Principal Name missing end quote"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "PrivateCredentialPermission Principal Class can not be a wildcard (*) value if Principal Name is not a wildcard (*) value"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\tPrincipal Class = {0}\n\tPrincipal Name = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "provided null name"},
        {"provided.null.keyword.map", "provided null keyword map"},
        {"provided.null.OID.map", "provided null OID map"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "invalid null AccessControlContext provided"},
        {"invalid.null.action.provided", "invalid null action provided"},
        {"invalid.null.Class.provided", "invalid null Class provided"},
        {"Subject.", "Subject:\n"},
        {".Principal.", "\tPrincipal: "},
        {".Public.Credential.", "\tPublic Credential: "},
        {".Private.Credential.", "\tPrivate Credential: "},
        {".Private.Credential.inaccessible.",
                "\tPrivate Credential inaccessible\n"},
        {"Subject.is.read.only", "Subject is read-only"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "attempting to add an object which is not an instance of java.security.Principal to a Subject's Principal Set"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "attempting to add an object which is not an instance of {0}"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "Invalid null input: name"},
        {"No.LoginModules.configured.for.name",
         "No LoginModules configured for {0}"},
        {"invalid.null.Subject.provided", "invalid null Subject provided"},
        {"invalid.null.CallbackHandler.provided",
                "invalid null CallbackHandler provided"},
        {"null.subject.logout.called.before.login",
                "null subject - logout called before login"},
        {"Login.Failure.all.modules.ignored",
                "Login Failure: all modules ignored"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: error parsing {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: error adding Permission, {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: error adding Entry:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "alias name not provided ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "unable to perform substitution on alias, {0}"},
        {"substitution.value.prefix.unsupported",
                "substitution value, {0}, unsupported"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","type can't be null"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "keystorePasswordURL can not be specified without also specifying keystore"},
        {"expected.keystore.type", "expected keystore type"},
        {"expected.keystore.provider", "expected keystore provider"},
        {"multiple.Codebase.expressions",
                "multiple Codebase expressions"},
        {"multiple.SignedBy.expressions","multiple SignedBy expressions"},
        {"duplicate.keystore.domain.name","duplicate keystore domain name: {0}"},
        {"duplicate.keystore.name","duplicate keystore name: {0}"},
        {"SignedBy.has.empty.alias","SignedBy has empty alias"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "can not specify Principal with a wildcard class without a wildcard name"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "expected codeBase or SignedBy or Principal"},
        {"expected.permission.entry", "expected permission entry"},
        {"number.", "number "},
        {"expected.expect.read.end.of.file.",
                "expected [{0}], read [end of file]"},
        {"expected.read.end.of.file.",
                "expected [;], read [end of file]"},
        {"line.number.msg", "line {0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "line {0}: expected [{1}], found [{2}]"},
        {"null.principalClass.or.principalName",
                "null principalClass or principalName"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "PKCS11 Token [{0}] Password: "},
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

