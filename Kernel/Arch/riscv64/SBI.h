/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/StdLibExtras.h>

// Documentation about the SBI:
// RISC-V Supervisor Binary Interface Specification (https://github.com/riscv-non-isa/riscv-sbi-doc)

namespace Kernel::SBI {

// Chapter 3. Binary Encoding
enum class SBIError : long {
    // SBI_SUCCESS: Completed successfully
    Success = 0,
    // SBI_ERR_FAILED: Failed
    Failed = -1,
    // SBI_ERR_NOT_SUPPORTED: Not supported
    NotSupported = -2,
    // SBI_ERR_INVALID_PARAM: Invalid parameter(s)
    InvalidParam = -3,
    // SBI_ERR_DENIED: Denied or not allowed
    Denied = -4,
    // SBI_ERR_INVALID_ADDRESS: Invalid address(s)
    InvalidAddress = -5,
    // SBI_ERR_ALREADY_AVAILABLE: Already available
    AlreadyAvailable = -6,
    // SBI_ERR_ALREADY_STARTED: Already started
    AlreadyStarted = -7,
    // SBI_ERR_ALREADY_STOPPED: Already stopped
    AlreadyStopped = -8,
    // SBI_ERR_NO_SHMEM: Shared memory not available
    NoSHMEM = -9,
};

template<typename T>
using SBIErrorOr = ErrorOr<T, SBIError>;

enum class EID : i32 {
    // Base Extension
    Base = 0x10,
    // Debug Console Extension ("DBCN")
    DebugConsole = 0x4442434E,
    // System Reset Extension ("SRST")
    SystemReset = 0x53525354,
    // Timer Extension ("TIME")
    Timer = 0x54494D45,
};

// Chapter 4. Base Extension (EID #0x10)
// Required extension since SBI v0.2
namespace Base {

enum class FID : i32 {
    GetSpecVersion = 0,
    GetImplID = 1,
    GetImplVersion = 2,
    ProbeExtension = 3,
    GetMVENDORID = 4,
    GetMARCHID = 5,
    GetMIMPID = 6,
};

struct SpecificationVersion {
    u32 minor : 24;
    u32 major : 7;
    u32 reserved : 1;
};
static_assert(AssertSize<SpecificationVersion, 4>());

// Get SBI specification version (FID #0)
// Returns the current SBI specification version. This function must always succeed.
// The minor number of the SBI specification is encoded in the low 24 bits,
// with the major number encoded in the next 7 bits. Bit 31 must be 0 and is reserved for future expansion.
SBIErrorOr<SpecificationVersion> get_spec_version();

// Get SBI implementation ID (FID #1)
// Returns the current SBI implementation ID, which is different for every SBI implementation. It is
// intended that this implementation ID allows software to probe for SBI implementation quirks.
SBIErrorOr<long> get_impl_id();

// Get SBI implementation version (FID #2)
// Returns the current SBI implementation version. The encoding of this version number is specific to
// the SBI implementation.
SBIErrorOr<long> get_impl_version();

// Probe SBI extension (FID #3)
// Returns 0 if the given SBI extension ID (EID) is not available, or 1 if it is available unless defined as
// any other non-zero value by the implementation.
SBIErrorOr<long> probe_extension(EID extension_id);

// Get machine vendor ID (FID #4)
// Return a value that is legal for the mvendorid CSR and 0 is always a legal value for this CSR.
SBIErrorOr<long> get_mvendorid();

// Get machine architecture ID (FID #5)
// Return a value that is legal for the marchid CSR and 0 is always a legal value for this CSR.
SBIErrorOr<long> get_marchid();

// Get machine implementation ID (FID #6)
// Return a value that is legal for the mimpid CSR and 0 is always a legal value for this CSR.
SBIErrorOr<long> get_mimpid();

}

// Chapter 5. Legacy Extensions (EIDs #0x00 - #0x0F)
namespace Legacy {

enum class LegacyEID : i32 {
    SetTimer = 0,
    ConsolePutchar = 1,
    ConsoleGetchar = 2,
    ClearIPI = 3,
    SendIPI = 4,
    RemoteFENCEI = 5,
    RemoteSFENCEVMA = 6,
    RemoteSFENCEVMAWithASID = 7,
    SystemShutdown = 8,
};

template<typename T>
using LegacySBIErrorOr = ErrorOr<T, long>;

// Set Timer (EID #0x00)
// Programs the clock for next event after stime_value time. This function also clears the pending
// timer interrupt bit.
LegacySBIErrorOr<void> set_timer(u64 stime_value);

// Console Putchar (EID #0x01)
// Write data present in ch to debug console.
LegacySBIErrorOr<void> console_putchar(int ch);

// System Shutdown (EID #0x08)
// Puts all the harts to shutdown state from supervisor point of view.
// This SBI call doesn’t return irrespective whether it succeeds or fails.
[[noreturn]] void shutdown();

}

// Chapter 6. Timer Extension (EID #0x54494D45 "TIME")
// Since SBI v0.2
namespace Timer {

enum class FID : i32 {
    SetTimer = 0,
};

// Set Timer (FID #0)
// Programs the clock for next event after stime_value time. stime_value is in absolute time. This
// function must clear the pending timer interrupt bit as well.
SBIErrorOr<void> set_timer(u64 stime_value);

}

// Chapter 10. System Reset Extension (EID #0x53525354 "SRST")
// Since SBI v0.2
namespace SystemReset {

enum class FID : i32 {
    SystemReset = 0,
};

enum class ResetType : u32 {
    Shutdown = 0x0,
    ColdReboot = 0x1,
    WarmReboot = 0x2,
};

enum class ResetReason : u32 {
    NoReason = 0x0,
    SystemFailure = 0x1,
};

// System reset (FID #0)
// Reset the system based on provided reset_type and reset_reason. This is a synchronous call and
// does not return if it succeeds.
SBIError system_reset(ResetType reset_type, ResetReason reset_reason);

}

// Chapter 12. Debug Console Extension (EID #0x4442434E "DBCN")
// Since SBI v2.0
namespace DBCN {

enum class FID : i32 {
    DebugConsoleWrite = 0,
    DebugConsoleRead = 1,
    DebugConsoleWriteByte = 2,
};

// Console Write Byte (FID #2)
// Write a single byte to the debug console.
SBIErrorOr<void> debug_console_write_byte(u8 byte);

}

void initialize();

}

template<>
struct AK::Formatter<Kernel::SBI::SBIError> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::SBI::SBIError error)
    {
        auto string = "Unknown error"sv;

        using enum Kernel::SBI::SBIError;
        switch (error) {
        case Success:
            string = "Completed successfully"sv;
            break;
        case Failed:
            string = "Failed"sv;
            break;
        case NotSupported:
            string = "Not supported"sv;
            break;
        case InvalidParam:
            string = "Invalid parameter(s)"sv;
            break;
        case Denied:
            string = "Denied or not allowed"sv;
            break;
        case InvalidAddress:
            string = "Invalid address(s)"sv;
            break;
        case AlreadyAvailable:
            string = "Already available"sv;
            break;
        case AlreadyStarted:
            string = "Already started"sv;
            break;
        case AlreadyStopped:
            string = "Already stopped"sv;
            break;
        case NoSHMEM:
            string = "Shared memory not available"sv;
            break;
        }

        return builder.put_string(string);
    }
};

template<>
struct AK::Formatter<Kernel::SBI::Base::SpecificationVersion> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::SBI::Base::SpecificationVersion const& version)
    {
        VERIFY(version.reserved == 0);
        return Formatter<FormatString>::format(builder, "{}.{}"sv, version.major, version.minor);
    }
};
