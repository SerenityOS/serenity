#include <Kernel/E1000NetworkAdapter.h>
#include <Kernel/PCI.h>
#include <Kernel/IO.h>

#define REG_CTRL        0x0000
#define REG_STATUS      0x0008
#define REG_EEPROM      0x0014
#define REG_CTRL_EXT    0x0018
#define REG_IMASK       0x00D0
#define REG_RCTRL       0x0100
#define REG_RXDESCLO    0x2800
#define REG_RXDESCHI    0x2804
#define REG_RXDESCLEN   0x2808
#define REG_RXDESCHEAD  0x2810
#define REG_RXDESCTAIL  0x2818
#define REG_TCTRL       0x0400
#define REG_TXDESCLO    0x3800
#define REG_TXDESCHI    0x3804
#define REG_TXDESCLEN   0x3808
#define REG_TXDESCHEAD  0x3810
#define REG_TXDESCTAIL  0x3818
#define REG_RDTR        0x2820 // RX Delay Timer Register
#define REG_RXDCTL      0x3828 // RX Descriptor Control
#define REG_RADV        0x282C // RX Int. Absolute Delay Timer
#define REG_RSRPD       0x2C00 // RX Small Packet Detect Interrupt
#define REG_TIPG        0x0410 // Transmit Inter Packet Gap

OwnPtr<E1000NetworkAdapter> E1000NetworkAdapter::autodetect()
{
    static const PCI::ID qemu_bochs_vbox_id = { 0x8086, 0x100e };
    PCI::Address found_address;
    PCI::enumerate_all([&] (const PCI::Address& address, PCI::ID id) {
        if (id == qemu_bochs_vbox_id) {
            found_address = address;
            return;
        }
    });
    if (found_address.is_null())
        return nullptr;
    byte irq = PCI::get_interrupt_line(found_address);
    return make<E1000NetworkAdapter>(found_address, irq);
}

E1000NetworkAdapter::E1000NetworkAdapter(PCI::Address pci_address, byte irq)
    : IRQHandler(irq)
    , m_pci_address(pci_address)
{
    kprintf("E1000: Found at PCI address %b:%b:%b\n", pci_address.bus(), pci_address.slot(), pci_address.function());
    m_mmio_base = PhysicalAddress(PCI::get_BAR0(m_pci_address));
    MM.map_for_kernel(LinearAddress(m_mmio_base.get()), m_mmio_base);
    m_use_mmio = true;
    m_io_base = PCI::get_BAR1(m_pci_address) & ~1;
    m_interrupt_line = PCI::get_interrupt_line(m_pci_address);
    kprintf("E1000: IO port base: %w\n", m_io_base);
    kprintf("E1000: MMIO base: P%x\n", m_mmio_base);
    kprintf("E1000: Interrupt line: %u\n", m_interrupt_line);
    detect_eeprom();
    kprintf("E1000: Has EEPROM? %u\n", m_has_eeprom);
    read_mac_address();
    auto* mac = mac_address();
    kprintf("E1000: MAC address: %b:%b:%b:%b:%b:%b\n", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    enable_irq();
}

E1000NetworkAdapter::~E1000NetworkAdapter()
{
}

void E1000NetworkAdapter::handle_irq()
{
    kprintf("E1000: IRQ!\n");
}

void E1000NetworkAdapter::detect_eeprom()
{
    out32(REG_EEPROM, 0x1);
    for (volatile int i = 0; i < 999; ++i) {
        dword data = in32(REG_EEPROM);
        if (data & 0x10) {
            m_has_eeprom = true;
            return;
        }
    }
    m_has_eeprom = false;
}

dword E1000NetworkAdapter::read_eeprom(byte address)
{
    word data = 0;
    dword tmp = 0;
    if (m_has_eeprom) {
        out32(REG_EEPROM, ((dword)address << 8) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 4)))
            ;
    } else {
        out32(REG_EEPROM, ((dword)address << 2) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
            ;
    }
    data = (tmp >> 16) & 0xffff;
    return data;
}

void E1000NetworkAdapter::read_mac_address()
{
    if (m_has_eeprom) {
        byte mac[6];
        dword tmp = read_eeprom(0);
        mac[0] = tmp & 0xff;
        mac[1] = tmp >> 8;
        tmp = read_eeprom(1);
        mac[2] = tmp & 0xff;
        mac[3] = tmp >> 8;
        tmp = read_eeprom(2);
        mac[4] = tmp & 0xff;
        mac[5] = tmp >> 8;
        set_mac_address(mac);
    } else {
        ASSERT_NOT_REACHED();
    }
}

void E1000NetworkAdapter::out8(word address, byte data)
{
    if (m_use_mmio) {
        auto* ptr = (volatile byte*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    IO::out8(m_io_base + address, data);
}

void E1000NetworkAdapter::out16(word address, word data)
{
    if (m_use_mmio) {
        auto* ptr = (volatile word*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    IO::out16(m_io_base + address, data);
}

void E1000NetworkAdapter::out32(word address, dword data)
{
    if (m_use_mmio) {
        auto* ptr = (volatile dword*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    IO::out32(m_io_base + address, data);
}

byte E1000NetworkAdapter::in8(word address)
{
    if (m_use_mmio)
        return *(volatile byte*)(m_mmio_base.get() + address);
    return IO::in8(m_io_base + address);
}

word E1000NetworkAdapter::in16(word address)
{
    if (m_use_mmio)
        return *(volatile word*)(m_mmio_base.get() + address);
    return IO::in16(m_io_base + address);
}

dword E1000NetworkAdapter::in32(word address)
{
    if (m_use_mmio)
        return *(volatile dword*)(m_mmio_base.get() + address);
    return IO::in32(m_io_base + address);
}
