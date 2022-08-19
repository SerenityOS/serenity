/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>

namespace Kernel {

AnonymousFile::AnonymousFile(NonnullLockRefPtr<Memory::AnonymousVMObject> vmobject)
    : m_vmobject(move(vmobject))
{
}

AnonymousFile::~AnonymousFile() = default;

ErrorOr<Memory::Region*> AnonymousFile::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    if (offset != 0)
        return EINVAL;

    return process.address_space().allocate_region_with_vmobject(range, m_vmobject, offset, {}, prot, shared);
}

ErrorOr<NonnullOwnPtr<KString>> AnonymousFile::pseudo_path(OpenFileDescription const&) const
{
    return KString::try_create(":anonymous-file:"sv);
}

}
