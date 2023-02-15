/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Rule.h>

namespace Web::CSS::Parser {

Rule::Rule(Rule::Type type, FlyString name, Vector<ComponentValue> prelude, RefPtr<Block> block)
    : m_type(type)
    , m_at_rule_name(move(name))
    , m_prelude(move(prelude))
    , m_block(move(block))
{
}

Rule::~Rule() = default;

}
