#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/IOAccess.h>

static PCI::Access* s_access;

PCI::Access& PCI::Access::the()
{
    if (s_access == nullptr) {
        ASSERT_NOT_REACHED(); // We failed to initialize the PCI subsystem, so stop here!
    }
    return *s_access;
}

bool PCI::Access::is_initialized()
{
    return (s_access != nullptr);
}

PCI::Access::Access()
{
    s_access = this;
}

void PCI::Access::enumerate_functions(int type, u8 bus, u8 slot, u8 function, Function<void(Address, ID)>& callback)
{
    Address address(0, bus, slot, function);
    if (type == -1 || type == read_type(address))
        callback(address, { read16_field(address, PCI_VENDOR_ID), read16_field(address, PCI_DEVICE_ID) });
    if (read_type(address) == PCI_TYPE_BRIDGE) {
        u8 secondary_bus = read8_field(address, PCI_SECONDARY_BUS);
#ifdef PCI_DEBUG
        kprintf("PCI: Found secondary bus: %u\n", secondary_bus);
#endif
        ASSERT(secondary_bus != bus);
        enumerate_bus(type, secondary_bus, callback);
    }
}

void PCI::Access::enumerate_slot(int type, u8 bus, u8 slot, Function<void(Address, ID)>& callback)
{
    Address address(0, bus, slot, 0);
    if (read16_field(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, slot, 0, callback);
    if (!(read8_field(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        Address address(0, bus, slot, function);
        if (read16_field(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, slot, function, callback);
    }
}

void PCI::Access::enumerate_bus(int type, u8 bus, Function<void(Address, ID)>& callback)
{
    for (u8 slot = 0; slot < 32; ++slot)
        enumerate_slot(type, bus, slot, callback);
}

void PCI::Access::enable_bus_mastering(Address address)
{
    auto value = read16_field(address, PCI_COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    write16_field(address, PCI_COMMAND, value);
}

void PCI::Access::disable_bus_mastering(Address address)
{
    auto value = read16_field(address, PCI_COMMAND);
    value &= ~(1 << 2);
    value |= (1 << 0);
    write16_field(address, PCI_COMMAND, value);
}

namespace PCI {
void enumerate_all(Function<void(Address, ID)> callback)
{
    PCI::Access::the().enumerate_all(callback);
}

u8 get_interrupt_line(Address address)
{
    return PCI::Access::the().get_interrupt_line(address);
}
u32 get_BAR0(Address address)
{
    return PCI::Access::the().get_BAR0(address);
}
u32 get_BAR1(Address address)
{
    return PCI::Access::the().get_BAR1(address);
}
u32 get_BAR2(Address address)
{
    return PCI::Access::the().get_BAR2(address);
}
u32 get_BAR3(Address address)
{
    return PCI::Access::the().get_BAR3(address);
}
u32 get_BAR4(Address address)
{
    return PCI::Access::the().get_BAR4(address);
}
u32 get_BAR5(Address address)
{
    return PCI::Access::the().get_BAR5(address);
}
u8 get_revision_id(Address address)
{
    return PCI::Access::the().get_revision_id(address);
}
u8 get_subclass(Address address)
{
    return PCI::Access::the().get_subclass(address);
}
u8 get_class(Address address)
{
    return PCI::Access::the().get_class(address);
}
u16 get_subsystem_id(Address address)
{
    return PCI::Access::the().get_subsystem_id(address);
}
u16 get_subsystem_vendor_id(Address address)
{
    return PCI::Access::the().get_subsystem_vendor_id(address);
}
void enable_bus_mastering(Address address)
{
    PCI::Access::the().enable_bus_mastering(address);
}
void disable_bus_mastering(Address address)
{
    PCI::Access::the().disable_bus_mastering(address);
}
size_t get_BAR_Space_Size(Address address, u8 bar_number)
{
    return PCI::Access::the().get_BAR_Space_Size(address, bar_number);
}
}
