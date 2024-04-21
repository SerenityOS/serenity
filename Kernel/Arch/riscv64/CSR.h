/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

// Documentation for the CSRs:
// RISC-V ISA Manual, Volume II (https://github.com/riscv/riscv-isa-manual/releases/download/Priv-v1.12/riscv-privileged-20211203.pdf)

namespace Kernel::RISCV64::CSR {

// 2.2 CSR Listing
enum class Address : u16 {
    // Supervisor Trap Setup
    SSTATUS = 0x100,
    SIE = 0x104,
    STVEC = 0x105,

    // Supervisor Protection and Translation
    SATP = 0x180,

    // Unprivileged Counters/Timers
    CYCLE = 0xc00,
    TIME = 0xc01,
};

ALWAYS_INLINE FlatPtr read(Address address)
{
    FlatPtr ret;
    asm volatile("csrr %0, %1"
                 : "=r"(ret)
                 : "i"(address));
    return ret;
}

ALWAYS_INLINE void write(Address address, FlatPtr value)
{
    asm volatile("csrw %0, %1" ::"i"(address), "Kr"(value));
}

ALWAYS_INLINE FlatPtr read_and_set_bits(Address address, FlatPtr bit_mask)
{
    FlatPtr ret;
    asm volatile("csrrs %0, %1, %2"
                 : "=r"(ret)
                 : "i"(address), "Kr"(bit_mask));
    return ret;
}

ALWAYS_INLINE void set_bits(Address address, FlatPtr bit_mask)
{
    asm volatile("csrs %0, %1" ::"i"(address), "Kr"(bit_mask));
}

ALWAYS_INLINE void clear_bits(Address address, FlatPtr bit_mask)
{
    asm volatile("csrc %0, %1" ::"i"(address), "Kr"(bit_mask));
}

// 4.1.11 Supervisor Address Translation and Protection (satp) Register
struct [[gnu::packed]] alignas(u64) SATP {
    enum class Mode : u64 {
        Bare = 0,
        Sv39 = 8,
        Sv48 = 9,
        Sv57 = 10,
    };

    // Physical page number of root page table
    u64 PPN : 44;

    // Address space identifier
    u64 ASID : 16;

    // Current address-translation scheme
    Mode MODE : 4;

    static ALWAYS_INLINE void write(SATP satp)
    {
        CSR::write(CSR::Address::SATP, bit_cast<FlatPtr>(satp));
    }

    static ALWAYS_INLINE SATP read()
    {
        return bit_cast<SATP>(CSR::read(CSR::Address::SATP));
    }

    bool operator==(SATP const& other) const
    {
        return bit_cast<u64>(*this) == bit_cast<u64>(other);
    }
};
static_assert(AssertSize<SATP, 8>());

// 4.1.1 Supervisor Status Register (sstatus)
struct [[gnu::packed]] alignas(u64) SSTATUS {
    // Useful for CSR::{set,clear}_bits
    enum class Offset {
        SIE = 1,
        SPIE = 5,
        UBE = 6,
        SPP = 8,
        VS = 9,
        FS = 13,
        XS = 15,
        SUM = 18,
        MXR = 19,
        UXL = 32,
        SD = 63,
    };

    enum class PrivilegeMode : u64 {
        User = 0,
        Supervisor = 1,
    };

    enum class FloatingPointStatus : u64 {
        Off = 0,
        Initial = 1,
        Clean = 2,
        Dirty = 3,
    };

    enum class VectorStatus : u64 {
        Off = 0,
        Initial = 1,
        Clean = 2,
        Dirty = 3,
    };

    enum class UserModeExtensionsStatus : u64 {
        AllOff = 0,
        NoneDirtyOrClean_SomeOn = 1,
        NoneDirty_SomeOn = 2,
        SomeDirty = 3,
    };

    enum class XLEN : u64 {
        Bits32 = 1,
        Bits64 = 2,
        Bits128 = 3,
    };

    u64 _reserved0 : 1;

    // Enables or disables all interrupts in supervisor mode
    u64 SIE : 1;

    u64 _reserved2 : 3;

    // Indicates whether supervisor interrupts were enabled prior to trapping into supervisor mode
    // When a trap is taken into supervisor mode, SPIE is set to SIE, and SIE is set to 0. When
    // an SRET instruction is executed, SIE is set to SPIE, then SPIE is set to 1.
    u64 SPIE : 1;

    // Controls the endianness of explicit memory accesses made from
    // U-mode, which may differ from the endianness of memory accesses in S-mode
    u64 UBE : 1;

    u64 _reserved7 : 1;

