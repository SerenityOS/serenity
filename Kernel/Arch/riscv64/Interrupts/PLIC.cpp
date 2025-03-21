/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Arch/riscv64/Interrupts/PLIC.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {

UNMAP_AFTER_INIT PLIC::PLIC(Memory::TypedMapping<RegisterMap volatile> registers, u32 interrupt_count, size_t boot_hart_supervisor_mode_context_id)
    : m_registers(move(registers))
    , m_interrupt_count(interrupt_count)
    , m_boot_hart_supervisor_mode_context_id(boot_hart_supervisor_mode_context_id)
{
    VERIFY(m_interrupt_count < 256); // TODO: Serenity currently only supports up to 256 unique interrupts, but the PLIC supports up to 1024
    initialize();
}

UNMAP_AFTER_INIT void PLIC::initialize()
{
    for (auto i = 1u; i < m_interrupt_count; ++i) {
        // Initialize all interrupt priorities to 1 (0 means never-interrupt)
        m_registers->interrupt_priority[i] = 1;
    }
    for (auto i = 0u; i <= ((m_interrupt_count - 1) >> 5); ++i) {
        // Initialize all interrupt sources to disabled
        m_registers->interrupt_enable_bitmap[m_boot_hart_supervisor_mode_context_id][i] = 0;
    }
    // Initialize priority-threshold to 0 (accept any interrupt of priority 1 or above)
    m_registers->contexts[m_boot_hart_supervisor_mode_context_id].priority_threshold = 0;
    // Enable external interrupts in the current hart
    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SIE, 1 << (to_underlying(RISCV64::CSR::SCAUSE::SupervisorExternalInterrupt) & ~RISCV64::CSR::SCAUSE_INTERRUPT_MASK));
}

void PLIC::enable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number > 0); // Interrupt number 0 is reserved to mean no-interrupt
    m_registers->interrupt_enable_bitmap[m_boot_hart_supervisor_mode_context_id][interrupt_number >> 5] |= 1u << (interrupt_number & 0x1F);
}

void PLIC::disable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number > 0); // Interrupt number 0 is reserved to mean no-interrupt
    m_registers->interrupt_enable_bitmap[m_boot_hart_supervisor_mode_context_id][interrupt_number >> 5] &= ~(1u << (interrupt_number & 0x1F));
}

void PLIC::eoi(GenericInterruptHandler const& handler)
{
    m_registers->contexts[m_boot_hart_supervisor_mode_context_id].claim_complete = handler.interrupt_number();
}

u8 PLIC::pending_interrupt() const
{
    return m_registers->contexts[m_boot_hart_supervisor_mode_context_id].claim_complete;
}

static constinit Array const compatibles_array = {
    "riscv,plic0"sv,
    "sifive,plic-1.0.0"sv,
};

DEVICETREE_DRIVER(PLICDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/sifive,plic-1.0.0.yaml
ErrorOr<void> PLICDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto physical_address = TRY(device.get_resource(0)).paddr;

    auto maybe_max_interrupt_id = device.node().get_property("riscv,ndev"sv);
    if (!maybe_max_interrupt_id.has_value())
        return EINVAL;
    auto max_interrupt_id = maybe_max_interrupt_id->as<u32>();

    size_t boot_hart_supervisor_mode_context_id = 0;

    // Get the context ID for the supervisor mode context of the boot hart.
    // FIXME: Support multiple contexts when we support SMP on riscv64.
    for (auto [context_id, interrupt] : enumerate(TRY(device.node().interrupts(DeviceTree::get())))) {
        // interrupts-extended: "Each node pointed to should be a riscv,cpu-intc node, which has a riscv node as parent."
        auto const& cpu_intc = *interrupt.domain_root;

        if (!cpu_intc.is_compatible_with("riscv,cpu-intc"sv))
            return EINVAL;

        auto const* cpu = cpu_intc.parent();
        if (cpu == nullptr)
            return EINVAL;

        if (!cpu->is_compatible_with("riscv"sv))
            return EINVAL;

        u64 interrupt_specifier = 0;
        if (interrupt.interrupt_identifier.size() == sizeof(u32))
            interrupt_specifier = *reinterpret_cast<BigEndian<u32> const*>(interrupt.interrupt_identifier.data());
        else if (interrupt.interrupt_identifier.size() == sizeof(u64))
            interrupt_specifier = *reinterpret_cast<BigEndian<u64> const*>(interrupt.interrupt_identifier.data());
        else
            return EINVAL;

        // https://www.kernel.org/doc/Documentation/devicetree/bindings/riscv/cpus.yaml
        // reg: "The hart ID of this CPU node."
        auto hart_id = TRY(TRY(TRY(cpu->reg()).entry(0)).bus_address().as_flatptr());
        if (hart_id == g_boot_info.arch_specific.boot_hart_id && interrupt_specifier == (to_underlying(RISCV64::CSR::SCAUSE::SupervisorExternalInterrupt) & ~RISCV64::CSR::SCAUSE_INTERRUPT_MASK)) {
            boot_hart_supervisor_mode_context_id = context_id;
            break;
        }
    }

    DeviceTree::DeviceRecipe<NonnullLockRefPtr<IRQController>> recipe {
        name(),
        device.node_name(),
        [physical_address, max_interrupt_id, boot_hart_supervisor_mode_context_id]() -> ErrorOr<NonnullLockRefPtr<IRQController>> {
            auto registers_mapping = TRY(Memory::map_typed_writable<PLIC::RegisterMap volatile>(physical_address));
            return adopt_nonnull_lock_ref_or_enomem(new (nothrow) PLIC(move(registers_mapping), max_interrupt_id + 1, boot_hart_supervisor_mode_context_id));
        },
    };

    InterruptManagement::add_recipe(move(recipe));

    return {};
}

}
