/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
public class Resources_ja extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "\u30AA\u30D7\u30B7\u30E7\u30F3:"},
        {"option.1.set.twice", "%s\u30AA\u30D7\u30B7\u30E7\u30F3\u304C\u8907\u6570\u56DE\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u3059\u3002\u6700\u5F8C\u306E\u3082\u306E\u4EE5\u5916\u306F\u3059\u3079\u3066\u7121\u8996\u3055\u308C\u307E\u3059\u3002"},
        {"multiple.commands.1.2", "1\u3064\u306E\u30B3\u30DE\u30F3\u30C9\u306E\u307F\u8A31\u53EF\u3055\u308C\u307E\u3059: %1$s\u3068%2$s\u306E\u4E21\u65B9\u304C\u6307\u5B9A\u3055\u308C\u307E\u3057\u305F\u3002"},
        {"Use.keytool.help.for.all.available.commands",
                 "\u3053\u306E\u30D8\u30EB\u30D7\u30FB\u30E1\u30C3\u30BB\u30FC\u30B8\u3092\u8868\u793A\u3059\u308B\u306B\u306F\"keytool -?\u3001-h\u307E\u305F\u306F--help\"\u3092\u4F7F\u7528\u3057\u307E\u3059"},
        {"Key.and.Certificate.Management.Tool",
                 "\u30AD\u30FC\u304A\u3088\u3073\u8A3C\u660E\u66F8\u7BA1\u7406\u30C4\u30FC\u30EB"},
        {"Commands.", "\u30B3\u30DE\u30F3\u30C9:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "command_name\u306E\u4F7F\u7528\u65B9\u6CD5\u306B\u3064\u3044\u3066\u306F\u3001\"keytool -command_name --help\"\u3092\u4F7F\u7528\u3057\u307E\u3059\u3002\n\u4E8B\u524D\u69CB\u6210\u6E08\u306E\u30AA\u30D7\u30B7\u30E7\u30F3\u30FB\u30D5\u30A1\u30A4\u30EB\u3092\u6307\u5B9A\u3059\u308B\u306B\u306F\u3001-conf <url>\u30AA\u30D7\u30B7\u30E7\u30F3\u3092\u4F7F\u7528\u3057\u307E\u3059\u3002"},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8\u3092\u751F\u6210\u3057\u307E\u3059"}, //-certreq
        {"Changes.an.entry.s.alias",
                "\u30A8\u30F3\u30C8\u30EA\u306E\u5225\u540D\u3092\u5909\u66F4\u3057\u307E\u3059"}, //-changealias
        {"Deletes.an.entry",
                "\u30A8\u30F3\u30C8\u30EA\u3092\u524A\u9664\u3057\u307E\u3059"}, //-delete
        {"Exports.certificate",
                "\u8A3C\u660E\u66F8\u3092\u30A8\u30AF\u30B9\u30DD\u30FC\u30C8\u3057\u307E\u3059"}, //-exportcert
        {"Generates.a.key.pair",
                "\u30AD\u30FC\u30FB\u30DA\u30A2\u3092\u751F\u6210\u3057\u307E\u3059"}, //-genkeypair
        {"Generates.a.secret.key",
                "\u79D8\u5BC6\u30AD\u30FC\u3092\u751F\u6210\u3057\u307E\u3059"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8\u304B\u3089\u8A3C\u660E\u66F8\u3092\u751F\u6210\u3057\u307E\u3059"}, //-gencert
        {"Generates.CRL", "CRL\u3092\u751F\u6210\u3057\u307E\u3059"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "{0}\u79D8\u5BC6\u30AD\u30FC\u3092\u751F\u6210\u3057\u307E\u3057\u305F"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "{0}\u30D3\u30C3\u30C8{1}\u79D8\u5BC6\u30AD\u30FC\u3092\u751F\u6210\u3057\u307E\u3057\u305F"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "JDK 1.1.x-style\u30A2\u30A4\u30C7\u30F3\u30C6\u30A3\u30C6\u30A3\u30FB\u30C7\u30FC\u30BF\u30D9\u30FC\u30B9\u304B\u3089\u30A8\u30F3\u30C8\u30EA\u3092\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u307E\u3059"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "\u8A3C\u660E\u66F8\u307E\u305F\u306F\u8A3C\u660E\u66F8\u30C1\u30A7\u30FC\u30F3\u3092\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u307E\u3059"}, //-importcert
        {"Imports.a.password",
                "\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u307E\u3059"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "\u5225\u306E\u30AD\u30FC\u30B9\u30C8\u30A2\u304B\u30891\u3064\u307E\u305F\u306F\u3059\u3079\u3066\u306E\u30A8\u30F3\u30C8\u30EA\u3092\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u307E\u3059"}, //-importkeystore
        {"Clones.a.key.entry",
                "\u30AD\u30FC\u30FB\u30A8\u30F3\u30C8\u30EA\u306E\u30AF\u30ED\u30FC\u30F3\u3092\u4F5C\u6210\u3057\u307E\u3059"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "\u30A8\u30F3\u30C8\u30EA\u306E\u30AD\u30FC\u30FB\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5909\u66F4\u3057\u307E\u3059"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u5185\u306E\u30A8\u30F3\u30C8\u30EA\u3092\u30EA\u30B9\u30C8\u3057\u307E\u3059"}, //-list
        {"Prints.the.content.of.a.certificate",
                "\u8A3C\u660E\u66F8\u306E\u5185\u5BB9\u3092\u51FA\u529B\u3057\u307E\u3059"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8\u306E\u5185\u5BB9\u3092\u51FA\u529B\u3057\u307E\u3059"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "CRL\u30D5\u30A1\u30A4\u30EB\u306E\u5185\u5BB9\u3092\u51FA\u529B\u3057\u307E\u3059"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "\u81EA\u5DF1\u7F72\u540D\u578B\u8A3C\u660E\u66F8\u3092\u751F\u6210\u3057\u307E\u3059"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30B9\u30C8\u30A2\u30FB\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5909\u66F4\u3057\u307E\u3059"}, //-storepasswd
        {"showinfo.command.help", "\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u95A2\u9023\u60C5\u5831\u3092\u8868\u793A\u3057\u307E\u3059"},

        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "\u51E6\u7406\u3059\u308B\u30A8\u30F3\u30C8\u30EA\u306E\u5225\u540D"}, //-alias
        {"groupname.option.help",
                "\u30B0\u30EB\u30FC\u30D7\u540D\u3002\u305F\u3068\u3048\u3070\u3001\u6955\u5186\u66F2\u7DDA\u540D\u3067\u3059\u3002"}, //-groupname
        {"destination.alias",
                "\u51FA\u529B\u5148\u306E\u5225\u540D"}, //-destalias
        {"destination.key.password",
                "\u51FA\u529B\u5148\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-destkeypass
        {"destination.keystore.name",
                "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u540D"}, //-destkeystore
        {"destination.keystore.password.protected",
                "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u4FDD\u8B77\u5BFE\u8C61\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-destprotected
        {"destination.keystore.provider.name",
                "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D7\u30ED\u30D0\u30A4\u30C0\u540D"}, //-destprovidername
        {"destination.keystore.password",
                "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-deststorepass
        {"destination.keystore.type",
                "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30BF\u30A4\u30D7"}, //-deststoretype
        {"distinguished.name",
                "\u8B58\u5225\u540D"}, //-dname
        {"X.509.extension",
                "X.509\u62E1\u5F35"}, //-ext
        {"output.file.name",
                "\u51FA\u529B\u30D5\u30A1\u30A4\u30EB\u540D"}, //-file and -outfile
        {"input.file.name",
                "\u5165\u529B\u30D5\u30A1\u30A4\u30EB\u540D"}, //-file and -infile
        {"key.algorithm.name",
                "\u30AD\u30FC\u30FB\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u540D"}, //-keyalg
        {"key.password",
                "\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-keypass
        {"key.bit.size",
                "\u30AD\u30FC\u306E\u30D3\u30C3\u30C8\u30FB\u30B5\u30A4\u30BA"}, //-keysize
        {"keystore.name",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u540D"}, //-keystore
        {"access.the.cacerts.keystore",
                "cacerts\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u30A2\u30AF\u30BB\u30B9\u3059\u308B"}, // -cacerts
        {"warning.cacerts.option",
                "\u8B66\u544A: cacerts\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u30A2\u30AF\u30BB\u30B9\u3059\u308B\u306B\u306F-cacerts\u30AA\u30D7\u30B7\u30E7\u30F3\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"new.password",
                "\u65B0\u898F\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-new
        {"do.not.prompt",
                "\u30D7\u30ED\u30F3\u30D7\u30C8\u3092\u8868\u793A\u3057\u306A\u3044"}, //-noprompt
        {"password.through.protected.mechanism",
                "\u4FDD\u8B77\u30E1\u30AB\u30CB\u30BA\u30E0\u306B\u3088\u308B\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-protected
        {"tls.option.help", "TLS\u69CB\u6210\u60C5\u5831\u3092\u8868\u793A\u3057\u307E\u3059"},

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "\u540D\u524D\u3067\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30D7\u30ED\u30D0\u30A4\u30C0\u3092\u8FFD\u52A0\u3059\u308B(SunPKCS11\u306A\u3069)\n-addprovider\u306E\u5F15\u6570\u3092\u69CB\u6210\u3059\u308B"}, //-addprovider
        {"provider.class.option",
                "\u5B8C\u5168\u4FEE\u98FE\u30AF\u30E9\u30B9\u540D\u3067\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30D7\u30ED\u30D0\u30A4\u30C0\u3092\u8FFD\u52A0\u3059\u308B\n-providerclass\u306E\u5F15\u6570\u3092\u69CB\u6210\u3059\u308B"}, //-providerclass

        {"provider.name",
                "\u30D7\u30ED\u30D0\u30A4\u30C0\u540D"}, //-providername
        {"provider.classpath",
                "\u30D7\u30ED\u30D0\u30A4\u30C0\u30FB\u30AF\u30E9\u30B9\u30D1\u30B9"}, //-providerpath
        {"output.in.RFC.style",
                "RFC\u30B9\u30BF\u30A4\u30EB\u306E\u51FA\u529B"}, //-rfc
        {"signature.algorithm.name",
                "\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u540D"}, //-sigalg
        {"source.alias",
                "\u30BD\u30FC\u30B9\u5225\u540D"}, //-srcalias
        {"source.key.password",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-srckeypass
        {"source.keystore.name",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u540D"}, //-srckeystore
        {"source.keystore.password.protected",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u4FDD\u8B77\u5BFE\u8C61\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-srcprotected
        {"source.keystore.provider.name",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D7\u30ED\u30D0\u30A4\u30C0\u540D"}, //-srcprovidername
        {"source.keystore.password",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-srcstorepass
        {"source.keystore.type",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30BF\u30A4\u30D7"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "SSL\u30B5\u30FC\u30D0\u30FC\u306E\u30DB\u30B9\u30C8\u3068\u30DD\u30FC\u30C8"}, //-sslserver
        {"signed.jar.file",
                "\u7F72\u540D\u4ED8\u304DJAR\u30D5\u30A1\u30A4\u30EB"}, //=jarfile
        {"certificate.validity.start.date.time",
                "\u8A3C\u660E\u66F8\u306E\u6709\u52B9\u958B\u59CB\u65E5\u6642"}, //-startdate
        {"keystore.password",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"}, //-storepass
        {"keystore.type",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30BF\u30A4\u30D7"}, //-storetype
        {"trust.certificates.from.cacerts",
                "cacerts\u304B\u3089\u306E\u8A3C\u660E\u66F8\u3092\u4FE1\u983C\u3059\u308B"}, //-trustcacerts
        {"verbose.output",
                "\u8A73\u7D30\u51FA\u529B"}, //-v
        {"validity.number.of.days",
                "\u59A5\u5F53\u6027\u65E5\u6570"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "\u5931\u52B9\u3059\u308B\u8A3C\u660E\u66F8\u306E\u30B7\u30EA\u30A2\u30EBID"}, //-id
        // keytool: Running part
        {"keytool.error.", "keytool\u30A8\u30E9\u30FC: "},
        {"Illegal.option.", "\u4E0D\u6B63\u306A\u30AA\u30D7\u30B7\u30E7\u30F3:  "},
        {"Illegal.value.", "\u4E0D\u6B63\u306A\u5024: "},
        {"Unknown.password.type.", "\u4E0D\u660E\u306A\u30D1\u30B9\u30EF\u30FC\u30C9\u30FB\u30BF\u30A4\u30D7: "},
        {"Cannot.find.environment.variable.",
                "\u74B0\u5883\u5909\u6570\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093: "},
        {"Cannot.find.file.", "\u30D5\u30A1\u30A4\u30EB\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093: "},
        {"Command.option.flag.needs.an.argument.", "\u30B3\u30DE\u30F3\u30C9\u30FB\u30AA\u30D7\u30B7\u30E7\u30F3{0}\u306B\u306F\u5F15\u6570\u304C\u5FC5\u8981\u3067\u3059\u3002"},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "\u8B66\u544A: PKCS12\u30AD\u30FC\u30B9\u30C8\u30A2\u3067\u306F\u3001\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3068\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u304C\u7570\u306A\u308B\u72B6\u6CC1\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093\u3002\u30E6\u30FC\u30B6\u30FC\u304C\u6307\u5B9A\u3057\u305F{0}\u306E\u5024\u306F\u7121\u8996\u3057\u307E\u3059\u3002"},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "-keystore\u307E\u305F\u306F-storetype\u30AA\u30D7\u30B7\u30E7\u30F3\u306F\u3001-cacerts\u30AA\u30D7\u30B7\u30E7\u30F3\u3068\u3068\u3082\u306B\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-storetype\u304C{0}\u306E\u5834\u5408\u3001-keystore\u306FNONE\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Too.many.retries.program.terminated",
                 "\u518D\u8A66\u884C\u304C\u591A\u3059\u304E\u307E\u3059\u3002\u30D7\u30ED\u30B0\u30E9\u30E0\u304C\u7D42\u4E86\u3057\u307E\u3057\u305F"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "-storetype\u304C{0}\u306E\u5834\u5408\u3001-storepasswd\u30B3\u30DE\u30F3\u30C9\u304A\u3088\u3073-keypasswd\u30B3\u30DE\u30F3\u30C9\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "-storetype\u304CPKCS12\u306E\u5834\u5408\u3001-keypasswd\u30B3\u30DE\u30F3\u30C9\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "-storetype\u304C{0}\u306E\u5834\u5408\u3001-keypass\u3068-new\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "-protected\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u308B\u5834\u5408\u3001-storepass\u3001-keypass\u304A\u3088\u3073-new\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "-srcprotected\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u308B\u5834\u5408\u3001-srcstorepass\u304A\u3088\u3073-srckeypass\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u304C\u30D1\u30B9\u30EF\u30FC\u30C9\u3067\u4FDD\u8B77\u3055\u308C\u3066\u3044\u306A\u3044\u5834\u5408\u3001-storepass\u3001-keypass\u304A\u3088\u3073-new\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u304C\u30D1\u30B9\u30EF\u30FC\u30C9\u3067\u4FDD\u8B77\u3055\u308C\u3066\u3044\u306A\u3044\u5834\u5408\u3001-srcstorepass\u304A\u3088\u3073-srckeypass\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"Illegal.startdate.value", "startdate\u5024\u304C\u7121\u52B9\u3067\u3059"},
        {"Validity.must.be.greater.than.zero",
                "\u59A5\u5F53\u6027\u306F\u30BC\u30ED\u3088\u308A\u5927\u304D\u3044\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"provclass.not.a.provider", "%s\u306F\u30D7\u30ED\u30D0\u30A4\u30C0\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},
        {"provider.name.not.found", "\u30D7\u30ED\u30D0\u30A4\u30C0\u540D\"%s\"\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},
        {"provider.class.not.found", "\u30D7\u30ED\u30D0\u30A4\u30C0\"%s\"\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},
        {"Usage.error.no.command.provided", "\u4F7F\u7528\u30A8\u30E9\u30FC: \u30B3\u30DE\u30F3\u30C9\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},
        {"Source.keystore.file.exists.but.is.empty.", "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D5\u30A1\u30A4\u30EB\u306F\u3001\u5B58\u5728\u3057\u307E\u3059\u304C\u7A7A\u3067\u3059: "},
        {"Please.specify.srckeystore", "-srckeystore\u3092\u6307\u5B9A\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "'list'\u30B3\u30DE\u30F3\u30C9\u306B-v\u3068-rfc\u306E\u4E21\u65B9\u3092\u6307\u5B9A\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},
        {"Key.password.must.be.at.least.6.characters",
                "\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u306F6\u6587\u5B57\u4EE5\u4E0A\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"New.password.must.be.at.least.6.characters",
                "\u65B0\u898F\u30D1\u30B9\u30EF\u30FC\u30C9\u306F6\u6587\u5B57\u4EE5\u4E0A\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Keystore.file.exists.but.is.empty.",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D5\u30A1\u30A4\u30EB\u306F\u5B58\u5728\u3057\u307E\u3059\u304C\u3001\u7A7A\u3067\u3059: "},
        {"Keystore.file.does.not.exist.",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D5\u30A1\u30A4\u30EB\u306F\u5B58\u5728\u3057\u307E\u305B\u3093: "},
        {"Must.specify.destination.alias", "\u51FA\u529B\u5148\u306E\u5225\u540D\u3092\u6307\u5B9A\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Must.specify.alias", "\u5225\u540D\u3092\u6307\u5B9A\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Keystore.password.must.be.at.least.6.characters",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u306F6\u6587\u5B57\u4EE5\u4E0A\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Enter.the.password.to.be.stored.",
                "\u4FDD\u5B58\u3059\u308B\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {"Enter.keystore.password.", "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {"Enter.source.keystore.password.", "\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {"Enter.destination.keystore.password.", "\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u304C\u77ED\u3059\u304E\u307E\u3059 - 6\u6587\u5B57\u4EE5\u4E0A\u306B\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Unknown.Entry.Type", "\u4E0D\u660E\u306A\u30A8\u30F3\u30C8\u30EA\u30FB\u30BF\u30A4\u30D7"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "\u5225\u540D{0}\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u30A4\u30F3\u30DD\u30FC\u30C8\u306B\u6210\u529F\u3057\u307E\u3057\u305F\u3002"},
        {"Entry.for.alias.alias.not.imported.", "\u5225\u540D{0}\u306E\u30A8\u30F3\u30C8\u30EA\u306F\u30A4\u30F3\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "\u5225\u540D{0}\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u30A4\u30F3\u30DD\u30FC\u30C8\u4E2D\u306B\u554F\u984C\u304C\u767A\u751F\u3057\u307E\u3057\u305F: {1}\u3002\n\u5225\u540D{0}\u306E\u30A8\u30F3\u30C8\u30EA\u306F\u30A4\u30F3\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "\u30A4\u30F3\u30DD\u30FC\u30C8\u30FB\u30B3\u30DE\u30F3\u30C9\u304C\u5B8C\u4E86\u3057\u307E\u3057\u305F: {0}\u4EF6\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u30A4\u30F3\u30DD\u30FC\u30C8\u304C\u6210\u529F\u3057\u307E\u3057\u305F\u3002{1}\u4EF6\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u30A4\u30F3\u30DD\u30FC\u30C8\u304C\u5931\u6557\u3057\u305F\u304B\u53D6\u308A\u6D88\u3055\u308C\u307E\u3057\u305F"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "\u8B66\u544A: \u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u5185\u306E\u65E2\u5B58\u306E\u5225\u540D{0}\u3092\u4E0A\u66F8\u304D\u3057\u3066\u3044\u307E\u3059"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "\u65E2\u5B58\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u5225\u540D{0}\u304C\u5B58\u5728\u3057\u3066\u3044\u307E\u3059\u3002\u4E0A\u66F8\u304D\u3057\u307E\u3059\u304B\u3002[\u3044\u3044\u3048]:  "},
        {"Too.many.failures.try.later", "\u969C\u5BB3\u304C\u591A\u3059\u304E\u307E\u3059 - \u5F8C\u3067\u5B9F\u884C\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Certification.request.stored.in.file.filename.",
                "\u8A8D\u8A3C\u30EA\u30AF\u30A8\u30B9\u30C8\u304C\u30D5\u30A1\u30A4\u30EB<{0}>\u306B\u4FDD\u5B58\u3055\u308C\u307E\u3057\u305F"},
        {"Submit.this.to.your.CA", "\u3053\u308C\u3092CA\u306B\u63D0\u51FA\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "\u5225\u540D\u3092\u6307\u5B9A\u3057\u306A\u3044\u5834\u5408\u3001\u51FA\u529B\u5148\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u5225\u540D\u304A\u3088\u3073\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u306F\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "\u51FA\u529B\u5148pkcs12\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u3001\u7570\u306A\u308Bstorepass\u304A\u3088\u3073keypass\u304C\u3042\u308A\u307E\u3059\u3002-destkeypass\u3092\u6307\u5B9A\u3057\u3066\u518D\u8A66\u884C\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
        {"Certificate.stored.in.file.filename.",
                "\u8A3C\u660E\u66F8\u304C\u30D5\u30A1\u30A4\u30EB<{0}>\u306B\u4FDD\u5B58\u3055\u308C\u307E\u3057\u305F"},
        {"Certificate.reply.was.installed.in.keystore",
                "\u8A3C\u660E\u66F8\u5FDC\u7B54\u304C\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u30A4\u30F3\u30B9\u30C8\u30FC\u30EB\u3055\u308C\u307E\u3057\u305F"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "\u8A3C\u660E\u66F8\u5FDC\u7B54\u304C\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u30A4\u30F3\u30B9\u30C8\u30FC\u30EB\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F"},
        {"Certificate.was.added.to.keystore",
                "\u8A3C\u660E\u66F8\u304C\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u8FFD\u52A0\u3055\u308C\u307E\u3057\u305F"},
        {"Certificate.was.not.added.to.keystore",
                "\u8A3C\u660E\u66F8\u304C\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u8FFD\u52A0\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F"},
        {".Storing.ksfname.", "[{0}\u3092\u683C\u7D0D\u4E2D]"},
        {"alias.has.no.public.key.certificate.",
                "{0}\u306B\u306F\u516C\u958B\u30AD\u30FC(\u8A3C\u660E\u66F8)\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"Cannot.derive.signature.algorithm",
                "\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u3092\u53D6\u5F97\u3067\u304D\u307E\u305B\u3093"},
        {"Alias.alias.does.not.exist",
                "\u5225\u540D<{0}>\u306F\u5B58\u5728\u3057\u307E\u305B\u3093"},
        {"Alias.alias.has.no.certificate",
                "\u5225\u540D<{0}>\u306B\u306F\u8A3C\u660E\u66F8\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"groupname.keysize.coexist",
                "-groupname\u3068-keysize\u306E\u4E21\u65B9\u3092\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},
        {"deprecate.keysize.for.ec",
                "-keysize\u306E\u6307\u5B9A\u306B\u3088\u308BEC\u30AD\u30FC\u306E\u751F\u6210\u306F\u975E\u63A8\u5968\u3067\u3059\u3002\u304B\u308F\u308A\u306B\"-groupname %s\"\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "\u30AD\u30FC\u30FB\u30DA\u30A2\u306F\u751F\u6210\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002\u5225\u540D<{0}>\u306F\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "{3}\u65E5\u9593\u6709\u52B9\u306A{0}\u30D3\u30C3\u30C8\u306E{1}\u306E\u30AD\u30FC\u30FB\u30DA\u30A2\u3068\u81EA\u5DF1\u7F72\u540D\u578B\u8A3C\u660E\u66F8({2})\u3092\u751F\u6210\u3057\u3066\u3044\u307E\u3059\n\t\u30C7\u30A3\u30EC\u30AF\u30C8\u30EA\u540D: {4}"},
        {"Enter.key.password.for.alias.", "<{0}>\u306E\u30AD\u30FC\u30FB\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3068\u540C\u3058\u5834\u5408\u306FRETURN\u3092\u62BC\u3057\u3066\u304F\u3060\u3055\u3044):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u304C\u77ED\u3059\u304E\u307E\u3059 - 6\u6587\u5B57\u4EE5\u4E0A\u3092\u6307\u5B9A\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Too.many.failures.key.not.added.to.keystore",
                "\u969C\u5BB3\u304C\u591A\u3059\u304E\u307E\u3059 - \u30AD\u30FC\u306F\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u8FFD\u52A0\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F"},
        {"Destination.alias.dest.already.exists",
                "\u51FA\u529B\u5148\u306E\u5225\u540D<{0}>\u306F\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "\u30D1\u30B9\u30EF\u30FC\u30C9\u304C\u77ED\u3059\u304E\u307E\u3059 - 6\u6587\u5B57\u4EE5\u4E0A\u3092\u6307\u5B9A\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Too.many.failures.Key.entry.not.cloned",
                "\u969C\u5BB3\u304C\u591A\u3059\u304E\u307E\u3059\u3002\u30AD\u30FC\u30FB\u30A8\u30F3\u30C8\u30EA\u306E\u30AF\u30ED\u30FC\u30F3\u306F\u4F5C\u6210\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F"},
        {"key.password.for.alias.", "<{0}>\u306E\u30AD\u30FC\u306E\u30D1\u30B9\u30EF\u30FC\u30C9"},
        {"No.entries.from.identity.database.added",
                "\u30A2\u30A4\u30C7\u30F3\u30C6\u30A3\u30C6\u30A3\u30FB\u30C7\u30FC\u30BF\u30D9\u30FC\u30B9\u304B\u3089\u8FFD\u52A0\u3055\u308C\u305F\u30A8\u30F3\u30C8\u30EA\u306F\u3042\u308A\u307E\u305B\u3093"},
        {"Alias.name.alias", "\u5225\u540D: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "\u4F5C\u6210\u65E5: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0},{1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "\u30A8\u30F3\u30C8\u30EA\u30FB\u30BF\u30A4\u30D7: {0}"},
        {"Certificate.chain.length.", "\u8A3C\u660E\u66F8\u30C1\u30A7\u30FC\u30F3\u306E\u9577\u3055: "},
        {"Certificate.i.1.", "\u8A3C\u660E\u66F8[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "\u8A3C\u660E\u66F8\u306E\u30D5\u30A3\u30F3\u30AC\u30D7\u30EA\u30F3\u30C8(SHA-256): "},
        {"Keystore.type.", "\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30BF\u30A4\u30D7: "},
        {"Keystore.provider.", "\u30AD\u30FC\u30B9\u30C8\u30A2\u30FB\u30D7\u30ED\u30D0\u30A4\u30C0: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u306F{0,number,integer}\u30A8\u30F3\u30C8\u30EA\u304C\u542B\u307E\u308C\u307E\u3059"},
        {"Your.keystore.contains.keyStore.size.entries",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u306F{0,number,integer}\u30A8\u30F3\u30C8\u30EA\u304C\u542B\u307E\u308C\u307E\u3059"},
        {"Failed.to.parse.input", "\u5165\u529B\u306E\u69CB\u6587\u89E3\u6790\u306B\u5931\u6557\u3057\u307E\u3057\u305F"},
        {"Empty.input", "\u5165\u529B\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"Not.X.509.certificate", "X.509\u8A3C\u660E\u66F8\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},
        {"alias.has.no.public.key", "{0}\u306B\u306F\u516C\u958B\u30AD\u30FC\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"alias.has.no.X.509.certificate", "{0}\u306B\u306FX.509\u8A3C\u660E\u66F8\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"New.certificate.self.signed.", "\u65B0\u3057\u3044\u8A3C\u660E\u66F8(\u81EA\u5DF1\u7F72\u540D\u578B):"},
        {"Reply.has.no.certificates", "\u5FDC\u7B54\u306B\u306F\u8A3C\u660E\u66F8\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "\u8A3C\u660E\u66F8\u306F\u30A4\u30F3\u30DD\u30FC\u30C8\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002\u5225\u540D<{0}>\u306F\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Input.not.an.X.509.certificate", "\u5165\u529B\u306FX.509\u8A3C\u660E\u66F8\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "\u8A3C\u660E\u66F8\u306F\u3001\u5225\u540D<{0}>\u306E\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Do.you.still.want.to.add.it.no.",
                "\u8FFD\u52A0\u3057\u307E\u3059\u304B\u3002[\u3044\u3044\u3048]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "\u8A3C\u660E\u66F8\u306F\u3001\u5225\u540D<{0}>\u306E\u30B7\u30B9\u30C6\u30E0\u898F\u6A21\u306ECA\u30AD\u30FC\u30B9\u30C8\u30A2\u5185\u306B\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u8FFD\u52A0\u3057\u307E\u3059\u304B\u3002 [\u3044\u3044\u3048]:  "},
        {"Trust.this.certificate.no.", "\u3053\u306E\u8A3C\u660E\u66F8\u3092\u4FE1\u983C\u3057\u307E\u3059\u304B\u3002 [\u3044\u3044\u3048]:  "},
        {"New.prompt.", "\u65B0\u898F{0}: "},
        {"Passwords.must.differ", "\u30D1\u30B9\u30EF\u30FC\u30C9\u306F\u7570\u306A\u3063\u3066\u3044\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},
        {"Re.enter.new.prompt.", "\u65B0\u898F{0}\u3092\u518D\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044: "},
        {"Re.enter.password.", "\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u518D\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044: "},
        {"Re.enter.new.password.", "\u65B0\u898F\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u518D\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044: "},
        {"They.don.t.match.Try.again", "\u4E00\u81F4\u3057\u307E\u305B\u3093\u3002\u3082\u3046\u4E00\u5EA6\u5B9F\u884C\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Enter.prompt.alias.name.", "{0}\u306E\u5225\u540D\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "\u65B0\u3057\u3044\u5225\u540D\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044\t(\u3053\u306E\u30A8\u30F3\u30C8\u30EA\u306E\u30A4\u30F3\u30DD\u30FC\u30C8\u3092\u53D6\u308A\u6D88\u3059\u5834\u5408\u306FRETURN\u3092\u62BC\u3057\u3066\u304F\u3060\u3055\u3044):  "},
        {"Enter.alias.name.", "\u5225\u540D\u3092\u5165\u529B\u3057\u3066\u304F\u3060\u3055\u3044:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(<{0}>\u3068\u540C\u3058\u5834\u5408\u306FRETURN\u3092\u62BC\u3057\u3066\u304F\u3060\u3055\u3044)"},
        {"What.is.your.first.and.last.name.",
                "\u59D3\u540D\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"What.is.the.name.of.your.organizational.unit.",
                "\u7D44\u7E54\u5358\u4F4D\u540D\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"What.is.the.name.of.your.organization.",
                "\u7D44\u7E54\u540D\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "\u90FD\u5E02\u540D\u307E\u305F\u306F\u5730\u57DF\u540D\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"What.is.the.name.of.your.State.or.Province.",
                "\u90FD\u9053\u5E9C\u770C\u540D\u307E\u305F\u306F\u5DDE\u540D\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "\u3053\u306E\u5358\u4F4D\u306B\u8A72\u5F53\u3059\u308B2\u6587\u5B57\u306E\u56FD\u30B3\u30FC\u30C9\u306F\u4F55\u3067\u3059\u304B\u3002"},
        {"Is.name.correct.", "{0}\u3067\u3088\u308D\u3057\u3044\u3067\u3059\u304B\u3002"},
        {"no", "\u3044\u3044\u3048"},
        {"yes", "\u306F\u3044"},
        {"y", "y"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "\u5225\u540D<{0}>\u306B\u306F\u30AD\u30FC\u304C\u3042\u308A\u307E\u305B\u3093"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "\u5225\u540D<{0}>\u304C\u53C2\u7167\u3057\u3066\u3044\u308B\u30A8\u30F3\u30C8\u30EA\u30FB\u30BF\u30A4\u30D7\u306F\u79D8\u5BC6\u30AD\u30FC\u30FB\u30A8\u30F3\u30C8\u30EA\u3067\u306F\u3042\u308A\u307E\u305B\u3093\u3002-keyclone\u30B3\u30DE\u30F3\u30C9\u306F\u79D8\u5BC6\u30AD\u30FC\u30FB\u30A8\u30F3\u30C8\u30EA\u306E\u30AF\u30ED\u30FC\u30F3\u4F5C\u6210\u306E\u307F\u3092\u30B5\u30DD\u30FC\u30C8\u3057\u307E\u3059"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "\u7F72\u540D\u8005\u756A\u53F7%d:"},
        {"Timestamp.", "\u30BF\u30A4\u30E0\u30B9\u30BF\u30F3\u30D7:"},
        {"Signature.", "\u7F72\u540D:"},
        {"Certificate.owner.", "\u8A3C\u660E\u66F8\u306E\u6240\u6709\u8005: "},
        {"Not.a.signed.jar.file", "\u7F72\u540D\u4ED8\u304DJAR\u30D5\u30A1\u30A4\u30EB\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},
        {"No.certificate.from.the.SSL.server",
                "SSL\u30B5\u30FC\u30D0\u30FC\u304B\u3089\u306E\u8A3C\u660E\u66F8\u304C\u3042\u308A\u307E\u305B\u3093"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "*\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u4FDD\u5B58\u3055\u308C\u305F\u60C5\u5831\u306E\u6574\u5408\u6027\u306F*\n*\u691C\u8A3C\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u6574\u5408\u6027\u3092\u691C\u8A3C\u3059\u308B\u306B\u306F*\n*\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002*"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "*\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306B\u4FDD\u5B58\u3055\u308C\u305F\u60C5\u5831\u306E\u6574\u5408\u6027\u306F*\n*\u691C\u8A3C\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u6574\u5408\u6027\u3092\u691C\u8A3C\u3059\u308B\u306B\u306F*\n*\u30BD\u30FC\u30B9\u30FB\u30AD\u30FC\u30B9\u30C8\u30A2\u306E\u30D1\u30B9\u30EF\u30FC\u30C9\u3092\u5165\u529B\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002*"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "\u8A3C\u660E\u66F8\u5FDC\u7B54\u306B\u306F\u3001<{0}>\u306E\u516C\u958B\u30AD\u30FC\u306F\u542B\u307E\u308C\u307E\u305B\u3093"},
        {"Incomplete.certificate.chain.in.reply",
                "\u5FDC\u7B54\u3057\u305F\u8A3C\u660E\u66F8\u30C1\u30A7\u30FC\u30F3\u306F\u4E0D\u5B8C\u5168\u3067\u3059"},
        {"Top.level.certificate.in.reply.",
                "\u5FDC\u7B54\u3057\u305F\u30C8\u30C3\u30D7\u30EC\u30D9\u30EB\u306E\u8A3C\u660E\u66F8:\n"},
        {".is.not.trusted.", "... \u306F\u4FE1\u983C\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002 "},
        {"Install.reply.anyway.no.", "\u5FDC\u7B54\u3092\u30A4\u30F3\u30B9\u30C8\u30FC\u30EB\u3057\u307E\u3059\u304B\u3002[\u3044\u3044\u3048]:  "},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "\u5FDC\u7B54\u3057\u305F\u516C\u958B\u30AD\u30FC\u3068\u30AD\u30FC\u30B9\u30C8\u30A2\u304C\u4E00\u81F4\u3057\u307E\u305B\u3093"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "\u8A3C\u660E\u66F8\u5FDC\u7B54\u3068\u30AD\u30FC\u30B9\u30C8\u30A2\u5185\u306E\u8A3C\u660E\u66F8\u304C\u540C\u3058\u3067\u3059"},
        {"Failed.to.establish.chain.from.reply",
                "\u5FDC\u7B54\u304B\u3089\u9023\u9396\u3092\u78BA\u7ACB\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},
        {"n", "n"},
        {"Wrong.answer.try.again", "\u5FDC\u7B54\u304C\u9593\u9055\u3063\u3066\u3044\u307E\u3059\u3002\u3082\u3046\u4E00\u5EA6\u5B9F\u884C\u3057\u3066\u304F\u3060\u3055\u3044"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "\u79D8\u5BC6\u30AD\u30FC\u306F\u751F\u6210\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002\u5225\u540D<{0}>\u306F\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},
        {"Please.provide.keysize.for.secret.key.generation",
                "\u79D8\u5BC6\u30AD\u30FC\u306E\u751F\u6210\u6642\u306B\u306F -keysize\u3092\u6307\u5B9A\u3057\u3066\u304F\u3060\u3055\u3044"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "\u8B66\u544A: \u691C\u8A3C\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002-keystore\u304C\u6B63\u3057\u3044\u3053\u3068\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
        {"warning.not.verified.make.sure.keystore.is.correct.or.specify.trustcacerts",
            "\u8B66\u544A: \u691C\u8A3C\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002-keystore\u304C\u6B63\u3057\u3044\u3053\u3068\u3092\u78BA\u8A8D\u3059\u308B\u304B\u3001-trustcacerts\u3092\u6307\u5B9A\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

        {"Extensions.", "\u62E1\u5F35: "},
        {".Empty.value.", "(\u7A7A\u306E\u5024)"},
        {"Extension.Request.", "\u62E1\u5F35\u30EA\u30AF\u30A8\u30B9\u30C8:"},
        {"Unknown.keyUsage.type.", "\u4E0D\u660E\u306AkeyUsage\u30BF\u30A4\u30D7: "},
        {"Unknown.extendedkeyUsage.type.", "\u4E0D\u660E\u306AextendedkeyUsage\u30BF\u30A4\u30D7: "},
        {"Unknown.AccessDescription.type.", "\u4E0D\u660E\u306AAccessDescription\u30BF\u30A4\u30D7: "},
        {"Unrecognized.GeneralName.type.", "\u8A8D\u8B58\u3055\u308C\u306A\u3044GeneralName\u30BF\u30A4\u30D7: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "\u3053\u306E\u62E1\u5F35\u306F\u30AF\u30EA\u30C6\u30A3\u30AB\u30EB\u3068\u3057\u3066\u30DE\u30FC\u30AF\u4ED8\u3051\u3067\u304D\u307E\u305B\u3093\u3002 "},
        {"Odd.number.of.hex.digits.found.", "\u5947\u6570\u306E16\u9032\u6570\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F: "},
        {"Unknown.extension.type.", "\u4E0D\u660E\u306A\u62E1\u5F35\u30BF\u30A4\u30D7: "},
        {"command.{0}.is.ambiguous.", "\u30B3\u30DE\u30F3\u30C9{0}\u306F\u3042\u3044\u307E\u3044\u3067\u3059:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8"},
        {"the.issuer", "\u767A\u884C\u8005"},
        {"the.generated.certificate", "\u751F\u6210\u3055\u308C\u305F\u8A3C\u660E\u66F8"},
        {"the.generated.crl", "\u751F\u6210\u3055\u308C\u305FCRL"},
        {"the.generated.certificate.request", "\u751F\u6210\u3055\u308C\u305F\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8"},
        {"the.certificate", "\u8A3C\u660E\u66F8"},
        {"the.crl", "CRL"},
        {"the.tsa.certificate", "TSA\u8A3C\u660E\u66F8"},
        {"the.input", "\u5165\u529B"},
        {"reply", "\u5FDC\u7B54"},
        {"one.in.many", "%1$s #%2$d / %3$d"},
        {"alias.in.cacerts", "cacerts\u5185\u306E\u767A\u884C\u8005<%s>"},
        {"alias.in.keystore", "\u767A\u884C\u8005<%s>"},
        {"with.weak", "%s (\u5F31)"},
        {"with.disabled", "%s (\u7121\u52B9)"},
        {"key.bit", "%1$d\u30D3\u30C3\u30C8%2$s\u30AD\u30FC"},
        {"key.bit.weak", "%1$d\u30D3\u30C3\u30C8%2$s\u30AD\u30FC(\u5F31)"},
        {"key.bit.disabled", "%1$d-bit %2$s key (\u7121\u52B9)"},
        {"unknown.size.1", "\u4E0D\u660E\u306A\u30B5\u30A4\u30BA\u306E%s\u30AD\u30FC"},
        {".PATTERN.printX509Cert.with.weak",
                "\u6240\u6709\u8005: {0}\n\u767A\u884C\u8005: {1}\n\u30B7\u30EA\u30A2\u30EB\u756A\u53F7: {2}\n\u6709\u52B9\u671F\u9593\u306E\u958B\u59CB\u65E5: {3}\u7D42\u4E86\u65E5: {4}\n\u8A3C\u660E\u66F8\u306E\u30D5\u30A3\u30F3\u30AC\u30D7\u30EA\u30F3\u30C8:\n\t SHA1: {5}\n\t SHA256: {6}\n\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u540D: {7}\n\u30B5\u30D6\u30B8\u30A7\u30AF\u30C8\u516C\u958B\u30AD\u30FC\u30FB\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0: {8}\n\u30D0\u30FC\u30B8\u30E7\u30F3: {9}"},
        {"PKCS.10.with.weak",
                "PKCS #10\u8A3C\u660E\u66F8\u30EA\u30AF\u30A8\u30B9\u30C8(\u30D0\u30FC\u30B8\u30E7\u30F31.0)\n\u30B5\u30D6\u30B8\u30A7\u30AF\u30C8: %1$s\n\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8: %2$s\n\u516C\u958B\u30AD\u30FC: %3$s\n\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0: %4$s\n"},
        {"verified.by.s.in.s.weak", "%2$s\u5185\u306E%1$s\u306B\u3088\u308A%3$s\u3067\u691C\u8A3C\u3055\u308C\u307E\u3057\u305F"},
        {"whose.sigalg.disabled", "%1$s\u306F%2$s\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u3092\u4F7F\u7528\u3057\u3066\u304A\u308A\u3001\u3053\u308C\u306F\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30EA\u30B9\u30AF\u3068\u307F\u306A\u3055\u308C\u3001\u7121\u52B9\u5316\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},
        {"whose.sigalg.weak", "%1$s\u306F%2$s\u7F72\u540D\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u3092\u4F7F\u7528\u3057\u3066\u304A\u308A\u3001\u3053\u308C\u306F\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30EA\u30B9\u30AF\u3068\u307F\u306A\u3055\u308C\u307E\u3059\u3002\u3053\u306E\u30A2\u30EB\u30B4\u30EA\u30BA\u30E0\u306F\u5C06\u6765\u306E\u66F4\u65B0\u3067\u7121\u52B9\u5316\u3055\u308C\u307E\u3059\u3002"},
        {"whose.key.disabled", "%1$s\u306F%2$s\u3092\u4F7F\u7528\u3057\u3066\u304A\u308A\u3001\u3053\u308C\u306F\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30EA\u30B9\u30AF\u3068\u307F\u306A\u3055\u308C\u3001\u7121\u52B9\u5316\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},
        {"whose.key.weak", "%1$s\u306F%2$s\u3092\u4F7F\u7528\u3057\u3066\u304A\u308A\u3001\u3053\u308C\u306F\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30EA\u30B9\u30AF\u3068\u307F\u306A\u3055\u308C\u307E\u3059\u3002\u3053\u306E\u30AD\u30FC\u30FB\u30B5\u30A4\u30BA\u306F\u5C06\u6765\u306E\u66F4\u65B0\u3067\u7121\u52B9\u5316\u3055\u308C\u307E\u3059\u3002"},
        {"jks.storetype.warning", "%1$s\u30AD\u30FC\u30B9\u30C8\u30A2\u306F\u72EC\u81EA\u306E\u5F62\u5F0F\u3092\u4F7F\u7528\u3057\u3066\u3044\u307E\u3059\u3002\"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"\u3092\u4F7F\u7528\u3059\u308B\u696D\u754C\u6A19\u6E96\u306E\u5F62\u5F0F\u3067\u3042\u308BPKCS12\u306B\u79FB\u884C\u3059\u308B\u3053\u3068\u3092\u304A\u85A6\u3081\u3057\u307E\u3059\u3002"},
        {"migrate.keystore.warning", "\"%1$s\"\u304C%4$s\u306B\u79FB\u884C\u3055\u308C\u307E\u3057\u305F\u3002%2$s\u30AD\u30FC\u30B9\u30C8\u30A2\u306F\"%3$s\"\u3068\u3057\u3066\u30D0\u30C3\u30AF\u30A2\u30C3\u30D7\u3055\u308C\u307E\u3059\u3002"},
        {"backup.keystore.warning", "\u5143\u306E\u30AD\u30FC\u30B9\u30C8\u30A2\"%1$s\"\u306F\"%3$s\"\u3068\u3057\u3066\u30D0\u30C3\u30AF\u30A2\u30C3\u30D7\u3055\u308C\u307E\u3059..."},
        {"importing.keystore.status", "\u30AD\u30FC\u30B9\u30C8\u30A2%1$s\u3092%2$s\u306B\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u3066\u3044\u307E\u3059..."},
        {"keyalg.option.missing.error", "-keyalg\u30AA\u30D7\u30B7\u30E7\u30F3\u3092\u6307\u5B9A\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        {"showinfo.no.option", "-showinfo\u306E\u30AA\u30D7\u30B7\u30E7\u30F3\u304C\u3042\u308A\u307E\u305B\u3093\u3002\"keytool -showinfo -tls\"\u3092\u8A66\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
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
