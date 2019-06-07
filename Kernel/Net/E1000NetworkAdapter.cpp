#include <Kernel/IO.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/PCI.h>

#define REG_CTRL 0x0000
#define REG_STATUS 0x0008
#define REG_EEPROM 0x0014
#define REG_CTRL_EXT 0x0018
#define REG_IMASK 0x00D0
#define REG_RCTRL 0x0100
#define REG_RXDESCLO 0x2800
#define REG_RXDESCHI 0x2804
#define REG_RXDESCLEN 0x2808
#define REG_RXDESCHEAD 0x2810
#define REG_RXDESCTAIL 0x2818
#define REG_TCTRL 0x0400
#define REG_TXDESCLO 0x3800
#define REG_TXDESCHI 0x3804
#define REG_TXDESCLEN 0x3808
#define REG_TXDESCHEAD 0x3810
#define REG_TXDESCTAIL 0x3818
#define REG_RDTR 0x2820             // RX Delay Timer Register
#define REG_RXDCTL 0x3828           // RX Descriptor Control
#define REG_RADV 0x282C             // RX Int. Absolute Delay Timer
#define REG_RSRPD 0x2C00            // RX Small Packet Detect Interrupt
#define REG_TIPG 0x0410             // Transmit Inter Packet Gap
#define ECTRL_SLU 0x40              //set link up
#define RCTL_EN (1 << 1)            // Receiver Enable
#define RCTL_SBP (1 << 2)           // Store Bad Packets
#define RCTL_UPE (1 << 3)           // Unicast Promiscuous Enabled
#define RCTL_MPE (1 << 4)           // Multicast Promiscuous Enabled
#define RCTL_LPE (1 << 5)           // Long Packet Reception Enable
#define RCTL_LBM_NONE (0 << 6)      // No Loopback
#define RCTL_LBM_PHY (3 << 6)       // PHY or external SerDesc loopback
#define RTCL_RDMTS_HALF (0 << 8)    // Free Buffer Threshold is 1/2 of RDLEN
#define RTCL_RDMTS_QUARTER (1 << 8) // Free Buffer Threshold is 1/4 of RDLEN
#define RTCL_RDMTS_EIGHTH (2 << 8)  // Free Buffer Threshold is 1/8 of RDLEN
#define RCTL_MO_36 (0 << 12)        // Multicast Offset - bits 47:36
#define RCTL_MO_35 (1 << 12)        // Multicast Offset - bits 46:35
#define RCTL_MO_34 (2 << 12)        // Multicast Offset - bits 45:34
#define RCTL_MO_32 (3 << 12)        // Multicast Offset - bits 43:32
#define RCTL_BAM (1 << 15)          // Broadcast Accept Mode
#define RCTL_VFE (1 << 18)          // VLAN Filter Enable
#define RCTL_CFIEN (1 << 19)        // Canonical Form Indicator Enable
#define RCTL_CFI (1 << 20)          // Canonical Form Indicator Bit Value
#define RCTL_DPF (1 << 22)          // Discard Pause Frames
#define RCTL_PMCF (1 << 23)         // Pass MAC Control Frames
#define RCTL_SECRC (1 << 26)        // Strip Ethernet CRC

// Buffer Sizes
#define RCTL_BSIZE_256 (3 << 16)
#define RCTL_BSIZE_512 (2 << 16)
#define RCTL_BSIZE_1024 (1 << 16)
#define RCTL_BSIZE_2048 (0 << 16)
#define RCTL_BSIZE_4096 ((3 << 16) | (1 << 25))
#define RCTL_BSIZE_8192 ((2 << 16) | (1 << 25))
#define RCTL_BSIZE_16384 ((1 << 16) | (1 << 25))

// Transmit Command

#define CMD_EOP (1 << 0)  // End of Packet
#define CMD_IFCS (1 << 1) // Insert FCS
#define CMD_IC (1 << 2)   // Insert Checksum
#define CMD_RS (1 << 3)   // Report Status
#define CMD_RPS (1 << 4)  // Report Packet Sent
#define CMD_VLE (1 << 6)  // VLAN Packet Enable
#define CMD_IDE (1 << 7)  // Interrupt Delay Enable

