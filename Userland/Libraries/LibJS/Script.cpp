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
NonnullRefPtr<Script> Script::parse(StringView source_text, GlobalObject& global_object)
{
    // 1. Let body be ParseText(sourceText, Script).
    auto body = Parser(Lexer(source_text)).parse_program();

    // FIXME: 2. If body is a List of errors, return body.

    // 3. Return Script Record { [[Realm]]: realm, [[ECMAScriptCode]]: body, [[HostDefined]]: hostDefined }.
    return adopt_ref(*new Script(global_object, move(body)));
}

Script::Script(GlobalObject& global_object, NonnullRefPtr<Program> parse_node)
    : m_global_object(make_handle(&global_object))
    , m_parse_node(move(parse_node))
{
}

Script::~Script()
{
}

}