    // Indicates the privilege level at which a hart was executing before entering supervisor mode
    PrivilegeMode SPP : 1;

    // Encodes the status of the vector extension state, including the vector registers v0–v31 and
    // the CSRs vcsr, vxrm, vxsat, vstart, vl, vtype, and vlenb.
    VectorStatus VS : 2;

    u64 _reserved11 : 2;

    // Encodes the status of the floating-point unit state,
    // including the floating-point registers f0–f31 and the CSRs fcsr, frm, and fflags.
    FloatingPointStatus FS : 2;

    // The XS field encodes the status of additional user-mode extensions and associated state.
    UserModeExtensionsStatus XS : 2;

    u64 _reserved17 : 1;

    // The SUM (permit Supervisor User Memory access) bit modifies the privilege with which S-mode
    // loads and stores access virtual memory. When SUM=0, S-mode memory accesses to pages that are
    // accessible by U-mode (U=1 in Figure 5.18) will fault. When SUM=1, these accesses are permitted.
    // SUM has no effect when page-based virtual memory is not in effect, nor when executing in U-mode.
    // Note that S-mode can never execute instructions from user pages, regardless of the state of SUM.
    u64 SUM : 1;

    // The MXR (Make eXecutable Readable) bit modifies the privilege with which loads access virtual
    // memory. When MXR=0, only loads from pages marked readable (R=1 in Figure 5.18) will succeed.
    // When MXR=1, loads from pages marked either readable or executable (R=1 or X=1) will succeed.
    // MXR has no effect when page-based virtual memory is not in effect.
    u64 MXR : 1;

    u64 _reserved20 : 12;

    // Controls the value of XLEN for U-mode
    XLEN UXL : 2;

    u64 _reserved34 : 29;

    // The SD bit is a read-only bit that summarizes whether either the FS, VS, or XS fields signal the
    // presence of some dirty state that will require saving extended user context to memory.
    u64 SD : 1;

    static ALWAYS_INLINE void write(SSTATUS sstatus)
    {
        CSR::write(CSR::Address::SSTATUS, bit_cast<FlatPtr>(sstatus));
    }

    static ALWAYS_INLINE SSTATUS read()
    {
        return bit_cast<SSTATUS>(CSR::read(CSR::Address::SSTATUS));
    }
};
static_assert(AssertSize<SSTATUS, 8>());

// 4.1.8 Supervisor Cause Register (scause)
constexpr u64 SCAUSE_INTERRUPT_MASK = 1LU << 63;

enum class SCAUSE : u64 {
    // Interrupts
    SupervisorSoftwareInterrupt = SCAUSE_INTERRUPT_MASK | 1,
    SupervisorTimerInterrupt = SCAUSE_INTERRUPT_MASK | 5,
    SupervisorExternalInterrupt = SCAUSE_INTERRUPT_MASK | 9,

    // Exceptions
    InstructionAddressMisaligned = 0,
    InstructionAccessFault = 1,
    IllegalInstrction = 2,
    Breakpoint = 3,
    LoadAddressMisaligned = 4,
    LoadAccessFault = 5,
    StoreOrAMOAddressMisaligned = 6,
    StoreOrAMOAccessFault = 7,
    EnvironmentCallFromUMode = 8,
    EnvironmentCallFromSMode = 9,

    InstructionPageFault = 12,
    LoadPageFault = 13,

    StoreOrAMOPageFault = 15,
};

}

