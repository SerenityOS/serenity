/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

struct NVMeCompletion;
struct NVMeSubmission;

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

// BAR
static constexpr u32 BAR_ADDR_MASK = 0xFFFFFFF0;
// DOORBELL
static constexpr u32 REG_SQ0TDBL_START = 0x1000;
static constexpr u32 REG_SQ0TDBL_END = 0x1003;
static constexpr u8 DBL_REG_SIZE = 8;
// CAP
static constexpr u8 CAP_DBL_SHIFT = 32;
static constexpr u8 CAP_DBL_MASK = 0xf;
static constexpr u8 CAP_TO_SHIFT = 24;
static constexpr u64 CAP_TO_MASK = 0xff << CAP_TO_SHIFT;
static constexpr u16 MQES(u64 cap)
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

static constexpr u16 CC_AQA_MASK = (0xfff);
static constexpr u16 ACQ_SIZE(u32 x)
{
    return (x >> 16) & CC_AQA_MASK;
}
static constexpr u16 ASQ_SIZE(u32 x)
{
    return x & CC_AQA_MASK;
}
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

static constexpr u16 IO_QUEUE_SIZE = 64; // TODO:Need to be configurable

// IDENTIFY
static constexpr u16 NVMe_IDENTIFY_SIZE = 4096;
static constexpr u8 NVMe_CNS_ID_ACTIVE_NS = 0x2;
static constexpr u8 NVMe_CNS_ID_NS = 0x0;
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
};

// IO opcodes
enum IOCommandOpcode {
    OP_NVME_WRITE = 0x1,
    OP_NVME_READ = 0x2
};

// FLAGS
static constexpr u8 QUEUE_PHY_CONTIGUOUS = (1 << 0);
static constexpr u8 QUEUE_IRQ_ENABLED = (1 << 1);

struct NVMeCompletion {
    LittleEndian<u32> cmd_spec;
    LittleEndian<u32> res;

    LittleEndian<u16> sq_head; /* how much of this queue may be reclaimed */
    LittleEndian<u16> sq_id;   /* submission queue that generated this entry */

    u16 command_id;           /* of the command which completed */
    LittleEndian<u16> status; /* did the command fail, and if so, why? */
};

struct DataPtr {
    LittleEndian<u64> prp1;
    LittleEndian<u64> prp2;
};

struct NVMeSubmission {
    LittleEndian<u8> op;
    LittleEndian<u8> flags;
    LittleEndian<u16> cmdid;
    LittleEndian<u32> nsid;
    LittleEndian<u64> rsvd;
    LittleEndian<u64> meta_ptr;
    struct DataPtr data_ptr;
    LittleEndian<u32> cdw10;
    LittleEndian<u32> cdw11;
    LittleEndian<u32> cdw12;
    LittleEndian<u32> cdw13;
    LittleEndian<u32> cdw14;
    LittleEndian<u32> cdw15;
};
