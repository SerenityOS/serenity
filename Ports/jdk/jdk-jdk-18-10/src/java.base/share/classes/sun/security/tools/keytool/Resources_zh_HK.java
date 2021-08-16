/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * This class represents the <code>ResourceBundle</code>
 * for the keytool.
 *
 */
public class Resources_zh_HK extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
// "Option" should be translated.
        {".OPTION.", " [\u9078\u9805]..."},
        {"Options.", "\u9078\u9805:"},
        {"Use.keytool.help.for.all.available.commands",
                 "\u4F7F\u7528 \"keytool -help\" \u53D6\u5F97\u6240\u6709\u53EF\u7528\u7684\u547D\u4EE4"},
        {"Key.and.Certificate.Management.Tool",
                 "\u91D1\u9470\u8207\u6191\u8B49\u7BA1\u7406\u5DE5\u5177"},
        {"Commands.", "\u547D\u4EE4:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "\u4F7F\u7528 \"keytool -command_name -help\" \u53D6\u5F97 command_name \u7684\u7528\u6CD5"},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "\u7522\u751F\u6191\u8B49\u8981\u6C42"}, //-certreq
        {"Changes.an.entry.s.alias",
                "\u8B8A\u66F4\u9805\u76EE\u7684\u5225\u540D"}, //-changealias
        {"Deletes.an.entry",
                "\u522A\u9664\u9805\u76EE"}, //-delete
        {"Exports.certificate",
                "\u532F\u51FA\u6191\u8B49"}, //-exportcert
        {"Generates.a.key.pair",
                "\u7522\u751F\u91D1\u9470\u7D44"}, //-genkeypair
