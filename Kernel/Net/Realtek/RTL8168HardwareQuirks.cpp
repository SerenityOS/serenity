/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
#include <Kernel/Net/Realtek/RTL8168Registers.h>

namespace Kernel {

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
    case ChipVersion::RTL8168EVL:
        hardware_quirks_e_2();
        return;
    case ChipVersion::RTL8168H:
        hardware_quirks_h();
        return;
    case ChipVersion::RTL8125B:
        hardware_quirks_8125_b();
        return;
    case ChipVersion::RTL8126A:
        hardware_quirks_8126_a();
        return;
    case ChipVersion::Unknown:
        VERIFY_NOT_REACHED();

    default:
        dmesgln_pci(*this, "There are no hardware quirks for this version, the card might not work entirely properly.");
        return;
    }
}

void RTL8168NetworkAdapter::hardware_quirks_b()
{
    // disable checked reserved bits
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) & ~Config3::BeaconEnable);

    // configure the maximum transmit packet size (again)
    out16(RTL8168Register::MaxTXPacketSize, MTPS_JUMBO);

    // disable checked reserved bits
    out8(RTL8168Register::Config4, in8(RTL8168Register::Config4) & ~1);
}

void RTL8168NetworkAdapter::hardware_quirks_c_1()
{
    csi_enable(CSI_ACCESS_2);

    out8(RTL8168Register::Debug, 0x06 | Debug::FixNAK1 | Debug::FixNAK2);

    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x02, 0x0800, 0x1000 },
        { 0x03, 0, 0x0002 },
        { 0x06, 0x0080, 0x0000 },
    });
    extended_phy_initialize(ephy_info);

    out8(RTL8168Register::Config1, in8(RTL8168Register::Config1) | Config1::SpeedDown);
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) & ~Config3::BeaconEnable);
}

void RTL8168NetworkAdapter::hardware_quirks_c_2()
{
    csi_enable(CSI_ACCESS_2);

    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x01, 0, 0x1 },
        { 0x03, 0x0400, 0x0020 },
    });
    extended_phy_initialize(ephy_info);

    out8(RTL8168Register::Config1, in8(RTL8168Register::Config1) | Config1::SpeedDown);
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) | Config3::BeaconEnable);

    // FIXME: Disable PCIe clock request
}

void RTL8168NetworkAdapter::hardware_quirks_c_3()
{
    csi_enable(CSI_ACCESS_2);

    out8(RTL8168Register::Config1, in8(RTL8168Register::Config1) | Config1::SpeedDown);
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) & ~Config3::BeaconEnable);

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
    out8(RTL8168Register::MaxTXPacketSize, 0x27);

    // FIXME: Disable PCIe clock request

    out8(RTL8168Register::DLLPR, in8(RTL8168Register::DLLPR) | DLLPR::PFMEnable);
    out32(RTL8168Register::Misc, in32(RTL8168Register::Misc) | Misc::PWMEnable);
    out8(RTL8168Register::Config5, in8(RTL8168Register::Config5) & ~Config5::SPIEnable);
}

void RTL8168NetworkAdapter::hardware_quirks_h()
{
    // disable aspm and clock request before accessing extended phy
    out8(RTL8168Register::Config2, in8(RTL8168Register::Config2) & ~Config2::ClockRequestEnable);
    out8(RTL8168Register::Config5, in8(RTL8168Register::Config5) & ~Config5::ASPMEnable);

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
    out32(RTL8168Register::Misc, in32(RTL8168Register::Misc) & ~Misc::RXDVGateEnable);

    // set early TX
    out8(RTL8168Register::MaxTXPacketSize, 0x27);

    // vendor magic values ???
    eri_out(0xC0, ERI_MASK_0011, 0, ERI_EXGMAC);
    eri_out(0xB8, ERI_MASK_0011, 0, ERI_EXGMAC);

    out8(RTL8168Register::DLLPR, in8(RTL8168Register::DLLPR) & ~DLLPR::PFMEnable);
    out8(RTL8168Register::Misc1, in8(RTL8168Register::Misc1) & ~Misc1::PFMD3ColdEnable);
    out8(RTL8168Register::DLLPR, in8(RTL8168Register::DLLPR) & ~DLLPR::TX10MPSEnable);

    // vendor magic values ???
    eri_update(0x1B0, ERI_MASK_0011, 0, 0x1000, ERI_EXGMAC);

    // disable l2l3 state
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) & ~Config3::ReadyToL23);

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

