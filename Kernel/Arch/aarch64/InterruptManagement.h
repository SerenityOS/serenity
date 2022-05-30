/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/IRQController.h>

namespace Kernel {

class InterruptManagement {
public:
    static InterruptManagement& the();

    static u8 acquire_mapped_interrupt_number(u8 original_irq);

    RefPtr<IRQController> get_responsible_irq_controller(u8 interrupt_vector);
};

}
