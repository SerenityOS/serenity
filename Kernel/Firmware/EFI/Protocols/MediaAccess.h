/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

// https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html

namespace Kernel::EFI {

// See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#efi-file-protocol-open
enum class FileOpenMode : u64 {
    Read = 0x1,
    Write = 0x2,
    Create = 0x8000000000000000,
};
AK_ENUM_BITWISE_OPERATORS(FileOpenMode)

// See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#efi-file-protocol-open
enum class FileAttribute : u64 {
    None = 0x0,
    ReadOnly = 0x1,
    Hidden = 0x2,
    System = 0x4,
    Reserved = 0x8,
    Directory = 0x10,
    Archive = 0x20,
};
AK_ENUM_BITWISE_OPERATORS(FileAttribute)

// EFI_FILE_INFO: https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#efi-file-info
struct FileInfo {
    static constexpr GUID guid = { 0x09576e92, 0x6d3f, 0x11d2, { 0x8e, 0x39, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    u64 size;
    u64 file_size;
    u64 physical_size;
    Time create_time;
    Time last_access_time;
    Time modification_time;
    FileAttribute attribute;
    char16_t file_name[];
};
static_assert(AssertSize<FileInfo, 80>());

// EFI_FILE_IO_TOKEN: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#efi-file-protocol-openex
struct FileIOToken {
    Event event;
    Status status;
    FlatPtr buffer_size;
    void* buffer;
};
static_assert(AssertSize<FileIOToken, 32>());

// EFI_FILE_PROTOCOL: https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#file-protocol
struct FileProtocol {
    using FileOpenFn = EFIAPI Status (*)(FileProtocol*, FileProtocol** new_handle, char16_t* file_name, FileOpenMode open_mode, FileAttribute attributes);
    using FileCloseFn = EFIAPI Status (*)(FileProtocol*);
    using FileDeleteFn = EFIAPI Status (*)(FileProtocol*);
    using FileReadFn = EFIAPI Status (*)(FileProtocol*, FlatPtr* buffer_size, void* buffer);
    using FileWriteFn = EFIAPI Status (*)(FileProtocol*, FlatPtr* buffer_size, void* buffer);
    using FileOpenExFn = EFIAPI Status (*)(FileProtocol*, FileProtocol** new_handle, char16_t* file_name, FileOpenMode open_mode, FileAttribute attributes, FileIOToken* token);
    using FileReadExFn = EFIAPI Status (*)(FileProtocol*, FileIOToken* token);
    using FileWriteExFn = EFIAPI Status (*)(FileProtocol*, FileIOToken* token);
    using FileFlushExFn = EFIAPI Status (*)(FileProtocol*, FileIOToken* token);
    using FileSetPositionFn = EFIAPI Status (*)(FileProtocol*, u64 position);
    using FileGetPositionFn = EFIAPI Status (*)(FileProtocol*, u64* position);
    using FileGetInfoFn = EFIAPI Status (*)(FileProtocol*, GUID* information_type, FlatPtr* buffer_size, void* buffer);
    using FileSetInfoFn = EFIAPI Status (*)(FileProtocol*, GUID* information_type, FlatPtr buffer_size, void* buffer);
    using FileFlushFn = EFIAPI Status (*)(FileProtocol*);

    u64 revision;
    FileOpenFn open;
    FileCloseFn close;
    FileDeleteFn delete_;
    FileReadFn read;
    FileWriteFn write;
    FileGetPositionFn get_position;
    FileSetPositionFn set_position;
    FileGetInfoFn get_info;
    FileSetInfoFn set_info;
    FileFlushFn flush;

    // Revision 2+

    FileOpenExFn open_ex;
    FileReadExFn read_ex;
    FileWriteExFn write_ex;
    FileFlushExFn flush_ex;
};
static_assert(AssertSize<FileProtocol, 120>());

// EFI_SIMPLE_FILE_SYSTEM_PROTOCOL: https://uefi.org/specs/UEFI/2.10/13_Protocols_Media_Access.html#simple-file-system-protocol
struct SimpleFileSystemProtocol {
    static constexpr GUID guid = { 0x0964e5b22, 0x6459, 0x11d2, { 0x8e, 0x39, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    using SimpleFileSystemProtocolOpenVolumeFn = EFIAPI Status (*)(SimpleFileSystemProtocol*, FileProtocol** root);

    u64 revision;
    SimpleFileSystemProtocolOpenVolumeFn open_volume;
};
static_assert(AssertSize<SimpleFileSystemProtocol, 16>());

}