void RTL8168NetworkAdapter::hardware_quirks_8125_generic()
{
    // Set default ASPM entry latency (L0 7us, L1 16us) and disable L2/L3 ready
    out8(RTL8168Register::Config3, in8(RTL8168Register::Config3) & ~Config3::ReadyToL23);

    out16(static_cast<RTL8168Register>(0x382), 0x221b);
    out32(RTL8168Register::RSS_CTRL_8125, 0);
    out16(RTL8168Register::Q_NUM_CTRL_8125, 0);

    // Disable UPS
    ocp_update(0xd40a, 0, 0x0010);
    out8(RTL8168Register::Config1, in8(RTL8168Register::Config1) & ~0x10);

    ocp_out(0xc140, 0xffff);
    ocp_out(0xc142, 0xffff);

    ocp_update(0xd3e2, 0x03a9, 0x0fff);
    ocp_update(0xd3e4, 0x0000, 0x00ff);
    ocp_update(0xe860, 0x0080, 0x0000);

    // Disable new TX descriptor format
    ocp_update(0xeb58, 0x0000, 0x0001);

    if (m_version >= ChipVersion::RTL8126A)
        out8(static_cast<RTL8168Register>(0xD8), in8(static_cast<RTL8168Register>(0xD8)) & ~0x02);

    switch (m_version) {
    case ChipVersion::RTL8125B:
        ocp_update(0xe614, 0x0200, 0x0700);
        break;
    case ChipVersion::RTL8126A:
        ocp_update(0xe614, 0x0400, 0x0700);
        break;
    case ChipVersion::RTL8127A:
        ocp_update(0xe614, 0x0f00, 0x0f00);
        break;
    default:
        ocp_update(0xe614, 0x0300, 0x0700);
        break;
    }

    if (m_version == ChipVersion::RTL8125B)
        ocp_update(0xe63e, 0x0000, 0x0c30);
    else
        ocp_update(0xe63e, 0x0020, 0x0c30);

    ocp_update(0xc0b4, 0x000c, 0x0000);
    ocp_update(0xeb6a, 0x0033, 0x00ff);
    ocp_update(0xeb50, 0x0040, 0x03e0);
    ocp_update(0xe056, 0x0000, 0x00f0);
    ocp_update(0xe040, 0x0000, 0x1000);
    ocp_update(0xea1c, 0x0001, 0x0003);

    if (m_version >= ChipVersion::RTL8126A)
        ocp_update(0xea1c, 0x0000, 0x0300);
    else
        ocp_update(0xea1c, 0x0000, 0x0004);

    ocp_update(0xe0c0, 0x4403, 0x4f0f);
    ocp_update(0xe052, 0x0068, 0x0080);
    ocp_update(0xd430, 0x047f, 0x0fff);

    ocp_update(0xea1c, 0x0000, 0x0004);
    ocp_update(0xeb54, 0x0001, 0x0000);
    Processor::wait_check();
    ocp_update(0xeb54, 0x0000, 0x0001);
    out16(static_cast<RTL8168Register>(0x1880), in16(static_cast<RTL8168Register>(0x1880)) & ~0x0030);

    ocp_out(0xe098, 0xc302);

    for (int i = 0; i < 1000 && (ocp_in(0xe00e) & (1 << 13)); ++i)
        Processor::wait_check();

    // Disable RXDV gate
    out32(RTL8168Register::Misc, in32(RTL8168Register::Misc) & ~Misc::RXDVGateEnable);
}

void RTL8168NetworkAdapter::hardware_quirks_8125_b()
{
    // Enable extended tally counter
    ocp_update(0xea84, 0x3, 0);

    static constexpr auto ephy_info = to_array<EPhyUpdate>({
        { 0x0b, 0xffff, 0xa908 },
        { 0x1e, 0xffff, 0x20eb },
        { 0x02, 0x0030, 0x0020 },
        { 0x1e, 0xffff, 0x20eb },
        { 0x02, 0x0030, 0x0020 },
        { 0x02, 0x0030, 0x0020 },
    });
    extended_phy_initialize(ephy_info);

    hardware_quirks_8125_generic();
}

void RTL8168NetworkAdapter::hardware_quirks_8126_a()
{
    // FIXME: This requires more stuff
    hardware_quirks_8125_generic();
}

}
