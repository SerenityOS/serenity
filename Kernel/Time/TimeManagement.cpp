/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <AK/Time.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#    include <Kernel/Arch/x86_64/RTC.h>
#    include <Kernel/Arch/x86_64/Time/APICTimer.h>
#    include <Kernel/Arch/x86_64/Time/HPET.h>
#    include <Kernel/Arch/x86_64/Time/HPETComparator.h>
#    include <Kernel/Arch/x86_64/Time/TSC.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/Timer.h>
#    include <Kernel/Arch/aarch64/Time/ARMv8Timer.h>
#    include <Kernel/Arch/aarch64/Time/PL031.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Timer.h>
#    include <Kernel/Devices/Time/GoldfishRTC.h>
#else
#    error Unknown architecture
#endif

#include <Kernel/Arch/CurrentTime.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Time/HardwareTimer.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/Time/TimerQueue.h>

namespace Kernel {

static Singleton<TimeManagement> s_the;

bool TimeManagement::is_initialized()
{
    return s_the.is_initialized() && s_the->m_initialized;
}

TimeManagement& TimeManagement::the()
{
    return *s_the;
}

ErrorOr<void> TimeManagement::register_hardware_timer(NonnullLockRefPtr<HardwareTimerBase> timer)
{
    // This function has to be called before TimeManagement is initialized,
    // as we do not support dynamic registration of timers.
    VERIFY(!is_initialized());

    return m_hardware_timers.try_append(move(timer));
}

ErrorOr<void> TimeManagement::register_time_source(NonnullLockRefPtr<TimeSource> time_source)
{
    // This function has to be called before TimeManagement is initialized,
    // as we do not support dynamic registration of time sources.
    VERIFY(!is_initialized());

    return m_time_sources.try_append(move(time_source));
}

// The s_scheduler_specific_current_time function provides a current time for scheduling purposes,
// which may not necessarily relate to wall time
static u64 (*s_scheduler_current_time)();

static u64 current_time_monotonic()
{
    // We always need a precise timestamp here, we cannot rely on a coarse timestamp
    return (u64)TimeManagement::the().monotonic_time(TimePrecision::Precise).nanoseconds();
}

u64 TimeManagement::scheduler_current_time()
{
    VERIFY(s_scheduler_current_time);
    return s_scheduler_current_time();
}

Duration TimeManagement::current_time(clockid_t clock_id)
{
    switch (clock_id) {
    case CLOCK_MONOTONIC:
        return monotonic_time(TimePrecision::Precise).time_since_start({});
    case CLOCK_MONOTONIC_COARSE:
        return monotonic_time(TimePrecision::Coarse).time_since_start({});
    case CLOCK_MONOTONIC_RAW:
        return monotonic_time_raw().time_since_start({});
    case CLOCK_REALTIME:
        return epoch_time(TimePrecision::Precise).offset_to_epoch();
    case CLOCK_REALTIME_COARSE:
        return epoch_time(TimePrecision::Coarse).offset_to_epoch();
    default:
        // Syscall entrypoint is missing a is_valid_clock_id(..) check?
        VERIFY_NOT_REACHED();
    }
}

bool TimeManagement::is_system_timer(HardwareTimerBase const& timer) const
{
    return &timer == m_system_timer.ptr();
}

void TimeManagement::set_epoch_time(UnixDateTime ts)
{
    // FIXME: The interrupt disabler intends to enforce atomic update of epoch time and remaining adjustment,
    //        but that sort of assumption is known to break on SMP.
    InterruptDisabler disabler;
    m_epoch_time = ts;
    m_remaining_epoch_time_adjustment = {};
}

MonotonicTime TimeManagement::monotonic_time(TimePrecision precision)
{
    // This is the time when last updated by an interrupt.
    u64 seconds;
    u32 ticks;

    u32 update_iteration;
    do {
        update_iteration = m_update1.load(AK::MemoryOrder::memory_order_acquire);
        seconds = m_seconds_since_boot;
        ticks = m_ticks_this_second;

        if (precision == TimePrecision::Precise)
            m_time_source->update_time(seconds, ticks, true);
    } while (update_iteration != m_update2.load(AK::MemoryOrder::memory_order_acquire));

    VERIFY(m_time_ticks_per_second > 0);
    VERIFY(ticks < m_time_ticks_per_second);
    u64 ns = ((u64)ticks * 1000000000ull) / m_time_ticks_per_second;
    VERIFY(ns < 1000000000ull);
    return MonotonicTime::from_hardware_time({}, seconds, ns);
}

UnixDateTime TimeManagement::epoch_time(TimePrecision) const
{
    // TODO: Take into account precision
    UnixDateTime time;
    u32 update_iteration;
    do {
        update_iteration = m_update1.load(AK::MemoryOrder::memory_order_acquire);
        time = m_epoch_time;
    } while (update_iteration != m_update2.load(AK::MemoryOrder::memory_order_acquire));
    return time;
}

u64 TimeManagement::uptime_ms()
{
    auto mtime = monotonic_time().time_since_start({}).to_timespec();
    // This overflows after 292 million years of uptime.
    // Since this is only used for performance timestamps and sys$times, that's probably enough.
    u64 ms = mtime.tv_sec * 1000ull;
    ms += mtime.tv_nsec / 1000000;
    return ms;
}

UNMAP_AFTER_INIT void TimeManagement::initialize([[maybe_unused]] u32 cpu)
{
    // Note: We must disable interrupts, because the timers interrupt might fire before
    //       the TimeManagement class is completely initialized.
    InterruptDisabler disabler;

    if (cpu == 0) {
        VERIFY(!s_the->m_initialized);
        s_the.ensure_instance();

        s_the->m_time_page_region = MM.allocate_kernel_region(PAGE_SIZE, "Duration page"sv, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value_but_fixme_should_propagate_errors();
#if ARCH(X86_64)
        if (!ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            RTC::initialize();
            s_the->m_epoch_time += boot_time().offset_to_epoch();
        } else {
            dmesgln("ACPI: RTC CMOS Not present");
        }

        if (!s_the->probe_and_set_x86_hardware_timers())
            PANIC("TimeManagement: No supported timer was found");

        if (APIC::initialized()) {
            // Initialize the APIC timers after the other timers as the
            // initialization needs to briefly enable interrupts, which then
            // would trigger a deadlock trying to get the s_the instance while
            // creating it.
            if (auto* apic_timer = APIC::the().initialize_timers(*s_the->m_system_timer)) {
                dmesgln("Duration: Using APIC timer as system timer");
                s_the->set_system_timer(*apic_timer);
            }
        }
#elif ARCH(AARCH64)
        auto rtc = PL031::the();
        if (rtc)
            s_the->m_epoch_time += boot_time().offset_to_epoch();
        s_the->probe_and_set_aarch64_hardware_timers();
#elif ARCH(RISCV64)
        auto rtc = GoldfishRTC::the();
        if (rtc)
            s_the->m_epoch_time += boot_time().offset_to_epoch();
        s_the->probe_and_set_riscv64_hardware_timers();
#else
#    error Unknown architecture
#endif
        auto* possible_arch_specific_current_time_function = optional_current_time();
        if (possible_arch_specific_current_time_function)
            s_scheduler_current_time = possible_arch_specific_current_time_function;
        else
            s_scheduler_current_time = current_time_monotonic;

        dbgln("TimeManagement: System timer: {}", s_the->m_system_timer->model());
        dbgln("TimeManagement: Time source: {}", s_the->m_time_source->model());

        s_the->m_initialized = true;
    } else {
        VERIFY(s_the->m_initialized);

#if ARCH(X86_64)
        if (auto* apic_timer = APIC::the().get_timer()) {
            dmesgln("Duration: Enable APIC timer on CPU #{}", cpu);
            apic_timer->enable_local_timer();
        }
#endif
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
    return m_time_source->frequency();
}

UnixDateTime TimeManagement::boot_time()
{
#if ARCH(X86_64)
    return RTC::boot_time();
#elif ARCH(AARCH64)
    auto rtc = PL031::the();
    if (!rtc)
        return UnixDateTime::epoch();
    return rtc->boot_time();
#elif ARCH(RISCV64)
    auto rtc = GoldfishRTC::the();
    if (!rtc)
        return UnixDateTime::epoch();
    return rtc->boot_time();
#else
#    error Unknown architecture
#endif
}

Duration TimeManagement::clock_resolution() const
{
    long nanoseconds_per_tick = 1'000'000'000 / m_time_source->frequency();
    return Duration::from_nanoseconds(nanoseconds_per_tick);
}

UnixDateTime TimeManagement::now()
{
    return s_the.ptr()->epoch_time();
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_and_initialize_periodic_timers()
{
    dbgln("Duration: Scanning for periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (hardware_timer->supports_mode(TimerMode::Periodic)) {
            timers.try_append(hardware_timer).release_value_but_fixme_should_propagate_errors();
            hardware_timer->set_mode(TimerMode::Periodic);
        }
    }
    return timers;
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_for_non_periodic_timers()
{
    dbgln("Duration: Scanning for non-periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (!hardware_timer->supports_mode(TimerMode::Periodic))
            timers.try_append(hardware_timer).release_value_but_fixme_should_propagate_errors();
    }
    return timers;
}

#if ARCH(X86_64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_x86_hardware_timers()
{
    auto maybe_hpet_result = HPET::initialize();
    if (maybe_hpet_result.is_error())
        return false;
    if (!maybe_hpet_result.value())
        return false;

    if (!HPET::the().comparators().size()) {
        dbgln("HPET initialization aborted.");
        return false;
    }
    dbgln("HPET: Setting appropriate functions to timers.");

    for (auto& hpet_comparator : HPET::the().comparators())
        m_hardware_timers.try_append(hpet_comparator).release_value_but_fixme_should_propagate_errors();

    auto periodic_timers = scan_and_initialize_periodic_timers();
    auto non_periodic_timers = scan_for_non_periodic_timers();

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

    m_system_timer->set_callback([this]() { system_timer_callback(); });

    // Use the HPET main counter frequency for time purposes. This is likely
    // a much higher frequency than the interrupt itself and allows us to
    // keep a more accurate time
    m_time_source = HPET::the();
    m_time_ticks_per_second = HPET::the().frequency();

    MUST(m_system_timer->try_to_set_frequency(m_system_timer->calculate_nearest_possible_frequency(OPTIMAL_TICKS_PER_SECOND_RATE)));

    if (TSC::is_supported_and_invariant()) {
        auto tsc = TSC::create(*m_system_timer).release_value_but_fixme_should_propagate_errors();
        register_time_source(tsc).release_value_but_fixme_should_propagate_errors();

        m_time_source = tsc;
        m_time_ticks_per_second = tsc->frequency();
    }

    if (periodic_timers.size() > taken_periodic_timers_count) {
        m_profile_timer = periodic_timers[taken_periodic_timers_count];
        taken_periodic_timers_count += 1;
    } else if (non_periodic_timers.size() > taken_non_periodic_timers_count) {
        m_profile_timer = non_periodic_timers[taken_non_periodic_timers_count];
        taken_non_periodic_timers_count += 1;
    }

    if (m_profile_timer) {
        m_profile_timer->set_callback(PerformanceManager::timer_tick);
        MUST(m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(1)));
    }

    return true;
}
#elif ARCH(AARCH64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_aarch64_hardware_timers()
{
    if (ACPI::is_enabled()) {
        auto gtdt_table_paddr = ACPI::Parser::the()->find_table("GTDT"sv);
        if (gtdt_table_paddr.has_value()) {
            auto gtdt = Memory::map_typed<::ACPI::Structures::GTDT>(gtdt_table_paddr.value()).release_value_but_fixme_should_propagate_errors();

            auto virtual_el1_gsi = gtdt->virtual_el1_timer_gsi;
            auto timer_frequency = Aarch64::CNTFRQ_EL0::read().ClockFrequency;

            auto timer = ARMv8Timer::initialize(virtual_el1_gsi, timer_frequency).release_value_but_fixme_should_propagate_errors();
            m_hardware_timers.try_append(timer).release_value_but_fixme_should_propagate_errors();
            m_time_sources.try_append(move(timer)).release_value_but_fixme_should_propagate_errors();
        }
    }

    if (m_hardware_timers.is_empty() || m_time_sources.is_empty())
        PANIC("TimeManagement: No supported timer was found");

    // TODO: Use some kind of heuristic to decide which timer to use.
    m_system_timer = m_hardware_timers.last();
    m_time_source = m_time_sources.last();

    m_time_ticks_per_second = m_time_source->frequency();

    m_system_timer->set_callback([this]() { system_timer_callback(); });
    return true;
}
#elif ARCH(RISCV64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_riscv64_hardware_timers()
{
    auto timer = RISCV64::Timer::initialize();
    m_hardware_timers.try_append(timer).release_value_but_fixme_should_propagate_errors();
    m_time_sources.try_append(move(timer)).release_value_but_fixme_should_propagate_errors();

    m_system_timer = m_hardware_timers[0];
    m_time_source = m_time_sources[0];
    m_time_ticks_per_second = m_time_source->frequency();

    m_system_timer->set_callback([this]() { system_timer_callback(); });
    return true;
}
#else
#    error Unknown architecture
#endif

void TimeManagement::update_time()
{
    // NOTE: m_seconds_since_boot and m_ticks_this_second are only ever updated from here!
    u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);
    auto delta_ns = m_time_source->update_time(m_seconds_since_boot, m_ticks_this_second, false);
    // TODO: Apply m_remaining_epoch_time_adjustment
    m_epoch_time += Duration::from_nanoseconds(delta_ns);
    m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

    update_time_page();
}

void TimeManagement::system_timer_callback()
{
    if (Processor::is_bootstrap_processor()) {
        // `system_timer_callback()` is used when we have a single physical timer for both
        // the system and the time keeping. To avoid hammering the `update_time()`
        // function, only the bootstrap processor is doing the time keping.
        update_time();
    }
    system_timer_tick();
}

void TimeManagement::system_timer_tick()
{
    TimerQueue::the().fire();
    Scheduler::timer_tick();
}

bool TimeManagement::enable_profile_timer()
{
    if (!m_profile_timer)
        return false;
    if (m_profile_enable_count.fetch_add(1) == 0)
        return !m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(OPTIMAL_PROFILE_TICKS_PER_SECOND_RATE)).is_error();
    return true;
}

bool TimeManagement::disable_profile_timer()
{
    if (!m_profile_timer)
        return false;
    if (m_profile_enable_count.fetch_sub(1) == 1)
        return !m_profile_timer->try_to_set_frequency(m_profile_timer->calculate_nearest_possible_frequency(1)).is_error();
    return true;
}

void TimeManagement::update_time_page()
{
    auto& page = time_page();
    u32 update_iteration = AK::atomic_fetch_add(&page.update2, 1u, AK::MemoryOrder::memory_order_acquire);
    page.clocks[CLOCK_REALTIME_COARSE] = m_epoch_time.to_timespec();
    page.clocks[CLOCK_MONOTONIC_COARSE] = monotonic_time(TimePrecision::Coarse).time_since_start({}).to_timespec();
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