template<>
struct AK::Formatter<Kernel::RISCV64::CSR::SSTATUS> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::RISCV64::CSR::SSTATUS value)
    {
        if (value.SD)
            TRY(builder.put_literal("SD "sv));

        switch (value.UXL) {
        case Kernel::RISCV64::CSR::SSTATUS::XLEN::Bits32:
            TRY(builder.put_literal("UXL=32 "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::XLEN::Bits64:
            TRY(builder.put_literal("UXL=64 "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::XLEN::Bits128:
            TRY(builder.put_literal("UXL=128 "sv));
            break;
        }

        if (value.MXR)
            TRY(builder.put_literal("MXR "sv));

        if (value.SUM)
            TRY(builder.put_literal("SUM "sv));

        switch (value.XS) {
        case Kernel::RISCV64::CSR::SSTATUS::UserModeExtensionsStatus::AllOff:
            TRY(builder.put_literal("XS=AllOff "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::UserModeExtensionsStatus::NoneDirtyOrClean_SomeOn:
            TRY(builder.put_literal("XS=NoneDirtyOrClean_SomeOn "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::UserModeExtensionsStatus::NoneDirty_SomeOn:
            TRY(builder.put_literal("XS=NoneDirty_SomeOn "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::UserModeExtensionsStatus::SomeDirty:
            TRY(builder.put_literal("XS=SomeDirty "sv));
            break;
        }

        switch (value.FS) {
        case Kernel::RISCV64::CSR::SSTATUS::FloatingPointStatus::Off:
            TRY(builder.put_literal("FS=Off "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::FloatingPointStatus::Initial:
            TRY(builder.put_literal("FS=Initial "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::FloatingPointStatus::Clean:
            TRY(builder.put_literal("FS=Clean "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::FloatingPointStatus::Dirty:
            TRY(builder.put_literal("FS=Dirty "sv));
            break;
        }

        switch (value.VS) {
        case Kernel::RISCV64::CSR::SSTATUS::VectorStatus::Off:
            TRY(builder.put_literal("VS=Off "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::VectorStatus::Initial:
            TRY(builder.put_literal("VS=Initial "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::VectorStatus::Clean:
            TRY(builder.put_literal("VS=Clean "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::VectorStatus::Dirty:
            TRY(builder.put_literal("VS=Dirty "sv));
            break;
        }

        switch (value.SPP) {
        case Kernel::RISCV64::CSR::SSTATUS::PrivilegeMode::User:
            TRY(builder.put_literal("SPP=User "sv));
            break;
        case Kernel::RISCV64::CSR::SSTATUS::PrivilegeMode::Supervisor:
            TRY(builder.put_literal("SPP=Supervisor "sv));
            break;
        }

        if (value.UBE)
            TRY(builder.put_literal("UBE "sv));

        if (value.SPIE)
            TRY(builder.put_literal("SPIE "sv));

        if (value.SIE)
            TRY(builder.put_literal("SIE "sv));

        TRY(builder.put_literal("("sv));
        TRY(builder.put_u64(bit_cast<u64>(value), 16, true, false, true, false, FormatBuilder::Align::Right, 16));
        TRY(builder.put_literal(")"sv));

        return {};
    }
};

template<>
struct AK::Formatter<Kernel::RISCV64::CSR::SCAUSE> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::RISCV64::CSR::SCAUSE value)
    {
        switch (value) {
        case Kernel::RISCV64::CSR::SCAUSE::SupervisorSoftwareInterrupt:
            return builder.put_string("Supervisor software interrupt"sv);
        case Kernel::RISCV64::CSR::SCAUSE::SupervisorTimerInterrupt:
            return builder.put_string("Supervisor timer interrupt"sv);
        case Kernel::RISCV64::CSR::SCAUSE::SupervisorExternalInterrupt:
            return builder.put_string("Supervisor external interrupt"sv);

        case Kernel::RISCV64::CSR::SCAUSE::InstructionAddressMisaligned:
            return builder.put_string("Instruction address misaligned"sv);
        case Kernel::RISCV64::CSR::SCAUSE::InstructionAccessFault:
            return builder.put_string("Instruction access fault"sv);
        case Kernel::RISCV64::CSR::SCAUSE::IllegalInstrction:
            return builder.put_string("Illegal instruction"sv);
        case Kernel::RISCV64::CSR::SCAUSE::Breakpoint:
            return builder.put_string("Breakpoint"sv);
        case Kernel::RISCV64::CSR::SCAUSE::LoadAddressMisaligned:
            return builder.put_string("Load address misaligned"sv);
        case Kernel::RISCV64::CSR::SCAUSE::LoadAccessFault:
            return builder.put_string("Load access fault"sv);
        case Kernel::RISCV64::CSR::SCAUSE::StoreOrAMOAddressMisaligned:
            return builder.put_string("Store/AMO address misaligned"sv);
        case Kernel::RISCV64::CSR::SCAUSE::StoreOrAMOAccessFault:
            return builder.put_string("Store/AMO access fault"sv);
        case Kernel::RISCV64::CSR::SCAUSE::EnvironmentCallFromUMode:
            return builder.put_string("Environment call from U-mode"sv);
        case Kernel::RISCV64::CSR::SCAUSE::EnvironmentCallFromSMode:
            return builder.put_string("Environment call from S-mode"sv);
        case Kernel::RISCV64::CSR::SCAUSE::InstructionPageFault:
            return builder.put_string("Instruction page fault"sv);
        case Kernel::RISCV64::CSR::SCAUSE::LoadPageFault:
            return builder.put_string("Load page fault"sv);
        case Kernel::RISCV64::CSR::SCAUSE::StoreOrAMOPageFault:
            return builder.put_string("Store/AMO page fault"sv);
        default:
            VERIFY_NOT_REACHED();
        }
    }
};
