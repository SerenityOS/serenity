/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/RPi/InterruptController.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel::RPi {

// "7.5 Interrupts Registers"
// https://github.com/raspberrypi/documentation/files/1888662/BCM2837-ARM-Peripherals.-.Revised.-.V2-1.pdf
struct InterruptControllerRegisters {
    u32 irq_basic_pending;
    u32 irq_pending_1;
    u32 irq_pending_2;
    u32 fiq_control;

    u32 enable_irqs_1;
    u32 enable_irqs_2;
    u32 enable_basic_irqs;

    u32 disable_irqs_1;
    u32 disable_irqs_2;
    u32 disable_basic_irqs;
};

InterruptController::InterruptController(Memory::TypedMapping<InterruptControllerRegisters volatile> registers_mapping)
    : m_registers(move(registers_mapping))
{
}

void InterruptController::enable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number < 64);

    if (interrupt_number < 32)
        m_registers->enable_irqs_1 = m_registers->enable_irqs_1 | (1 << interrupt_number);
    else
        m_registers->enable_irqs_2 = m_registers->enable_irqs_2 | (1 << (interrupt_number - 32));
}

void InterruptController::disable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number < 64);

    if (interrupt_number < 32)
        m_registers->disable_irqs_1 = m_registers->disable_irqs_1 | (1 << interrupt_number);
    else
        m_registers->disable_irqs_2 = m_registers->disable_irqs_2 | (1 << (interrupt_number - 32));
}

void InterruptController::eoi(GenericInterruptHandler const&)
{
    // NOTE: The interrupt controller cannot clear the interrupt, since it is basically just a big multiplexer.
    //       The interrupt should be cleared by the corresponding device driver, such as a timer or uart.
}

Optional<size_t> InterruptController::pending_interrupt() const
{
    auto irq_number_plus_one = bit_scan_forward(((u64)m_registers->irq_pending_2 << 32) | (u64)m_registers->irq_pending_1);
    if (irq_number_plus_one == 0)
        return {};
    return irq_number_plus_one - 1;
}

ErrorOr<size_t> InterruptController::translate_interrupt_specifier_to_interrupt_number(ReadonlyBytes interrupt_specifier) const
{
    // https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/brcm,bcm2835-armctrl-ic.txt

    if (interrupt_specifier.size() != 2 * sizeof(u32))
        return EINVAL;

    FixedMemoryStream stream { interrupt_specifier };

    enum class InterruptBank : u32 {
        BasicPendingRegister = 0,
        GPUPendingRegister1 = 1,
        GPUPendingRegister2 = 2,
    };

    auto interrupt_bank = MUST(stream.read_value<BigEndian<InterruptBank>>());
    auto interrupt_number = MUST(stream.read_value<BigEndian<u32>>());

    if (interrupt_bank == InterruptBank::BasicPendingRegister) {
        dbgln("FIXME: Support interrupts in the BCM2835 basic pending register");
        return ENOTSUP;
    }

    if (interrupt_bank == InterruptBank::GPUPendingRegister1) {
        // We map interrupts in GPU pending register 1 to 0-31.
        if (interrupt_number > 31)
            return ERANGE;

        return interrupt_number;
    }

    if (interrupt_bank == InterruptBank::GPUPendingRegister2) {
        // We map interrupts in GPU pending register 2 to 31-63.
        if (interrupt_number > 31)
            return ERANGE;

        return interrupt_number + 32;
    }

    return EINVAL;
}

static constinit Array const compatibles_array = {
    "brcm,bcm2836-armctrl-ic"sv,
};

INTERRUPT_CONTROLLER_DEVICETREE_DRIVER(BCM2836InterruptControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/brcm,bcm2835-armctrl-ic.txt
ErrorOr<void> BCM2836InterruptControllerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto physical_address = TRY(device.get_resource(0)).paddr;

    auto registers_mapping = TRY(Memory::map_typed_writable<InterruptControllerRegisters volatile>(physical_address));
    auto interrupt_controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) InterruptController(move(registers_mapping))));

    MUST(DeviceTree::Management::register_interrupt_controller(device, *interrupt_controller));
    MUST(InterruptManagement::register_interrupt_controller(move(interrupt_controller)));

    return {};
}

}
