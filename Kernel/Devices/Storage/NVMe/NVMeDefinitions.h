/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

struct ControllerRegister {
    u64 cap;
    u32 vs;
    u32 intms;
    u32 intmc;
    u32 cc;
    u32 rsvd1;
    u32 csts;
    u32 nssr;
    u32 aqa;
    u64 asq;
    u64 acq;
    u64 rsvd2[505];
};

struct IdentifyNamespace {
    u64 nsze;
    u64 ncap;
    u8 rsdv1[10];
    u8 flbas;
    u8 rsvd2[100];
    u32 lbaf[16];
    u64 rsvd3[488];
};

// FIXME: For now only one value is used. Once we start using
// more values from id_ctrl command, use separate member variables
// instead of using rsd array.
struct IdentifyController {
    u8 rsdv1[256];
    u16 oacs;
    u8 rsdv2[3838];
};

// DOORBELL
static constexpr u32 REG_SQ0TDBL_START = 0x1000;
static constexpr u32 REG_SQ0TDBL_END = 0x1003;
static constexpr u8 DBL_REG_SIZE = 8;
static constexpr u16 ID_CTRL_SHADOW_DBBUF_MASK = 0x0100;

// CAP
static constexpr u8 CAP_DBL_SHIFT = 32;
static constexpr u8 CAP_DBL_MASK = 0xf;
static constexpr u8 CAP_TO_SHIFT = 24;
static constexpr u64 CAP_TO_MASK = 0xffu << CAP_TO_SHIFT;
static constexpr u32 MQES(u64 cap)
{
    return (cap & 0xffff) + 1;
}

static constexpr u32 CAP_TO(u64 cap)
{
    return (cap & CAP_TO_MASK) >> CAP_TO_SHIFT;
}

// CC – Controller Configuration
static constexpr u8 CC_EN_BIT = 0x0;
static constexpr u8 CSTS_RDY_BIT = 0x0;
static constexpr u8 CSTS_SHST_SHIFT = 2;
static constexpr u32 CSTS_SHST_MASK = 0x3 << CSTS_SHST_SHIFT;
static constexpr u8 CC_IOSQES_BIT = 16;
static constexpr u8 CC_IOCQES_BIT = 20;

static constexpr u32 CSTS_SHST(u32 x)
{
    return (x & CSTS_SHST_MASK) >> CSTS_SHST_SHIFT;
}

static constexpr u16 AQA_ACQ_SHIFT = 16;
static constexpr u8 CQ_WIDTH = 4; // CQ is 16 bytes(2^4) in size.
static constexpr u8 SQ_WIDTH = 6; // SQ size is 64 bytes(2^6) in size.
static constexpr u16 CQ_SIZE(u16 q_depth)
{
    return q_depth << CQ_WIDTH;
}
static constexpr u16 SQ_SIZE(u16 q_depth)
{
    return q_depth << SQ_WIDTH;
}
static constexpr u8 PHASE_TAG(u16 x)
{
    return x & 0x1;
}
static constexpr u16 CQ_STATUS_FIELD_MASK = 0xfffe;
static constexpr u16 CQ_STATUS_FIELD(u16 x)
{
    return (x & CQ_STATUS_FIELD_MASK) >> 1;
}

static constexpr u16 ADMIN_QUEUE_SIZE = 2;
static constexpr u16 IO_QUEUE_SIZE = 64; // TODO:Need to be configurable

// IDENTIFY
static constexpr u16 NVMe_IDENTIFY_SIZE = 4096;
static constexpr u8 NVMe_CNS_ID_NS = 0x0;
static constexpr u8 NVMe_CNS_ID_CTRL = 0x1;
static constexpr u8 NVMe_CNS_ID_ACTIVE_NS = 0x2;
static constexpr u8 FLBA_SIZE_INDEX = 26;
static constexpr u8 FLBA_SIZE_MASK = 0xf;
static constexpr u8 LBA_FORMAT_SUPPORT_INDEX = 128;
static constexpr u32 LBA_SIZE_MASK = 0x00ff0000;

