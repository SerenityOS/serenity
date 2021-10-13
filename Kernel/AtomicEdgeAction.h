/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/Arch/Processor.h>

namespace Kernel {

template<typename AtomicRefCountType>
class AtomicEdgeAction {
public:
    template<typename FirstRefAction>
    bool ref(FirstRefAction first_ref_action)
    {
        AtomicRefCountType expected = 0;
        AtomicRefCountType desired = (1 << 1) | 1;
        // Least significant bit indicates we're busy protecting/unprotecting
        for (;;) {
            if (m_atomic_ref_count.compare_exchange_strong(expected, desired, AK::memory_order_relaxed))
                break;

            Processor::wait_check();

            expected &= ~1;
            desired = expected + (1 << 1);
            VERIFY(desired > expected);
            if (expected == 0)
                desired |= 1;
        }

        atomic_thread_fence(AK::memory_order_acquire);

        if (expected == 0) {
            first_ref_action();

            // drop the busy flag
            m_atomic_ref_count.store(desired & ~1, AK::memory_order_release);
            return true;
        }
        return false;
    }

    template<typename LastRefAction>
    bool unref(LastRefAction last_ref_action)
    {
        AtomicRefCountType expected = 1 << 1;
        AtomicRefCountType desired = (1 << 1) | 1;
        // Least significant bit indicates we're busy protecting/unprotecting
        for (;;) {
            if (m_atomic_ref_count.compare_exchange_strong(expected, desired, AK::memory_order_relaxed))
                break;

            Processor::wait_check();

            expected &= ~1;
            VERIFY(expected != 0); // Someone should always have at least one reference

            if (expected == 1 << 1) {
                desired = (1 << 1) | 1;
            } else {
                desired = expected - (1 << 1);
                VERIFY(desired < expected);
            }
        }

        AK::atomic_thread_fence(AK::memory_order_release);

        if (expected == 1 << 1) {
            last_ref_action();

            // drop the busy flag and release reference
            m_atomic_ref_count.store(0, AK::memory_order_release);
            return true;
        }
        return false;
    }

private:
    Atomic<AtomicRefCountType> m_atomic_ref_count { 0 };
};

}
