/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

#include <Kernel/Arch/riscv64/SBI.h>

namespace Kernel::SBI {

static bool s_sbi_is_legacy = false;

static SBIErrorOr<long> sbi_ecall0(EID extension_id, u32 function_id)
{
    register unsigned long a0 asm("a0");
    register unsigned long a1 asm("a1");
    register unsigned long a6 asm("a6") = function_id;
    register unsigned long a7 asm("a7") = to_underlying(extension_id);
    asm volatile("ecall"
                 : "=r"(a0), "=r"(a1)
                 : "r"(a6), "r"(a7)
                 : "memory");
    if (a0 == to_underlying(SBIError::Success))
        return static_cast<long>(a1);

    return static_cast<SBIError>(a0);
}

static SBIErrorOr<long> sbi_ecall1(EID extension_id, u32 function_id, unsigned long arg0)
{
    register unsigned long a0 asm("a0") = arg0;
    register unsigned long a1 asm("a1");
    register unsigned long a6 asm("a6") = function_id;
    register unsigned long a7 asm("a7") = to_underlying(extension_id);
    asm volatile("ecall"
                 : "+r"(a0), "=r"(a1)
                 : "r"(a0), "r"(a6), "r"(a7)
                 : "memory");
    if (a0 == to_underlying(SBIError::Success))
        return static_cast<long>(a1);

    return static_cast<SBIError>(a0);
}

static SBIErrorOr<long> sbi_ecall2(EID extension_id, u32 function_id, unsigned long arg0, unsigned long arg1)
{
    register unsigned long a0 asm("a0") = arg0;
    register unsigned long a1 asm("a1") = arg1;
    register unsigned long a6 asm("a6") = function_id;
    register unsigned long a7 asm("a7") = to_underlying(extension_id);
    asm volatile("ecall"
                 : "+r"(a0), "+r"(a1)
                 : "r"(a0), "r"(a1), "r"(a6), "r"(a7)
                 : "memory");
    if (a0 == to_underlying(SBIError::Success))
        return static_cast<long>(a1);

    return static_cast<SBIError>(a0);
}

namespace Base {

SBIErrorOr<SpecificationVersion> get_spec_version()
{
    auto version = TRY(SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetSpecVersion)));
    return bit_cast<SpecificationVersion>(static_cast<u32>(version));
}

SBIErrorOr<long> get_impl_id()
{
    return SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetImplID));
}

SBIErrorOr<long> get_impl_version()
{
    return SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetImplVersion));
}

SBIErrorOr<long> probe_extension(EID extension_id)
{
    return SBI::sbi_ecall1(EID::Base, to_underlying(FID::ProbeExtension), to_underlying(extension_id));
}

SBIErrorOr<long> get_mvendorid()
{
    return SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetMVENDORID));
}

SBIErrorOr<long> get_marchid()
{
    return SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetMARCHID));
}

SBIErrorOr<long> get_mimpid()
{
    return SBI::sbi_ecall0(EID::Base, to_underlying(FID::GetMIMPID));
}

}

namespace Legacy {

static long sbi_legacy_ecall0(LegacyEID extension_id)
{
    register unsigned long a0 asm("a0");
    register unsigned long a7 asm("a7") = to_underlying(extension_id);
    asm volatile("ecall"
                 : "=r"(a0)
                 : "r"(a7)
                 : "memory");
    return static_cast<long>(a0);
}

static long sbi_legacy_ecall1(LegacyEID extension_id, unsigned long arg0)
{
    register unsigned long a0 asm("a0") = arg0;
    register unsigned long a7 asm("a7") = to_underlying(extension_id);
    asm volatile("ecall"
                 : "+r"(a0)
                 : "r"(a0), "r"(a7)
                 : "memory");
    return static_cast<long>(a0);
}

LegacySBIErrorOr<void> set_timer(u64 stime_value)
{
    auto err = sbi_legacy_ecall1(LegacyEID::SetTimer, stime_value);
    if (err == 0)
        return {};

    return err;
}

LegacySBIErrorOr<void> console_putchar(int ch)
{
    auto err = sbi_legacy_ecall1(LegacyEID::ConsolePutchar, ch);
    if (err == 0)
        return {};

    return err;
}

void shutdown()
{
    sbi_legacy_ecall0(LegacyEID::SystemShutdown);

    VERIFY_NOT_REACHED();
}

}

namespace Timer {

SBIErrorOr<void> set_timer(u64 stime_value)
{
    TRY(SBI::sbi_ecall1(EID::Timer, to_underlying(FID::SetTimer), stime_value));
    return {};
}

}

namespace SystemReset {

SBIError system_reset(ResetType reset_type, ResetReason reset_reason)
{
    auto const res = SBI::sbi_ecall2(EID::SystemReset, to_underlying(FID::SystemReset), to_underlying(reset_type), to_underlying(reset_reason));

    // This SBI call shold only return if it didn't succeed
    VERIFY(res.is_error());

    return res.error();
}

}

namespace DBCN {

SBIErrorOr<void> debug_console_write_byte(u8 byte)
{
    TRY(SBI::sbi_ecall1(EID::DebugConsole, to_underlying(FID::DebugConsoleWriteByte), byte));
    return {};
}

}

void initialize()
{
    auto spec_version = Base::get_spec_version();
    if (spec_version.is_error()) {
        s_sbi_is_legacy = true;
        dbgln("SBI: Specification version: 0.1");
    } else {
        dbgln("SBI: Specification version: {}", spec_version.value());
        dbgln("SBI: Implementation ID: {}", MUST(Base::get_impl_id()));
        dbgln("SBI: Implementation version: {:#x}", MUST(Base::get_impl_version()));
        dbgln("SBI: mvendorid: {:#x}", MUST(Base::get_mvendorid()));
        dbgln("SBI: marchid: {:#x}", MUST(Base::get_marchid()));
        dbgln("SBI: mimpid: {:#x}", MUST(Base::get_mimpid()));
    }
}

}
