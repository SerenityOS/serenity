/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/SourceTextModule.h>

namespace JS {

// 16.2.1.6.1 ParseModule ( sourceText, realm, hostDefined ), https://tc39.es/ecma262/#sec-parsemodule
Result<NonnullRefPtr<SourceTextModule>, Vector<Parser::Error>> SourceTextModule::parse(StringView source_text, Realm& realm, [[maybe_unused]] StringView filename)
{
    // 1. Let body be ParseText(sourceText, Module).
    auto parser = Parser(Lexer(source_text, filename), Program::Type::Module);
    auto body = parser.parse_program();

    // 2. If body is a List of errors, return body.
    if (parser.has_errors())
        return parser.errors();

    // FIXME: Implement the rest of ParseModule.
    return adopt_ref(*new SourceTextModule(realm, move(body)));
}

SourceTextModule::SourceTextModule(Realm& realm, NonnullRefPtr<Program> program)
    : Module(realm)
    , m_ecmascript_code(move(program))
{
}

SourceTextModule::~SourceTextModule()
{
}

}
