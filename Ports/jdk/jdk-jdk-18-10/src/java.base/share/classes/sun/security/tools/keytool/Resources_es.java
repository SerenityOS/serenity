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

package sun.security.tools.keytool;

/**
 * <p> This class represents the <code>ResourceBundle</code>
 * for the keytool.
 *
 */
public class Resources_es extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "Opciones:"},
        {"option.1.set.twice", "La opci\u00F3n %s se\u00A0ha especificado\u00A0varias veces. Se ignorar\u00E1n todas excepto la \u00FAltima."},
        {"multiple.commands.1.2", "Solo se permite un comando: se ha especificado tanto %1$s como %2$s"},
        {"Use.keytool.help.for.all.available.commands",
                 "Utilice\"keytool -help\" para todos los comandos disponibles"},
        {"Key.and.Certificate.Management.Tool",
                 "Herramienta de Gesti\u00F3n de Certificados y Claves"},
        {"Commands.", "Comandos:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "Utilice \"keytool -command_name -help\" para la sintaxis de nombre_comando.\nUtilice la opci\u00F3n -conf <url> para especificar un archivo de opciones preconfigurado."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "Genera una solicitud de certificado"}, //-certreq
        {"Changes.an.entry.s.alias",
                "Cambia un alias de entrada"}, //-changealias
        {"Deletes.an.entry",
                "Suprime una entrada"}, //-delete
        {"Exports.certificate",
                "Exporta el certificado"}, //-exportcert
        {"Generates.a.key.pair",
                "Genera un par de claves"}, //-genkeypair
        {"Generates.a.secret.key",
                "Genera un clave secreta"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "Genera un certificado a partir de una solicitud de certificado"}, //-gencert
        {"Generates.CRL", "Genera CRL"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "Clave secreta {0} generada"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "Clave secreta {1} de {0} bits generada"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "Importa entradas desde una base de datos de identidades JDK 1.1.x-style"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "Importa un certificado o una cadena de certificados"}, //-importcert
        {"Imports.a.password",
                "Importa una contrase\u00F1a"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "Importa una o todas las entradas desde otro almac\u00E9n de claves"}, //-importkeystore
        {"Clones.a.key.entry",
                "Clona una entrada de clave"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "Cambia la contrase\u00F1a de clave de una entrada"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "Enumera las entradas de un almac\u00E9n de claves"}, //-list
        {"Prints.the.content.of.a.certificate",
                "Imprime el contenido de un certificado"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "Imprime el contenido de una solicitud de certificado"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "Imprime el contenido de un archivo CRL"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "Genera un certificado autofirmado"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "Cambia la contrase\u00F1a de almac\u00E9n de un almac\u00E9n de claves"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "nombre de alias de la entrada que se va a procesar"}, //-alias
        {"destination.alias",
                "alias de destino"}, //-destalias
        {"destination.key.password",
                "contrase\u00F1a de clave de destino"}, //-destkeypass
        {"destination.keystore.name",
                "nombre de almac\u00E9n de claves de destino"}, //-destkeystore
        {"destination.keystore.password.protected",
                "almac\u00E9n de claves de destino protegido por contrase\u00F1a"}, //-destprotected
        {"destination.keystore.provider.name",
                "nombre de proveedor de almac\u00E9n de claves de destino"}, //-destprovidername
        {"destination.keystore.password",
                "contrase\u00F1a de almac\u00E9n de claves de destino"}, //-deststorepass
        {"destination.keystore.type",
                "tipo de almac\u00E9n de claves de destino"}, //-deststoretype
        {"distinguished.name",
                "nombre distintivo"}, //-dname
        {"X.509.extension",
                "extensi\u00F3n X.509"}, //-ext
        {"output.file.name",
                "nombre de archivo de salida"}, //-file and -outfile
        {"input.file.name",
                "nombre de archivo de entrada"}, //-file and -infile
        {"key.algorithm.name",
                "nombre de algoritmo de clave"}, //-keyalg
        {"key.password",
                "contrase\u00F1a de clave"}, //-keypass
        {"key.bit.size",
                "tama\u00F1o de bit de clave"}, //-keysize
        {"keystore.name",
                "nombre de almac\u00E9n de claves"}, //-keystore
        {"access.the.cacerts.keystore",
                "acceso al almac\u00E9n de claves cacerts"}, // -cacerts
        {"warning.cacerts.option",
                "Advertencia: Utilice la opci\u00F3n -cacerts para acceder al almac\u00E9n de claves cacerts"},
        {"new.password",
                "nueva contrase\u00F1a"}, //-new
        {"do.not.prompt",
                "no solicitar"}, //-noprompt
        {"password.through.protected.mechanism",
                "contrase\u00F1a a trav\u00E9s de mecanismo protegido"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "agregar proveedor de seguridad por nombre (por ejemplo, SunPKCS11)\nconfigurar elemento para -addprovider"}, //-addprovider
        {"provider.class.option",
                "agregar proveedor de seguridad por nombre de clase totalmente cualificado\nconfigurar argumento para -providerclass"}, //-providerclass

        {"provider.name",
                "nombre del proveedor"}, //-providername
        {"provider.classpath",
                "classpath de proveedor"}, //-providerpath
        {"output.in.RFC.style",
                "salida en estilo RFC"}, //-rfc
        {"signature.algorithm.name",
                "nombre de algoritmo de firma"}, //-sigalg
        {"source.alias",
                "alias de origen"}, //-srcalias
        {"source.key.password",
                "contrase\u00F1a de clave de origen"}, //-srckeypass
        {"source.keystore.name",
                "nombre de almac\u00E9n de claves de origen"}, //-srckeystore
        {"source.keystore.password.protected",
                "almac\u00E9n de claves de origen protegido por contrase\u00F1a"}, //-srcprotected
        {"source.keystore.provider.name",
                "nombre de proveedor de almac\u00E9n de claves de origen"}, //-srcprovidername
        {"source.keystore.password",
                "contrase\u00F1a de almac\u00E9n de claves de origen"}, //-srcstorepass
        {"source.keystore.type",
                "tipo de almac\u00E9n de claves de origen"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "puerto y host del servidor SSL"}, //-sslserver
        {"signed.jar.file",
                "archivo jar firmado"}, //=jarfile
        {"certificate.validity.start.date.time",
                "fecha/hora de inicio de validez del certificado"}, //-startdate
        {"keystore.password",
                "contrase\u00F1a de almac\u00E9n de claves"}, //-storepass
        {"keystore.type",
                "tipo de almac\u00E9n de claves"}, //-storetype
        {"trust.certificates.from.cacerts",
                "certificados de protecci\u00F3n de cacerts"}, //-trustcacerts
        {"verbose.output",
                "salida detallada"}, //-v
        {"validity.number.of.days",
                "n\u00FAmero de validez de d\u00EDas"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "identificador de serie del certificado que se va a revocar"}, //-id
        // keytool: Running part
        {"keytool.error.", "error de herramienta de claves: "},
        {"Illegal.option.", "Opci\u00F3n no permitida:  "},
        {"Illegal.value.", "Valor no permitido: "},
        {"Unknown.password.type.", "Tipo de contrase\u00F1a desconocido: "},
        {"Cannot.find.environment.variable.",
                "No se ha encontrado la variable del entorno: "},
        {"Cannot.find.file.", "No se ha encontrado el archivo: "},
        {"Command.option.flag.needs.an.argument.", "La opci\u00F3n de comando {0} necesita un argumento."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "Advertencia: los almacenes de claves en formato PKCS12 no admiten contrase\u00F1as de clave y almacenamiento distintas. Se ignorar\u00E1 el valor especificado por el usuario, {0}."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "Las opciones -keystore o -storetype no se pueden utilizar con la opci\u00F3n -cacerts"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-keystore debe ser NONE si -storetype es {0}"},
        {"Too.many.retries.program.terminated",
                 "Ha habido demasiados intentos, se ha cerrado el programa"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "Los comandos -storepasswd y -keypasswd no est\u00E1n soportados si -storetype es {0}"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "Los comandos -keypasswd no est\u00E1n soportados si -storetype es PKCS12"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "-keypass y -new no se pueden especificar si -storetype es {0}"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "si se especifica -protected, no deben especificarse -storepass, -keypass ni -new"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Si se especifica -srcprotected, no se puede especificar -srcstorepass ni -srckeypass"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "Si keystore no est\u00E1 protegido por contrase\u00F1a, no se deben especificar -storepass, -keypass ni -new"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Si el almac\u00E9n de claves de origen no est\u00E1 protegido por contrase\u00F1a, no se deben especificar -srcstorepass ni -srckeypass"},
        {"Illegal.startdate.value", "Valor de fecha de inicio no permitido"},
        {"Validity.must.be.greater.than.zero",
                "La validez debe ser mayor que cero"},
        {"provclass.not.a.provider", "%s no es un proveedor"},
        {"provider.name.not.found", "No se ha encontrado el proveedor denominado \"%s\""},
        {"provider.class.not.found", "No se ha encontrado el proveedor \"%s\""},
        {"Usage.error.no.command.provided", "Error de sintaxis: no se ha proporcionado ning\u00FAn comando"},
        {"Source.keystore.file.exists.but.is.empty.", "El archivo de almac\u00E9n de claves de origen existe, pero est\u00E1 vac\u00EDo: "},
        {"Please.specify.srckeystore", "Especifique -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "No se deben especificar -v y -rfc simult\u00E1neamente con el comando 'list'"},
        {"Key.password.must.be.at.least.6.characters",
                "La contrase\u00F1a de clave debe tener al menos 6 caracteres"},
        {"New.password.must.be.at.least.6.characters",
                "La nueva contrase\u00F1a debe tener al menos 6 caracteres"},
        {"Keystore.file.exists.but.is.empty.",
                "El archivo de almac\u00E9n de claves existe, pero est\u00E1 vac\u00EDo: "},
        {"Keystore.file.does.not.exist.",
                "El archivo de almac\u00E9n de claves no existe: "},
        {"Must.specify.destination.alias", "Se debe especificar un alias de destino"},
        {"Must.specify.alias", "Se debe especificar un alias"},
        {"Keystore.password.must.be.at.least.6.characters",
                "La contrase\u00F1a del almac\u00E9n de claves debe tener al menos 6 caracteres"},
        {"Enter.the.password.to.be.stored.",
                "Introduzca la contrase\u00F1a que se va a almacenar:  "},
        {"Enter.keystore.password.", "Introduzca la contrase\u00F1a del almac\u00E9n de claves:  "},
        {"Enter.source.keystore.password.", "Introduzca la contrase\u00F1a de almac\u00E9n de claves de origen:  "},
        {"Enter.destination.keystore.password.", "Introduzca la contrase\u00F1a de almac\u00E9n de claves de destino:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "La contrase\u00F1a del almac\u00E9n de claves es demasiado corta, debe tener al menos 6 caracteres"},
        {"Unknown.Entry.Type", "Tipo de Entrada Desconocido"},
        {"Too.many.failures.Alias.not.changed", "Demasiados fallos. No se ha cambiado el alias"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "La entrada del alias {0} se ha importado correctamente."},
        {"Entry.for.alias.alias.not.imported.", "La entrada del alias {0} no se ha importado."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "Problema al importar la entrada del alias {0}: {1}.\nNo se ha importado la entrada del alias {0}."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "Comando de importaci\u00F3n completado: {0} entradas importadas correctamente, {1} entradas incorrectas o canceladas"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "Advertencia: se sobrescribir\u00E1 el alias {0} en el almac\u00E9n de claves de destino"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "El alias de entrada existente {0} ya existe, \u00BFdesea sobrescribirlo? [no]:  "},
        {"Too.many.failures.try.later", "Demasiados fallos; int\u00E9ntelo m\u00E1s adelante"},
        {"Certification.request.stored.in.file.filename.",
                "Solicitud de certificaci\u00F3n almacenada en el archivo <{0}>"},
        {"Submit.this.to.your.CA", "Enviar a la CA"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "si no se especifica el alias, no se debe especificar destalias ni srckeypass"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "El almac\u00E9n de claves pkcs12 de destino tiene storepass y keypass diferentes. Vuelva a intentarlo con -destkeypass especificado."},
        {"Certificate.stored.in.file.filename.",
                "Certificado almacenado en el archivo <{0}>"},
        {"Certificate.reply.was.installed.in.keystore",
                "Se ha instalado la respuesta del certificado en el almac\u00E9n de claves"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "No se ha instalado la respuesta del certificado en el almac\u00E9n de claves"},
        {"Certificate.was.added.to.keystore",
                "Se ha agregado el certificado al almac\u00E9n de claves"},
        {"Certificate.was.not.added.to.keystore",
                "No se ha agregado el certificado al almac\u00E9n de claves"},
        {".Storing.ksfname.", "[Almacenando {0}]"},
        {"alias.has.no.public.key.certificate.",
                "{0} no tiene clave p\u00FAblica (certificado)"},
        {"Cannot.derive.signature.algorithm",
                "No se puede derivar el algoritmo de firma"},
        {"Alias.alias.does.not.exist",
                "El alias <{0}> no existe"},
        {"Alias.alias.has.no.certificate",
                "El alias <{0}> no tiene certificado"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "No se ha generado el par de claves, el alias <{0}> ya existe"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "Generando par de claves {1} de {0} bits para certificado autofirmado ({2}) con una validez de {3} d\u00EDas\n\tpara: {4}"},
        {"Enter.key.password.for.alias.", "Introduzca la contrase\u00F1a de clave para <{0}>"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(INTRO si es la misma contrase\u00F1a que la del almac\u00E9n de claves):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "La contrase\u00F1a de clave es demasiado corta; debe tener al menos 6 caracteres"},
        {"Too.many.failures.key.not.added.to.keystore",
                "Demasiados fallos; no se ha agregado la clave al almac\u00E9n de claves"},
        {"Destination.alias.dest.already.exists",
                "El alias de destino <{0}> ya existe"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "La contrase\u00F1a es demasiado corta; debe tener al menos 6 caracteres"},
        {"Too.many.failures.Key.entry.not.cloned",
                "Demasiados fallos. No se ha clonado la entrada de clave"},
        {"key.password.for.alias.", "contrase\u00F1a de clave para <{0}>"},
        {"Keystore.entry.for.id.getName.already.exists",
                "La entrada de almac\u00E9n de claves para <{0}> ya existe"},
        {"Creating.keystore.entry.for.id.getName.",
                "Creando entrada de almac\u00E9n de claves para <{0}> ..."},
        {"No.entries.from.identity.database.added",
                "No se han agregado entradas de la base de datos de identidades"},
        {"Alias.name.alias", "Nombre de Alias: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "Fecha de Creaci\u00F3n: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "Tipo de Entrada: {0}"},
        {"Certificate.chain.length.", "Longitud de la Cadena de Certificado: "},
        {"Certificate.i.1.", "Certificado[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "Huella de certificado (SHA-256): "},
        {"Keystore.type.", "Tipo de Almac\u00E9n de Claves: "},
        {"Keystore.provider.", "Proveedor de Almac\u00E9n de Claves: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "Su almac\u00E9n de claves contiene {0,number,integer} entrada"},
        {"Your.keystore.contains.keyStore.size.entries",
                "Su almac\u00E9n de claves contiene {0,number,integer} entradas"},
        {"Failed.to.parse.input", "Fallo al analizar la entrada"},
        {"Empty.input", "Entrada vac\u00EDa"},
        {"Not.X.509.certificate", "No es un certificado X.509"},
        {"alias.has.no.public.key", "{0} no tiene clave p\u00FAblica"},
        {"alias.has.no.X.509.certificate", "{0} no tiene certificado X.509"},
        {"New.certificate.self.signed.", "Nuevo Certificado (Autofirmado):"},
        {"Reply.has.no.certificates", "La respuesta no tiene certificados"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "Certificado no importado, el alias <{0}> ya existe"},
        {"Input.not.an.X.509.certificate", "La entrada no es un certificado X.509"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "El certificado ya existe en el almac\u00E9n de claves con el alias <{0}>"},
        {"Do.you.still.want.to.add.it.no.",
                "\u00BFA\u00FAn desea agregarlo? [no]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "El certificado ya existe en el almac\u00E9n de claves de la CA del sistema, con el alias <{0}>"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "\u00BFA\u00FAn desea agregarlo a su propio almac\u00E9n de claves? [no]:  "},
        {"Trust.this.certificate.no.", "\u00BFConfiar en este certificado? [no]:  "},
        {"YES", "S\u00CD"},
        {"New.prompt.", "Nuevo {0}: "},
        {"Passwords.must.differ", "Las contrase\u00F1as deben ser distintas"},
        {"Re.enter.new.prompt.", "Vuelva a escribir el nuevo {0}: "},
        {"Re.enter.password.", "Vuelva a introducir la contrase\u00F1a: "},
        {"Re.enter.new.password.", "Volver a escribir la contrase\u00F1a nueva: "},
        {"They.don.t.match.Try.again", "No coinciden. Int\u00E9ntelo de nuevo"},
        {"Enter.prompt.alias.name.", "Escriba el nombre de alias de {0}:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "Indique el nuevo nombre de alias\t(INTRO para cancelar la importaci\u00F3n de esta entrada):  "},
        {"Enter.alias.name.", "Introduzca el nombre de alias:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(INTRO si es el mismo que para <{0}>)"},
        {"What.is.your.first.and.last.name.",
                "\u00BFCu\u00E1les son su nombre y su apellido?"},
        {"What.is.the.name.of.your.organizational.unit.",
                "\u00BFCu\u00E1l es el nombre de su unidad de organizaci\u00F3n?"},
        {"What.is.the.name.of.your.organization.",
                "\u00BFCu\u00E1l es el nombre de su organizaci\u00F3n?"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "\u00BFCu\u00E1l es el nombre de su ciudad o localidad?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "\u00BFCu\u00E1l es el nombre de su estado o provincia?"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "\u00BFCu\u00E1l es el c\u00F3digo de pa\u00EDs de dos letras de la unidad?"},
        {"Is.name.correct.", "\u00BFEs correcto {0}?"},
        {"no", "no"},
        {"yes", "s\u00ED"},
        {"y", "s"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "El alias <{0}> no tiene clave"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "El alias <{0}> hace referencia a un tipo de entrada que no es una clave privada. El comando -keyclone s\u00F3lo permite la clonaci\u00F3n de entradas de claves privadas"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "#%d de Firmante:"},
        {"Timestamp.", "Registro de Hora:"},
        {"Signature.", "Firma:"},
        {"CRLs.", "CRL:"},
        {"Certificate.owner.", "Propietario del Certificado: "},
        {"Not.a.signed.jar.file", "No es un archivo jar firmado"},
        {"No.certificate.from.the.SSL.server",
                "Ning\u00FAn certificado del servidor SSL"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* La integridad de la informaci\u00F3n almacenada en el almac\u00E9n de claves  *\n* NO se ha comprobado.  Para comprobar dicha integridad, *\n* debe proporcionar la contrase\u00F1a del almac\u00E9n de claves.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* La integridad de la informaci\u00F3n almacenada en srckeystore*\n* NO se ha comprobado.  Para comprobar dicha integridad, *\n* debe proporcionar la contrase\u00F1a de srckeystore.                *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "La respuesta de certificado no contiene una clave p\u00FAblica para <{0}>"},
        {"Incomplete.certificate.chain.in.reply",
                "Cadena de certificado incompleta en la respuesta"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "La cadena de certificado de la respuesta no verifica: "},
        {"Top.level.certificate.in.reply.",
                "Certificado de nivel superior en la respuesta:\n"},
        {".is.not.trusted.", "... no es de confianza. "},
        {"Install.reply.anyway.no.", "\u00BFInstalar respuesta de todos modos? [no]:  "},
        {"NO", "NO"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "Las claves p\u00FAblicas en la respuesta y en el almac\u00E9n de claves no coinciden"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "La respuesta del certificado y el certificado en el almac\u00E9n de claves son id\u00E9nticos"},
        {"Failed.to.establish.chain.from.reply",
                "No se ha podido definir una cadena a partir de la respuesta"},
        {"n", "n"},
        {"Wrong.answer.try.again", "Respuesta incorrecta, vuelva a intentarlo"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "No se ha generado la clave secreta, el alias <{0}> ya existe"},
        {"Please.provide.keysize.for.secret.key.generation",
                "Proporcione el valor de -keysize para la generaci\u00F3n de claves secretas"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "ADVERTENCIA: no se ha verificado. Aseg\u00FArese de que el valor de -keystore es correcto."},

        {"Extensions.", "Extensiones: "},
        {".Empty.value.", "(Valor vac\u00EDo)"},
        {"Extension.Request.", "Solicitud de Extensi\u00F3n:"},
        {"Unknown.keyUsage.type.", "Tipo de uso de clave desconocido: "},
        {"Unknown.extendedkeyUsage.type.", "Tipo de uso de clave extendida desconocido: "},
        {"Unknown.AccessDescription.type.", "Tipo de descripci\u00F3n de acceso desconocido: "},
        {"Unrecognized.GeneralName.type.", "Tipo de nombre general no reconocido: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "Esta extensi\u00F3n no se puede marcar como cr\u00EDtica. "},
        {"Odd.number.of.hex.digits.found.", "Se ha encontrado un n\u00FAmero impar de d\u00EDgitos hexadecimales: "},
        {"Unknown.extension.type.", "Tipo de extensi\u00F3n desconocida: "},
        {"command.{0}.is.ambiguous.", "El comando {0} es ambiguo:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "La solicitud de certificado"},
        {"the.issuer", "El emisor"},
        {"the.generated.certificate", "El certificado generado"},
        {"the.generated.crl", "La CRL generada"},
        {"the.generated.certificate.request", "La solicitud de certificado generada"},
        {"the.certificate", "El certificado"},
        {"the.crl", "La CRL"},
        {"the.tsa.certificate", "El certificado de TSA"},
        {"the.input", "La entrada"},
        {"reply", "Responder"},
        {"one.in.many", "%1$s #%2$d de %3$d"},
        {"alias.in.cacerts", "Emisor <%s> en cacerts"},
        {"alias.in.keystore", "Emisor <%s>"},
        {"with.weak", "%s (d\u00E9bil)"},
        {"key.bit", "Clave %2$s de %1$d bits"},
        {"key.bit.weak", "Clave %2$s de %1$d bits (d\u00E9bil)"},
        {"unknown.size.1", "clave %s de tama\u00F1o desconocido"},
        {".PATTERN.printX509Cert.with.weak",
                "Propietario: {0}\nEmisor: {1}\nN\u00FAmero de serie: {2}\nV\u00E1lido desde: {3} hasta: {4}\nHuellas digitales del certificado:\n\t SHA1: {5}\n\t SHA256: {6}\nNombre del algoritmo de firma: {7}\nAlgoritmo de clave p\u00FAblica de asunto: {8}\nVersi\u00F3n: {9}"},
        {"PKCS.10.with.weak",
                "Solicitud de certificado PKCS #10 (Versi\u00F3n 1.0)\nAsunto: %1$s\nFormato: %2$s\nClave p\u00FAblica: %3$s\nAlgoritmo de firma: %4$s\n"},
        {"verified.by.s.in.s.weak", "Verificado por %1$s en %2$s con %3$s"},
        {"whose.sigalg.risk", "%1$s utiliza el algoritmo de firma %2$s, lo que se considera un riesgo de seguridad."},
        {"whose.key.risk", "%1$s utiliza %2$s, lo que se considera riesgo de seguridad."},
        {"jks.storetype.warning", "El almac\u00E9n de claves %1$s utiliza un formato propietario. Se recomienda migrar a PKCS12, que es un formato est\u00E1ndar del sector que utiliza \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"."},
        {"migrate.keystore.warning", "Se ha migrado \"%1$s\" a %4$s. Se ha realizado la copia de seguridad del almac\u00E9n de claves %2$s como \"%3$s\"."},
        {"backup.keystore.warning", "La copia de seguridad del almac\u00E9n de claves \"%1$s\" se ha realizado como \"%3$s\"..."},
        {"importing.keystore.status", "Importando el almac\u00E9n de claves de %1$s a %2$s..."},
    };


    /**
     * Returns the contents of this <code>ResourceBundle</code>.
     *
     * <p>
     *
     * @return the contents of this <code>ResourceBundle</code>.
     */
    @Override
    public Object[][] getContents() {
        return contents;
    }
}
