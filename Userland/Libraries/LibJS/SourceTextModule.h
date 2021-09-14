/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Forward.h>
#include <LibJS/Module.h>
#include <LibJS/Parser.h>

namespace JS {

// 16.2.1.6 Source Text Module Records, https://tc39.es/ecma262/#sec-source-text-module-records
class SourceTextModule final : public Module {
public:
    static Result<NonnullRefPtr<SourceTextModule>, Vector<Parser::Error>> parse(StringView source_text, Realm&, StringView filename = {});
    virtual ~SourceTextModule();

private:
    explicit SourceTextModule(Realm&, NonnullRefPtr<Program>);

    NonnullRefPtr<Program> m_ecmascript_code; // [[ECMAScriptCode]]
};

}
