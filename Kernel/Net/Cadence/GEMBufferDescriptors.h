/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/Cadence/GEM.h>

namespace Kernel {

struct CadenceGEMNetworkAdapter::RxBufferDescriptorEntry {
    // Word 0
    u32 is_software_owned : 1;
    u32 is_last_descriptor : 1;
    u32 buffer_address_31_2 : 30;

    // Word 1
    u32 received_frame_length : 12;
    u32 frame_has_bad_fcs : 1;
    u32 buffer_contains_start_of_frame : 1;
    u32 buffer_contains_end_of_frame : 1;
    u32 canonical_format_indicator_bit : 1;
    u32 vlan_priority : 3;
    u32 priority_tag_detected : 1;
    u32 vlan_tag_detected : 1;
    u32 type_id_register_match_or_rx_checksum_offloading_status : 2;
    u32 : 1;
    u32 specific_address_register_match : 2;
    u32 specific_address_register_match_found : 1;
    u32 io_address_match : 1;
    u32 unicast_hash_match : 1;
    u32 multicast_hash_match : 1;
    u32 global_all_ones_broadcast_address_detected : 1;

    // Word 2
    u32 buffer_address_63_32;

    // Word 3
    u32 _;
};
static_assert(AssertSize<CadenceGEMNetworkAdapter::RxBufferDescriptorEntry, 4 * sizeof(u32)>());

struct CadenceGEMNetworkAdapter::TxBufferDescriptorEntry {
    // Word 0
    u32 buffer_address_31_0;

    // Word 1
    u32 buffer_length : 14;
    u32 : 1;
    u32 last_buffer : 1;
    u32 no_crc_to_be_appended_by_mac : 1;
    u32 : 3;
    u32 checksum_offload_error_status : 3;
    u32 : 3;
    u32 late_collision : 1;
    u32 transmit_frame_corruption_due_to_axi_error : 1;
    u32 : 1;
    u32 retry_limit_exceeded : 1;
    u32 is_last_descriptor : 1;
    u32 is_software_owned : 1;

    // Word 2
    u32 buffer_address_63_32;

    // Word 3
    u32 _;
};
static_assert(AssertSize<CadenceGEMNetworkAdapter::TxBufferDescriptorEntry, 4 * sizeof(u32)>());

}