// OPCODES
// ADMIN COMMAND SET
enum AdminCommandOpCode {
    OP_ADMIN_CREATE_COMPLETION_QUEUE = 0x5,
    OP_ADMIN_CREATE_SUBMISSION_QUEUE = 0x1,
    OP_ADMIN_IDENTIFY = 0x6,
    OP_ADMIN_DBBUF_CONFIG = 0x7C,
};

// IO opcodes
enum IOCommandOpcode {
    OP_NVME_WRITE = 0x1,
    OP_NVME_READ = 0x2
};

// FLAGS
static constexpr u8 QUEUE_PHY_CONTIGUOUS = (1 << 0);
static constexpr u8 QUEUE_IRQ_ENABLED = (1 << 1);
static constexpr u8 QUEUE_IRQ_DISABLED = (0 << 1);

struct [[gnu::packed]] NVMeCompletion {
    LittleEndian<u32> cmd_spec;
    LittleEndian<u32> res;

    LittleEndian<u16> sq_head; /* how much of this queue may be reclaimed */
    LittleEndian<u16> sq_id;   /* submission queue that generated this entry */

    u16 command_id;           /* of the command which completed */
    LittleEndian<u16> status; /* did the command fail, and if so, why? */
};

struct [[gnu::packed]] DataPtr {
    LittleEndian<u64> prp1;
    LittleEndian<u64> prp2;
};

struct [[gnu::packed]] NVMeGenericCmd {
    LittleEndian<u32> nsid;
    LittleEndian<u64> rsvd;
    LittleEndian<u64> metadata;
    struct DataPtr data_ptr;
    LittleEndian<u32> cdw10;
    LittleEndian<u32> cdw11;
    LittleEndian<u32> cdw12;
    LittleEndian<u32> cdw13;
    LittleEndian<u32> cdw14;
    LittleEndian<u32> cdw15;
};

struct [[gnu::packed]] NVMeRWCmd {
    LittleEndian<u32> nsid;
    LittleEndian<u64> rsvd;
    LittleEndian<u64> metadata;
    struct DataPtr data_ptr;
    LittleEndian<u64> slba;
    LittleEndian<u16> length;
    LittleEndian<u16> control;
    LittleEndian<u32> dsmgmt;
    LittleEndian<u32> reftag;
    LittleEndian<u16> apptag;
    LittleEndian<u16> appmask;
};

struct [[gnu::packed]] NVMeIdentifyCmd {
    LittleEndian<u32> nsid;
    LittleEndian<u64> rsvd1[2];
    struct DataPtr data_ptr;
    u8 cns;
    u8 rsvd2;
    LittleEndian<u16> ctrlid;
    u8 rsvd3[3];
    u8 csi;
    u64 rsvd4[2];
};

struct [[gnu::packed]] NVMeCreateCQCmd {
    u32 rsvd1[5];
    LittleEndian<u64> prp1;
    u64 rsvd2;
    LittleEndian<u16> cqid;
    LittleEndian<u16> qsize;
    LittleEndian<u16> cq_flags;
    LittleEndian<u16> irq_vector;
    u64 rsvd12[2];
};

struct [[gnu::packed]] NVMeCreateSQCmd {
    u32 rsvd1[5];
    LittleEndian<u64> prp1;
    u64 rsvd2;
    LittleEndian<u16> sqid;
    LittleEndian<u16> qsize;
    LittleEndian<u16> sq_flags;
    LittleEndian<u16> cqid;
    u64 rsvd12[2];
};

struct [[gnu::packed]] NVMeDBBUFCmd {
    u32 rsvd1[5];
    struct DataPtr data_ptr;
    u32 rsvd12[6];
};

struct [[gnu::packed]] NVMeSubmission {
    u8 op;
    u8 flags;
    LittleEndian<u16> cmdid;
    union [[gnu::packed]] {
        NVMeGenericCmd generic;
        NVMeIdentifyCmd identify;
        NVMeRWCmd rw;
        NVMeCreateCQCmd create_cq;
        NVMeCreateSQCmd create_sq;
        NVMeDBBUFCmd dbbuf_cmd;
    };
};
