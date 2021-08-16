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
public class Resources_it extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "input nullo/i non valido/i"},
        {"actions.can.only.be.read.", "le azioni possono essere solamente 'lette'"},
        {"permission.name.name.syntax.invalid.",
                "sintassi [{0}] non valida per il nome autorizzazione: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "la classe di credenziali non \u00E8 seguita da un nome e una classe di principal"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "la classe di principal non \u00E8 seguita da un nome principal"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "il nome principal deve essere compreso tra apici"},
        {"Principal.Name.missing.end.quote",
                "apice di chiusura del nome principal mancante"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "la classe principal PrivateCredentialPermission non pu\u00F2 essere un valore carattere jolly (*) se il nome principal a sua volta non \u00E8 un valore carattere jolly (*)"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\tclasse Principal = {0}\n\tNome Principal = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "il nome fornito \u00E8 nullo"},
        {"provided.null.keyword.map", "specificata mappa parole chiave null"},
        {"provided.null.OID.map", "specificata mappa OID null"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "fornito un valore nullo non valido per AccessControlContext"},
        {"invalid.null.action.provided", "fornita un'azione nulla non valida"},
        {"invalid.null.Class.provided", "fornita una classe nulla non valida"},
        {"Subject.", "Oggetto:\n"},
        {".Principal.", "\tPrincipal: "},
        {".Public.Credential.", "\tCredenziale pubblica: "},
        {".Private.Credentials.inaccessible.",
                "\tImpossibile accedere alle credenziali private\n"},
        {".Private.Credential.", "\tCredenziale privata: "},
        {".Private.Credential.inaccessible.",
                "\tImpossibile accedere alla credenziale privata\n"},
        {"Subject.is.read.only", "L'oggetto \u00E8 di sola lettura"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "si \u00E8 tentato di aggiungere un oggetto che non \u00E8 un'istanza di java.security.Principal a un set principal dell'oggetto"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "si \u00E8 tentato di aggiungere un oggetto che non \u00E8 un''istanza di {0}"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "Input nullo non valido: nome"},
        {"No.LoginModules.configured.for.name",
         "Nessun LoginModules configurato per {0}"},
        {"invalid.null.Subject.provided", "fornito un valore nullo non valido per l'oggetto"},
        {"invalid.null.CallbackHandler.provided",
                "fornito un valore nullo non valido per CallbackHandler"},
        {"null.subject.logout.called.before.login",
                "oggetto nullo - il logout \u00E8 stato richiamato prima del login"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "impossibile creare un''istanza di LoginModule {0} in quanto non restituisce un argomento vuoto per il costruttore"},
        {"unable.to.instantiate.LoginModule",
                "impossibile creare un'istanza di LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "impossibile creare un'istanza di LoginModule: "},
        {"unable.to.find.LoginModule.class.",
                "impossibile trovare la classe LoginModule: "},
        {"unable.to.access.LoginModule.",
                "impossibile accedere a LoginModule "},
        {"Login.Failure.all.modules.ignored",
                "Errore di login: tutti i moduli sono stati ignorati"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: errore durante l''analisi di {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: errore durante l''aggiunta dell''autorizzazione {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: errore durante l''aggiunta della voce:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "impossibile fornire nome alias ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "impossibile eseguire una sostituzione sull''alias, {0}"},
        {"substitution.value.prefix.unsupported",
                "valore sostituzione, {0}, non supportato"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","il tipo non pu\u00F2 essere nullo"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "Impossibile specificare keystorePasswordURL senza specificare anche il keystore"},
        {"expected.keystore.type", "tipo keystore previsto"},
        {"expected.keystore.provider", "provider di keystore previsto"},
        {"multiple.Codebase.expressions",
                "espressioni Codebase multiple"},
        {"multiple.SignedBy.expressions","espressioni SignedBy multiple"},
        {"duplicate.keystore.domain.name","nome dominio keystore duplicato: {0}"},
        {"duplicate.keystore.name","nome keystore duplicato: {0}"},
        {"SignedBy.has.empty.alias","SignedBy presenta un alias vuoto"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "impossibile specificare un principal con una classe carattere jolly senza un nome carattere jolly"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "previsto codeBase o SignedBy o principal"},
        {"expected.permission.entry", "prevista voce di autorizzazione"},
        {"number.", "numero "},
        {"expected.expect.read.end.of.file.",
                "previsto [{0}], letto [end of file]"},
        {"expected.read.end.of.file.",
                "previsto [;], letto [end of file]"},
        {"line.number.msg", "riga {0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "riga {0}: previsto [{1}], trovato [{2}]"},
        {"null.principalClass.or.principalName",
                "principalClass o principalName nullo"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "Password per token PKCS11 [{0}]: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "impossibile creare un'istanza dei criteri basati sull'oggetto"}
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

