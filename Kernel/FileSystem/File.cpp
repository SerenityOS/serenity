/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Userspace.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
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
    const_cast<File&>(*this).before_removing();
    delete this;
    return true;
}

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

ErrorOr<Memory::Region*> File::mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64, int, bool)
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
