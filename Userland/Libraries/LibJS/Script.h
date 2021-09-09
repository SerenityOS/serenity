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
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 16.1.4 Script Records, https://tc39.es/ecma262/#sec-script-records
class Script : public RefCounted<Script> {
public:
    ~Script();
    static NonnullRefPtr<Script> create(GlobalObject&, NonnullRefPtr<ASTNode> parse_node);

    GlobalObject& global_object() { return *m_global_object.cell(); }
    ASTNode const& parse_node() const { return *m_parse_node; }

private:
    Script(GlobalObject&, NonnullRefPtr<ASTNode>);

    Handle<GlobalObject> m_global_object;
    NonnullRefPtr<ASTNode> m_parse_node;
};

}
