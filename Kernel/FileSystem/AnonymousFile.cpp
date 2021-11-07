/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/AnonymousFile.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Process.h>

namespace Kernel {

AnonymousFile::AnonymousFile(NonnullRefPtr<Memory::AnonymousVMObject> vmobject)
    : m_vmobject(move(vmobject))
{
}

AnonymousFile::~AnonymousFile()
{
}

ErrorOr<Memory::Region*> AnonymousFile::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    if (offset != 0)
        return EINVAL;

    if (range.size() != m_vmobject->size())
        return EINVAL;

    return process.address_space().allocate_region_with_vmobject(range, m_vmobject, offset, {}, prot, shared);
}

ErrorOr<NonnullOwnPtr<KString>> AnonymousFile::pseudo_path(const OpenFileDescription&) const
{
    return KString::try_create(":anonymous-file:"sv);
}

}
