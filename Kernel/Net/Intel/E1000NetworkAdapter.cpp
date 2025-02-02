/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

// https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf Section 5.2
UNMAP_AFTER_INIT static bool is_valid_device_id(u16 device_id)
{
    // FIXME: There are probably more compatible devices out there
    // FIXME: This code is essentially copied in the operating mode detection
    switch (device_id) {
    // 8254x series
    // https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
    // Section 5.2
    case 0x100E: // 82540EM-A
    case 0x100F: // 82545EM-A (COPPER)
    case 0x1010: // 82546EB-A1 (COPPER)
    case 0x1011: // 82545EM-A (FIBER)
    case 0x1012: // 82546EB-A1 (FIBER)
    case 0x1013: // 82541EI-A0, 82541EI-B0
    case 0x1015: // 82540EM-A (LOM)
    case 0x1016: // 82540EP-A (LOM)
    case 0x1017: // 82540EP-A
    case 0x1018: // 82541EI-B0 (MOBILE)
    case 0x1019: // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
    case 0x101A: // 82547EI-B0 (MOBILE)
    case 0x101D: // 82546EB-A1 (QUAD-COPPER)
    case 0x1026: // 82545GM-B (COPPER)
    case 0x1027: // 82545GM-B (FIBER)
    case 0x1028: // 82545GM-B (SERDES)
    case 0x1076: // 82541GI-B1, 82541PI-C0
    case 0x1077: // 82541GI-B1 (MOBILE)
    case 0x1078: // 82541ER-C0
    case 0x1079: // 82546GB-B0 (COPPER)
    case 0x107A: // 82546GB-B0 (FIBER)
    case 0x107B: // 82546GB-B0 (SERDES)
    case 0x1107: // 82544EI-A4
    case 0x1112: // 82544GC-A4
        return true;
    // 63[12]xESB
    // 8256[34]EB
    // 8257[123]:
    // FIXME: Intel link
    // https://ftp.mizar.org/packages/e1000/8257x%20Developer%20Manual/Revision%201.8/OpenSDM_8257x-18.pdf)
    case 0x105E:
    case 0x1081: // (Dual Port, Gb/s, copper)
    case 0x1082: // (Dual Port, Mb/s, Fiber/SerDes)
    case 0x1083: // (Dual Port, Mb/s, 1000BASE-X Backplane)
    case 0x1096: // (Dual Port, Gb/s, copper + IO Acceleration)
    case 0x1097: // (Dual Port, Gb/s, Fiber/SerDes + IO Acceleration)
    case 0x1098: // (Dual Port, Mb/s, 1000BASE-X Backplane + IO Acceleration)
    case 0x108B:
    case 0x108C: // (Single Port, Gb / s, copper)
        return true;
    // 82574:
    case 0x10D3: // 82574L
        return true;
    // 82576:
    case 0x10C9: // Dual port copper
    case 0x10E6: // Dual port fiber
    case 0x10E7: // Dual port SerDes
        return true;
    // 82580:
    // https://cdrdv2-public.intel.com/333167/333167%20-%2082580-eb-db-gbe-controller-datasheet.pdf
    case 0x1509: // EEPROM-less
    case 0x150E: // copper
    case 0x150F: // fiber
    case 0x1510: // 1000BASE-KX/BX backplane
    case 0x1511: // SGMII
    case 0x1516: // copper dual
        return true;
    // I231
    // https://cdrdv2-public.intel.com/333017/333017%20-%20I211_Datasheet_v_3_4.pdf
    case 0x1539:
        // FIXME: This has a iNVM memory module, which we don't support yet
        //        But we can still try to use it without the iNVM
        return true;
    // I350
    // https://cdrdv2-public.intel.com/333171/ethernet-controller-i350-datasheet.pdf
    case 0x151F: // EEPROM-less
    case 0x1521: // Copper
    case 0x1522: // Fiber
    case 0x1523: // 1000BASE-KX/BX backplane
    case 0x1524: // SGMII PHY
        return true;

    // FIXME: Likely compatible devices,
    //        Disabled for now until we can classify their operating mode
    //        Some of these are chipset NICs, which only specify their compatible PHY models
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
    case 0x1014: // 82541ER_LOM
    case 0x101E: // 82540EP_LP
    case 0x1049: // ICH8_IGP_M_AMT
    case 0x104A: // ICH8_IGP_AMT
    case 0x104B: // ICH8_IGP_C
    case 0x104C: // ICH8_IFE
    case 0x104D: // ICH8_IGP_M
    case 0x105F: // 82571EB_FIBER
    case 0x1060: // 82571EB_SERDES
    case 0x1075: // 82547GI
    case 0x107C: // 82541GI_LF
    case 0x107D: // 82572EI_COPPER
    case 0x107E: // 82572EI_FIBER
    case 0x107F: // 82572EI_SERDES
    case 0x108A: // 82546GB_PCIE
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
    case 0x1518: // 82576_NS_SERDES
    case 0x1520: // I350_VF
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
    default:
        return false;
    }
}

