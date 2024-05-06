/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibJS/Bytecode/BasicBlock.h>

namespace JS::Bytecode {

class Label {
public:
    explicit Label(BasicBlock const& block)
        : m_block(&block)
    {
    }

    // Used while compiling.
    BasicBlock const& block() const { return *m_block; }

    // Used after compiling.
    size_t address() const { return m_address; }

    void set_address(size_t address) { m_address = address; }

private:
    union {
        // Relevant while compiling.
        BasicBlock const* m_block { nullptr };

        // Relevant after compiling.
        size_t m_address;
    };
};

}

template<>
struct AK::Formatter<JS::Bytecode::Label> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, JS::Bytecode::Label const& label)
    {
        return AK::Formatter<FormatString>::format(builder, "@{:x}"sv, label.address());
    }
};
