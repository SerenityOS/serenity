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
public class Resources_zh_CN extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "\u9009\u9879:"},
        {"option.1.set.twice", "\u591A\u6B21\u6307\u5B9A\u4E86 %s \u9009\u9879\u3002\u9664\u6700\u540E\u4E00\u4E2A\u4E4B\u5916, \u5176\u4F59\u7684\u5C06\u5168\u90E8\u5FFD\u7565\u3002"},
        {"multiple.commands.1.2", "\u53EA\u5141\u8BB8\u4E00\u4E2A\u547D\u4EE4: \u540C\u65F6\u6307\u5B9A\u4E86 %1$s \u548C %2$s\u3002"},
        {"Use.keytool.help.for.all.available.commands",
                 "\u4F7F\u7528 \"keytool -?, -h, or --help\" \u53EF\u8F93\u51FA\u6B64\u5E2E\u52A9\u6D88\u606F"},
        {"Key.and.Certificate.Management.Tool",
                 "\u5BC6\u94A5\u548C\u8BC1\u4E66\u7BA1\u7406\u5DE5\u5177"},
        {"Commands.", "\u547D\u4EE4:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "\u4F7F\u7528 \"keytool -command_name --help\" \u53EF\u83B7\u53D6 command_name \u7684\u7528\u6CD5\u3002\n\u4F7F\u7528 -conf <url> \u9009\u9879\u53EF\u6307\u5B9A\u9884\u914D\u7F6E\u7684\u9009\u9879\u6587\u4EF6\u3002"},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "\u751F\u6210\u8BC1\u4E66\u8BF7\u6C42"}, //-certreq
        {"Changes.an.entry.s.alias",
                "\u66F4\u6539\u6761\u76EE\u7684\u522B\u540D"}, //-changealias
        {"Deletes.an.entry",
                "\u5220\u9664\u6761\u76EE"}, //-delete
        {"Exports.certificate",
                "\u5BFC\u51FA\u8BC1\u4E66"}, //-exportcert
        {"Generates.a.key.pair",
                "\u751F\u6210\u5BC6\u94A5\u5BF9"}, //-genkeypair
        {"Generates.a.secret.key",
                "\u751F\u6210\u5BC6\u94A5"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "\u6839\u636E\u8BC1\u4E66\u8BF7\u6C42\u751F\u6210\u8BC1\u4E66"}, //-gencert
        {"Generates.CRL", "\u751F\u6210 CRL"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "\u5DF2\u751F\u6210{0}\u5BC6\u94A5"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "\u5DF2\u751F\u6210 {0} \u4F4D{1}\u5BC6\u94A5"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "\u4ECE JDK 1.1.x \u6837\u5F0F\u7684\u8EAB\u4EFD\u6570\u636E\u5E93\u5BFC\u5165\u6761\u76EE"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "\u5BFC\u5165\u8BC1\u4E66\u6216\u8BC1\u4E66\u94FE"}, //-importcert
        {"Imports.a.password",
                "\u5BFC\u5165\u53E3\u4EE4"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "\u4ECE\u5176\u4ED6\u5BC6\u94A5\u5E93\u5BFC\u5165\u4E00\u4E2A\u6216\u6240\u6709\u6761\u76EE"}, //-importkeystore
        {"Clones.a.key.entry",
                "\u514B\u9686\u5BC6\u94A5\u6761\u76EE"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "\u66F4\u6539\u6761\u76EE\u7684\u5BC6\u94A5\u53E3\u4EE4"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "\u5217\u51FA\u5BC6\u94A5\u5E93\u4E2D\u7684\u6761\u76EE"}, //-list
        {"Prints.the.content.of.a.certificate",
                "\u6253\u5370\u8BC1\u4E66\u5185\u5BB9"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "\u6253\u5370\u8BC1\u4E66\u8BF7\u6C42\u7684\u5185\u5BB9"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "\u6253\u5370 CRL \u6587\u4EF6\u7684\u5185\u5BB9"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "\u751F\u6210\u81EA\u7B7E\u540D\u8BC1\u4E66"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "\u66F4\u6539\u5BC6\u94A5\u5E93\u7684\u5B58\u50A8\u53E3\u4EE4"}, //-storepasswd
        {"showinfo.command.help", "\u663E\u793A\u5B89\u5168\u76F8\u5173\u4FE1\u606F"},

        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "\u8981\u5904\u7406\u7684\u6761\u76EE\u7684\u522B\u540D"}, //-alias
        {"groupname.option.help",
                "\u7EC4\u540D\u3002\u4F8B\u5982\uFF0C\u692D\u5706\u66F2\u7EBF\u540D\u79F0\u3002"}, //-groupname
        {"destination.alias",
                "\u76EE\u6807\u522B\u540D"}, //-destalias
        {"destination.key.password",
                "\u76EE\u6807\u5BC6\u94A5\u53E3\u4EE4"}, //-destkeypass
        {"destination.keystore.name",
                "\u76EE\u6807\u5BC6\u94A5\u5E93\u540D\u79F0"}, //-destkeystore
        {"destination.keystore.password.protected",
                "\u53D7\u4FDD\u62A4\u7684\u76EE\u6807\u5BC6\u94A5\u5E93\u53E3\u4EE4"}, //-destprotected
        {"destination.keystore.provider.name",
                "\u76EE\u6807\u5BC6\u94A5\u5E93\u63D0\u4F9B\u65B9\u540D\u79F0"}, //-destprovidername
        {"destination.keystore.password",
                "\u76EE\u6807\u5BC6\u94A5\u5E93\u53E3\u4EE4"}, //-deststorepass
        {"destination.keystore.type",
                "\u76EE\u6807\u5BC6\u94A5\u5E93\u7C7B\u578B"}, //-deststoretype
        {"distinguished.name",
                "\u552F\u4E00\u5224\u522B\u540D"}, //-dname
        {"X.509.extension",
                "X.509 \u6269\u5C55"}, //-ext
        {"output.file.name",
                "\u8F93\u51FA\u6587\u4EF6\u540D"}, //-file and -outfile
        {"input.file.name",
                "\u8F93\u5165\u6587\u4EF6\u540D"}, //-file and -infile
        {"key.algorithm.name",
                "\u5BC6\u94A5\u7B97\u6CD5\u540D\u79F0"}, //-keyalg
        {"key.password",
                "\u5BC6\u94A5\u53E3\u4EE4"}, //-keypass
        {"key.bit.size",
                "\u5BC6\u94A5\u4F4D\u5927\u5C0F"}, //-keysize
        {"keystore.name",
                "\u5BC6\u94A5\u5E93\u540D\u79F0"}, //-keystore
        {"access.the.cacerts.keystore",
                "\u8BBF\u95EE cacerts \u5BC6\u94A5\u5E93"}, // -cacerts
        {"warning.cacerts.option",
                "\u8B66\u544A: \u4F7F\u7528 -cacerts \u9009\u9879\u8BBF\u95EE cacerts \u5BC6\u94A5\u5E93"},
        {"new.password",
                "\u65B0\u53E3\u4EE4"}, //-new
        {"do.not.prompt",
                "\u4E0D\u63D0\u793A"}, //-noprompt
        {"password.through.protected.mechanism",
                "\u901A\u8FC7\u53D7\u4FDD\u62A4\u7684\u673A\u5236\u7684\u53E3\u4EE4"}, //-protected
        {"tls.option.help", "\u663E\u793A TLS \u914D\u7F6E\u4FE1\u606F"},

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "\u6309\u540D\u79F0 (\u4F8B\u5982 SunPKCS11) \u6DFB\u52A0\u5B89\u5168\u63D0\u4F9B\u65B9\n\u914D\u7F6E -addprovider \u7684\u53C2\u6570"}, //-addprovider
        {"provider.class.option",
                "\u6309\u5168\u9650\u5B9A\u7C7B\u540D\u6DFB\u52A0\u5B89\u5168\u63D0\u4F9B\u65B9\n\u914D\u7F6E -providerclass \u7684\u53C2\u6570"}, //-providerclass

        {"provider.name",
                "\u63D0\u4F9B\u65B9\u540D\u79F0"}, //-providername
        {"provider.classpath",
                "\u63D0\u4F9B\u65B9\u7C7B\u8DEF\u5F84"}, //-providerpath
        {"output.in.RFC.style",
                "\u4EE5 RFC \u6837\u5F0F\u8F93\u51FA"}, //-rfc
        {"signature.algorithm.name",
                "\u7B7E\u540D\u7B97\u6CD5\u540D\u79F0"}, //-sigalg
        {"source.alias",
                "\u6E90\u522B\u540D"}, //-srcalias
        {"source.key.password",
                "\u6E90\u5BC6\u94A5\u53E3\u4EE4"}, //-srckeypass
        {"source.keystore.name",
                "\u6E90\u5BC6\u94A5\u5E93\u540D\u79F0"}, //-srckeystore
        {"source.keystore.password.protected",
                "\u53D7\u4FDD\u62A4\u7684\u6E90\u5BC6\u94A5\u5E93\u53E3\u4EE4"}, //-srcprotected
        {"source.keystore.provider.name",
                "\u6E90\u5BC6\u94A5\u5E93\u63D0\u4F9B\u65B9\u540D\u79F0"}, //-srcprovidername
        {"source.keystore.password",
                "\u6E90\u5BC6\u94A5\u5E93\u53E3\u4EE4"}, //-srcstorepass
        {"source.keystore.type",
                "\u6E90\u5BC6\u94A5\u5E93\u7C7B\u578B"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "SSL \u670D\u52A1\u5668\u4E3B\u673A\u548C\u7AEF\u53E3"}, //-sslserver
        {"signed.jar.file",
                "\u5DF2\u7B7E\u540D\u7684 jar \u6587\u4EF6"}, //=jarfile
        {"certificate.validity.start.date.time",
                "\u8BC1\u4E66\u6709\u6548\u671F\u5F00\u59CB\u65E5\u671F/\u65F6\u95F4"}, //-startdate
        {"keystore.password",
                "\u5BC6\u94A5\u5E93\u53E3\u4EE4"}, //-storepass
        {"keystore.type",
                "\u5BC6\u94A5\u5E93\u7C7B\u578B"}, //-storetype
        {"trust.certificates.from.cacerts",
                "\u4FE1\u4EFB\u6765\u81EA cacerts \u7684\u8BC1\u4E66"}, //-trustcacerts
        {"verbose.output",
                "\u8BE6\u7EC6\u8F93\u51FA"}, //-v
        {"validity.number.of.days",
                "\u6709\u6548\u5929\u6570"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "\u8981\u64A4\u9500\u7684\u8BC1\u4E66\u7684\u5E8F\u5217 ID"}, //-id
        // keytool: Running part
        {"keytool.error.", "keytool \u9519\u8BEF: "},
        {"Illegal.option.", "\u975E\u6CD5\u9009\u9879:  "},
        {"Illegal.value.", "\u975E\u6CD5\u503C: "},
        {"Unknown.password.type.", "\u672A\u77E5\u53E3\u4EE4\u7C7B\u578B: "},
        {"Cannot.find.environment.variable.",
                "\u627E\u4E0D\u5230\u73AF\u5883\u53D8\u91CF: "},
        {"Cannot.find.file.", "\u627E\u4E0D\u5230\u6587\u4EF6: "},
        {"Command.option.flag.needs.an.argument.", "\u547D\u4EE4\u9009\u9879{0}\u9700\u8981\u4E00\u4E2A\u53C2\u6570\u3002"},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "\u8B66\u544A: PKCS12 \u5BC6\u94A5\u5E93\u4E0D\u652F\u6301\u5176\u4ED6\u5B58\u50A8\u548C\u5BC6\u94A5\u53E3\u4EE4\u3002\u6B63\u5728\u5FFD\u7565\u7528\u6237\u6307\u5B9A\u7684{0}\u503C\u3002"},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "-keystore \u6216 -storetype \u9009\u9879\u4E0D\u80FD\u4E0E -cacerts \u9009\u9879\u4E00\u8D77\u4F7F\u7528"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u4E3A {0}, \u5219 -keystore \u5FC5\u987B\u4E3A NONE"},
        {"Too.many.retries.program.terminated",
                 "\u91CD\u8BD5\u6B21\u6570\u8FC7\u591A, \u7A0B\u5E8F\u5DF2\u7EC8\u6B62"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u4E3A {0}, \u5219\u4E0D\u652F\u6301 -storepasswd \u548C -keypasswd \u547D\u4EE4"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "\u5982\u679C -storetype \u4E3A PKCS12, \u5219\u4E0D\u652F\u6301 -keypasswd \u547D\u4EE4"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u4E3A {0}, \u5219\u4E0D\u80FD\u6307\u5B9A -keypass \u548C -new"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "\u5982\u679C\u6307\u5B9A\u4E86 -protected, \u5219\u4E0D\u80FD\u6307\u5B9A -storepass, -keypass \u548C -new"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\u5982\u679C\u6307\u5B9A\u4E86 -srcprotected, \u5219\u4E0D\u80FD\u6307\u5B9A -srcstorepass \u548C -srckeypass"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "\u5982\u679C\u5BC6\u94A5\u5E93\u672A\u53D7\u53E3\u4EE4\u4FDD\u62A4, \u5219\u4E0D\u80FD\u6307\u5B9A -storepass, -keypass \u548C -new"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\u5982\u679C\u6E90\u5BC6\u94A5\u5E93\u672A\u53D7\u53E3\u4EE4\u4FDD\u62A4, \u5219\u4E0D\u80FD\u6307\u5B9A -srcstorepass \u548C -srckeypass"},
        {"Illegal.startdate.value", "\u975E\u6CD5\u5F00\u59CB\u65E5\u671F\u503C"},
        {"Validity.must.be.greater.than.zero",
                "\u6709\u6548\u6027\u5FC5\u987B\u5927\u4E8E\u96F6"},
        {"provclass.not.a.provider", "%s\u4E0D\u662F\u63D0\u4F9B\u65B9"},
        {"provider.name.not.found", "\u672A\u627E\u5230\u540D\u4E3A \"%s\" \u7684\u63D0\u4F9B\u65B9"},
        {"provider.class.not.found", "\u672A\u627E\u5230\u63D0\u4F9B\u65B9 \"%s\""},
        {"Usage.error.no.command.provided", "\u7528\u6CD5\u9519\u8BEF: \u6CA1\u6709\u63D0\u4F9B\u547D\u4EE4"},
        {"Source.keystore.file.exists.but.is.empty.", "\u6E90\u5BC6\u94A5\u5E93\u6587\u4EF6\u5B58\u5728, \u4F46\u4E3A\u7A7A: "},
        {"Please.specify.srckeystore", "\u8BF7\u6307\u5B9A -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "\u4E0D\u80FD\u4F7F\u7528 'list' \u547D\u4EE4\u6765\u6307\u5B9A -v \u53CA -rfc"},
        {"Key.password.must.be.at.least.6.characters",
                "\u5BC6\u94A5\u53E3\u4EE4\u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"New.password.must.be.at.least.6.characters",
                "\u65B0\u53E3\u4EE4\u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"Keystore.file.exists.but.is.empty.",
                "\u5BC6\u94A5\u5E93\u6587\u4EF6\u5B58\u5728, \u4F46\u4E3A\u7A7A: "},
        {"Keystore.file.does.not.exist.",
                "\u5BC6\u94A5\u5E93\u6587\u4EF6\u4E0D\u5B58\u5728: "},
        {"Must.specify.destination.alias", "\u5FC5\u987B\u6307\u5B9A\u76EE\u6807\u522B\u540D"},
        {"Must.specify.alias", "\u5FC5\u987B\u6307\u5B9A\u522B\u540D"},
        {"Keystore.password.must.be.at.least.6.characters",
                "\u5BC6\u94A5\u5E93\u53E3\u4EE4\u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"Enter.the.password.to.be.stored.",
                "\u8F93\u5165\u8981\u5B58\u50A8\u7684\u53E3\u4EE4:  "},
        {"Enter.keystore.password.", "\u8F93\u5165\u5BC6\u94A5\u5E93\u53E3\u4EE4:  "},
        {"Enter.source.keystore.password.", "\u8F93\u5165\u6E90\u5BC6\u94A5\u5E93\u53E3\u4EE4:  "},
        {"Enter.destination.keystore.password.", "\u8F93\u5165\u76EE\u6807\u5BC6\u94A5\u5E93\u53E3\u4EE4:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "\u5BC6\u94A5\u5E93\u53E3\u4EE4\u592A\u77ED - \u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"Unknown.Entry.Type", "\u672A\u77E5\u6761\u76EE\u7C7B\u578B"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "\u5DF2\u6210\u529F\u5BFC\u5165\u522B\u540D {0} \u7684\u6761\u76EE\u3002"},
        {"Entry.for.alias.alias.not.imported.", "\u672A\u5BFC\u5165\u522B\u540D {0} \u7684\u6761\u76EE\u3002"},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "\u5BFC\u5165\u522B\u540D {0} \u7684\u6761\u76EE\u65F6\u51FA\u73B0\u95EE\u9898: {1}\u3002\n\u672A\u5BFC\u5165\u522B\u540D {0} \u7684\u6761\u76EE\u3002"},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "\u5DF2\u5B8C\u6210\u5BFC\u5165\u547D\u4EE4: {0} \u4E2A\u6761\u76EE\u6210\u529F\u5BFC\u5165, {1} \u4E2A\u6761\u76EE\u5931\u8D25\u6216\u53D6\u6D88"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "\u8B66\u544A: \u6B63\u5728\u8986\u76D6\u76EE\u6807\u5BC6\u94A5\u5E93\u4E2D\u7684\u73B0\u6709\u522B\u540D {0}"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "\u5B58\u5728\u73B0\u6709\u6761\u76EE\u522B\u540D {0}, \u662F\u5426\u8986\u76D6? [\u5426]:  "},
        {"Too.many.failures.try.later", "\u6545\u969C\u592A\u591A - \u8BF7\u7A0D\u540E\u518D\u8BD5"},
        {"Certification.request.stored.in.file.filename.",
                "\u5B58\u50A8\u5728\u6587\u4EF6 <{0}> \u4E2D\u7684\u8BA4\u8BC1\u8BF7\u6C42"},
        {"Submit.this.to.your.CA", "\u5C06\u6B64\u63D0\u4EA4\u7ED9\u60A8\u7684 CA"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "\u5982\u679C\u6CA1\u6709\u6307\u5B9A\u522B\u540D, \u5219\u4E0D\u80FD\u6307\u5B9A\u76EE\u6807\u522B\u540D\u548C\u6E90\u5BC6\u94A5\u5E93\u53E3\u4EE4"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "\u76EE\u6807 pkcs12 \u5BC6\u94A5\u5E93\u5177\u6709\u4E0D\u540C\u7684 storepass \u548C keypass\u3002\u8BF7\u5728\u6307\u5B9A\u4E86 -destkeypass \u65F6\u91CD\u8BD5\u3002"},
        {"Certificate.stored.in.file.filename.",
                "\u5B58\u50A8\u5728\u6587\u4EF6 <{0}> \u4E2D\u7684\u8BC1\u4E66"},
        {"Certificate.reply.was.installed.in.keystore",
                "\u8BC1\u4E66\u56DE\u590D\u5DF2\u5B89\u88C5\u5728\u5BC6\u94A5\u5E93\u4E2D"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "\u8BC1\u4E66\u56DE\u590D\u672A\u5B89\u88C5\u5728\u5BC6\u94A5\u5E93\u4E2D"},
        {"Certificate.was.added.to.keystore",
                "\u8BC1\u4E66\u5DF2\u6DFB\u52A0\u5230\u5BC6\u94A5\u5E93\u4E2D"},
        {"Certificate.was.not.added.to.keystore",
                "\u8BC1\u4E66\u672A\u6DFB\u52A0\u5230\u5BC6\u94A5\u5E93\u4E2D"},
        {".Storing.ksfname.", "[\u6B63\u5728\u5B58\u50A8{0}]"},
        {"alias.has.no.public.key.certificate.",
                "{0}\u6CA1\u6709\u516C\u5171\u5BC6\u94A5 (\u8BC1\u4E66)"},
        {"Cannot.derive.signature.algorithm",
                "\u65E0\u6CD5\u6D3E\u751F\u7B7E\u540D\u7B97\u6CD5"},
        {"Alias.alias.does.not.exist",
                "\u522B\u540D <{0}> \u4E0D\u5B58\u5728"},
        {"Alias.alias.has.no.certificate",
                "\u522B\u540D <{0}> \u6CA1\u6709\u8BC1\u4E66"},
        {"groupname.keysize.coexist",
                "\u65E0\u6CD5\u540C\u65F6\u6307\u5B9A -groupname \u548C -keysize"},
        {"deprecate.keysize.for.ec",
                "\u4E3A\u751F\u6210 EC \u5BC6\u94A5\u6307\u5B9A -keysize \u5DF2\u8FC7\u65F6\uFF0C\u8BF7\u6539\u4E3A\u4F7F\u7528 \"-groupname %s\"\u3002"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "\u672A\u751F\u6210\u5BC6\u94A5\u5BF9, \u522B\u540D <{0}> \u5DF2\u7ECF\u5B58\u5728"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "\u6B63\u5728\u4E3A\u4EE5\u4E0B\u5BF9\u8C61\u751F\u6210 {0} \u4F4D{1}\u5BC6\u94A5\u5BF9\u548C\u81EA\u7B7E\u540D\u8BC1\u4E66 ({2}) (\u6709\u6548\u671F\u4E3A {3} \u5929):\n\t {4}"},
        {"Enter.key.password.for.alias.", "\u8F93\u5165 <{0}> \u7684\u5BC6\u94A5\u53E3\u4EE4"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(\u5982\u679C\u548C\u5BC6\u94A5\u5E93\u53E3\u4EE4\u76F8\u540C, \u6309\u56DE\u8F66):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "\u5BC6\u94A5\u53E3\u4EE4\u592A\u77ED - \u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"Too.many.failures.key.not.added.to.keystore",
                "\u6545\u969C\u592A\u591A - \u5BC6\u94A5\u672A\u6DFB\u52A0\u5230\u5BC6\u94A5\u5E93\u4E2D"},
        {"Destination.alias.dest.already.exists",
                "\u76EE\u6807\u522B\u540D <{0}> \u5DF2\u7ECF\u5B58\u5728"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "\u53E3\u4EE4\u592A\u77ED - \u81F3\u5C11\u5FC5\u987B\u4E3A 6 \u4E2A\u5B57\u7B26"},
        {"Too.many.failures.Key.entry.not.cloned",
                "\u6545\u969C\u592A\u591A\u3002\u672A\u514B\u9686\u5BC6\u94A5\u6761\u76EE"},
        {"key.password.for.alias.", "<{0}> \u7684\u5BC6\u94A5\u53E3\u4EE4"},
        {"No.entries.from.identity.database.added",
                "\u672A\u4ECE\u8EAB\u4EFD\u6570\u636E\u5E93\u4E2D\u6DFB\u52A0\u4EFB\u4F55\u6761\u76EE"},
        {"Alias.name.alias", "\u522B\u540D: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "\u521B\u5EFA\u65E5\u671F: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "\u6761\u76EE\u7C7B\u578B: {0}"},
        {"Certificate.chain.length.", "\u8BC1\u4E66\u94FE\u957F\u5EA6: "},
        {"Certificate.i.1.", "\u8BC1\u4E66[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "\u8BC1\u4E66\u6307\u7EB9 (SHA-256): "},
        {"Keystore.type.", "\u5BC6\u94A5\u5E93\u7C7B\u578B: "},
        {"Keystore.provider.", "\u5BC6\u94A5\u5E93\u63D0\u4F9B\u65B9: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "\u60A8\u7684\u5BC6\u94A5\u5E93\u5305\u542B {0,number,integer} \u4E2A\u6761\u76EE"},
        {"Your.keystore.contains.keyStore.size.entries",
                "\u60A8\u7684\u5BC6\u94A5\u5E93\u5305\u542B {0,number,integer} \u4E2A\u6761\u76EE"},
        {"Failed.to.parse.input", "\u65E0\u6CD5\u89E3\u6790\u8F93\u5165"},
        {"Empty.input", "\u7A7A\u8F93\u5165"},
        {"Not.X.509.certificate", "\u975E X.509 \u8BC1\u4E66"},
        {"alias.has.no.public.key", "{0}\u6CA1\u6709\u516C\u5171\u5BC6\u94A5"},
        {"alias.has.no.X.509.certificate", "{0}\u6CA1\u6709 X.509 \u8BC1\u4E66"},
        {"New.certificate.self.signed.", "\u65B0\u8BC1\u4E66 (\u81EA\u7B7E\u540D):"},
        {"Reply.has.no.certificates", "\u56DE\u590D\u4E2D\u6CA1\u6709\u8BC1\u4E66"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "\u8BC1\u4E66\u672A\u5BFC\u5165, \u522B\u540D <{0}> \u5DF2\u7ECF\u5B58\u5728"},
        {"Input.not.an.X.509.certificate", "\u6240\u8F93\u5165\u7684\u4E0D\u662F X.509 \u8BC1\u4E66"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "\u5728\u522B\u540D <{0}> \u4E4B\u4E0B, \u8BC1\u4E66\u5DF2\u7ECF\u5B58\u5728\u4E8E\u5BC6\u94A5\u5E93\u4E2D"},
        {"Do.you.still.want.to.add.it.no.",
                "\u662F\u5426\u4ECD\u8981\u6DFB\u52A0? [\u5426]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "\u5728\u522B\u540D <{0}> \u4E4B\u4E0B, \u8BC1\u4E66\u5DF2\u7ECF\u5B58\u5728\u4E8E\u7CFB\u7EDF\u8303\u56F4\u7684 CA \u5BC6\u94A5\u5E93\u4E2D"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "\u662F\u5426\u4ECD\u8981\u5C06\u5B83\u6DFB\u52A0\u5230\u81EA\u5DF1\u7684\u5BC6\u94A5\u5E93? [\u5426]:  "},
        {"Trust.this.certificate.no.", "\u662F\u5426\u4FE1\u4EFB\u6B64\u8BC1\u4E66? [\u5426]:  "},
        {"New.prompt.", "\u65B0{0}: "},
        {"Passwords.must.differ", "\u53E3\u4EE4\u4E0D\u80FD\u76F8\u540C"},
        {"Re.enter.new.prompt.", "\u91CD\u65B0\u8F93\u5165\u65B0{0}: "},
        {"Re.enter.password.", "\u518D\u6B21\u8F93\u5165\u53E3\u4EE4: "},
        {"Re.enter.new.password.", "\u518D\u6B21\u8F93\u5165\u65B0\u53E3\u4EE4: "},
        {"They.don.t.match.Try.again", "\u5B83\u4EEC\u4E0D\u5339\u914D\u3002\u8BF7\u91CD\u8BD5"},
        {"Enter.prompt.alias.name.", "\u8F93\u5165{0}\u522B\u540D:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "\u5BFC\u5165\u65B0\u7684\u522B\u540D\t(\u6309\u56DE\u8F66\u4EE5\u53D6\u6D88\u5BF9\u6B64\u6761\u76EE\u7684\u5BFC\u5165):  "},
        {"Enter.alias.name.", "\u8F93\u5165\u522B\u540D:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(\u5982\u679C\u548C <{0}> \u76F8\u540C, \u5219\u6309\u56DE\u8F66)"},
        {"What.is.your.first.and.last.name.",
                "\u60A8\u7684\u540D\u5B57\u4E0E\u59D3\u6C0F\u662F\u4EC0\u4E48?"},
        {"What.is.the.name.of.your.organizational.unit.",
                "\u60A8\u7684\u7EC4\u7EC7\u5355\u4F4D\u540D\u79F0\u662F\u4EC0\u4E48?"},
        {"What.is.the.name.of.your.organization.",
                "\u60A8\u7684\u7EC4\u7EC7\u540D\u79F0\u662F\u4EC0\u4E48?"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "\u60A8\u6240\u5728\u7684\u57CE\u5E02\u6216\u533A\u57DF\u540D\u79F0\u662F\u4EC0\u4E48?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "\u60A8\u6240\u5728\u7684\u7701/\u5E02/\u81EA\u6CBB\u533A\u540D\u79F0\u662F\u4EC0\u4E48?"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "\u8BE5\u5355\u4F4D\u7684\u53CC\u5B57\u6BCD\u56FD\u5BB6/\u5730\u533A\u4EE3\u7801\u662F\u4EC0\u4E48?"},
        {"Is.name.correct.", "{0}\u662F\u5426\u6B63\u786E?"},
        {"no", "\u5426"},
        {"yes", "\u662F"},
        {"y", "y"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "\u522B\u540D <{0}> \u6CA1\u6709\u5BC6\u94A5"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "\u522B\u540D <{0}> \u5F15\u7528\u4E86\u4E0D\u5C5E\u4E8E\u79C1\u6709\u5BC6\u94A5\u6761\u76EE\u7684\u6761\u76EE\u7C7B\u578B\u3002-keyclone \u547D\u4EE4\u4EC5\u652F\u6301\u5BF9\u79C1\u6709\u5BC6\u94A5\u6761\u76EE\u7684\u514B\u9686"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "\u7B7E\u540D\u8005 #%d:"},
        {"Timestamp.", "\u65F6\u95F4\u6233:"},
        {"Signature.", "\u7B7E\u540D:"},
        {"Certificate.owner.", "\u8BC1\u4E66\u6240\u6709\u8005: "},
        {"Not.a.signed.jar.file", "\u4E0D\u662F\u5DF2\u7B7E\u540D\u7684 jar \u6587\u4EF6"},
        {"No.certificate.from.the.SSL.server",
                "\u6CA1\u6709\u6765\u81EA SSL \u670D\u52A1\u5668\u7684\u8BC1\u4E66"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* \u5B58\u50A8\u5728\u60A8\u7684\u5BC6\u94A5\u5E93\u4E2D\u7684\u4FE1\u606F\u7684\u5B8C\u6574\u6027  *\n* \u5C1A\u672A\u7ECF\u8FC7\u9A8C\u8BC1!  \u4E3A\u4E86\u9A8C\u8BC1\u5176\u5B8C\u6574\u6027, *\n* \u5FC5\u987B\u63D0\u4F9B\u5BC6\u94A5\u5E93\u53E3\u4EE4\u3002                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* \u5B58\u50A8\u5728 srckeystore \u4E2D\u7684\u4FE1\u606F\u7684\u5B8C\u6574\u6027*\n* \u5C1A\u672A\u7ECF\u8FC7\u9A8C\u8BC1!  \u4E3A\u4E86\u9A8C\u8BC1\u5176\u5B8C\u6574\u6027, *\n* \u5FC5\u987B\u63D0\u4F9B\u6E90\u5BC6\u94A5\u5E93\u53E3\u4EE4\u3002                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "\u8BC1\u4E66\u56DE\u590D\u4E2D\u4E0D\u5305\u542B <{0}> \u7684\u516C\u5171\u5BC6\u94A5"},
        {"Incomplete.certificate.chain.in.reply",
                "\u56DE\u590D\u4E2D\u7684\u8BC1\u4E66\u94FE\u4E0D\u5B8C\u6574"},
        {"Top.level.certificate.in.reply.",
                "\u56DE\u590D\u4E2D\u7684\u9876\u7EA7\u8BC1\u4E66:\n"},
        {".is.not.trusted.", "... \u662F\u4E0D\u53EF\u4FE1\u7684\u3002"},
        {"Install.reply.anyway.no.", "\u662F\u5426\u4ECD\u8981\u5B89\u88C5\u56DE\u590D? [\u5426]:  "},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "\u56DE\u590D\u4E2D\u7684\u516C\u5171\u5BC6\u94A5\u4E0E\u5BC6\u94A5\u5E93\u4E0D\u5339\u914D"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "\u8BC1\u4E66\u56DE\u590D\u4E0E\u5BC6\u94A5\u5E93\u4E2D\u7684\u8BC1\u4E66\u662F\u76F8\u540C\u7684"},
        {"Failed.to.establish.chain.from.reply",
                "\u65E0\u6CD5\u4ECE\u56DE\u590D\u4E2D\u5EFA\u7ACB\u94FE"},
        {"n", "n"},
        {"Wrong.answer.try.again", "\u9519\u8BEF\u7684\u7B54\u6848, \u8BF7\u518D\u8BD5\u4E00\u6B21"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "\u6CA1\u6709\u751F\u6210\u5BC6\u94A5, \u522B\u540D <{0}> \u5DF2\u7ECF\u5B58\u5728"},
        {"Please.provide.keysize.for.secret.key.generation",
                "\u8BF7\u63D0\u4F9B -keysize \u4EE5\u751F\u6210\u5BC6\u94A5"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "\u8B66\u544A: \u672A\u9A8C\u8BC1\u3002\u8BF7\u786E\u4FDD\u5BC6\u94A5\u5E93\u662F\u6B63\u786E\u7684\u3002"},
        {"warning.not.verified.make.sure.keystore.is.correct.or.specify.trustcacerts",
            "\u8B66\u544A\uFF1A\u672A\u9A8C\u8BC1\u3002\u8BF7\u786E\u4FDD\u5BC6\u94A5\u5E93\u662F\u6B63\u786E\u7684\uFF0C\u6216\u8005\u6307\u5B9A -trustcacerts\u3002"},

        {"Extensions.", "\u6269\u5C55: "},
        {".Empty.value.", "(\u7A7A\u503C)"},
        {"Extension.Request.", "\u6269\u5C55\u8BF7\u6C42:"},
        {"Unknown.keyUsage.type.", "\u672A\u77E5 keyUsage \u7C7B\u578B: "},
        {"Unknown.extendedkeyUsage.type.", "\u672A\u77E5 extendedkeyUsage \u7C7B\u578B: "},
        {"Unknown.AccessDescription.type.", "\u672A\u77E5 AccessDescription \u7C7B\u578B: "},
        {"Unrecognized.GeneralName.type.", "\u65E0\u6CD5\u8BC6\u522B\u7684 GeneralName \u7C7B\u578B: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "\u65E0\u6CD5\u5C06\u6B64\u6269\u5C55\u6807\u8BB0\u4E3A\u201C\u4E25\u91CD\u201D\u3002"},
        {"Odd.number.of.hex.digits.found.", "\u627E\u5230\u5947\u6570\u4E2A\u5341\u516D\u8FDB\u5236\u6570\u5B57: "},
        {"Unknown.extension.type.", "\u672A\u77E5\u6269\u5C55\u7C7B\u578B: "},
        {"command.{0}.is.ambiguous.", "\u547D\u4EE4{0}\u4E0D\u660E\u786E:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "\u8BC1\u4E66\u8BF7\u6C42"},
        {"the.issuer", "\u53D1\u5E03\u8005"},
        {"the.generated.certificate", "\u751F\u6210\u7684\u8BC1\u4E66"},
        {"the.generated.crl", "\u751F\u6210\u7684 CRL"},
        {"the.generated.certificate.request", "\u751F\u6210\u7684\u8BC1\u4E66\u8BF7\u6C42"},
        {"the.certificate", "\u8BC1\u4E66"},
        {"the.crl", "CRL"},
        {"the.tsa.certificate", "TSA \u8BC1\u4E66"},
        {"the.input", "\u8F93\u5165"},
        {"reply", "\u56DE\u590D"},
        {"one.in.many", "%1$s #%2$d/%3$d"},
        {"alias.in.cacerts", "cacerts \u4E2D\u7684\u53D1\u5E03\u8005 <%s>"},
        {"alias.in.keystore", "\u53D1\u5E03\u8005 <%s>"},
        {"with.weak", "%s (\u5F31)"},
        {"with.disabled", "%s\uFF08\u7981\u7528\uFF09"},
        {"key.bit", "%1$d \u4F4D %2$s \u5BC6\u94A5"},
        {"key.bit.weak", "%1$d \u4F4D %2$s \u5BC6\u94A5 (\u5F31)"},
        {"key.bit.disabled", "%1$d \u4F4D %2$s \u5BC6\u94A5\uFF08\u7981\u7528\uFF09"},
        {"unknown.size.1", "\u672A\u77E5\u5927\u5C0F\u7684 %s \u5BC6\u94A5"},
        {".PATTERN.printX509Cert.with.weak",
                "\u6240\u6709\u8005: {0}\n\u53D1\u5E03\u8005: {1}\n\u5E8F\u5217\u53F7: {2}\n\u751F\u6548\u65F6\u95F4: {3}, \u5931\u6548\u65F6\u95F4: {4}\n\u8BC1\u4E66\u6307\u7EB9:\n\t SHA1: {5}\n\t SHA256: {6}\n\u7B7E\u540D\u7B97\u6CD5\u540D\u79F0: {7}\n\u4E3B\u4F53\u516C\u5171\u5BC6\u94A5\u7B97\u6CD5: {8}\n\u7248\u672C: {9}"},
        {"PKCS.10.with.weak",
                "PKCS #10 \u8BC1\u4E66\u8BF7\u6C42 (\u7248\u672C 1.0)\n\u4E3B\u4F53: %1$s\n\u683C\u5F0F: %2$s\n\u516C\u5171\u5BC6\u94A5: %3$s\n\u7B7E\u540D\u7B97\u6CD5: %4$s\n"},
        {"verified.by.s.in.s.weak", "\u7531 %2$s \u4E2D\u7684 %1$s \u4EE5 %3$s \u9A8C\u8BC1"},
        {"whose.sigalg.disabled", "%1$s \u4F7F\u7528\u7684 %2$s \u7B7E\u540D\u7B97\u6CD5\u88AB\u89C6\u4E3A\u5B58\u5728\u5B89\u5168\u98CE\u9669\u800C\u4E14\u88AB\u7981\u7528\u3002"},
        {"whose.sigalg.weak", "%1$s \u4F7F\u7528\u7684 %2$s \u7B7E\u540D\u7B97\u6CD5\u88AB\u89C6\u4E3A\u5B58\u5728\u5B89\u5168\u98CE\u9669\u3002\u6B64\u7B97\u6CD5\u5C06\u5728\u672A\u6765\u7684\u66F4\u65B0\u4E2D\u88AB\u7981\u7528\u3002"},
        {"whose.key.disabled", "%1$s \u4F7F\u7528\u7684 %2$s \u88AB\u89C6\u4E3A\u5B58\u5728\u5B89\u5168\u98CE\u9669\u800C\u4E14\u88AB\u7981\u7528\u3002"},
        {"whose.key.weak", "%1$s \u4F7F\u7528\u7684 %2$s \u88AB\u89C6\u4E3A\u5B58\u5728\u5B89\u5168\u98CE\u9669\u3002\u6B64\u5BC6\u94A5\u5927\u5C0F\u5C06\u5728\u672A\u6765\u7684\u66F4\u65B0\u4E2D\u88AB\u7981\u7528\u3002"},
        {"jks.storetype.warning", "%1$s \u5BC6\u94A5\u5E93\u4F7F\u7528\u4E13\u7528\u683C\u5F0F\u3002\u5EFA\u8BAE\u4F7F\u7528 \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\" \u8FC1\u79FB\u5230\u884C\u4E1A\u6807\u51C6\u683C\u5F0F PKCS12\u3002"},
        {"migrate.keystore.warning", "\u5DF2\u5C06 \"%1$s\" \u8FC1\u79FB\u5230 %4$s\u3002\u5C06 %2$s \u5BC6\u94A5\u5E93\u4F5C\u4E3A \"%3$s\" \u8FDB\u884C\u4E86\u5907\u4EFD\u3002"},
        {"backup.keystore.warning", "\u5DF2\u5C06\u539F\u59CB\u5BC6\u94A5\u5E93 \"%1$s\" \u5907\u4EFD\u4E3A \"%3$s\"..."},
        {"importing.keystore.status", "\u6B63\u5728\u5C06\u5BC6\u94A5\u5E93 %1$s \u5BFC\u5165\u5230 %2$s..."},
        {"keyalg.option.missing.error", "\u5FC5\u987B\u6307\u5B9A -keyalg \u9009\u9879\u3002"},

        {"showinfo.no.option", "-showinfo \u7F3A\u5C11\u9009\u9879\u3002\u8BF7\u5C1D\u8BD5\u4F7F\u7528 \"keytool -showinfo -tls\"\u3002"},
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
