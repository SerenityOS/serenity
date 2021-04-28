/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/RefCounted.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class FutexQueue : public Thread::BlockCondition
    , public RefCounted<FutexQueue>
    , public VMObjectDeletedHandler {
public:
    FutexQueue(FlatPtr user_address_or_offset, VMObject* vmobject = nullptr);
    virtual ~FutexQueue();

    u32 wake_n_requeue(u32, const Function<FutexQueue*()>&, u32, bool&, bool&);
    u32 wake_n(u32, const Optional<u32>&, bool&);
    u32 wake_all(bool&);

    template<class... Args>
    Thread::BlockResult wait_on(const Thread::BlockTimeout& timeout, Args&&... args)
    {
        return Thread::current()->block<Thread::FutexBlocker>(timeout, *this, forward<Args>(args)...);
    }

    virtual void vmobject_deleted(VMObject&) override;

protected:
    virtual bool should_add_blocker(Thread::Blocker& b, void* data) override;

private:
    // For private futexes we just use the user space address.
    // But for global futexes we use the offset into the VMObject
    const FlatPtr m_user_address_or_offset;
    WeakPtr<VMObject> m_vmobject;
    const bool m_is_global;
};

}
