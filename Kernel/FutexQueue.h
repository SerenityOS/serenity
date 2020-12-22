/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
