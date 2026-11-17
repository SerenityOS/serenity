/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define REG_MAC 0x00
#define REG_MAR0 0x08
#define REG_MAR4 0x0C
#define REG_EEE_LED 0x1B
#define REG_TXADDR 0x20
#define REG_COMMAND 0x37
#define REG_TXSTART 0x38
#define REG_IMR 0x3C
#define REG_IMR_8125 0x38
#define REG_ISR 0x3E
#define REG_ISR_8125 0x3C
#define REG_TXCFG 0x40
#define REG_RXCFG 0x44
#define REG_MPC 0x4C
#define REG_CFG9346 0x50
#define REG_CONFIG1 0x52
#define REG_CONFIG2 0x53
#define REG_CONFIG3 0x54
#define REG_CONFIG4 0x55
#define REG_CONFIG5 0x56
#define REG_MULTIINTR 0x5C
#define REG_PHYACCESS 0x60
#define REG_CSI_DATA 0x64
#define REG_CSI_ADDR 0x68
#define REG_PHYSTATUS 0x6C
#define REG_MACDBG 0x6D
#define REG_GPIO 0x6E
#define REG_PMCH 0x6F
#define REG_ERI_DATA 0x70
#define REG_ERI_ADDR 0x74
#define REG_EPHYACCESS 0x80
#define REG_OCP_DATA 0xB0
#define REG_OCP_ADDR 0xB4
#define REG_GPHY_OCP 0xB8
#define REG_DLLPR 0xD0
#define REG_DBG 0xD1
#define REG_MCU 0xD3
#define REG_RMS 0xDA
#define REG_CPLUS_COMMAND 0xE0
#define REG_INT_MOD 0xE2
#define REG_RXADDR 0xE4
#define REG_MTPS 0xEC
#define REG_MISC 0xF0
#define REG_MISC2 0xF2
#define REG_IBCR0 0xF8
#define REG_IBCR2 0xF9
#define REG_IBISR0 0xFB

#define COMMAND_TX_ENABLE 0x4
#define COMMAND_RX_ENABLE 0x8
#define COMMAND_RESET 0x10
#define COMMAND_STOP 0x80

#define CPLUS_COMMAND_VERIFY_CHECKSUM 0x20
#define CPLUS_COMMAND_VLAN_STRIP 0x40
#define CPLUS_COMMAND_MAC_DBGO_SEL 0x1C
#define CPLUS_COMMAND_PACKET_CONTROL_DISABLE 0x80
#define CPLUS_COMMAND_ASF 0x100
#define CPLUS_COMMAND_CXPL_DBG_SEL 0x200
#define CPLUS_COMMAND_FORCE_TXFLOW_ENABLE 0x400
#define CPLUS_COMMAND_FORCE_RXFLOW_ENABLE 0x800
#define CPLUS_COMMAND_FORCE_HALF_DUP 0x1000
#define CPLUS_COMMAND_MAC_DBGO_OE 0x4000
#define CPLUS_COMMAND_ENABLE_BIST 0x8000

#define INT_RXOK 0x1
#define INT_RXERR 0x2
#define INT_TXOK 0x4
#define INT_TXERR 0x8
#define INT_RX_OVERFLOW 0x10
#define INT_LINK_CHANGE 0x20
#define INT_RX_FIFO_OVERFLOW 0x40
#define INT_SYS_ERR 0x8000

#define CFG9346_NONE 0x00
#define CFG9346_EEM0 0x40
#define CFG9346_EEM1 0x80
#define CFG9346_UNLOCK (CFG9346_EEM0 | CFG9346_EEM1)

#define TXCFG_AUTO_FIFO 0x80
#define TXCFG_MAX_DMA_UNLIMITED 0x700
#define TXCFG_EMPTY 0x800
#define TXCFG_IFG011 0x3000000

#define RXCFG_READ_MASK 0x3F

#define RXCFG_APM 0x2
#define RXCFG_AM 0x4
#define RXCFG_AB 0x8
#define RXCFG_MAX_DMA_UNLIMITED 0x700
#define RXCFG_EARLY_OFF_V2 0x800
#define RXCFG_FTH_NONE 0xE000
#define RXCFG_MULTI_ENABLE 0x4000
#define RXCFG_128INT_ENABLE 0x8000

#define CFG1_SPEED_DOWN 0x10

#define CFG2_CLOCK_REQUEST_ENABLE 0x80

#define CFG3_BEACON_ENABLE 0x1
#define CFG3_READY_TO_L23 0x2

#define CFG5_ASPM_ENABLE 0x1
#define CFG5_SPI_ENABLE 0x8

#define PHY_LINK_STATUS 0x2

#define PHY_FLAG 0x80000000
#define PHY_REG_BMCR 0x00
#define PHY_REG_ANAR 0x4
#define PHY_REG_GBCR 0x9

#define CSI_FLAG 0x80000000
#define CSI_BYTE_ENABLE 0xF000
#define CSI_FUNC_NIC 0x20000
#define CSI_FUNC_NIC2 0x10000

#define CSI_ACCESS_1 0x17000000
#define CSI_ACCESS_2 0x27000000

#define EPHY_FLAG 0x80000000

#define ERI_FLAG 0x80000000
#define ERI_MASK_0001 0x1000
#define ERI_MASK_0011 0x3000
#define ERI_MASK_0100 0x4000
#define ERI_MASK_0101 0x5000
#define ERI_MASK_1111 0xF000

#define ERI_EXGMAC 0x0

#define OCP_FLAG 0x80000000
#define OCP_STANDARD_PHY_BASE 0xa400

#define TXSTART_START 0x40

#define BMCR_RESET 0x8000
#define BMCR_SPEED_0 0x2000
#define BMCR_AUTO_NEGOTIATE 0x1000
#define BMCR_RESTART_AUTO_NEGOTIATE 0x200
#define BMCR_DUPLEX 0x100
#define BMCR_SPEED_1 0x40

#define ADVERTISE_10_HALF 0x20
#define ADVERTISE_10_FULL 0x40
#define ADVERTISE_100_HALF 0x80
#define ADVERTISE_100_FULL 0x100
#define ADVERTISE_PAUSE_CAP 0x400
#define ADVERTISE_PAUSE_ASYM 0x800

#define ADVERTISE_1000_HALF 0x100
#define ADVERTISE_1000_FULL 0x200

#define DLLPR_PFM_ENABLE 0x40
#define DLLPR_TX_10M_PS_ENABLE 0x80

#define MCU_LINK_LIST_READY 0x2
#define MCU_RX_EMPTY 0x10
#define MCU_TX_EMPTY 0x20
#define MCU_NOW_IS_OOB 0x80

#define MTPS_JUMBO 0x3F

#define MISC_RXDV_GATE_ENABLE 0x80000
#define MISC_PWM_ENABLE 0x400000

#define MISC2_PFM_D3COLD_ENABLE 0x40

#define PHYSTATUS_FULLDUP 0x01
#define PHYSTATUS_1000MF 0x10
#define PHYSTATUS_100M 0x08
#define PHYSTATUS_10M 0x04

#define GPIO_ENABLE 0x1

#define DBG_FIX_NAK_2 0x8
#define DBG_FIX_NAK_1 0x10

#define TX_BUFFER_SIZE 0x1FF8
#define RX_BUFFER_SIZE 0x1FF8 // FIXME: this should be increased (0x3FFF)

UNMAP_AFTER_INIT ErrorOr<bool> RTL8168NetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Realtek)
        return false;

    switch (pci_device_identifier.hardware_id().device_id) {
    case 0x8168:
    case 0x8125:
    case 0x8126:
    case 0x8127:
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> RTL8168NetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    InterruptNumber irq = pci_device_identifier.interrupt_line().value();
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR2));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RTL8168NetworkAdapter(interface_name.representable_view(), pci_device_identifier, irq, move(registers_io_window))));
}

