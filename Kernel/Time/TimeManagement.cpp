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
#    include <Kernel/Arch/x86_64/Time/PIT.h>
#    include <Kernel/Arch/x86_64/Time/RTC.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/Timer.h>
#    include <Kernel/Arch/aarch64/Time/ARMv8Timer.h>
#    include <Kernel/Arch/aarch64/Time/PL031.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Timer.h>
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

static NeverDestroyed<Vector<DeviceTree::DeviceRecipe<NonnullLockRefPtr<HardwareTimerBase>>>> s_recipes;
static Singleton<TimeManagement> s_the;

bool TimeManagement::is_initialized()
{
    return s_the.is_initialized();
}

TimeManagement& TimeManagement::the()
{
    return *s_the;
}

void TimeManagement::add_recipe(DeviceTree::DeviceRecipe<NonnullLockRefPtr<HardwareTimerBase>> recipe)
{
    // This function has to be called before TimeManagement is initialized,
    // as we do not support dynamic registration of timers.
    VERIFY(!is_initialized());

    s_recipes->append(move(recipe));
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

ErrorOr<void> TimeManagement::validate_clock_id(clockid_t clock_id)
{
    switch (clock_id) {
    case CLOCK_MONOTONIC:
    case CLOCK_MONOTONIC_COARSE:
    case CLOCK_MONOTONIC_RAW:
    case CLOCK_REALTIME:
    case CLOCK_REALTIME_COARSE:
        return {};
    default:
        return EINVAL;
    };
}

Duration TimeManagement::current_time(clockid_t clock_id) const
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

MonotonicTime TimeManagement::monotonic_time(TimePrecision precision) const
{
    // This is the time when last updated by an interrupt.
    u64 seconds;
    u32 ticks;

    bool do_query = precision == TimePrecision::Precise && m_can_query_precise_time.was_set();

    u32 update_iteration;
    do {
        update_iteration = m_update1.load(AK::MemoryOrder::memory_order_acquire);
        seconds = m_seconds_since_boot;
        ticks = m_ticks_this_second;

        if (do_query) {
#if ARCH(X86_64)
            // We may have to do this over again if the timer interrupt fires
            // while we're trying to query the information. In that case, our
            // seconds and ticks became invalid, producing an incorrect time.
            // Be sure to not modify m_seconds_since_boot and m_ticks_this_second
            // because this may only be modified by the interrupt handler
            HPET::the().update_time(seconds, ticks, true);
#elif ARCH(AARCH64)
            // FIXME: Get rid of these horrible casts
            if (m_system_timer->timer_type() == HardwareTimerType::RPiTimer)
                const_cast<RPi::Timer*>(static_cast<RPi::Timer const*>(m_system_timer.ptr()))->update_time(seconds, ticks, true);
            else if (m_system_timer->timer_type() == HardwareTimerType::ARMv8Timer)
                const_cast<ARMv8Timer*>(static_cast<ARMv8Timer const*>(m_system_timer.ptr()))->update_time(seconds, ticks, true);
            else
                VERIFY_NOT_REACHED();
#elif ARCH(RISCV64)
            // FIXME: Get rid of these horrible casts
            if (m_system_timer->timer_type() == HardwareTimerType::RISCVTimer)
                const_cast<RISCV64::Timer*>(static_cast<RISCV64::Timer const*>(m_system_timer.ptr()))->update_time(seconds, ticks, true);
            else
                VERIFY_NOT_REACHED();
#else
#    error Unknown architecture
#endif
        }
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

u64 TimeManagement::uptime_ms() const
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

#if ARCH(X86_64)
    if (cpu == 0) {
        VERIFY(!s_the.is_initialized());
        s_the.ensure_instance();

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
    } else {
        VERIFY(s_the.is_initialized());
        if (auto* apic_timer = APIC::the().get_timer()) {
            dmesgln("Duration: Enable APIC timer on CPU #{}", cpu);
            apic_timer->enable_local_timer();
        }
    }
#elif ARCH(AARCH64)
    if (cpu == 0) {
        VERIFY(!s_the.is_initialized());
        s_the.ensure_instance();
    }
#elif ARCH(RISCV64)
    if (cpu == 0) {
        VERIFY(!s_the.is_initialized());
        s_the.ensure_instance();
    }
#else
#    error Unknown architecture
#endif
    auto* possible_arch_specific_current_time_function = optional_current_time();
    if (possible_arch_specific_current_time_function)
        s_scheduler_current_time = possible_arch_specific_current_time_function;
    else
        s_scheduler_current_time = current_time_monotonic;
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
    // FIXME: Return correct boot time
    return UnixDateTime::epoch();
#else
#    error Unknown architecture
#endif
}

Duration TimeManagement::clock_resolution() const
{
    long nanoseconds_per_tick = 1'000'000'000 / m_time_keeper_timer->ticks_per_second();
    return Duration::from_nanoseconds(nanoseconds_per_tick);
}

UNMAP_AFTER_INIT TimeManagement::TimeManagement()
    : m_time_page_region(MM.allocate_kernel_region(PAGE_SIZE, "Duration page"sv, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value_but_fixme_should_propagate_errors())
{
#if ARCH(X86_64)
    bool probe_non_legacy_hardware_timers = !(kernel_command_line().is_legacy_time_enabled());
    if (ACPI::is_enabled()) {
        if (!ACPI::Parser::the()->x86_specific_flags().cmos_rtc_not_present) {
            RTC::initialize();
            m_epoch_time += boot_time().offset_to_epoch();
        } else {
            dmesgln("ACPI: RTC CMOS Not present");
        }
    } else {
        // We just assume that we can access RTC CMOS, if ACPI isn't usable.
        RTC::initialize();
        m_epoch_time += boot_time().offset_to_epoch();
    }
    if (probe_non_legacy_hardware_timers) {
        if (!probe_and_set_x86_non_legacy_hardware_timers())
            if (!probe_and_set_x86_legacy_hardware_timers())
                VERIFY_NOT_REACHED();
    } else if (!probe_and_set_x86_legacy_hardware_timers()) {
        VERIFY_NOT_REACHED();
    }
#elif ARCH(AARCH64)
    auto rtc = PL031::the();
    if (rtc)
        m_epoch_time += boot_time().offset_to_epoch();
    probe_and_set_aarch64_hardware_timers();
#elif ARCH(RISCV64)
    probe_and_set_riscv64_hardware_timers();
#else
#    error Unknown architecture
#endif
}

UnixDateTime TimeManagement::now()
{
    return s_the.ptr()->epoch_time();
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_and_initialize_periodic_timers()
{
    bool should_enable = is_hpet_periodic_mode_allowed();
    dbgln("Duration: Scanning for periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (hardware_timer->is_periodic_capable()) {
            timers.append(hardware_timer);
            if (should_enable)
                hardware_timer->set_periodic();
        }
    }
    return timers;
}

UNMAP_AFTER_INIT Vector<HardwareTimerBase*> TimeManagement::scan_for_non_periodic_timers()
{
    dbgln("Duration: Scanning for non-periodic timers");
    Vector<HardwareTimerBase*> timers;
    for (auto& hardware_timer : m_hardware_timers) {
        if (!hardware_timer->is_periodic_capable())
            timers.append(hardware_timer);
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

#if ARCH(X86_64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_x86_non_legacy_hardware_timers()
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

    m_system_timer->set_callback([this]() {
        // Update the time. We don't really care too much about the
        // frequency of the interrupt because we'll query the main
        // counter to get an accurate time.
        if (Processor::is_bootstrap_processor()) {
            // TODO: Have the other CPUs call system_timer_tick directly
            increment_time_since_boot_hpet();
        }

        system_timer_tick();
    });

    // Use the HPET main counter frequency for time purposes. This is likely
    // a much higher frequency than the interrupt itself and allows us to
    // keep a more accurate time
    m_can_query_precise_time.set();
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

UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_x86_legacy_hardware_timers()
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

void TimeManagement::update_time()
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
    timespec time_adjustment = { (time_t)(delta_ns / 1000000000), (long)(delta_ns % 1000000000) };
    m_epoch_time += Duration::from_timespec(time_adjustment);

    m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

    update_time_page();
}
#elif ARCH(AARCH64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_aarch64_hardware_timers()
{
    for (auto& recipe : *s_recipes) {
        auto device_or_error = recipe.create_device();
        if (device_or_error.is_error()) {
            dmesgln("TimeManagement: Failed to create timer for device \"{}\" with driver {}: {}", recipe.node_name, recipe.driver_name, device_or_error.release_error());
            continue;
        }

        m_hardware_timers.append(device_or_error.release_value());
    }

    if (m_hardware_timers.is_empty())
        PANIC("TimeManagement: No supported timer found in devicetree");

    // TODO: Use some kind of heuristic to decide which timer to use.
    m_system_timer = m_hardware_timers.last();
    dbgln("TimeManagement: System timer: {}", m_system_timer->model());

    m_time_ticks_per_second = m_system_timer->ticks_per_second();

    m_system_timer->set_callback([this]() {
        auto seconds_since_boot = m_seconds_since_boot;
        auto ticks_this_second = m_ticks_this_second;

        u64 delta_ns;
        if (m_system_timer->timer_type() == HardwareTimerType::RPiTimer)
            delta_ns = static_cast<RPi::Timer*>(m_system_timer.ptr())->update_time(seconds_since_boot, ticks_this_second, false);
        else if (m_system_timer->timer_type() == HardwareTimerType::ARMv8Timer)
            delta_ns = static_cast<ARMv8Timer*>(m_system_timer.ptr())->update_time(seconds_since_boot, ticks_this_second, false);
        else
            VERIFY_NOT_REACHED();

        u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);
        m_seconds_since_boot = seconds_since_boot;
        m_ticks_this_second = ticks_this_second;

        m_epoch_time += Duration::from_nanoseconds(delta_ns);

        m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

        update_time_page();

        system_timer_tick();
    });

    m_can_query_precise_time.set();
    m_time_keeper_timer = m_system_timer;

    return true;
}
#elif ARCH(RISCV64)
UNMAP_AFTER_INIT bool TimeManagement::probe_and_set_riscv64_hardware_timers()
{
    m_hardware_timers.append(RISCV64::Timer::initialize());
    m_system_timer = m_hardware_timers[0];
    m_time_ticks_per_second = m_system_timer->ticks_per_second();

    m_system_timer->set_callback([this]() {
        auto seconds_since_boot = m_seconds_since_boot;
        auto ticks_this_second = m_ticks_this_second;
        auto delta_ns = static_cast<RISCV64::Timer*>(m_system_timer.ptr())->update_time(seconds_since_boot, ticks_this_second, false);

        u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);
        m_seconds_since_boot = seconds_since_boot;
        m_ticks_this_second = ticks_this_second;

        m_epoch_time += Duration::from_nanoseconds(delta_ns);

        m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

        update_time_page();

        system_timer_tick();
    });

    m_can_query_precise_time.set();
    m_time_keeper_timer = m_system_timer;

    return true;
}
#else
#    error Unknown architecture
#endif

void TimeManagement::increment_time_since_boot()
{
    VERIFY(!m_time_keeper_timer.is_null());

    // Compute time adjustment for adjtime. Let the clock run up to 1% fast or slow.
    // That way, adjtime can adjust up to 36 seconds per hour, without time getting very jumpy.
    // Once we have a smarter NTP service that also adjusts the frequency instead of just slewing time, maybe we can lower this.
    long nanos_per_tick = 1'000'000'000 / m_time_keeper_timer->ticks_per_second();
    time_t max_slew_nanos = nanos_per_tick / 100;

    u32 update_iteration = m_update2.fetch_add(1, AK::MemoryOrder::memory_order_acquire);

    auto slew_nanos = Duration::from_nanoseconds(
        clamp(m_remaining_epoch_time_adjustment.to_nanoseconds(), -max_slew_nanos, max_slew_nanos));
    m_remaining_epoch_time_adjustment -= slew_nanos;

    m_epoch_time += Duration::from_nanoseconds(nanos_per_tick + slew_nanos.to_nanoseconds());

    if (++m_ticks_this_second >= m_time_keeper_timer->ticks_per_second()) {
        // FIXME: Synchronize with other clock somehow to prevent drifting apart.
        ++m_seconds_since_boot;
        m_ticks_this_second = 0;
    }

    m_update1.store(update_iteration + 1, AK::MemoryOrder::memory_order_release);

    update_time_page();
}

void TimeManagement::system_timer_tick()
{
    if (Processor::current_in_irq() <= 1) {
        // Don't expire timers while handling IRQs
        TimerQueue::the().fire();
    }
    Scheduler::timer_tick();
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
