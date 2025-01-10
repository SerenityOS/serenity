/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Userspace.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

File::File() = default;
File::~File() = default;

ErrorOr<NonnullRefPtr<OpenFileDescription>> File::open(int options)
{
    auto description = OpenFileDescription::try_create(*this);
    if (!description.is_error()) {
        description.value()->set_rw_mode(options);
        description.value()->set_file_flags(options);
    }
    return description;
}

ErrorOr<void> File::close()
{
    return {};
}

ErrorOr<void> File::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return ENOTTY;
}

ErrorOr<File::VMObjectAndMemoryType> File::vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64&, bool)
{
    return ENODEV;
}

ErrorOr<void> File::attach(OpenFileDescription&)
{
    m_attach_count++;
    return {};
}

void File::detach(OpenFileDescription&)
{
    m_attach_count--;
}
}