bool RTL8168NetworkAdapter::determine_supported_version() const
{
    switch (m_version) {
    case ChipVersion::RTL8168B:
    case ChipVersion::RTL8168C_1:
    case ChipVersion::RTL8168C_2:
    case ChipVersion::RTL8168C_3:
    case ChipVersion::RTL8168C_4:
        return true;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        return false;
    case ChipVersion::RTL8168E:
    case ChipVersion::RTL8168EVL:
        return true;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
    case ChipVersion::RTL8411:
    case ChipVersion::RTL8168G:
    case ChipVersion::RTL8168GU:
    case ChipVersion::RTL8411B:
        return false;
    case ChipVersion::RTL8168H:
        return true;
    case ChipVersion::RTL8168EP:
    case ChipVersion::RTL8168FP:
    case ChipVersion::RTL8125A:
    case ChipVersion::RTL8125B:
    case ChipVersion::RTL8125D:
    case ChipVersion::RTL8125BP:
    case ChipVersion::RTL8126A:
    case ChipVersion::RTL8127A:
    case ChipVersion::Unknown:
        return false;
    }

    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT RTL8168NetworkAdapter::RTL8168NetworkAdapter(StringView interface_name, PCI::DeviceIdentifier const& device_identifier, InterruptNumber irq, NonnullOwnPtr<IOWindow> registers_io_window)
    : NetworkAdapter(interface_name)
    , PCI::Device(device_identifier)
    , IRQHandler(irq)
    , m_registers_io_window(move(registers_io_window))
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    , m_rx_descriptors(Memory::allocate_dma_region_as_typed_array<RXDescriptor volatile>(number_of_rx_descriptors + 1, "RTL8168 RX"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value_but_fixme_should_propagate_errors())
    , m_tx_descriptors(Memory::allocate_dma_region_as_typed_array<TXDescriptor volatile>(number_of_tx_descriptors + 1, "RTL8168 TX"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value_but_fixme_should_propagate_errors())
{
    dmesgln_pci(*this, "Found @ {}", device_identifier.address());
    dmesgln_pci(*this, "I/O port base: {}", m_registers_io_window);
}

UNMAP_AFTER_INIT ErrorOr<void> RTL8168NetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    m_version = TRY(identify_chip_version());
    dmesgln_pci(*this, "Version detected - {} ({})", possible_device_name(), (u8)m_version);

    if (!determine_supported_version()) {
        dmesgln_pci(*this, "Aborting initialization! Support for your chip version ({}) is not implemented yet, please open a GH issue and include this message.", (u8)m_version);
        return Error::from_errno(ENODEV); // Each ChipVersion requires a specific implementation of configure_phy and hardware_quirks
    }
    // set initial REG_RXCFG
    auto rx_config = RXCFG_MAX_DMA_UNLIMITED;
    if (m_version <= ChipVersion::RTL8168B) {
        rx_config |= RXCFG_FTH_NONE;
    } else if (m_version <= ChipVersion::RTL8168CP_2 || (m_version >= ChipVersion::RTL8168EVL && m_version <= ChipVersion::RTL8168F_2)) {
        rx_config |= RXCFG_128INT_ENABLE | RXCFG_MULTI_ENABLE;
    } else if (m_version >= ChipVersion::RTL8168G) {
        rx_config |= RXCFG_128INT_ENABLE | RXCFG_MULTI_ENABLE | RXCFG_EARLY_OFF_V2;
    } else {
        rx_config |= RXCFG_128INT_ENABLE;
    }
    out32(REG_RXCFG, rx_config);

    // disable interrupts
    set_interrupt_mask(0);

    // initialize hardware
    if (m_version == ChipVersion::RTL8168EP) {
        // disable CMAC
        out8(REG_IBCR2, in8(REG_IBCR2) & ~1);

        while ((in8(REG_IBISR0) & 0x2) != 0)
            Processor::wait_check();

        out8(REG_IBISR0, in8(REG_IBISR0) | 0x20);
        out8(REG_IBCR0, in8(REG_IBCR0) & ~1);
    }
    if (m_version >= ChipVersion::RTL8168G) {
        m_ocp_base_address = OCP_STANDARD_PHY_BASE;

        // enable RXDV gate
        out32(REG_MISC, in32(REG_MISC) | MISC_RXDV_GATE_ENABLE);

        while ((in32(REG_TXCFG) & TXCFG_EMPTY) == 0)
            Processor::wait_check();

        while ((in8(REG_MCU) & (MCU_RX_EMPTY | MCU_TX_EMPTY)) == 0)
            Processor::wait_check();

        out8(REG_COMMAND, in8(REG_COMMAND) & ~(COMMAND_RX_ENABLE | COMMAND_TX_ENABLE));
        out8(REG_MCU, in8(REG_MCU) & ~MCU_NOW_IS_OOB);

        // vendor magic values ???
        auto data = ocp_in(0xe8de);
        data &= ~(1 << 14);
        ocp_out(0xe8de, data);

        while ((in8(REG_MCU) & MCU_LINK_LIST_READY) == 0)
            Processor::wait_check();

        // vendor magic values ???
        data = ocp_in(0xe8de);
        data |= (1 << 15);
        ocp_out(0xe8de, data);

        while ((in8(REG_MCU) & MCU_LINK_LIST_READY) == 0)
            Processor::wait_check();
    }

    // clear interrupts
    set_interrupt_status(0xFFFF);

    pci_commit();

    // software reset
    reset();

    PCI::enable_bus_mastering(device_identifier());

    read_mac_address();
    dmesgln_pci(*this, "MAC address: {}", mac_address().to_string());

    // notify about driver start
    if (m_version >= ChipVersion::RTL8168DP_1 && m_version <= ChipVersion::RTL8168DP_2) {
        // if check_dash
        //  notify
        TODO();
    } else if (m_version == ChipVersion::RTL8168EP) {
        // if check_dash
        //  notify
        TODO();
    }

    startup();
    return {};
}

void RTL8168NetworkAdapter::startup()
{
    // initialize descriptors
    initialize_rx_descriptors();
    initialize_tx_descriptors();

    // register irq
    enable_irq();

    // version specific phy configuration
    configure_phy();
    pci_commit();

    // disable interrupts
    set_interrupt_mask(0);
    set_interrupt_status(0xFFFF);
    pci_commit();

    // send stop command
    out8(REG_COMMAND, COMMAND_STOP);

    reset();

    // software reset phy
    phy_out(PHY_REG_BMCR, phy_in(PHY_REG_BMCR) | BMCR_RESET);
    while ((phy_in(PHY_REG_BMCR) & BMCR_RESET) != 0)
        Processor::wait_check();

    set_phy_speed();

    // set C+ command
    auto cplus_command = in16(REG_CPLUS_COMMAND) | CPLUS_COMMAND_VERIFY_CHECKSUM | CPLUS_COMMAND_VLAN_STRIP;
    out16(REG_CPLUS_COMMAND, cplus_command);
    in16(REG_CPLUS_COMMAND); // C+ Command barrier

    if (m_version == ChipVersion::RTL8168C_2 || m_version == ChipVersion::RTL8168C_3) {
        if (in8(REG_MACDBG) & 0x80)
            out8(REG_GPIO, in8(REG_GPIO) | GPIO_ENABLE);
        else
            out8(REG_GPIO, in8(REG_GPIO) & ~GPIO_ENABLE);
    }

    // power up phy
    if (m_version >= ChipVersion::RTL8168D_1 && m_version <= ChipVersion::RTL8168E) {
        out8(REG_PMCH, in8(REG_PMCH) | 0x80);
    } else if (m_version >= ChipVersion::RTL8168GU) {
        out8(REG_PMCH, in8(REG_PMCH) | 0xC0);
    } else if (m_version == ChipVersion::RTL8168G) {
        out8(REG_PMCH, in8(REG_PMCH) | 0xC0);
        // vendor magic values ???
        eri_update(0x1a8, ERI_MASK_1111, 0xfc000000, 0, ERI_EXGMAC);
    }

    // wakeup phy (more vendor magic values)
    phy_out(0x1F, 0);
    if (m_version <= ChipVersion::RTL8168DP_2) {
        phy_out(0x0E, 0);
    }
    phy_out(PHY_REG_BMCR, BMCR_AUTO_NEGOTIATE); // send known good phy write (acts as a phy barrier)
    start_hardware();
    pci_commit();

    // re-enable interrupts
    u32 enabled_interrupts = INT_RXOK | INT_RXERR | INT_TXOK | INT_TXERR | INT_RX_OVERFLOW | INT_LINK_CHANGE | INT_SYS_ERR;
    set_interrupt_mask(enabled_interrupts);
    pci_commit();

    // update link status
    m_link_up = (in8(REG_PHYSTATUS) & PHY_LINK_STATUS) != 0;
    autoconfigure_link_local_ipv6();
}

void RTL8168NetworkAdapter::configure_phy()
{
    // this method sets a bunch of magic vendor values to the phy configuration registers based on the hardware version
    switch (m_version) {
    case ChipVersion::RTL8168B:
        configure_phy_b();
        return;
    case ChipVersion::RTL8168C_1:
        configure_phy_c_1();
        return;
    case ChipVersion::RTL8168C_2:
        configure_phy_c_2();
        return;
    case ChipVersion::RTL8168C_3:
    case ChipVersion::RTL8168C_4:
        configure_phy_c_3();
        return;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        TODO();
    case ChipVersion::RTL8168E:
        // FIXME: This might not actually be correct
        configure_phy_e_2();
        return;
    case ChipVersion::RTL8168EVL:
        configure_phy_e_2();
        return;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
    case ChipVersion::RTL8411:
    case ChipVersion::RTL8168G:
    case ChipVersion::RTL8168GU:
    case ChipVersion::RTL8411B:
        TODO();
    case ChipVersion::RTL8168H: {
        configure_phy_h_2();
        return;
    }
    case ChipVersion::RTL8168EP:
    case ChipVersion::RTL8168FP:
    case ChipVersion::RTL8125A:
    case ChipVersion::RTL8125B:
    case ChipVersion::RTL8125D:
    case ChipVersion::RTL8125BP:
    case ChipVersion::RTL8126A:
    case ChipVersion::RTL8127A:
        TODO();
    case ChipVersion::Unknown:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

void RTL8168NetworkAdapter::configure_phy_b()
{
    static constexpr auto phy_registers = to_array<PhyRegister>({
        { 0x1f, 0x1 },
        { 0x10, 0xf41b },
        { 0x1f, 0 },
    });

    phy_out_batch(phy_registers);
}

void RTL8168NetworkAdapter::configure_phy_c_1()
{
    static constexpr auto phy_registers = to_array<PhyRegister>({
        { 0x1f, 0x0001 },
        { 0x12, 0x2300 },
        { 0x1f, 0x0002 },
        { 0x00, 0x88d4 },
        { 0x01, 0x82b1 },
        { 0x03, 0x7002 },
        { 0x08, 0x9e30 },
        { 0x09, 0x01f0 },
        { 0x0a, 0x5500 },
        { 0x0c, 0x00c8 },
        { 0x1f, 0x0003 },
        { 0x12, 0xc096 },
        { 0x16, 0x000a },
        { 0x1f, 0x0000 },
        { 0x1f, 0x0000 },
        { 0x09, 0x2000 },
        { 0x09, 0x0000 },
    });
    phy_out_batch(phy_registers);

    phy_update(0x14, 1 << 5, 0);
    phy_update(0x0d, 1 << 5, 0);
}

void RTL8168NetworkAdapter::configure_phy_c_2()
{
    static constexpr auto phy_registers = to_array<PhyRegister>({
        { 0x1f, 0x0001 },
        { 0x12, 0x2300 },
        { 0x03, 0x802f },
        { 0x02, 0x4f02 },
        { 0x01, 0x0409 },
        { 0x00, 0xf099 },
        { 0x04, 0x9800 },
        { 0x04, 0x9000 },
        { 0x1d, 0x3d98 },
        { 0x1f, 0x0002 },
        { 0x0c, 0x7eb8 },
        { 0x06, 0x0761 },
        { 0x1f, 0x0003 },
        { 0x16, 0x0f0a },
        { 0x1f, 0x0000 },
    });
    phy_out_batch(phy_registers);

    phy_update(0x16, 0x1, 0);
    phy_update(0x14, 1 << 5, 0);
    phy_update(0x0d, 1 << 5, 0);
}

void RTL8168NetworkAdapter::configure_phy_c_3()
{
    static constexpr auto phy_registers = to_array<PhyRegister>({
        { 0x1f, 0x0001 },
        { 0x12, 0x2300 },
        { 0x1d, 0x3d98 },
        { 0x1f, 0x0002 },
        { 0x0c, 0x7eb8 },
        { 0x06, 0x5461 },
        { 0x1f, 0x0003 },
        { 0x16, 0x0f0a },
        { 0x1f, 0x0000 },
    });
    phy_out_batch(phy_registers);

    phy_update(0x16, 0x1, 0);
    phy_update(0x14, 1 << 5, 0);
    phy_update(0x0d, 1 << 5, 0);
}

void RTL8168NetworkAdapter::configure_phy_e_2()
{
    // FIXME: linux's driver writes a firmware blob to the device at this point, is this needed?

    static constexpr auto phy_registers = to_array<PhyRegister>({
        // Enable delay cap
        { 0x1f, 0x4 },
        { 0x1f, 0x7 },
        { 0x1e, 0xac },
        { 0x18, 0x6 },
        { 0x1f, 0x2 },
        { 0x1f, 0 },
        { 0x1f, 0 },

        // Channel estimation fine tune
        { 0x1f, 0x3 },
        { 0x9, 0xa20f },
        { 0x1f, 0 },
        { 0x1f, 0 },

        // Green Setting
        { 0x1f, 0x5 },
        { 0x5, 0x8b5b },
        { 0x6, 0x9222 },
        { 0x5, 0x8b6d },
        { 0x6, 0x8000 },
        { 0x5, 0x8b76 },
        { 0x6, 0x8000 },
        { 0x1f, 0 },
    });

    phy_out_batch(phy_registers);

    // 4 corner performance improvement
    phy_out(0x1f, 0x5);
    phy_out(0x5, 0x8b80);
    phy_update(0x17, 0x6, 0);
    phy_out(0x1f, 0);

    // PHY auto speed down
    phy_out(0x1f, 0x4);
    phy_out(0x1f, 0x7);
    phy_out(0x1e, 0x2d);
    phy_update(0x18, 0x10, 0);
    phy_out(0x1f, 0x2);
    phy_out(0x1f, 0);
    phy_update(0x14, 0x8000, 0);

    // Improve 10M EEE waveform
    phy_out(0x1f, 0x5);
    phy_out(0x5, 0x8b86);
    phy_update(0x6, 0x1, 0);
    phy_out(0x1f, 0);

    // Improve 2-pair detection performance
    phy_out(0x1f, 0x5);
    phy_out(0x5, 0x8b85);
    phy_update(0x6, 0x4000, 0);
    phy_out(0x1f, 0);

    // EEE Setting
    eri_update(0x1b0, ERI_MASK_1111, 0, 0x3, ERI_EXGMAC);
    phy_out(0x1f, 0x5);
    phy_out(0x5, 0x8b85);
    phy_update(0x6, 0, 0x2000);
    phy_out(0x1f, 0x4);
    phy_out(0x1f, 0x7);
    phy_out(0x1e, 0x20);
    phy_update(0x15, 0, 0x100);
    phy_out(0x1f, 0x2);
    phy_out(0x1f, 0);
    phy_out(0xd, 0x7);
    phy_out(0xe, 0x3c);
    phy_out(0xd, 0x4007);
    phy_out(0xe, 0);
    phy_out(0xd, 0);

    // Green feature
    phy_out(0x1f, 0x3);
    phy_update(0x19, 0, 0x1);
    phy_update(0x10, 0, 0x400);
    phy_out(0x1f, 0);

    // Broken BIOS workaround: feed GigaMAC registers with MAC address.
    rar_exgmac_set();
}

void RTL8168NetworkAdapter::configure_phy_h_2()
{
    // FIXME: linux's driver writes a firmware blob to the device at this point, is this needed?

    // CHIN EST parameter update
    phy_out(0x1f, 0x0a43);
    phy_out(0x13, 0x808a);
    phy_update(0x14, 0x000a, 0x3f);
    phy_out(0x1f, 0);

    // enable R-tune & PGA-retune function
    phy_out(0x1f, 0x0a43);
    phy_out(0x13, 0x811);
    phy_update(0x14, 0x800, 0);
    phy_out(0x1f, 0x0a42);
    phy_update(0x16, 0x2, 0);
    phy_out(0x1f, 0);

    // enable GPHY 10M
    phy_out(0x1f, 0x0a44);
    phy_update(0x11, 0x800, 0);
    phy_out(0x1f, 0);

    ocp_out(0xdd02, 0x807d);
    auto data = ocp_in(0xdd02);
    u16 ioffset_p3 = ((data & 0x80) >> 7);
    ioffset_p3 <<= 3;

    data = ocp_in(0xdd00);
    ioffset_p3 |= ((data & (0xe000)) >> 13);
    u16 ioffset_p2 = ((data & (0x1e00)) >> 9);
    u16 ioffset_p1 = ((data & (0x1e0)) >> 5);
    u16 ioffset_p0 = ((data & 0x10) >> 4);
    ioffset_p0 <<= 3;
    ioffset_p0 |= (data & (0x7));
    data = (ioffset_p3 << 12) | (ioffset_p2 << 8) | (ioffset_p1 << 4) | (ioffset_p0);

    if ((ioffset_p3 != 0x0f) || (ioffset_p2 != 0x0f) || (ioffset_p1 != 0x0f) || (ioffset_p0 != 0x0f)) {
        phy_out(0x1f, 0x0bcf);
        phy_out(0x16, data);
        phy_out(0x1f, 0);
    }

    // Modify rlen (TX LPF corner frequency) level
    phy_out(0x1f, 0x0bcd);
    data = phy_in(0x16);
    data &= 0x000f;
    u16 rlen = 0;
    if (data > 3)
        rlen = data - 3;
    data = rlen | (rlen << 4) | (rlen << 8) | (rlen << 12);
    phy_out(0x17, data);
    phy_out(0x1f, 0x0bcd);
    phy_out(0x1f, 0);

    // disable phy pfm mode
    phy_out(0x1f, 0x0a44);
    phy_update(0x11, 0, 0x80);
    phy_out(0x1f, 0);

    // Check ALDPS bit, disable it if enabled
    phy_out(0x1f, 0x0a43);
    if (phy_in(0x10) & 0x4)
        phy_update(0x10, 0, 0x4);

    phy_out(0x1f, 0);
}

void RTL8168NetworkAdapter::rar_exgmac_set()
{
    auto mac = mac_address();

    auto const w = to_array<u16>({
        (u16)(mac[0] | (mac[1] << 8)),
        (u16)(mac[2] | (mac[3] << 8)),
        (u16)(mac[4] | (mac[5] << 8)),
    });

    auto const exg_mac_registers = to_array<ExgMacRegister>({
        { 0xe0, ERI_MASK_1111, (u32)(w[0] | (w[1] << 16)) },
        { 0xe4, ERI_MASK_1111, (u32)w[2] },
        { 0xf0, ERI_MASK_1111, (u32)(w[0] << 16) },
        { 0xf4, ERI_MASK_1111, (u32)(w[1] | (w[2] << 16)) },
    });

    exgmac_out_batch(exg_mac_registers);
}

void RTL8168NetworkAdapter::start_hardware()
{
    // unlock config registers
    out8(REG_CFG9346, CFG9346_UNLOCK);

    // configure the maximum transmit packet size
    out16(REG_MTPS, MTPS_JUMBO);

    // configure the maximum receive packet size
    out16(REG_RMS, RX_BUFFER_SIZE);

    auto cplus_command = in16(REG_CPLUS_COMMAND);
    cplus_command |= CPLUS_COMMAND_PACKET_CONTROL_DISABLE;
    // undocumented magic value???
    cplus_command |= 0x1;
    out16(REG_CPLUS_COMMAND, cplus_command);

    // setup interrupt moderation, magic from vendor (Linux Driver uses 0x5151, *BSD Driver uses 0x5100, RTL Driver use 0x5f51???)
    out16(REG_INT_MOD, 0x5151);

    // point to tx descriptors
    out64(REG_TXADDR, m_tx_descriptors.paddr.get());

    // point to rx descriptors
    out64(REG_RXADDR, m_rx_descriptors.paddr.get());

    // configure tx: use the maximum dma transfer size, default interframe gap time.
    out32(REG_TXCFG, TXCFG_IFG011 | TXCFG_MAX_DMA_UNLIMITED);

    // version specific quirks and tweaks
    hardware_quirks();

    // known good read (acts as a barrier)
    in32(REG_TXCFG);

    // relock config registers
    out8(REG_CFG9346, CFG9346_NONE);

    // enable rx/tx
    out8(REG_COMMAND, COMMAND_RX_ENABLE | COMMAND_TX_ENABLE);
    pci_commit();

    // turn on all multicast
    out32(REG_MAR0, 0xFFFFFFFF);
    out32(REG_MAR4, 0xFFFFFFFF);

    // configure rx mode: accept physical (MAC) match, multicast, and broadcast
    out32(REG_RXCFG, (in32(REG_RXCFG) & ~RXCFG_READ_MASK) | RXCFG_APM | RXCFG_AM | RXCFG_AB);

    // disable early-rx interrupts
    out16(REG_MULTIINTR, in16(REG_MULTIINTR) & 0xF000);
}

void RTL8168NetworkAdapter::hardware_quirks()
{
    switch (m_version) {
    case ChipVersion::RTL8168B:
        hardware_quirks_b();
        return;
    case ChipVersion::RTL8168C_1:
        hardware_quirks_c_1();
        return;
    case ChipVersion::RTL8168C_2:
    case ChipVersion::RTL8168C_3:
        hardware_quirks_c_2();
        return;
    case ChipVersion::RTL8168C_4:
        hardware_quirks_c_3();
        return;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        TODO();
    case ChipVersion::RTL8168E:
        // TODO: Implement the actual sequence
        return;
    case ChipVersion::RTL8168EVL:
        hardware_quirks_e_2();
        return;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
    case ChipVersion::RTL8411:
    case ChipVersion::RTL8168G:
    case ChipVersion::RTL8168GU:
    case ChipVersion::RTL8411B:
        TODO();
    case ChipVersion::RTL8168H:
        hardware_quirks_h();
        return;
    case ChipVersion::RTL8168EP:
    case ChipVersion::RTL8168FP:
    case ChipVersion::RTL8125A:
    case ChipVersion::RTL8125B:
    case ChipVersion::RTL8125D:
    case ChipVersion::RTL8125BP:
    case ChipVersion::RTL8126A:
    case ChipVersion::RTL8127A:
        TODO();
    case ChipVersion::Unknown:
        VERIFY_NOT_REACHED();
    }
}

void RTL8168NetworkAdapter::hardware_quirks_b()
{
    // disable checked reserved bits
    out8(REG_CONFIG3, in8(REG_CONFIG3) & ~CFG3_BEACON_ENABLE);
    constexpr u16 version1_cplus_quirks = CPLUS_COMMAND_ENABLE_BIST | CPLUS_COMMAND_MAC_DBGO_OE | CPLUS_COMMAND_FORCE_HALF_DUP | CPLUS_COMMAND_FORCE_RXFLOW_ENABLE | CPLUS_COMMAND_FORCE_TXFLOW_ENABLE | CPLUS_COMMAND_CXPL_DBG_SEL | CPLUS_COMMAND_ASF | CPLUS_COMMAND_PACKET_CONTROL_DISABLE | CPLUS_COMMAND_MAC_DBGO_SEL;
    out16(REG_CPLUS_COMMAND, in16(REG_CPLUS_COMMAND) & ~version1_cplus_quirks);

    // configure the maximum transmit packet size (again)
    out16(REG_MTPS, MTPS_JUMBO);

    // disable checked reserved bits
    out8(REG_CONFIG4, in8(REG_CONFIG4) & ~1);
}

void RTL8168NetworkAdapter::hardware_quirks_c_1()
{
    csi_enable(CSI_ACCESS_2);

    out8(REG_DBG, 0x06 | DBG_FIX_NAK_1 | DBG_FIX_NAK_2);

    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x02, 0x0800, 0x1000 },
        { 0x03, 0, 0x0002 },
        { 0x06, 0x0080, 0x0000 },
    });
    extended_phy_initialize(ephy_info);

    out8(REG_CONFIG1, in8(REG_CONFIG1) | CFG1_SPEED_DOWN);
    out8(REG_CONFIG3, in8(REG_CONFIG3) & ~CFG3_BEACON_ENABLE);
}

void RTL8168NetworkAdapter::hardware_quirks_c_2()
{
    csi_enable(CSI_ACCESS_2);

    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x01, 0, 0x1 },
        { 0x03, 0x0400, 0x0020 },
    });
    extended_phy_initialize(ephy_info);

    out8(REG_CONFIG1, in8(REG_CONFIG1) | CFG1_SPEED_DOWN);
    out8(REG_CONFIG3, in8(REG_CONFIG3) | CFG3_BEACON_ENABLE);

    // FIXME: Disable PCIe clock request
}

