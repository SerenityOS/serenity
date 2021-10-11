/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <Kernel/Arch/Processor.h>

namespace Kernel {

class ScopedCritical {
    AK_MAKE_NONCOPYABLE(ScopedCritical);

public:
    ScopedCritical()
    {
        enter();
    }

    ~ScopedCritical()
    {
        if (m_valid)
            leave();
    }

    ScopedCritical(ScopedCritical&& from)
        : m_valid(exchange(from.m_valid, false))
    {
    }

    ScopedCritical& operator=(ScopedCritical&& from)
    {
        if (&from != this) {
            m_valid = exchange(from.m_valid, false);
        }
        return *this;
    }

    void leave()
    {
        VERIFY(m_valid);
        m_valid = false;
        Processor::leave_critical();
    }

    void enter()
    {
        VERIFY(!m_valid);
        m_valid = true;
        Processor::enter_critical();
    }

private:
    bool m_valid { false };
};

}
