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
