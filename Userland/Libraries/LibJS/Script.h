/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibJS/AST.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

// 16.1.4 Script Records, https://tc39.es/ecma262/#sec-script-records
class Script
    : public RefCounted<Script>
    , public Weakable<Script> {
public:
    struct HostDefined {
        virtual ~HostDefined() = default;
    };

    ~Script();
    static Result<NonnullRefPtr<Script>, Vector<Parser::Error>> parse(StringView source_text, Realm&, StringView filename = {}, HostDefined* = nullptr);

    Realm& realm() { return *m_realm.cell(); }
    Program const& parse_node() const { return *m_parse_node; }

    HostDefined* host_defined() { return m_host_defined; }
    StringView filename() const { return m_filename; }

private:
    Script(Realm&, StringView filename, NonnullRefPtr<Program>, HostDefined* = nullptr);
    // Handles are not safe unless we keep the VM alive.
    NonnullRefPtr<VM> m_vm;

    Handle<Realm> m_realm;               // [[Realm]]
    NonnullRefPtr<Program> m_parse_node; // [[ECMAScriptCode]]

    // Needed for potential lookups of modules.
    String m_filename;
    HostDefined* m_host_defined { nullptr }; // [[HostDefined]]
};

}
