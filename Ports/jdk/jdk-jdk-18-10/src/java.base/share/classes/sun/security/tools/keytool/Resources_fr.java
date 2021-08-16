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
public class Resources_fr extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "Options :"},
        {"option.1.set.twice", "L'option %s est sp\u00E9cifi\u00E9e plusieurs fois. Toutes les occurrences seront ignor\u00E9es, sauf la derni\u00E8re."},
        {"multiple.commands.1.2", "Une seule commande est autoris\u00E9e : %1$s et %2$s ont \u00E9t\u00E9 sp\u00E9cifi\u00E9es."},
        {"Use.keytool.help.for.all.available.commands",
                 "Utiliser \"keytool -help\" pour toutes les commandes disponibles"},
        {"Key.and.Certificate.Management.Tool",
                 "Outil de gestion de certificats et de cl\u00E9s"},
        {"Commands.", "Commandes :"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "Utilisez \"keytool -command_name -help\" pour la syntaxe de command_name.\nUtilisez l'option -conf <url> pour indiquer un fichier d'options pr\u00E9configur\u00E9es."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "G\u00E9n\u00E8re une demande de certificat"}, //-certreq
        {"Changes.an.entry.s.alias",
                "Modifie l'alias d'une entr\u00E9e"}, //-changealias
        {"Deletes.an.entry",
                "Supprime une entr\u00E9e"}, //-delete
        {"Exports.certificate",
                "Exporte le certificat"}, //-exportcert
        {"Generates.a.key.pair",
                "G\u00E9n\u00E8re une paire de cl\u00E9s"}, //-genkeypair
        {"Generates.a.secret.key",
                "G\u00E9n\u00E8re une cl\u00E9 secr\u00E8te"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "G\u00E9n\u00E8re le certificat \u00E0 partir d'une demande de certificat"}, //-gencert
        {"Generates.CRL", "G\u00E9n\u00E8re la liste des certificats r\u00E9voqu\u00E9s (CRL)"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "Cl\u00E9 secr\u00E8te {0} g\u00E9n\u00E9r\u00E9e"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "Cl\u00E9 secr\u00E8te {0} bits {1} g\u00E9n\u00E9r\u00E9e"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "Importe les entr\u00E9es \u00E0 partir d'une base de donn\u00E9es d'identit\u00E9s de type JDK 1.1.x"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "Importe un certificat ou une cha\u00EEne de certificat"}, //-importcert
        {"Imports.a.password",
                "Importe un mot de passe"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "Importe une entr\u00E9e ou la totalit\u00E9 des entr\u00E9es depuis un autre fichier de cl\u00E9s"}, //-importkeystore
        {"Clones.a.key.entry",
                "Clone une entr\u00E9e de cl\u00E9"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "Modifie le mot de passe de cl\u00E9 d'une entr\u00E9e"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "R\u00E9pertorie les entr\u00E9es d'un fichier de cl\u00E9s"}, //-list
        {"Prints.the.content.of.a.certificate",
                "Imprime le contenu d'un certificat"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "Imprime le contenu d'une demande de certificat"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "Imprime le contenu d'un fichier de liste des certificats r\u00E9voqu\u00E9s (CRL)"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "G\u00E9n\u00E8re un certificat auto-sign\u00E9"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "Modifie le mot de passe de banque d'un fichier de cl\u00E9s"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "nom d'alias de l'entr\u00E9e \u00E0 traiter"}, //-alias
        {"destination.alias",
                "alias de destination"}, //-destalias
        {"destination.key.password",
                "mot de passe de la cl\u00E9 de destination"}, //-destkeypass
        {"destination.keystore.name",
                "nom du fichier de cl\u00E9s de destination"}, //-destkeystore
        {"destination.keystore.password.protected",
                "mot de passe du fichier de cl\u00E9s de destination prot\u00E9g\u00E9"}, //-destprotected
        {"destination.keystore.provider.name",
                "nom du fournisseur du fichier de cl\u00E9s de destination"}, //-destprovidername
        {"destination.keystore.password",
                "mot de passe du fichier de cl\u00E9s de destination"}, //-deststorepass
        {"destination.keystore.type",
                "type du fichier de cl\u00E9s de destination"}, //-deststoretype
        {"distinguished.name",
                "nom distinctif"}, //-dname
        {"X.509.extension",
                "extension X.509"}, //-ext
        {"output.file.name",
                "nom du fichier de sortie"}, //-file and -outfile
        {"input.file.name",
                "nom du fichier d'entr\u00E9e"}, //-file and -infile
        {"key.algorithm.name",
                "nom de l'algorithme de cl\u00E9"}, //-keyalg
        {"key.password",
                "mot de passe de la cl\u00E9"}, //-keypass
        {"key.bit.size",
                "taille en bits de la cl\u00E9"}, //-keysize
        {"keystore.name",
                "nom du fichier de cl\u00E9s"}, //-keystore
        {"access.the.cacerts.keystore",
                "acc\u00E9der au fichier de cl\u00E9s cacerts"}, // -cacerts
        {"warning.cacerts.option",
                "Avertissement : utiliser l'option -cacerts pour acc\u00E9der au fichier de cl\u00E9s cacerts"},
        {"new.password",
                "nouveau mot de passe"}, //-new
        {"do.not.prompt",
                "ne pas inviter"}, //-noprompt
        {"password.through.protected.mechanism",
                "mot de passe via m\u00E9canisme prot\u00E9g\u00E9"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "ajouter un fournisseur de s\u00E9curit\u00E9 par nom (par ex. SunPKCS11)\nconfigurer l'argument pour -addprovider"}, //-addprovider
        {"provider.class.option",
                "ajouter un fournisseur de s\u00E9curit\u00E9 par nom de classe qualifi\u00E9 complet\nconfigurer l'argument pour -providerclass"}, //-providerclass

        {"provider.name",
                "nom du fournisseur"}, //-providername
        {"provider.classpath",
                "variable d'environnement CLASSPATH du fournisseur"}, //-providerpath
        {"output.in.RFC.style",
                "sortie au style RFC"}, //-rfc
        {"signature.algorithm.name",
                "nom de l'algorithme de signature"}, //-sigalg
        {"source.alias",
                "alias source"}, //-srcalias
        {"source.key.password",
                "mot de passe de la cl\u00E9 source"}, //-srckeypass
        {"source.keystore.name",
                "nom du fichier de cl\u00E9s source"}, //-srckeystore
        {"source.keystore.password.protected",
                "mot de passe du fichier de cl\u00E9s source prot\u00E9g\u00E9"}, //-srcprotected
        {"source.keystore.provider.name",
                "nom du fournisseur du fichier de cl\u00E9s source"}, //-srcprovidername
        {"source.keystore.password",
                "mot de passe du fichier de cl\u00E9s source"}, //-srcstorepass
        {"source.keystore.type",
                "type du fichier de cl\u00E9s source"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "Port et h\u00F4te du serveur SSL"}, //-sslserver
        {"signed.jar.file",
                "fichier JAR sign\u00E9"}, //=jarfile
        {"certificate.validity.start.date.time",
                "date/heure de d\u00E9but de validit\u00E9 du certificat"}, //-startdate
        {"keystore.password",
                "mot de passe du fichier de cl\u00E9s"}, //-storepass
        {"keystore.type",
                "type du fichier de cl\u00E9s"}, //-storetype
        {"trust.certificates.from.cacerts",
                "certificats s\u00E9curis\u00E9s issus de certificats CA"}, //-trustcacerts
        {"verbose.output",
                "sortie en mode verbose"}, //-v
        {"validity.number.of.days",
                "nombre de jours de validit\u00E9"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "ID de s\u00E9rie du certificat \u00E0 r\u00E9voquer"}, //-id
        // keytool: Running part
        {"keytool.error.", "erreur keytool : "},
        {"Illegal.option.", "Option non admise :  "},
        {"Illegal.value.", "Valeur non admise : "},
        {"Unknown.password.type.", "Type de mot de passe inconnu : "},
        {"Cannot.find.environment.variable.",
                "Variable d'environnement introuvable : "},
        {"Cannot.find.file.", "Fichier introuvable : "},
        {"Command.option.flag.needs.an.argument.", "L''option de commande {0} requiert un argument."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "Avertissement\u00A0: les mots de passe de cl\u00E9 et de banque distincts ne sont pas pris en charge pour les fichiers de cl\u00E9s d''acc\u00E8s PKCS12. La valeur {0} sp\u00E9cifi\u00E9e par l''utilisateur est ignor\u00E9e."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "Les options -keystore ou -storetype ne peuvent pas \u00EAtre utilis\u00E9es avec l'option -cacerts"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-keystore doit \u00EAtre d\u00E9fini sur NONE si -storetype est {0}"},
        {"Too.many.retries.program.terminated",
                 "Trop de tentatives, fin du programme"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "Les commandes -storepasswd et -keypasswd ne sont pas prises en charge si -storetype est d\u00E9fini sur {0}"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "Les commandes -keypasswd ne sont pas prises en charge si -storetype est d\u00E9fini sur PKCS12"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "Les commandes -keypass et -new ne peuvent pas \u00EAtre sp\u00E9cifi\u00E9es si -storetype est d\u00E9fini sur {0}"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "si -protected est sp\u00E9cifi\u00E9, -storepass, -keypass et -new ne doivent pas \u00EAtre indiqu\u00E9s"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Si -srcprotected est indiqu\u00E9, les commandes -srcstorepass et -srckeypass ne doivent pas \u00EAtre sp\u00E9cifi\u00E9es"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "Si le fichier de cl\u00E9s n'est pas prot\u00E9g\u00E9 par un mot de passe, les commandes -storepass, -keypass et -new ne doivent pas \u00EAtre sp\u00E9cifi\u00E9es"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Si le fichier de cl\u00E9s source n'est pas prot\u00E9g\u00E9 par un mot de passe, les commandes -srcstorepass et -srckeypass ne doivent pas \u00EAtre sp\u00E9cifi\u00E9es"},
        {"Illegal.startdate.value", "Valeur de date de d\u00E9but non admise"},
        {"Validity.must.be.greater.than.zero",
                "La validit\u00E9 doit \u00EAtre sup\u00E9rieure \u00E0 z\u00E9ro"},
        {"provclass.not.a.provider", "%s n'est pas un fournisseur"},
        {"provider.name.not.found", "Fournisseur nomm\u00E9 \"%s\" introuvable"},
        {"provider.class.not.found", "Fournisseur \"%s\" introuvable"},
        {"Usage.error.no.command.provided", "Erreur de syntaxe\u00A0: aucune commande fournie"},
        {"Source.keystore.file.exists.but.is.empty.", "Le fichier de cl\u00E9s source existe mais il est vide : "},
        {"Please.specify.srckeystore", "Indiquez -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "-v et -rfc ne doivent pas \u00EAtre sp\u00E9cifi\u00E9s avec la commande 'list'"},
        {"Key.password.must.be.at.least.6.characters",
                "Un mot de passe de cl\u00E9 doit comporter au moins 6 caract\u00E8res"},
        {"New.password.must.be.at.least.6.characters",
                "Le nouveau mot de passe doit comporter au moins 6 caract\u00E8res"},
        {"Keystore.file.exists.but.is.empty.",
                "Fichier de cl\u00E9s existant mais vide : "},
        {"Keystore.file.does.not.exist.",
                "Le fichier de cl\u00E9s n'existe pas : "},
        {"Must.specify.destination.alias", "L'alias de destination doit \u00EAtre sp\u00E9cifi\u00E9"},
        {"Must.specify.alias", "L'alias doit \u00EAtre sp\u00E9cifi\u00E9"},
        {"Keystore.password.must.be.at.least.6.characters",
                "Un mot de passe de fichier de cl\u00E9s doit comporter au moins 6 caract\u00E8res"},
        {"Enter.the.password.to.be.stored.",
                "Saisissez le mot de passe \u00E0 stocker :  "},
        {"Enter.keystore.password.", "Entrez le mot de passe du fichier de cl\u00E9s :  "},
        {"Enter.source.keystore.password.", "Entrez le mot de passe du fichier de cl\u00E9s source\u00A0:  "},
        {"Enter.destination.keystore.password.", "Entrez le mot de passe du fichier de cl\u00E9s de destination\u00A0:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "Le mot de passe du fichier de cl\u00E9s est trop court : il doit comporter au moins 6 caract\u00E8res"},
        {"Unknown.Entry.Type", "Type d'entr\u00E9e inconnu"},
        {"Too.many.failures.Alias.not.changed", "Trop d'erreurs. Alias non modifi\u00E9"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "L''entr\u00E9e de l''alias {0} a \u00E9t\u00E9 import\u00E9e."},
        {"Entry.for.alias.alias.not.imported.", "L''entr\u00E9e de l''alias {0} n''a pas \u00E9t\u00E9 import\u00E9e."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "Probl\u00E8me lors de l''import de l''entr\u00E9e de l''alias {0}\u00A0: {1}.\nL''entr\u00E9e de l''alias {0} n''a pas \u00E9t\u00E9 import\u00E9e."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "Commande d''import ex\u00E9cut\u00E9e\u00A0: {0} entr\u00E9es import\u00E9es, \u00E9chec ou annulation de {1} entr\u00E9es"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "Avertissement\u00A0: l''alias {0} existant sera remplac\u00E9 dans le fichier de cl\u00E9s d''acc\u00E8s de destination"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "L''alias d''entr\u00E9e {0} existe d\u00E9j\u00E0. Voulez-vous le remplacer ? [non]\u00A0:  "},
        {"Too.many.failures.try.later", "Trop d'erreurs. R\u00E9essayez plus tard"},
        {"Certification.request.stored.in.file.filename.",
                "Demande de certification stock\u00E9e dans le fichier <{0}>"},
        {"Submit.this.to.your.CA", "Soumettre \u00E0 votre CA"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "si l'alias n'est pas sp\u00E9cifi\u00E9, destalias et srckeypass ne doivent pas \u00EAtre sp\u00E9cifi\u00E9s"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "Le fichier de cl\u00E9s pkcs12 de destination contient un mot de passe de fichier de cl\u00E9s et un mot de passe de cl\u00E9 diff\u00E9rents. R\u00E9essayez en sp\u00E9cifiant -destkeypass."},
        {"Certificate.stored.in.file.filename.",
                "Certificat stock\u00E9 dans le fichier <{0}>"},
        {"Certificate.reply.was.installed.in.keystore",
                "R\u00E9ponse de certificat install\u00E9e dans le fichier de cl\u00E9s"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "R\u00E9ponse de certificat non install\u00E9e dans le fichier de cl\u00E9s"},
        {"Certificate.was.added.to.keystore",
                "Certificat ajout\u00E9 au fichier de cl\u00E9s"},
        {"Certificate.was.not.added.to.keystore",
                "Certificat non ajout\u00E9 au fichier de cl\u00E9s"},
        {".Storing.ksfname.", "[Stockage de {0}]"},
        {"alias.has.no.public.key.certificate.",
                "{0} ne poss\u00E8de pas de cl\u00E9 publique (certificat)"},
        {"Cannot.derive.signature.algorithm",
                "Impossible de d\u00E9duire l'algorithme de signature"},
        {"Alias.alias.does.not.exist",
                "L''alias <{0}> n''existe pas"},
        {"Alias.alias.has.no.certificate",
                "L''alias <{0}> ne poss\u00E8de pas de certificat"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "Paire de cl\u00E9s non g\u00E9n\u00E9r\u00E9e, l''alias <{0}> existe d\u00E9j\u00E0"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "G\u00E9n\u00E9ration d''une paire de cl\u00E9s {1} de {0} bits et d''un certificat auto-sign\u00E9 ({2}) d''une validit\u00E9 de {3} jours\n\tpour : {4}"},
        {"Enter.key.password.for.alias.", "Entrez le mot de passe de la cl\u00E9 pour <{0}>"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(appuyez sur Entr\u00E9e s'il s'agit du mot de passe du fichier de cl\u00E9s) :  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "Le mot de passe de la cl\u00E9 est trop court : il doit comporter au moins 6 caract\u00E8res"},
        {"Too.many.failures.key.not.added.to.keystore",
                "Trop d'erreurs. Cl\u00E9 non ajout\u00E9e au fichier de cl\u00E9s"},
        {"Destination.alias.dest.already.exists",
                "L''alias de la destination <{0}> existe d\u00E9j\u00E0"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "Le mot de passe est trop court : il doit comporter au moins 6 caract\u00E8res"},
        {"Too.many.failures.Key.entry.not.cloned",
                "Trop d'erreurs. Entr\u00E9e de cl\u00E9 non clon\u00E9e"},
        {"key.password.for.alias.", "mot de passe de cl\u00E9 pour <{0}>"},
        {"Keystore.entry.for.id.getName.already.exists",
                "L''entr\u00E9e de fichier de cl\u00E9s d''acc\u00E8s pour <{0}> existe d\u00E9j\u00E0"},
        {"Creating.keystore.entry.for.id.getName.",
                "Cr\u00E9ation d''une entr\u00E9e de fichier de cl\u00E9s d''acc\u00E8s pour <{0}>..."},
        {"No.entries.from.identity.database.added",
                "Aucune entr\u00E9e ajout\u00E9e \u00E0 partir de la base de donn\u00E9es d'identit\u00E9s"},
        {"Alias.name.alias", "Nom d''alias : {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "Date de cr\u00E9ation : {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "Type d''entr\u00E9e\u00A0: {0}"},
        {"Certificate.chain.length.", "Longueur de cha\u00EEne du certificat : "},
        {"Certificate.i.1.", "Certificat[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "Empreinte du certificat (SHA-256) : "},
        {"Keystore.type.", "Type de fichier de cl\u00E9s : "},
        {"Keystore.provider.", "Fournisseur de fichier de cl\u00E9s : "},
        {"Your.keystore.contains.keyStore.size.entry",
                "Votre fichier de cl\u00E9s d''acc\u00E8s contient {0,number,integer} entr\u00E9e"},
        {"Your.keystore.contains.keyStore.size.entries",
                "Votre fichier de cl\u00E9s d''acc\u00E8s contient {0,number,integer} entr\u00E9es"},
        {"Failed.to.parse.input", "L'analyse de l'entr\u00E9e a \u00E9chou\u00E9"},
        {"Empty.input", "Entr\u00E9e vide"},
        {"Not.X.509.certificate", "Pas un certificat X.509"},
        {"alias.has.no.public.key", "{0} ne poss\u00E8de pas de cl\u00E9 publique"},
        {"alias.has.no.X.509.certificate", "{0} ne poss\u00E8de pas de certificat X.509"},
        {"New.certificate.self.signed.", "Nouveau certificat (auto-sign\u00E9) :"},
        {"Reply.has.no.certificates", "La r\u00E9ponse n'a pas de certificat"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "Certificat non import\u00E9, l''alias <{0}> existe d\u00E9j\u00E0"},
        {"Input.not.an.X.509.certificate", "L'entr\u00E9e n'est pas un certificat X.509"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "Le certificat existe d\u00E9j\u00E0 dans le fichier de cl\u00E9s d''acc\u00E8s sous l''alias <{0}>"},
        {"Do.you.still.want.to.add.it.no.",
                "Voulez-vous toujours l'ajouter ? [non] :  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "Le certificat existe d\u00E9j\u00E0 dans le fichier de cl\u00E9s d''acc\u00E8s CA syst\u00E8me sous l''alias <{0}>"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "Voulez-vous toujours l'ajouter \u00E0 votre fichier de cl\u00E9s ? [non] :  "},
        {"Trust.this.certificate.no.", "Faire confiance \u00E0 ce certificat ? [non] :  "},
        {"YES", "OUI"},
        {"New.prompt.", "Nouveau {0} : "},
        {"Passwords.must.differ", "Les mots de passe doivent diff\u00E9rer"},
        {"Re.enter.new.prompt.", "Indiquez encore le nouveau {0} : "},
        {"Re.enter.password.", "R\u00E9p\u00E9tez le mot de passe : "},
        {"Re.enter.new.password.", "Ressaisissez le nouveau mot de passe : "},
        {"They.don.t.match.Try.again", "Ils sont diff\u00E9rents. R\u00E9essayez."},
        {"Enter.prompt.alias.name.", "Indiquez le nom d''alias {0} :  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "Saisissez le nom du nouvel alias\t(ou appuyez sur Entr\u00E9e pour annuler l'import de cette entr\u00E9e)\u00A0:  "},
        {"Enter.alias.name.", "Indiquez le nom d'alias :  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(appuyez sur Entr\u00E9e si le r\u00E9sultat est identique \u00E0 <{0}>)"},
        {"What.is.your.first.and.last.name.",
                "Quels sont vos nom et pr\u00E9nom ?"},
        {"What.is.the.name.of.your.organizational.unit.",
                "Quel est le nom de votre unit\u00E9 organisationnelle ?"},
        {"What.is.the.name.of.your.organization.",
                "Quel est le nom de votre entreprise ?"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "Quel est le nom de votre ville de r\u00E9sidence ?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "Quel est le nom de votre \u00E9tat ou province ?"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "Quel est le code pays \u00E0 deux lettres pour cette unit\u00E9 ?"},
        {"Is.name.correct.", "Est-ce {0} ?"},
        {"no", "non"},
        {"yes", "oui"},
        {"y", "o"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "L''alias <{0}> n''est associ\u00E9 \u00E0 aucune cl\u00E9"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "L''entr\u00E9e \u00E0 laquelle l''alias <{0}> fait r\u00E9f\u00E9rence n''est pas une entr\u00E9e de type cl\u00E9 priv\u00E9e. La commande -keyclone prend uniquement en charge le clonage des cl\u00E9s priv\u00E9es"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "Signataire n\u00B0%d :"},
        {"Timestamp.", "Horodatage :"},
        {"Signature.", "Signature :"},
        {"CRLs.", "Listes des certificats r\u00E9voqu\u00E9s (CRL) :"},
        {"Certificate.owner.", "Propri\u00E9taire du certificat : "},
        {"Not.a.signed.jar.file", "Fichier JAR non sign\u00E9"},
        {"No.certificate.from.the.SSL.server",
                "Aucun certificat du serveur SSL"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* L'int\u00E9grit\u00E9 des informations stock\u00E9es dans votre fichier de cl\u00E9s  *\n* n'a PAS \u00E9t\u00E9 v\u00E9rifi\u00E9e. Pour cela, *\n* vous devez fournir le mot de passe de votre fichier de cl\u00E9s.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* L'int\u00E9grit\u00E9 des informations stock\u00E9es dans le fichier de cl\u00E9s source  *\n* n'a PAS \u00E9t\u00E9 v\u00E9rifi\u00E9e. Pour cela, *\n* vous devez fournir le mot de passe de votre fichier de cl\u00E9s source.                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "La r\u00E9ponse au certificat ne contient pas de cl\u00E9 publique pour <{0}>"},
        {"Incomplete.certificate.chain.in.reply",
                "Cha\u00EEne de certificat incompl\u00E8te dans la r\u00E9ponse"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "La cha\u00EEne de certificat de la r\u00E9ponse ne concorde pas : "},
        {"Top.level.certificate.in.reply.",
                "Certificat de niveau sup\u00E9rieur dans la r\u00E9ponse :\n"},
        {".is.not.trusted.", "... non s\u00E9curis\u00E9. "},
        {"Install.reply.anyway.no.", "Installer la r\u00E9ponse quand m\u00EAme ? [non] :  "},
        {"NO", "NON"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "Les cl\u00E9s publiques de la r\u00E9ponse et du fichier de cl\u00E9s ne concordent pas"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "La r\u00E9ponse au certificat et le certificat du fichier de cl\u00E9s sont identiques"},
        {"Failed.to.establish.chain.from.reply",
                "Impossible de cr\u00E9er une cha\u00EEne \u00E0 partir de la r\u00E9ponse"},
        {"n", "n"},
        {"Wrong.answer.try.again", "R\u00E9ponse incorrecte, recommencez"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "Cl\u00E9 secr\u00E8te non g\u00E9n\u00E9r\u00E9e, l''alias <{0}> existe d\u00E9j\u00E0"},
        {"Please.provide.keysize.for.secret.key.generation",
                "Indiquez -keysize pour la g\u00E9n\u00E9ration de la cl\u00E9 secr\u00E8te"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "AVERTISSEMENT : non v\u00E9rifi\u00E9. Assurez-vous que -keystore est correct."},

        {"Extensions.", "Extensions\u00A0: "},
        {".Empty.value.", "(Valeur vide)"},
        {"Extension.Request.", "Demande d'extension :"},
        {"Unknown.keyUsage.type.", "Type keyUsage inconnu : "},
        {"Unknown.extendedkeyUsage.type.", "Type extendedkeyUsage inconnu : "},
        {"Unknown.AccessDescription.type.", "Type AccessDescription inconnu : "},
        {"Unrecognized.GeneralName.type.", "Type GeneralName non reconnu : "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "Cette extension ne peut pas \u00EAtre marqu\u00E9e comme critique. "},
        {"Odd.number.of.hex.digits.found.", "Nombre impair de chiffres hexad\u00E9cimaux trouv\u00E9 : "},
        {"Unknown.extension.type.", "Type d'extension inconnu : "},
        {"command.{0}.is.ambiguous.", "commande {0} ambigu\u00EB :"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "Demande de certificat"},
        {"the.issuer", "Emetteur"},
        {"the.generated.certificate", "Certificat g\u00E9n\u00E9r\u00E9"},
        {"the.generated.crl", "Liste des certificats r\u00E9voqu\u00E9s g\u00E9n\u00E9r\u00E9e"},
        {"the.generated.certificate.request", "Demande de certificat g\u00E9n\u00E9r\u00E9"},
        {"the.certificate", "Certificat"},
        {"the.crl", "Liste de certificats r\u00E9voqu\u00E9s"},
        {"the.tsa.certificate", "Certificat TSA"},
        {"the.input", "Entr\u00E9e"},
        {"reply", "R\u00E9pondre"},
        {"one.in.many", "%1$s #%2$d sur %3$d"},
        {"alias.in.cacerts", "Emetteur <%s> dans les certificats CA"},
        {"alias.in.keystore", "Emetteur <%s>"},
        {"with.weak", "%s (faible)"},
        {"key.bit", "Cl\u00E9 %2$s %1$d bits"},
        {"key.bit.weak", "Cl\u00E9 %2$s %1$d bits (faible)"},
        {"unknown.size.1", "taille de cl\u00E9 %s inconnue"},
        {".PATTERN.printX509Cert.with.weak",
                "Propri\u00E9taire : {0}\nEmetteur : {1}\nNum\u00E9ro de s\u00E9rie : {2}\nValide du {3} au {4}\nEmpreintes du certificat :\n\t SHA 1: {5}\n\t SHA 256: {6}\nNom de l''algorithme de signature : {7}\nAlgorithme de cl\u00E9 publique du sujet : {8}\nVersion : {9}"},
        {"PKCS.10.with.weak",
                "Demande de certificat PKCS #10 (version 1.0)\nSujet : %1$s\nFormat : %2$s\nCl\u00E9 publique : %3$s\nAlgorithme de signature : %4$s\n"},
        {"verified.by.s.in.s.weak", "V\u00E9rifi\u00E9 par %1$s dans %2$s avec un \u00E9l\u00E9ment %3$s"},
        {"whose.sigalg.risk", "%1$s utilise l'algorithme de signature %2$s, qui repr\u00E9sente un risque pour la s\u00E9curit\u00E9."},
        {"whose.key.risk", "%1$s utilise un \u00E9l\u00E9ment %2$s, qui repr\u00E9sente un risque pour la s\u00E9curit\u00E9."},
        {"jks.storetype.warning", "Le fichier de cl\u00E9s %1$s utilise un format propri\u00E9taire. Il est recommand\u00E9 de migrer vers PKCS12, qui est un format standard de l'industrie en utilisant \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"."},
        {"migrate.keystore.warning", "El\u00E9ment \"%1$s\" migr\u00E9 vers %4$s. Le fichier de cl\u00E9s %2$s est sauvegard\u00E9 en tant que \"%3$s\"."},
        {"backup.keystore.warning", "Le fichier de cl\u00E9s d'origine \"%1$s\" est sauvegard\u00E9 en tant que \"%3$s\"..."},
        {"importing.keystore.status", "Import du fichier de cl\u00E9s %1$s vers %2$s..."},
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
