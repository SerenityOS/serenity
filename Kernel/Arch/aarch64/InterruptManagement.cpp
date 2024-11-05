/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/RPi/InterruptController.h>
#include <Kernel/Library/Panic.h>

namespace Kernel {

static NeverDestroyed<Vector<DeviceTree::DeviceRecipe<NonnullLockRefPtr<IRQController>>>> s_recipes;
static InterruptManagement* s_interrupt_management;

bool InterruptManagement::initialized()
{
    return s_interrupt_management != nullptr;
}

InterruptManagement& InterruptManagement::the()
{
    VERIFY(InterruptManagement::initialized());
    return *s_interrupt_management;
}

void InterruptManagement::initialize()
{
    VERIFY(!InterruptManagement::initialized());
    s_interrupt_management = new InterruptManagement;

    the().find_controllers();
}

void InterruptManagement::add_recipe(DeviceTree::DeviceRecipe<NonnullLockRefPtr<IRQController>> recipe)
{
    s_recipes->append(move(recipe));
}

void InterruptManagement::find_controllers()
{
    for (auto& recipe : *s_recipes) {
        auto device_or_error = recipe.create_device();
        if (device_or_error.is_error()) {
            dmesgln("InterruptManagement: Failed to create interrupt controller for device \"{}\" with driver {}: {}", recipe.node_name, recipe.driver_name, device_or_error.release_error());
            continue;
        }

        m_interrupt_controllers.append(device_or_error.release_value());
    }

    if (m_interrupt_controllers.is_empty())
        PANIC("InterruptManagement: No supported interrupt controller found in devicetree");
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 interrupt_number)
{
    return interrupt_number;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return m_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(u8)
{
    // TODO: Support more interrupt controllers
    VERIFY(m_interrupt_controllers.size() == 1);
    return m_interrupt_controllers[0];
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>)
{
    TODO_AARCH64();
}

}