void RTL8168NetworkAdapter::hardware_quirks_c_3()
{
    csi_enable(CSI_ACCESS_2);

    out8(REG_CONFIG1, in8(REG_CONFIG1) | CFG1_SPEED_DOWN);
    out8(REG_CONFIG3, in8(REG_CONFIG3) & ~CFG3_BEACON_ENABLE);

    // FIXME: Disable PCIe clock request
}

void RTL8168NetworkAdapter::hardware_quirks_e_2()
{
    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x9, 0, 0x80 },
        { 0x19, 0, 0x224 },
    });

    csi_enable(CSI_ACCESS_1);

    extended_phy_initialize(ephy_info);

    // FIXME: MTU performance tweak

    eri_out(0xc0, ERI_MASK_0011, 0, ERI_EXGMAC);
    eri_out(0xb8, ERI_MASK_0011, 0, ERI_EXGMAC);
    eri_out(0xc8, ERI_MASK_1111, 0x100002, ERI_EXGMAC);
    eri_out(0xe8, ERI_MASK_1111, 0x100006, ERI_EXGMAC);
    eri_out(0xcc, ERI_MASK_1111, 0x50, ERI_EXGMAC);
    eri_out(0xd0, ERI_MASK_1111, 0x7ff0060, ERI_EXGMAC);
    eri_update(0x1b0, ERI_MASK_0001, 0x10, 0, ERI_EXGMAC);
    eri_update(0xd4, ERI_MASK_0011, 0xc00, 0xff00, ERI_EXGMAC);

    // Set early TX
    out8(REG_MTPS, 0x27);

    // FIXME: Disable PCIe clock request

    // enable tx auto fifo
    out32(REG_TXCFG, in32(REG_TXCFG) | TXCFG_AUTO_FIFO);

    out8(REG_MCU, in8(REG_MCU) & ~MCU_NOW_IS_OOB);

    // Set EEE LED frequency
    out8(REG_EEE_LED, in8(REG_EEE_LED) & ~0x7);

    out8(REG_DLLPR, in8(REG_DLLPR) | DLLPR_PFM_ENABLE);
    out32(REG_MISC, in32(REG_MISC) | MISC_PWM_ENABLE);
    out8(REG_CONFIG5, in8(REG_CONFIG5) & ~CFG5_SPI_ENABLE);
}

