/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::ACPI::AML {

// 20.3 AML Byte Stream Byte Values

static constexpr u8 AliasOp = 0x06;
static constexpr u8 NameOp = 0x08;
static constexpr u8 ScopeOp = 0x10;
static constexpr u8 BufferOp = 0x11;
static constexpr u8 PackageOp = 0x12;
static constexpr u8 VarPackageOp = 0x13;
static constexpr u8 MethodOp = 0x14;
static constexpr u8 ExternalOp = 0x15;
static constexpr u8 Local0Op = 0x60;
static constexpr u8 Local1Op = 0x61;
static constexpr u8 Local2Op = 0x62;
static constexpr u8 Local3Op = 0x63;
static constexpr u8 Local4Op = 0x64;
static constexpr u8 Local5Op = 0x65;
static constexpr u8 Local6Op = 0x66;
static constexpr u8 Local7Op = 0x67;
static constexpr u8 Arg0Op = 0x68;
static constexpr u8 Arg1Op = 0x69;
static constexpr u8 Arg2Op = 0x6A;
static constexpr u8 Arg3Op = 0x6B;
static constexpr u8 Arg4Op = 0x6C;
static constexpr u8 Arg5Op = 0x6D;
static constexpr u8 Arg6Op = 0x6E;
static constexpr u8 StoreOp = 0x70;
static constexpr u8 RefOfOp = 0x71;
static constexpr u8 AddOp = 0x72;
static constexpr u8 ConcatOp = 0x73;
static constexpr u8 SubtractOp = 0x74;
static constexpr u8 IncrementOp = 0x75;
static constexpr u8 DecrementOp = 0x76;
static constexpr u8 MultiplyOp = 0x77;
static constexpr u8 DivideOp = 0x78;
static constexpr u8 ShiftLeftOp = 0x79;
static constexpr u8 ShiftRightOp = 0x7A;
static constexpr u8 AndOp = 0x7B;
static constexpr u8 NandOp = 0x7C;
static constexpr u8 OrOp = 0x7D;
static constexpr u8 NorOp = 0x7E;
static constexpr u8 XorOp = 0x7F;
static constexpr u8 NotOp = 0x80;
static constexpr u8 FindSetLeftBitOp = 0x81;
static constexpr u8 FindSetRightBitOp = 0x82;
static constexpr u8 DerefOfOp = 0x83;
static constexpr u8 ConcatResOp = 0x84;
static constexpr u8 ModOp = 0x85;
static constexpr u8 NotifyOp = 0x86;
static constexpr u8 SizeOfOp = 0x87;
static constexpr u8 IndexOp = 0x88;
static constexpr u8 MatchOp = 0x89;
static constexpr u8 CreateDWordFieldOp = 0x8A;
static constexpr u8 CreateWordFieldOp = 0x8B;
static constexpr u8 CreateByteFieldOp = 0x8C;
static constexpr u8 CreateBitFieldOp = 0x8D;
static constexpr u8 ObjectTypeOp = 0x8E;
static constexpr u8 CreateQWordFieldOp = 0x8F;
static constexpr u8 LandOp = 0x90;
static constexpr u8 LorOp = 0x91;
static constexpr u8 LnotOp = 0x92;
static constexpr u8 LequalOp = 0x93;
static constexpr u8 LgreaterOp = 0x94;
static constexpr u8 LlessOp = 0x95;
static constexpr u8 ToBufferOp = 0x96;
static constexpr u8 ToDecimalStringOp = 0x97;
static constexpr u8 ToHexStringOp = 0x98;
static constexpr u8 ToIntegerOp = 0x99;
static constexpr u8 ToStringOp = 0x9C;
static constexpr u8 CopyObjectOp = 0x9D;
static constexpr u8 MidOp = 0x9E;
static constexpr u8 ContinueOp = 0x9F;
static constexpr u8 IfOp = 0xA0;
static constexpr u8 ElseOp = 0xA1;
static constexpr u8 WhileOp = 0xA2;
static constexpr u8 NoopOp = 0xA3;
static constexpr u8 ReturnOp = 0xA4;
static constexpr u8 BreakOp = 0xA5;
static constexpr u8 BreakPointOp = 0xCC;

static constexpr u8 ExtOpPrefix = 0x5B;
static constexpr u8 MutexOp = 0x01;
static constexpr u8 EventOp = 0x02;
static constexpr u8 CondRefOfOp = 0x12;
static constexpr u8 LoadTableOp = 0x13;
static constexpr u8 LoadOp = 0x20;
static constexpr u8 StallOp = 0x21;
static constexpr u8 SleepOp = 0x22;
static constexpr u8 AcquireOp = 0x23;
static constexpr u8 SignalOp = 0x24;
static constexpr u8 WaitOp = 0x25;
static constexpr u8 ResetOp = 0x26;
static constexpr u8 ReleaseOp = 0x27;
static constexpr u8 FromBCDOp = 0x28;
static constexpr u8 ToBCDOp = 0x29;
static constexpr u8 RevisionOp = 0x30;
static constexpr u8 DebugOp = 0x31;
static constexpr u8 FatalOp = 0x32;
static constexpr u8 TimerOp = 0x33;
static constexpr u8 OpRegionOp = 0x80;
static constexpr u8 FieldOp = 0x81;
static constexpr u8 DeviceOp = 0x82;
static constexpr u8 ProcessorOp = 0x83;
static constexpr u8 PowerResOp = 0x84;
static constexpr u8 ThermalZoneOp = 0x85;
static constexpr u8 IndexFieldOp = 0x86;
static constexpr u8 BankFieldOp = 0x87;
static constexpr u8 DataRegionOp = 0x88;

static constexpr u8 RootChar = '\\';
static constexpr u8 ParentPrefixChar = '^';

static constexpr u8 DualNamePrefix = 0x2E;
static constexpr u8 MultiNamePrefix = 0x2F;
static constexpr u8 NullName = 0x00;

static constexpr u8 BytePrefix = 0x0A;
static constexpr u8 WordPrefix = 0x0B;
static constexpr u8 DWordPrefix = 0x0C;
static constexpr u8 StringPrefix = 0x0D;
static constexpr u8 QWordPrefix = 0x0E;

static constexpr u8 ZeroOp = 0x00;
static constexpr u8 OneOp = 0x01;
static constexpr u8 OnesOp = 0xFF;

}
