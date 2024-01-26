/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Types.h>

namespace AK {

class SetOnce {
    AK_MAKE_NONCOPYABLE(SetOnce);
    AK_MAKE_NONMOVABLE(SetOnce);

public:
    SetOnce() = default;

    void set()
    {
        m_value = true;
    }

    bool was_set() const { return m_value; }

private:
    bool m_value { false };
};

}

#if USING_AK_GLOBALLY
using AK::SetOnce;
#endif
