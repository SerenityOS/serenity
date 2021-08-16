/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef SUN_NIO_CH_SCTP_H
#define SUN_NIO_CH_SCTP_H

#include <stdint.h>
#include <linux/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include "jni.h"

//Causes compiler error if not found, should make warning and uncomment
/*#include <netinet/sctp.h>*/

#ifndef IPPROTO_SCTP
#define IPPROTO_SCTP    132
#endif

/* The current version of lksctp does
 * not define the following option that the Java API (optionally) supports */
#ifndef SCTP_EXPLICIT_EOR
#define SCTP_EXPLICIT_EOR -1
#endif

/* Definitions taken from lksctp-tools-1.0.8/src/include/netinet/sctp.h */
#ifndef SCTP_INITMSG

enum sctp_optname {
        SCTP_RTOINFO,
#define SCTP_RTOINFO SCTP_RTOINFO
        SCTP_ASSOCINFO,
#define SCTP_ASSOCINFO SCTP_ASSOCINFO
        SCTP_INITMSG,
#define SCTP_INITMSG SCTP_INITMSG
        SCTP_NODELAY,   /* Get/set nodelay option. */
#define SCTP_NODELAY    SCTP_NODELAY
        SCTP_AUTOCLOSE,
#define SCTP_AUTOCLOSE SCTP_AUTOCLOSE
        SCTP_SET_PEER_PRIMARY_ADDR,
#define SCTP_SET_PEER_PRIMARY_ADDR SCTP_SET_PEER_PRIMARY_ADDR
        SCTP_PRIMARY_ADDR,
#define SCTP_PRIMARY_ADDR SCTP_PRIMARY_ADDR
        SCTP_ADAPTATION_LAYER,
#define SCTP_ADAPTATION_LAYER SCTP_ADAPTATION_LAYER
        SCTP_DISABLE_FRAGMENTS,
#define SCTP_DISABLE_FRAGMENTS SCTP_DISABLE_FRAGMENTS
        SCTP_PEER_ADDR_PARAMS,
#define SCTP_PEER_ADDR_PARAMS SCTP_PEER_ADDR_PARAMS
        SCTP_DEFAULT_SEND_PARAM,
#define SCTP_DEFAULT_SEND_PARAM SCTP_DEFAULT_SEND_PARAM
        SCTP_EVENTS,
#define SCTP_EVENTS SCTP_EVENTS
        SCTP_I_WANT_MAPPED_V4_ADDR,  /* Turn on/off mapped v4 addresses  */
#define SCTP_I_WANT_MAPPED_V4_ADDR SCTP_I_WANT_MAPPED_V4_ADDR
        SCTP_MAXSEG,    /* Get/set maximum fragment. */
#define SCTP_MAXSEG     SCTP_MAXSEG
        SCTP_STATUS,
#define SCTP_STATUS SCTP_STATUS
        SCTP_GET_PEER_ADDR_INFO,
#define SCTP_GET_PEER_ADDR_INFO SCTP_GET_PEER_ADDR_INFO
        SCTP_DELAYED_ACK_TIME,
#define SCTP_DELAYED_ACK_TIME SCTP_DELAYED_ACK_TIME
        SCTP_CONTEXT,   /* Receive Context */
#define SCTP_CONTEXT SCTP_CONTEXT
        SCTP_FRAGMENT_INTERLEAVE,
#define SCTP_FRAGMENT_INTERLEAVE SCTP_FRAGMENT_INTERLEAVE
        SCTP_PARTIAL_DELIVERY_POINT,    /* Set/Get partial delivery point */
#define SCTP_PARTIAL_DELIVERY_POINT SCTP_PARTIAL_DELIVERY_POINT
        SCTP_MAX_BURST,         /* Set/Get max burst */
#define SCTP_MAX_BURST SCTP_MAX_BURST
};

enum sctp_sac_state {
        SCTP_COMM_UP,
        SCTP_COMM_LOST,
        SCTP_RESTART,
        SCTP_SHUTDOWN_COMP,
        SCTP_CANT_STR_ASSOC,
};

enum sctp_spc_state {
        SCTP_ADDR_AVAILABLE,
        SCTP_ADDR_UNREACHABLE,
        SCTP_ADDR_REMOVED,
        SCTP_ADDR_ADDED,
        SCTP_ADDR_MADE_PRIM,
        SCTP_ADDR_CONFIRMED,
};

enum sctp_sinfo_flags {
        SCTP_UNORDERED = 1,  /* Send/receive message unordered. */
        SCTP_ADDR_OVER = 2,  /* Override the primary destination. */
        SCTP_ABORT=4,        /* Send an ABORT message to the peer. */
        SCTP_EOF=MSG_FIN,    /* Initiate graceful shutdown process. */
};

enum sctp_sn_type {
        SCTP_SN_TYPE_BASE     = (1<<15),
        SCTP_ASSOC_CHANGE,
        SCTP_PEER_ADDR_CHANGE,
        SCTP_SEND_FAILED,
        SCTP_REMOTE_ERROR,
        SCTP_SHUTDOWN_EVENT,
        SCTP_PARTIAL_DELIVERY_EVENT,
        SCTP_ADAPTATION_INDICATION,
};

typedef enum sctp_cmsg_type {
        SCTP_INIT,              /* 5.2.1 SCTP Initiation Structure */
#define SCTP_INIT SCTP_INIT
        SCTP_SNDRCV,            /* 5.2.2 SCTP Header Information Structure */
#define SCTP_SNDRCV SCTP_SNDRCV
} sctp_cmsg_t;

