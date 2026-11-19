/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

enum class RTL8168Register : u16 {
    MACAddress = 0x00,
    MulticastAddress0 = 0x08,
    MulticastAddress4 = 0x0C,

    TXAddress = 0x20,

    Command = 0x37,
    TXStart = 0x38,
    InterruptMask = 0x3C,
    InterruptStatus = 0x3E,
    TXConfig = 0x40,
    RXConfig = 0x44,

    CFG9346 = 0x50,
    Config0 = 0x51,
    Config1 = 0x52,
    Config2 = 0x53,
    Config3 = 0x54,
    Config4 = 0x55,
    Config5 = 0x56,

    MultipleInterrupt = 0x5C,

    PHYAccess = 0x60,

    CSIData = 0x64,
    CSIAddress = 0x68,

    PHYStatus = 0x6C,

    MACDebug = 0x6D,
    GPIO = 0x6E,
    PMCH = 0x6F,

    ERIData = 0x70,
    ERIAddress = 0x74,
    EPHYAccess = 0x80,

    OCPData = 0xB0,
    OCPAddress = 0xB4,
    GPHYOCP = 0xB8,

    DLLPR = 0xD0,
    Debug = 0xD1,
    MCU = 0xD3,
    RMS = 0xDA,

    CPlusCommand = 0xE0,
    InterruptModeration = 0xE2,
    RXAddress = 0xE4,

    MaxTXPacketSize = 0xEC,
    Misc = 0xF0,
    Misc1 = 0xF2,

    IBCR0 = 0xF8,
    IBCR2 = 0xF9,
    IBISR0 = 0xFB,

    INT_CFG0_8125 = 0x34,
    InterruptMask_8125 = 0x38,
    InterruptStatus_8125 = 0x3C,
    INT_CFG1_8125 = 0x7A,
    TXStart_8125 = 0x90,
    RSS_CTRL_8125 = 0x4500,
    Q_NUM_CTRL_8125 = 0x4800,
};

enum Interrupt : u32 {
    RXOK = 1 << 0,
    RXError = 1 << 1,
    TXOK = 1 << 2,
    TXError = 1 << 3,
    RXOverflow = 1 << 4,
    LinkChange = 1 << 5,
    RXFIFOOverflow = 1 << 6,
    SystemError = 1 << 15,
};
AK_ENUM_BITWISE_OPERATORS(Interrupt);

enum Command : u8 {
    TXEnable = 1 << 2,
    RXEnable = 1 << 3,
    Reset = 1 << 4,
    Stop = 1 << 7,
};
AK_ENUM_BITWISE_OPERATORS(Command);

enum TXStart : u8 {
    Start = 1 << 6,
};
AK_ENUM_BITWISE_OPERATORS(TXStart);

enum TXConfig : u32 {
    MaxTXDMAUnlimited = 7 << 8,
    Empty = 1 << 11,
    InterFrameGap = 3 << 24,
};
AK_ENUM_BITWISE_OPERATORS(TXConfig);

enum RXConfig : u32 {
    AcceptAll = 1 << 0,
    AcceptPhysicalMatch = 1 << 1,
    AcceptMulticast = 1 << 2,
    AcceptBroadcast = 1 << 3,
    AcceptRunt = 1 << 4,
    AcceptError = 1 << 5,

    AcceptMask = AcceptAll | AcceptPhysicalMatch | AcceptMulticast | AcceptBroadcast | AcceptRunt | AcceptError,

    MaxRXDMAUnlimited = 7 << 8,

    EarlyOff = 1 << 11,
    PauseSlot = 1 << 11,

    NoRXThreshold = 7 << 13,

    MultiEnable = 1 << 14,
    _128IntEnable = 1 << 15,

    FetchDefault8125 = 1 << 30,
};
AK_ENUM_BITWISE_OPERATORS(RXConfig);

enum CFG9346 : u8 {
    EEM0 = 1 << 6,
    EEM1 = 1 << 7,
    Unlock = EEM0 | EEM1,
};
AK_ENUM_BITWISE_OPERATORS(CFG9346);

enum Config1 : u8 {
    SpeedDown = 1 << 4,
};
AK_ENUM_BITWISE_OPERATORS(Config1);

enum Config2 : u8 {
    ClockRequestEnable = 1 << 7,
};
AK_ENUM_BITWISE_OPERATORS(Config2);

enum Config3 : u8 {
    BeaconEnable = 1 << 0,
    ReadyToL23 = 1 << 1,
};
AK_ENUM_BITWISE_OPERATORS(Config3);

