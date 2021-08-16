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
public class Resources_fr extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "entr\u00E9es NULL non valides"},
        {"actions.can.only.be.read.", "les actions sont accessibles en lecture uniquement"},
        {"permission.name.name.syntax.invalid.",
                "syntaxe de nom de droit [{0}] non valide : "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "Classe Credential non suivie d'une classe et d'un nom de principal"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "Classe de principal non suivie d'un nom de principal"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "Le nom de principal doit \u00EAtre indiqu\u00E9 entre guillemets"},
        {"Principal.Name.missing.end.quote",
                "Guillemet fermant manquant pour le nom de principal"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "La classe de principal PrivateCredentialPermission ne peut pas \u00EAtre une valeur g\u00E9n\u00E9rique (*) si le nom de principal n'est pas une valeur g\u00E9n\u00E9rique (*)"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner :\n\tClasse de principal = {0}\n\tNom de principal = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "nom NULL fourni"},
        {"provided.null.keyword.map", "mappage de mots-cl\u00E9s NULL fourni"},
        {"provided.null.OID.map", "mappage OID NULL fourni"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "AccessControlContext NULL fourni non valide"},
        {"invalid.null.action.provided", "action NULL fournie non valide"},
        {"invalid.null.Class.provided", "classe NULL fournie non valide"},
        {"Subject.", "Objet :\n"},
        {".Principal.", "\tPrincipal : "},
        {".Public.Credential.", "\tInformations d'identification publiques : "},
        {".Private.Credentials.inaccessible.",
                "\tInformations d'identification priv\u00E9es inaccessibles\n"},
        {".Private.Credential.", "\tInformations d'identification priv\u00E9es : "},
        {".Private.Credential.inaccessible.",
                "\tInformations d'identification priv\u00E9es inaccessibles\n"},
        {"Subject.is.read.only", "Sujet en lecture seule"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "tentative d'ajout d'un objet qui n'est pas une instance de java.security.Principal dans un ensemble de principaux du sujet"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "tentative d''ajout d''un objet qui n''est pas une instance de {0}"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag : "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "Entr\u00E9e NULL non valide : nom"},
        {"No.LoginModules.configured.for.name",
         "Aucun LoginModule configur\u00E9 pour {0}"},
        {"invalid.null.Subject.provided", "sujet NULL fourni non valide"},
        {"invalid.null.CallbackHandler.provided",
                "CallbackHandler NULL fourni non valide"},
        {"null.subject.logout.called.before.login",
                "sujet NULL - Tentative de d\u00E9connexion avant la connexion"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "impossible d''instancier LoginModule {0} car il ne fournit pas de constructeur sans argument"},
        {"unable.to.instantiate.LoginModule",
                "impossible d'instancier LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "impossible d'instancier LoginModule\u00A0: "},
        {"unable.to.find.LoginModule.class.",
                "classe LoginModule introuvable : "},
        {"unable.to.access.LoginModule.",
                "impossible d'acc\u00E9der \u00E0 LoginModule : "},
        {"Login.Failure.all.modules.ignored",
                "Echec de connexion : tous les modules ont \u00E9t\u00E9 ignor\u00E9s"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy : erreur d''analyse de {0} :\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy : erreur d''ajout de droit, {0} :\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy : erreur d''ajout d''entr\u00E9e :\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "nom d''alias non fourni ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "impossible d''effectuer une substitution pour l''alias, {0}"},
        {"substitution.value.prefix.unsupported",
                "valeur de substitution, {0}, non prise en charge"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","le type ne peut \u00EAtre NULL"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "Impossible de sp\u00E9cifier keystorePasswordURL sans indiquer aussi le fichier de cl\u00E9s"},
        {"expected.keystore.type", "type de fichier de cl\u00E9s attendu"},
        {"expected.keystore.provider", "fournisseur de fichier de cl\u00E9s attendu"},
        {"multiple.Codebase.expressions",
                "expressions Codebase multiples"},
        {"multiple.SignedBy.expressions","expressions SignedBy multiples"},
        {"duplicate.keystore.domain.name","nom de domaine de fichier de cl\u00E9s en double : {0}"},
        {"duplicate.keystore.name","nom de fichier de cl\u00E9s en double : {0}"},
        {"SignedBy.has.empty.alias","SignedBy poss\u00E8de un alias vide"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "impossible de sp\u00E9cifier le principal avec une classe g\u00E9n\u00E9rique sans nom g\u00E9n\u00E9rique"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "codeBase, SignedBy ou Principal attendu"},
        {"expected.permission.entry", "entr\u00E9e de droit attendue"},
        {"number.", "nombre "},
        {"expected.expect.read.end.of.file.",
                "attendu [{0}], lu [fin de fichier]"},
        {"expected.read.end.of.file.",
                "attendu [;], lu [fin de fichier]"},
        {"line.number.msg", "ligne {0} : {1}"},
        {"line.number.expected.expect.found.actual.",
                "ligne {0} : attendu [{1}], trouv\u00E9 [{2}]"},
        {"null.principalClass.or.principalName",
                "principalClass ou principalName NULL"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "Mot de passe PKCS11 Token [{0}] : "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "impossible d'instancier les r\u00E8gles bas\u00E9es sur le sujet"}
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

