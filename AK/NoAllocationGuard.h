/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Noncopyable.h>

#if defined(KERNEL)
#    include <Kernel/Arch/Processor.h>
#    include <Kernel/Heap/kmalloc.h>
#else
#    include <LibC/sys/internals.h>
#endif

namespace AK {

class NoAllocationGuard {
    AK_MAKE_NONCOPYABLE(NoAllocationGuard);
    AK_MAKE_NONMOVABLE(NoAllocationGuard);

public:
    NoAllocationGuard()
        : m_allocation_enabled_previously(set_thread_allocation_state(false))
    {
    }

    ~NoAllocationGuard()
    {
        set_thread_allocation_state(m_allocation_enabled_previously);
    }

private:
    static bool set_thread_allocation_state(bool value)
    {
#if defined(KERNEL)
        auto old_state = Processor::current_thread()->get_allocation_enabled();
        Processor::current_thread()->set_allocation_enabled(value);
        return old_state;
#elif defined(AK_OS_SERENITY)
        return __set_allocation_enabled(value);
#else
        (void)value;
        return true;
#endif
    }

    bool m_allocation_enabled_previously { true };
};

}

#if USING_AK_GLOBALLY
using AK::NoAllocationGuard;
#endif
