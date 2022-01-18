/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <AK/Time.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/APICTimer.h>
#include <Kernel/Time/HPET.h>
#include <Kernel/Time/HPETComparator.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/PIT.h>
#include <Kernel/Time/RTC.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

namespace Kernel {

static Singleton<TimeManagement> s_the;

bool TimeManagement::is_initialized()
{
    return s_the.is_initialized();
}

TimeManagement& TimeManagement::the()
{
    return *s_the;
}

bool TimeManagement::is_valid_clock_id(clockid_t clock_id)
{
    switch (clock_id) {
    case CLOCK_MONOTONIC:
    case CLOCK_MONOTONIC_COARSE:
    case CLOCK_MONOTONIC_RAW:
    case CLOCK_REALTIME:
    case CLOCK_REALTIME_COARSE:
        return true;
    default:
        return false;
    };
}

Time TimeManagement::current_time(clockid_t clock_id) const
{
    switch (clock_id) {
    case CLOCK_MONOTONIC:
        return monotonic_time(TimePrecision::Precise);
    case CLOCK_MONOTONIC_COARSE:
        return monotonic_time(TimePrecision::Coarse);
    case CLOCK_MONOTONIC_RAW:
        return monotonic_time_raw();
    case CLOCK_REALTIME:
        return epoch_time(TimePrecision::Precise);
    case CLOCK_REALTIME_COARSE:
        return epoch_time(TimePrecision::Coarse);
    default:
        // Syscall entrypoint is missing a is_valid_clock_id(..) check?
        VERIFY_NOT_REACHED();
    }
}

bool TimeManagement::is_system_timer(const HardwareTimerBase& timer) const
{
    return &timer == m_system_timer.ptr();
}

void TimeManagement::set_epoch_time(Time ts)
{
    InterruptDisabler disabler;
    // FIXME: Should use AK::Time internally
    m_epoch_time = ts.to_timespec();
    m_remaining_epoch_time_adjustment = { 0, 0 };
}

Time TimeManagement::monotonic_time(TimePrecision precision) const
{
    // This is the time when last updated by an interrupt.
    u64 seconds;
    u32 ticks;

    bool do_query = precision == TimePrecision::Precise && m_can_query_precise_time;

    u32 update_iteration;
    do {
        update_iteration = m_update1.load(AK::MemoryOrder::memory_order_acquire);
        seconds = m_seconds_since_boot;
        ticks = m_ticks_this_second;

        if (do_query) {
            // We may have to do this over again if the timer interrupt fires
            // while we're trying to query the information. In that case, our
            // seconds and ticks became invalid, producing an incorrect time.
            // Be sure to not modify m_seconds_since_boot and m_ticks_this_second
            // because this may only be modified by the interrupt handler
            HPET::the().update_time(seconds, ticks, true);
        }
    } while (update_iteration != m_update2.load(AK::MemoryOrder::memory_order_acquire));

    VERIFY(m_time_ticks_per_second > 0);
    VERIFY(ticks < m_time_ticks_per_second);
    u64 ns = ((u64)ticks * 1000000000ull) / m_time_ticks_per_second;
    VERIFY(ns < 1000000000ull);
    return Time::from_timespec({ (i64)seconds, (i32)ns });
}

Time TimeManagement::epoch_time(TimePrecision) const
{
    // TODO: Take into account precision
    timespec ts;
    u32 update_iteration;
    do {
        update_iteration = m_update1.load(AK::MemoryOrder::memory_order_acquire);
        ts = m_epoch_time;
    } while (update_iteration != m_update2.load(AK::MemoryOrder::memory_order_acquire));
    return Time::from_timespec(ts);
}

u64 TimeManagement::uptime_ms() const
{
    auto mtime = monotonic_time().to_timespec();
    // This overflows after 292 million years of uptime.
    // Since this is only used for performance timestamps and sys$times, that's probably enough.
    u64 ms = mtime.tv_sec * 1000ull;
    ms += mtime.tv_nsec / 1000000;
    return ms;
}

UNMAP_AFTER_INIT void TimeManagement::initialize(u32 cpu)
{
    if (cpu == 0) {
        VERIFY(!s_the.is_initialized());
        s_the.ensure_instance();

        if (APIC::initialized()) {
            // Initialize the APIC timers after the other timers as the
            // initialization needs to briefly enable interrupts, which then
            // would trigger a deadlock trying to get the s_the instance while
            // creating it.
            if (auto* apic_timer = APIC::the().initialize_timers(*s_the->m_system_timer)) {
                dmesgln("Time: Using APIC timer as system timer");
                s_the->set_system_timer(*apic_timer);
            }
        }
    } else {
        VERIFY(s_the.is_initialized());
        if (auto* apic_timer = APIC::the().get_timer()) {
            dmesgln("Time: Enable APIC timer on CPU #{}", cpu);
            apic_timer->enable_local_timer();
        }
    }
}

void TimeManagement::set_system_timer(HardwareTimerBase& timer)
{
    VERIFY(Processor::is_bootstrap_processor()); // This should only be called on the BSP!
    auto original_callback = m_system_timer->set_callback(nullptr);
    m_system_timer->disable();
    timer.set_callback(move(original_callback));
    m_system_timer = timer;
}

time_t TimeManagement::ticks_per_second() const
{
    return m_time_keeper_timer->ticks_per_second();
}

time_t TimeManagement::boot_time() const
{
    return RTC::boot_time();
}

UNMAP_AFTER_INIT TimeManagement::TimeManagement()
    : m_time_page_region(MM.allocate_kernel_region(PAGE_SIZE, "Time page"sv, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value_but_fixme_should_propagate_errors())
{
    bool probe_non_legacy_hardware_timers = !(kernel_command_line().is_legacy_time_enabled());
    if (ACPI::is_enabled()) {
        if (!ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            RTC::initialize();
            m_epoch_time.tv_sec += boot_time();
        } else {
            dmesgln("ACPI: RTC CMOS Not present");
        }
    } else {
        // We just assume that we can access RTC CMOS, if ACPI isn't usable.
        RTC::initialize();
        m_epoch_time.tv_sec += boot_time();
    }
    if (probe_non_legacy_hardware_timers) {
        if (!probe_and_set_non_legacy_hardware_timers())
            if (!probe_and_set_legacy_hardware_timers())
                VERIFY_NOT_REACHED();
    } else if (!probe_and_set_legacy_hardware_timers()) {
        VERIFY_NOT_REACHED();
    }
}

Time TimeManagement::now()
{
    return s_the.ptr()->epoch_time();
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_and_initialize_periodic_timers()
{
    bool should_enable = is_hpet_periodic_mode_allowed();
    dbgln("Time: Scanning for periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (hardware_timer.is_periodic_capable()) {
            timers.append(&hardware_timer);
            if (should_enable)
                hardware_timer.set_periodic();
        }
    }
    return timers;
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_for_non_periodic_timers()
{
    dbgln("Time: Scanning for non-periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (!hardware_timer.is_periodic_capable())
            timers.append(&hardware_timer);
    }
    return timers;
}

bool TimeManagement::is_hpet_periodic_mode_allowed()
{
    switch (kernel_command_line().hpet_mode()) {
    case HPETMode::Periodic:
        return true;
    case HPETMode::NonPeriodic:
        return false;
    default:
        VERIFY_NOT_REACHED();
    }
}

UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_non_legacy_hardware_timers()
{
    if (!ACPI::is_enabled())
        return false;
    if (!HPET::test_and_initialize())
        return false;
    if (!HPET::the().comparators().size()) {
        dbgln("HPET initialization aborted.");
        return false;
    }
    dbgln("HPET: Setting appropriate functions to timers.");

    for (auto& hpet_comparator : HPET::the().comparators())
        m_hardware_timers.append(hpet_comparator);

    auto periodic_timers = scan_and_initialize_periodic_timers();
    auto non_periodic_timers = scan_for_non_periodic_timers();

    if (is_hpet_periodic_mode_allowed())
        VERIFY(!periodic_timers.is_empty());

    VERIFY(periodic_timers.size() + non_periodic_timers.size() > 0);

    size_t taken_periodic_timers_count = 0;
    size_t taken_non_periodic_timers_count = 0;

    if (periodic_timers.size() > taken_periodic_timers_count) {
        m_system_timer = periodic_timers[taken_periodic_timers_count];
        taken_periodic_timers_count += 1;
    } else if (non_periodic_timers.size() > taken_non_periodic_timers_count) {
        m_system_timer = non_periodic_timers[taken_non_periodic_timers_count];
        taken_non_periodic_timers_count += 1;
    }

    m_system_timer->set_callback([this](const RegisterState& regs) {
        // Update the time. We don't really care too much about the
        // frequency of the interrupt because we'll query the main
        // counter to get an accurate time.
        if (Processor::is_bootstrap_processor()) {
            // TODO: Have the other CPUs call system_timer_tick directly
            increment_time_since_boot_hpet();
        }

        system_timer_tick(regs);
    });

    // Use the HPET main counter frequency for time purposes. This is likely
    // a much higher frequency than the interrupt itself and allows us to
    // keep a more accurate time
    m_can_query_precise_time = true;
    m_time_ticks_per_second = HPET::the().frequency();

    m_system_timer->try_to_set_frequency(m_system_timer->calculate_nearest_possible_frequency(OPTIMAL_TICKS_PER_SECOND_RATE));

    // We don't need an interrupt for time keeping purposes because we
    // can query the timer.
    m_time_keeper_timer = m_system_timer;

    if (periodic_timers.size() > taken_periodic_timers_count) {
        m_profile_timer = periodic_timers[taken_periodic_timers_count];
        taken_periodic_timers_count += 1;
    } else if (non_periodic_timers.size() > taken_non_periodic_timers_count) {
        m_profile_timer = non_periodic_timers[taken_non_periodic_timers_count];
        taken_non_periodic_timers_count += 1;
    }

    if (m_profile_timer) {
        m_profile_timer->set_callback(PerformanceManager::timer_tick);
        m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(1));
    }

    return true;
}

UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_legacy_hardware_timers()
{
    if (ACPI::is_enabled()) {
        if (ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            dbgln("ACPI: CMOS RTC Not Present");
            return false;
        } else {
            dbgln("ACPI: CMOS RTC Present");
        }
    }

    m_hardware_timers.append(PIT::initialize(TimeManagement::update_time));
    m_hardware_timers.append(RealTimeClock::create(TimeManagement::system_timer_tick));
    m_time_keeper_timer = m_hardware_timers[0];
    m_system_timer = m_hardware_timers[1];

    // The timer is only as accurate as the interrupts...
    m_time_ticks_per_second = m_time_keeper_timer->ticks_per_second();
    return true;
}

void TimeManagement::update_time(const RegisterState&)
{
    TimeManagement::the().increment_time_since_boot();
}

void TimeManagement::increment_time_since_boot_hpet()
{
    VERIFY(!m_time_keeper_timer.is_null());
    VERIFY(m_time_keeper_timer->timer_type() == HardwareTimerType::HighPrecisionEventTimer);

    // NOTE: m_seconds_since_boot and m_ticks_this_second are only ever
    // updated here! So we can safely read that information, query the clock,
    // and when we're all done we can update the information. This reduces
    // contention when other processors attempt to read the clock.
    auto seconds_since_boot = m_seconds_since_boot;
    auto ticks_this_second = m_ticks_this_second;
    auto delta_ns = HPET::the().update_time(seconds_since_boot, ticks_this_second, false);

    // Now that we have a precise time, go update it as quickly as we can
    u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);
    m_seconds_since_boot = seconds_since_boot;
    m_ticks_this_second = ticks_this_second;
    // TODO: Apply m_remaining_epoch_time_adjustment
    timespec_add(m_epoch_time, { (time_t)(delta_ns / 1000000000), (long)(delta_ns % 1000000000) }, m_epoch_time);

    m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

    update_time_page();
}

void TimeManagement::increment_time_since_boot()
{
    VERIFY(!m_time_keeper_timer.is_null());

    // Compute time adjustment for adjtime. Let the clock run up to 1% fast or slow.
    // That way, adjtime can adjust up to 36 seconds per hour, without time getting very jumpy.
    // Once we have a smarter NTP service that also adjusts the frequency instead of just slewing time, maybe we can lower this.
    long NanosPerTick = 1'000'000'000 / m_time_keeper_timer->frequency();
    time_t MaxSlewNanos = NanosPerTick / 100;

    u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);

