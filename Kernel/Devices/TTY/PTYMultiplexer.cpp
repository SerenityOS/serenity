/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/MasterPTY.h>
#include <Kernel/Devices/TTY/PTYMultiplexer.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<PTYMultiplexer> s_the;

PTYMultiplexer& PTYMultiplexer::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Console, 2)
{
    m_freelist.with([&](auto& freelist) {
        freelist.ensure_capacity(max_pty_pairs);
        for (int i = max_pty_pairs; i > 0; --i)
            freelist.unchecked_append(i - 1);
    });
}

UNMAP_AFTER_INIT PTYMultiplexer::~PTYMultiplexer() = default;

UNMAP_AFTER_INIT void PTYMultiplexer::initialize()
{
    MUST(the().after_inserting());
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> PTYMultiplexer::open(int options)
{
    return m_freelist.with([&](auto& freelist) -> ErrorOr<NonnullRefPtr<OpenFileDescription>> {
        if (freelist.is_empty())
            return EBUSY;

        auto master_index = freelist.take_last();
        auto master = TRY(MasterPTY::try_create(master_index));
        dbgln_if(PTMX_DEBUG, "PTYMultiplexer::open: Vending master {}", master->index());
        auto description = TRY(OpenFileDescription::try_create(*master));
        description->set_rw_mode(options);
        description->set_file_flags(options);
        return description;
    });
}

void PTYMultiplexer::notify_master_destroyed(Badge<MasterPTY>, unsigned index)
{
    m_freelist.with([&](auto& freelist) {
        freelist.append(index);
        dbgln_if(PTMX_DEBUG, "PTYMultiplexer: {} added to freelist", index);
    });
}

}
