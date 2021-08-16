/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/RefCounted.h>
#include <Kernel/Locking/SpinLock.h>
#include <Kernel/Memory/VMObject.h>
#include <Kernel/Thread.h>

namespace Kernel {

class FutexQueue
    : public RefCounted<FutexQueue>
    , public Thread::BlockCondition {
public:
    FutexQueue();
    virtual ~FutexQueue();

    u32 wake_n_requeue(u32, const Function<FutexQueue*()>&, u32, bool&, bool&);
    u32 wake_n(u32, const Optional<u32>&, bool&);
    u32 wake_all(bool&);

    template<class... Args>
    Thread::BlockResult wait_on(const Thread::BlockTimeout& timeout, Args&&... args)
    {
        return Thread::current()->block<Thread::FutexBlocker>(timeout, *this, forward<Args>(args)...);
    }

    bool queue_imminent_wait();
    void did_remove();
    bool try_remove();

    bool is_empty_and_no_imminent_waits()
    {
        ScopedSpinLock lock(m_lock);
        return is_empty_and_no_imminent_waits_locked();
    }
    bool is_empty_and_no_imminent_waits_locked();

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override;

private:
    size_t m_imminent_waits { 1 }; // We only create this object if we're going to be waiting, so start out with 1
    bool m_was_removed { false };
};

}