// Heritage of controllers, I could find:
// 8255x: (https://www.intel.com/content/dam/doc/manual/8255x-10-100-mbps-ethernet-controller-software-dev-manual.pdf)
//    * 1229: 82557-B, 8255ER
//    * 1029- config over EEPROM: 82557-C, 82558, 82559
// 63[12]xESB, 8256[34]EB 8257[123] (https://ftp.mizar.org/packages/e1000/8257x%20Developer%20Manual/Revision%201.8/OpenSDM_8257x-18.pdf)
//    * 105E/1081^a (Dual Port, Gb/s, copper)
//    * 1082^a      (Dual Port, Mb/s, Fiber/SerDes)
//    * 1083^a      (Dual Port, Mb/s, 1000BASE-X Backplane)
//    * 1096^a      (Dual Port, Gb/s, copper + IO Acceleration)
//    * 1097^a      (Dual Port, Gb/s, Fiber/SerDes + IO Acceleration)
//    * 1098^a      (Dual Port, Mb/s, 1000BASE-X Backplane + IO Acceleration)
//    * 108B/108C   (Single Port, Gb/s, copper)
//    * 109A      - 82573L (???) "Not applicable to "631xESB/632xESB"
//    ^a: 631xESB/632xESB
// 82574: (http://web.archive.org/web/20191030005441/https://digitallibrary.intel.com/content/dam/ccl/public/82574l-gbe-controller-datasheet.pdf?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50SWQiOiIzMTc2OTQiLCJlbnRlcnByaXNlSWQiOiIxMzYuMjQzLjMzLjE1MCIsIkFDQ1RfTk0iOiIiLCJDTkRBX05CUiI6IiIsImlhdCI6MTU3MjM5Njg3N30.qWfHYA2b2JUaloVU4Sxyq6ltDR3cbEDd1O1IhpSROGw)
//        Intel link is dead....
// * 0x10D3: 82574/Default

// 82575 -> no direct data sheet?
// |-82576: (https://www.intel.com/content/dam/www/public/us/en/documents/datasheets/82576eb-gigabit-ethernet-controller-datasheet.pdf)
//   * 10C9: Dual port copper
//   * 10E6: Dual port fiber
//   * 10E7: Dual port SerDes
//   * 10CA: Virtual Function
//   * 10A6: Dummy device
//   |-82580:
//     * 1509 EEPROM-less
//     * 150E copper
//     * 150F fiber
//     * 1510 1000BASE-KX/BX backplane
//     * 1511 SGMII
//     * 1516 copper dual
//     * 10A6 Dummy device
//     |-I350:
//       * 151F EEPROM-less
//       * 1521 Copper
//       * 1522 Fiber
//       * 1523 1000BASE-KX/BX backplane
//       * 1524 SGMII PHY
//       * 10A6 Dummy device

