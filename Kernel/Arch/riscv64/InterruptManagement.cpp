/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/riscv64/CPU.h>
#include <Kernel/Arch/riscv64/IRQController.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Arch/riscv64/Interrupts/PLIC.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>

namespace Kernel {

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

void InterruptManagement::find_controllers()
{
    auto const& device_tree = DeviceTree::get();
    auto maybe_soc = device_tree.get_child("soc"sv);
    if (!maybe_soc.has_value()) {
        dmesgln("Interrupts: No `soc` node found in the device tree, Interrupts initialization will be skipped");
        return;
    }
    auto const& soc = maybe_soc.value();
    auto soc_address_cells = soc.get_property("#address-cells"sv).value().as<u32>();
    auto soc_size_cells = soc.get_property("#size-cells"sv).value().as<u32>();
    auto interrupt_controllers_seen = 0;
    for (auto const& [node_name, node] : soc.children()) {
        if (!node.has_property("interrupt-controller"sv))
            continue;

        interrupt_controllers_seen++;

        auto maybe_compatible = node.get_property("compatible"sv);
        if (!maybe_compatible.has_value()) {
            dmesgln("Interrupts: Devicetree node for {} does not have a 'compatible' string, rejecting", node_name);
            continue;
        }

        // Reject non sifive-compatible interrupt controllers
        auto sifive_plic = false;
        maybe_compatible->for_each_string([&sifive_plic](StringView compatible_string) -> IterationDecision {
            if (compatible_string == "sifive,plic-1.0.0"sv) {
                sifive_plic = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (!sifive_plic)
            continue;

        auto maybe_reg = node.get_property("reg"sv);
        if (!maybe_reg.has_value()) {
            dmesgln("Interrupts: Devicetree node for {} does not have a physical address assigned to it, rejecting", node_name);
            continue;
        }
        auto reg = maybe_reg.value();
        auto stream = reg.as_stream();
        FlatPtr paddr;
        if (soc_address_cells == 1)
            paddr = MUST(stream.read_value<BigEndian<u32>>());
        else
            paddr = MUST(stream.read_value<BigEndian<u64>>());
        size_t size;
        if (soc_size_cells == 1)
            size = MUST(stream.read_value<BigEndian<u32>>());
        else
            size = MUST(stream.read_value<BigEndian<u64>>());
        auto max_interrupt_id = node.get_property("riscv,ndev"sv).value().as<u32>();
        m_interrupt_controllers.append(adopt_lock_ref(*new (nothrow) PLIC(PhysicalAddress(paddr), size, max_interrupt_id + 1)));
    }

    if (interrupt_controllers_seen > 0 && m_interrupt_controllers.is_empty()) {
        dmesgln("Interrupts: {} interrupt controllers seen, but none are compatible", interrupt_controllers_seen);
    }
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 original_irq)
{
    return original_irq;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return m_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(size_t)
{
    // TODO: Support more interrupt controllers
    VERIFY(m_interrupt_controllers.size() == 1);
    return m_interrupt_controllers[0];
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>)
{
    TODO_RISCV64();
}

}
