/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace JS::Bytecode {

class Register {
public:
    explicit Register(u32 index)
        : m_index(index)
    {
    }

    u32 index() const { return m_index; }

private:
    u32 m_index { 0 };
};

}
