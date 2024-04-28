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

    // NOTE: Userland has way more use-cases than the Kernel, and
    // converting to use was_set() is not a sane thing to do for all of
    // these cases (at least not in a short period of time).
    // On the Kernel side, it is more manageable, so let's not allow to
    // go down that slope of not caring about the was_set() method there
    // anymore.
#ifndef KERNEL
    operator bool() const
    {
        return was_set();
    }
    bool operator!() const { return !was_set(); }
#else
    operator bool() const = delete;
    bool operator!() const = delete;
#endif

private:
    bool m_value { false };
};

}

#if USING_AK_GLOBALLY
using AK::SetOnce;
#endif
