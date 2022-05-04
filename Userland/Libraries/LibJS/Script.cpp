/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Script.h>

namespace JS {

// 16.1.5 ParseScript ( sourceText, realm, hostDefined ), https://tc39.es/ecma262/#sec-parse-script
Result<NonnullRefPtr<Script>, Vector<Parser::Error>> Script::parse(StringView source_text, Realm& realm, StringView filename, HostDefined* host_defined, size_t line_number_offset)
{
    // 1. Let script be ParseText(sourceText, Script).
    auto parser = Parser(Lexer(source_text, filename, line_number_offset));
    auto script = parser.parse_program();

    // 2. If script is a List of errors, return body.
    if (parser.has_errors())
        return parser.errors();

    // 3. Return Script Record { [[Realm]]: realm, [[ECMAScriptCode]]: script, [[HostDefined]]: hostDefined }.
    return adopt_ref(*new Script(realm, filename, move(script), host_defined));
}

Script::Script(Realm& realm, StringView filename, NonnullRefPtr<Program> parse_node, HostDefined* host_defined)
    : m_vm(realm.vm())
    , m_realm(make_handle(&realm))
    , m_parse_node(move(parse_node))
    , m_filename(filename)
    , m_host_defined(host_defined)
{
}

}
