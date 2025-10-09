/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <Kernel/EFIPrekernel/Filesystem.h>
#include <Kernel/EFIPrekernel/Globals.h>

namespace Kernel {

EFIErrorOr<EFI::FileProtocol*> open_root_directory(EFI::LoadedImageProtocol* loaded_image_protocol)
{
    // EFI_LOADED_IMAGE_PROTOCOL.DeviceHandle is the device handle where we were loaded from.
    auto simple_file_system_protocol_guid = EFI::SimpleFileSystemProtocol::guid;
    EFI::SimpleFileSystemProtocol* root_fs;
    if (auto status = g_efi_system_table->boot_services->handle_protocol(loaded_image_protocol->device_handle, &simple_file_system_protocol_guid, reinterpret_cast<void**>(&root_fs)); status != EFI::Status::Success)
        return status;

    EFI::FileProtocol* root_dir;
    if (auto status = root_fs->open_volume(root_fs, &root_dir); status != EFI::Status::Success)
        return status;

    return root_dir;
}

EFIErrorOr<EFI::FileProtocol*> open_file(EFI::FileProtocol* base_directory, char16_t const* path, EFI::FileOpenMode open_mode, EFI::FileAttribute attributes)
{
    EFI::FileProtocol* file = nullptr;
    if (auto status = base_directory->open(base_directory, &file, const_cast<char16_t*>(path), open_mode, attributes); status != EFI::Status::Success)
        return status;

    return file;
}

EFIErrorOr<void> close_file(EFI::FileProtocol* file)
{
    if (auto status = file->close(file); status != EFI::Status::Success)
        return status;
    return {};
}

EFIErrorOr<ByteBuffer> read_entire_file(EFI::FileProtocol* file)
{
    auto file_info_guid = EFI::FileInfo::guid;

    // The file size can be retrieved via EFI_FILE_PROTOCOL.GetInfo().
    // To get the EFI_FILE_INFO, we first need to determine the size of it and then allocate a buffer of that size.
    FlatPtr file_info_size = 0;
    if (auto status = file->get_info(file, &file_info_guid, &file_info_size, nullptr); status != EFI::Status::BufferTooSmall)
        return status;

    ByteBuffer file_info_buffer;
    if (file_info_buffer.try_resize(file_info_size).is_error())
        return EFI::Status::OutOfResources;

    // Get the EFI_FILE_INFO for this file.
    if (auto status = file->get_info(file, &file_info_guid, &file_info_size, file_info_buffer.data()); status != EFI::Status::Success)
        return status;

    auto const* file_info = reinterpret_cast<EFI::FileInfo const*>(file_info_buffer.data());

    ByteBuffer file_data;
    if (file_data.try_resize(file_info->file_size).is_error())
        return EFI::Status::OutOfResources;

    // Read the entire file.
    FlatPtr file_size = file_info->file_size;
    if (auto status = file->read(file, &file_size, file_data.data()); status != EFI::Status::Success)
        return status;

    VERIFY(file_size == file_info->file_size);
    return file_data;
}

}
