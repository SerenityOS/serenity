/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Format.h>
#include <LibJS/Bytecode/BasicBlock.h>

namespace JS::Bytecode {

class Label {
public:
    explicit Label(BasicBlock const& block)
        : m_block(&block)
    {
    }

    auto& block() const { return *m_block; }

private:
    BasicBlock const* m_block { nullptr };
};

}

template<>
struct YAK::Formatter<JS::Bytecode::Label> : YAK::Formatter<FormatString> {
    void format(FormatBuilder& builder, JS::Bytecode::Label const& value)
    {
        return YAK::Formatter<FormatString>::format(builder, "@{}", value.block().name());
    }
};
