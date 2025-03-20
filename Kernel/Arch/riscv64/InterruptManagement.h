/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Arch/riscv64/IRQController.h>
#include <Kernel/Firmware/DeviceTree/DeviceRecipe.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

class InterruptManagement {
public:
    static InterruptManagement& the();
    static void initialize();
    static bool initialized();

    static void add_recipe(DeviceTree::DeviceRecipe<NonnullLockRefPtr<IRQController>>);

    static u8 acquire_mapped_interrupt_number(u8 original_irq);

    Vector<NonnullLockRefPtr<IRQController>> const& controllers();
    NonnullLockRefPtr<IRQController> get_responsible_irq_controller(size_t irq_number);

    void enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>);

private:
    InterruptManagement() = default;
    void find_controllers();

    Vector<NonnullLockRefPtr<IRQController>> m_interrupt_controllers;
};

}