void RTL8168NetworkAdapter::hardware_quirks_h()
{
    // disable aspm and clock request before accessing extended phy
    out8(REG_CONFIG2, in8(REG_CONFIG2) & ~CFG2_CLOCK_REQUEST_ENABLE);
    out8(REG_CONFIG5, in8(REG_CONFIG5) & ~CFG5_ASPM_ENABLE);

    // initialize extended phy
    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x1e, 0x800, 0x1 },
        { 0x1d, 0, 0x800 },
        { 0x5, 0xffff, 0x2089 },
        { 0x6, 0xffff, 0x5881 },
        { 0x4, 0xffff, 0x154a },
        { 0x1, 0xffff, 0x68b },
    });
    extended_phy_initialize(ephy_info);

    // enable tx auto fifo
    out32(REG_TXCFG, in32(REG_TXCFG) | TXCFG_AUTO_FIFO);

    // vendor magic values ???
    eri_out(0xC8, ERI_MASK_0101, 0x80002, ERI_EXGMAC);
    eri_out(0xCC, ERI_MASK_0001, 0x38, ERI_EXGMAC);
    eri_out(0xD0, ERI_MASK_0001, 0x48, ERI_EXGMAC);
    eri_out(0xE8, ERI_MASK_1111, 0x100006, ERI_EXGMAC);

    csi_enable(CSI_ACCESS_1);

    // vendor magic values ???
    eri_update(0xDC, ERI_MASK_0001, 0x0, 0x1, ERI_EXGMAC);
    eri_update(0xDC, ERI_MASK_0001, 0x1, 0x0, ERI_EXGMAC);
    eri_update(0xDC, ERI_MASK_1111, 0x10, 0x0, ERI_EXGMAC);
    eri_update(0xD4, ERI_MASK_1111, 0x1F00, 0x0, ERI_EXGMAC);
    eri_out(0x5F0, ERI_MASK_0011, 0x4F87, ERI_EXGMAC);

    // disable rxdv gate
    out32(REG_MISC, in32(REG_MISC) & ~MISC_RXDV_GATE_ENABLE);

    // set early TX
    out8(REG_MTPS, 0x27);

    // vendor magic values ???
    eri_out(0xC0, ERI_MASK_0011, 0, ERI_EXGMAC);
    eri_out(0xB8, ERI_MASK_0011, 0, ERI_EXGMAC);

    // Set EEE LED frequency
    out8(REG_EEE_LED, in8(REG_EEE_LED) & ~0x7);

    out8(REG_DLLPR, in8(REG_DLLPR) & ~DLLPR_PFM_ENABLE);
    out8(REG_MISC2, in8(REG_MISC2) & ~MISC2_PFM_D3COLD_ENABLE);
    out8(REG_DLLPR, in8(REG_DLLPR) & ~DLLPR_TX_10M_PS_ENABLE);

    // vendor magic values ???
    eri_update(0x1B0, ERI_MASK_0011, 0, 0x1000, ERI_EXGMAC);

    // disable l2l3 state
    out8(REG_CONFIG3, in8(REG_CONFIG3) & ~CFG3_READY_TO_L23);

    // blackmagic code taken from linux's r8169
    phy_out(0x1F, 0x0C42);
    auto rg_saw_count = (phy_in(0x13) & 0x3FFF);
    phy_out(0x1F, 0);
    if (rg_saw_count > 0) {
        u16 sw_count_1ms_ini = 16000000 / rg_saw_count;
        sw_count_1ms_ini &= 0x0fff;
        u32 data = ocp_in(0xd412);
        data &= ~0x0fff;
        data |= sw_count_1ms_ini;
        ocp_out(0xd412, data);
    }

    u32 data = ocp_in(0xe056);
    data &= ~0xf0;
    data |= 0x70;
    ocp_out(0xe056, data);

    data = ocp_in(0xe052);
    data &= ~0x6000;
    data |= 0x8008;
    ocp_out(0xe052, data);

    data = ocp_in(0xe0d6);
    data &= ~0x1ff;
    data |= 0x17f;
    ocp_out(0xe0d6, data);

    data = ocp_in(0xd420);
    data &= ~0x0fff;
    data |= 0x47f;
    ocp_out(0xd420, data);

    ocp_out(0xe63e, 0x1);
    ocp_out(0xe63e, 0);
    ocp_out(0xc094, 0);
    ocp_out(0xc09e, 0);
}

