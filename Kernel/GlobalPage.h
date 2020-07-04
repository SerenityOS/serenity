/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Types.h>
#if defined(KERNEL)
#    include <Kernel/UnixTypes.h>
#else
#    include <limits.h>
#    include <stdint.h>
#    include <sys/time.h>
#endif

#if defined(KERNEL)
#    define global_page() \
        (*(reinterpret_cast<::GlobalPage*>(0xffe00000)))
#    define global_page_user() \
        FlatPtr(0x00800000 - PAGE_SIZE)
#else
#    define global_page() \
        (*(reinterpret_cast<const ::GlobalPage*>(0x007ff000)))
#endif

struct GlobalPage {
    volatile Atomic<u32> time_update1 { 0 };
    volatile time_t epoch_time { 0 };
    volatile time_t seconds_since_boot { 0 };
    volatile suseconds_t useconds { 0 };
    volatile Atomic<u32> time_update2 { 0 };

    inline timeval read_timeofday() const
    {
        timeval tv;
        u32 up1;
        do {
            up1 = time_update1.load(AK::MemoryOrder::memory_order_consume);
            tv.tv_sec = epoch_time;
            tv.tv_usec = useconds;
        } while (up1 != time_update2.load(AK::MemoryOrder::memory_order_consume));
        return tv;
    }

    inline timespec read_monotonic() const
    {
        timespec ts;
        u32 up1;
        do {
            up1 = time_update1.load(AK::MemoryOrder::memory_order_consume);
            ts.tv_sec = seconds_since_boot;
            ts.tv_nsec = useconds;
        } while (up1 != time_update2.load(AK::MemoryOrder::memory_order_consume));
        ts.tv_nsec *= 1000;
        return ts;
    }

    inline timespec read_realtime() const
    {
        timespec ts;
        u32 up1;
        do {
            up1 = time_update1.load(AK::MemoryOrder::memory_order_consume);
            ts.tv_sec = epoch_time;
            ts.tv_nsec = useconds;
        } while (up1 != time_update2.load(AK::MemoryOrder::memory_order_consume));
        ts.tv_nsec *= 1000;
        return ts;
    }

#if defined(KERNEL)
    inline void write_time(time_t epoch_time, time_t seconds_since_boot, suseconds_t useconds)
    {
        u32 up2 = time_update1.fetch_add(1u, AK::MemoryOrder::memory_order_acq_rel) + 1;
        this->epoch_time = epoch_time;
        this->seconds_since_boot = seconds_since_boot;
        this->useconds = useconds;
        time_update2.store(up2, AK::MemoryOrder::memory_order_release);
    }
#endif
};