enum sctp_msg_flags {
        MSG_NOTIFICATION = 0x8000,
#define MSG_NOTIFICATION MSG_NOTIFICATION
};

#define SCTP_BINDX_ADD_ADDR 0x01
#define SCTP_BINDX_REM_ADDR 0x02

typedef __s32 sctp_assoc_t;

struct sctp_initmsg {
        __u16 sinit_num_ostreams;
        __u16 sinit_max_instreams;
        __u16 sinit_max_attempts;
        __u16 sinit_max_init_timeo;
};

struct sctp_sndrcvinfo {
        __u16 sinfo_stream;
        __u16 sinfo_ssn;
        __u16 sinfo_flags;
        __u32 sinfo_ppid;
        __u32 sinfo_context;
        __u32 sinfo_timetolive;
        __u32 sinfo_tsn;
        __u32 sinfo_cumtsn;
        sctp_assoc_t sinfo_assoc_id;
};

struct sctp_event_subscribe {
        __u8 sctp_data_io_event;
        __u8 sctp_association_event;
        __u8 sctp_address_event;
        __u8 sctp_send_failure_event;
        __u8 sctp_peer_error_event;
        __u8 sctp_shutdown_event;
        __u8 sctp_partial_delivery_event;
        __u8 sctp_adaptation_layer_event;
};

struct sctp_send_failed {
        __u16 ssf_type;
        __u16 ssf_flags;
        __u32 ssf_length;
        __u32 ssf_error;
        struct sctp_sndrcvinfo ssf_info;
        sctp_assoc_t ssf_assoc_id;
        __u8 ssf_data[0];
};

struct sctp_assoc_change {
        __u16 sac_type;
        __u16 sac_flags;
        __u32 sac_length;
        __u16 sac_state;
        __u16 sac_error;
        __u16 sac_outbound_streams;
        __u16 sac_inbound_streams;
        sctp_assoc_t sac_assoc_id;
        __u8 sac_info[0];
};

struct sctp_shutdown_event {
        __u16 sse_type;
        __u16 sse_flags;
        __u32 sse_length;
        sctp_assoc_t sse_assoc_id;
};

struct sctp_paddr_change {
        __u16 spc_type;
        __u16 spc_flags;
        __u32 spc_length;
        struct sockaddr_storage spc_aaddr;
        int spc_state;
        int spc_error;
        sctp_assoc_t spc_assoc_id;
} __attribute__((packed, aligned(4)));

struct sctp_remote_error {
        __u16 sre_type;
        __u16 sre_flags;
        __u32 sre_length;
        __u16 sre_error;
        sctp_assoc_t sre_assoc_id;
        __u8 sre_data[0];
};

struct sctp_adaptation_event {
        __u16 sai_type;
        __u16 sai_flags;
        __u32 sai_length;
        __u32 sai_adaptation_ind;
        sctp_assoc_t sai_assoc_id;
};

struct sctp_setprim {
        sctp_assoc_t            ssp_assoc_id;
        struct sockaddr_storage ssp_addr;
} __attribute__((packed, aligned(4)));

struct sctp_setpeerprim {
        sctp_assoc_t            sspp_assoc_id;
        struct sockaddr_storage sspp_addr;
} __attribute__((packed, aligned(4)));


struct sctp_pdapi_event {
        __u16 pdapi_type;
        __u16 pdapi_flags;
        __u32 pdapi_length;
        __u32 pdapi_indication;
        sctp_assoc_t pdapi_assoc_id;
};

union sctp_notification {
        struct {
                __u16 sn_type;             /* Notification type. */
                __u16 sn_flags;
                __u32 sn_length;
        } sn_header;
        struct sctp_assoc_change sn_assoc_change;
        struct sctp_paddr_change sn_paddr_change;
        struct sctp_remote_error sn_remote_error;
        struct sctp_send_failed sn_send_failed;
        struct sctp_shutdown_event sn_shutdown_event;
        struct sctp_adaptation_event sn_adaptation_event;
        struct sctp_pdapi_event sn_pdapi_event;
};

#endif /* SCTP_INITMSG */

/* Function types to support dynamic linking of socket API extension functions
 * for SCTP. This is so that there is no linkage depandancy during build or
 * runtime for libsctp.*/
typedef int sctp_getladdrs_func(int sd, sctp_assoc_t id, struct sockaddr **addrs);
typedef int sctp_freeladdrs_func(struct sockaddr *addrs);
typedef int sctp_getpaddrs_func(int sd, sctp_assoc_t id, struct sockaddr **addrs);
typedef int sctp_freepaddrs_func(struct sockaddr *addrs);
typedef int sctp_bindx_func(int sd, struct sockaddr *addrs, int addrcnt, int flags);
typedef int sctp_peeloff_func(int sock, sctp_assoc_t id);


extern sctp_getladdrs_func* nio_sctp_getladdrs;
extern sctp_freeladdrs_func* nio_sctp_freeladdrs;
extern sctp_getpaddrs_func* nio_sctp_getpaddrs;
extern sctp_freepaddrs_func* nio_sctp_freepaddrs;
extern sctp_bindx_func* nio_sctp_bindx;
extern sctp_peeloff_func* nio_sctp_peeloff;

jboolean loadSocketExtensionFuncs(JNIEnv* env);

#endif /* !SUN_NIO_CH_SCTP_H */
