/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace Kernel::MII {

// https://ieeexplore.ieee.org/document/9844436
//  Table 22–6—MII management register set
enum class Register {
    Control = 0,
    Status = 1,
    PHYID1 = 2,
    PHYID2 = 3,
    AutoNegotiationAdvertisement = 4,
    AutoNegotiationLinkPartnerBasePageAbility = 5,
    AutoNegotiationExpansion = 6,
    AutoNegotiationNextPageTransmit = 7,
    AutoNegotiationLinkPartnerReceivedNextPage = 8,
    MasterSlaveControl = 9,
    MasterSlaveStatus = 10,
    PSEControl = 11,
    PSEStatus = 12,
    MMDAccessControl = 13,
    MMDAccessData = 14,
    ExtendedStatus = 15,
    Vendor0 = 16,
    // All others are vendor specific, up to 31
};

// Table 22–7—Control register bit definitions
struct Control {
    u16 : 5;
    u16 unidirection_enable : 1;
    u16 speed_selection_msb : 1;
    u16 collision_test : 1;
    u16 duplex_mode : 1;
    u16 restart_auto_negotiation : 1;
    u16 isolate : 1;
    u16 power_down : 1;
    u16 auto_negotiation_enable : 1;
    u16 speed_selection_lsb : 1;
    u16 loopback : 1;
    u16 reset : 1;
};
static_assert(AssertSize<Control, 2>());

// Table 22–8—Status register bit definitions
struct Status {
    u16 extended_capabilities : 1;
    u16 jabber_detect : 1;
    u16 link_status : 1;
    u16 auto_negotiation_ability : 1;
    u16 remote_fault : 1;
    u16 auto_negotiation_complete : 1;
    u16 mf_preamble_suppression : 1;
    u16 unidirectional_ability : 1;
    u16 extended_status : 1;
    // FIXME: These should differ based on the speed?
    u16 _100BaseT2_half_duplex : 1;
    u16 _100BaseT2_full_duplex : 1;
    u16 _10Mbs_half_duplex : 1;
    u16 _10Mbs_full_duplex : 1;
    u16 _100BaseX_half_duplex : 1;
    u16 _100BaseX_full_duplex : 1;
    u16 _100BaseT4 : 1;
};
static_assert(AssertSize<Status, 2>());

// Table 28A–1—Selector Field value mappings
enum class ANASelector : u16 {
    // Reserved: 0b00000
    Std802_3 = 0b00001,
    Std802_9a_1995 = 0b00010, // Withdrawn
    Std802_5v_2001 = 0b00011, // Withdrawn
    Std1394 = 0b00100,
    INCITS = 0b00101,
    // Others are reserved
};

// Table 28–2—Advertisement register bit definitions
struct AutoNegotiationAdvertisement {
    ANASelector selector : 5;   // S0-4
    u16 technology_ability : 7; // A0-6
    u16 extended_next_page : 1; // XNP
    u16 remote_fault : 1;       // RF
    u16 : 1;                    // Ack?
    u16 next_page : 1;          // NP
};
static_assert(AssertSize<AutoNegotiationAdvertisement, 2>());

// Table 28B–1—Technology Ability Field bit assignments
// alternatively better formatted:
// https://www.intel.com/content/dam/www/public/us/en/documents/datasheets/82576eb-gigabit-ethernet-controller-datasheet.pdf
// 8.25.5 Auto–Negotiation Advertisement Register - ANA (04d; R/W)
struct AutoNegotiationAdvertisement802_3 {
    ANASelector selector : 5; // S0-4, must be 802.3
    u16 _10BaseT : 1;         // 10BASE-T
    u16 _10BaseT_FD : 1;      // 10BASE-T Full Duplex
    u16 _100BaseTX : 1;       // 100BASE-TX
    u16 _100BaseTX_FD : 1;    // 100BASE-TX Full Duplex
    u16 _100BaseT4 : 1;       // 100BASE-T4
    u16 pause : 1;
    u16 asymmetric_pause : 1;
    u16 extended_next_page : 1; // XNP
    u16 remote_fault : 1;       // RF
    u16 : 1;                    // Ack?
    u16 next_page : 1;          // NP
};
static_assert(AssertSize<AutoNegotiationAdvertisement, 2>());

// Table 22–11—Extended Status register bit definitions
struct ExtendedStatus {
    u16 : 12;
    u16 _1000BaseT_half_duplex : 1;
    u16 _1000BaseT_full_duplex : 1;
    u16 _1000BaseX_half_duplex : 1;
    u16 _1000BaseX_full_duplex : 1;
};
static_assert(AssertSize<ExtendedStatus, 2>());

inline bool is_full_duplex(Status status, ExtendedStatus extended_status)
{
    return 0
        // FIXME: What about 100 Base T4?
        | status._100BaseT2_full_duplex
        | status._10Mbs_full_duplex
        | status._100BaseX_full_duplex
        | extended_status._1000BaseT_full_duplex
        | extended_status._1000BaseX_full_duplex;
}

template<Register T>
struct RegisterTraits;

template<>
struct RegisterTraits<Register::Control> {
    using RegisterType = Control;
};

template<>
struct RegisterTraits<Register::Status> {
    using RegisterType = Status;
};

template<>
struct RegisterTraits<Register::AutoNegotiationAdvertisement> {
    using RegisterType = AutoNegotiationAdvertisement;
};

template<>
struct RegisterTraits<Register::ExtendedStatus> {
    using RegisterType = ExtendedStatus;
};

template<Register T>
using RegisterType = typename RegisterTraits<T>::RegisterType;

}
