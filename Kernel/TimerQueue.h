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
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/Arch/i386/PIT.h>

struct Timer {
    u64 id;
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
};

enum TimeUnit {
    MS = TICKS_PER_SECOND / 1000,
    S = TICKS_PER_SECOND,
    M = TICKS_PER_SECOND * 60
};

class TimerQueue {
public:
    static TimerQueue& the();

    u64 add_timer(NonnullOwnPtr<Timer>&&);
    u64 add_timer(u64 duration, TimeUnit, Function<void()>&& callback);
    bool cancel_timer(u64 id);
    void fire();

private:
    void update_next_timer_due();

    u64 m_next_timer_due { 0 };
    u64 m_timer_id_count { 0 };
    SinglyLinkedList<NonnullOwnPtr<Timer>> m_timer_queue;
};
