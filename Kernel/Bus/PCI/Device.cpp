/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/PCIMSI.h>
#include <Kernel/Bus/PCI/Bus.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::PCI {

void Device::set_parent_bus(Bus& parent_bus)
{
    VERIFY(!m_parent_bus);
    m_parent_bus = &parent_bus;
}

void Device::set_host_controller(PCI::HostController& host_controller)
{
    VERIFY(!m_host_controller);
    m_host_controller = &host_controller;
}

ErrorOr<NonnullRefPtr<Device>> Device::from_enumerable_identifier(EnumerableDeviceIdentifier const& identifier)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Device(identifier));
}

Device::Device(EnumerableDeviceIdentifier const& identifier)
    : m_device_id(identifier)
{
    m_interrupt_range.m_start_irq = identifier.interrupt_line().value();
    m_interrupt_range.m_irq_count = 1;
}

void Device::set_capabilities(Vector<Capability> capabilities)
{
    VERIFY(m_capabilities.is_empty());
    m_capabilities = move(capabilities);
}

u8 Device::config_space_read8_locked(PCI::RegisterOffset field)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_host_controller);
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    return m_host_controller->read8_field(bus_number, device_number, function_number, static_cast<u32>(field));
}

u16 Device::config_space_read16_locked(PCI::RegisterOffset field)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_host_controller);
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    return m_host_controller->read16_field(bus_number, device_number, function_number, static_cast<u32>(field));
}

u32 Device::config_space_read32_locked(PCI::RegisterOffset field)
{
    VERIFY(m_operation_lock.is_locked());
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    return m_host_controller->read32_field(bus_number, device_number, function_number, static_cast<u32>(field));
}

void Device::config_space_write8_locked(PCI::RegisterOffset field, u8 value)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_host_controller);
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    m_host_controller->write8_field(bus_number, device_number, function_number, static_cast<u32>(field), value);
}

void Device::config_space_write16_locked(PCI::RegisterOffset field, u16 value)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_host_controller);
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    m_host_controller->write16_field(bus_number, device_number, function_number, static_cast<u32>(field), value);
}

void Device::config_space_write32_locked(PCI::RegisterOffset field, u32 value)
{
    VERIFY(m_operation_lock.is_locked());
    VERIFY(m_host_controller);
    auto const& device_address = device_id().address();
    auto bus_number = device_address.bus();
    auto device_number = device_address.device();
    auto function_number = device_address.function();
    m_host_controller->write32_field(bus_number, device_number, function_number, static_cast<u32>(field), value);
}

void Device::enable_pin_based_interrupts()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) & ~(1 << 10));
}

void Device::disable_pin_based_interrupts()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) | 1 << 10);
}

void Device::enable_io_space()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) | (1 << 0));
}
void Device::disable_io_space()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) & ~(1 << 0));
}

void Device::enable_memory_space()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) | (1 << 1));
}
void Device::disable_memory_space()
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND,
        config_space_read16_locked(PCI::RegisterOffset::COMMAND) & ~(1 << 1));
}

void Device::enable_bus_mastering()
{
    SpinlockLocker locker(m_operation_lock);
    auto value = config_space_read16_locked(PCI::RegisterOffset::COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND, value);
}

void Device::disable_bus_mastering()
{
    SpinlockLocker locker(m_operation_lock);
    auto value = config_space_read16_locked(PCI::RegisterOffset::COMMAND);
    value &= ~(1 << 2);
    value |= (1 << 0);
    config_space_write16_locked(PCI::RegisterOffset::COMMAND, value);
}

void Device::enable_message_signalled_interrupts()
{
    for (auto& capability : m_capabilities) {
        if (capability.id().value() == PCI::Capabilities::ID::MSI)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) | msi_control_enable);
    }
}
void Device::disable_message_signalled_interrupts()
{
    for (auto& capability : m_capabilities) {
        if (capability.id().value() == PCI::Capabilities::ID::MSI)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) & ~(msi_control_enable));
    }
}

void Device::enable_extended_message_signalled_interrupts()
{
    for (auto& capability : m_capabilities) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) | msix_control_enable);
    }
}

void Device::disable_extended_message_signalled_interrupts()
{
    for (auto& capability : m_capabilities) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) & ~(msix_control_enable));
    }
}

PCI::InterruptType Device::get_interrupt_type()
{
    return m_interrupt_range.m_type;
}

u8 Device::config_space_read8(PCI::RegisterOffset field)
{
    SpinlockLocker locker(m_operation_lock);
    return config_space_read8_locked(field);
}

u16 Device::config_space_read16(PCI::RegisterOffset field)
{
    SpinlockLocker locker(m_operation_lock);
    return config_space_read16_locked(field);
}

u32 Device::config_space_read32(PCI::RegisterOffset field)
{
    SpinlockLocker locker(m_operation_lock);
    return config_space_read32_locked(field);
}

void Device::config_space_write8(PCI::RegisterOffset field, u8 value)
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write8_locked(field, value);
}

void Device::config_space_write16(PCI::RegisterOffset field, u16 value)
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write16_locked(field, value);
}

void Device::config_space_write32(PCI::RegisterOffset field, u32 value)
{
    SpinlockLocker locker(m_operation_lock);
    config_space_write32_locked(field, value);
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
    auto table_bar_ptr = m_resources[static_cast<size_t>(get_msix_table_bar())].physical_memory_address();
    auto table_offset = get_msix_table_offset();

    return table_bar_ptr.offset(table_offset + (index * 16));
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
        for (auto& capability : m_capabilities) {
            if (capability.id().value() == PCI::Capabilities::ID::MSI) {
                capability.write32(msi_address_low_offset, addr & 0xffffffff);

                if (!is_msi_64bit_address_format()) {
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
