/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

class WaitQueue final : public Thread::BlockerSet {
public:
    u32 wake_one();
    u32 wake_n(u32 wake_count);
    u32 wake_all();

    template<class... Args>
    Thread::BlockResult wait_on(Thread::BlockTimeout const& timeout, Args&&... args)
    {
        return Thread::current()->block<Thread::WaitQueueBlocker>(timeout, *this, forward<Args>(args)...);
    }

    template<class... Args>
    void wait_forever(Args&&... args)
    {
        (void)Thread::current()->block<Thread::WaitQueueBlocker>({}, *this, forward<Args>(args)...);
    }

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void*) override;

private:
    bool m_wake_requested { false };
};

}
