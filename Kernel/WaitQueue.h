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
#include <Kernel/SpinLock.h>
#include <Kernel/Thread.h>

namespace Kernel {

class WaitQueue : public Thread::BlockCondition {
public:
    u32 wake_one();
    u32 wake_n(u32 wake_count);
    u32 wake_all();

    void should_block(bool block)
    {
        ScopedSpinLock lock(m_lock);
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
