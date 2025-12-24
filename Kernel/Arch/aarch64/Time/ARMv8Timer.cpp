/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/aarch64/Time/ARMv8Timer.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {

static ARMv8Timer* s_the = nullptr;

ARMv8Timer::ARMv8Timer(u8 interrupt_number, u32 frequency)
    : HardwareTimer(interrupt_number)
    , m_frequency(frequency)
{
    m_interrupt_interval = m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE;

    start_timer(m_interrupt_interval);
}

ErrorOr<NonnullLockRefPtr<ARMv8Timer>> ARMv8Timer::initialize(u8 interrupt_number, u32 frequency)
{
    auto timer = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) ARMv8Timer(interrupt_number, frequency)));

    // Enable the physical timer.
    auto ctl = Aarch64::CNTV_CTL_EL0::read();
    ctl.IMASK = 0;
    ctl.ENABLE = 1;
    Aarch64::CNTV_CTL_EL0::write(ctl);

    timer->enable_irq();

    return timer;
}

bool ARMv8Timer::is_initialized()
{
    return s_the != nullptr;
}

ARMv8Timer& ARMv8Timer::the()
{
    return *s_the;
}

u64 ARMv8Timer::current_ticks()
{
    return Aarch64::CNTVCT_EL0::read().VirtualCount;
}

bool ARMv8Timer::handle_irq()
{
    HardwareTimer::handle_irq();

    if (Aarch64::CNTV_CTL_EL0::read().ISTATUS == 0)
        return false;

    start_timer(m_interrupt_interval);

    return true;
}

void ARMv8Timer::disable()
{
    disable_irq();
}

u64 ARMv8Timer::update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only)
{
    // Should only be called by the time keeper interrupt handler!
    u64 current_value = current_ticks();
    u64 delta_ticks = m_main_counter_drift;
    if (current_value >= m_main_counter_last_read) {
        delta_ticks += current_value - m_main_counter_last_read;
    } else {
        // the counter wrapped around
        delta_ticks += (NumericLimits<u64>::max() - m_main_counter_last_read + 1) + current_value;
    }

    u64 ticks_since_last_second = (u64)ticks_this_second + delta_ticks;
    auto frequency = ticks_per_second();
    seconds_since_boot += ticks_since_last_second / frequency;
    ticks_this_second = ticks_since_last_second % frequency;

    if (!query_only) {
        m_main_counter_drift = 0;
        m_main_counter_last_read = current_value;
    }

    // Return the time passed (in ns) since last time update_time was called
    return (delta_ticks * 1'000'000'000ull) / frequency;
}

void ARMv8Timer::start_timer(u32 delta)
{
    Aarch64::CNTV_TVAL_EL0::write(Aarch64::CNTV_TVAL_EL0 {
        .TimerValue = delta,
    });
}

static constinit Array const compatibles_array = {
    "arm,armv7-timer"sv, // The Raspberry Pi 3 and QEMU virt machine use this compatible string even for AArch64.
    "arm,armv8-timer"sv,
};

EARLY_DEVICETREE_DRIVER(ARMv8TimerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/timer/arm,arch_timer.yaml
ErrorOr<void> ARMv8TimerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    if (device.node().has_property("interrupt-names"sv))
        return ENOTSUP; // TODO: Support the interrupt-names property.

    enum class DeviceTreeTimerInterruptIndex {
        EL3Physical = 0,          // "sec-phys"
        EL1Physical = 1,          // "phys"
        EL1Virtual = 2,           // "virt"
        NonSecureEL2Physical = 3, // "hyp-phys"
    };

    // Use the EL1 virtual timer, as that timer should should be accessible to us both on device and in a VM.
    auto interrupt_number = TRY(device.get_interrupt_number(to_underlying(DeviceTreeTimerInterruptIndex::EL1Virtual)));

    u32 frequency = 0;

    auto clock_frequency_property = device.node().get_property("clock-frequency"sv);
    if (clock_frequency_property.has_value()) {
        if (clock_frequency_property->size() != sizeof(u32)) {
            dmesgln("ARMv8Timer: \"clock-frequency\" property for \"{}\" has invalid size: {}", device.node_name(), clock_frequency_property->size());
            return EINVAL;
        }

        frequency = clock_frequency_property->as<u32>();
    } else {
        frequency = Aarch64::CNTFRQ_EL0::read().ClockFrequency;
    }

    if (frequency == 0) {
        dmesgln("ARMv8Timer: Unable to determine clock frequency for \"{}\"", device.node_name());
        return EINVAL;
    }

    auto timer = TRY(ARMv8Timer::initialize(interrupt_number, frequency));

    MUST(TimeManagement::register_hardware_timer(timer));

    s_the = &timer.leak_ref();

    return {};
}

}
