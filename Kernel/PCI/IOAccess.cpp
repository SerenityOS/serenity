#include <Kernel/IO.h>
#include <Kernel/PCI/IOAccess.h>

void PCI::IOAccess::initialize()
{
    if (!PCI::Access::is_initialized())
        new PCI::IOAccess();
}

PCI::IOAccess::IOAccess()
{
    kprintf("PCI: Using IO Mechanism for PCI Configuartion Space Access\n");
}

u8 PCI::IOAccess::read8_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}

u16 PCI::IOAccess::read16_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}

u32 PCI::IOAccess::read32_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}

void PCI::IOAccess::write8_field(Address address, u32 field, u8 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out8(PCI_VALUE_PORT + (field & 3), value);
}
void PCI::IOAccess::write16_field(Address address, u32 field, u16 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out16(PCI_VALUE_PORT + (field & 2), value);
}
void PCI::IOAccess::write32_field(Address address, u32 field, u32 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out32(PCI_VALUE_PORT, value);
}

void PCI::IOAccess::enumerate_all(Function<void(Address, ID)>& callback)
{
    // Single PCI host controller.
    if ((read8_field(Address(), PCI_HEADER_TYPE) & 0x80) == 0) {
        enumerate_bus(-1, 0, callback);
        return;
    }

    // Multiple PCI host controllers.
    for (u8 function = 0; function < 8; ++function) {
        if (read16_field(Address(0, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
            break;
        enumerate_bus(-1, function, callback);
    }
}
