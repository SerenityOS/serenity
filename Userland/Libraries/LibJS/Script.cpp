/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibJS/AST.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Script.h>

namespace JS {

// 16.1.5 ParseScript ( sourceText, realm, hostDefined ), https://tc39.es/ecma262/#sec-parse-script
NonnullRefPtr<Script> Script::parse(StringView source_text, Realm& realm, StringView filename)
{
    auto timer = Core::ElapsedTimer::start_new();
    ScopeGuard timer_guard([&] {
        dbgln("JS::Script: Parsed {} in {}ms", filename, timer.elapsed());
    });

    // 1. Let body be ParseText(sourceText, Script).
    auto body = Parser(Lexer(source_text, filename)).parse_program();

    // FIXME: 2. If body is a List of errors, return body.

    // 3. Return Script Record { [[Realm]]: realm, [[ECMAScriptCode]]: body, [[HostDefined]]: hostDefined }.
    return adopt_ref(*new Script(realm, move(body)));
}

Script::Script(Realm& realm, NonnullRefPtr<Program> parse_node)
    : m_realm(make_handle(&realm))
    , m_parse_node(move(parse_node))
{
}

Script::~Script()
{
}

}
