/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/aarch64/Time/ARMv8Timer.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {

ARMv8Timer::ARMv8Timer(u8 interrupt_number)
    : HardwareTimer(interrupt_number)
{
    m_frequency = Aarch64::CNTFRQ_EL0::read().ClockFrequency;

    // TODO: Fall back to the devicetree clock-frequency property.
    VERIFY(m_frequency != 0);

    m_interrupt_interval = m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE;

    start_timer(m_interrupt_interval);
}

ErrorOr<NonnullLockRefPtr<ARMv8Timer>> ARMv8Timer::initialize(u8 interrupt_number)
{
    auto timer = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) ARMv8Timer(interrupt_number)));

    // Enable the physical timer.
    auto ctl = Aarch64::CNTV_CTL_EL0::read();
    ctl.IMASK = 0;
    ctl.ENABLE = 1;
    Aarch64::CNTV_CTL_EL0::write(ctl);

    timer->enable_irq();

    return timer;
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

DEVICETREE_DRIVER(ARMv8TimerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/timer/arm,arch_timer.yaml
ErrorOr<void> ARMv8TimerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto const interrupts = TRY(device.node().interrupts(DeviceTree::get()));

    if (device.node().has_property("interrupt-names"sv) || interrupts.size() != 4)
        return ENOTSUP; // TODO: Support the interrupt-names property.

    enum class DeviceTreeTimerInterruptIndex {
        EL3Physical = 0,          // "sec-phys"
        EL1Physical = 1,          // "phys"
        EL1Virtual = 2,           // "virt"
        NonSecureEL2Physical = 3, // "hyp-phys"
    };

    // Use the EL1 virtual timer, as that timer should should be accessible to us both on device and in a VM.
    auto const& interrupt = interrupts[to_underlying(DeviceTreeTimerInterruptIndex::EL1Virtual)];

    // FIXME: Don't depend on a specific interrupt descriptor format and implement proper devicetree interrupt mapping/translation.
    if (!interrupt.domain_root->is_compatible_with("arm,gic-400"sv) && !interrupt.domain_root->is_compatible_with("arm,cortex-a15-gic"sv))
        return ENOTSUP;
    if (interrupt.interrupt_identifier.size() != 3 * sizeof(BigEndian<u32>))
        return ENOTSUP;

    // The ARM timer uses a PPI (Private Peripheral Interrupt).
    // GIC interrupts 16-31 are for PPIs, so add 16 to get the GIC interrupt ID.

    // The interrupt type is in the first cell. It should be 1 for PPIs.
    if (reinterpret_cast<BigEndian<u32> const*>(interrupt.interrupt_identifier.data())[0] != 1)
        return ENOTSUP;

    // The interrupt number is in the second cell.
    auto interrupt_number = (reinterpret_cast<BigEndian<u32> const*>(interrupt.interrupt_identifier.data())[1]) + 16;

    DeviceTree::DeviceRecipe<NonnullLockRefPtr<HardwareTimerBase>> recipe {
        name(),
        device.node_name(),
        [interrupt_number] {
            return ARMv8Timer::initialize(interrupt_number);
        },
    };

    TimeManagement::add_recipe(move(recipe));

    return {};
}

}
