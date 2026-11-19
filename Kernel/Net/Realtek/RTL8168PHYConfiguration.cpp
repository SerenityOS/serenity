/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
#include <Kernel/Net/Realtek/RTL8168Registers.h>

namespace Kernel {

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
    case ChipVersion::RTL8168EVL:
        configure_phy_e_2();
        return;
    case ChipVersion::RTL8168H:
        configure_phy_h_2();
        return;
    case ChipVersion::RTL8126A:
        configure_phy_8126_a();
        return;
    case ChipVersion::Unknown:
        VERIFY_NOT_REACHED();
    default:
        dmesgln_pci(*this, "There is no PHY configuration for this version, the card might not achieve maximum speed.");
        return;
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

void RTL8168NetworkAdapter::configure_phy_8126_a()
{
    // FIXME: linux also loads a PHY firmware blob here, without it 2.5G/5G might not work

    auto update_paged = [&](u16 page, u8 reg, u16 set, u16 clear) {
        phy_out(0x1f, page);
        phy_update(reg, set, clear);
        phy_out(0x1f, 0);
    };

    update_paged(0xa44, 0x11, 1 << 11, 0); // enable gphy 10m
    update_paged(0xa5b, 0x12, 0, 1 << 15); // legacy force mode
    update_paged(0xa43, 0x10, 0, 1 << 2);  // disable ALDPS
    update_paged(0xa6d, 0x14, 0, 0x0010);  // EEE
    update_paged(0xa42, 0x14, 0, 0x0080);
    update_paged(0xa4a, 0x11, 0, 0x0200);
}

}
