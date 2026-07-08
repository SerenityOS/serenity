/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Forward.h>

namespace Kernel::MDIO {

enum class LinkStatus {
    Down,
    Up,
};

// FIXME: Support Clause 45. At least some MACs seem to suppport both Clause 22 and 45, so we should
//        maybe make the Interface class support both at once and move it out of the Clause22 namespace.

// IEEE 802.3-2022 22. Reconciliation Sublayer (RS) and Media Independent Interface (MII)
namespace Clause22 {

// IEEE 802.3-2022 22.2.4 Management functions
enum class RegisterAddress : u8 {
    // Basic register set
    Control = 0,
    Status = 1,

    // Extended register set
    PHYIdentifier1 = 2,
    PHYIdentifier2 = 3,
    AutoNegotiationAdvertisement = 4,
    AutoNegotiationLinkPartnerBasePageAbility = 5,
    AutoNegotiationExpansion = 6,
    AutoNegotiationNextPageTransmit = 7,
    AutoNegotiationLinkPartnerReceivedNextPage = 8,
    MasterSlaveControlRegister = 9,
    MasterSlaveStatusRegister = 10,
    PSEControlRegister = 11,
    PSEStatusRegister = 12,
    MMDAccessControlRegister = 13,
    MMDAccessAddressDataRegister = 14,
    ExtendedStatus = 15,
};

class Interface {
public:
    virtual ~Interface() = default;

protected:
    virtual void on_phy_link_status_change(LinkStatus) { }

    virtual u16 read_phy_register(u8 phy_id, RegisterAddress address) = 0;
    virtual void write_phy_register(u8 phy_id, RegisterAddress address, u16 value) = 0;

    template<typename Register>
    Register read_phy_register(u8 phy_id)
    requires(requires { Register::REGISTER_ADDRESS; })
    {
        static_assert(sizeof(Register) == sizeof(u16));
        return bit_cast<Register>(read_phy_register(phy_id, Register::REGISTER_ADDRESS));
    }

    template<typename Register>
    void write_phy_register(u8 phy_id, Register value)
    requires(requires { Register::REGISTER_ADDRESS; })
    {
        static_assert(sizeof(Register) == sizeof(u16));
        write_phy_register(phy_id, Register::REGISTER_ADDRESS, bit_cast<u16>(value));
    }

    // FIXME: Support handling multiple PHYs per interface.
    ErrorOr<NonnullRefPtr<Process>> spawn_mdio_handling_task(u8 phy_id);

private:
    void mdio_handling_thread(u8 phy_id);
};

// IEEE 802.3-2022 22.2.4.1 Control register (Register 0)
struct Control {
    static constexpr auto REGISTER_ADDRESS = RegisterAddress::Control;

    u16 reserved : 5;
    u16 unidirectional_enable : 1;
    u16 speed_selection_msb : 1;
    u16 collision_test : 1;
    u16 full_duplex : 1;
    u16 restart_auto_negotiation : 1;
    u16 isolate : 1;
    u16 power_down : 1;
    u16 auto_negotiation_enable : 1;
    u16 speed_selection_lsb : 1;
    u16 enable_loopback_mode : 1;
    u16 reset : 1;
};
static_assert(AssertSize<Control, sizeof(u16)>());

// IEEE 802.3-2022 22.2.4.2 Status register (Register 1)
struct Status {
    static constexpr auto REGISTER_ADDRESS = RegisterAddress::Status;

    u16 has_extended_register_capabilities : 1;
    u16 jabber_condition_detected : 1;
    u16 link_up : 1;
    u16 has_auto_negotiation_ability : 1;
    u16 remote_fault_condition_detected : 1;
    u16 auto_negotiation_process_completed : 1;
    u16 accepts_management_framces_with_preamble_suppressed : 1;
    u16 has_unidirectional_ability : 1;
    u16 has_extended_status_information_in_register_15 : 1;
    u16 supports_half_duplex_100_base_t2 : 1;
    u16 supports_full_duplex_100_base_t2 : 1;
    u16 supports_10_mb_s_in_half_duplex_mode : 1;
    u16 supports_10_mb_s_in_full_duplex_mode : 1;
    u16 supports_half_duplex_100_base_x : 1;
    u16 supports_full_duplex_100_base_x : 1;
    u16 supports_100_base_t4 : 1;
};
static_assert(AssertSize<Status, sizeof(u16)>());

}

}
