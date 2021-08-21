/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Thread.h>

namespace Kernel {

class WaitQueue : public Thread::BlockCondition {
public:
    u32 wake_one();
    u32 wake_n(u32 wake_count);
    u32 wake_all();

    void should_block(bool block)
    {
        ScopedSpinlock lock(m_lock);
        m_should_block = block;
    }

    template<class... Args>
    Thread::BlockResult wait_on(const Thread::BlockTimeout& timeout, Args&&... args)
    {
        return Thread::current()->block<Thread::QueueBlocker>(timeout, *this, forward<Args>(args)...);
    }

    template<class... Args>
    void wait_forever(Args&&... args)
    {
        (void)Thread::current()->block<Thread::QueueBlocker>({}, *this, forward<Args>(args)...);
    }

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override;

private:
    bool m_wake_requested { false };
    bool m_should_block { true };
};

}
