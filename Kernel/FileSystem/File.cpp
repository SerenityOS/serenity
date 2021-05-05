/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileDescription.h>

namespace Kernel {

File::File()
{
}

File::~File()
{
}

KResultOr<NonnullRefPtr<FileDescription>> File::open(int options)
{
    auto description = FileDescription::create(*this);
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

int File::ioctl(FileDescription&, unsigned, FlatPtr)
{
    return -ENOTTY;
}

KResultOr<Region*> File::mmap(Process&, FileDescription&, const Range&, u64, int, bool)
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

void File::remember_thread_access()
{
    auto current_thread = Thread::current();
    auto first_thread = m_accessing_threads[0].strong_ref();
    auto second_thread = m_accessing_threads[1].strong_ref();
    // FIXME: This is terrible.
    if (!first_thread) {
        m_accessing_threads[0] = current_thread;
        return;
    }
    if (!second_thread && first_thread != current_thread) {
        m_accessing_threads[1] = current_thread;
        return;
    }
}

RefPtr<Thread> File::likely_peer_thread() const
{
    auto current_thread = Thread::current();
    auto first_thread = m_accessing_threads[0].strong_ref();
    if (first_thread == current_thread)
        return m_accessing_threads[1].strong_ref();
    else
        return first_thread;
}

}