void RTL8168NetworkAdapter::set_phy_speed()
{
    // wakeup phy
    phy_out(0x1F, 0);

    // advertise all available features to get best connection possible
    auto auto_negotiation_advertisement = phy_in(PHY_REG_ANAR);
    auto_negotiation_advertisement |= ADVERTISE_10_HALF;    // 10 mbit half duplex
    auto_negotiation_advertisement |= ADVERTISE_10_FULL;    // 10 mbit full duplex
    auto_negotiation_advertisement |= ADVERTISE_100_HALF;   // 100 mbit half duplex
    auto_negotiation_advertisement |= ADVERTISE_100_FULL;   // 100 mbit full duplex
    auto_negotiation_advertisement |= ADVERTISE_PAUSE_CAP;  // capable of pause flow control
    auto_negotiation_advertisement |= ADVERTISE_PAUSE_ASYM; // capable of asymmetric pause flow control
    phy_out(PHY_REG_ANAR, auto_negotiation_advertisement);

    auto gigabyte_control = phy_in(PHY_REG_GBCR);
    gigabyte_control |= ADVERTISE_1000_HALF; // 1000 mbit half dulpex
    gigabyte_control |= ADVERTISE_1000_FULL; // 1000 mbit full duplex
    phy_out(PHY_REG_GBCR, gigabyte_control);

    // restart auto-negotiation with set advertisements
    phy_out(PHY_REG_BMCR, BMCR_AUTO_NEGOTIATE | BMCR_RESTART_AUTO_NEGOTIATE);
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::initialize_rx_descriptors()
{
    for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = m_rx_descriptors[i];
        auto region = MM.allocate_contiguous_kernel_region(Memory::page_round_up(RX_BUFFER_SIZE).release_value_but_fixme_should_propagate_errors(), "RTL8168 RX buffer"sv, Memory::Region::Access::ReadWrite).release_value();
        memset(region->vaddr().as_ptr(), 0, region->size()); // MM already zeros out newly allocated pages, but we do it again in case that ever changes
        m_rx_buffers_regions.try_append(move(region)).release_value_but_fixme_should_propagate_errors();

        descriptor.buffer_size = RX_BUFFER_SIZE;
        descriptor.flags = RXDescriptor::Ownership; // let the NIC know it can use this descriptor
        auto physical_address = m_rx_buffers_regions[i]->physical_page(0)->paddr().get();
        descriptor.buffer_address_low = physical_address & 0xFFFFFFFF;
        descriptor.buffer_address_high = (u64)physical_address >> 32; // cast to prevent shift count >= with of type warnings in 32 bit systems
    }
    m_rx_descriptors[number_of_rx_descriptors - 1].flags = m_rx_descriptors[number_of_rx_descriptors - 1].flags | RXDescriptor::EndOfRing;
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::initialize_tx_descriptors()
{
    for (size_t i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        auto region = MM.allocate_contiguous_kernel_region(Memory::page_round_up(TX_BUFFER_SIZE).release_value_but_fixme_should_propagate_errors(), "RTL8168 TX buffer"sv, Memory::Region::Access::ReadWrite).release_value();
        memset(region->vaddr().as_ptr(), 0, region->size()); // MM already zeros out newly allocated pages, but we do it again in case that ever changes
        m_tx_buffers_regions.try_append(move(region)).release_value_but_fixme_should_propagate_errors();

        descriptor.flags = TXDescriptor::FirstSegment | TXDescriptor::LastSegment;
        auto physical_address = m_tx_buffers_regions[i]->physical_page(0)->paddr().get();
        descriptor.buffer_address_low = physical_address & 0xFFFFFFFF;
        descriptor.buffer_address_high = (u64)physical_address >> 32;
    }
    m_tx_descriptors[number_of_tx_descriptors - 1].flags = m_tx_descriptors[number_of_tx_descriptors - 1].flags | TXDescriptor::EndOfRing;
}

UNMAP_AFTER_INIT RTL8168NetworkAdapter::~RTL8168NetworkAdapter() = default;

bool RTL8168NetworkAdapter::handle_irq()
{
    bool was_handled = false;
    for (;;) {
        u32 status = read_interrupt_status();
        set_interrupt_status(status);

        m_entropy_source.add_random_event(status);

        dbgln_if(RTL8168_DEBUG, "RTL8168: handle_irq status={:#04x}", status);

        if ((status & (INT_RXOK | INT_RXERR | INT_TXOK | INT_TXERR | INT_RX_OVERFLOW | INT_LINK_CHANGE | INT_RX_FIFO_OVERFLOW | INT_SYS_ERR)) == 0)
            break;

        was_handled = true;
        if (status & INT_RXOK) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: RX ready");
            receive();
        }
        if (status & INT_RXERR) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: RX error - invalid packet");
        }
        if (status & INT_TXOK) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: TX complete");
            m_wait_queue.wake_one();
        }
        if (status & INT_TXERR) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: TX error - invalid packet");
        }
        if (status & INT_RX_OVERFLOW) {
            dmesgln_pci(*this, "RX descriptor unavailable (packet lost)");
            receive();
        }
        if (status & INT_LINK_CHANGE) {
            m_link_up = (in8(REG_PHYSTATUS) & PHY_LINK_STATUS) != 0;
            dmesgln_pci(*this, "Link status changed up={}", m_link_up);
            autoconfigure_link_local_ipv6();
        }
        if (status & INT_RX_FIFO_OVERFLOW) {
            dmesgln_pci(*this, "RX FIFO overflow");
            receive();
        }
        if (status & INT_SYS_ERR) {
            dmesgln_pci(*this, "Fatal system error");
        }
    }
    return was_handled;
}

