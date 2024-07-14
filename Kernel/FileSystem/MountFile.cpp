/*
 * Copyright (c) 2022-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
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
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) MountFile(file_system_initializer, flags)));
}

MountFile::MountFile(FileSystemInitializer const& file_system_initializer, int flags)
    : m_flags(flags)
    , m_file_system_initializer(file_system_initializer)
{
}

MountFile::~MountFile() = default;

static ErrorOr<void> verify_mount_specific_option_data(MountSpecificFlag const& data)
{
    // NOTE: We put this limit in place because we assume that don't need to handle huge
    // amounts of bytes when trying to handle a mount fs-specific flag. A zero-sized value
    // is also not valid either.
    // Anything larger than this constant (which could be changed if needed) is deemed to
    // potentially cause OOM condition, and cannot represent any reasonable and "honest" data
    // from userspace.
    constexpr auto value_max_size = max(sizeof(unsigned),
        max(sizeof(signed),
            sizeof(u64)));

    if (data.value_length == 0)
        return EINVAL;

    if (data.value_length > value_max_size)
        return E2BIG;

    if (data.value_addr == nullptr)
        return EFAULT;

    switch (data.value_type) {
    case MountSpecificFlag::ValueType::SignedInteger:
    case MountSpecificFlag::ValueType::Boolean:
    case MountSpecificFlag::ValueType::UnsignedInteger:
        return {};

    default:
        return EINVAL;
    }
}

ErrorOr<void> MountFile::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    return m_filesystem_specific_options.with_exclusive([&](auto& filesystem_specific_options) -> ErrorOr<void> {
        auto user_mount_specific_data = static_ptr_cast<MountSpecificFlag const*>(arg);
        auto mount_specific_data = TRY(copy_typed_from_user(user_mount_specific_data));

        Syscall::StringArgument user_key_string { reinterpret_cast<char const*>(mount_specific_data.key_string_addr), static_cast<size_t>(mount_specific_data.key_string_length) };
        auto key_string = TRY(Process::get_syscall_name_string_fixed_buffer<MOUNT_SPECIFIC_FLAG_KEY_STRING_MAX_LENGTH>(user_key_string));

        switch (request) {
        case MOUNT_IOCTL_DELETE_MOUNT_SPECIFIC_FLAG: {
            if (!filesystem_specific_options.remove(key_string.representable_view()))
                dbgln("MountFile: WARNING: mount option by key {} was not found, deletion request ignored", key_string.representable_view());
            return {};
        }
        case MOUNT_IOCTL_SET_MOUNT_SPECIFIC_FLAG: {
            TRY(verify_mount_specific_option_data(mount_specific_data));
            if (filesystem_specific_options.get(key_string.representable_view()).has_value())
                return Error::from_errno(EEXIST);

            auto add_key_with_value = [&filesystem_specific_options](StringView name, NonnullOwnPtr<FileSystemSpecificOption> option) -> ErrorOr<void> {
                auto kstring_name = TRY(KString::try_create(name));
                auto result = TRY(filesystem_specific_options.try_set(move(kstring_name), move(option)));
                // NOTE: We checked earlier that there's no matching entry, so we must have a result
                // of newly inserted entry.
                VERIFY(result == HashSetResult::InsertedNewEntry);
                return {};
            };

            // NOTE: We enforce that the passed argument will be either i64 or u64, so it will always be
            // exactly 8 bytes. We do that to simplify handling of integers as well as to ensure ABI correctness
            // in all possible cases.
            switch (mount_specific_data.value_type) {
            // NOTE: This is actually considered as simply boolean flag.
            case MountSpecificFlag::ValueType::Boolean: {
                VERIFY(m_file_system_initializer.validate_mount_boolean_flag);
                Userspace<u64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                if (value_integer != 0 && value_integer != 1)
                    return EDOM;
                bool value = (value_integer == 1) ? true : false;
                TRY(m_file_system_initializer.validate_mount_boolean_flag(key_string.representable_view(), value));

                auto file_system_specific_option = TRY(FileSystemSpecificOption::create_as_boolean(value));
                TRY(add_key_with_value(key_string.representable_view(), move(file_system_specific_option)));
                return {};
            }
            case MountSpecificFlag::ValueType::UnsignedInteger: {
                VERIFY(m_file_system_initializer.validate_mount_unsigned_integer_flag);
                Userspace<u64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_file_system_initializer.validate_mount_unsigned_integer_flag(key_string.representable_view(), value_integer));

                auto file_system_specific_option = TRY(FileSystemSpecificOption::create_as_unsigned(value_integer));
                TRY(add_key_with_value(key_string.representable_view(), move(file_system_specific_option)));
                return {};
            }
            case MountSpecificFlag::ValueType::SignedInteger: {
                VERIFY(m_file_system_initializer.validate_mount_signed_integer_flag);
                Userspace<i64*> user_value_addr(reinterpret_cast<FlatPtr>(mount_specific_data.value_addr));
                auto value_integer = TRY(copy_typed_from_user(user_value_addr));
                TRY(m_file_system_initializer.validate_mount_signed_integer_flag(key_string.representable_view(), value_integer));

                auto file_system_specific_option = TRY(FileSystemSpecificOption::create_as_signed(value_integer));
                TRY(add_key_with_value(key_string.representable_view(), move(file_system_specific_option)));
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
