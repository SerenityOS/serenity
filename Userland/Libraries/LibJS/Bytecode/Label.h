/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace JS::Bytecode {

class Label {
public:
    explicit Label(size_t address)
        : m_address(address)
    {
    }

    size_t address() const { return m_address; }

private:
    size_t m_address { 0 };
};

}

template<>
struct AK::Formatter<JS::Bytecode::Label> : AK::Formatter<FormatString> {
    void format(FormatBuilder& builder, JS::Bytecode::Label const& value)
    {
        return AK::Formatter<FormatString>::format(builder, "@{:x}", value.address());
    }
};