void RTL8168NetworkAdapter::reset()
{
    out8(REG_COMMAND, COMMAND_RESET);
    while ((in8(REG_COMMAND) & COMMAND_RESET) != 0)
        Processor::wait_check();
}

void RTL8168NetworkAdapter::pci_commit()
{
    // read any register to commit previous PCI write
    in8(REG_COMMAND);
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::read_mac_address()
{
    MACAddress mac {};
    for (int i = 0; i < 6; i++)
        mac[i] = in8(REG_MAC + i);
    set_mac_address(mac);
}

void RTL8168NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(RTL8168_DEBUG, "RTL8168: send_raw length={}", payload.size());

    if (payload.size() > TX_BUFFER_SIZE) {
        dmesgln_pci(*this, "Packet was too big; discarding");
        return;
    }

    auto& free_descriptor = m_tx_descriptors[m_tx_free_index];

    if ((free_descriptor.flags & TXDescriptor::Ownership) != 0) {
        dbgln_if(RTL8168_DEBUG, "RTL8168: No free TX buffers, sleeping until one is available");
        m_wait_queue.wait_forever("RTL8168NetworkAdapter"sv);
        return send_raw(payload);
        // if we woke up a TX descriptor is guaranteed to be available, so this should never recurse more than once
        // but this can probably be done more cleanly
    }

    dbgln_if(RTL8168_DEBUG, "RTL8168: Chose descriptor {}", m_tx_free_index);
    memcpy(m_tx_buffers_regions[m_tx_free_index]->vaddr().as_ptr(), payload.data(), payload.size());

    m_tx_free_index = (m_tx_free_index + 1) % number_of_tx_descriptors;

    free_descriptor.frame_length = payload.size() & 0x3FFF;
    free_descriptor.flags = free_descriptor.flags | TXDescriptor::Ownership;

    doorbell(); // FIXME: this shouldn't be done so often, we should look into doing this using the watchdog timer
}

void RTL8168NetworkAdapter::receive()
{
    for (u16 i = 0; i < number_of_rx_descriptors; ++i) {
        auto descriptor_index = (m_rx_free_index + i) % number_of_rx_descriptors;
        auto& descriptor = m_rx_descriptors[descriptor_index];

        if ((descriptor.flags & RXDescriptor::Ownership) != 0) {
            m_rx_free_index = descriptor_index;
            break;
        }

        u16 flags = descriptor.flags;
        u16 length = descriptor.buffer_size & 0x3FFF;

        dbgln_if(RTL8168_DEBUG, "RTL8168: receive, flags={:#04x}, length={}, descriptor={}", flags, length, descriptor_index);

        if (length > RX_BUFFER_SIZE || (flags & RXDescriptor::ErrorSummary) != 0) {
            dmesgln_pci(*this, "receive got bad packet, flags={:#04x}, length={}", flags, length);
        } else if ((flags & RXDescriptor::FirstSegment) != 0 && (flags & RXDescriptor::LastSegment) == 0) {
            VERIFY_NOT_REACHED();
            // Our maximum received packet size is smaller than the descriptor buffer size, so packets should never be segmented
            // if this happens on a real NIC it might not respect that, and we will have to support packet segmentation
        } else {
            did_receive({ m_rx_buffers_regions[descriptor_index]->vaddr().as_ptr(), length });
        }

        descriptor.buffer_size = RX_BUFFER_SIZE;
        flags = RXDescriptor::Ownership;
        if (descriptor_index == number_of_rx_descriptors - 1)
            flags |= RXDescriptor::EndOfRing;
        descriptor.flags = flags; // let the NIC know it can use this descriptor again
    }
}

