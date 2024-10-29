/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/OwnPtr.h>
#include <AK/Platform.h>
#include <AK/SetOnce.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/API/TimePage.h>
#include <Kernel/Firmware/DeviceTree/DeviceRecipe.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define OPTIMAL_TICKS_PER_SECOND_RATE 250
#define OPTIMAL_PROFILE_TICKS_PER_SECOND_RATE 1000

class HardwareTimerBase;

enum class TimePrecision {
    Coarse = 0,
    Precise
};

class TimeManagement {

public:
    TimeManagement();
    static void initialize(u32 cpu);
    static bool is_initialized();
    static TimeManagement& the();

    static void add_recipe(DeviceTree::DeviceRecipe<NonnullLockRefPtr<HardwareTimerBase>>);

    static u64 scheduler_current_time();

    static ErrorOr<void> validate_clock_id(clockid_t);
    // This API cannot distinguish returned time types; prefer the clock-specific functions instead.
    Duration current_time(clockid_t) const;
    MonotonicTime monotonic_time(TimePrecision = TimePrecision::Coarse) const;
    MonotonicTime monotonic_time_raw() const
    {
        // TODO: implement
        return monotonic_time(TimePrecision::Precise);
    }
    UnixDateTime epoch_time(TimePrecision = TimePrecision::Precise) const;
    void set_epoch_time(UnixDateTime);
    time_t ticks_per_second() const;
    static UnixDateTime boot_time();
    Duration clock_resolution() const;

    bool is_system_timer(HardwareTimerBase const&) const;

    void increment_time_since_boot();

    static bool is_hpet_periodic_mode_allowed();

    bool enable_profile_timer();
    bool disable_profile_timer();

    u64 uptime_ms() const;
    static UnixDateTime now();

    // FIXME: Most likely broken, because it does not check m_update[12] for in-progress updates.
    Duration remaining_epoch_time_adjustment() const { return m_remaining_epoch_time_adjustment; }
    // FIXME: Most likely broken, because it does not check m_update[12] for in-progress updates.
    void set_remaining_epoch_time_adjustment(Duration adjustment) { m_remaining_epoch_time_adjustment = adjustment; }

    bool can_query_precise_time() const { return m_can_query_precise_time.was_set(); }

    Memory::VMObject& time_page_vmobject();

private:
    TimePage& time_page();
    void update_time_page();

#if ARCH(X86_64)
    bool probe_and_set_x86_legacy_hardware_timers();
    bool probe_and_set_x86_non_legacy_hardware_timers();
    void increment_time_since_boot_hpet();
    static void update_time();
#elif ARCH(AARCH64)
    bool probe_and_set_aarch64_hardware_timers();
#elif ARCH(RISCV64)
    bool probe_and_set_riscv64_hardware_timers();
#else
#    error Unknown architecture
#endif
    Vector<HardwareTimerBase*> scan_and_initialize_periodic_timers();
    Vector<HardwareTimerBase*> scan_for_non_periodic_timers();
    Vector<NonnullLockRefPtr<HardwareTimerBase>> m_hardware_timers;
    void set_system_timer(HardwareTimerBase&);
    static void system_timer_tick();

    // Variables between m_update1 and m_update2 are synchronized
    // FIXME: Replace m_update1 and m_update2 with a SpinlockLocker
    Atomic<u32> m_update1 { 0 };
    u32 m_ticks_this_second { 0 };
    u64 m_seconds_since_boot { 0 };
    UnixDateTime m_epoch_time {};
    Duration m_remaining_epoch_time_adjustment {};
    Atomic<u32> m_update2 { 0 };

    u32 m_time_ticks_per_second { 0 }; // may be different from interrupts/second (e.g. hpet)
    SetOnce m_can_query_precise_time;
    bool m_updating_time { false }; // may only be accessed from the BSP!

    LockRefPtr<HardwareTimerBase> m_system_timer;
    LockRefPtr<HardwareTimerBase> m_time_keeper_timer;

    Atomic<u32> m_profile_enable_count { 0 };
    LockRefPtr<HardwareTimerBase> m_profile_timer;

    NonnullOwnPtr<Memory::Region> m_time_page_region;
};

}