UNMAP_AFTER_INIT ErrorOr<bool> E1000NetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Intel)
        return false;
    return is_valid_device_id(pci_device_identifier.hardware_id().device_id);
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> E1000NetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    u8 irq = pci_device_identifier.interrupt_line().value();
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));

    auto rx_buffer_region = TRY(MM.allocate_contiguous_kernel_region(rx_buffer_size * number_of_rx_descriptors, "E1000 RX buffers"sv, Memory::Region::Access::ReadWrite));
    auto tx_buffer_region = MM.allocate_contiguous_kernel_region(tx_buffer_size * number_of_tx_descriptors, "E1000 TX buffers"sv, Memory::Region::Access::ReadWrite).release_value();
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    // Note: There seems to be something about a No-snoop option
    auto rx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<E1000::RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    auto tx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<E1000::TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) E1000NetworkAdapter(interface_name.representable_view(),
        pci_device_identifier,
        irq, move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors),
        move(tx_descriptors))));
}

UNMAP_AFTER_INIT ErrorOr<void> E1000NetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    dmesgln_pci(*this, "Found @ {}", device_identifier().address());

    enable_bus_mastering(device_identifier());

    dmesgln_pci(*this, "IO base: {}", m_registers.window());
    dmesgln_pci(*this, "Interrupt line: {}", interrupt_number());
    detect_model_and_operating_mode();
    detect_eeprom();
    dmesgln_pci(*this, "Has EEPROM? {}", m_has_eeprom.was_set());
    read_mac_address();
    auto const& mac = mac_address();
    dmesgln_pci(*this, "MAC address: {}", mac.to_string());

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    setup_interrupts();

    m_link_up = m_registers.read<Register::Status>().link_up != 0;
    autoconfigure_link_local_ipv6();

    return {};
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::detect_model_and_operating_mode()
{
    u16 device_id = device_identifier().hardware_id().device_id;
    // 82544GC/EI
    if (first_is_one_of(device_id,
            0x1008, // 82544EI (COPPER)
            0x1009, // 82544EI (FIBER)
            0x100C, // 82544GC (COPPER)
            0x100D, // 82544GC_LOM
            0x1107, // 82544EI-A4 (COPPER)
            0x1112  // 82544GC-A4 (COPPER)
            )) {
        dmesgln_pci(*this, "E1000: Using Intel8254x legacy mode");

        m_operating_mode = OperatingMode::Intel8254x_legacy;
        return;
    }
    // 8254x \(82541xx, 82547GI/EI)
    if (first_is_one_of(device_id,
            0x100E, // 82540EM-A
            0x100F, // 82545EM-A (COPPER)
            0x1010, // 82546EB-A1 (COPPER)
            0x1011, // 82545EM-A (FIBER)
            0x1012, // 82546EB-A1 (FIBER)
            0x1015, // 82540EM-A (LOM)
            0x1016, // 82540EP-A (LOM)
            0x1017, // 82540EP-A
            0x1019, // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
            0x101A, // 82547EI-B0 (MOBILE)
            0x101D, // 82546EB-A1 (QUAD-COPPER)
            0x1026, // 82545GM-B (COPPER)
            0x1027, // 82545GM-B (FIBER)
            0x1028, // 82545GM-B (SERDES)
            0x1079, // 82546GB-B0 (COPPER)
            0x107A, // 82546GB-B0 (FIBER)
            0x107B  // 82546GB-B0 (SERDES)
            )) {

        dmesgln_pci(*this, "E1000: Using Intel8254x mode");
        m_operating_mode = OperatingMode::Intel8254x;

        return;
    }
    // 82541xx, 82547GI/EI
    // 63[12]xESB
    // 8256[34]EB
    // 8257[123]
    // 82574
    if (first_is_one_of(device_id,
            0x1013, // 82541EI
            0x1013, // 82541EI-A0, 82541EI-B0
            0x1014, // 82541ER_LOM
            0x1018, // 82541EI-B0
            0x1019, // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
            0x101A, // 82547EI-B0
            0x1076, // 82541GI
            0x1076, // 82541GI-B1, 82541PI-C0
            0x1077, // 82541GI_MOBILE
            0x1077, // 82541GI-B1
            0x1078, // 82541ER
            0x1078, // 82541ER-C0
            0x107C, // 82541GI_LF
            // 63[12]xESB
            // 8256[34]EB
            // 8257[123]
            0x105E,
            0x1081,
            0x1082,
            0x1083,
            0x1096,
            0x1097,
            0x1098,
            0x108B,
            0x108C,

            0x10D3 // 82574L
            )) {
        dmesgln_pci(*this, "E1000: Late E1000 mode");

        m_operating_mode = OperatingMode::Intel8254x_14bit_til_82574;
        return;
    }
    dmesgln_pci(*this, "E1000: Usign IGB/82576 and later mode");
    // 82576 and later
    m_operating_mode = OperatingMode::Intel82576_and_later;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_link()
{
    auto ctrl = m_registers.read<Register::Ctrl>();
    ctrl.set_link_up = 1;
    m_registers.write<Register::Ctrl>(ctrl);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_interrupts()
{
    // FIXME: Do this properly,
    //        like set the interrupt rate depending on the current utilization and link speed
    m_registers.write<Register::InterruptThrottling>(6000); // Interrupt rate of 1.536 milliseconds

    // We want: Link status change, RX timer, RX overrun
    using InterruptMask = E1000::Interrupt;
    m_registers.write<Register::InterruptMask>(
        InterruptMask::LSC | InterruptMask::RXT0 | InterruptMask::RXO);

    (void)m_registers.read<Register::InterruptCauseR>();

    enable_irq();
}

UNMAP_AFTER_INIT E1000NetworkAdapter::E1000NetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier, u8 irq,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<E1000::RxDescriptor volatile[]> rx_descriptors,
    Memory::TypedMapping<E1000::TxDescriptor volatile[]> tx_descriptors)
    : NetworkAdapter(interface_name)
    , PCI::Device(device_identifier)
    , IRQHandler(irq)
    , m_registers(move(registers_io_window))
    , m_rx_descriptors(move(rx_descriptors))
    , m_tx_descriptors(move(tx_descriptors))
    , m_rx_buffer_region(move(rx_buffer_region))
    , m_tx_buffer_region(move(tx_buffer_region))
{
}

UNMAP_AFTER_INIT E1000NetworkAdapter::~E1000NetworkAdapter() = default;

bool E1000NetworkAdapter::handle_irq()
{
    auto irq_cause = m_registers.read<Register::InterruptCauseR>();

    m_entropy_source.add_random_event(irq_cause);

    using enum E1000::Interrupt;
    using E1000::has_flag;

    if (irq_cause == None)
        return false;

    // Let's be honest and only handle the interrupts we care about
    if (!E1000::has_any_flag(irq_cause,
            LSC | RXO | RXT0))
        return false;

    if (has_flag(irq_cause, LSC)) {
        auto ctrl = m_registers.read<Register::Ctrl>();
        ctrl.set_link_up = 1;
        m_registers.write<Register::Ctrl>(ctrl);

        m_link_up = m_registers.read<Register::Status>().link_up != 0;

        autoconfigure_link_local_ipv6();
    }
    if (has_flag(irq_cause, RXO)) {
        dbgln_if(E1000_DEBUG, "E1000: RX buffer overrun");
    }
    if (has_flag(irq_cause, RXT0)) {
        // Note: "RXDW" on newer NICs, but it sounds like it has the same meaning
        receive();
    }

    m_wait_queue.wake_all();

    m_registers.write<Register::InterruptCauseR>(InterruptClear);
    return true;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::detect_eeprom()
{
    if (m_operating_mode != OperatingMode::Intel8254x_legacy) {
        // FIXME: Some models seem to lie here?
        if (m_registers.read<Register::EEPROMControl>().eeprom_present)
            m_has_eeprom.set();
        else
            dmesgln_pci(*this, "E1000: EEPROM not present");
        return;
    }

    // The 82544GC/EI models do not have an EEPROM present bit
    // So we cannot use that to determine if the EEPROM is present
    // But have to try to read from it, to see if it's there
    E1000::EEPROMRead eerd = {};
    eerd.address_8.start = 1;
    m_registers.write<Register::EEPROMRead>(eerd);
    for (int i = 0; i < 999; ++i) {
        auto data = m_registers.read<Register::EEPROMRead>();
        if (data.address_8.done) {
            m_has_eeprom.set();
            return;
        }
        Processor::wait_check();
    }

    dmesgln_pci(*this, "E1000: EEPROM failed to initialize");
}

UNMAP_AFTER_INIT u16 E1000NetworkAdapter::read_eeprom(u16 address)
{
    // FIXME: Should this just return 0 then?
    VERIFY(m_has_eeprom.was_set());
    if (first_is_one_of(m_operating_mode,
            OperatingMode::Intel8254x_14bit_til_82574,
            OperatingMode::Intel82576_and_later)) {
        E1000::EEPROMRead eerd = {};
        eerd.address_14.start = 1;
        eerd.address_14.address = address;
        m_registers.write<Register::EEPROMRead>(eerd);
        while (eerd = m_registers.read<Register::EEPROMRead>(),
            !eerd.address_14.done)
            Processor::wait_check();
        return eerd.address_14.data;
    }

    // The 8254x models only have an 8 bit address
    VERIFY(address < 0xFF);
    E1000::EEPROMRead eerd {};
    eerd.address_8.start = 1;
    eerd.address_8.address = address;
    m_registers.write<Register::EEPROMRead>(eerd);
    while (eerd = m_registers.read<Register::EEPROMRead>(), !eerd.address_8.done)
        Processor::wait_check();
    return eerd.address_8.data;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::read_mac_address()
{
    // FIXME: Support other ways of getting the mac address
    //        Like iNVM on the I211
    auto ral = m_registers.read<Register::RAL0>();
    auto rah = m_registers.read<Register::RAH0>();
    bool rah_va = rah & 0x80000000;

    rah &= 0xffff;
    MACAddress mac_ra {};
    mac_ra[0] = (ral >> 0) & 0xff;
    mac_ra[1] = (ral >> 8) & 0xff;
    mac_ra[2] = (ral >> 16) & 0xff;
    mac_ra[3] = (ral >> 24) & 0xff;
    mac_ra[4] = (rah >> 0) & 0xff;
    mac_ra[5] = (rah >> 8) & 0xff;

    dbgln("E1000: MAC address from RAL/RAH: {}, valid?={}", mac_ra.to_string(), rah_va);
    if (rah_va) {
        dmesgln_pci(*this, "E1000: Using MAC address from RAL0/RAH0");
        set_mac_address(mac_ra);
        return;
    }
    VERIFY(m_has_eeprom.was_set());

    // 5.6.1 Ethernet Address (00h-02h)
    MACAddress mac {};

    u16 tmp = read_eeprom(0);
    mac[0] = tmp & 0xff;
    mac[1] = tmp >> 8;

    tmp = read_eeprom(1);
    mac[2] = tmp & 0xff;
    mac[3] = tmp >> 8;

    tmp = read_eeprom(2);
    mac[4] = tmp & 0xff;
    mac[5] = tmp >> 8;

    dbgln("E1000: MAC address from EEPROM: {}", mac.to_string());

    dmesgln_pci(*this, "E1000: Using MAC address from EEPROM");
    // In this case, we have to fill in the RAL0/RAH0 registers
    ral = (mac[0] << 0) | (mac[1] << 8) | (mac[2] << 16) | (mac[3] << 24);
    rah = (mac[4] << 0) | (mac[5] << 8) | 0x80000000;
    m_registers.write<Register::RAL0>(ral);
    m_registers.write<Register::RAH0>(rah);

    set_mac_address(mac);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_rx_descriptors()
{
    constexpr auto rx_buffer_page_count = rx_buffer_size / PAGE_SIZE;

    if (m_operating_mode == OperatingMode::Intel82576_and_later) {
        auto srrctl = m_registers.read<Register::SRRCTL0>();
        srrctl.descriptor_type = E1000::SplitAndReplicationReceiveControl::DescriptorType::AdvancedOneBuffer;

        srrctl.bsize_packet = rx_buffer_size / 1024;
        srrctl.bsize_header = 0;

        m_registers.write<Register::SRRCTL0>(srrctl);

        for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
            auto& descriptor = m_rx_descriptors[i];
            m_rx_buffers[i] = m_rx_buffer_region->vaddr().as_ptr() + rx_buffer_size * i;
            descriptor.advanced.packet_buffer_address = m_rx_buffer_region->physical_page(rx_buffer_page_count * i)->paddr().get();
            // FIXME: Is this always allowed?
            descriptor.advanced.header_buffer_address = 0;
        }
    } else {
        for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
            auto& descriptor = m_rx_descriptors[i];
            m_rx_buffers[i] = m_rx_buffer_region->vaddr().as_ptr() + rx_buffer_size * i;
            descriptor.legacy.length = 0;
            descriptor.legacy.addr = m_rx_buffer_region->physical_page(rx_buffer_page_count * i)->paddr().get();
            descriptor.legacy.status = E1000::RxDescriptorStatus::None;
        }
    }

    m_registers.write<Register::RXDescLow>(m_rx_descriptors.paddr.get() & 0xffffffff);
    m_registers.write<Register::RXDescHigh>(m_rx_descriptors.paddr.get() >> 32);
    m_registers.write<Register::RXDescLength>(number_of_rx_descriptors * sizeof(E1000::RxDescriptor));
    m_registers.write<Register::RXDescHead>(0);
    m_registers.write<Register::RXDescTail>(number_of_rx_descriptors - 1);

    E1000::ReceiveControl rctl = m_registers.read<Register::RCtrl>();
    rctl.enable = 0;
    m_registers.write<Register::RCtrl>(rctl);

    rctl.enable = 1;
    rctl.store_bad_frames = 1;
    rctl.unicast_promiscuous_enable = 1;
    rctl.multicast_promiscuous_enable = 1;
    rctl.loopback_mode = E1000::LoopbackMode::None;
    rctl.broadcast_accept_mode = 1;
    rctl.strip_ethernet_crc = 1;

    if (m_operating_mode != OperatingMode::Intel82576_and_later) {
        rctl.buffer_size = E1000::ReceiveControl::BufferSize::Size8192;
        rctl.buffer_size_extension = 1;
        rctl.read_descriptor_minimum_threshold_size = E1000::ReceiveControl::FreeBufferThreshold::Half;
    } else {
        rctl.buffer_size = E1000::ReceiveControl::BufferSize::Size2048;
        // FIXME: Fill out the read_descriptor_minimum_threshold_size equivalent
    }

    m_registers.write<Register::RCtrl>(rctl);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_tx_descriptors()
{
    // FIXME: Newer NICs only allow 2048 bytes per legacy descriptor
    // Hence FIXME: Support Advanced descriptors
    constexpr auto tx_buffer_page_count = tx_buffer_size / PAGE_SIZE;

    for (size_t i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        m_tx_buffers[i] = m_tx_buffer_region->vaddr().as_ptr() + tx_buffer_size * i;
        descriptor.addr = m_tx_buffer_region->physical_page(tx_buffer_page_count * i)->paddr().get();
        descriptor.cmd = {};
    }

    m_registers.write<Register::TXDescLow>(m_tx_descriptors.paddr.get() & 0xffffffff);
    m_registers.write<Register::TXDescHigh>(m_tx_descriptors.paddr.get() >> 32);
    m_registers.write<Register::TXDescLength>(number_of_tx_descriptors * sizeof(E1000::TxDescriptor));
    m_registers.write<Register::TXDescHead>(0);
    m_registers.write<Register::TXDescTail>(0);

    E1000::TransmitControl tctl = m_registers.read<Register::TCtrl>();
    tctl.enable = 0;
    m_registers.write<Register::TCtrl>(tctl);

    tctl.enable = 1;
    tctl.pad_short_packets = 1;
    m_registers.write<Register::TCtrl>(tctl);

    set_tipg();
}

void E1000NetworkAdapter::set_tipg()
{
    auto device_id = device_identifier().hardware_id().device_id;
    auto tipg = E1000::TransmitInterPacketGap {};
    // 8254x 14.5 Transmit Initialization
    if (m_operating_mode == OperatingMode::Intel8254x_legacy) {
        if (device_id == 0x1009) {
            // FIBER
            tipg.ipgt = 6;
            tipg.ipgr1 = 8;
            tipg.ipgr = 6;
        } else {
            // COPPER
            tipg.ipgt = 8;
            tipg.ipgr1 = 8;
            tipg.ipgr = 6;
        }
    } else if (
        m_operating_mode == OperatingMode::Intel8254x || m_operating_mode == OperatingMode::Intel8254x_14bit_til_82574) {
        // FIXME: This is a bit of a mess
        //        Each model family seems to have different defaults, which we need to set here
        //        8254x: 10.5.6 Transmit Initialization -> 10,10,10
        //        8257[123] -> 8,8,7
        //        82574 -> 8,2,10, but the default is 8,8,6?
        if (m_operating_mode == OperatingMode::Intel8254x || first_is_one_of(device_id, 0x1013, // 82541EI
                0x1013,                                                                         // 82541EI-A0, 82541EI-B0
                0x1014,                                                                         // 82541ER_LOM
                0x1018,                                                                         // 82541EI-B0
                0x1019,                                                                         // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
                0x101A,                                                                         // 82547EI-B0
                0x1076,                                                                         // 82541GI
                0x1076,                                                                         // 82541GI-B1, 82541PI-C0
                0x1077,                                                                         // 82541GI_MOBILE
                0x1077,                                                                         // 82541GI-B1
                0x1078,                                                                         // 82541ER
                0x1078,                                                                         // 82541ER-C0
                0x107C                                                                          // 82541GI_LF
                )) {
            tipg.ipgt = 10;
            tipg.ipgr1 = 10;
            tipg.ipgr = 10;
        } else if (first_is_one_of(device_id, 0x105E,
                       0x1081,
                       0x1082,
                       0x1083,
                       0x1096,
                       0x1097,
                       0x1098,
                       0x108B,
                       0x108C)) {
            tipg.ipgt = 8;
            tipg.ipgr1 = 8;
            tipg.ipgr = 7;
        } else if (first_is_one_of(device_id, 0x10D3)) {
            tipg.ipgt = 8;
            tipg.ipgr1 = 2;
            tipg.ipgr = 10;
        } else {
            dmesgln_pci(*this, "E1000: Unknown device ID {:#04x}", device_id);
            VERIFY_NOT_REACHED();
        }
    } else {
        // 82576 and later
        // Modern NICs have propper HW/FW defaults for this, as it seems
        tipg = m_registers.read<Register::TIPG>();
    }

    m_registers.write<Register::TIPG>(tipg);
}

void E1000NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    // FIXME: Support splitting the packet into multiple descriptors
    if (m_operating_mode != OperatingMode::Intel82576_and_later) {
        VERIFY(payload.size() <= 8192);
    } else {
        // FIXME: Support Advanced descriptors
        VERIFY(payload.size() <= 2048);
    }

    disable_irq();

    size_t tx_current = m_registers.read<Register::TXDescTail>() % number_of_tx_descriptors;
    [[maybe_unused]] auto tx_head = m_registers.read<Register::TXDescHead>();

    dbgln_if(E1000_DEBUG, "E1000: Sending packet ({} bytes)", payload.size());
    dbgln_if(E1000_DEBUG, "E1000: Using tx descriptor {} (head is at {})", tx_current, tx_head);
    auto& descriptor = m_tx_descriptors[tx_current];

    void* buffer = m_tx_buffers[tx_current];
    memcpy(buffer, payload.data(), payload.size());

    descriptor.length = payload.size();
    descriptor.status = 0;
    using enum E1000::TXCommand;
    descriptor.cmd = EOP | IFCS | RS; // Single packet, insert FCS, report status
    tx_current = (tx_current + 1) % number_of_tx_descriptors;

    // FIXME: This seems odd to me:
    //        We disable interrupts and then wait for and irq to happen...
    Processor::disable_interrupts();
    enable_irq();

    m_registers.write<Register::TXDescTail>(tx_current);
    for (;;) {
        if (descriptor.status) {
            Processor::enable_interrupts();
            break;
        }
        m_wait_queue.wait_forever("E1000NetworkAdapter"sv);
    }
    // FIXME: This should probably do some error checking
    dbgln_if(E1000_DEBUG, "E1000: Sent packet, status is now {:#02x}!", (u8)descriptor.status);
}

void E1000NetworkAdapter::receive()
{
    u32 rx_current;
    for (;;) {
        rx_current = m_registers.read<Register::RXDescTail>() % number_of_rx_descriptors;
        rx_current = (rx_current + 1) % number_of_rx_descriptors;

        // FIXME: We may receive packets split across multiple descriptors
        if (m_operating_mode != OperatingMode::Intel82576_and_later) {

            if (!has_flag(m_rx_descriptors[rx_current].legacy.status, E1000::RxDescriptorStatus::DD))
                break;
            // FIXME: Support split packets
            VERIFY(has_flag(m_rx_descriptors[rx_current].legacy.status, E1000::RxDescriptorStatus::EOP));

            auto* buffer = m_rx_buffers[rx_current];
            u16 length = m_rx_descriptors[rx_current].legacy.length;

            VERIFY(length <= 8192);

            dbgln_if(E1000_DEBUG, "E1000: Received 1 packet @ {:p} ({} bytes)", buffer, length);
            did_receive({ buffer, length });

            m_rx_descriptors[rx_current].legacy.status = E1000::RxDescriptorStatus::None;
            m_registers.write<Register::RXDescTail>(rx_current);
        } else {
            // The write-back descriptor for advanced descriptors is completely different from it's normal descriptor
            // So we need to restore the normal descriptor after we've read it
            {
                auto& descriptor = m_rx_descriptors[rx_current].advanced_write_back;
                if (!has_flag(descriptor.extended_status, E1000::RxDescriptorExtendedStatus::DD))
                    break;

                // FIXME: Support split packets
                VERIFY(has_flag(descriptor.extended_status, E1000::RxDescriptorExtendedStatus::EOP));

                auto* buffer = m_rx_buffers[rx_current];
                u16 length = descriptor.pkt_len;

                VERIFY(length <= 8192);

                dbgln_if(E1000_DEBUG, "E1000: Received 1 packet @ {:p} ({} bytes)", buffer, length);
                did_receive({ buffer, length });
            }
            {
                // Reset the descriptor
                // FIXME: If we'd want to be really nice we should probably do a placement new here or the like
                auto& descriptor = m_rx_descriptors[rx_current].advanced;
                descriptor.packet_buffer_address = bit_cast<u64>(m_rx_buffers[rx_current]);
                // FIXME: Is this always allowed?
                descriptor.header_buffer_address = 0;
            }

            m_registers.write<Register::RXDescTail>(rx_current);
        };
    }
}

i32 E1000NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    auto status = m_registers.read<Register::Status>();
    using enum E1000::LinkSpeed;
    switch (status.speed) {
    case Speed10M:
        return 10;
    case Speed100M:
        return 100;
    case Speed1000M_1:
    case Speed1000M_2:
        return 1000;
    default:
        return NetworkAdapter::LINKSPEED_INVALID;
    }
}

bool E1000NetworkAdapter::link_full_duplex()
{
    auto status = m_registers.read<Register::Status>();
    return status.full_duplex != 0;
}

}
