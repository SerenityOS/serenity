/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Arch/aarch64/IRQController.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

class InterruptManagement {
public:
    static InterruptManagement& the();
    static void initialize();
    static bool initialized();

    static u8 acquire_mapped_interrupt_number(u8 original_irq);

    Vector<LockRefPtr<IRQController>> const& controllers();
    LockRefPtr<IRQController> get_responsible_irq_controller(u8 interrupt_vector);

private:
    InterruptManagement() = default;
    void find_controllers();

    Vector<LockRefPtr<IRQController>> m_interrupt_controllers;
};

}
