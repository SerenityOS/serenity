/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

// UEFI uses the Microsoft calling convention on x86.
#if ARCH(X86_64)
#    define EFIAPI __attribute__((ms_abi))
#else
#    define EFIAPI
#endif

namespace Kernel::EFI {

// The UEFI spec requires 4K pages to be used for all its current architectures.
// All function arguments and struct members that refer to "pages" also assume 4K pages.
// e.g. the AllocatePages() "Pages" argument: https://uefi.org/specs/UEFI/2.10/07_Services_Boot_Services.html#efi-boot-services-allocatepages
static constexpr size_t EFI_PAGE_SIZE = 4 * KiB;

// EFI Data Types: https://uefi.org/specs/UEFI/2.10/02_Overview.html#data-types

// BOOLEAN, Logical Boolean. 1-byte value containing a 0 for FALSE or a 1 for TRUE. Other values are undefined.
enum class Boolean : u8 {
    False = 0,
    True = 1,
};

// EFI_GUID, 128-bit buffer containing a unique identifier value. Unless otherwise specified, aligned on a 64-bit boundary.
struct alignas(u64) GUID {
    u32 part1;
    u16 part2;
    u16 part3;
    u8 part4[8];

    bool operator==(GUID const&) const = default;
};
static_assert(AssertSize<GUID, 16>());

// EFI_STATUS, Status code
// Standard status codes are defined here: https://uefi.org/specs/UEFI/2.10/Apx_D_Status_Codes.html
static constexpr FlatPtr ERROR_MASK = 1ull << (NumericLimits<FlatPtr>::digits() - 1);
enum class Status : FlatPtr {
    Success = 0,

    LoadError = 1 | ERROR_MASK,
    InvalidParameter = 2 | ERROR_MASK,
    Unsupported = 3 | ERROR_MASK,
    BadBufferSize = 4 | ERROR_MASK,
    BufferTooSmall = 5 | ERROR_MASK,
    NotReady = 6 | ERROR_MASK,
    DeviceError = 7 | ERROR_MASK,
    WriteProtected = 8 | ERROR_MASK,
    OutOfResources = 9 | ERROR_MASK,
    VolumeCorrupted = 10 | ERROR_MASK,
    VolumeFull = 11 | ERROR_MASK,
    NoMedia = 12 | ERROR_MASK,
    MediaChanged = 13 | ERROR_MASK,
    NotFound = 14 | ERROR_MASK,
    AccessDenied = 15 | ERROR_MASK,
    NoResponse = 16 | ERROR_MASK,
    NoMapping = 17 | ERROR_MASK,
    Timeout = 18 | ERROR_MASK,
    NotStarted = 19 | ERROR_MASK,
    AlreadyStarted = 20 | ERROR_MASK,
    Aborted = 21 | ERROR_MASK,
    ICMPError = 22 | ERROR_MASK,
    TFTPError = 23 | ERROR_MASK,
    ProtocolError = 24 | ERROR_MASK,
    IncompatibleVersion = 25 | ERROR_MASK,
    SecurityViolation = 26 | ERROR_MASK,
    CRCError = 27 | ERROR_MASK,
    EndOfMedia = 28 | ERROR_MASK,

    EndOfFile = 31 | ERROR_MASK,
    InvalidLanguage = 32 | ERROR_MASK,
    CompromisedData = 33 | ERROR_MASK,
    IPAddressConflict = 34 | ERROR_MASK,
    HTTPError = 35 | ERROR_MASK,
};

Optional<StringView> status_description(Status);

// EFI_HANDLE, A collection of related interfaces
using Handle = FlatPtr;

// EFI_EVENT, Handle to an event structure
using Event = void*;

// EFI_TPL, Task priority level
using TPL = FlatPtr;

// EFI_TABLE_HEADER, Data structure that precedes all of the standard EFI table types.
// https://uefi.org/specs/UEFI/2.10/04_EFI_System_Table.html#id4
struct TableHeader {
    u64 signature;
    u32 revision;
    u32 header_size;
    u32 crc32;
    u32 reserved;
};
static_assert(AssertSize<TableHeader, 24>());

// EFI_TIME: https://uefi.org/specs/UEFI/2.10/08_Services_Runtime_Services.html#gettime
struct Time {
    u16 year;  // 1900 - 9999
    u8 month;  // 1 - 12
    u8 day;    // 1 - 31
    u8 hour;   // 0 - 23
    u8 minute; // 0 - 59
    u8 second; // 0 - 59
    u8 pad1;
    u32 nanosecond; // 0 - 999,999,999
    i16 time_zone;  // —1440 to 1440 or 2047
    u8 daylight;
    u8 pad2;
};
static_assert(AssertSize<Time, 16>());

// EFI_PHYSICAL_ADDRESS: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/07_Services_Boot_Services.html#efi-boot-services-allocatepages
using PhysicalAddress = u64;

// EFI_VIRTUAL_ADDRESS: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/07_Services_Boot_Services.html#efi-boot-services-getmemorymap
using VirtualAddress = u64;

}

template<>
struct AK::Formatter<Kernel::EFI::Status> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::EFI::Status status)
    {
        if (auto maybe_description = Kernel::EFI::status_description(status); maybe_description.has_value())
            return builder.put_string(maybe_description.release_value());

        TRY(builder.put_literal("(EFI::Status)"sv));
        return builder.put_u64(to_underlying(status), 16, true);
    }
};
