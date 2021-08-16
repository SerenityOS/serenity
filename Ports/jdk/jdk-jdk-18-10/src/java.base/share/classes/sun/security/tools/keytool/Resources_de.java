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
public class Resources_de extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "Optionen:"},
        {"option.1.set.twice", "Die Option %s wurde mehrmals angegeben. Alle Angaben bis auf die letzte werden ignoriert."},
        {"multiple.commands.1.2", "Nur ein Befehl ist zul\u00E4ssig: Sowohl %1$s als auch %2$s wurden angegeben."},
        {"Use.keytool.help.for.all.available.commands",
                 "\"keytool -help\" f\u00FCr alle verf\u00FCgbaren Befehle verwenden"},
        {"Key.and.Certificate.Management.Tool",
                 "Schl\u00FCssel- und Zertifikatsverwaltungstool"},
        {"Commands.", "Befehle:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "Verwenden Sie \"keytool -command_name -help\" f\u00FCr die Verwendung von command_name.\nVerwenden Sie die Option -conf <url>, um eine vorkonfigurierte Optionsdatei anzugeben."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "Generiert eine Zertifikatanforderung"}, //-certreq
        {"Changes.an.entry.s.alias",
                "\u00C4ndert den Alias eines Eintrags"}, //-changealias
        {"Deletes.an.entry",
                "L\u00F6scht einen Eintrag"}, //-delete
        {"Exports.certificate",
                "Exportiert ein Zertifikat"}, //-exportcert
        {"Generates.a.key.pair",
                "Generiert ein Schl\u00FCsselpaar"}, //-genkeypair
        {"Generates.a.secret.key",
                "Generiert einen Secret Key"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "Generiert ein Zertifikat aus einer Zertifikatanforderung"}, //-gencert
        {"Generates.CRL", "Generiert eine CRL"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "{0} Secret Key generiert"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "{0}-Bit {1} Secret Key generiert"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "Importiert Eintr\u00E4ge aus einer Identity-Datenbank im JDK 1.1.x-Stil"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "Importiert ein Zertifikat oder eine Zertifikatskette"}, //-importcert
        {"Imports.a.password",
                "Importiert ein Kennwort"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "Importiert einen oder alle Eintr\u00E4ge aus einem anderen Keystore"}, //-importkeystore
        {"Clones.a.key.entry",
                "Clont einen Schl\u00FCsseleintrag"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "\u00C4ndert das Schl\u00FCsselkennwort eines Eintrags"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "Listet die Eintr\u00E4ge in einem Keystore auf"}, //-list
        {"Prints.the.content.of.a.certificate",
                "Druckt den Content eines Zertifikats"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "Druckt den Content einer Zertifikatanforderung"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "Druckt den Content einer CRL-Datei"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "Generiert ein selbst signiertes Zertifikat"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "\u00C4ndert das Speicherkennwort eines Keystores"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "Aliasname des zu verarbeitenden Eintrags"}, //-alias
        {"destination.alias",
                "Zielalias"}, //-destalias
        {"destination.key.password",
                "Zielschl\u00FCssel-Kennwort"}, //-destkeypass
        {"destination.keystore.name",
                "Ziel-Keystore-Name"}, //-destkeystore
        {"destination.keystore.password.protected",
                "Ziel-Keystore kennwortgesch\u00FCtzt"}, //-destprotected
        {"destination.keystore.provider.name",
                "Ziel-Keystore-Providername"}, //-destprovidername
        {"destination.keystore.password",
                "Ziel-Keystore-Kennwort"}, //-deststorepass
        {"destination.keystore.type",
                "Ziel-Keystore-Typ"}, //-deststoretype
        {"distinguished.name",
                "Distinguished Name"}, //-dname
        {"X.509.extension",
                "X.509-Erweiterung"}, //-ext
        {"output.file.name",
                "Ausgabedateiname"}, //-file and -outfile
        {"input.file.name",
                "Eingabedateiname"}, //-file and -infile
        {"key.algorithm.name",
                "Schl\u00FCsselalgorithmusname"}, //-keyalg
        {"key.password",
                "Schl\u00FCsselkennwort"}, //-keypass
        {"key.bit.size",
                "Schl\u00FCsselbitgr\u00F6\u00DFe"}, //-keysize
        {"keystore.name",
                "Keystore-Name"}, //-keystore
        {"access.the.cacerts.keystore",
                "Zugriff auf den cacerts Keystore"}, // -cacerts
        {"warning.cacerts.option",
                "Warnung: Verwenden Sie die Option -cacerts f\u00FCr den Zugriff auf den cacerts Keystore"},
        {"new.password",
                "Neues Kennwort"}, //-new
        {"do.not.prompt",
                "Kein Prompt"}, //-noprompt
        {"password.through.protected.mechanism",
                "Kennwort \u00FCber gesch\u00FCtzten Mechanismus"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "Sicherheitsprovider nach Name hinzuf\u00FCgen (z.B. SunPKCS11)\nArgument f\u00FCr -addprovider konfigurieren"}, //-addprovider
        {"provider.class.option",
                "Sicherheitsprovider nach vollst\u00E4ndig angegebenem Klassennamen hinzuf\u00FCgen\nArgument f\u00FCr -providerclass konfigurieren"}, //-providerclass

        {"provider.name",
                "Providername"}, //-providername
        {"provider.classpath",
                "Provider-Classpath"}, //-providerpath
        {"output.in.RFC.style",
                "Ausgabe in RFC-Stil"}, //-rfc
        {"signature.algorithm.name",
                "Signaturalgorithmusname"}, //-sigalg
        {"source.alias",
                "Quellalias"}, //-srcalias
        {"source.key.password",
                "Quellschl\u00FCssel-Kennwort"}, //-srckeypass
        {"source.keystore.name",
                "Quell-Keystore-Name"}, //-srckeystore
        {"source.keystore.password.protected",
                "Quell-Keystore kennwortgesch\u00FCtzt"}, //-srcprotected
        {"source.keystore.provider.name",
                "Quell-Keystore-Providername"}, //-srcprovidername
        {"source.keystore.password",
                "Quell-Keystore-Kennwort"}, //-srcstorepass
        {"source.keystore.type",
                "Quell-Keystore-Typ"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "SSL-Serverhost und -port"}, //-sslserver
        {"signed.jar.file",
                "Signierte JAR-Datei"}, //=jarfile
        {"certificate.validity.start.date.time",
                "Anfangsdatum/-zeit f\u00FCr Zertifikatsg\u00FCltigkeit"}, //-startdate
        {"keystore.password",
                "Keystore-Kennwort"}, //-storepass
        {"keystore.type",
                "Keystore-Typ"}, //-storetype
        {"trust.certificates.from.cacerts",
                "Zertifikaten aus cacerts vertrauen"}, //-trustcacerts
        {"verbose.output",
                "Verbose-Ausgabe"}, //-v
        {"validity.number.of.days",
                "G\u00FCltigkeitsdauer (Tage)"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "Serielle ID des zu entziehenden Certs"}, //-id
        // keytool: Running part
        {"keytool.error.", "Keytool-Fehler: "},
        {"Illegal.option.", "Ung\u00FCltige Option:  "},
        {"Illegal.value.", "Ung\u00FCltiger Wert: "},
        {"Unknown.password.type.", "Unbekannter Kennworttyp: "},
        {"Cannot.find.environment.variable.",
                "Umgebungsvariable kann nicht gefunden werden: "},
        {"Cannot.find.file.", "Datei kann nicht gefunden werden: "},
        {"Command.option.flag.needs.an.argument.", "Befehlsoption {0} ben\u00F6tigt ein Argument."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "Warnung: Keine Unterst\u00FCtzung f\u00FCr unterschiedliche Speicher- und Schl\u00FCsselkennw\u00F6rter bei PKCS12 KeyStores. Der benutzerdefinierte Wert {0} wird ignoriert."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "Die Option -keystore oder -storetype kann nicht mit der Option -cacerts verwendet werden"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-keystore muss NONE sein, wenn -storetype {0} ist"},
        {"Too.many.retries.program.terminated",
                 "Zu viele erneute Versuche. Programm wird beendet"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "Befehle -storepasswd und -keypasswd werden nicht unterst\u00FCtzt, wenn -storetype {0} ist"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "Befehle des Typs -keypasswd werden nicht unterst\u00FCtzt, wenn -storetype PKCS12 ist"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "-keypass und -new k\u00F6nnen nicht angegeben werden, wenn -storetype {0} ist"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "Wenn -protected angegeben ist, d\u00FCrfen -storepass, -keypass und -new nicht angegeben werden"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Wenn -srcprotected angegeben ist, d\u00FCrfen -srcstorepass und -srckeypass nicht angegeben werden"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "Wenn der Keystore nicht kennwortgesch\u00FCtzt ist, d\u00FCrfen -storepass, -keypass und -new nicht angegeben werden"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "Wenn der Quell-Keystore nicht kennwortgesch\u00FCtzt ist, d\u00FCrfen -srcstorepass und -srckeypass nicht angegeben werden"},
        {"Illegal.startdate.value", "Ung\u00FCltiger Wert f\u00FCr Anfangsdatum"},
        {"Validity.must.be.greater.than.zero",
                "G\u00FCltigkeit muss gr\u00F6\u00DFer als null sein"},
        {"provclass.not.a.provider", "%s kein Provider"},
        {"provider.name.not.found", "Provider namens \"%s\" nicht gefunden"},
        {"provider.class.not.found", "Provider \"%s\" nicht gefunden"},
        {"Usage.error.no.command.provided", "Verwendungsfehler: Kein Befehl angegeben"},
        {"Source.keystore.file.exists.but.is.empty.", "Quell-Keystore-Datei ist zwar vorhanden, ist aber leer: "},
        {"Please.specify.srckeystore", "Geben Sie -srckeystore an"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "-v und -rfc d\u00FCrfen bei Befehl \"list\" nicht beide angegeben werden"},
        {"Key.password.must.be.at.least.6.characters",
                "Schl\u00FCsselkennwort muss mindestens sechs Zeichen lang sein"},
        {"New.password.must.be.at.least.6.characters",
                "Neues Kennwort muss mindestens sechs Zeichen lang sein"},
        {"Keystore.file.exists.but.is.empty.",
                "Keystore-Datei ist vorhanden, ist aber leer: "},
        {"Keystore.file.does.not.exist.",
                "Keystore-Datei ist nicht vorhanden: "},
        {"Must.specify.destination.alias", "Sie m\u00FCssen einen Zielalias angeben"},
        {"Must.specify.alias", "Sie m\u00FCssen einen Alias angeben"},
        {"Keystore.password.must.be.at.least.6.characters",
                "Keystore-Kennwort muss mindestens sechs Zeichen lang sein"},
        {"Enter.the.password.to.be.stored.",
                "Geben Sie das Kennwort ein, das gespeichert werden soll:  "},
        {"Enter.keystore.password.", "Keystore-Kennwort eingeben:  "},
        {"Enter.source.keystore.password.", "Quell-Keystore-Kennwort eingeben:  "},
        {"Enter.destination.keystore.password.", "Ziel-Keystore-Kennwort eingeben:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "Keystore-Kennwort ist zu kurz. Es muss mindestens sechs Zeichen lang sein"},
        {"Unknown.Entry.Type", "Unbekannter Eintragstyp"},
        {"Too.many.failures.Alias.not.changed", "Zu viele Fehler. Alias nicht ge\u00E4ndert"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "Eintrag f\u00FCr Alias {0} erfolgreich importiert."},
        {"Entry.for.alias.alias.not.imported.", "Eintrag f\u00FCr Alias {0} nicht importiert."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "Problem beim Importieren des Eintrags f\u00FCr Alias {0}: {1}.\nEintrag f\u00FCr Alias {0} nicht importiert."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "Importbefehl abgeschlossen: {0} Eintr\u00E4ge erfolgreich importiert, {1} Eintr\u00E4ge nicht erfolgreich oder abgebrochen"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "Warnung: Vorhandener Alias {0} in Ziel-Keystore wird \u00FCberschrieben"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "Eintragsalias {0} ist bereits vorhanden. \u00DCberschreiben? [Nein]:  "},
        {"Too.many.failures.try.later", "Zu viele Fehler. Versuchen Sie es sp\u00E4ter erneut"},
        {"Certification.request.stored.in.file.filename.",
                "Zertifizierungsanforderung in Datei <{0}> gespeichert"},
        {"Submit.this.to.your.CA", "Leiten Sie dies an die CA weiter"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "Wenn kein Alias angegeben ist, d\u00FCrfen destalias und srckeypass nicht angegeben werden"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "Der Ziel-Keystore pkcs12 hat unterschiedliche Kennw\u00F6rter f\u00FCr storepass und keypass. Wiederholen Sie den Vorgang, indem Sie -destkeypass angeben."},
        {"Certificate.stored.in.file.filename.",
                "Zertifikat in Datei <{0}> gespeichert"},
        {"Certificate.reply.was.installed.in.keystore",
                "Zertifikatantwort wurde in Keystore installiert"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "Zertifikatantwort wurde nicht in Keystore installiert"},
        {"Certificate.was.added.to.keystore",
                "Zertifikat wurde Keystore hinzugef\u00FCgt"},
        {"Certificate.was.not.added.to.keystore",
                "Zertifikat wurde nicht zu Keystore hinzugef\u00FCgt"},
        {".Storing.ksfname.", "[{0} wird gesichert]"},
        {"alias.has.no.public.key.certificate.",
                "{0} hat keinen Public Key (Zertifikat)"},
        {"Cannot.derive.signature.algorithm",
                "Signaturalgorithmus kann nicht abgeleitet werden"},
        {"Alias.alias.does.not.exist",
                "Alias <{0}> ist nicht vorhanden"},
        {"Alias.alias.has.no.certificate",
                "Alias <{0}> hat kein Zertifikat"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "Schl\u00FCsselpaar wurde nicht generiert. Alias <{0}> ist bereits vorhanden"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "Generieren von Schl\u00FCsselpaar (Typ {1}, {0} Bit) und selbst signiertem Zertifikat ({2}) mit einer G\u00FCltigkeit von {3} Tagen\n\tf\u00FCr: {4}"},
        {"Enter.key.password.for.alias.", "Schl\u00FCsselkennwort f\u00FCr <{0}> eingeben"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(RETURN, wenn identisch mit Keystore-Kennwort):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "Schl\u00FCsselkennwort ist zu kurz. Es muss mindestens sechs Zeichen lang sein"},
        {"Too.many.failures.key.not.added.to.keystore",
                "Zu viele Fehler. Schl\u00FCssel wurde nicht zu Keystore hinzugef\u00FCgt"},
        {"Destination.alias.dest.already.exists",
                "Zielalias <{0}> bereits vorhanden"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "Kennwort ist zu kurz. Es muss mindestens sechs Zeichen lang sein"},
        {"Too.many.failures.Key.entry.not.cloned",
                "Zu viele Fehler. Schl\u00FCsseleintrag wurde nicht geclont"},
        {"key.password.for.alias.", "Schl\u00FCsselkennwort f\u00FCr <{0}>"},
        {"Keystore.entry.for.id.getName.already.exists",
                "Keystore-Eintrag f\u00FCr <{0}> bereits vorhanden"},
        {"Creating.keystore.entry.for.id.getName.",
                "Keystore-Eintrag f\u00FCr <{0}> wird erstellt..."},
        {"No.entries.from.identity.database.added",
                "Keine Eintr\u00E4ge aus Identity-Datenbank hinzugef\u00FCgt"},
        {"Alias.name.alias", "Aliasname: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "Erstellungsdatum: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "Eintragstyp: {0}"},
        {"Certificate.chain.length.", "Zertifikatskettenl\u00E4nge: "},
        {"Certificate.i.1.", "Zertifikat[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "Zertifikat-Fingerprint (SHA-256): "},
        {"Keystore.type.", "Keystore-Typ: "},
        {"Keystore.provider.", "Keystore-Provider: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "Keystore enth\u00E4lt {0,number,integer} Eintrag"},
        {"Your.keystore.contains.keyStore.size.entries",
                "Keystore enth\u00E4lt {0,number,integer} Eintr\u00E4ge"},
        {"Failed.to.parse.input", "Eingabe konnte nicht geparst werden"},
        {"Empty.input", "Leere Eingabe"},
        {"Not.X.509.certificate", "Kein X.509-Zertifikat"},
        {"alias.has.no.public.key", "{0} hat keinen Public Key"},
        {"alias.has.no.X.509.certificate", "{0} hat kein X.509-Zertifikat"},
        {"New.certificate.self.signed.", "Neues Zertifikat (selbst signiert):"},
        {"Reply.has.no.certificates", "Antwort hat keine Zertifikate"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "Zertifikat nicht importiert. Alias <{0}> ist bereits vorhanden"},
        {"Input.not.an.X.509.certificate", "Eingabe kein X.509-Zertifikat"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "Zertifikat ist bereits unter Alias <{0}> im Keystore vorhanden"},
        {"Do.you.still.want.to.add.it.no.",
                "M\u00F6chten Sie es trotzdem hinzuf\u00FCgen? [Nein]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "Zertifikat ist bereits unter Alias <{0}> im systemweiten CA-Keystore vorhanden"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "M\u00F6chten Sie es trotzdem zu Ihrem eigenen Keystore hinzuf\u00FCgen? [Nein]:  "},
        {"Trust.this.certificate.no.", "Diesem Zertifikat vertrauen? [Nein]:  "},
        {"YES", "JA"},
        {"New.prompt.", "Neues {0}: "},
        {"Passwords.must.differ", "Kennw\u00F6rter m\u00FCssen sich unterscheiden"},
        {"Re.enter.new.prompt.", "Neues {0} erneut eingeben: "},
        {"Re.enter.password.", "Geben Sie das Kennwort erneut ein: "},
        {"Re.enter.new.password.", "Neues Kennwort erneut eingeben: "},
        {"They.don.t.match.Try.again", "Keine \u00DCbereinstimmung. Wiederholen Sie den Vorgang"},
        {"Enter.prompt.alias.name.", "{0}-Aliasnamen eingeben:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "Geben Sie einen neuen Aliasnamen ein\t(RETURN, um den Import dieses Eintrags abzubrechen):  "},
        {"Enter.alias.name.", "Aliasnamen eingeben:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(RETURN, wenn identisch mit <{0}>)"},
        {"What.is.your.first.and.last.name.",
                "Wie lautet Ihr Vor- und Nachname?"},
        {"What.is.the.name.of.your.organizational.unit.",
                "Wie lautet der Name Ihrer organisatorischen Einheit?"},
        {"What.is.the.name.of.your.organization.",
                "Wie lautet der Name Ihrer Organisation?"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "Wie lautet der Name Ihrer Stadt oder Gemeinde?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "Wie lautet der Name Ihres Bundeslands?"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "Wie lautet der L\u00E4ndercode (zwei Buchstaben) f\u00FCr diese Einheit?"},
        {"Is.name.correct.", "Ist {0} richtig?"},
        {"no", "Nein"},
        {"yes", "Ja"},
        {"y", "J"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "Alias <{0}> verf\u00FCgt \u00FCber keinen Schl\u00FCssel"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "Alias <{0}> verweist auf einen Eintragstyp, der kein Private Key-Eintrag ist. Der Befehl -keyclone unterst\u00FCtzt nur das Klonen von Private Key-Eintr\u00E4gen"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "Signaturgeber #%d:"},
        {"Timestamp.", "Zeitstempel:"},
        {"Signature.", "Signatur:"},
        {"CRLs.", "CRLs:"},
        {"Certificate.owner.", "Zertifikateigent\u00FCmer: "},
        {"Not.a.signed.jar.file", "Keine signierte JAR-Datei"},
        {"No.certificate.from.the.SSL.server",
                "Kein Zertifikat vom SSL-Server"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* Die Integrit\u00E4t der Informationen, die in Ihrem Keystore gespeichert sind, *\n* wurde NICHT gepr\u00FCft. Um die Integrit\u00E4t zu pr\u00FCfen, *\n* m\u00FCssen Sie Ihr Keystore-Kennwort angeben.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* Die Integrit\u00E4t der Informationen, die in Ihrem Srckeystore gespeichert sind, *\n* wurde NICHT gepr\u00FCft. Um die Integrit\u00E4t zu pr\u00FCfen, *\n* m\u00FCssen Sie Ihr Srckeystore-Kennwort angeben.                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "Zertifikatantwort enth\u00E4lt keinen Public Key f\u00FCr <{0}>"},
        {"Incomplete.certificate.chain.in.reply",
                "Unvollst\u00E4ndige Zertifikatskette in Antwort"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "Zertifikatskette in Antwort verifiziert nicht: "},
        {"Top.level.certificate.in.reply.",
                "Zertifikat der obersten Ebene in Antwort:\n"},
        {".is.not.trusted.", "... ist nicht vertrauensw\u00FCrdig. "},
        {"Install.reply.anyway.no.", "Antwort trotzdem installieren? [Nein]:  "},
        {"NO", "NEIN"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "Public Keys in Antwort und Keystore stimmen nicht \u00FCberein"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "Zertifikatantwort und Zertifikat in Keystore sind identisch"},
        {"Failed.to.establish.chain.from.reply",
                "Kette konnte der Antwort nicht entnommen werden"},
        {"n", "N"},
        {"Wrong.answer.try.again", "Falsche Antwort. Wiederholen Sie den Vorgang"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "Secret Key wurde nicht generiert. Alias <{0}> ist bereits vorhanden"},
        {"Please.provide.keysize.for.secret.key.generation",
                "Geben Sie -keysize zum Erstellen eines Secret Keys an"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "WARNUNG: Nicht gepr\u00FCft. Stellen Sie sicher, dass -keystore korrekt ist."},

        {"Extensions.", "Erweiterungen: "},
        {".Empty.value.", "(Leerer Wert)"},
        {"Extension.Request.", "Erweiterungsanforderung:"},
        {"Unknown.keyUsage.type.", "Unbekannter keyUsage-Typ: "},
        {"Unknown.extendedkeyUsage.type.", "Unbekannter extendedkeyUsage-Typ: "},
        {"Unknown.AccessDescription.type.", "Unbekannter AccessDescription-Typ: "},
        {"Unrecognized.GeneralName.type.", "Unbekannter GeneralName-Typ: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "Erweiterung kann nicht als \"Kritisch\" markiert werden. "},
        {"Odd.number.of.hex.digits.found.", "Ungerade Anzahl hexadezimaler Ziffern gefunden: "},
        {"Unknown.extension.type.", "Unbekannter Erweiterungstyp: "},
        {"command.{0}.is.ambiguous.", "Befehl {0} ist mehrdeutig:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "Die Zertifikatanforderung"},
        {"the.issuer", "Der Aussteller"},
        {"the.generated.certificate", "Das generierte Zertifikat"},
        {"the.generated.crl", "Die generierte CRL"},
        {"the.generated.certificate.request", "Die generierte Zertifikatanforderung"},
        {"the.certificate", "Das Zertifikat"},
        {"the.crl", "Die CRL"},
        {"the.tsa.certificate", "Das TSA-Zertifikat"},
        {"the.input", "Die Eingabe"},
        {"reply", "Antwort"},
        {"one.in.many", "%1$s #%2$d von %3$d"},
        {"alias.in.cacerts", "Aussteller <%s> in cacerts"},
        {"alias.in.keystore", "Aussteller <%s>"},
        {"with.weak", "%s (schwach)"},
        {"key.bit", "%1$d-Bit-%2$s-Schl\u00FCssel"},
        {"key.bit.weak", "%1$d-Bit-%2$s-Schl\u00FCssel (schwach)"},
        {"unknown.size.1", "unbekannte Gr\u00F6\u00DFe: %s-Schl\u00FCssel"},
        {".PATTERN.printX509Cert.with.weak",
                "Eigent\u00FCmer: {0}\nAussteller: {1}\nSeriennummer: {2}\nG\u00FCltig von: {3} bis: {4}\nZertifikatsfingerprints:\n\t SHA1: {5}\n\t SHA256: {6}\nSignaturalgorithmusname: {7}\nPublic Key-Algorithmus von Subject: {8}\nVersion: {9}"},
        {"PKCS.10.with.weak",
                "PKCS #10-Zertifikatanforderung (Version 1.0)\nSubject: %1$s\nFormat: %2$s\nPublic Key: %3$s\nSignaturalgorithmus: %4$s\n"},
        {"verified.by.s.in.s.weak", "Von %1$s in %2$s mit %3$s verifiziert"},
        {"whose.sigalg.risk", "%1$s verwendet den Signaturalgorithmus %2$s. Dies gilt als Sicherheitsrisiko."},
        {"whose.key.risk", "%1$s verwendet %2$s. Dies gilt als Sicherheitsrisiko."},
        {"jks.storetype.warning", "Der %1$s-Keystore verwendet ein propriet\u00E4res Format. Es wird empfohlen, auf PKCS12 zu migrieren, das ein Industriestandardformat mit \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\" ist."},
        {"migrate.keystore.warning", "\"%1$s\" zu %4$s migriert. Der %2$s-Keystore wurde als \"%3$s\" gesichert."},
        {"backup.keystore.warning", "Der urspr\u00FCngliche Keystore \"%1$s\" wird als \"%3$s\" gesichert..."},
        {"importing.keystore.status", "Keystore %1$s wird in %2$s importiert..."},
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
