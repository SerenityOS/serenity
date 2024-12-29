/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/PCIMSI.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::PCI {

Device::Device(DeviceIdentifier const& pci_identifier)
    : m_pci_identifier(pci_identifier)
{
    m_pci_identifier->initialize();
    m_interrupt_range.m_start_irq = m_pci_identifier->interrupt_line().value();
    m_interrupt_range.m_irq_count = 1;
}

bool Device::is_msi_capable() const
{
    // FIXME: Support MSI on aarch64 and riscv64
#if ARCH(AARCH64) || ARCH(RISCV64)
    return false;
#endif

    return m_pci_identifier->is_msi_capable();
}

bool Device::is_msix_capable() const
{
    // FIXME: Support MSIx on aarch64 and riscv64
#if ARCH(AARCH64) || ARCH(RISCV64)
    return false;
#endif

    return m_pci_identifier->is_msix_capable();
}

void Device::enable_pin_based_interrupts() const
{
    PCI::enable_interrupt_line(m_pci_identifier);
}
void Device::disable_pin_based_interrupts() const
{
    PCI::disable_interrupt_line(m_pci_identifier);
}

void Device::enable_message_signalled_interrupts()
{
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSI)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) | msi_control_enable);
    }
}
void Device::disable_message_signalled_interrupts()
{
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSI)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) & ~(msi_control_enable));
    }
}

void Device::enable_extended_message_signalled_interrupts()
{
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) | msix_control_enable);
    }
}

void Device::disable_extended_message_signalled_interrupts()
{
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) & ~(msix_control_enable));
    }
}

PCI::InterruptType Device::get_interrupt_type()
{
    return m_interrupt_range.m_type;
}

// Reserve `numbers_of_irqs` for this device. Returns the interrupt type
// that was reserved. It is a noop for pin based interrupts as there
// is nothing left to do. The second parameter `msi` is used by the
// driver to indicate its intent to use message signalled interrupts.
// MSI(x) is preferred over MSI if the device supports both.
ErrorOr<InterruptType> Device::reserve_irqs(u8 number_of_irqs, bool msi)
{
    // Let us not allow partial allocation of IRQs for MSIx.
    if (msi && is_msix_capable()) {
        m_interrupt_range.m_start_irq = TRY(reserve_interrupt_handlers(number_of_irqs));
        m_interrupt_range.m_irq_count = number_of_irqs;
        m_interrupt_range.m_type = InterruptType::MSIX;
        // If MSIx is available, disable the pin based interrupts
        disable_pin_based_interrupts();
        enable_extended_message_signalled_interrupts();
    } else if (msi && is_msi_capable()) {
        // TODO: Add MME support. Fallback to pin-based until this support is added.
        if (number_of_irqs > 1)
            return m_interrupt_range.m_type;

        m_interrupt_range.m_start_irq = TRY(reserve_interrupt_handlers(number_of_irqs));
        m_interrupt_range.m_irq_count = number_of_irqs;
        m_interrupt_range.m_type = InterruptType::MSI;
        disable_pin_based_interrupts();
        enable_message_signalled_interrupts();
    }
    return m_interrupt_range.m_type;
}

PhysicalAddress Device::msix_table_entry_address(u8 irq)
{
    auto index = static_cast<int>(irq) - m_interrupt_range.m_start_irq;

    VERIFY(index < m_interrupt_range.m_irq_count);
    VERIFY(index >= 0);
    auto table_bar_paddr = PCI::get_bar_address(device_identifier(), static_cast<PCI::HeaderType0BaseRegister>(m_pci_identifier->get_msix_table_bar())).release_value_but_fixme_should_propagate_errors();
    auto table_offset = m_pci_identifier->get_msix_table_offset();

    return table_bar_paddr.offset(table_offset + (index * 16));
}

// This function is used to allocate an irq at an index and returns
// the actual IRQ that was programmed at that index. This function is
// mainly useful for MSI/MSIx based interrupt mechanism where the driver
// needs to program. If the PCI device doesn't support MSIx interrupts, then
// this function will just return the irq used for pin based interrupt.
ErrorOr<u8> Device::allocate_irq(u8 index)
{
    if (Checked<u8>::addition_would_overflow(m_interrupt_range.m_start_irq, index))
        return Error::from_errno(EINVAL);

    if ((m_interrupt_range.m_type == InterruptType::MSIX) && is_msix_capable()) {
        auto entry_ptr = TRY(Memory::map_typed_writable<MSIxTableEntry volatile>(msix_table_entry_address(index + m_interrupt_range.m_start_irq)));
        entry_ptr->data = msi_data_register(m_interrupt_range.m_start_irq + index, false, false);
        // TODO: we map all the IRQs to cpu 0 by default. We could attach
        //  cpu affinity in the future where specific LAPIC id could be used.
        u64 addr = msi_address_register(0, false, false);
        entry_ptr->address_low = addr & 0xffffffff;
        entry_ptr->address_high = addr >> 32;

        u32 vector_ctrl = msix_vector_control_register(entry_ptr->vector_control, true);
        entry_ptr->vector_control = vector_ctrl;

        return m_interrupt_range.m_start_irq + index;
    } else if ((m_interrupt_range.m_type == InterruptType::MSI) && is_msi_capable()) {
        // TODO: Add MME support.
        if (index > 0)
            return Error::from_errno(EINVAL);

        auto data = msi_data_register(m_interrupt_range.m_start_irq + index, false, false);
        auto addr = msi_address_register(0, false, false);
        for (auto& capability : m_pci_identifier->capabilities()) {
            if (capability.id().value() == PCI::Capabilities::ID::MSI) {
                capability.write32(msi_address_low_offset, addr & 0xffffffff);

                if (!m_pci_identifier->is_msi_64bit_address_format()) {
                    capability.write16(msi_address_high_or_data_offset, data);
                    break;
                }

                capability.write32(msi_address_high_or_data_offset, addr >> 32);
                capability.write16(msi_data_offset, data);
            }
        }
        return m_interrupt_range.m_start_irq + index;
    }
    // For pin based interrupts, we share the IRQ.
    return m_interrupt_range.m_start_irq;
}

void Device::enable_interrupt(u8 irq)
{
    if ((m_interrupt_range.m_type == InterruptType::MSIX) && is_msix_capable()) {
        auto entry = Memory::map_typed_writable<MSIxTableEntry volatile>(PhysicalAddress(msix_table_entry_address(irq)));

        if (entry.is_error()) {
            dmesgln_pci(*this, "Unable to map the MSIx table area");
            return;
        }

        auto entry_ptr = entry.release_value();
        u32 vector_ctrl = msix_vector_control_register(entry_ptr->vector_control, false);
        entry_ptr->vector_control = vector_ctrl;
    } else if ((m_interrupt_range.m_type == InterruptType::MSI) && is_msi_capable()) {
        enable_message_signalled_interrupts();
    }
}

void Device::disable_interrupt(u8 irq)
{
    if ((m_interrupt_range.m_type == InterruptType::MSIX) && is_msix_capable()) {
        auto entry = Memory::map_typed_writable<MSIxTableEntry volatile>(PhysicalAddress(msix_table_entry_address(irq)));

        if (entry.is_error()) {
            dmesgln_pci(*this, "Unable to map the MSIx table area");
            return;
        }

        auto entry_ptr = entry.release_value();
        u32 vector_ctrl = msix_vector_control_register(entry_ptr->vector_control, true);
        entry_ptr->vector_control = vector_ctrl;
    } else if ((m_interrupt_range.m_type == InterruptType::MSI) && is_msi_capable()) {
        disable_message_signalled_interrupts();
    }
}

}
