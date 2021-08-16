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
public class Resources_it extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "Opzioni:"},
        {"option.1.set.twice", "L'opzione %s \u00E8 specificata pi\u00F9 volte. Tutte le ricorrenze verranno ignorate tranne l'ultima."},
        {"multiple.commands.1.2", "\u00C8 consentito un solo comando: \u00E8 stato specificato sia %1$s che %2$s."},
        {"Use.keytool.help.for.all.available.commands",
                 "Utilizzare \"keytool -help\" per visualizzare tutti i comandi disponibili"},
        {"Key.and.Certificate.Management.Tool",
                 "Strumento di gestione di chiavi e certificati"},
        {"Commands.", "Comandi:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "Utilizzare \"keytool -command_name -help\" per informazioni sull'uso di command_name.\nUtilizzare l'opzione -conf <url> per specificare un file di opzioni preconfigurato."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "Genera una richiesta di certificato"}, //-certreq
        {"Changes.an.entry.s.alias",
                "Modifica l'alias di una voce"}, //-changealias
        {"Deletes.an.entry",
                "Elimina una voce"}, //-delete
        {"Exports.certificate",
                "Esporta il certificato"}, //-exportcert
        {"Generates.a.key.pair",
                "Genera una coppia di chiavi"}, //-genkeypair
        {"Generates.a.secret.key",
                "Genera una chiave segreta"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "Genera un certificato da una richiesta di certificato"}, //-gencert
        {"Generates.CRL", "Genera CRL"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "Generata chiave segreta {0}"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "Generata chiave segreta {1} a {0} bit"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "Importa le voci da un database delle identit\u00E0 di tipo JDK 1.1.x"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "Importa un certificato o una catena di certificati"}, //-importcert
        {"Imports.a.password",
                "Importa una password"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "Importa una o tutte le voci da un altro keystore"}, //-importkeystore
        {"Clones.a.key.entry",
                "Duplica una voce di chiave"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "Modifica la password della chiave per una voce"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "Elenca le voci in un keystore"}, //-list
        {"Prints.the.content.of.a.certificate",
                "Visualizza i contenuti di un certificato"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "Visualizza i contenuti di una richiesta di certificato"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "Visualizza i contenuti di un file CRL"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "Genera certificato con firma automatica"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "Modifica la password di area di memorizzazione di un keystore"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "nome alias della voce da elaborare"}, //-alias
        {"destination.alias",
                "alias di destinazione"}, //-destalias
        {"destination.key.password",
                "password chiave di destinazione"}, //-destkeypass
        {"destination.keystore.name",
                "nome keystore di destinazione"}, //-destkeystore
        {"destination.keystore.password.protected",
                "password keystore di destinazione protetta"}, //-destprotected
        {"destination.keystore.provider.name",
                "nome provider keystore di destinazione"}, //-destprovidername
        {"destination.keystore.password",
                "password keystore di destinazione"}, //-deststorepass
        {"destination.keystore.type",
                "tipo keystore di destinazione"}, //-deststoretype
        {"distinguished.name",
                "nome distinto"}, //-dname
        {"X.509.extension",
                "estensione X.509"}, //-ext
        {"output.file.name",
                "nome file di output"}, //-file and -outfile
        {"input.file.name",
                "nome file di input"}, //-file and -infile
        {"key.algorithm.name",
                "nome algoritmo chiave"}, //-keyalg
        {"key.password",
                "password chiave"}, //-keypass
        {"key.bit.size",
                "dimensione bit chiave"}, //-keysize
        {"keystore.name",
                "nome keystore"}, //-keystore
        {"access.the.cacerts.keystore",
                "accedi al keystore cacerts"}, // -cacerts
        {"warning.cacerts.option",
                "Avvertenza: utilizzare l'opzione -cacerts per accedere al keystore cacerts"},
        {"new.password",
                "nuova password"}, //-new
        {"do.not.prompt",
                "non richiedere"}, //-noprompt
        {"password.through.protected.mechanism",
                "password mediante meccanismo protetto"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "aggiunge il provider di sicurezza in base al nome (ad esempio SunPKCS11)\nconfigura l'argomento per -addprovider"}, //-addprovider
        {"provider.class.option",
                "aggiunge il provider di sicurezza in base al nome di classe completamente qualificato\nconfigura l'argomento per -providerclass"}, //-providerclass

        {"provider.name",
                "nome provider"}, //-providername
        {"provider.classpath",
                "classpath provider"}, //-providerpath
        {"output.in.RFC.style",
                "output in stile RFC"}, //-rfc
        {"signature.algorithm.name",
                "nome algoritmo firma"}, //-sigalg
        {"source.alias",
                "alias origine"}, //-srcalias
        {"source.key.password",
                "password chiave di origine"}, //-srckeypass
        {"source.keystore.name",
                "nome keystore di origine"}, //-srckeystore
        {"source.keystore.password.protected",
                "password keystore di origine protetta"}, //-srcprotected
        {"source.keystore.provider.name",
                "nome provider keystore di origine"}, //-srcprovidername
        {"source.keystore.password",
                "password keystore di origine"}, //-srcstorepass
        {"source.keystore.type",
                "tipo keystore di origine"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "host e porta server SSL"}, //-sslserver
        {"signed.jar.file",
                "file jar firmato"}, //=jarfile
        {"certificate.validity.start.date.time",
                "data/ora di inizio validit\u00E0 certificato"}, //-startdate
        {"keystore.password",
                "password keystore"}, //-storepass
        {"keystore.type",
                "tipo keystore"}, //-storetype
        {"trust.certificates.from.cacerts",
                "considera sicuri i certificati da cacerts"}, //-trustcacerts
        {"verbose.output",
                "output descrittivo"}, //-v
        {"validity.number.of.days",
                "numero di giorni di validit\u00E0"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "ID seriale del certificato da revocare"}, //-id
        // keytool: Running part
        {"keytool.error.", "Errore keytool: "},
        {"Illegal.option.", "Opzione non valida:  "},
        {"Illegal.value.", "Valore non valido: "},
        {"Unknown.password.type.", "Tipo di password sconosciuto: "},
        {"Cannot.find.environment.variable.",
                "Impossibile trovare la variabile di ambiente: "},
        {"Cannot.find.file.", "Impossibile trovare il file: "},
        {"Command.option.flag.needs.an.argument.", "\u00C8 necessario specificare un argomento per l''opzione di comando {0}."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "Avvertenza: non sono supportate password diverse di chiave e di archivio per i keystore PKCS12. Il valore {0} specificato dall''utente verr\u00E0 ignorato."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "L'opzione -keystore o -storetype non pu\u00F2 essere utilizzata con l'opzione -cacerts"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "Se -storetype \u00E8 impostato su {0}, -keystore deve essere impostato su NONE"},
        {"Too.many.retries.program.terminated",
                 "Il numero dei tentativi consentiti \u00E8 stato superato. Il programma verr\u00E0 terminato."},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "Se -storetype \u00E8 impostato su {0}, i comandi -storepasswd e -keypasswd non sono supportati"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "Se -storetype \u00E8 impostato su PKCS12 i comandi -keypasswd non vengono supportati"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "Se -storetype \u00E8 impostato su {0}, non \u00E8 possibile specificare un valore per -keypass e -new"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "Se \u00E8 specificata l'opzione -protected, le opzioni -storepass, -keypass e -new non possono essere specificate"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Se viene specificato -srcprotected, -srcstorepass e -srckeypass non dovranno essere specificati"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "Se il file keystore non \u00E8 protetto da password, non deve essere specificato alcun valore per -storepass, -keypass e -new"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Se il file keystore non \u00E8 protetto da password, non deve essere specificato alcun valore per -srcstorepass e -srckeypass"},
        {"Illegal.startdate.value", "Valore di data di inizio non valido"},
        {"Validity.must.be.greater.than.zero",
                "La validit\u00E0 deve essere maggiore di zero"},
        {"provclass.not.a.provider", "%s non \u00E8 un provider"},
        {"provider.name.not.found", "Provider denominato \"%s\" non trovato"},
        {"provider.class.not.found", "Provider \"%s\" non trovato"},
        {"Usage.error.no.command.provided", "Errore di utilizzo: nessun comando specificato"},
        {"Source.keystore.file.exists.but.is.empty.", "Il file keystore di origine esiste, ma \u00E8 vuoto: "},
        {"Please.specify.srckeystore", "Specificare -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "Impossibile specificare sia -v sia -rfc con il comando 'list'"},
        {"Key.password.must.be.at.least.6.characters",
                "La password della chiave deve contenere almeno 6 caratteri"},
        {"New.password.must.be.at.least.6.characters",
                "La nuova password deve contenere almeno 6 caratteri"},
        {"Keystore.file.exists.but.is.empty.",
                "Il file keystore esiste ma \u00E8 vuoto: "},
        {"Keystore.file.does.not.exist.",
                "Il file keystore non esiste: "},
        {"Must.specify.destination.alias", "\u00C8 necessario specificare l'alias di destinazione"},
        {"Must.specify.alias", "\u00C8 necessario specificare l'alias"},
        {"Keystore.password.must.be.at.least.6.characters",
                "La password del keystore deve contenere almeno 6 caratteri"},
        {"Enter.the.password.to.be.stored.",
                "Immettere la password da memorizzare:  "},
        {"Enter.keystore.password.", "Immettere la password del keystore:  "},
        {"Enter.source.keystore.password.", "Immettere la password del keystore di origine:  "},
        {"Enter.destination.keystore.password.", "Immettere la password del keystore di destinazione:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "La password del keystore \u00E8 troppo corta - deve contenere almeno 6 caratteri"},
        {"Unknown.Entry.Type", "Tipo di voce sconosciuto"},
        {"Too.many.failures.Alias.not.changed", "Numero eccessivo di errori. L'alias non \u00E8 stato modificato."},
        {"Entry.for.alias.alias.successfully.imported.",
                 "La voce dell''alias {0} \u00E8 stata importata."},
        {"Entry.for.alias.alias.not.imported.", "La voce dell''alias {0} non \u00E8 stata importata."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "Si \u00E8 verificato un problema durante l''importazione della voce dell''alias {0}: {1}.\nLa voce dell''alias {0} non \u00E8 stata importata."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "Comando di importazione completato: {0} voce/i importata/e, {1} voce/i non importata/e o annullata/e"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "Avvertenza: sovrascrittura in corso dell''alias {0} nel file keystore di destinazione"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "La voce dell''alias {0} esiste gi\u00E0. Sovrascrivere? [no]:  "},
        {"Too.many.failures.try.later", "Troppi errori - riprovare"},
        {"Certification.request.stored.in.file.filename.",
                "La richiesta di certificazione \u00E8 memorizzata nel file <{0}>"},
        {"Submit.this.to.your.CA", "Sottomettere alla propria CA"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "Se l'alias non \u00E8 specificato, destalias e srckeypass non dovranno essere specificati"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "Keystore pkcs12 di destinazione con storepass e keypass differenti. Riprovare con -destkeypass specificato."},
        {"Certificate.stored.in.file.filename.",
                "Il certificato \u00E8 memorizzato nel file <{0}>"},
        {"Certificate.reply.was.installed.in.keystore",
                "La risposta del certificato \u00E8 stata installata nel keystore"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "La risposta del certificato non \u00E8 stata installata nel keystore"},
        {"Certificate.was.added.to.keystore",
                "Il certificato \u00E8 stato aggiunto al keystore"},
        {"Certificate.was.not.added.to.keystore",
                "Il certificato non \u00E8 stato aggiunto al keystore"},
        {".Storing.ksfname.", "[Memorizzazione di {0}] in corso"},
        {"alias.has.no.public.key.certificate.",
                "{0} non dispone di chiave pubblica (certificato)"},
        {"Cannot.derive.signature.algorithm",
                "Impossibile derivare l'algoritmo di firma"},
        {"Alias.alias.does.not.exist",
                "L''alias <{0}> non esiste"},
        {"Alias.alias.has.no.certificate",
                "L''alias <{0}> non dispone di certificato"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "Non \u00E8 stata generata la coppia di chiavi, l''alias <{0}> \u00E8 gi\u00E0 esistente"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "Generazione in corso di una coppia di chiavi {1} da {0} bit e di un certificato autofirmato ({2}) con una validit\u00E0 di {3} giorni\n\tper: {4}"},
        {"Enter.key.password.for.alias.", "Immettere la password della chiave per <{0}>"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(INVIO se corrisponde alla password del keystore):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "La password della chiave \u00E8 troppo corta - deve contenere almeno 6 caratteri"},
        {"Too.many.failures.key.not.added.to.keystore",
                "Troppi errori - la chiave non \u00E8 stata aggiunta al keystore"},
        {"Destination.alias.dest.already.exists",
                "L''alias di destinazione <{0}> \u00E8 gi\u00E0 esistente"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "La password \u00E8 troppo corta - deve contenere almeno 6 caratteri"},
        {"Too.many.failures.Key.entry.not.cloned",
                "Numero eccessivo di errori. Il valore della chiave non \u00E8 stato copiato."},
        {"key.password.for.alias.", "password della chiave per <{0}>"},
        {"Keystore.entry.for.id.getName.already.exists",
                "La voce del keystore per <{0}> esiste gi\u00E0"},
        {"Creating.keystore.entry.for.id.getName.",
                "Creazione della voce del keystore per <{0}> in corso..."},
        {"No.entries.from.identity.database.added",
                "Nessuna voce aggiunta dal database delle identit\u00E0"},
        {"Alias.name.alias", "Nome alias: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "Data di creazione: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "Tipo di voce: {0}"},
        {"Certificate.chain.length.", "Lunghezza catena certificati: "},
        {"Certificate.i.1.", "Certificato[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "Copia di certificato (SHA-256): "},
        {"Keystore.type.", "Tipo keystore: "},
        {"Keystore.provider.", "Provider keystore: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "Il keystore contiene {0,number,integer} voce"},
        {"Your.keystore.contains.keyStore.size.entries",
                "Il keystore contiene {0,number,integer} voci"},
        {"Failed.to.parse.input", "Impossibile analizzare l'input"},
        {"Empty.input", "Input vuoto"},
        {"Not.X.509.certificate", "Il certificato non \u00E8 X.509"},
        {"alias.has.no.public.key", "{0} non dispone di chiave pubblica"},
        {"alias.has.no.X.509.certificate", "{0} non dispone di certificato X.509"},
        {"New.certificate.self.signed.", "Nuovo certificato (autofirmato):"},
        {"Reply.has.no.certificates", "La risposta non dispone di certificati"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "Impossibile importare il certificato, l''alias <{0}> \u00E8 gi\u00E0 esistente"},
        {"Input.not.an.X.509.certificate", "L'input non \u00E8 un certificato X.509"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "Il certificato esiste gi\u00E0 nel keystore con alias <{0}>"},
        {"Do.you.still.want.to.add.it.no.",
                "Aggiungerlo ugualmente? [no]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "Il certificato esiste gi\u00E0 nel keystore CA con alias <{0}>"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "Aggiungerlo al proprio keystore? [no]:  "},
        {"Trust.this.certificate.no.", "Considerare sicuro questo certificato? [no]:  "},
        {"YES", "S\u00EC"},
        {"New.prompt.", "Nuova {0}: "},
        {"Passwords.must.differ", "Le password non devono coincidere"},
        {"Re.enter.new.prompt.", "Reimmettere un nuovo valore per {0}: "},
        {"Re.enter.password.", "Reimmettere la password: "},
        {"Re.enter.new.password.", "Immettere nuovamente la nuova password: "},
        {"They.don.t.match.Try.again", "Non corrispondono. Riprovare."},
        {"Enter.prompt.alias.name.", "Immettere nome alias {0}:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "Immettere un nuovo nome alias\t(premere INVIO per annullare l'importazione della voce):  "},
        {"Enter.alias.name.", "Immettere nome alias:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(INVIO se corrisponde al nome di <{0}>)"},
        {"What.is.your.first.and.last.name.",
                "Specificare nome e cognome"},
        {"What.is.the.name.of.your.organizational.unit.",
                "Specificare il nome dell'unit\u00E0 organizzativa"},
        {"What.is.the.name.of.your.organization.",
                "Specificare il nome dell'organizzazione"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "Specificare la localit\u00E0"},
        {"What.is.the.name.of.your.State.or.Province.",
                "Specificare la provincia"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "Specificare il codice a due lettere del paese in cui si trova l'unit\u00E0"},
        {"Is.name.correct.", "Il dato {0} \u00E8 corretto?"},
        {"no", "no"},
        {"yes", "s\u00EC"},
        {"y", "s"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "All''alias <{0}> non \u00E8 associata alcuna chiave"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "L''alias <{0}> fa riferimento a un tipo di voce che non \u00E8 una voce di chiave privata. Il comando -keyclone supporta solo la copia delle voci di chiave private."},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "Firmatario #%d:"},
        {"Timestamp.", "Indicatore orario:"},
        {"Signature.", "Firma:"},
        {"CRLs.", "CRL:"},
        {"Certificate.owner.", "Proprietario certificato: "},
        {"Not.a.signed.jar.file", "Non \u00E8 un file jar firmato"},
        {"No.certificate.from.the.SSL.server",
                "Nessun certificato dal server SSL"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* L'integrit\u00E0 delle informazioni memorizzate nel keystore *\n* NON \u00E8 stata verificata. Per verificarne l'integrit\u00E0 *\n* \u00E8 necessario fornire la password del keystore.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* L'integrit\u00E0 delle informazioni memorizzate nel srckeystore *\n* NON \u00E8 stata verificata. Per verificarne l'integrit\u00E0 *\n* \u00E8 necessario fornire la password del srckeystore.                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "La risposta del certificato non contiene la chiave pubblica per <{0}>"},
        {"Incomplete.certificate.chain.in.reply",
                "Catena dei certificati incompleta nella risposta"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "La catena dei certificati nella risposta non verifica: "},
        {"Top.level.certificate.in.reply.",
                "Certificato di primo livello nella risposta:\n"},
        {".is.not.trusted.", "...non \u00E8 considerato sicuro. "},
        {"Install.reply.anyway.no.", "Installare la risposta? [no]:  "},
        {"NO", "NO"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "Le chiavi pubbliche nella risposta e nel keystore non corrispondono"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "La risposta del certificato e il certificato nel keystore sono identici"},
        {"Failed.to.establish.chain.from.reply",
                "Impossibile stabilire la catena dalla risposta"},
        {"n", "n"},
        {"Wrong.answer.try.again", "Risposta errata, riprovare"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "La chiave segreta non \u00E8 stata generata; l''alias <{0}> esiste gi\u00E0"},
        {"Please.provide.keysize.for.secret.key.generation",
                "Specificare il valore -keysize per la generazione della chiave segreta"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "AVVERTENZA: non verificato. Assicurarsi che -keystore sia corretto."},

        {"Extensions.", "Estensioni: "},
        {".Empty.value.", "(valore vuoto)"},
        {"Extension.Request.", "Richiesta di estensione:"},
        {"Unknown.keyUsage.type.", "Tipo keyUsage sconosciuto: "},
        {"Unknown.extendedkeyUsage.type.", "Tipo extendedkeyUsage sconosciuto: "},
        {"Unknown.AccessDescription.type.", "Tipo AccessDescription sconosciuto: "},
        {"Unrecognized.GeneralName.type.", "Tipo GeneralName non riconosciuto: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "Impossibile contrassegnare questa estensione come critica. "},
        {"Odd.number.of.hex.digits.found.", "\u00C8 stato trovato un numero dispari di cifre esadecimali: "},
        {"Unknown.extension.type.", "Tipo di estensione sconosciuto: "},
        {"command.{0}.is.ambiguous.", "il comando {0} \u00E8 ambiguo:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "La richiesta di certificato"},
        {"the.issuer", "L'emittente"},
        {"the.generated.certificate", "Il certificato generato"},
        {"the.generated.crl", "La CRL generata"},
        {"the.generated.certificate.request", "La richiesta di certificato generata"},
        {"the.certificate", "Il certificato"},
        {"the.crl", "La CRL"},
        {"the.tsa.certificate", "Il certificato TSA"},
        {"the.input", "L'input"},
        {"reply", "Rispondi"},
        {"one.in.many", "%1$s #%2$d di %3$d"},
        {"alias.in.cacerts", "Emittente <%s> in cacerts"},
        {"alias.in.keystore", "Emittente <%s>"},
        {"with.weak", "%s (debole)"},
        {"key.bit", "Chiave %2$s a %1$d bit"},
        {"key.bit.weak", "Chiave %2$s a %1$d bit (debole)"},
        {"unknown.size.1", "chiave %s di dimensione sconosciuta"},
        {".PATTERN.printX509Cert.with.weak",
                "Proprietario: {0}\nEmittente: {1}\nNumero di serie: {2}\nValido da: {3} a: {4}\nImpronte digitali certificato:\n\t SHA1: {5}\n\t SHA256: {6}\nNome algoritmo firma: {7}\nAlgoritmo di chiave pubblica oggetto: {8}\nVersione: {9}"},
        {"PKCS.10.with.weak",
                "Richiesta di certificato PKCS #10 (versione 1.0)\nOggetto: %1$s\nFormato: %2$s\nChiave pubblica: %3$s\nAlgoritmo firma: %4$s\n"},
        {"verified.by.s.in.s.weak", "Verificato da %1$s in %2$s con un %3$s"},
        {"whose.sigalg.risk", "%1$s utilizza l'algoritmo firma %2$s che \u00E8 considerato un rischio per la sicurezza."},
        {"whose.key.risk", "%1$s utilizza un %2$s che \u00E8 considerato un rischio per la sicurezza."},
        {"jks.storetype.warning", "Il keystore %1$s utilizza un formato proprietario. Si consiglia di eseguire la migrazione a PKCS12, un formato standard di settore, utilizzando il comando \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"."},
        {"migrate.keystore.warning", "Migrazione di \"%1$s\" in %4$s eseguita. Backup del keystore %2$s eseguito con il nome \"%3$s\"."},
        {"backup.keystore.warning", "Backup del keystore originale \"%1$s\" eseguito con il nome \"%3$s\"..."},
        {"importing.keystore.status", "Importazione del keystore %1$s in %2$s in corso..."},
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
