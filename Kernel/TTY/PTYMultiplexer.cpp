/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PTYMultiplexer.h"
#include "MasterPTY.h"
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static AK::Singleton<PTYMultiplexer> s_the;

PTYMultiplexer& PTYMultiplexer::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(5, 2)
{
    constexpr unsigned max_pty_pairs = 8;
    m_freelist.ensure_capacity(max_pty_pairs);
    for (int i = max_pty_pairs; i > 0; --i)
        m_freelist.unchecked_append(i - 1);
}

UNMAP_AFTER_INIT PTYMultiplexer::~PTYMultiplexer()
{
}

KResultOr<NonnullRefPtr<FileDescription>> PTYMultiplexer::open(int options)
{
    Locker locker(m_lock);
    if (m_freelist.is_empty())
        return EBUSY;
    auto master_index = m_freelist.take_last();
    auto master = adopt_ref_if_nonnull(new MasterPTY(master_index));
    if (!master)
        return ENOMEM;
    dbgln_if(PTMX_DEBUG, "PTYMultiplexer::open: Vending master {}", master->index());
    auto description = FileDescription::create(*master);
    if (!description.is_error()) {
        description.value()->set_rw_mode(options);
        description.value()->set_file_flags(options);
    }
    return description;
}

void PTYMultiplexer::notify_master_destroyed(Badge<MasterPTY>, unsigned index)
{
    Locker locker(m_lock);
    m_freelist.append(index);
    dbgln_if(PTMX_DEBUG, "PTYMultiplexer: {} added to freelist", index);
}

}
