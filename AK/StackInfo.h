/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

class StackInfo {
public:
    StackInfo();

    FlatPtr base() const { return m_base; }
    FlatPtr top() const { return m_top; }
    size_t size() const { return m_size; }
    size_t size_free() const
    {
        FlatPtr dummy;
        return reinterpret_cast<FlatPtr>(&dummy) - m_base;
    }

private:
    FlatPtr m_base;
    FlatPtr m_top;
    size_t m_size;
};

}

using AK::StackInfo;
