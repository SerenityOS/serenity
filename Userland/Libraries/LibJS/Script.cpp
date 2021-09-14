/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Script.h>

namespace JS {

// 16.1.5 ParseScript ( sourceText, realm, hostDefined ), https://tc39.es/ecma262/#sec-parse-script
Result<NonnullRefPtr<Script>, Vector<Parser::Error>> Script::parse(StringView source_text, Realm& realm, StringView filename)
{
    // 1. Let body be ParseText(sourceText, Script).
    auto parser = Parser(Lexer(source_text, filename));
    auto body = parser.parse_program();

    // 2. If body is a List of errors, return body.
    if (parser.has_errors())
        return parser.errors();

    // 3. Return Script Record { [[Realm]]: realm, [[ECMAScriptCode]]: body, [[HostDefined]]: hostDefined }.
    return adopt_ref(*new Script(realm, move(body)));
}

Script::Script(Realm& realm, NonnullRefPtr<Program> parse_node)
    : m_vm(realm.vm())
    , m_realm(make_handle(&realm))
    , m_parse_node(move(parse_node))
{
}

Script::~Script()
{
}

}
