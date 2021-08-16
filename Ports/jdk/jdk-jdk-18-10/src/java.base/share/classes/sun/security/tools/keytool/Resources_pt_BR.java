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
public class Resources_pt_BR extends java.util.ListResourceBundle {

    private static final Object[][] contents = {
        {"NEWLINE", "\n"},
        {"STAR",
                "*******************************************"},
        {"STARNN",
                "*******************************************\n\n"},

        // keytool: Help part
        {".OPTION.", " [OPTION]..."},
        {"Options.", "Op\u00E7\u00F5es:"},
        {"option.1.set.twice", "A op\u00E7\u00E3o %s foi especificada v\u00E1rias vezes. Todas, exceto a \u00FAltima, ser\u00E3o ignoradas."},
        {"multiple.commands.1.2", "Somente um comando \u00E9 permitido: tanto %1$s quanto %2$s foram especificados."},
        {"Use.keytool.help.for.all.available.commands",
                 "Use \"keytool -help\" para todos os comandos dispon\u00EDveis"},
        {"Key.and.Certificate.Management.Tool",
                 "Ferramenta de Gerenciamento de Chave e Certificado"},
        {"Commands.", "Comandos:"},
        {"Use.keytool.command.name.help.for.usage.of.command.name",
                "Utilize \"keytool -command_name -help\" para uso de command_name.\nUtilize a op\u00E7\u00E3o -conf <url> para especificar um arquivo de op\u00E7\u00F5es pr\u00E9-configurado."},
        // keytool: help: commands
        {"Generates.a.certificate.request",
                "Gera uma solicita\u00E7\u00E3o de certificado"}, //-certreq
        {"Changes.an.entry.s.alias",
                "Altera um alias de entrada"}, //-changealias
        {"Deletes.an.entry",
                "Exclui uma entrada"}, //-delete
        {"Exports.certificate",
                "Exporta o certificado"}, //-exportcert
        {"Generates.a.key.pair",
                "Gera um par de chaves"}, //-genkeypair
        {"Generates.a.secret.key",
                "Gera uma chave secreta"}, //-genseckey
        {"Generates.certificate.from.a.certificate.request",
                "Gera um certificado de uma solicita\u00E7\u00E3o de certificado"}, //-gencert
        {"Generates.CRL", "Gera CRL"}, //-gencrl
        {"Generated.keyAlgName.secret.key",
                "Chave secreta {0} gerada"}, //-genseckey
        {"Generated.keysize.bit.keyAlgName.secret.key",
                "Chave secreta {1} de {0} bits gerada"}, //-genseckey
        {"Imports.entries.from.a.JDK.1.1.x.style.identity.database",
                "Importa entradas de um banco de dados de identidade JDK 1.1.x-style"}, //-identitydb
        {"Imports.a.certificate.or.a.certificate.chain",
                "Importa um certificado ou uma cadeia de certificados"}, //-importcert
        {"Imports.a.password",
                "Importa uma senha"}, //-importpass
        {"Imports.one.or.all.entries.from.another.keystore",
                "Importa uma ou todas as entradas de outra \u00E1rea de armazenamento de chaves"}, //-importkeystore
        {"Clones.a.key.entry",
                "Clona uma entrada de chave"}, //-keyclone
        {"Changes.the.key.password.of.an.entry",
                "Altera a senha da chave de uma entrada"}, //-keypasswd
        {"Lists.entries.in.a.keystore",
                "Lista entradas em uma \u00E1rea de armazenamento de chaves"}, //-list
        {"Prints.the.content.of.a.certificate",
                "Imprime o conte\u00FAdo de um certificado"}, //-printcert
        {"Prints.the.content.of.a.certificate.request",
                "Imprime o conte\u00FAdo de uma solicita\u00E7\u00E3o de certificado"}, //-printcertreq
        {"Prints.the.content.of.a.CRL.file",
                "Imprime o conte\u00FAdo de um arquivo CRL"}, //-printcrl
        {"Generates.a.self.signed.certificate",
                "Gera um certificado autoassinado"}, //-selfcert
        {"Changes.the.store.password.of.a.keystore",
                "Altera a senha de armazenamento de uma \u00E1rea de armazenamento de chaves"}, //-storepasswd
        // keytool: help: options
        {"alias.name.of.the.entry.to.process",
                "nome do alias da entrada a ser processada"}, //-alias
        {"destination.alias",
                "alias de destino"}, //-destalias
        {"destination.key.password",
                "senha da chave de destino"}, //-destkeypass
        {"destination.keystore.name",
                "nome da \u00E1rea de armazenamento de chaves de destino"}, //-destkeystore
        {"destination.keystore.password.protected",
                "senha protegida da \u00E1rea de armazenamento de chaves de destino"}, //-destprotected
        {"destination.keystore.provider.name",
                "nome do fornecedor da \u00E1rea de armazenamento de chaves de destino"}, //-destprovidername
        {"destination.keystore.password",
                "senha da \u00E1rea de armazenamento de chaves de destino"}, //-deststorepass
        {"destination.keystore.type",
                "tipo de \u00E1rea de armazenamento de chaves de destino"}, //-deststoretype
        {"distinguished.name",
                "nome distinto"}, //-dname
        {"X.509.extension",
                "extens\u00E3o X.509"}, //-ext
        {"output.file.name",
                "nome do arquivo de sa\u00EDda"}, //-file and -outfile
        {"input.file.name",
                "nome do arquivo de entrada"}, //-file and -infile
        {"key.algorithm.name",
                "nome do algoritmo da chave"}, //-keyalg
        {"key.password",
                "senha da chave"}, //-keypass
        {"key.bit.size",
                "tamanho do bit da chave"}, //-keysize
        {"keystore.name",
                "nome da \u00E1rea de armazenamento de chaves"}, //-keystore
        {"access.the.cacerts.keystore",
                "acessar a \u00E1rea de armazenamento de chaves cacerts"}, // -cacerts
        {"warning.cacerts.option",
                "Advert\u00EAncia: use a op\u00E7\u00E3o -cacerts para acessar a \u00E1rea de armazenamento de chaves cacerts"},
        {"new.password",
                "nova senha"}, //-new
        {"do.not.prompt",
                "n\u00E3o perguntar"}, //-noprompt
        {"password.through.protected.mechanism",
                "senha por meio de mecanismo protegido"}, //-protected

        // The following 2 values should span 2 lines, the first for the
        // option itself, the second for its -providerArg value.
        {"addprovider.option",
                "adicionar provedor de seguran\u00E7a por nome (por exemplo, SunPKCS11)\nconfigurar argumento para -addprovider"}, //-addprovider
        {"provider.class.option",
                "adicionar provedor de seguran\u00E7a por nome de classe totalmente qualificado\nconfigurar argumento para -providerclass"}, //-providerclass

        {"provider.name",
                "nome do fornecedor"}, //-providername
        {"provider.classpath",
                "classpath do fornecedor"}, //-providerpath
        {"output.in.RFC.style",
                "sa\u00EDda no estilo RFC"}, //-rfc
        {"signature.algorithm.name",
                "nome do algoritmo de assinatura"}, //-sigalg
        {"source.alias",
                "alias de origem"}, //-srcalias
        {"source.key.password",
                "senha da chave de origem"}, //-srckeypass
        {"source.keystore.name",
                "nome da \u00E1rea de armazenamento de chaves de origem"}, //-srckeystore
        {"source.keystore.password.protected",
                "senha protegida da \u00E1rea de armazenamento de chaves de origem"}, //-srcprotected
        {"source.keystore.provider.name",
                "nome do fornecedor da \u00E1rea de armazenamento de chaves de origem"}, //-srcprovidername
        {"source.keystore.password",
                "senha da \u00E1rea de armazenamento de chaves de origem"}, //-srcstorepass
        {"source.keystore.type",
                "tipo de \u00E1rea de armazenamento de chaves de origem"}, //-srcstoretype
        {"SSL.server.host.and.port",
                "porta e host do servidor SSL"}, //-sslserver
        {"signed.jar.file",
                "arquivo jar assinado"}, //=jarfile
        {"certificate.validity.start.date.time",
                "data/hora inicial de validade do certificado"}, //-startdate
        {"keystore.password",
                "senha da \u00E1rea de armazenamento de chaves"}, //-storepass
        {"keystore.type",
                "tipo de \u00E1rea de armazenamento de chaves"}, //-storetype
        {"trust.certificates.from.cacerts",
                "certificados confi\u00E1veis do cacerts"}, //-trustcacerts
        {"verbose.output",
                "sa\u00EDda detalhada"}, //-v
        {"validity.number.of.days",
                "n\u00FAmero de dias da validade"}, //-validity
        {"Serial.ID.of.cert.to.revoke",
                 "ID de s\u00E9rie do certificado a ser revogado"}, //-id
        // keytool: Running part
        {"keytool.error.", "erro de keytool: "},
        {"Illegal.option.", "Op\u00E7\u00E3o inv\u00E1lida:  "},
        {"Illegal.value.", "Valor inv\u00E1lido: "},
        {"Unknown.password.type.", "Tipo de senha desconhecido: "},
        {"Cannot.find.environment.variable.",
                "N\u00E3o \u00E9 poss\u00EDvel localizar a vari\u00E1vel do ambiente: "},
        {"Cannot.find.file.", "N\u00E3o \u00E9 poss\u00EDvel localizar o arquivo: "},
        {"Command.option.flag.needs.an.argument.", "A op\u00E7\u00E3o de comando {0} precisa de um argumento."},
        {"Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value.",
                "Advert\u00EAncia: Senhas de chave e de armazenamento diferentes n\u00E3o suportadas para KeyStores PKCS12. Ignorando valor {0} especificado pelo usu\u00E1rio."},
        {"the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option",
            "A op\u00E7\u00E3o -keystore ou -storetype n\u00E3o pode ser usada com a op\u00E7\u00E3o -cacerts"},
        {".keystore.must.be.NONE.if.storetype.is.{0}",
                "-keystore deve ser NONE se -storetype for {0}"},
        {"Too.many.retries.program.terminated",
                 "Excesso de tentativas de repeti\u00E7\u00E3o; programa finalizado"},
        {".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}",
                "comandos -storepasswd e -keypasswd n\u00E3o suportados se -storetype for {0}"},
        {".keypasswd.commands.not.supported.if.storetype.is.PKCS12",
                "comandos -keypasswd n\u00E3o suportados se -storetype for PKCS12"},
        {".keypass.and.new.can.not.be.specified.if.storetype.is.{0}",
                "-keypass e -new n\u00E3o podem ser especificados se -storetype for {0}"},
        {"if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified",
                "se -protected for especificado, ent\u00E3o -storepass, -keypass e -new n\u00E3o dever\u00E3o ser especificados"},
        {"if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "se -srcprotected for especificado, ent\u00E3o -srcstorepass e -srckeypass n\u00E3o dever\u00E3o ser especificados"},
        {"if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified",
                "se a \u00E1rea de armazenamento de chaves n\u00E3o estiver protegida por senha, ent\u00E3o -storepass, -keypass e -new n\u00E3o dever\u00E3o ser especificados"},
        {"if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified",
                "se a \u00E1rea de armazenamento de chaves de origem n\u00E3o estiver protegida por senha, ent\u00E3o -srcstorepass e -srckeypass n\u00E3o dever\u00E3o ser especificados"},
        {"Illegal.startdate.value", "valor da data inicial inv\u00E1lido"},
        {"Validity.must.be.greater.than.zero",
                "A validade deve ser maior do que zero"},
        {"provclass.not.a.provider", "%s n\u00E3o \u00E9 um fornecedor"},
        {"provider.name.not.found", "O fornecedor chamado \"%s\" n\u00E3o foi encontrado"},
        {"provider.class.not.found", "Fornecedor \"%s\" n\u00E3o encontrado"},
        {"Usage.error.no.command.provided", "Erro de uso: nenhum comando fornecido"},
        {"Source.keystore.file.exists.but.is.empty.", "O arquivo da \u00E1rea de armazenamento de chaves de origem existe, mas est\u00E1 vazio: "},
        {"Please.specify.srckeystore", "Especifique -srckeystore"},
        {"Must.not.specify.both.v.and.rfc.with.list.command",
                "N\u00E3o devem ser especificados -v e -rfc com o comando 'list'"},
        {"Key.password.must.be.at.least.6.characters",
                "A senha da chave deve ter, no m\u00EDnimo, 6 caracteres"},
        {"New.password.must.be.at.least.6.characters",
                "A nova senha deve ter, no m\u00EDnimo, 6 caracteres"},
        {"Keystore.file.exists.but.is.empty.",
                "O arquivo da \u00E1rea de armazenamento de chaves existe, mas est\u00E1 vazio: "},
        {"Keystore.file.does.not.exist.",
                "O arquivo da \u00E1rea de armazenamento de chaves n\u00E3o existe. "},
        {"Must.specify.destination.alias", "Deve ser especificado um alias de destino"},
        {"Must.specify.alias", "Deve ser especificado um alias"},
        {"Keystore.password.must.be.at.least.6.characters",
                "A senha da \u00E1rea de armazenamento de chaves deve ter, no m\u00EDnimo, 6 caracteres"},
        {"Enter.the.password.to.be.stored.",
                "Digite a senha a ser armazenada:  "},
        {"Enter.keystore.password.", "Informe a senha da \u00E1rea de armazenamento de chaves:  "},
        {"Enter.source.keystore.password.", "Informe a senha da \u00E1rea de armazenamento de chaves de origem:  "},
        {"Enter.destination.keystore.password.", "Informe a senha da \u00E1rea de armazenamento de chaves de destino:  "},
        {"Keystore.password.is.too.short.must.be.at.least.6.characters",
         "A senha da \u00E1rea de armazenamento de chaves \u00E9 muito curta - ela deve ter, no m\u00EDnimo, 6 caracteres"},
        {"Unknown.Entry.Type", "Tipo de Entrada Desconhecido"},
        {"Too.many.failures.Alias.not.changed", "Excesso de falhas. Alias n\u00E3o alterado"},
        {"Entry.for.alias.alias.successfully.imported.",
                 "Entrada do alias {0} importada com \u00EAxito."},
        {"Entry.for.alias.alias.not.imported.", "Entrada do alias {0} n\u00E3o importada."},
        {"Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported.",
                 "Problema ao importar a entrada do alias {0}: {1}.\nEntrada do alias {0} n\u00E3o importada."},
        {"Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled",
                 "Comando de importa\u00E7\u00E3o conclu\u00EDdo:  {0} entradas importadas com \u00EAxito, {1} entradas falharam ou foram canceladas"},
        {"Warning.Overwriting.existing.alias.alias.in.destination.keystore",
                 "Advert\u00EAncia: Substitui\u00E7\u00E3o do alias {0} existente na \u00E1rea de armazenamento de chaves de destino"},
        {"Existing.entry.alias.alias.exists.overwrite.no.",
                 "Entrada j\u00E1 existente no alias {0}, substituir? [n\u00E3o]:  "},
        {"Too.many.failures.try.later", "Excesso de falhas - tente mais tarde"},
        {"Certification.request.stored.in.file.filename.",
                "Solicita\u00E7\u00E3o de certificado armazenada no arquivo <{0}>"},
        {"Submit.this.to.your.CA", "Submeter \u00E0 CA"},
        {"if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified",
            "se o alias n\u00E3o estiver especificado, destalias e srckeypass n\u00E3o dever\u00E3o ser especificados"},
        {"The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified.",
            "O armazenamento de chaves pkcs12 de destino tem storepass e keypass diferentes. Tente novamente especificando -destkeypass."},
        {"Certificate.stored.in.file.filename.",
                "Certificado armazenado no arquivo <{0}>"},
        {"Certificate.reply.was.installed.in.keystore",
                "A resposta do certificado foi instalada na \u00E1rea de armazenamento de chaves"},
        {"Certificate.reply.was.not.installed.in.keystore",
                "A resposta do certificado n\u00E3o foi instalada na \u00E1rea de armazenamento de chaves"},
        {"Certificate.was.added.to.keystore",
                "O certificado foi adicionado \u00E0 \u00E1rea de armazenamento de chaves"},
        {"Certificate.was.not.added.to.keystore",
                "O certificado n\u00E3o foi adicionado \u00E0 \u00E1rea de armazenamento de chaves"},
        {".Storing.ksfname.", "[Armazenando {0}]"},
        {"alias.has.no.public.key.certificate.",
                "{0} n\u00E3o tem chave p\u00FAblica (certificado)"},
        {"Cannot.derive.signature.algorithm",
                "N\u00E3o \u00E9 poss\u00EDvel obter um algoritmo de assinatura"},
        {"Alias.alias.does.not.exist",
                "O alias <{0}> n\u00E3o existe"},
        {"Alias.alias.has.no.certificate",
                "O alias <{0}> n\u00E3o tem certificado"},
        {"Key.pair.not.generated.alias.alias.already.exists",
                "Par de chaves n\u00E3o gerado; o alias <{0}> j\u00E1 existe"},
        {"Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for",
                "Gerando o par de chaves {1} de {0} bit e o certificado autoassinado ({2}) com uma validade de {3} dias\n\tpara: {4}"},
        {"Enter.key.password.for.alias.", "Informar a senha da chave de <{0}>"},
        {".RETURN.if.same.as.keystore.password.",
                "\t(RETURN se for igual \u00E0 senha da \u00E1rea do armazenamento de chaves):  "},
        {"Key.password.is.too.short.must.be.at.least.6.characters",
                "A senha da chave \u00E9 muito curta - deve ter, no m\u00EDnimo, 6 caracteres"},
        {"Too.many.failures.key.not.added.to.keystore",
                "Excesso de falhas - chave n\u00E3o adicionada a \u00E1rea de armazenamento de chaves"},
        {"Destination.alias.dest.already.exists",
                "O alias de destino <{0}> j\u00E1 existe"},
        {"Password.is.too.short.must.be.at.least.6.characters",
                "A senha \u00E9 muito curta - deve ter, no m\u00EDnimo, 6 caracteres"},
        {"Too.many.failures.Key.entry.not.cloned",
                "Excesso de falhas. Entrada da chave n\u00E3o clonada"},
        {"key.password.for.alias.", "senha da chave de <{0}>"},
        {"Keystore.entry.for.id.getName.already.exists",
                "A entrada da \u00E1rea do armazenamento de chaves de <{0}> j\u00E1 existe"},
        {"Creating.keystore.entry.for.id.getName.",
                "Criando entrada da \u00E1rea do armazenamento de chaves para <{0}> ..."},
        {"No.entries.from.identity.database.added",
                "Nenhuma entrada adicionada do banco de dados de identidades"},
        {"Alias.name.alias", "Nome do alias: {0}"},
        {"Creation.date.keyStore.getCreationDate.alias.",
                "Data de cria\u00E7\u00E3o: {0,date}"},
        {"alias.keyStore.getCreationDate.alias.",
                "{0}, {1,date}, "},
        {"alias.", "{0}, "},
        {"Entry.type.type.", "Tipo de entrada: {0}"},
        {"Certificate.chain.length.", "Comprimento da cadeia de certificados: "},
        {"Certificate.i.1.", "Certificado[{0,number,integer}]:"},
        {"Certificate.fingerprint.SHA.256.", "Fingerprint (SHA-256) do certificado: "},
        {"Keystore.type.", "Tipo de \u00E1rea de armazenamento de chaves: "},
        {"Keystore.provider.", "Fornecedor da \u00E1rea de armazenamento de chaves: "},
        {"Your.keystore.contains.keyStore.size.entry",
                "Sua \u00E1rea de armazenamento de chaves cont\u00E9m {0,number,integer} entrada"},
        {"Your.keystore.contains.keyStore.size.entries",
                "Sua \u00E1rea de armazenamento de chaves cont\u00E9m {0,number,integer} entradas"},
        {"Failed.to.parse.input", "Falha durante o parsing da entrada"},
        {"Empty.input", "Entrada vazia"},
        {"Not.X.509.certificate", "N\u00E3o \u00E9 um certificado X.509"},
        {"alias.has.no.public.key", "{0} n\u00E3o tem chave p\u00FAblica"},
        {"alias.has.no.X.509.certificate", "{0} n\u00E3o tem certificado X.509"},
        {"New.certificate.self.signed.", "Novo certificado (autoassinado):"},
        {"Reply.has.no.certificates", "A resposta n\u00E3o tem certificado"},
        {"Certificate.not.imported.alias.alias.already.exists",
                "Certificado n\u00E3o importado, o alias <{0}> j\u00E1 existe"},
        {"Input.not.an.X.509.certificate", "A entrada n\u00E3o \u00E9 um certificado X.509"},
        {"Certificate.already.exists.in.keystore.under.alias.trustalias.",
                "O certificado j\u00E1 existe no armazenamento de chaves no alias <{0}>"},
        {"Do.you.still.want.to.add.it.no.",
                "Ainda deseja adicion\u00E1-lo? [n\u00E3o]:  "},
        {"Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias.",
                "O certificado j\u00E1 existe na \u00E1rea de armazenamento de chaves da CA em todo o sistema no alias <{0}>"},
        {"Do.you.still.want.to.add.it.to.your.own.keystore.no.",
                "Ainda deseja adicion\u00E1-lo \u00E0 sua \u00E1rea de armazenamento de chaves? [n\u00E3o]:  "},
        {"Trust.this.certificate.no.", "Confiar neste certificado? [n\u00E3o]:  "},
        {"YES", "SIM"},
        {"New.prompt.", "Nova {0}: "},
        {"Passwords.must.differ", "As senhas devem ser diferentes"},
        {"Re.enter.new.prompt.", "Informe novamente a nova {0}: "},
        {"Re.enter.password.", "Redigite a senha: "},
        {"Re.enter.new.password.", "Informe novamente a nova senha: "},
        {"They.don.t.match.Try.again", "Elas n\u00E3o correspondem. Tente novamente"},
        {"Enter.prompt.alias.name.", "Informe o nome do alias {0}:  "},
        {"Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry.",
                 "Informe o novo nome do alias\t(RETURN para cancelar a importa\u00E7\u00E3o desta entrada):  "},
        {"Enter.alias.name.", "Informe o nome do alias:  "},
        {".RETURN.if.same.as.for.otherAlias.",
                "\t(RETURN se for igual ao de <{0}>)"},
        {"What.is.your.first.and.last.name.",
                "Qual \u00E9 o seu nome e o seu sobrenome?"},
        {"What.is.the.name.of.your.organizational.unit.",
                "Qual \u00E9 o nome da sua unidade organizacional?"},
        {"What.is.the.name.of.your.organization.",
                "Qual \u00E9 o nome da sua empresa?"},
        {"What.is.the.name.of.your.City.or.Locality.",
                "Qual \u00E9 o nome da sua Cidade ou Localidade?"},
        {"What.is.the.name.of.your.State.or.Province.",
                "Qual \u00E9 o nome do seu Estado ou Munic\u00EDpio?"},
        {"What.is.the.two.letter.country.code.for.this.unit.",
                "Quais s\u00E3o as duas letras do c\u00F3digo do pa\u00EDs desta unidade?"},
        {"Is.name.correct.", "{0} Est\u00E1 correto?"},
        {"no", "n\u00E3o"},
        {"yes", "sim"},
        {"y", "s"},
        {".defaultValue.", "  [{0}]:  "},
        {"Alias.alias.has.no.key",
                "O alias <{0}> n\u00E3o tem chave"},
        {"Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key",
                 "O alias <{0}> faz refer\u00EAncia a um tipo de entrada que n\u00E3o \u00E9 uma entrada de chave privada. O comando -keyclone oferece suporte somente \u00E0 clonagem de entradas de chave privada"},

        {".WARNING.WARNING.WARNING.",
            "*****************  WARNING WARNING WARNING  *****************"},
        {"Signer.d.", "Signat\u00E1rio #%d:"},
        {"Timestamp.", "Timestamp:"},
        {"Signature.", "Assinatura:"},
        {"CRLs.", "CRLs:"},
        {"Certificate.owner.", "Propriet\u00E1rio do certificado: "},
        {"Not.a.signed.jar.file", "N\u00E3o \u00E9 um arquivo jar assinado"},
        {"No.certificate.from.the.SSL.server",
                "N\u00E3o \u00E9 um certificado do servidor SSL"},

        {".The.integrity.of.the.information.stored.in.your.keystore.",
            "* A integridade das informa\u00E7\u00F5es armazenadas na sua \u00E1rea de armazenamento de chaves  *\n* N\u00C3O foi verificada!  Para que seja poss\u00EDvel verificar sua integridade, *\n* voc\u00EA deve fornecer a senha da \u00E1rea de armazenamento de chaves.                  *"},
        {".The.integrity.of.the.information.stored.in.the.srckeystore.",
            "* A integridade das informa\u00E7\u00F5es armazenadas no srckeystore  *\n* N\u00C3O foi verificada!  Para que seja poss\u00EDvel verificar sua integridade, *\n* voc\u00EA deve fornecer a senha do srckeystore.                  *"},

        {"Certificate.reply.does.not.contain.public.key.for.alias.",
                "A resposta do certificado n\u00E3o cont\u00E9m a chave p\u00FAblica de <{0}>"},
        {"Incomplete.certificate.chain.in.reply",
                "Cadeia de certificados incompleta na resposta"},
        {"Certificate.chain.in.reply.does.not.verify.",
                "A cadeia de certificados da resposta n\u00E3o verifica: "},
        {"Top.level.certificate.in.reply.",
                "Certificado de n\u00EDvel superior na resposta:\n"},
        {".is.not.trusted.", "... n\u00E3o \u00E9 confi\u00E1vel. "},
        {"Install.reply.anyway.no.", "Instalar resposta assim mesmo? [n\u00E3o]:  "},
        {"NO", "N\u00C3O"},
        {"Public.keys.in.reply.and.keystore.don.t.match",
                "As chaves p\u00FAblicas da resposta e da \u00E1rea de armazenamento de chaves n\u00E3o correspondem"},
        {"Certificate.reply.and.certificate.in.keystore.are.identical",
                "O certificado da resposta e o certificado da \u00E1rea de armazenamento de chaves s\u00E3o id\u00EAnticos"},
        {"Failed.to.establish.chain.from.reply",
                "Falha ao estabelecer a cadeia a partir da resposta"},
        {"n", "n"},
        {"Wrong.answer.try.again", "Resposta errada; tente novamente"},
        {"Secret.key.not.generated.alias.alias.already.exists",
                "Chave secreta n\u00E3o gerada; o alias <{0}> j\u00E1 existe"},
        {"Please.provide.keysize.for.secret.key.generation",
                "Forne\u00E7a o -keysize para a gera\u00E7\u00E3o da chave secreta"},

        {"warning.not.verified.make.sure.keystore.is.correct",
            "ADVERT\u00CANCIA: n\u00E3o verificado. Certifique-se que -keystore esteja correto."},

        {"Extensions.", "Extens\u00F5es: "},
        {".Empty.value.", "(Valor vazio)"},
        {"Extension.Request.", "Solicita\u00E7\u00E3o de Extens\u00E3o:"},
        {"Unknown.keyUsage.type.", "Tipo de keyUsage desconhecido: "},
        {"Unknown.extendedkeyUsage.type.", "Tipo de extendedkeyUsage desconhecido: "},
        {"Unknown.AccessDescription.type.", "Tipo de AccessDescription desconhecido: "},
        {"Unrecognized.GeneralName.type.", "Tipo de GeneralName n\u00E3o reconhecido: "},
        {"This.extension.cannot.be.marked.as.critical.",
                 "Esta extens\u00E3o n\u00E3o pode ser marcada como cr\u00EDtica. "},
        {"Odd.number.of.hex.digits.found.", "Encontrado n\u00FAmero \u00EDmpar de seis d\u00EDgitos: "},
        {"Unknown.extension.type.", "Tipo de extens\u00E3o desconhecido: "},
        {"command.{0}.is.ambiguous.", "o comando {0} \u00E9 amb\u00EDguo:"},

        // 8171319: keytool should print out warnings when reading or
        // generating cert/cert req using weak algorithms
        {"the.certificate.request", "A solicita\u00E7\u00E3o do certificado"},
        {"the.issuer", "O emissor"},
        {"the.generated.certificate", "O certificado gerado"},
        {"the.generated.crl", "A CRL gerada"},
        {"the.generated.certificate.request", "A solicita\u00E7\u00E3o do certificado gerada"},
        {"the.certificate", "O certificado"},
        {"the.crl", "A CRL"},
        {"the.tsa.certificate", "O certificado TSA"},
        {"the.input", "A entrada"},
        {"reply", "Resposta"},
        {"one.in.many", "%1$s #%2$d de %3$d"},
        {"alias.in.cacerts", "Emissor <%s> no cacerts"},
        {"alias.in.keystore", "Emissor <%s>"},
        {"with.weak", "%s (fraca)"},
        {"key.bit", "Chave %2$s de %1$d bits"},
        {"key.bit.weak", "Chave %2$s de %1$d bits (fraca)"},
        {"unknown.size.1", "chave de tamanho desconhecido  %s"},
        {".PATTERN.printX509Cert.with.weak",
                "Propriet\u00E1rio: {0}\nEmissor: {1}\nN\u00FAmero de s\u00E9rie: {2}\nV\u00E1lido de: {3} at\u00E9: {4}\nFingerprints do certificado:\n\t SHA1: {5}\n\t SHA256: {6}\nNome do algoritmo de assinatura: {7}\nAlgoritmo de Chave P\u00FAblica do Assunto: {8}\nVers\u00E3o: {9}"},
        {"PKCS.10.with.weak",
                "Solicita\u00E7\u00E3o do Certificado PKCS #10 (Vers\u00E3o 1.0)\nAssunto: %1$s\nFormato: %2$s\nChave P\u00FAblica: %3$s\nAlgoritmo de assinatura: %4$s\n"},
        {"verified.by.s.in.s.weak", "Verificado por %1$s em %2$s com um %3$s"},
        {"whose.sigalg.risk", "%1$s usa o algoritmo de assinatura %2$s que \u00E9 considerado um risco \u00E0 seguran\u00E7a."},
        {"whose.key.risk", "%1$s usa um %2$s que \u00E9 considerado um risco \u00E0 seguran\u00E7a."},
        {"jks.storetype.warning", "O armazenamento de chaves %1$s usa um formato propriet\u00E1rio. \u00C9 recomendada a migra\u00E7\u00E3o para PKCS12, que \u00E9 um formato de padr\u00E3o industrial que usa \"keytool -importkeystore -srckeystore %2$s -destkeystore %2$s -deststoretype pkcs12\"."},
        {"migrate.keystore.warning", "\"%1$s\" foi migrado para %4$s. O backup do armazenamento de chaves %2$s \u00E9 feito como \"%3$s\"."},
        {"backup.keystore.warning", "O backup do armazenamento de chaves original \"%1$s\" \u00E9 feito como \"%3$s\"..."},
        {"importing.keystore.status", "Importando armazenamento de chaves %1$s to %2$s..."},
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
