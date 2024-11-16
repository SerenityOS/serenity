/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define REG_EECD 0x0010
#define REG_EEPROM 0x0014

// EECD Register

#define EECD_PRES 0x100

static bool is_valid_device_id(u16 device_id)
{
    // Note: All ids listed here are valid, but only the ones that are verified working are set to 'true'
    switch (device_id) {
    case 0x10D3: // 82574L
        return true;
    case 0x1000: // 82542
    case 0x0438: // DH89XXCC_SGMII
    case 0x043A: // DH89XXCC_SERDES
    case 0x043C: // DH89XXCC_BACKPLANE
    case 0x0440: // DH89XXCC_SFP
    case 0x1001: // 82543GC_FIBER
    case 0x1004: // 82543GC_COPPER
    case 0x1008: // 82544EI_COPPER
    case 0x1009: // 82544EI_FIBER
    case 0x100C: // 82544GC_COPPER
    case 0x100D: // 82544GC_LOM
    case 0x100E: // 82540EM
    case 0x100F: // 82545EM_COPPER
    case 0x1010: // 82546EB_COPPER
    case 0x1011: // 82545EM_FIBER
    case 0x1012: // 82546EB_FIBER
    case 0x1013: // 82541EI
    case 0x1014: // 82541ER_LOM
    case 0x1015: // 82540EM_LOM
    case 0x1016: // 82540EP_LOM
    case 0x1017: // 82540EP
    case 0x1018: // 82541EI_MOBILE
    case 0x1019: // 82547EI
    case 0x101A: // 82547EI_MOBILE
    case 0x101D: // 82546EB_QUAD_COPPER
    case 0x101E: // 82540EP_LP
    case 0x1026: // 82545GM_COPPER
    case 0x1027: // 82545GM_FIBER
    case 0x1028: // 82545GM_SERDES
    case 0x1049: // ICH8_IGP_M_AMT
    case 0x104A: // ICH8_IGP_AMT
    case 0x104B: // ICH8_IGP_C
    case 0x104C: // ICH8_IFE
    case 0x104D: // ICH8_IGP_M
    case 0x105E: // 82571EB_COPPER
    case 0x105F: // 82571EB_FIBER
    case 0x1060: // 82571EB_SERDES
    case 0x1075: // 82547GI
    case 0x1076: // 82541GI
    case 0x1077: // 82541GI_MOBILE
    case 0x1078: // 82541ER
    case 0x1079: // 82546GB_COPPER
    case 0x107A: // 82546GB_FIBER
    case 0x107B: // 82546GB_SERDES
    case 0x107C: // 82541GI_LF
    case 0x107D: // 82572EI_COPPER
    case 0x107E: // 82572EI_FIBER
    case 0x107F: // 82572EI_SERDES
    case 0x108A: // 82546GB_PCIE
    case 0x108B: // 82573E
    case 0x108C: // 82573E_IAMT
    case 0x1096: // 80003ES2LAN_COPPER_DPT
    case 0x1098: // 80003ES2LAN_SERDES_DPT
    case 0x1099: // 82546GB_QUAD_COPPER
    case 0x109A: // 82573L
    case 0x10A4: // 82571EB_QUAD_COPPER
    case 0x10A5: // 82571EB_QUAD_FIBER
    case 0x10A7: // 82575EB_COPPER
    case 0x10A9: // 82575EB_FIBER_SERDES
    case 0x10B5: // 82546GB_QUAD_COPPER_KSP3
    case 0x10B9: // 82572EI
    case 0x10BA: // 80003ES2LAN_COPPER_SPT
    case 0x10BB: // 80003ES2LAN_SERDES_SPT
    case 0x10BC: // 82571EB_QUAD_COPPER_LP
    case 0x10BD: // ICH9_IGP_AMT
    case 0x10BF: // ICH9_IGP_M
    case 0x10C0: // ICH9_IFE
    case 0x10C2: // ICH9_IFE_G
    case 0x10C3: // ICH9_IFE_GT
    case 0x10C4: // ICH8_IFE_GT
    case 0x10C5: // ICH8_IFE_G
    case 0x10C9: // 82576
    case 0x10CA: // 82576_VF
    case 0x10CB: // ICH9_IGP_M_V
    case 0x10CC: // ICH10_R_BM_LM
    case 0x10CD: // ICH10_R_BM_LF
    case 0x10CE: // ICH10_R_BM_V
    case 0x10D5: // 82571PT_QUAD_COPPER
    case 0x10D6: // 82575GB_QUAD_COPPER
    case 0x10D9: // 82571EB_SERDES_DUAL
    case 0x10DA: // 82571EB_SERDES_QUAD
    case 0x10DE: // ICH10_D_BM_LM
    case 0x10DF: // ICH10_D_BM_LF
    case 0x10E5: // ICH9_BM
    case 0x10E6: // 82576_FIBER
    case 0x10E7: // 82576_SERDES
    case 0x10E8: // 82576_QUAD_COPPER
    case 0x10EA: // PCH_M_HV_LM
    case 0x10EB: // PCH_M_HV_LC
    case 0x10EF: // PCH_D_HV_DM
    case 0x10F0: // PCH_D_HV_DC
    case 0x10F5: // ICH9_IGP_M_AMT
    case 0x10F6: // 82574LA
    case 0x1501: // ICH8_82567V_3
    case 0x1502: // PCH2_LV_LM
    case 0x1503: // PCH2_LV_V
    case 0x150A: // 82576_NS
    case 0x150C: // 82583V
    case 0x150D: // 82576_SERDES_QUAD
    case 0x150E: // 82580_COPPER
    case 0x150F: // 82580_FIBER
    case 0x1510: // 82580_SERDES
    case 0x1511: // 82580_SGMII
    case 0x1516: // 82580_COPPER_DUAL
    case 0x1518: // 82576_NS_SERDES
    case 0x1520: // I350_VF
    case 0x1521: // I350_COPPER
    case 0x1522: // I350_FIBER
    case 0x1523: // I350_SERDES
    case 0x1524: // I350_SGMII
    case 0x1525: // ICH10_D_BM_V
    case 0x1526: // 82576_QUAD_COPPER_ET2
    case 0x1527: // 82580_QUAD_FIBER
    case 0x152D: // 82576_VF_HV
    case 0x152F: // I350_VF_HV
    case 0x1533: // I210_COPPER
    case 0x1534: // I210_COPPER_OEM1
    case 0x1535: // I210_COPPER_IT
    case 0x1536: // I210_FIBER
    case 0x1537: // I210_SERDES
    case 0x1538: // I210_SGMII
    case 0x1539: // I211_COPPER
    case 0x153A: // PCH_LPT_I217_LM
    case 0x153B: // PCH_LPT_I217_V
    case 0x1546: // I350_DA4
    case 0x1559: // PCH_LPTLP_I218_V
    case 0x155A: // PCH_LPTLP_I218_LM
    case 0x156F: // PCH_SPT_I219_LM
    case 0x1570: // PCH_SPT_I219_V
    case 0x157B: // I210_COPPER_FLASHLESS
    case 0x157C: // I210_SERDES_FLASHLESS
    case 0x15A0: // PCH_I218_LM2
    case 0x15A1: // PCH_I218_V2
    case 0x15A2: // PCH_I218_LM3
    case 0x15A3: // PCH_I218_V3
    case 0x15B7: // PCH_SPT_I219_LM2
    case 0x15B8: // PCH_SPT_I219_V2
    case 0x15B9: // PCH_LBG_I219_LM3
    case 0x15BB: // PCH_CNP_I219_LM7
    case 0x15BC: // PCH_CNP_I219_V7
    case 0x15BD: // PCH_CNP_I219_LM6
    case 0x15BE: // PCH_CNP_I219_V6
    case 0x15D6: // PCH_SPT_I219_V5
    case 0x15D7: // PCH_SPT_I219_LM4
    case 0x15D8: // PCH_SPT_I219_V4
    case 0x15DF: // PCH_ICP_I219_LM8
    case 0x15E0: // PCH_ICP_I219_V8
    case 0x15E1: // PCH_ICP_I219_LM9
    case 0x15E2: // PCH_ICP_I219_V9
    case 0x15E3: // PCH_SPT_I219_LM5
    case 0x1F40: // I354_BACKPLANE_1GBPS
    case 0x1F41: // I354_SGMII
    case 0x1F45: // I354_BACKPLANE_2_5GBPS
    case 0x294C: // ICH9_IGP_C
        return false;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT ErrorOr<bool> E1000ENetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Intel)
        return false;
    return is_valid_device_id(pci_device_identifier.hardware_id().device_id);
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> E1000ENetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    u8 irq = pci_device_identifier.interrupt_line().value();
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));

    auto rx_buffer_region = TRY(MM.allocate_contiguous_kernel_region(rx_buffer_size * number_of_rx_descriptors, "E1000 RX buffers"sv, Memory::Region::Access::ReadWrite));
    auto tx_buffer_region = MM.allocate_contiguous_kernel_region(tx_buffer_size * number_of_tx_descriptors, "E1000 TX buffers"sv, Memory::Region::Access::ReadWrite).release_value();
    auto rx_descriptors_region = TRY(Memory::allocate_dma_region_as_typed_array<RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite));
    auto tx_descriptors_region = TRY(Memory::allocate_dma_region_as_typed_array<TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite));

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) E1000ENetworkAdapter(interface_name.representable_view(),
        pci_device_identifier,
        irq, move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors_region),
        move(tx_descriptors_region))));
}

