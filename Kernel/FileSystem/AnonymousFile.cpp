/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

AnonymousFile::AnonymousFile(NonnullLockRefPtr<Memory::AnonymousVMObject> vmobject)
    : m_vmobject(move(vmobject))
{
}

AnonymousFile::~AnonymousFile() = default;

ErrorOr<File::VMObjectAndMemoryType> AnonymousFile::vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64& offset, bool)
{
    if (offset != 0)
        return EINVAL;

    return VMObjectAndMemoryType {
        .vmobject = m_vmobject,
        .memory_type = Memory::MemoryType::Normal,
    };
}

ErrorOr<NonnullOwnPtr<KString>> AnonymousFile::pseudo_path(OpenFileDescription const&) const
{
    return KString::try_create(":anonymous-file:"sv);
}

}
