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
public class Resources_es extends java.util.ListResourceBundle {

    private static final Object[][] contents = {

        // javax.security.auth.PrivateCredentialPermission
        {"invalid.null.input.s.", "entradas nulas no v\u00E1lidas"},
        {"actions.can.only.be.read.", "las acciones s\u00F3lo pueden 'leerse'"},
        {"permission.name.name.syntax.invalid.",
                "sintaxis de nombre de permiso [{0}] no v\u00E1lida: "},
        {"Credential.Class.not.followed.by.a.Principal.Class.and.Name",
                "La clase de credencial no va seguida de una clase y nombre de principal"},
        {"Principal.Class.not.followed.by.a.Principal.Name",
                "La clase de principal no va seguida de un nombre de principal"},
        {"Principal.Name.must.be.surrounded.by.quotes",
                "El nombre de principal debe ir entre comillas"},
        {"Principal.Name.missing.end.quote",
                "Faltan las comillas finales en el nombre de principal"},
        {"PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value",
                "La clase de principal PrivateCredentialPermission no puede ser un valor comod\u00EDn (*) si el nombre de principal no lo es tambi\u00E9n"},
        {"CredOwner.Principal.Class.class.Principal.Name.name",
                "CredOwner:\n\tClase de Principal = {0}\n\tNombre de Principal = {1}"},

        // javax.security.auth.x500
        {"provided.null.name", "se ha proporcionado un nombre nulo"},
        {"provided.null.keyword.map", "mapa de palabras clave proporcionado nulo"},
        {"provided.null.OID.map", "mapa de OID proporcionado nulo"},

        // javax.security.auth.Subject
        {"NEWLINE", "\n"},
        {"invalid.null.AccessControlContext.provided",
                "se ha proporcionado un AccessControlContext nulo no v\u00E1lido"},
        {"invalid.null.action.provided", "se ha proporcionado una acci\u00F3n nula no v\u00E1lida"},
        {"invalid.null.Class.provided", "se ha proporcionado una clase nula no v\u00E1lida"},
        {"Subject.", "Asunto:\n"},
        {".Principal.", "\tPrincipal: "},
        {".Public.Credential.", "\tCredencial P\u00FAblica: "},
        {".Private.Credentials.inaccessible.",
                "\tCredenciales Privadas Inaccesibles\n"},
        {".Private.Credential.", "\tCredencial Privada: "},
        {".Private.Credential.inaccessible.",
                "\tCredencial Privada Inaccesible\n"},
        {"Subject.is.read.only", "El asunto es de s\u00F3lo lectura"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set",
                "intentando agregar un objeto que no es una instancia de java.security.Principal al juego principal de un asunto"},
        {"attempting.to.add.an.object.which.is.not.an.instance.of.class",
                "intentando agregar un objeto que no es una instancia de {0}"},

        // javax.security.auth.login.AppConfigurationEntry
        {"LoginModuleControlFlag.", "LoginModuleControlFlag: "},

        // javax.security.auth.login.LoginContext
        {"Invalid.null.input.name", "Entrada nula no v\u00E1lida: nombre"},
        {"No.LoginModules.configured.for.name",
         "No se han configurado LoginModules para {0}"},
        {"invalid.null.Subject.provided", "se ha proporcionado un asunto nulo no v\u00E1lido"},
        {"invalid.null.CallbackHandler.provided",
                "se ha proporcionado CallbackHandler nulo no v\u00E1lido"},
        {"null.subject.logout.called.before.login",
                "asunto nulo - se ha llamado al cierre de sesi\u00F3n antes del inicio de sesi\u00F3n"},
        {"unable.to.instantiate.LoginModule.module.because.it.does.not.provide.a.no.argument.constructor",
                "no se ha podido instanciar LoginModule, {0}, porque no incluye un constructor sin argumentos"},
        {"unable.to.instantiate.LoginModule",
                "no se ha podido instanciar LoginModule"},
        {"unable.to.instantiate.LoginModule.",
                "no se ha podido instanciar LoginModule: "},
        {"unable.to.find.LoginModule.class.",
                "no se ha encontrado la clase LoginModule: "},
        {"unable.to.access.LoginModule.",
                "no se ha podido acceder a LoginModule: "},
        {"Login.Failure.all.modules.ignored",
                "Fallo en inicio de sesi\u00F3n: se han ignorado todos los m\u00F3dulos"},

        // sun.security.provider.PolicyFile

        {"java.security.policy.error.parsing.policy.message",
                "java.security.policy: error de an\u00E1lisis de {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Permission.perm.message",
                "java.security.policy: error al agregar un permiso, {0}:\n\t{1}"},
        {"java.security.policy.error.adding.Entry.message",
                "java.security.policy: error al agregar una entrada:\n\t{0}"},
        {"alias.name.not.provided.pe.name.", "no se ha proporcionado el nombre de alias ({0})"},
        {"unable.to.perform.substitution.on.alias.suffix",
                "no se puede realizar la sustituci\u00F3n en el alias, {0}"},
        {"substitution.value.prefix.unsupported",
                "valor de sustituci\u00F3n, {0}, no soportado"},
        {"SPACE", " "},
        {"LPARAM", "("},
        {"RPARAM", ")"},
        {"type.can.t.be.null","el tipo no puede ser nulo"},

        // sun.security.provider.PolicyParser
        {"keystorePasswordURL.can.not.be.specified.without.also.specifying.keystore",
                "keystorePasswordURL no puede especificarse sin especificar tambi\u00E9n el almac\u00E9n de claves"},
        {"expected.keystore.type", "se esperaba un tipo de almac\u00E9n de claves"},
        {"expected.keystore.provider", "se esperaba un proveedor de almac\u00E9n de claves"},
        {"multiple.Codebase.expressions",
                "expresiones m\u00FAltiples de CodeBase"},
        {"multiple.SignedBy.expressions","expresiones m\u00FAltiples de SignedBy"},
        {"duplicate.keystore.domain.name","nombre de dominio de almac\u00E9n de claves duplicado: {0}"},
        {"duplicate.keystore.name","nombre de almac\u00E9n de claves duplicado: {0}"},
        {"SignedBy.has.empty.alias","SignedBy tiene un alias vac\u00EDo"},
        {"can.not.specify.Principal.with.a.wildcard.class.without.a.wildcard.name",
                "no se puede especificar Principal con una clase de comod\u00EDn sin un nombre de comod\u00EDn"},
        {"expected.codeBase.or.SignedBy.or.Principal",
                "se esperaba codeBase o SignedBy o Principal"},
        {"expected.permission.entry", "se esperaba un permiso de entrada"},
        {"number.", "n\u00FAmero "},
        {"expected.expect.read.end.of.file.",
                "se esperaba [{0}], se ha le\u00EDdo [final de archivo]"},
        {"expected.read.end.of.file.",
                "se esperaba [;], se ha le\u00EDdo [final de archivo]"},
        {"line.number.msg", "l\u00EDnea {0}: {1}"},
        {"line.number.expected.expect.found.actual.",
                "l\u00EDnea {0}: se esperaba [{1}], se ha encontrado [{2}]"},
        {"null.principalClass.or.principalName",
                "principalClass o principalName nulos"},

        // sun.security.pkcs11.SunPKCS11
        {"PKCS11.Token.providerName.Password.",
                "Contrase\u00F1a del Token PKCS11 [{0}]: "},

        /* --- DEPRECATED --- */
        // javax.security.auth.Policy
        {"unable.to.instantiate.Subject.based.policy",
                "no se ha podido instanciar una pol\u00EDtica basada en asunto"}
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

