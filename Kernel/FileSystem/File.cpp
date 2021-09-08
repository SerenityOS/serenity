/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Userspace.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

KResult File::attach_new_file_blocker()
{
    VERIFY(!m_blocker_set);
    m_blocker_set = adopt_ref_if_nonnull(new FileBlockerSet);
    return m_blocker_set ? KSuccess : KResult(ENOMEM);
}

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

const FIFO* File::as_fifo() const
{
    return is_fifo() ? static_cast<const FIFO*>(this) : nullptr;
}
FIFO* File::as_fifo()
{
    return is_fifo() ? static_cast<FIFO*>(this) : nullptr;
}
const Socket* File::as_socket() const
{
    return is_socket() ? static_cast<const Socket*>(this) : nullptr;
}
Socket* File::as_socket()
{
    return is_socket() ? static_cast<Socket*>(this) : nullptr;
}
const InodeWatcher* File::as_inode_watcher() const
{
    return is_inode_watcher() ? static_cast<const InodeWatcher*>(this) : nullptr;
}
InodeWatcher* File::as_inode_watcher()
{
    return is_inode_watcher() ? static_cast<InodeWatcher*>(this) : nullptr;
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