// translation of "secret" key should be different to "private" key.
        {"Generates.a.secret.key",
                "\u7522\u751F\u79D8\u5BC6\u91D1\u9470"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "\u5F9E\u6191\u8B49\u8981\u6C42\u7522\u751F\u6191\u8B49"}, //-gencert
        {"Generates.CRL", "\u7522\u751F CRL"}, //-gencrl
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "\u5F9E JDK 1.1.x-style \u8B58\u5225\u8CC7\u6599\u5EAB\u532F\u5165\u9805\u76EE"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "\u532F\u5165\u6191\u8B49\u6216\u6191\u8B49\u93C8"}, //-importcert
        {"Imports.one.or.all.entries.from.another.keystore",
                "\u5F9E\u5176\u4ED6\u91D1\u9470\u5132\u5B58\u5EAB\u532F\u5165\u4E00\u500B\u6216\u5168\u90E8\u9805\u76EE"}, //-importkeystore
        {"Clones.a.key.entry",
                "\u8907\u88FD\u91D1\u9470\u9805\u76EE"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "\u8B8A\u66F4\u9805\u76EE\u7684\u91D1\u9470\u5BC6\u78BC"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "\u5217\u793A\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u7684\u9805\u76EE"}, //-list
        {"Prints.the.content.of.a.certificate",
                "\u5217\u5370\u6191\u8B49\u7684\u5167\u5BB9"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "\u5217\u5370\u6191\u8B49\u8981\u6C42\u7684\u5167\u5BB9"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "\u5217\u5370 CRL \u6A94\u6848\u7684\u5167\u5BB9"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "\u7522\u751F\u81EA\u884C\u7C3D\u7F72\u7684\u6191\u8B49"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "\u8B8A\u66F4\u91D1\u9470\u5132\u5B58\u5EAB\u7684\u5132\u5B58\u5BC6\u78BC"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "\u8981\u8655\u7406\u9805\u76EE\u7684\u5225\u540D\u540D\u7A31"}, //-alias
        {"destination.alias",
                "\u76EE\u7684\u5730\u5225\u540D"}, //-destalias
        {"destination.key.password",
                "\u76EE\u7684\u5730\u91D1\u9470\u5BC6\u78BC"}, //-destkeypass
        {"destination.keystore.name",
                "\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u540D\u7A31"}, //-destkeystore
        {"destination.keystore.password.protected",
                "\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u4FDD\u8B77"}, //-destprotected
        {"destination.keystore.provider.name",
                "\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u63D0\u4F9B\u8005\u540D\u7A31"}, //-destprovidername
        {"destination.keystore.password",
                "\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC"}, //-deststorepass
        {"destination.keystore.type",
                "\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u985E\u578B"}, //-deststoretype
        {"distinguished.name",
                "\u8FA8\u5225\u540D\u7A31"}, //-dname
        {"X.509.extension",
                "X.509 \u64F4\u5145\u5957\u4EF6"}, //-ext
        {"output.file.name",
                "\u8F38\u51FA\u6A94\u6848\u540D\u7A31"}, //-file and -outfile
        {"input.file.name",
                "\u8F38\u5165\u6A94\u6848\u540D\u7A31"}, //-file and -infile
        {"key.algorithm.name",
                "\u91D1\u9470\u6F14\u7B97\u6CD5\u540D\u7A31"}, //-keyalg
        {"key.password",
                "\u91D1\u9470\u5BC6\u78BC"}, //-keypass
        {"key.bit.size",
                "\u91D1\u9470\u4F4D\u5143\u5927\u5C0F"}, //-keysize
        {"keystore.name",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u540D\u7A31"}, //-keystore
        {"new.password",
                "\u65B0\u5BC6\u78BC"}, //-new
        {"do.not.prompt",
                "\u4E0D\u8981\u63D0\u793A"}, //-noprompt
        {"password.through.protected.mechanism",
                "\u7D93\u7531\u4FDD\u8B77\u6A5F\u5236\u7684\u5BC6\u78BC"}, //-protected
        {"provider.argument",
                "\u63D0\u4F9B\u8005\u5F15\u6578"}, //-providerarg
        {"provider.class.name",
                "\u63D0\u4F9B\u8005\u985E\u5225\u540D\u7A31"}, //-providerclass
        {"provider.name",
                "\u63D0\u4F9B\u8005\u540D\u7A31"}, //-providername
        {"provider.classpath",
                "\u63D0\u4F9B\u8005\u985E\u5225\u8DEF\u5F91"}, //-providerpath
        {"output.in.RFC.style",
                "\u4EE5 RFC \u6A23\u5F0F\u8F38\u51FA"}, //-rfc
        {"signature.algorithm.name",
                "\u7C3D\u7AE0\u6F14\u7B97\u6CD5\u540D\u7A31"}, //-sigalg
        {"source.alias",
                "\u4F86\u6E90\u5225\u540D"}, //-srcalias
        {"source.key.password",
                "\u4F86\u6E90\u91D1\u9470\u5BC6\u78BC"}, //-srckeypass
        {"source.keystore.name",
                "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u540D\u7A31"}, //-srckeystore
        {"source.keystore.password.protected",
                "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u4FDD\u8B77"}, //-srcprotected
        {"source.keystore.provider.name",
                "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u63D0\u4F9B\u8005\u540D\u7A31"}, //-srcprovidername
        {"source.keystore.password",
                "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC"}, //-srcstorepass
        {"source.keystore.type",
                "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u985E\u578B"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "SSL \u4F3A\u670D\u5668\u4E3B\u6A5F\u8207\u9023\u63A5\u57E0"}, //-sslserver
        {"signed.jar.file",
                "\u7C3D\u7F72\u7684 jar \u6A94\u6848"}, //=jarfile
        {"certificate.validity.start.date.time",
                "\u6191\u8B49\u6709\u6548\u6027\u958B\u59CB\u65E5\u671F/\u6642\u9593"}, //-startdate
        {"keystore.password",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC"}, //-storepass
        {"keystore.type",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u985E\u578B"}, //-storetype
        {"trust.certificates.from.cacerts",
                "\u4F86\u81EA cacerts \u7684\u4FE1\u4EFB\u6191\u8B49"}, //-trustcacerts
        {"verbose.output",
                "\u8A73\u7D30\u8CC7\u8A0A\u8F38\u51FA"}, //-v
        {"validity.number.of.days",
                "\u6709\u6548\u6027\u65E5\u6578"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "\u8981\u64A4\u92B7\u6191\u8B49\u7684\u5E8F\u5217 ID"}, //-id
        // keytool: Running part
        {"keytool.error.", "\u91D1\u9470\u5DE5\u5177\u932F\u8AA4: "},
        {"Illegal.option.", "\u7121\u6548\u7684\u9078\u9805:"},
        {"Illegal.value.", "\u7121\u6548\u503C: "},
        {"Unknown.password.type.", "\u4E0D\u660E\u7684\u5BC6\u78BC\u985E\u578B: "},
        {"Cannot.find.environment.variable.",
                "\u627E\u4E0D\u5230\u74B0\u5883\u8B8A\u6578: "},
        {"Cannot.find.file.", "\u627E\u4E0D\u5230\u6A94\u6848: "},
        {"Command.option.flag.needs.an.argument.", "\u547D\u4EE4\u9078\u9805 {0} \u9700\u8981\u5F15\u6578\u3002"},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "\u8B66\u544A: PKCS12 \u91D1\u9470\u5132\u5B58\u5EAB\u4E0D\u652F\u63F4\u4E0D\u540C\u7684\u5132\u5B58\u5EAB\u548C\u91D1\u9470\u5BC6\u78BC\u3002\u5FFD\u7565\u4F7F\u7528\u8005\u6307\u5B9A\u7684 {0} \u503C\u3002"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u70BA {0}\uFF0C\u5247 -keystore \u5FC5\u9808\u70BA NONE"},
        {"Too.many.retries.program.terminated",
                 "\u91CD\u8A66\u6B21\u6578\u592A\u591A\uFF0C\u7A0B\u5F0F\u5DF2\u7D42\u6B62"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u70BA {0}\uFF0C\u5247\u4E0D\u652F\u63F4 -storepasswd \u548C -keypasswd \u547D\u4EE4"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "\u5982\u679C -storetype \u70BA PKCS12\uFF0C\u5247\u4E0D\u652F\u63F4 -keypasswd \u547D\u4EE4"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "\u5982\u679C -storetype \u70BA {0}\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A -keypass \u548C -new"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "\u5982\u679C\u6307\u5B9A -protected\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A -storepass\u3001-keypass \u548C -new"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\u5982\u679C\u6307\u5B9A -srcprotected\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A -srcstorepass \u548C -srckeypass"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "\u5982\u679C\u91D1\u9470\u5132\u5B58\u5EAB\u4E0D\u53D7\u5BC6\u78BC\u4FDD\u8B77\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A -storepass\u3001-keypass \u548C -new"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "\u5982\u679C\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u4E0D\u53D7\u5BC6\u78BC\u4FDD\u8B77\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A -srcstorepass \u548C -srckeypass"},
        {"Illegal.startdate.value", "\u7121\u6548\u7684 startdate \u503C"},
        {"Validity.must.be.greater.than.zero",
                "\u6709\u6548\u6027\u5FC5\u9808\u5927\u65BC\u96F6"},
        {"provName.not.a.provider", "{0} \u4E0D\u662F\u4E00\u500B\u63D0\u4F9B\u8005"},
        {"Usage.error.no.command.provided", "\u7528\u6CD5\u932F\u8AA4: \u672A\u63D0\u4F9B\u547D\u4EE4"},
        {"Source.keystore.file.exists.but.is.empty.", "\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u6A94\u6848\u5B58\u5728\uFF0C\u4F46\u70BA\u7A7A: "},
        {"Please.specify.srckeystore", "\u8ACB\u6307\u5B9A -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                " 'list' \u547D\u4EE4\u4E0D\u80FD\u540C\u6642\u6307\u5B9A -v \u53CA -rfc"},
        {"Key.password.must.be.at.least.6.characters",
                "\u91D1\u9470\u5BC6\u78BC\u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"New.password.must.be.at.least.6.characters",
                "\u65B0\u7684\u5BC6\u78BC\u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"Keystore.file.exists.but.is.empty.",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u6A94\u6848\u5B58\u5728\uFF0C\u4F46\u70BA\u7A7A\u767D: "},
        {"Keystore.file.does.not.exist.",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u6A94\u6848\u4E0D\u5B58\u5728: "},
        {"Must.specify.destination.alias", "\u5FC5\u9808\u6307\u5B9A\u76EE\u7684\u5730\u5225\u540D"},
        {"Must.specify.alias", "\u5FC5\u9808\u6307\u5B9A\u5225\u540D"},
        {"Keystore.password.must.be.at.least.6.characters",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"Enter.keystore.password.", "\u8F38\u5165\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC:  "},
        {"Enter.source.keystore.password.", "\u8ACB\u8F38\u5165\u4F86\u6E90\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC: "},
        {"Enter.destination.keystore.password.", "\u8ACB\u8F38\u5165\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC: "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u592A\u77ED - \u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"Unknown.Entry.Type", "\u4E0D\u660E\u7684\u9805\u76EE\u985E\u578B"},
        {"Too.many.failures.Alias.not.changed", "\u592A\u591A\u932F\u8AA4\u3002\u672A\u8B8A\u66F4\u5225\u540D"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "\u5DF2\u6210\u529F\u532F\u5165\u5225\u540D {0} \u7684\u9805\u76EE\u3002"},
        {"Entry.for.alias.alias.not.imported.", "\u672A\u532F\u5165\u5225\u540D {0} \u7684\u9805\u76EE\u3002"},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "\u532F\u5165\u5225\u540D {0} \u7684\u9805\u76EE\u6642\u51FA\u73FE\u554F\u984C: {1}\u3002\n\u672A\u532F\u5165\u5225\u540D {0} \u7684\u9805\u76EE\u3002"},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "\u5DF2\u5B8C\u6210\u532F\u5165\u547D\u4EE4: \u6210\u529F\u532F\u5165 {0} \u500B\u9805\u76EE\uFF0C{1} \u500B\u9805\u76EE\u5931\u6557\u6216\u5DF2\u53D6\u6D88"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "\u8B66\u544A: \u6B63\u5728\u8986\u5BEB\u76EE\u7684\u5730\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u7684\u73FE\u6709\u5225\u540D {0}"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "\u73FE\u6709\u9805\u76EE\u5225\u540D {0} \u5B58\u5728\uFF0C\u662F\u5426\u8986\u5BEB\uFF1F[\u5426]:  "},
        {"Too.many.failures.try.later", "\u592A\u591A\u932F\u8AA4 - \u8ACB\u7A0D\u5F8C\u518D\u8A66"},
        {"Certification.request.stored.in.file.filename.",
                "\u8A8D\u8B49\u8981\u6C42\u5132\u5B58\u5728\u6A94\u6848 <{0}>"},
        {"Submit.this.to.your.CA", "\u5C07\u6B64\u9001\u51FA\u81F3\u60A8\u7684 CA"},
        {"if.alias.not.specified.destalias.srckeypass.and.destkeypass.must.not.be.specified",
            "\u5982\u679C\u672A\u6307\u5B9A\u5225\u540D\uFF0C\u5247\u4E0D\u80FD\u6307\u5B9A destalias\u3001srckeypass \u53CA destkeypass"},
        {"Certificate.stored.in.file.filename.",
                "\u6191\u8B49\u5132\u5B58\u5728\u6A94\u6848 <{0}>"},
        {"Certificate.reply.was.installed.in.keystore",
                "\u6191\u8B49\u56DE\u8986\u5DF2\u5B89\u88DD\u5728\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "\u6191\u8B49\u56DE\u8986\u672A\u5B89\u88DD\u5728\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D"},
        {"Certificate.was.added.to.keystore",
                "\u6191\u8B49\u5DF2\u65B0\u589E\u81F3\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D"},
        {"Certificate.was.not.added.to.keystore",
                "\u6191\u8B49\u672A\u65B0\u589E\u81F3\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D"},
        {".Storing.ksfname.", "[\u5132\u5B58 {0}]"},
        {"alias.has.no.public.key.certificate.",
                "{0} \u6C92\u6709\u516C\u958B\u91D1\u9470 (\u6191\u8B49)"},
        {"Cannot.derive.signature.algorithm",
                "\u7121\u6CD5\u53D6\u5F97\u7C3D\u7AE0\u6F14\u7B97\u6CD5"},
        {"Alias.alias.does.not.exist",
                "\u5225\u540D <{0}> \u4E0D\u5B58\u5728"},
        {"Alias.alias.has.no.certificate",
                "\u5225\u540D <{0}> \u6C92\u6709\u6191\u8B49"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "\u6C92\u6709\u5EFA\u7ACB\u91D1\u9470\u7D44\uFF0C\u5225\u540D <{0}> \u5DF2\u7D93\u5B58\u5728"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "\u91DD\u5C0D {4} \u7522\u751F\u6709\u6548\u671F {3} \u5929\u7684 {0} \u4F4D\u5143 {1} \u91D1\u9470\u7D44\u4EE5\u53CA\u81EA\u6211\u7C3D\u7F72\u6191\u8B49 ({2})\n\t"},
        {"Enter.key.password.for.alias.", "\u8F38\u5165 <{0}> \u7684\u91D1\u9470\u5BC6\u78BC"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(RETURN \u5982\u679C\u548C\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u76F8\u540C):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "\u91D1\u9470\u5BC6\u78BC\u592A\u77ED - \u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"Too.many.failures.key.not.added.to.keystore",
                "\u592A\u591A\u932F\u8AA4 - \u91D1\u9470\u672A\u65B0\u589E\u81F3\u91D1\u9470\u5132\u5B58\u5EAB"},
        {"Destination.alias.dest.already.exists",
                "\u76EE\u7684\u5730\u5225\u540D <{0}> \u5DF2\u7D93\u5B58\u5728"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "\u5BC6\u78BC\u592A\u77ED - \u5FC5\u9808\u81F3\u5C11\u70BA 6 \u500B\u5B57\u5143"},
        {"Too.many.failures.Key.entry.not.cloned",
                "\u592A\u591A\u932F\u8AA4\u3002\u672A\u8907\u88FD\u91D1\u9470\u9805\u76EE"},
        {"key.password.for.alias.", "<{0}> \u7684\u91D1\u9470\u5BC6\u78BC"},
        {"Keystore.entry.for.id.getName.already.exists",
                "<{0}> \u7684\u91D1\u9470\u5132\u5B58\u5EAB\u9805\u76EE\u5DF2\u7D93\u5B58\u5728"},
        {"Creating.keystore.entry.for.id.getName.",
                "\u5EFA\u7ACB <{0}> \u7684\u91D1\u9470\u5132\u5B58\u5EAB\u9805\u76EE..."},
        {"No.entries.from.identity.database.added",
                "\u6C92\u6709\u65B0\u589E\u4F86\u81EA\u8B58\u5225\u8CC7\u6599\u5EAB\u7684\u9805\u76EE"},
        {"Alias.name.alias", "\u5225\u540D\u540D\u7A31: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "\u5EFA\u7ACB\u65E5\u671F: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "\u9805\u76EE\u985E\u578B: {0}"},
        {"Certificate.chain.length.", "\u6191\u8B49\u93C8\u9577\u5EA6: "},
        {"Certificate.i.1.", "\u6191\u8B49 [{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA1.", "\u6191\u8B49\u6307\u7D0B (SHA1): "},
        {"Keystore.type.", "\u91D1\u9470\u5132\u5B58\u5EAB\u985E\u578B: "},
        {"Keystore.provider.", "\u91D1\u9470\u5132\u5B58\u5EAB\u63D0\u4F9B\u8005: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "\u60A8\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u5305\u542B {0,number,integer} \u9805\u76EE"},
        {"Your.keystore.contains.keyStore.size.entries",
                "\u60A8\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u5305\u542B {0,number,integer} \u9805\u76EE"},
        {"Failed.to.parse.input", "\u7121\u6CD5\u5256\u6790\u8F38\u5165"},
        {"Empty.input", "\u7A7A\u8F38\u5165"},
        {"Not.X.509.certificate", "\u975E X.509 \u6191\u8B49"},
        {"alias.has.no.public.key", "{0} \u7121\u516C\u958B\u91D1\u9470"},
        {"alias.has.no.X.509.certificate", "{0} \u7121 X.509 \u6191\u8B49"},
        {"New.certificate.self.signed.", "\u65B0\u6191\u8B49 (\u81EA\u6211\u7C3D\u7F72): "},
        {"Reply.has.no.certificates", "\u56DE\u8986\u4E0D\u542B\u6191\u8B49"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "\u6191\u8B49\u672A\u8F38\u5165\uFF0C\u5225\u540D <{0}> \u5DF2\u7D93\u5B58\u5728"},
        {"Input.not.an.X.509.certificate", "\u8F38\u5165\u7684\u4E0D\u662F X.509 \u6191\u8B49"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u7684 <{0}> \u5225\u540D\u4E4B\u4E0B\uFF0C\u6191\u8B49\u5DF2\u7D93\u5B58\u5728"},
        {"Do.you.still.want.to.add.it.no.",
                "\u60A8\u4ECD\u7136\u60F3\u8981\u5C07\u4E4B\u65B0\u589E\u55CE\uFF1F [\u5426]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "\u6574\u500B\u7CFB\u7D71 CA \u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u7684 <{0}> \u5225\u540D\u4E4B\u4E0B\uFF0C\u6191\u8B49\u5DF2\u7D93\u5B58\u5728"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "\u60A8\u4ECD\u7136\u60F3\u8981\u5C07\u4E4B\u65B0\u589E\u81F3\u81EA\u5DF1\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u55CE\uFF1F [\u5426]:  "},
        {"Trust.this.certificate.no.", "\u4FE1\u4EFB\u9019\u500B\u6191\u8B49\uFF1F [\u5426]:  "},
        {"YES", "\u662F"},
        {"New.prompt.", "\u65B0 {0}: "},
        {"Passwords.must.differ", "\u5FC5\u9808\u662F\u4E0D\u540C\u7684\u5BC6\u78BC"},
        {"Re.enter.new.prompt.", "\u91CD\u65B0\u8F38\u5165\u65B0 {0}: "},
        {"Re.enter.new.password.", "\u91CD\u65B0\u8F38\u5165\u65B0\u5BC6\u78BC: "},
        {"They.don.t.match.Try.again", "\u5B83\u5011\u4E0D\u76F8\u7B26\u3002\u8ACB\u91CD\u8A66"},
        {"Enter.prompt.alias.name.", "\u8F38\u5165 {0} \u5225\u540D\u540D\u7A31:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "\u8ACB\u8F38\u5165\u65B0\u7684\u5225\u540D\u540D\u7A31\t(RETURN \u4EE5\u53D6\u6D88\u532F\u5165\u6B64\u9805\u76EE):"},
        {"Enter.alias.name.", "\u8F38\u5165\u5225\u540D\u540D\u7A31:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(RETURN \u5982\u679C\u548C <{0}> \u7684\u76F8\u540C)"},
        {".PATTERN.printX509Cert",
                "\u64C1\u6709\u8005: {0}\n\u767C\u51FA\u8005: {1}\n\u5E8F\u865F: {2}\n\u6709\u6548\u671F\u81EA: {3} \u5230: {4}\n\u6191\u8B49\u6307\u7D0B:\n\t MD5:  {5}\n\t SHA1: {6}\n\t SHA256: {7}\n\t \u7C3D\u7AE0\u6F14\u7B97\u6CD5\u540D\u7A31: {8}\n\t \u7248\u672C: {9}"},
        {"What.is.your.first.and.last.name.",
                "\u60A8\u7684\u540D\u5B57\u8207\u59D3\u6C0F\u70BA\u4F55\uFF1F"},
        {"What.is.the.name.of.your.organizational.unit.",
                "\u60A8\u7684\u7D44\u7E54\u55AE\u4F4D\u540D\u7A31\u70BA\u4F55\uFF1F"},
        {"What.is.the.name.of.your.organization.",
                "\u60A8\u7684\u7D44\u7E54\u540D\u7A31\u70BA\u4F55\uFF1F"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "\u60A8\u6240\u5728\u7684\u57CE\u5E02\u6216\u5730\u5340\u540D\u7A31\u70BA\u4F55\uFF1F"},
        {"What.is.the.name.of.your.State.or.Province.",
                "\u60A8\u6240\u5728\u7684\u5DDE\u53CA\u7701\u4EFD\u540D\u7A31\u70BA\u4F55\uFF1F"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "\u6B64\u55AE\u4F4D\u7684\u5169\u500B\u5B57\u6BCD\u570B\u5225\u4EE3\u78BC\u70BA\u4F55\uFF1F"},
        {"Is.name.correct.", "{0} \u6B63\u78BA\u55CE\uFF1F"},
        {"no", "\u5426"},
        {"yes", "\u662F"},
        {"y", "y"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "\u5225\u540D <{0}> \u6C92\u6709\u91D1\u9470"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "\u5225\u540D <{0}> \u6240\u53C3\u7167\u7684\u9805\u76EE\u4E0D\u662F\u79C1\u5BC6\u91D1\u9470\u985E\u578B\u3002-keyclone \u547D\u4EE4\u50C5\u652F\u63F4\u79C1\u5BC6\u91D1\u9470\u9805\u76EE\u7684\u8907\u88FD"},

        {".WARNING.WARNING.WARNING.",
            "*****************  \u8B66\u544A \u8B66\u544A \u8B66\u544A  *****************"},
        {"Signer.d.", "\u7C3D\u7F72\u8005 #%d:"},
        {"Timestamp.", "\u6642\u6233:"},
        {"Signature.", "\u7C3D\u7AE0:"},
        {"CRLs.", "CRL:"},
        {"Certificate.owner.", "\u6191\u8B49\u64C1\u6709\u8005: "},
        {"Not.a.signed.jar.file", "\u4E0D\u662F\u7C3D\u7F72\u7684 jar \u6A94\u6848"},
        {"No.certificate.from.the.SSL.server",
                "\u6C92\u6709\u4F86\u81EA SSL \u4F3A\u670D\u5668\u7684\u6191\u8B49"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* \u5C1A\u672A\u9A57\u8B49\u5132\u5B58\u65BC\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u8CC7\u8A0A  *\n* \u7684\u5B8C\u6574\u6027\uFF01\u82E5\u8981\u9A57\u8B49\u5176\u5B8C\u6574\u6027\uFF0C*\n* \u60A8\u5FC5\u9808\u63D0\u4F9B\u60A8\u7684\u91D1\u9470\u5132\u5B58\u5EAB\u5BC6\u78BC\u3002                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* \u5C1A\u672A\u9A57\u8B49\u5132\u5B58\u65BC srckeystore \u4E2D\u8CC7\u8A0A*\n* \u7684\u5B8C\u6574\u6027\uFF01\u82E5\u8981\u9A57\u8B49\u5176\u5B8C\u6574\u6027\uFF0C\u60A8\u5FC5\u9808 *\n* \u63D0\u4F9B srckeystore \u5BC6\u78BC\u3002          *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "\u6191\u8B49\u56DE\u8986\u4E26\u672A\u5305\u542B <{0}> \u7684\u516C\u958B\u91D1\u9470"},
        {"Incomplete.certificate.chain.in.reply",
                "\u56DE\u8986\u6642\u7684\u6191\u8B49\u93C8\u4E0D\u5B8C\u6574"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "\u56DE\u8986\u6642\u7684\u6191\u8B49\u93C8\u672A\u9A57\u8B49: "},
        {"Top.level.certificate.in.reply.",
                "\u56DE\u8986\u6642\u7684\u6700\u9AD8\u7D1A\u6191\u8B49:\\n"},
        {".is.not.trusted.", "... \u662F\u4E0D\u88AB\u4FE1\u4EFB\u7684\u3002"},
        {"Install.reply.anyway.no.", "\u9084\u662F\u8981\u5B89\u88DD\u56DE\u8986\uFF1F [\u5426]:  "},
        {"NO", "\u5426"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "\u56DE\u8986\u6642\u7684\u516C\u958B\u91D1\u9470\u8207\u91D1\u9470\u5132\u5B58\u5EAB\u4E0D\u7B26"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "\u6191\u8B49\u56DE\u8986\u8207\u91D1\u9470\u5132\u5B58\u5EAB\u4E2D\u7684\u6191\u8B49\u662F\u76F8\u540C\u7684"},
        {"Failed.to.establish.chain.from.reply",
                "\u7121\u6CD5\u5F9E\u56DE\u8986\u4E2D\u5C07\u93C8\u5EFA\u7ACB\u8D77\u4F86"},
        {"n", "n"},
        {"Wrong.answer.try.again", "\u932F\u8AA4\u7684\u7B54\u6848\uFF0C\u8ACB\u518D\u8A66\u4E00\u6B21"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "\u672A\u7522\u751F\u79D8\u5BC6\u91D1\u9470\uFF0C\u5225\u540D <{0}> \u5DF2\u5B58\u5728"},
        {"Please.provide.keysize.for.secret.key.generation",
                "\u8ACB\u63D0\u4F9B -keysize \u4EE5\u7522\u751F\u79D8\u5BC6\u91D1\u9470"},

        {"Extensions.", "\u64F4\u5145\u5957\u4EF6: "},
        {".Empty.value.", "(\u7A7A\u767D\u503C)"},
        {"Extension.Request.", "\u64F4\u5145\u5957\u4EF6\u8981\u6C42:"},
        {"PKCS.10.Certificate.Request.Version.1.0.Subject.s.Public.Key.s.format.s.key.",
                "PKCS #10 \u6191\u8B49\u8981\u6C42 (\u7248\u672C 1.0)\n\u4E3B\u9AD4: %s\n\u516C\u7528\u91D1\u9470: %s \u683C\u5F0F %s \u91D1\u9470\n"},
        {"Unknown.keyUsage.type.", "\u4E0D\u660E\u7684 keyUsage \u985E\u578B: "},
        {"Unknown.extendedkeyUsage.type.", "\u4E0D\u660E\u7684 extendedkeyUsage \u985E\u578B: "},
        {"Unknown.AccessDescription.type.", "\u4E0D\u660E\u7684 AccessDescription \u985E\u578B: "},
        {"Unrecognized.GeneralName.type.", "\u7121\u6CD5\u8FA8\u8B58\u7684 GeneralName \u985E\u578B: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "\u6B64\u64F4\u5145\u5957\u4EF6\u7121\u6CD5\u6A19\u793A\u70BA\u95DC\u9375\u3002"},
        {"Odd.number.of.hex.digits.found.", "\u627E\u5230\u5341\u516D\u9032\u4F4D\u6578\u5B57\u7684\u5947\u6578: "},
        {"Unknown.extension.type.", "\u4E0D\u660E\u7684\u64F4\u5145\u5957\u4EF6\u985E\u578B: "},
        {"command.{0}.is.ambiguous.", "\u547D\u4EE4 {0} \u4E0D\u660E\u78BA:"}
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
