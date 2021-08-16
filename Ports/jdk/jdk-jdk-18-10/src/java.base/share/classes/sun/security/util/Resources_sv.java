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
public class Resources_sv extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "ogiltiga null-indata"},
        {"actions.can.only.be.read.", "\u00E5tg\u00E4rder kan endast 'l\u00E4sas'"},
        {"permission.name.name.syntax.invalid.",
                "syntaxen f\u00F6r beh\u00F6righetsnamnet [{0}] \u00E4r ogiltig: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "Inloggningsuppgiftsklassen f\u00F6ljs inte av klass eller namn f\u00F6r identitetshavare"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "Identitetshavareklassen f\u00F6ljs inte av n\u00E5got identitetshavarenamn"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "Identitetshavarenamnet m\u00E5ste anges inom citattecken"},
        {"Principal.Name.missing.end.quote",
                "Identitetshavarenamnet saknar avslutande citattecken"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "Identitetshavareklassen PrivateCredentialPermission kan inte ha n\u00E5got jokertecken (*) om inte namnet p\u00E5 identitetshavaren anges med jokertecken (*)"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\tIdentitetshavareklass = {0}\n\tIdentitetshavarenamn = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "null-namn angavs"},
        {"provided.null.keyword.map", "nullnyckelordsmappning angavs"},
        {"provided.null.OID.map", "null-OID-mappning angavs"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "ogiltigt null-AccessControlContext"},
        {"invalid.null.action.provided", "ogiltig null-funktion"},
        {"invalid.null.Class.provided", "ogiltig null-klass"},
        {"Subject.", "Innehavare:\n"},
        {".Principal.", "\tIdentitetshavare: "},
        {".Public.Credential.", "\tOffentlig inloggning: "},
        {".Private.Credentials.inaccessible.",
                "\tPrivat inloggning \u00E4r inte tillg\u00E4nglig\n"},
        {".Private.Credential.", "\tPrivat inloggning: "},
        {".Private.Credential.inaccessible.",
                "\tPrivat inloggning \u00E4r inte tillg\u00E4nglig\n"},
        {"Subject.is.read.only", "Innehavare \u00E4r skrivskyddad"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "f\u00F6rs\u00F6k att l\u00E4gga till ett objekt som inte \u00E4r en instans av java.security.Principal till ett subjekts upps\u00E4ttning av identitetshavare"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "f\u00F6rs\u00F6ker l\u00E4gga till ett objekt som inte \u00E4r en instans av {0}"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "Ogiltiga null-indata: namn"},
        {"No.LoginModules.configured.for.name",
         "Inga inloggningsmoduler har konfigurerats f\u00F6r {0}"},
        {"invalid.null.Subject.provided", "ogiltig null-subjekt"},
        {"invalid.null.CallbackHandler.provided",
                "ogiltig null-CallbackHandler"},
        {"null.subject.logout.called.before.login",
                "null-subjekt - utloggning anropades f\u00F6re inloggning"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "kan inte instansiera LoginModule, {0}, eftersom den inte tillhandah\u00E5ller n\u00E5gon icke-argumentskonstruktor"},
        {"unable.to.instantiate.LoginModule",
                "kan inte instansiera LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "kan inte instansiera LoginModule: "},
        {"unable.to.find.LoginModule.class.",
                "hittar inte LoginModule-klassen: "},
        {"unable.to.access.LoginModule.",
                "ingen \u00E5tkomst till LoginModule: "},
        {"Login.Failure.all.modules.ignored",
                "Inloggningsfel: alla moduler ignoreras"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: fel vid tolkning av {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: fel vid till\u00E4gg av beh\u00F6righet, {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: fel vid till\u00E4gg av post:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "aliasnamn ej angivet ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "kan ej ers\u00E4tta alias, {0}"},
        {"substitution.value.prefix.unsupported",
                "ers\u00E4ttningsv\u00E4rde, {0}, st\u00F6ds ej"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","typen kan inte vara null"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "kan inte ange keystorePasswordURL utan att ange nyckellager"},
        {"expected.keystore.type", "f\u00F6rv\u00E4ntad nyckellagertyp"},
        {"expected.keystore.provider", "nyckellagerleverant\u00F6r f\u00F6rv\u00E4ntades"},
        {"multiple.Codebase.expressions",
                "flera CodeBase-uttryck"},
        {"multiple.SignedBy.expressions","flera SignedBy-uttryck"},
        {"duplicate.keystore.domain.name","dom\u00E4nnamn f\u00F6r dubbelt nyckellager: {0}"},
        {"duplicate.keystore.name","namn f\u00F6r dubbelt nyckellager: {0}"},
        {"SignedBy.has.empty.alias","SignedBy har ett tomt alias"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "kan inte ange identitetshavare med en jokerteckenklass utan ett jokerteckennamn"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "f\u00F6rv\u00E4ntad codeBase eller SignedBy eller identitetshavare"},
        {"expected.permission.entry", "f\u00F6rv\u00E4ntade beh\u00F6righetspost"},
        {"number.", "nummer"},
        {"expected.expect.read.end.of.file.",
                "f\u00F6rv\u00E4ntade [{0}], l\u00E4ste [filslut]"},
        {"expected.read.end.of.file.",
                "f\u00F6rv\u00E4ntade [;], l\u00E4ste [filslut]"},
        {"line.number.msg", "rad {0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "rad {0}: f\u00F6rv\u00E4ntade [{1}], hittade [{2}]"},
        {"null.principalClass.or.principalName",
                "null-principalClass eller -principalName"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "L\u00F6senord f\u00F6r PKCS11-token [{0}]: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "kan inte instansiera subjektbaserad policy"}
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

