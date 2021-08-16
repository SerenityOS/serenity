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
public class Resources_ko extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "\uC635\uC158:"},
        {"option.1.set.twice", "%s \uC635\uC158\uC774 \uC5EC\uB7EC \uBC88 \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB9C8\uC9C0\uB9C9 \uD56D\uBAA9\uC744 \uC81C\uC678\uD55C \uBAA8\uB4E0 \uD56D\uBAA9\uC774 \uBB34\uC2DC\uB429\uB2C8\uB2E4."},
        {"multiple.commands.1.2", "\uBA85\uB839\uC740 \uD558\uB098\uB9CC \uD5C8\uC6A9\uB429\uB2C8\uB2E4. %1$s \uBC0F %2$s\uC774(\uAC00) \uBAA8\uB450 \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Use.keytool.help.for.all.available.commands",
                 "\uC0AC\uC6A9 \uAC00\uB2A5\uD55C \uBAA8\uB4E0 \uBA85\uB839\uC5D0 \"keytool -help\" \uC0AC\uC6A9"},
        {"Key.and.Certificate.Management.Tool",
                 "\uD0A4 \uBC0F \uC778\uC99D\uC11C \uAD00\uB9AC \uD234"},
        {"Commands.", "\uBA85\uB839:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "command_name \uC0AC\uC6A9\uBC95\uC5D0 \"keytool -command_name -help\"\uB97C \uC0AC\uC6A9\uD569\uB2C8\uB2E4.\n-conf <url> \uC635\uC158\uC744 \uC0AC\uC6A9\uD558\uC5EC \uC0AC\uC804 \uAD6C\uC131\uB41C \uC635\uC158 \uD30C\uC77C\uC744 \uC9C0\uC815\uD569\uB2C8\uB2E4."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "\uC778\uC99D\uC11C \uC694\uCCAD\uC744 \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-certreq
        {"Changes.an.entry.s.alias",
                "\uD56D\uBAA9\uC758 \uBCC4\uCE6D\uC744 \uBCC0\uACBD\uD569\uB2C8\uB2E4."}, //-changealias
        {"Deletes.an.entry",
                "\uD56D\uBAA9\uC744 \uC0AD\uC81C\uD569\uB2C8\uB2E4."}, //-delete
        {"Exports.certificate",
                "\uC778\uC99D\uC11C\uB97C \uC775\uC2A4\uD3EC\uD2B8\uD569\uB2C8\uB2E4."}, //-exportcert
        {"Generates.a.key.pair",
                "\uD0A4 \uC30D\uC744 \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-genkeypair
        {"Generates.a.secret.key",
                "\uBCF4\uC548 \uD0A4\uB97C \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "\uC778\uC99D\uC11C \uC694\uCCAD\uC5D0\uC11C \uC778\uC99D\uC11C\uB97C \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-gencert
        {"Generates.CRL", "CRL\uC744 \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "{0} \uBCF4\uC548 \uD0A4\uB97C \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "{0}\uBE44\uD2B8 {1} \uBCF4\uC548 \uD0A4\uB97C \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "JDK 1.1.x \uC2A4\uD0C0\uC77C ID \uB370\uC774\uD130\uBCA0\uC774\uC2A4\uC5D0\uC11C \uD56D\uBAA9\uC744 \uC784\uD3EC\uD2B8\uD569\uB2C8\uB2E4."}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "\uC778\uC99D\uC11C \uB610\uB294 \uC778\uC99D\uC11C \uCCB4\uC778\uC744 \uC784\uD3EC\uD2B8\uD569\uB2C8\uB2E4."}, //-importcert
        {"Imports.a.password",
                "\uBE44\uBC00\uBC88\uD638\uB97C \uC784\uD3EC\uD2B8\uD569\uB2C8\uB2E4."}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "\uB2E4\uB978 \uD0A4 \uC800\uC7A5\uC18C\uC5D0\uC11C \uD558\uB098 \uB610\uB294 \uBAA8\uB4E0 \uD56D\uBAA9\uC744 \uC784\uD3EC\uD2B8\uD569\uB2C8\uB2E4."}, //-importkeystore
        {"Clones.a.key.entry",
                "\uD0A4 \uD56D\uBAA9\uC744 \uBCF5\uC81C\uD569\uB2C8\uB2E4."}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "\uD56D\uBAA9\uC758 \uD0A4 \uBE44\uBC00\uBC88\uD638\uB97C \uBCC0\uACBD\uD569\uB2C8\uB2E4."}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "\uD0A4 \uC800\uC7A5\uC18C\uC758 \uD56D\uBAA9\uC744 \uB098\uC5F4\uD569\uB2C8\uB2E4."}, //-list
        {"Prints.the.content.of.a.certificate",
                "\uC778\uC99D\uC11C\uC758 \uCF58\uD150\uCE20\uB97C \uC778\uC1C4\uD569\uB2C8\uB2E4."}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "\uC778\uC99D\uC11C \uC694\uCCAD\uC758 \uCF58\uD150\uCE20\uB97C \uC778\uC1C4\uD569\uB2C8\uB2E4."}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "CRL \uD30C\uC77C\uC758 \uCF58\uD150\uCE20\uB97C \uC778\uC1C4\uD569\uB2C8\uB2E4."}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "\uC790\uCCB4 \uC11C\uBA85\uB41C \uC778\uC99D\uC11C\uB97C \uC0DD\uC131\uD569\uB2C8\uB2E4."}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "\uD0A4 \uC800\uC7A5\uC18C\uC758 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uB97C \uBCC0\uACBD\uD569\uB2C8\uB2E4."}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "\uCC98\uB9AC\uD560 \uD56D\uBAA9\uC758 \uBCC4\uCE6D \uC774\uB984"}, //-alias
        {"destination.alias",
                "\uB300\uC0C1 \uBCC4\uCE6D"}, //-destalias
        {"destination.key.password",
                "\uB300\uC0C1 \uD0A4 \uBE44\uBC00\uBC88\uD638"}, //-destkeypass
        {"destination.keystore.name",
                "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uC774\uB984"}, //-destkeystore
        {"destination.keystore.password.protected",
                "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uB85C \uBCF4\uD638\uB428"}, //-destprotected
        {"destination.keystore.provider.name",
                "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uC81C\uACF5\uC790 \uC774\uB984"}, //-destprovidername
        {"destination.keystore.password",
                "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638"}, //-deststorepass
        {"destination.keystore.type",
                "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uC720\uD615"}, //-deststoretype
        {"distinguished.name",
                "\uC2DD\uBCC4 \uC774\uB984"}, //-dname
        {"X.509.extension",
                "X.509 \uD655\uC7A5"}, //-ext
        {"output.file.name",
                "\uCD9C\uB825 \uD30C\uC77C \uC774\uB984"}, //-file and -outfile
        {"input.file.name",
                "\uC785\uB825 \uD30C\uC77C \uC774\uB984"}, //-file and -infile
        {"key.algorithm.name",
                "\uD0A4 \uC54C\uACE0\uB9AC\uC998 \uC774\uB984"}, //-keyalg
        {"key.password",
                "\uD0A4 \uBE44\uBC00\uBC88\uD638"}, //-keypass
        {"key.bit.size",
                "\uD0A4 \uBE44\uD2B8 \uD06C\uAE30"}, //-keysize
        {"keystore.name",
                "\uD0A4 \uC800\uC7A5\uC18C \uC774\uB984"}, //-keystore
        {"access.the.cacerts.keystore",
                "cacerts \uD0A4 \uC800\uC7A5\uC18C \uC561\uC138\uC2A4"}, // -cacerts
        {"warning.cacerts.option",
                "\uACBD\uACE0: -cacerts \uC635\uC158\uC744 \uC0AC\uC6A9\uD558\uC5EC cacerts \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC561\uC138\uC2A4\uD558\uC2ED\uC2DC\uC624."},
        {"new.password",
                "\uC0C8 \uBE44\uBC00\uBC88\uD638"}, //-new
        {"do.not.prompt",
                "\uD655\uC778\uD558\uC9C0 \uC54A\uC74C"}, //-noprompt
        {"password.through.protected.mechanism",
                "\uBCF4\uD638\uB418\uB294 \uBA54\uCEE4\uB2C8\uC998\uC744 \uD1B5\uD55C \uBE44\uBC00\uBC88\uD638"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "\uC774\uB984\uBCC4 \uBCF4\uC548 \uC81C\uACF5\uC790 \uCD94\uAC00(\uC608: SunPKCS11)\n-addprovider\uC5D0 \uB300\uD55C \uC778\uC218 \uAD6C\uC131"}, //-addprovider
        {"provider.class.option",
                "\uC804\uCCB4 \uD074\uB798\uC2A4 \uC774\uB984\uBCC4 \uBCF4\uC548 \uC81C\uACF5\uC790 \uCD94\uAC00\n-providerclass\uC5D0 \uB300\uD55C \uC778\uC218 \uAD6C\uC131"}, //-providerclass

        {"provider.name",
                "\uC81C\uACF5\uC790 \uC774\uB984"}, //-providername
        {"provider.classpath",
                "\uC81C\uACF5\uC790 \uD074\uB798\uC2A4 \uACBD\uB85C"}, //-providerpath
        {"output.in.RFC.style",
                "RFC \uC2A4\uD0C0\uC77C\uC758 \uCD9C\uB825"}, //-rfc
        {"signature.algorithm.name",
                "\uC11C\uBA85 \uC54C\uACE0\uB9AC\uC998 \uC774\uB984"}, //-sigalg
        {"source.alias",
                "\uC18C\uC2A4 \uBCC4\uCE6D"}, //-srcalias
        {"source.key.password",
                "\uC18C\uC2A4 \uD0A4 \uBE44\uBC00\uBC88\uD638"}, //-srckeypass
        {"source.keystore.name",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uC774\uB984"}, //-srckeystore
        {"source.keystore.password.protected",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uB85C \uBCF4\uD638\uB428"}, //-srcprotected
        {"source.keystore.provider.name",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uC81C\uACF5\uC790 \uC774\uB984"}, //-srcprovidername
        {"source.keystore.password",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638"}, //-srcstorepass
        {"source.keystore.type",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uC720\uD615"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "SSL \uC11C\uBC84 \uD638\uC2A4\uD2B8 \uBC0F \uD3EC\uD2B8"}, //-sslserver
        {"signed.jar.file",
                "\uC11C\uBA85\uB41C jar \uD30C\uC77C"}, //=jarfile
        {"certificate.validity.start.date.time",
                "\uC778\uC99D\uC11C \uC720\uD6A8 \uAE30\uAC04 \uC2DC\uC791 \uB0A0\uC9DC/\uC2DC\uAC04"}, //-startdate
        {"keystore.password",
                "\uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638"}, //-storepass
        {"keystore.type",
                "\uD0A4 \uC800\uC7A5\uC18C \uC720\uD615"}, //-storetype
        {"trust.certificates.from.cacerts",
                "cacerts\uC758 \uBCF4\uC548 \uC778\uC99D\uC11C"}, //-trustcacerts
        {"verbose.output",
                "\uC0C1\uC138 \uC815\uBCF4 \uCD9C\uB825"}, //-v
        {"validity.number.of.days",
                "\uC720\uD6A8 \uAE30\uAC04 \uC77C \uC218"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "\uCCA0\uD68C\uD560 \uC778\uC99D\uC11C\uC758 \uC77C\uB828 ID"}, //-id
        // keytool: Running part
        {"keytool.error.", "keytool \uC624\uB958: "},
        {"Illegal.option.", "\uC798\uBABB\uB41C \uC635\uC158:  "},
        {"Illegal.value.", "\uC798\uBABB\uB41C \uAC12: "},
        {"Unknown.password.type.", "\uC54C \uC218 \uC5C6\uB294 \uBE44\uBC00\uBC88\uD638 \uC720\uD615: "},
        {"Cannot.find.environment.variable.",
                "\uD658\uACBD \uBCC0\uC218\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: "},
        {"Cannot.find.file.", "\uD30C\uC77C\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC74C: "},
        {"Command.option.flag.needs.an.argument.", "\uBA85\uB839 \uC635\uC158 {0}\uC5D0 \uC778\uC218\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "\uACBD\uACE0: \uB2E4\uB978 \uC800\uC7A5\uC18C \uBC0F \uD0A4 \uBE44\uBC00\uBC88\uD638\uB294 PKCS12 KeyStores\uC5D0 \uB300\uD574 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4. \uC0AC\uC6A9\uC790\uAC00 \uC9C0\uC815\uD55C {0} \uAC12\uC744 \uBB34\uC2DC\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "-keystore \uB610\uB294 -storetype \uC635\uC158\uC740 -cacerts \uC635\uC158\uACFC \uD568\uAED8 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-storetype\uC774 {0}\uC778 \uACBD\uC6B0 -keystore\uB294 NONE\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Too.many.retries.program.terminated",
                 "\uC7AC\uC2DC\uB3C4 \uD69F\uC218\uAC00 \uB108\uBB34 \uB9CE\uC544 \uD504\uB85C\uADF8\uB7A8\uC774 \uC885\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "-storetype\uC774 {0}\uC778 \uACBD\uC6B0 -storepasswd \uBC0F -keypasswd \uBA85\uB839\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "-storetype\uC774 PKCS12\uC778 \uACBD\uC6B0 -keypasswd \uBA85\uB839\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "-storetype\uC774 {0}\uC778 \uACBD\uC6B0 -keypass \uBC0F -new\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "-protected\uB97C \uC9C0\uC815\uD55C \uACBD\uC6B0 -storepass, -keypass \uBC0F -new\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "-srcprotected\uB97C \uC9C0\uC815\uD55C \uACBD\uC6B0 -srcstorepass \uBC0F -srckeypass\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "\uD0A4 \uC800\uC7A5\uC18C\uAC00 \uBE44\uBC00\uBC88\uD638\uB85C \uBCF4\uD638\uB418\uC9C0 \uC54A\uB294 \uACBD\uC6B0 -storepass, -keypass \uBC0F -new\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C\uAC00 \uBE44\uBC00\uBC88\uD638\uB85C \uBCF4\uD638\uB418\uC9C0 \uC54A\uB294 \uACBD\uC6B0 -srcstorepass \uBC0F -srckeypass\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"Illegal.startdate.value", "startdate \uAC12\uC774 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Validity.must.be.greater.than.zero",
                "\uC720\uD6A8 \uAE30\uAC04\uC740 0\uBCF4\uB2E4 \uCEE4\uC57C \uD569\uB2C8\uB2E4."},
        {"provclass.not.a.provider", "%s\uC740(\uB294) \uC81C\uACF5\uC790\uAC00 \uC544\uB2D9\uB2C8\uB2E4."},
        {"provider.name.not.found", "\"%s\" \uC774\uB984\uC758 \uC81C\uACF5\uC790\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"provider.class.not.found", "\"%s\" \uC81C\uACF5\uC790\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Usage.error.no.command.provided", "\uC0AC\uC6A9\uBC95 \uC624\uB958: \uBA85\uB839\uC744 \uC785\uB825\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Source.keystore.file.exists.but.is.empty.", "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uD30C\uC77C\uC774 \uC874\uC7AC\uD558\uC9C0\uB9CC \uBE44\uC5B4 \uC788\uC74C: "},
        {"Please.specify.srckeystore", "-srckeystore\uB97C \uC9C0\uC815\uD558\uC2ED\uC2DC\uC624."},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "'list' \uBA85\uB839\uC5D0 -v\uC640 -rfc\uB97C \uD568\uAED8 \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"Key.password.must.be.at.least.6.characters",
                "\uD0A4 \uBE44\uBC00\uBC88\uD638\uB294 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"New.password.must.be.at.least.6.characters",
                "\uC0C8 \uBE44\uBC00\uBC88\uD638\uB294 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Keystore.file.exists.but.is.empty.",
                "\uD0A4 \uC800\uC7A5\uC18C \uD30C\uC77C\uC774 \uC874\uC7AC\uD558\uC9C0\uB9CC \uBE44\uC5B4 \uC788\uC74C: "},
        {"Keystore.file.does.not.exist.",
                "\uD0A4 \uC800\uC7A5\uC18C \uD30C\uC77C\uC774 \uC874\uC7AC\uD558\uC9C0 \uC54A\uC74C: "},
        {"Must.specify.destination.alias", "\uB300\uC0C1 \uBCC4\uCE6D\uC744 \uC9C0\uC815\uD574\uC57C \uD569\uB2C8\uB2E4."},
        {"Must.specify.alias", "\uBCC4\uCE6D\uC744 \uC9C0\uC815\uD574\uC57C \uD569\uB2C8\uB2E4."},
        {"Keystore.password.must.be.at.least.6.characters",
                "\uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uB294 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Enter.the.password.to.be.stored.",
                "\uC800\uC7A5\uD560 \uBE44\uBC00\uBC88\uD638 \uC785\uB825:  "},
        {"Enter.keystore.password.", "\uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638 \uC785\uB825:  "},
        {"Enter.source.keystore.password.", "\uC18C\uC2A4 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638 \uC785\uB825:  "},
        {"Enter.destination.keystore.password.", "\uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638 \uC785\uB825:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "\uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uAC00 \uB108\uBB34 \uC9E7\uC74C - 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Unknown.Entry.Type", "\uC54C \uC218 \uC5C6\uB294 \uD56D\uBAA9 \uC720\uD615"},
        {"Too.many.failures.Alias.not.changed", "\uC624\uB958\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4. \uBCC4\uCE6D\uC774 \uBCC0\uACBD\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Entry.for.alias.alias.successfully.imported.",
                 "{0} \uBCC4\uCE6D\uC5D0 \uB300\uD55C \uD56D\uBAA9\uC774 \uC131\uACF5\uC801\uC73C\uB85C \uC784\uD3EC\uD2B8\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Entry.for.alias.alias.not.imported.", "{0} \uBCC4\uCE6D\uC5D0 \uB300\uD55C \uD56D\uBAA9\uC774 \uC784\uD3EC\uD2B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "{0} \uBCC4\uCE6D\uC5D0 \uB300\uD55C \uD56D\uBAA9\uC744 \uC784\uD3EC\uD2B8\uD558\uB294 \uC911 \uBB38\uC81C \uBC1C\uC0DD: {1}.\n{0} \uBCC4\uCE6D\uC5D0 \uB300\uD55C \uD56D\uBAA9\uC774 \uC784\uD3EC\uD2B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "\uC784\uD3EC\uD2B8 \uBA85\uB839 \uC644\uB8CC: \uC131\uACF5\uC801\uC73C\uB85C \uC784\uD3EC\uD2B8\uB41C \uD56D\uBAA9\uC740 {0}\uAC1C, \uC2E4\uD328\uD558\uAC70\uB098 \uCDE8\uC18C\uB41C \uD56D\uBAA9\uC740 {1}\uAC1C\uC785\uB2C8\uB2E4."},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "\uACBD\uACE0: \uB300\uC0C1 \uD0A4 \uC800\uC7A5\uC18C\uC5D0\uC11C \uAE30\uC874 \uBCC4\uCE6D {0}\uC744(\uB97C) \uACB9\uCCD0 \uC4F0\uB294 \uC911"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "\uAE30\uC874 \uD56D\uBAA9 \uBCC4\uCE6D {0}\uC774(\uAC00) \uC874\uC7AC\uD569\uB2C8\uB2E4. \uACB9\uCCD0 \uC4F0\uACA0\uC2B5\uB2C8\uAE4C? [\uC544\uB2C8\uC624]:  "},
        {"Too.many.failures.try.later", "\uC624\uB958\uAC00 \uB108\uBB34 \uB9CE\uC74C - \uB098\uC911\uC5D0 \uC2DC\uB3C4\uD558\uC2ED\uC2DC\uC624."},
        {"Certification.request.stored.in.file.filename.",
                "\uC778\uC99D \uC694\uCCAD\uC774 <{0}> \uD30C\uC77C\uC5D0 \uC800\uC7A5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Submit.this.to.your.CA", "CA\uC5D0\uAC8C \uC81C\uCD9C\uD558\uC2ED\uC2DC\uC624."},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "\uBCC4\uCE6D\uC744 \uC9C0\uC815\uD558\uC9C0 \uC54A\uC740 \uACBD\uC6B0 destalias \uBC0F srckeypass\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "\uB300\uC0C1 pkcs12 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uB2E4\uB978 storepass \uBC0F keypass\uAC00 \uC788\uC2B5\uB2C8\uB2E4. \uC9C0\uC815\uB41C -destkeypass\uB85C \uC7AC\uC2DC\uB3C4\uD558\uC2ED\uC2DC\uC624."},
        {"Certificate.stored.in.file.filename.",
                "\uC778\uC99D\uC11C\uAC00 <{0}> \uD30C\uC77C\uC5D0 \uC800\uC7A5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Certificate.reply.was.installed.in.keystore",
                "\uC778\uC99D\uC11C \uD68C\uC2E0\uC774 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC124\uCE58\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Certificate.reply.was.not.installed.in.keystore",
                "\uC778\uC99D\uC11C \uD68C\uC2E0\uC774 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC124\uCE58\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Certificate.was.added.to.keystore",
                "\uC778\uC99D\uC11C\uAC00 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uCD94\uAC00\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"Certificate.was.not.added.to.keystore",
                "\uC778\uC99D\uC11C\uAC00 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uCD94\uAC00\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {".Storing.ksfname.", "[{0}\uC744(\uB97C) \uC800\uC7A5\uD558\uB294 \uC911]"},
        {"alias.has.no.public.key.certificate.",
                "{0}\uC5D0 \uACF5\uC6A9 \uD0A4(\uC778\uC99D\uC11C)\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Cannot.derive.signature.algorithm",
                "\uC11C\uBA85 \uC54C\uACE0\uB9AC\uC998\uC744 \uD30C\uC0DD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Alias.alias.does.not.exist",
                "<{0}> \uBCC4\uCE6D\uC774 \uC874\uC7AC\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {"Alias.alias.has.no.certificate",
                "<{0}> \uBCC4\uCE6D\uC5D0 \uC778\uC99D\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "\uD0A4 \uC30D\uC774 \uC0DD\uC131\uB418\uC9C0 \uC54A\uC558\uC73C\uBA70 <{0}> \uBCC4\uCE6D\uC774 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "\uB2E4\uC74C\uC5D0 \uB300\uD574 \uC720\uD6A8 \uAE30\uAC04\uC774 {3}\uC77C\uC778 {0}\uBE44\uD2B8 {1} \uD0A4 \uC30D \uBC0F \uC790\uCCB4 \uC11C\uBA85\uB41C \uC778\uC99D\uC11C({2})\uB97C \uC0DD\uC131\uD558\uB294 \uC911\n\t: {4}"},
        {"Enter.key.password.for.alias.", "<{0}>\uC5D0 \uB300\uD55C \uD0A4 \uBE44\uBC00\uBC88\uD638\uB97C \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {".RETURN.if.same.as.keystore.password.",
                "\t(\uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uC640 \uB3D9\uC77C\uD55C \uACBD\uC6B0 Enter \uD0A4\uB97C \uB204\uB984):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "\uD0A4 \uBE44\uBC00\uBC88\uD638\uAC00 \uB108\uBB34 \uC9E7\uC74C - 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Too.many.failures.key.not.added.to.keystore",
                "\uC624\uB958\uAC00 \uB108\uBB34 \uB9CE\uC74C - \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uD0A4\uAC00 \uCD94\uAC00\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"Destination.alias.dest.already.exists",
                "\uB300\uC0C1 \uBCC4\uCE6D <{0}>\uC774(\uAC00) \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "\uBE44\uBC00\uBC88\uD638\uAC00 \uB108\uBB34 \uC9E7\uC74C - 6\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},
        {"Too.many.failures.Key.entry.not.cloned",
                "\uC624\uB958\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4. \uD0A4 \uD56D\uBAA9\uC774 \uBCF5\uC81C\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},
        {"key.password.for.alias.", "<{0}>\uC5D0 \uB300\uD55C \uD0A4 \uBE44\uBC00\uBC88\uD638"},
        {"Keystore.entry.for.id.getName.already.exists",
                "<{0}>\uC5D0 \uB300\uD55C \uD0A4 \uC800\uC7A5\uC18C \uD56D\uBAA9\uC774 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Creating.keystore.entry.for.id.getName.",
                "<{0}>\uC5D0 \uB300\uD55C \uD0A4 \uC800\uC7A5\uC18C \uD56D\uBAA9\uC744 \uC0DD\uC131\uD558\uB294 \uC911..."},
        {"No.entries.from.identity.database.added",
                "ID \uB370\uC774\uD130\uBCA0\uC774\uC2A4\uC5D0\uC11C \uCD94\uAC00\uB41C \uD56D\uBAA9\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Alias.name.alias", "\uBCC4\uCE6D \uC774\uB984: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "\uC0DD\uC131 \uB0A0\uC9DC: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "\uD56D\uBAA9 \uC720\uD615: {0}"},
        {"Certificate.chain.length.", "\uC778\uC99D\uC11C \uCCB4\uC778 \uAE38\uC774: "},
        {"Certificate.i.1.", "\uC778\uC99D\uC11C[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "\uC778\uC99D\uC11C \uC9C0\uBB38(SHA-256): "},
        {"Keystore.type.", "\uD0A4 \uC800\uC7A5\uC18C \uC720\uD615: "},
        {"Keystore.provider.", "\uD0A4 \uC800\uC7A5\uC18C \uC81C\uACF5\uC790: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "\uD0A4 \uC800\uC7A5\uC18C\uC5D0 {0,number,integer}\uAC1C\uC758 \uD56D\uBAA9\uC774 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},
        {"Your.keystore.contains.keyStore.size.entries",
                "\uD0A4 \uC800\uC7A5\uC18C\uC5D0 {0,number,integer}\uAC1C\uC758 \uD56D\uBAA9\uC774 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},
        {"Failed.to.parse.input", "\uC785\uB825\uAC12\uC758 \uAD6C\uBB38 \uBD84\uC11D\uC744 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},
        {"Empty.input", "\uC785\uB825\uAC12\uC774 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},
        {"Not.X.509.certificate", "X.509 \uC778\uC99D\uC11C\uAC00 \uC544\uB2D9\uB2C8\uB2E4."},
        {"alias.has.no.public.key", "{0}\uC5D0 \uACF5\uC6A9 \uD0A4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"alias.has.no.X.509.certificate", "{0}\uC5D0 X.509 \uC778\uC99D\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"New.certificate.self.signed.", "\uC0C8 \uC778\uC99D\uC11C(\uC790\uCCB4 \uC11C\uBA85):"},
        {"Reply.has.no.certificates", "\uD68C\uC2E0\uC5D0 \uC778\uC99D\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Certificate.not.imported.alias.alias.already.exists",
                "\uC778\uC99D\uC11C\uAC00 \uC784\uD3EC\uD2B8\uB418\uC9C0 \uC54A\uC558\uC73C\uBA70 <{0}> \uBCC4\uCE6D\uC774 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Input.not.an.X.509.certificate", "\uC785\uB825\uC774 X.509 \uC778\uC99D\uC11C\uAC00 \uC544\uB2D9\uB2C8\uB2E4."},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "\uC778\uC99D\uC11C\uAC00 <{0}> \uBCC4\uCE6D \uC544\uB798\uC758 \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Do.you.still.want.to.add.it.no.",
                "\uCD94\uAC00\uD558\uACA0\uC2B5\uB2C8\uAE4C? [\uC544\uB2C8\uC624]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "\uC778\uC99D\uC11C\uAC00 <{0}> \uBCC4\uCE6D \uC544\uB798\uC5D0 \uC788\uB294 \uC2DC\uC2A4\uD15C \uCC28\uC6D0\uC758 CA \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "\uACE0\uC720\uD55C \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uCD94\uAC00\uD558\uACA0\uC2B5\uB2C8\uAE4C? [\uC544\uB2C8\uC624]:  "},
        {"Trust.this.certificate.no.", "\uC774 \uC778\uC99D\uC11C\uB97C \uC2E0\uB8B0\uD569\uB2C8\uAE4C? [\uC544\uB2C8\uC624]:  "},
        {"YES", "\uC608"},
        {"New.prompt.", "\uC0C8 {0}: "},
        {"Passwords.must.differ", "\uBE44\uBC00\uBC88\uD638\uB294 \uB2EC\uB77C\uC57C \uD569\uB2C8\uB2E4."},
        {"Re.enter.new.prompt.", "\uC0C8 {0} \uB2E4\uC2DC \uC785\uB825: "},
        {"Re.enter.password.", "\uBE44\uBC00\uBC88\uD638  \uB2E4\uC2DC \uC785\uB825: "},
        {"Re.enter.new.password.", "\uC0C8 \uBE44\uBC00\uBC88\uD638 \uB2E4\uC2DC \uC785\uB825: "},
        {"They.don.t.match.Try.again", "\uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD558\uC2ED\uC2DC\uC624."},
        {"Enter.prompt.alias.name.", "{0} \uBCC4\uCE6D \uC774\uB984 \uC785\uB825:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "\uC0C8 \uBCC4\uCE6D \uC774\uB984 \uC785\uB825\t(\uC774 \uD56D\uBAA9\uC5D0 \uB300\uD55C \uC784\uD3EC\uD2B8\uB97C \uCDE8\uC18C\uD558\uB824\uBA74 Enter \uD0A4\uB97C \uB204\uB984):  "},
        {"Enter.alias.name.", "\uBCC4\uCE6D \uC774\uB984 \uC785\uB825:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(<{0}>\uACFC(\uC640) \uB3D9\uC77C\uD55C \uACBD\uC6B0 Enter \uD0A4\uB97C \uB204\uB984)"},
        {"What.is.your.first.and.last.name.",
                "\uC774\uB984\uACFC \uC131\uC744 \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {"What.is.the.name.of.your.organizational.unit.",
                "\uC870\uC9C1 \uB2E8\uC704 \uC774\uB984\uC744 \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {"What.is.the.name.of.your.organization.",
                "\uC870\uC9C1 \uC774\uB984\uC744 \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {"What.is.the.name.of.your.City.or.Locality.",
                "\uAD6C/\uAD70/\uC2DC \uC774\uB984\uC744 \uC785\uB825\uD558\uC2ED\uC2DC\uC624?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "\uC2DC/\uB3C4 \uC774\uB984\uC744 \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "\uC774 \uC870\uC9C1\uC758 \uB450 \uC790\uB9AC \uAD6D\uAC00 \uCF54\uB4DC\uB97C \uC785\uB825\uD558\uC2ED\uC2DC\uC624."},
        {"Is.name.correct.", "{0}\uC774(\uAC00) \uB9DE\uC2B5\uB2C8\uAE4C?"},
        {"no", "\uC544\uB2C8\uC624"},
        {"yes", "\uC608"},
        {"y", "y"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "<{0}> \uBCC4\uCE6D\uC5D0 \uD0A4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "<{0}> \uBCC4\uCE6D\uC740 \uC804\uC6A9 \uD0A4 \uD56D\uBAA9\uC774 \uC544\uB2CC \uD56D\uBAA9 \uC720\uD615\uC744 \uCC38\uC870\uD569\uB2C8\uB2E4. -keyclone \uBA85\uB839\uC740 \uC804\uC6A9 \uD0A4 \uD56D\uBAA9\uC758 \uBCF5\uC81C\uB9CC \uC9C0\uC6D0\uD569\uB2C8\uB2E4."},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "\uC11C\uBA85\uC790 #%d:"},
        {"Timestamp.", "\uC2DC\uAC04 \uAE30\uB85D:"},
        {"Signature.", "\uC11C\uBA85:"},
        {"CRLs.", "CRL:"},
        {"Certificate.owner.", "\uC778\uC99D\uC11C \uC18C\uC720\uC790: "},
        {"Not.a.signed.jar.file", "\uC11C\uBA85\uB41C jar \uD30C\uC77C\uC774 \uC544\uB2D9\uB2C8\uB2E4."},
        {"No.certificate.from.the.SSL.server",
                "SSL \uC11C\uBC84\uC5D0\uC11C \uAC00\uC838\uC628 \uC778\uC99D\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* \uD0A4 \uC800\uC7A5\uC18C\uC5D0 \uC800\uC7A5\uB41C \uC815\uBCF4\uC758 \uBB34\uACB0\uC131\uC774  *\n* \uD655\uC778\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4! \uBB34\uACB0\uC131\uC744 \uD655\uC778\uD558\uB824\uBA74, *\n* \uD0A4 \uC800\uC7A5\uC18C \uBE44\uBC00\uBC88\uD638\uB97C \uC81C\uACF5\uD574\uC57C \uD569\uB2C8\uB2E4.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* srckeystore\uC5D0 \uC800\uC7A5\uB41C \uC815\uBCF4\uC758 \uBB34\uACB0\uC131\uC774  *\n* \uD655\uC778\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4! \uBB34\uACB0\uC131\uC744 \uD655\uC778\uD558\uB824\uBA74, *\n* srckeystore \uBE44\uBC00\uBC88\uD638\uB97C \uC81C\uACF5\uD574\uC57C \uD569\uB2C8\uB2E4.                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "\uC778\uC99D\uC11C \uD68C\uC2E0\uC5D0 <{0}>\uC5D0 \uB300\uD55C \uACF5\uC6A9 \uD0A4\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {"Incomplete.certificate.chain.in.reply",
                "\uD68C\uC2E0\uC5D0 \uBD88\uC644\uC804\uD55C \uC778\uC99D\uC11C \uCCB4\uC778\uC774 \uC788\uC2B5\uB2C8\uB2E4."},
        {"Certificate.chain.in.reply.does.not.verify.",
                "\uD68C\uC2E0\uC758 \uC778\uC99D\uC11C \uCCB4\uC778\uC774 \uD655\uC778\uB418\uC9C0 \uC54A\uC74C: "},
        {"Top.level.certificate.in.reply.",
                "\uD68C\uC2E0\uC5D0 \uCD5C\uC0C1\uC704 \uB808\uBCA8 \uC778\uC99D\uC11C\uAC00 \uC788\uC74C:\n"},
        {".is.not.trusted.", "...\uC744(\uB97C) \uC2E0\uB8B0\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. "},
        {"Install.reply.anyway.no.", "\uD68C\uC2E0\uC744 \uC124\uCE58\uD558\uACA0\uC2B5\uB2C8\uAE4C? [\uC544\uB2C8\uC624]:  "},
        {"NO", "\uC544\uB2C8\uC624"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "\uD68C\uC2E0\uACFC \uD0A4 \uC800\uC7A5\uC18C\uC758 \uACF5\uC6A9 \uD0A4\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "\uD68C\uC2E0\uACFC \uD0A4 \uC800\uC7A5\uC18C\uC758 \uC778\uC99D\uC11C\uAC00 \uB3D9\uC77C\uD569\uB2C8\uB2E4."},
        {"Failed.to.establish.chain.from.reply",
                "\uD68C\uC2E0\uC758 \uCCB4\uC778 \uC124\uC815\uC744 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},
        {"n", "n"},
        {"Wrong.answer.try.again", "\uC798\uBABB\uB41C \uC751\uB2F5\uC785\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD558\uC2ED\uC2DC\uC624."},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "\uBCF4\uC548 \uD0A4\uAC00 \uC0DD\uC131\uB418\uC9C0 \uC54A\uC558\uC73C\uBA70 <{0}> \uBCC4\uCE6D\uC774 \uC874\uC7AC\uD569\uB2C8\uB2E4."},
        {"Please.provide.keysize.for.secret.key.generation",
                "\uBCF4\uC548 \uD0A4\uB97C \uC0DD\uC131\uD558\uB824\uBA74 -keysize\uB97C \uC81C\uACF5\uD558\uC2ED\uC2DC\uC624."},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "\uACBD\uACE0: \uD655\uC778\uB418\uC9C0 \uC54A\uC74C. -keystore\uAC00 \uC62C\uBC14\uB978\uC9C0 \uD655\uC778\uD558\uC2ED\uC2DC\uC624."},

        {"Extensions.", "\uD655\uC7A5: "},
        {".Empty.value.", "(\uBE44\uC5B4 \uC788\uB294 \uAC12)"},
        {"Extension.Request.", "\uD655\uC7A5 \uC694\uCCAD:"},
        {"Unknown.keyUsage.type.", "\uC54C \uC218 \uC5C6\uB294 keyUsage \uC720\uD615: "},
        {"Unknown.extendedkeyUsage.type.", "\uC54C \uC218 \uC5C6\uB294 extendedkeyUsage \uC720\uD615: "},
        {"Unknown.AccessDescription.type.", "\uC54C \uC218 \uC5C6\uB294 AccessDescription \uC720\uD615: "},
        {"Unrecognized.GeneralName.type.", "\uC54C \uC218 \uC5C6\uB294 GeneralName \uC720\uD615: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "\uC774 \uD655\uC7A5\uC740 \uC911\uC694\uD55C \uAC83\uC73C\uB85C \uD45C\uC2DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. "},
        {"Odd.number.of.hex.digits.found.", "\uD640\uC218 \uAC1C\uC758 16\uC9C4\uC218\uAC00 \uBC1C\uACAC\uB428: "},
        {"Unknown.extension.type.", "\uC54C \uC218 \uC5C6\uB294 \uD655\uC7A5 \uC720\uD615: "},
        {"command.{0}.is.ambiguous.", "{0} \uBA85\uB839\uC774 \uBAA8\uD638\uD568:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "\uC778\uC99D\uC11C \uC694\uCCAD"},
        {"the.issuer", "\uBC1C\uD589\uC790"},
        {"the.generated.certificate", "\uC0DD\uC131\uB41C \uC778\uC99D\uC11C"},
        {"the.generated.crl", "\uC0DD\uC131\uB41C CRL"},
        {"the.generated.certificate.request", "\uC0DD\uC131\uB41C \uC778\uC99D\uC11C \uC694\uCCAD"},
        {"the.certificate", "\uC778\uC99D\uC11C"},
        {"the.crl", "CRL"},
        {"the.tsa.certificate", "TSA \uC778\uC99D\uC11C"},
        {"the.input", "\uC785\uB825"},
        {"reply", "\uD68C\uC2E0"},
        {"one.in.many", "%1$s #%2$d/%3$d"},
        {"alias.in.cacerts", "cacerts\uC758 <%s> \uBC1C\uD589\uC790"},
        {"alias.in.keystore", "<%s> \uBC1C\uD589\uC790"},
        {"with.weak", "%s(\uC57D\uD568)"},
        {"key.bit", "%1$d\uBE44\uD2B8 %2$s \uD0A4"},
        {"key.bit.weak", "%1$d\uBE44\uD2B8 %2$s \uD0A4(\uC57D\uD568)"},
        {"unknown.size.1", "\uC54C \uC218 \uC5C6\uB294 \uD06C\uAE30 %s \uD0A4"},
        {".PATTERN.printX509Cert.with.weak",
                "\uC18C\uC720\uC790: {0}\n\uBC1C\uD589\uC790: {1}\n\uC77C\uB828 \uBC88\uD638: {2}\n\uC801\uD569\uD55C \uC2DC\uC791 \uB0A0\uC9DC: {3} \uC885\uB8CC \uB0A0\uC9DC: {4}\n\uC778\uC99D\uC11C \uC9C0\uBB38:\n\t SHA1: {5}\n\t SHA256: {6}\n\uC11C\uBA85 \uC54C\uACE0\uB9AC\uC998 \uC774\uB984: {7}\n\uC8FC\uCCB4 \uACF5\uC6A9 \uD0A4 \uC54C\uACE0\uB9AC\uC998: {8}\n\uBC84\uC804: {9}"},
        {"PKCS.10.with.weak",
                "PKCS #10 \uC778\uC99D\uC11C \uC694\uCCAD(1.0 \uBC84\uC804)\n\uC81C\uBAA9: %1$s\n\uD615\uC2DD: %2$s\n\uACF5\uC6A9 \uD0A4: %3$s\n\uC11C\uBA85 \uC54C\uACE0\uB9AC\uC998: %4$s\n"},
        {"verified.by.s.in.s.weak", "%3$s\uC744(\uB97C) \uD3EC\uD568\uD558\uB294 %2$s\uC758 %1$s\uC5D0 \uC758\uD574 \uD655\uC778\uB428"},
        {"whose.sigalg.risk", "%1$s\uC774(\uAC00) \uBCF4\uC548 \uC704\uD5D8\uC73C\uB85C \uAC04\uC8FC\uB418\uB294 %2$s \uC11C\uBA85 \uC54C\uACE0\uB9AC\uC998\uC744 \uC0AC\uC6A9\uD569\uB2C8\uB2E4."},
        {"whose.key.risk", "%1$s\uC774(\uAC00) \uBCF4\uC548 \uC704\uD5D8\uC73C\uB85C \uAC04\uC8FC\uB418\uB294 %2$s\uC744(\uB97C) \uC0AC\uC6A9\uD569\uB2C8\uB2E4."},
        {"jks.storetype.warning", "%1$s \uD0A4 \uC800\uC7A5\uC18C\uB294 \uACE0\uC720 \uD615\uC2DD\uC744 \uC0AC\uC6A9\uD569\uB2C8\uB2E4. \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"\uB97C \uC0AC\uC6A9\uD558\uB294 \uC0B0\uC5C5 \uD45C\uC900 \uD615\uC2DD\uC778 PKCS12\uB85C \uC774\uC804\uD558\uB294 \uAC83\uC774 \uC88B\uC2B5\uB2C8\uB2E4."},
        {"migrate.keystore.warning", "\"%1$s\"\uC744(\uB97C) %4$s(\uC73C)\uB85C \uC774\uC804\uD588\uC2B5\uB2C8\uB2E4. %2$s \uD0A4 \uC800\uC7A5\uC18C\uAC00 \"%3$s\"(\uC73C)\uB85C \uBC31\uC5C5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"backup.keystore.warning", "\uC6D0\uBCF8 \uD0A4 \uC800\uC7A5\uC18C \"%1$s\"\uC774(\uAC00) \"%3$s\"(\uC73C)\uB85C \uBC31\uC5C5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},
        {"importing.keystore.status", "\uD0A4 \uC800\uC7A5\uC18C %1$s\uC744(\uB97C) %2$s(\uC73C)\uB85C \uC784\uD3EC\uD2B8\uD558\uB294 \uC911..."},
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
