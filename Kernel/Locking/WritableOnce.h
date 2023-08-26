/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>

namespace Kernel {

template<typename T>
class WritableOnce {
    AK_MAKE_NONCOPYABLE(WritableOnce);
    AK_MAKE_NONMOVABLE(WritableOnce);

public:
    template<typename... Args>
    WritableOnce(Args&&... args)
        : m_value(forward<Args>(args)...)
        , m_default_value(forward<Args>(args)...)
    {
    }

    ErrorOr<void> set(T value)
    {
        T default_value = m_default_value;
        if (!m_value.compare_exchange_strong(default_value, value))
            return Error::from_errno(EBUSY);
        return {};
    }

    T get() const { return m_value.load(); }

private:
    Atomic<T> m_value;
    T const m_default_value;
};

}
