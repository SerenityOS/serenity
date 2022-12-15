/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/FileSystem/MountSpecificFlags.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/unistd.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Process.h>
#include <Kernel/StdLib.h>
#include <LibC/sys/ioctl_numbers.h>

#include <Kernel/FileSystem/DevPtsFS/FileSystem.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/FileSystem/SysFS/FileSystem.h>
#include <Kernel/FileSystem/TmpFS/FileSystem.h>

namespace Kernel {

static ErrorOr<void> handle_mount_boolean_flag_as_invalid(Span<u8>, StringView)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_unsigned_integer_flag_as_invalid(Span<u8>, StringView, u64)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_signed_integer_flag_as_invalid(Span<u8>, StringView, i64)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_ascii_string_flag_as_invalid(Span<u8>, StringView, StringView)
{
    return EINVAL;
}

static constexpr MountFile::FileSystemInitializer s_initializers[] = {
    { "proc"sv, "ProcFS"sv, false, false, false, {}, ProcFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "devpts"sv, "DevPtsFS"sv, false, false, false, {}, DevPtsFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "sys"sv, "SysFS"sv, false, false, false, {}, SysFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "tmp"sv, "TmpFS"sv, false, false, false, {}, TmpFS::try_create, handle_mount_boolean_flag_as_invalid, TmpFS::handle_mount_unsigned_integer_flag, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "ext2"sv, "Ext2FS"sv, true, true, true, Ext2FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "9p"sv, "Plan9FS"sv, true, true, true, Plan9FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "iso9660"sv, "ISO9660FS"sv, true, true, true, ISO9660FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "fat"sv, "FATFS"sv, true, true, true, FATFS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
};

static bool find_filesystem_type(StringView fs_type)
{
    for (auto& initializer_entry : s_initializers) {
        if (fs_type != initializer_entry.short_name && fs_type != initializer_entry.name)
            continue;
        return true;
    }
    return false;
}

ErrorOr<NonnullLockRefPtr<MountFile>> MountFile::create(NonnullOwnPtr<KString> filesystem_type, int flags)
{
    // NOTE: We should not open a MountFile if someone wants to either remount or bindmount.
    // There's a check for this in the fsopen syscall entry handler, but here we just assert
    // to ensure this never happens.
    VERIFY(!(flags & MS_BIND));
    VERIFY(!(flags & MS_REMOUNT));
    if (!find_filesystem_type(filesystem_type->view()))
        return Error::from_errno(ENODEV);
    auto mount_specific_data_buffer = TRY(KBuffer::try_create_with_size("Mount Specific Data"sv, 4096, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) MountFile(move(filesystem_type), flags, move(mount_specific_data_buffer))));
}

static MountFile::FileSystemInitializer const& find_filesystem_initializer(StringView fs_type)
{
    for (auto& initializer_entry : s_initializers) {
        if (fs_type != initializer_entry.short_name && fs_type != initializer_entry.name)
            continue;
        return initializer_entry;
    }
    // NOTE: We should never reach this point in the code, because we check in the construction method that
    // there's a valid FileSystemInitializer.
    VERIFY_NOT_REACHED();
}

MountFile::MountFile(NonnullOwnPtr<KString> filesystem_type, int flags, NonnullOwnPtr<KBuffer> mount_specific_data)
    : m_flags(flags)
    , m_filesystem_initializer(find_filesystem_initializer(filesystem_type->view()))
    , m_filesystem_type(move(filesystem_type))
    , m_mount_specific_data(move(mount_specific_data))
{
    memset(m_mount_specific_data->data(), 0, m_mount_specific_data->size());
}

MountFile::~MountFile() = default;

ErrorOr<void> MountFile::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    return m_filesystem.with_exclusive([&](auto& filesystem) -> ErrorOr<void> {
        switch (request) {
        case MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG: {
            auto user_mount_specific_data = static_ptr_cast<mount_specific_flag const*>(arg);
            auto mount_specific_data = TRY(copy_typed_from_user(user_mount_specific_data));
            if (filesystem)
                return EPERM;
            if ((mount_specific_data.value_type == mount_specific_flag::ValueType::SignedInteger || mount_specific_data.value_type == mount_specific_flag::ValueType::UnsignedInteger) && mount_specific_data.value_length != 8)
                return EDOM;
            if (mount_specific_data.key_string_length > 64)
                return ENAMETOOLONG;
            if (mount_specific_data.value_type != mount_specific_flag::ValueType::None && mount_specific_data.value_length == 0)
                return EINVAL;
            if (mount_specific_data.value_type != mount_specific_flag::ValueType::None && mount_specific_data.value_addr == nullptr)
                return EFAULT;
            if (mount_specific_data.value_type != mount_specific_flag::ValueType::ASCIIString && mount_specific_data.value_length > 64)
                return E2BIG;
            if (mount_specific_data.value_type == mount_specific_flag::ValueType::ASCIIString && mount_specific_data.value_length > 1024)
                return E2BIG;
            // NOTE: We enforce that the passed argument will be either i64 or u64, so it will always be
            // exactly 8 bytes. We do that to simplify handling of integers as well as to ensure ABI correctness
            // in all possible cases.
            auto key_string = TRY(try_copy_kstring_from_user({ reinterpret_cast<char const*>(mount_specific_data.key_string_addr), static_cast<size_t>(mount_specific_data.key_string_length) }));
            switch (mount_specific_data.value_type) {
            // NOTE: This is actually considered as simply boolean flag.
            case mount_specific_flag::ValueType::None: {
                VERIFY(m_filesystem_initializer.handle_mount_boolean_flag);
                TRY(m_filesystem_initializer.handle_mount_boolean_flag(m_mount_specific_data->bytes(), key_string->view()));
                return {};
            }
            case mount_specific_flag::ValueType::UnsignedInteger: {
                VERIFY(m_filesystem_initializer.handle_mount_unsigned_integer_flag);
                Userspace<u64*> user_value_addr((FlatPtr)mount_specific_data.value_addr);
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_filesystem_initializer.handle_mount_unsigned_integer_flag(m_mount_specific_data->bytes(), key_string->view(), value_integer));
                return {};
            }
            case mount_specific_flag::ValueType::SignedInteger: {
                VERIFY(m_filesystem_initializer.handle_mount_signed_integer_flag);
                Userspace<i64*> user_value_addr((FlatPtr)mount_specific_data.value_addr);
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_filesystem_initializer.handle_mount_signed_integer_flag(m_mount_specific_data->bytes(), key_string->view(), value_integer));
                return {};
            }
            case mount_specific_flag::ValueType::ASCIIString: {
                VERIFY(m_filesystem_initializer.handle_mount_ascii_string_flag);
                auto value_string = TRY(try_copy_kstring_from_user({ reinterpret_cast<char const*>(mount_specific_data.value_addr), static_cast<size_t>(mount_specific_data.value_length) }));
                TRY(m_filesystem_initializer.handle_mount_ascii_string_flag(m_mount_specific_data->bytes(), key_string->view(), value_string->view()));
                return {};
            }
            default:
                return EINVAL;
            }
            return {};
        }
        case MOUNT_IOCTL_SET_FILE_SOURCE: {
            auto source_fd_value = static_cast<int>(arg.ptr());
            if (filesystem)
                return EPERM;
            m_source_fd = source_fd_value;
            return {};
        }
        case MOUNT_IOCTL_CREATE_FILESYSTEM: {
            auto description_or_error = Process::current().open_file_description(m_source_fd);
            if (!description_or_error.is_error())
                dbgln("MountFile: prepare filesystem {}, source fd {}", m_filesystem_type->view(), m_source_fd);
            else
                dbgln("MountFile: prepare filesystem {}", m_filesystem_type->view());

            LockRefPtr<FileSystem> new_fs;
            if (!description_or_error.is_error()) {
                auto description = description_or_error.release_value();
                if (!m_filesystem_initializer.requires_open_file_description) {
                    VERIFY(m_filesystem_initializer.create);
                    new_fs = TRY(m_filesystem_initializer.create(m_mount_specific_data->bytes()));
                } else {
                    // Note: If there's an associated file description with the filesystem, we could
                    // try to first find it from the VirtualFileSystem filesystem list and if it was not found,
                    // then create it and add it.
                    VERIFY(m_filesystem_initializer.create_with_fd);
                    if (m_filesystem_initializer.requires_block_device && !description->file().is_block_device())
                        return ENOTBLK;
                    if (m_filesystem_initializer.requires_seekable_file && !description->file().is_seekable()) {
                        dbgln("mount: this is not a seekable file");
                        return ENODEV;
                    }
                    new_fs = TRY(VirtualFileSystem::the().find_already_existing_or_create_file_backed_file_system(*description, m_mount_specific_data->bytes(), m_filesystem_initializer.create_with_fd));
                }
            } else {
                if (m_filesystem_initializer.requires_open_file_description)
                    return ENOTSUP;
                if (!m_filesystem_initializer.create)
                    return ENOTSUP;
                new_fs = TRY(m_filesystem_initializer.create(m_mount_specific_data->bytes()));
            }
            TRY(new_fs->initialize());
            filesystem = *new_fs;
            return {};
        }
        default:
            return EINVAL;
        }
    });
}

ErrorOr<NonnullOwnPtr<KString>> MountFile::pseudo_path(OpenFileDescription const&) const
{
    return KString::try_create(":mount-file:"sv);
}

}
