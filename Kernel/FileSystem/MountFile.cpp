/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/FileSystem/MountSpecificFlags.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/unistd.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<MountFile>> MountFile::create(FileSystemInitializer const& file_system_initializer, int flags)
{
    // NOTE: We should not open a MountFile if someone wants to either remount or bindmount.
    // There's a check for this in the fsopen syscall entry handler, but here we just assert
    // to ensure this never happens.
    VERIFY(!(flags & MS_BIND));
    VERIFY(!(flags & MS_REMOUNT));
    auto mount_specific_data_buffer = TRY(KBuffer::try_create_with_size("Mount Specific Data"sv, PAGE_SIZE, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) MountFile(file_system_initializer, flags, move(mount_specific_data_buffer))));
}

MountFile::MountFile(FileSystemInitializer const& file_system_initializer, int flags, NonnullOwnPtr<KBuffer> mount_specific_data)
    : m_flags(flags)
    , m_file_system_initializer(file_system_initializer)
{
    m_mount_specific_data.with_exclusive([&](auto& our_mount_specific_data) {
        our_mount_specific_data = move(mount_specific_data);
        memset(our_mount_specific_data->data(), 0, our_mount_specific_data->size());
    });
}

MountFile::~MountFile() = default;

ErrorOr<void> MountFile::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    return m_mount_specific_data.with_exclusive([&](auto& our_mount_specific_data) -> ErrorOr<void> {
        switch (request) {
        case MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG: {
            auto user_mount_specific_data = static_ptr_cast<MountSpecificFlag const*>(arg);
            auto mount_specific_data = TRY(copy_typed_from_user(user_mount_specific_data));
            if ((mount_specific_data.value_type == MountSpecificFlag::ValueType::SignedInteger || mount_specific_data.value_type == MountSpecificFlag::ValueType::UnsignedInteger) && mount_specific_data.value_length != 8)
                return EDOM;

            Syscall::StringArgument user_key_string { reinterpret_cast<const char*>(mount_specific_data.key_string_addr), static_cast<size_t>(mount_specific_data.key_string_length) };
            auto key_string = TRY(Process::get_syscall_name_string_fixed_buffer<MOUNT_SPECIFIC_FLAG_KEY_STRING_MAX_LENGTH>(user_key_string));

            if (mount_specific_data.value_type != MountSpecificFlag::ValueType::Boolean && mount_specific_data.value_length == 0)
                return EINVAL;
            if (mount_specific_data.value_type != MountSpecificFlag::ValueType::Boolean && mount_specific_data.value_addr == nullptr)
                return EFAULT;

            // NOTE: We put these limits in place because we assume that don't need to handle huge
            // amounts of bytes when trying to handle a mount fs-specific flag.
            // Anything larger than these constants (which could be changed if needed) is deemed to
            // potentially cause OOM condition, and cannot represent any reasonable and "honest" data
            // from userspace.
            if (mount_specific_data.value_type != MountSpecificFlag::ValueType::ASCIIString && mount_specific_data.value_length > MOUNT_SPECIFIC_FLAG_NON_ASCII_STRING_TYPE_MAX_LENGTH)
                return E2BIG;
            if (mount_specific_data.value_type == MountSpecificFlag::ValueType::ASCIIString && mount_specific_data.value_length > MOUNT_SPECIFIC_FLAG_ASCII_STRING_TYPE_MAX_LENGTH)
                return E2BIG;

            // NOTE: We enforce that the passed argument will be either i64 or u64, so it will always be
            // exactly 8 bytes. We do that to simplify handling of integers as well as to ensure ABI correctness
            // in all possible cases.
            switch (mount_specific_data.value_type) {
            // NOTE: This is actually considered as simply boolean flag.
            case MountSpecificFlag::ValueType::Boolean: {
                VERIFY(m_file_system_initializer.handle_mount_boolean_flag);
                Userspace<u64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                if (value_integer != 0 && value_integer != 1)
                    return EDOM;
                bool value = (value_integer == 1) ? true : false;
                TRY(m_file_system_initializer.handle_mount_boolean_flag(our_mount_specific_data->bytes(), key_string.representable_view(), value));
                return {};
            }
            case MountSpecificFlag::ValueType::UnsignedInteger: {
                VERIFY(m_file_system_initializer.handle_mount_unsigned_integer_flag);
                Userspace<u64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_file_system_initializer.handle_mount_unsigned_integer_flag(our_mount_specific_data->bytes(), key_string.representable_view(), value_integer));
                return {};
            }
            case MountSpecificFlag::ValueType::SignedInteger: {
                VERIFY(m_file_system_initializer.handle_mount_signed_integer_flag);
                Userspace<i64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_file_system_initializer.handle_mount_signed_integer_flag(our_mount_specific_data->bytes(), key_string.representable_view(), value_integer));
                return {};
            }
            case MountSpecificFlag::ValueType::ASCIIString: {
                VERIFY(m_file_system_initializer.handle_mount_ascii_string_flag);
                auto value_string = TRY(try_copy_kstring_from_user(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr), static_cast<size_t>(mount_specific_data.value_length)));
                TRY(m_file_system_initializer.handle_mount_ascii_string_flag(our_mount_specific_data->bytes(), key_string.representable_view(), value_string->view()));
                return {};
            }
            default:
                return EINVAL;
            }
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
