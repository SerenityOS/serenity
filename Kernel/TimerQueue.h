#pragma once

#include <Kernel/Arch/i386/PIT.h>

struct Timer {
    u64 id;
    u64 expires;
    void (*callback)();
    bool operator<(const Timer& rhs) const
    {
        return expires < rhs.expires;
    }
    bool operator>( const Timer& rhs) const
    {
        return expires > rhs.expires;
    }

    bool operator==( const X& rhs) const
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
friend class SinglyLinkedList;
public:
    static void initialize();
    static u64 add_timer(Timer&);
    static u64 add_timer(u64 duration, TimeUnit, void (callback)());
    static Timer& get_timer(u64 id);
    static bool update_timer(Timer&)
    static bool cancel_timer(u64 id);
private:
    static void fire_timer();
    static void sorted_list_insert_slow()
};