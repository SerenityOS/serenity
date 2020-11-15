/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

typedef u64 TimerId;

struct Timer : public RefCounted<Timer> {
    TimerId id;
    u64 expires;
    Function<void()> callback;
    bool operator<(const Timer& rhs) const
    {
        return expires < rhs.expires;
    }
    bool operator>(const Timer& rhs) const
    {
        return expires > rhs.expires;
    }
    bool operator==(const Timer& rhs) const
    {
        return id == rhs.id;
    }

    Timer(u64 expires, Function<void()>&& callback)
        : expires(expires)
        , callback(move(callback))
    {
    }
};

class TimerQueue {
public:
    TimerQueue();
    static TimerQueue& the();

    TimerId add_timer(NonnullRefPtr<Timer>&&);
    RefPtr<Timer> add_timer_without_id(const timespec& timeout, Function<void()>&& callback);
    TimerId add_timer(timeval& timeout, Function<void()>&& callback);
    bool cancel_timer(TimerId id);
    bool cancel_timer(const NonnullRefPtr<Timer>&);
    void fire();

private:
    void update_next_timer_due();
    void add_timer_locked(NonnullRefPtr<Timer>);

    timespec ticks_to_time(u64 ticks) const;
    u64 time_to_ticks(const timespec&) const;

    u64 m_next_timer_due { 0 };
    u64 m_timer_id_count { 0 };
    u64 m_ticks_per_second { 0 };
    SinglyLinkedList<NonnullRefPtr<Timer>> m_timer_queue;
};

}