UNMAP_AFTER_INIT ErrorOr<void> E1000ENetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    dmesgln("E1000e: Found @ {}", device_identifier().address());
    enable_bus_mastering(device_identifier());

    dmesgln("E1000e: IO base: {}", m_registers_io_window);
    dmesgln("E1000e: Interrupt line: {}", interrupt_number());
    detect_eeprom();
    dmesgln("E1000e: Has EEPROM? {}", m_has_eeprom.was_set());
    read_mac_address();
    auto const& mac = mac_address();
    dmesgln("E1000e: MAC address: {}", mac.to_string());

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    setup_interrupts();
    autoconfigure_link_local_ipv6();
    return {};
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::E1000ENetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier, u8 irq,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
    Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors)
    : E1000NetworkAdapter(interface_name, device_identifier, irq, move(registers_io_window),
          move(rx_buffer_region),
          move(tx_buffer_region),
          move(rx_descriptors),
          move(tx_descriptors))
{
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::~E1000ENetworkAdapter() = default;

UNMAP_AFTER_INIT void E1000ENetworkAdapter::detect_eeprom()
{
    // Section 13.4.3 of https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
    if (in32(REG_EECD) & EECD_PRES)
        m_has_eeprom.set();
}

UNMAP_AFTER_INIT u32 E1000ENetworkAdapter::read_eeprom(u8 address)
{
    VERIFY(m_has_eeprom.was_set());
    u16 data = 0;
    u32 tmp = 0;
    out32(REG_EEPROM, ((u32)address << 2) | 1);
    while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
        Processor::wait_check();
    data = (tmp >> 16) & 0xffff;
    return data;
}

}
