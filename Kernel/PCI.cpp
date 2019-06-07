#include <Kernel/IO.h>
#include <Kernel/PCI.h>

#define PCI_VENDOR_ID 0x00       // word
#define PCI_DEVICE_ID 0x02       // word
#define PCI_COMMAND 0x04         // word
#define PCI_STATUS 0x06          // word
#define PCI_REVISION_ID 0x08     // byte
#define PCI_PROG_IF 0x09         // byte
#define PCI_SUBCLASS 0x0a        // byte
#define PCI_CLASS 0x0b           // byte
#define PCI_CACHE_LINE_SIZE 0x0c // byte
#define PCI_LATENCY_TIMER 0x0d   // byte
#define PCI_HEADER_TYPE 0x0e     // byte
#define PCI_BIST 0x0f            // byte
#define PCI_BAR0 0x10            // dword
#define PCI_BAR1 0x14            // dword
#define PCI_BAR2 0x18            // dword
#define PCI_BAR3 0x1C            // dword
#define PCI_BAR4 0x20            // dword
#define PCI_BAR5 0x24            // dword
#define PCI_INTERRUPT_LINE 0x3C  // byte
#define PCI_SECONDARY_BUS 0x19   // byte
#define PCI_HEADER_TYPE_DEVICE 0
#define PCI_HEADER_TYPE_BRIDGE 1
#define PCI_TYPE_BRIDGE 0x0604
#define PCI_ADDRESS_PORT 0xCF8
#define PCI_VALUE_PORT 0xCFC
#define PCI_NONE 0xFFFF

namespace PCI {

template<typename T>
T read_field(Address address, dword field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    if constexpr (sizeof(T) == 4)
        return IO::in32(PCI_VALUE_PORT);
    if constexpr (sizeof(T) == 2)
        return IO::in16(PCI_VALUE_PORT + (field & 2));
    if constexpr (sizeof(T) == 1)
        return IO::in8(PCI_VALUE_PORT + (field & 3));
}

template<typename T>
void write_field(Address address, dword field, T value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    if constexpr (sizeof(T) == 4)
        IO::out32(PCI_VALUE_PORT, value);
    if constexpr (sizeof(T) == 2)
        IO::out16(PCI_VALUE_PORT + (field & 2), value);
    if constexpr (sizeof(T) == 1)
        IO::out8(PCI_VALUE_PORT + (field & 3), value);
}

word read_type(Address address)
{
    return (read_field<byte>(address, PCI_CLASS) << 8u) | read_field<byte>(address, PCI_SUBCLASS);
}

void enumerate_bus(int type, byte bus, Function<void(Address, ID)>&);

void enumerate_functions(int type, byte bus, byte slot, byte function, Function<void(Address, ID)>& callback)
{
    Address address(bus, slot, function);
    if (type == -1 || type == read_type(address))
        callback(address, { read_field<word>(address, PCI_VENDOR_ID), read_field<word>(address, PCI_DEVICE_ID) });
    if (read_type(address) == PCI_TYPE_BRIDGE) {
        byte secondary_bus = read_field<byte>(address, PCI_SECONDARY_BUS);
        kprintf("PCI: Found secondary bus: %u\n", secondary_bus);
        ASSERT(secondary_bus != bus);
        enumerate_bus(type, secondary_bus, callback);
    }
}

void enumerate_slot(int type, byte bus, byte slot, Function<void(Address, ID)>& callback)
{
    Address address(bus, slot, 0);
    if (read_field<word>(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, slot, 0, callback);
    if (!(read_field<byte>(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (byte function = 1; function < 8; ++function) {
        Address address(bus, slot, function);
        if (read_field<word>(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, slot, function, callback);
    }
}

void enumerate_bus(int type, byte bus, Function<void(Address, ID)>& callback)
{
    for (byte slot = 0; slot < 32; ++slot)
        enumerate_slot(type, bus, slot, callback);
}

byte get_interrupt_line(Address address) { return read_field<byte>(address, PCI_INTERRUPT_LINE); }
dword get_BAR0(Address address) { return read_field<dword>(address, PCI_BAR0); }
dword get_BAR1(Address address) { return read_field<dword>(address, PCI_BAR1); }
dword get_BAR2(Address address) { return read_field<dword>(address, PCI_BAR2); }
dword get_BAR3(Address address) { return read_field<dword>(address, PCI_BAR3); }
dword get_BAR4(Address address) { return read_field<dword>(address, PCI_BAR4); }
dword get_BAR5(Address address) { return read_field<dword>(address, PCI_BAR5); }

void enable_bus_mastering(Address address)
{
    auto value = read_field<word>(address, PCI_COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    write_field<word>(address, PCI_COMMAND, value);
}

void enumerate_all(Function<void(Address, ID)> callback)
{
    // Single PCI host controller.
    if ((read_field<byte>(Address(), PCI_HEADER_TYPE) & 0x80) == 0) {
        enumerate_bus(-1, 0, callback);
        return;
    }

    // Multiple PCI host controllers.
    for (byte function = 0; function < 8; ++function) {
        if (read_field<word>(Address(0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
            break;
        enumerate_bus(-1, function, callback);
    }
}

}