void RTL8168NetworkAdapter::set_interrupt_mask(u32 mask)
{
    if (m_version >= ChipVersion::RTL8125A) {
        out32(REG_IMR_8125, mask);
    } else {
        VERIFY((mask & 0xFFFF0000) == 0);
        out16(REG_IMR, mask);
    }
}

u32 RTL8168NetworkAdapter::read_interrupt_status()
{
    if (m_version >= ChipVersion::RTL8125A)
        return in32(REG_ISR_8125);

    return in16(REG_ISR);
}

void RTL8168NetworkAdapter::set_interrupt_status(u32 status)
{
    if (m_version >= ChipVersion::RTL8125A) {
        out32(REG_ISR_8125, status);
    } else {
        VERIFY((status & 0xFFFF0000) == 0);
        out16(REG_ISR, status);
    }
}

void RTL8168NetworkAdapter::doorbell()
{
    out8(REG_TXSTART, TXSTART_START);
}

void RTL8168NetworkAdapter::out8(u16 address, u8 data)
{
    m_registers_io_window->write8(address, data);
}

void RTL8168NetworkAdapter::out16(u16 address, u16 data)
{
    m_registers_io_window->write16(address, data);
}

void RTL8168NetworkAdapter::out32(u16 address, u32 data)
{
    m_registers_io_window->write32(address, data);
}

void RTL8168NetworkAdapter::out64(u16 address, u64 data)
{
    // ORDER MATTERS: Some NICs require the high part of the address to be written first
    m_registers_io_window->write32(address + 4, (u32)(data >> 32));
    m_registers_io_window->write32(address, (u32)(data & 0xFFFFFFFF));
}

u8 RTL8168NetworkAdapter::in8(u16 address)
{
    return m_registers_io_window->read8(address);
}

u16 RTL8168NetworkAdapter::in16(u16 address)
{
    return m_registers_io_window->read16(address);
}

u32 RTL8168NetworkAdapter::in32(u16 address)
{
    return m_registers_io_window->read32(address);
}

void RTL8168NetworkAdapter::phy_out(u8 address, u16 data)
{
    if (m_version == ChipVersion::RTL8168DP_1 || m_version == ChipVersion::RTL8168DP_2) {
        TODO();
    } else if (m_version >= ChipVersion::RTL8168G) {
        if (address == 0x1F) {
            m_ocp_base_address = data ? data << 4 : OCP_STANDARD_PHY_BASE;
            return;
        }

        if (m_ocp_base_address != OCP_STANDARD_PHY_BASE)
            address -= 0x10;

        ocp_phy_out(m_ocp_base_address + address * 2, data);
    } else {
        VERIFY((address & 0xE0) == 0); // register address is only 5 bit
        out32(REG_PHYACCESS, PHY_FLAG | (address & 0x1F) << 16 | (data & 0xFFFF));
        while ((in32(REG_PHYACCESS) & PHY_FLAG) != 0)
            Processor::wait_check();
    }
}

u16 RTL8168NetworkAdapter::phy_in(u8 address)
{
    if (m_version == ChipVersion::RTL8168DP_1 || m_version == ChipVersion::RTL8168DP_2) {
        TODO();
    } else if (m_version >= ChipVersion::RTL8168G) {
        if (m_ocp_base_address != OCP_STANDARD_PHY_BASE)
            address -= 0x10;

        return ocp_phy_in(m_ocp_base_address + address * 2);
    } else {
        VERIFY((address & 0xE0) == 0); // register address is only 5 bit
        out32(REG_PHYACCESS, (address & 0x1F) << 16);
        while ((in32(REG_PHYACCESS) & PHY_FLAG) == 0)
            Processor::wait_check();
        return in32(REG_PHYACCESS) & 0xFFFF;
    }
}

void RTL8168NetworkAdapter::phy_update(u32 address, u32 set, u32 clear)
{
    auto value = phy_in(address);
    phy_out(address, (value & ~clear) | set);
}

void RTL8168NetworkAdapter::phy_out_batch(ReadonlySpan<PhyRegister> phy_registers)
{
    for (auto const& phy_register : phy_registers) {
        phy_out(phy_register.address, phy_register.data);
    }
}

void RTL8168NetworkAdapter::extended_phy_out(u8 address, u16 data)
{
    VERIFY((address & 0xE0) == 0); // register address is only 5 bit
    out32(REG_EPHYACCESS, EPHY_FLAG | (address & 0x1F) << 16 | (data & 0xFFFF));
    while ((in32(REG_EPHYACCESS) & EPHY_FLAG) != 0)
        Processor::wait_check();
}

u16 RTL8168NetworkAdapter::extended_phy_in(u8 address)
{
    VERIFY((address & 0xE0) == 0); // register address is only 5 bit
    out32(REG_EPHYACCESS, (address & 0x1F) << 16);
    while ((in32(REG_EPHYACCESS) & EPHY_FLAG) == 0)
        Processor::wait_check();
    return in32(REG_EPHYACCESS) & 0xFFFF;
}

void RTL8168NetworkAdapter::extended_phy_initialize(ReadonlySpan<EPhyUpdate> ephy_info)
{
    for (auto const& info : ephy_info) {
        auto updated_value = (extended_phy_in(info.offset) & ~info.clear) | info.set;
        extended_phy_out(info.offset, updated_value);
    }
}

void RTL8168NetworkAdapter::eri_out(u32 address, u32 mask, u32 data, u32 type)
{
    out32(REG_ERI_DATA, data);
    out32(REG_ERI_ADDR, ERI_FLAG | type | mask | address);
    while ((in32(REG_ERI_ADDR) & ERI_FLAG) != 0)
        Processor::wait_check();
}

u32 RTL8168NetworkAdapter::eri_in(u32 address, u32 type)
{
    out32(REG_ERI_ADDR, type | ERI_MASK_1111 | address);
    while ((in32(REG_ERI_ADDR) & ERI_FLAG) == 0)
        Processor::wait_check();
    return in32(REG_ERI_DATA);
}

void RTL8168NetworkAdapter::eri_update(u32 address, u32 mask, u32 set, u32 clear, u32 type)
{
    auto value = eri_in(address, type);
    eri_out(address, mask, (value & ~clear) | set, type);
}

void RTL8168NetworkAdapter::exgmac_out_batch(ReadonlySpan<ExgMacRegister> exgmac_registers)
{
    for (auto const& exgmac_register : exgmac_registers) {
        eri_out(exgmac_register.address, exgmac_register.mask, exgmac_register.value, ERI_EXGMAC);
    }
}

void RTL8168NetworkAdapter::csi_out(u32 address, u32 data)
{
    VERIFY(m_version >= ChipVersion::RTL8168C_1);
    out32(REG_CSI_DATA, data);
    auto modifier = CSI_BYTE_ENABLE;
    if (m_version == ChipVersion::RTL8411) {
        modifier |= CSI_FUNC_NIC;
    } else if (m_version == ChipVersion::RTL8411B) {
        modifier |= CSI_FUNC_NIC2;
    }
    out32(REG_CSI_ADDR, CSI_FLAG | (address & 0xFFF) | modifier);
    while ((in32(REG_CSI_ADDR) & CSI_FLAG) != 0)
        Processor::wait_check();
}

