/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Format.h>
#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Capability.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Resource.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

namespace PCI {

enum class InterruptType {
    PIN,
    MSI,
    MSIX
};

struct InterruptRange {
    u8 m_start_irq { 0 };
    u8 m_irq_count { 0 };
    InterruptType m_type { InterruptType::PIN };
};

struct [[gnu::packed]] MSIxTableEntry {
    u32 address_low;
    u32 address_high;
    u32 data;
    u32 vector_control;
};

class Bus;
class HostController;
class Driver;
class Device final : public AtomicRefCounted<Device> {
    AK_MAKE_NONCOPYABLE(Device);

    friend class Bus;
    friend class HostController;
    friend class Driver;
    friend struct ClassedDriverDeviceLists;

public:
    static ErrorOr<NonnullRefPtr<Device>> from_enumerable_identifier(EnumerableDeviceIdentifier const& identifier);

    bool is_msix_capable() const { return m_msix_info.table_size > 0; }
    PCI::HeaderType0BaseRegister get_msix_table_bar() const { return static_cast<PCI::HeaderType0BaseRegister>(m_msix_info.table_bar); }
    u32 get_msix_table_offset() const { return m_msix_info.table_offset; }

    bool is_msi_capable() const { return m_msi_info.count > 0; }
    bool is_msi_64bit_address_format() { return m_msi_info.message_address_64_bit_format; }

    Spinlock<LockRank::None>& operation_lock() { return m_operation_lock; }
    Array<Resource, 6> const& resources() const { return m_resources; }
    Vector<Capability> const& capabilities() const { return m_capabilities; }
    EnumerableDeviceIdentifier const& device_id() const { return m_device_id; }
    Bus* parent_bus() { return m_parent_bus; }

    SpinlockProtected<RawPtr<PCI::Driver>, LockRank::None>& driver(Badge<Access>) { return m_driver; }

    template<typename T>
    inline ErrorOr<Memory::TypedMapping<T>> map_resource(HeaderType0BaseRegister bar, size_t size = sizeof(T), Memory::Region::Access access = IsConst<T> ? Memory::Region::Access::Read : Memory::Region::Access::ReadWrite)
    {
        VERIFY(to_underlying(bar) >= 0);
        VERIFY(static_cast<size_t>(to_underlying(bar)) < m_resources.size());
        auto& resource = m_resources[static_cast<size_t>(to_underlying(bar))];
        auto pci_bar_space_type = resource.type;

        if (pci_bar_space_type == PCI::Resource::SpaceType::IOSpace)
            return EIO;

        auto bar_address = PhysicalAddress(resource.address);
        auto pci_bar_space_size = resource.length;
        if (pci_bar_space_size < size)
            return Error::from_errno(EIO);

        if (pci_bar_space_type == PCI::Resource::SpaceType::Memory32BitSpace && Checked<u32>::addition_would_overflow(bar_address.get(), size))
            return Error::from_errno(EOVERFLOW);
        if (pci_bar_space_type == PCI::Resource::SpaceType::Memory16BitSpace && Checked<u16>::addition_would_overflow(bar_address.get(), size))
            return Error::from_errno(EOVERFLOW);
        if (pci_bar_space_type == PCI::Resource::SpaceType::Memory64BitSpace && Checked<u64>::addition_would_overflow(bar_address.get(), size))
            return Error::from_errno(EOVERFLOW);

        return Memory::map_typed<T>(bar_address, size, access);
    }

    virtual ~Device() = default;

    void enable_pin_based_interrupts();
    void disable_pin_based_interrupts();

    void enable_message_signalled_interrupts();
    void disable_message_signalled_interrupts();

    void enable_extended_message_signalled_interrupts();
    void disable_extended_message_signalled_interrupts();
    ErrorOr<InterruptType> reserve_irqs(u8 number_of_irqs, bool msi);
    ErrorOr<u8> allocate_irq(u8 index);
    PCI::InterruptType get_interrupt_type();
    void enable_interrupt(u8 irq);
    void disable_interrupt(u8 irq);

    u8 config_space_read8(PCI::RegisterOffset field);
    u16 config_space_read16(PCI::RegisterOffset field);
    u32 config_space_read32(PCI::RegisterOffset field);
    void config_space_write8(PCI::RegisterOffset field, u8);
    void config_space_write16(PCI::RegisterOffset field, u16);
    void config_space_write32(PCI::RegisterOffset field, u32);

    void enable_bus_mastering();
    void disable_bus_mastering();
    void enable_io_space();
    void disable_io_space();
    void enable_memory_space();
    void disable_memory_space();

    // NOTE: Use these only during enumeration!
    // FIXME: Maybe use badges here?
    void set_capabilities(Vector<Capability>);
    void set_parent_bus(Bus&);
    void set_host_controller(PCI::HostController&);

private:
    u8 config_space_read8_locked(PCI::RegisterOffset field);
    u16 config_space_read16_locked(PCI::RegisterOffset field);
    u32 config_space_read32_locked(PCI::RegisterOffset field);

    void config_space_write8_locked(PCI::RegisterOffset field, u8);
    void config_space_write16_locked(PCI::RegisterOffset field, u16);
    void config_space_write32_locked(PCI::RegisterOffset field, u32);

    PCI::Resource m_expansion_rom_resource;
    Array<PCI::Resource, 6> m_resources;

    explicit Device(EnumerableDeviceIdentifier const& pci_identifier);

    PhysicalAddress msix_table_entry_address(u8 irq);

    // NOTE: These class members don't need any locking because we immediately
    // set them after construction and never again afterwards.
    RawPtr<PCI::HostController> m_host_controller {};
    RawPtr<Bus> m_parent_bus {};
    Vector<Capability> m_capabilities;
    IntrusiveListNode<PCI::Device, NonnullRefPtr<PCI::Device>> m_bus_list_node;
    IntrusiveListNode<Device> m_host_controller_list_node;
    IntrusiveListNode<Device> m_classed_list_node;
    EnumerableDeviceIdentifier m_device_id;

    InterruptRange m_interrupt_range;

    SpinlockProtected<RawPtr<PCI::Driver>, LockRank::None> m_driver {};

    Spinlock<LockRank::None> m_operation_lock;
    MSIxInfo m_msix_info {};
    MSIInfo m_msi_info {};
};

}

template<typename... Parameters>
void dmesgln_pci(PCI::Device const& device, AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters)
{
    AK::StringBuilder builder;
    if (builder.try_append("{}: "sv).is_error())
        return;
    if (builder.try_append(fmt.view()).is_error())
        return;
    AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::Yes, PCI::Address, Parameters...> variadic_format_params { device.device_id().address(), parameters... };
    vdmesgln(builder.string_view(), variadic_format_params);
}

}
