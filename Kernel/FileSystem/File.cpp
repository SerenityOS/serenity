/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

KResultOr<NonnullRefPtr<OpenFileDescription>> File::open(int options)
{
    auto description = OpenFileDescription::try_create(*this);
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

KResult File::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return ENOTTY;
}

KResultOr<Memory::Region*> File::mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64, int, bool)
{
    return ENODEV;
}

KResult File::attach(OpenFileDescription&)
{
    m_attach_count++;
    return KSuccess;
}

void File::detach(OpenFileDescription&)
{
    m_attach_count--;
}
}
