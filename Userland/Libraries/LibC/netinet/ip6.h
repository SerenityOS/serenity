/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <assert.h>
#include <inttypes.h>
#include <netinet/in.h>

__BEGIN_DECLS

struct ip6_hdr {
    union {
        struct ip6_hdrctl {
            uint32_t ip6_un1_flow; // 4 bits version, 8 bits TC, 20 bits flow-ID
            uint16_t ip6_un1_plen; // payload length
            uint8_t ip6_un1_nxt;   // next header
            uint8_t ip6_un1_hlim;  // hop limit
        } ip6_un1;
        uint8_t ip6_un2_vfc; // 4 bits version, top 4 bits tclass
    } ip6_ctlun;
    struct in6_addr ip6_src; // source address
    struct in6_addr ip6_dst; // destination address
} __attribute__((packed));
static_assert(sizeof(struct ip6_hdr) == 40, "struct ip6_hdr: invalid length");

#define ip6_vfc ip6_ctlun.ip6_un2_vfc
#define ip6_flow ip6_ctlun.ip6_un1.ip6_un1_flow
#define ip6_plen ip6_ctlun.ip6_un1.ip6_un1_plen
#define ip6_nxt ip6_ctlun.ip6_un1.ip6_un1_nxt
#define ip6_hlim ip6_ctlun.ip6_un1.ip6_un1_hlim
#define ip6_hops ip6_ctlun.ip6_un1.ip6_un1_hlim

struct ip6_ext {
    uint8_t ip6e_nxt;
    uint8_t ip6e_len;
} __attribute__((packed));

struct ip6_hbh {
    uint8_t ip6h_nxt;
    uint8_t ip6h_len;
    // followed by options
} __attribute__((packed));

struct ip6_dest {
    uint8_t ip6d_nxt;
    uint8_t ip6d_len;
    // followed by options
} __attribute__((packed));

struct ip6_rthdr {
    uint8_t ip6r_nxt;
    uint8_t ip6r_len;
    uint8_t ip6r_type;
    uint8_t ip6r_segleft;
    // followed by routing type specific data
} __attribute__((packed));

struct ip6_frag {
    uint8_t ip6f_nxt;
    uint8_t ip6f_reserved;
    uint16_t ip6f_offlg;
    uint32_t ip6f_ident;
};

#if __BYTE_ORDER == __BIG_ENDIAN
#    define IP6F_OFF_MASK 0xfff8
#    define IP6F_RESERVED_MASK 0x0006
#    define IP6F_MORE_FRAG 0x0001
#elif __BYTE_ORDER == __LITTLE_ENDIAN
#    define IP6F_OFF_MASK 0xf8ff
#    define IP6F_RESERVED_MASK 0x0600
#    define IP6F_MORE_FRAG 0x0100
#endif

struct ip6_opt {
    uint8_t ip6o_type;
    uint8_t ip6o_len;
};

#define IP6OPT_TYPE(o) ((o) & 0xc0)
#define IP6OPT_TYPE_SKIP 0x00
#define IP6OPT_TYPE_DISCARD 0x40
#define IP6OPT_TYPE_FORCEICMP 0x80
#define IP6OPT_TYPE_ICMP 0xc0
#define IP6OPT_TYPE_MUTABLE 0x20
#define IP6OPT_PAD1 0
#define IP6OPT_PADN 1
#define IP6OPT_JUMBO 0xc2
#define IP6OPT_NSAP_ADDR 0xc3
#define IP6OPT_TUNNEL_LIMIT 0x04
#define IP6OPT_ROUTER_ALERT 0x05

__END_DECLS
