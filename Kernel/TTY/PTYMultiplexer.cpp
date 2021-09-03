/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static Singleton<PTYMultiplexer> s_the;

PTYMultiplexer& PTYMultiplexer::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(5, 2)
{
    m_freelist.with_exclusive([&](auto& freelist) {
        freelist.ensure_capacity(max_pty_pairs);
        for (int i = max_pty_pairs; i > 0; --i)
            freelist.unchecked_append(i - 1);
    });
}

UNMAP_AFTER_INIT PTYMultiplexer::~PTYMultiplexer()
{
}

KResultOr<NonnullRefPtr<FileDescription>> PTYMultiplexer::open(int options)
{
    return m_freelist.with_exclusive([&](auto& freelist) -> KResultOr<NonnullRefPtr<FileDescription>> {
        if (freelist.is_empty())
            return EBUSY;

        auto master_index = freelist.take_last();
        auto master = MasterPTY::try_create(master_index);
        if (!master)
            return ENOMEM;
        dbgln_if(PTMX_DEBUG, "PTYMultiplexer::open: Vending master {}", master->index());
        auto description = FileDescription::try_create(*master);
        if (!description.is_error()) {
            description.value()->set_rw_mode(options);
            description.value()->set_file_flags(options);
        }
        return description;
    });
}

void PTYMultiplexer::notify_master_destroyed(Badge<MasterPTY>, unsigned index)
{
    m_freelist.with_exclusive([&](auto& freelist) {
        freelist.append(index);
        dbgln_if(PTMX_DEBUG, "PTYMultiplexer: {} added to freelist", index);
    });
}

}