u32 RTL8168NetworkAdapter::csi_in(u32 address)
{
    VERIFY(m_version >= ChipVersion::RTL8168C_1);
    auto modifier = CSI_BYTE_ENABLE;
    if (m_version == ChipVersion::RTL8411) {
        modifier |= CSI_FUNC_NIC;
    } else if (m_version == ChipVersion::RTL8411B) {
        modifier |= CSI_FUNC_NIC2;
    }
    out32(REG_CSI_ADDR, (address & 0xFFF) | modifier);
    while ((in32(REG_CSI_ADDR) & CSI_FLAG) == 0)
        Processor::wait_check();
    return in32(REG_CSI_DATA) & 0xFFFF;
}

void RTL8168NetworkAdapter::csi_enable(u32 bits)
{
    auto csi = csi_in(0x70c) & 0x00ffffff;
    csi_out(0x70c, csi | bits);
}

void RTL8168NetworkAdapter::ocp_out(u32 address, u32 data)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(REG_OCP_DATA, OCP_FLAG | address << 15 | data);
}

u32 RTL8168NetworkAdapter::ocp_in(u32 address)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(REG_OCP_DATA, address << 15);
    return in32(REG_OCP_DATA);
}

void RTL8168NetworkAdapter::ocp_phy_out(u32 address, u32 data)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(REG_GPHY_OCP, OCP_FLAG | (address << 15) | data);
    while ((in32(REG_GPHY_OCP) & OCP_FLAG) != 0)
        Processor::wait_check();
}

u16 RTL8168NetworkAdapter::ocp_phy_in(u32 address)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(REG_GPHY_OCP, address << 15);
    while ((in32(REG_GPHY_OCP) & OCP_FLAG) == 0)
        Processor::wait_check();
    return in32(REG_GPHY_OCP) & 0xFFFF;
}

ErrorOr<RTL8168NetworkAdapter::ChipVersion> RTL8168NetworkAdapter::identify_chip_version()
{
    struct VersionInfo {
        u32 hardware_version_id_0;
        Optional<u32> hardware_version_id_1;
        Optional<RTL8168NetworkAdapter::ChipVersion> version;
    };

    static constexpr auto VERSION_LIST = to_array<VersionInfo>({
        { 0x300, {}, {} },

        { 0x380, {}, ChipVersion::RTL8168B },

        { 0x3C0, 0x0, ChipVersion::RTL8168C_1 },
        { 0x3C0, 0x2, ChipVersion::RTL8168C_2 },
        { 0x3C0, 0x3, ChipVersion::RTL8168C_3 },
        { 0x3C0, {}, ChipVersion::RTL8168C_4 },

        { 0x3C8, 0x0, ChipVersion::RTL8168CP_1 },
        { 0x3C8, {}, ChipVersion::RTL8168CP_2 },

        { 0x280, 0x1, ChipVersion::RTL8168D_1 },
        { 0x280, {}, ChipVersion::RTL8168D_2 },

        { 0x288, 0x0, {} },
        { 0x288, 0x2, ChipVersion::RTL8168DP_1 },
        { 0x288, 0x3, ChipVersion::RTL8168DP_2 },

        { 0x2C0, {}, ChipVersion::RTL8168E },

        { 0x2C8, {}, ChipVersion::RTL8168EVL },

        { 0x480, 0x0, ChipVersion::RTL8168F_1 },
        { 0x480, 0x1, ChipVersion::RTL8168F_2 },

        { 0x488, {}, ChipVersion::RTL8411 },

        { 0x4C0, 0x0, ChipVersion::RTL8168G },

        { 0x500, 0x0, {} },
        { 0x500, 0x1, {} },
        { 0x500, 0x2, ChipVersion::RTL8168EP },

        { 0x508, 0x1, ChipVersion::RTL8168GU },

        { 0x5C8, 0x0, ChipVersion::RTL8411B },

        { 0x540, 0x0, {} },
        { 0x540, 0x1, ChipVersion::RTL8168H },

        { 0x548, 0x2, ChipVersion::RTL8168FP },
        { 0x548, 0x3, ChipVersion::RTL8168FP },

        { 0x608, 0x1, ChipVersion::RTL8125A },

        { 0x640, 0x1, ChipVersion::RTL8125B },

        { 0x648, 0x1, ChipVersion::RTL8126A },
        { 0x648, 0x2, ChipVersion::RTL8126A },

        { 0x680, 0x1, ChipVersion::RTL8125BP },

        { 0x688, 0x0, ChipVersion::RTL8125D },
        { 0x688, 0x1, ChipVersion::RTL8125D },
        { 0x688, 0x2, ChipVersion::RTL8125D },
        { 0x688, 0x3, ChipVersion::RTL8125D },

        { 0x6C0, 0x0, ChipVersion::RTL8168H },

        { 0x6C8, 0x1, ChipVersion::RTL8127A },

        { 0x708, 0x0, ChipVersion::RTL8125D },
    });

    auto transmit_config = in32(REG_TXCFG);
    auto hardware_version_id_0 = (transmit_config & 0x7c800000) >> 20;
    auto hardware_version_id_1 = (transmit_config & 0x700000) >> 20;

    for (auto const& version : VERSION_LIST) {
        if (version.hardware_version_id_0 != hardware_version_id_0)
            continue;

        if (version.hardware_version_id_1.has_value() && version.hardware_version_id_1.value() != hardware_version_id_1)
            continue;

        if (version.version.has_value())
            return version.version.value();

        dmesgln_pci(*this, "Unsupported chip revision");
        return ENOTSUP;
    }

    dmesgln_pci(*this, "Unable to determine device version: {:#04x} ({:#01x})", hardware_version_id_0, hardware_version_id_1);
    return ChipVersion::Unknown;
}

StringView RTL8168NetworkAdapter::possible_device_name()
{
    switch (m_version) {
    case ChipVersion::RTL8168B:
        return "RTL8168B/8111B"sv;
    case ChipVersion::RTL8168C_1:
    case ChipVersion::RTL8168C_2:
    case ChipVersion::RTL8168C_3:
    case ChipVersion::RTL8168C_4:
        return "RTL8168C/8111C"sv;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
        return "RTL8168CP/8111CP"sv;
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
        return "RTL8168D/8111D"sv;
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        return "RTL8168DP/8111DP"sv;
    case ChipVersion::RTL8168E:
        return "RTL8168E/8111E"sv;
    case ChipVersion::RTL8168EVL:
        return "RTL8168E-VL/8111E-VL"sv;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
        return "RTL8168F/8111F"sv;
    case ChipVersion::RTL8411:
        return "RTL8411"sv;
    case ChipVersion::RTL8168G:
        return "RTL8168G/8111G"sv;
    case ChipVersion::RTL8168GU:
        return "RTL8168GU/8111GU"sv;
    case ChipVersion::RTL8411B:
        return "RTL8411B"sv;
    case ChipVersion::RTL8168H:
        return "RTL8168H/8111H"sv;
    case ChipVersion::RTL8168EP:
        return "RTL8168EP/8111EP"sv;
    case ChipVersion::RTL8168FP:
        return "RTL8168FP/8117"sv;
    case ChipVersion::RTL8125A:
        return "RTL8125A"sv;
    case ChipVersion::RTL8125B:
        return "RTL8125B"sv;
    case ChipVersion::RTL8125D:
        return "RTL8125D/RTL8125K/RTL9151A/RTL8125CP"sv;
    case ChipVersion::RTL8125BP:
        return "RTL8125BP"sv;
    case ChipVersion::RTL8126A:
        return "RTL8126A"sv;
    case ChipVersion::RTL8127A:
        return "RTL8127A"sv;
    case ChipVersion::Unknown:
        return "Unknown"sv;
    }
    VERIFY_NOT_REACHED();
}

bool RTL8168NetworkAdapter::link_full_duplex()
{
    u8 phystatus = in8(REG_PHYSTATUS);
    return !!(phystatus & (PHYSTATUS_FULLDUP | PHYSTATUS_1000MF));
}

i32 RTL8168NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    u8 phystatus = in8(REG_PHYSTATUS);
    if (phystatus & PHYSTATUS_1000MF)
        return 1000;
    if (phystatus & PHYSTATUS_100M)
        return 100;
    if (phystatus & PHYSTATUS_10M)
        return 10;

    return NetworkAdapter::LINKSPEED_INVALID;
}
}
