/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibJS/AST.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

// 16.1.4 Script Records, https://tc39.es/ecma262/#sec-script-records
class Script : public RefCounted<Script> {
public:
    ~Script();
    static NonnullRefPtr<Script> parse(StringView source_text, Realm&, StringView filename = {});

    Realm& realm() { return *m_realm.cell(); }
    Program const& parse_node() const { return *m_parse_node; }

private:
    Script(Realm&, NonnullRefPtr<Program>);

    // Handles are not safe unless we keep the VM alive.
    NonnullRefPtr<VM> m_vm;

    Handle<Realm> m_realm;               // [[Realm]]
    NonnullRefPtr<Program> m_parse_node; // [[ECMAScriptCode]]
};

}