    // Clamp twice, to make sure intermediate fits into a long.
    long slew_nanos = clamp(clamp(m_remaining_epoch_time_adjustment.tv_sec, (time_t)-1, (time_t)1) * 1'000'000'000 + m_remaining_epoch_time_adjustment.tv_nsec, -MaxSlewNanos, MaxSlewNanos);
    timespec slew_nanos_ts;
    timespec_sub({ 0, slew_nanos }, { 0, 0 }, slew_nanos_ts); // Normalize tv_nsec to be positive.
    timespec_sub(m_remaining_epoch_time_adjustment, slew_nanos_ts, m_remaining_epoch_time_adjustment);

    timespec epoch_tick = { .tv_sec = 0, .tv_nsec = NanosPerTick };
    epoch_tick.tv_nsec += slew_nanos; // No need for timespec_add(), guaranteed to be in range.
    timespec_add(m_epoch_time, epoch_tick, m_epoch_time);

    if (++m_ticks_this_second >= m_time_keeper_timer->ticks_per_second()) {
        // FIXME: Synchronize with other clock somehow to prevent drifting apart.
        ++m_seconds_since_boot;
        m_ticks_this_second = 0;
    }

    m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

    update_time_page();
}

void TimeManagement::system_timer_tick(const RegisterState& regs)
{
    if (Processor::current_in_irq() <= 1) {
        // Don't expire timers while handling IRQs
        TimerQueue::the().fire();
    }
    Scheduler::timer_tick(regs);
}

bool TimeManagement::enable_profile_timer()
{
    if (!m_profile_timer)
        return false;
    if (m_profile_enable_count.fetch_add(1) == 0)
        return m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(OPTIMAL_PROFILE_TICKS_PER_SECOND_RATE));
    return true;
}

bool TimeManagement::disable_profile_timer()
{
    if (!m_profile_timer)
        return false;
    if (m_profile_enable_count.fetch_sub(1) == 1)
        return m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(1));
    return true;
}

void TimeManagement::update_time_page()
{
    auto& page = time_page();
    u32 update_iteration = AK::atomic_fetch_add(&page.update2, 1u, AK::MemoryOrder::memory_order_acquire);
    page.clocks[CLOCK_REALTIME_COARSE] = m_epoch_time;
    page.clocks[CLOCK_MONOTONIC_COARSE] = monotonic_time(TimePrecision::Coarse).to_timespec();
    AK::atomic_store(&page.update1, update_iteration + 1u, AK::MemoryOrder::memory_order_release);
}

TimePage& TimeManagement::time_page()
{
    return *static_cast<TimePage*>((void*)m_time_page_region->vaddr().as_ptr());
}

Memory::VMObject& TimeManagement::time_page_vmobject()
{
    return m_time_page_region->vmobject();
}

}
