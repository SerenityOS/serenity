/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Script.h>

namespace JS {

NonnullRefPtr<Script> Script::create(GlobalObject& global_object, NonnullRefPtr<ASTNode> parse_node)
{
    return adopt_ref(*new Script(global_object, move(parse_node)));
}

Script::Script(GlobalObject& global_object, NonnullRefPtr<ASTNode> parse_node)
    : m_global_object(make_handle(&global_object))
    , m_parse_node(move(parse_node))
{
}

Script::~Script()
{
}

}
