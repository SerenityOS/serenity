/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

class FutexQueue final
    : public AtomicRefCounted<FutexQueue>
    , public Thread::BlockerSet {
public:
    FutexQueue();
    virtual ~FutexQueue();

    ErrorOr<u32> wake_n_requeue(u32, Function<ErrorOr<FutexQueue*>()> const&, u32, bool&, bool&);
    u32 wake_n(u32, Optional<u32> const&, bool&);
    u32 wake_all(bool&);

    template<class... Args>
    Thread::BlockResult wait_on(Thread::BlockTimeout const& timeout, Args&&... args)
    {
        return Thread::current()->block<Thread::FutexBlocker>(timeout, *this, forward<Args>(args)...);
    }

    bool queue_imminent_wait();
    bool try_remove();

    bool is_empty_and_no_imminent_waits()
    {
        SpinlockLocker lock(m_lock);
        return is_empty_and_no_imminent_waits_locked();
    }
    bool is_empty_and_no_imminent_waits_locked();

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void*) override;

private:
    size_t m_imminent_waits { 1 }; // We only create this object if we're going to be waiting, so start out with 1
    bool m_was_removed { false };
};

}
