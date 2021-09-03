/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringView.h>
#include <YAK/Userspace.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

File::File()
{
}

File::~File()
{
}

bool File::unref() const
{
    if (deref_base())
        return false;
    delete this;
    return true;
}

KResultOr<NonnullRefPtr<FileDescription>> File::open(int options)
{
    auto description = FileDescription::try_create(*this);
    if (!description.is_error()) {
        description.value()->set_rw_mode(options);
        description.value()->set_file_flags(options);
    }
    return description;
}

KResult File::close()
{
    return KSuccess;
}

KResult File::ioctl(FileDescription&, unsigned, Userspace<void*>)
{
    return ENOTTY;
}

KResultOr<Memory::Region*> File::mmap(Process&, FileDescription&, Memory::VirtualRange const&, u64, int, bool)
{
    return ENODEV;
}

KResult File::attach(FileDescription&)
{
    m_attach_count++;
    return KSuccess;
}

void File::detach(FileDescription&)
{
    m_attach_count--;
}
}