enum Config5 : u8 {
    ASPMEnable = 1 << 0,
    SPIEnable = 1 << 3,
};
AK_ENUM_BITWISE_OPERATORS(Config5);

constexpr u32 PHY_FLAG = 1u << 31;

constexpr u32 CSI_FLAG = 1u << 31;
constexpr u32 CSI_BYTE_ENABLE = 0xF000;
constexpr u32 CSI_FUNC_NIC = 0x20000;
constexpr u32 CSI_FUNC_NIC2 = 0x10000;

constexpr u32 CSI_ACCESS_1 = 0x17000000;
constexpr u32 CSI_ACCESS_2 = 0x27000000;

enum PHYStatus : u32 {
    Speed10000MF = 1 << 14,
    Speed5000MF = 1 << 12,
    Speed2500MF = 1 << 10,
    Speed1000MF = 1 << 4,
    Speed100 = 1 << 3,
    Speed10 = 1 << 2,
    LinkStatus = 1 << 1,
    FullDuplex = 1 << 0,
};
AK_ENUM_BITWISE_OPERATORS(PHYStatus);

enum GPIO : u8 {
    Enable = 0x1,
};
AK_ENUM_BITWISE_OPERATORS(GPIO);

constexpr u32 ERI_FLAG = 0x80000000;
constexpr u32 ERI_MASK_0001 = 0x1000;
constexpr u32 ERI_MASK_0011 = 0x3000;
constexpr u32 ERI_MASK_0100 = 0x4000;
constexpr u32 ERI_MASK_0101 = 0x5000;
constexpr u32 ERI_MASK_1111 = 0xF000;
constexpr u32 ERI_EXGMAC = 0x0;

constexpr u32 EPHY_FLAG = 1u << 31;

constexpr u32 OCP_FLAG = 0x80000000;
constexpr u32 OCP_STANDARD_PHY_BASE = 0xA400;

enum DLLPR : u8 {
    PFMEnable = 0x40,
    TX10MPSEnable = 0x80,
};
AK_ENUM_BITWISE_OPERATORS(DLLPR);

enum Debug : u8 {
    FixNAK1 = 1 << 3,
    FixNAK2 = 1 << 4,
};
AK_ENUM_BITWISE_OPERATORS(Debug);

enum MCU : u8 {
    LinkListReady = 1 << 1,
    RXEmpty = 1 << 4,
    TXEmpty = 1 << 5,
    NowIsOOB = 1 << 7,
};

enum CPlusCommand : u16 {
    VerifyChecksum = 1 << 5,
    VLANStrip = 1 << 6,
    PacketControlDisable = 1 << 7,
    ASF = 1 << 8,
    CXPL_DBG_SEL = 1 << 9,
    ForceTXFlowEnable = 1 << 10,
    ForceRXFlowEnable = 1 << 11,
    ForceHalfDuplex = 1 << 12,
    MacDBGOOE = 1 << 14,
    EnableBist = 1 << 15,
};
AK_ENUM_BITWISE_OPERATORS(CPlusCommand);

constexpr u8 MTPS_JUMBO = 0x3F;

enum Misc : u32 {
    RXDVGateEnable = 1 << 19,
    PWMEnable = 1 << 22,
};
AK_ENUM_BITWISE_OPERATORS(Misc);

enum Misc1 : u8 {
    PFMD3ColdEnable = 0x40,
};
AK_ENUM_BITWISE_OPERATORS(Misc1);

constexpr u8 PHY_REG_BMCR = 0x00;
constexpr u8 PHY_REG_ANAR = 0x4;
constexpr u8 PHY_REG_GBCR = 0x9;

constexpr u16 BMCR_RESET = 0x8000;
constexpr u16 BMCR_SPEED_0 = 0x2000;
constexpr u16 BMCR_AUTO_NEGOTIATE = 0x1000;
constexpr u16 BMCR_RESTART_AUTO_NEGOTIATE = 0x200;
constexpr u16 BMCR_DUPLEX = 0x100;
constexpr u16 BMCR_SPEED_1 = 0x40;

constexpr u16 ADVERTISE_10_HALF = 0x20;
constexpr u16 ADVERTISE_10_FULL = 0x40;
constexpr u16 ADVERTISE_100_HALF = 0x80;
constexpr u16 ADVERTISE_100_FULL = 0x100;
constexpr u16 ADVERTISE_PAUSE_CAP = 0x400;
constexpr u16 ADVERTISE_PAUSE_ASYM = 0x800;

constexpr u16 ADVERTISE_1000_HALF = 0x100;
constexpr u16 ADVERTISE_1000_FULL = 0x200;

}