// TCTL Register

#define TCTL_EN (1 << 1)      // Transmit Enable
#define TCTL_PSP (1 << 3)     // Pad Short Packets
#define TCTL_CT_SHIFT 4       // Collision Threshold
#define TCTL_COLD_SHIFT 12    // Collision Distance
#define TCTL_SWXOFF (1 << 22) // Software XOFF Transmission
#define TCTL_RTLC (1 << 24)   // Re-transmit on Late Collision

#define TSTA_DD (1 << 0) // Descriptor Done
#define TSTA_EC (1 << 1) // Excess Collisions
#define TSTA_LC (1 << 2) // Late Collision
#define LSTA_TU (1 << 3) // Transmit Underrun

OwnPtr<E1000NetworkAdapter> E1000NetworkAdapter::autodetect()
{
    static const PCI::ID qemu_bochs_vbox_id = { 0x8086, 0x100e };
    PCI::Address found_address;
    PCI::enumerate_all([&](const PCI::Address& address, PCI::ID id) {
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

static E1000NetworkAdapter* s_the;
E1000NetworkAdapter* E1000NetworkAdapter::the()
{
    return s_the;
}

E1000NetworkAdapter::E1000NetworkAdapter(PCI::Address pci_address, byte irq)
    : IRQHandler(irq)
    , m_pci_address(pci_address)
{
    s_the = this;
    kprintf("E1000: Found at PCI address %b:%b:%b\n", pci_address.bus(), pci_address.slot(), pci_address.function());

    enable_bus_mastering(m_pci_address);

    m_mmio_base = PhysicalAddress(PCI::get_BAR0(m_pci_address));
    MM.map_for_kernel(LinearAddress(m_mmio_base.get()), m_mmio_base);
    MM.map_for_kernel(LinearAddress(m_mmio_base.offset(4096).get()), m_mmio_base.offset(4096));
    MM.map_for_kernel(LinearAddress(m_mmio_base.offset(8192).get()), m_mmio_base.offset(8192));
    MM.map_for_kernel(LinearAddress(m_mmio_base.offset(12288).get()), m_mmio_base.offset(12288));
    MM.map_for_kernel(LinearAddress(m_mmio_base.offset(16384).get()), m_mmio_base.offset(16384));
    m_use_mmio = true;
    m_io_base = PCI::get_BAR1(m_pci_address) & ~1;
    m_interrupt_line = PCI::get_interrupt_line(m_pci_address);
    kprintf("E1000: IO port base: %w\n", m_io_base);
    kprintf("E1000: MMIO base: P%x\n", m_mmio_base);
    kprintf("E1000: Interrupt line: %u\n", m_interrupt_line);
    detect_eeprom();
    kprintf("E1000: Has EEPROM? %u\n", m_has_eeprom);
    read_mac_address();
    const auto& mac = mac_address();
    kprintf("E1000: MAC address: %b:%b:%b:%b:%b:%b\n", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    dword flags = in32(REG_CTRL);
    out32(REG_CTRL, flags | ECTRL_SLU);

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    out32(REG_IMASK, 0x1f6dc);
    out32(REG_IMASK, 0xff & ~4);
    in32(0xc0);

    enable_irq();
}

E1000NetworkAdapter::~E1000NetworkAdapter()
{
}

void E1000NetworkAdapter::handle_irq()
{
    out32(REG_IMASK, 0x1);

    dword status = in32(0xc0);
    if (status & 4) {
        dword flags = in32(REG_CTRL);
        out32(REG_CTRL, flags | ECTRL_SLU);
    }
    if (status & 0x10) {
        // Threshold OK?
    }
    if (status & 0x80) {
        receive();
    }
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

void E1000NetworkAdapter::initialize_rx_descriptors()
{
    auto ptr = (dword)kmalloc_eternal(sizeof(e1000_rx_desc) * number_of_rx_descriptors + 16);
    // Make sure it's 16-byte aligned.
    if (ptr % 16)
        ptr = (ptr + 16) - (ptr % 16);
    m_rx_descriptors = (e1000_rx_desc*)ptr;
    for (int i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = m_rx_descriptors[i];
        descriptor.addr = (qword)kmalloc_eternal(8192 + 16);
        descriptor.status = 0;
    }

    out32(REG_RXDESCLO, ptr);
    out32(REG_RXDESCHI, 0);
    out32(REG_RXDESCLEN, number_of_rx_descriptors * sizeof(e1000_rx_desc));
    out32(REG_RXDESCHEAD, 0);
    out32(REG_RXDESCTAIL, number_of_rx_descriptors - 1);

    out32(REG_RCTRL, RCTL_EN | RCTL_SBP | RCTL_UPE | RCTL_MPE | RCTL_LBM_NONE | RTCL_RDMTS_HALF | RCTL_BAM | RCTL_SECRC | RCTL_BSIZE_8192);
}

void E1000NetworkAdapter::initialize_tx_descriptors()
{
    auto ptr = (dword)kmalloc_eternal(sizeof(e1000_tx_desc) * number_of_tx_descriptors + 16);
    // Make sure it's 16-byte aligned.
    if (ptr % 16)
        ptr = (ptr + 16) - (ptr % 16);
    m_tx_descriptors = (e1000_tx_desc*)ptr;
    for (int i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        descriptor.addr = (qword)kmalloc_eternal(8192 + 16);
        descriptor.cmd = 0;
    }

    out32(REG_TXDESCLO, ptr);
    out32(REG_TXDESCHI, 0);
    out32(REG_TXDESCLEN, number_of_tx_descriptors * sizeof(e1000_tx_desc));
    out32(REG_TXDESCHEAD, 0);
    out32(REG_TXDESCTAIL, 0);

    out32(REG_TCTRL, in32(REG_TCTRL) | TCTL_EN | TCTL_PSP);
    out32(REG_TIPG, 0x0060200A);
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

void E1000NetworkAdapter::send_raw(const byte* data, int length)
{
    dword tx_current = in32(REG_TXDESCTAIL);
#ifdef E1000_DEBUG
    kprintf("E1000: Sending packet (%d bytes)\n", length);
#endif
    auto& descriptor = m_tx_descriptors[tx_current];
    ASSERT(length <= 8192);
    memcpy((void*)descriptor.addr, data, length);
    descriptor.length = length;
    descriptor.status = 0;
    descriptor.cmd = CMD_EOP | CMD_IFCS | CMD_RS;
#ifdef E1000_DEBUG
    kprintf("E1000: Using tx descriptor %d (head is at %d)\n", tx_current, in32(REG_TXDESCHEAD));
#endif
    tx_current = (tx_current + 1) % number_of_tx_descriptors;
    out32(REG_TXDESCTAIL, tx_current);
    while (!descriptor.status)
        ;
#ifdef E1000_DEBUG
    kprintf("E1000: Sent packet, status is now %b!\n", descriptor.status);
#endif
}

void E1000NetworkAdapter::receive()
{
    dword rx_current;
    for (;;) {
        rx_current = in32(REG_RXDESCTAIL);
        if (rx_current == in32(REG_RXDESCHEAD))
            return;
        rx_current = (rx_current + 1) % number_of_rx_descriptors;
        if (!(m_rx_descriptors[rx_current].status & 1))
            break;
        auto* buffer = (byte*)m_rx_descriptors[rx_current].addr;
        word length = m_rx_descriptors[rx_current].length;
#ifdef E1000_DEBUG
        kprintf("E1000: Received 1 packet @ %p (%u) bytes!\n", buffer, length);
#endif
        did_receive(buffer, length);
        m_rx_descriptors[rx_current].status = 0;
        out32(REG_RXDESCTAIL, rx_current);
    }
}
