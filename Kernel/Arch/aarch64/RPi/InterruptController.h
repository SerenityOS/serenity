/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/aarch64/IRQController.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct InterruptControllerRegisters;

// This class implements the simple Interrupt Controller found in the BCM2837. (RPi3)
// A description of this device can be found at chapter 7 (Interrupts) of the manual:
// https://github.com/raspberrypi/documentation/files/1888662/BCM2837-ARM-Peripherals.-.Revised.-.V2-1.pdf (RPi3)
class InterruptController : public IRQController {
public:
    InterruptController(Memory::TypedMapping<InterruptControllerRegisters volatile>);

private:
    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;

    virtual void eoi(GenericInterruptHandler const&) override;

    virtual Optional<size_t> pending_interrupt() const override;

    virtual StringView model() const override
    {
        return "Raspberry Pi Interrupt Controller"sv;
    }

    Memory::TypedMapping<InterruptControllerRegisters volatile> m_registers;
};

}
