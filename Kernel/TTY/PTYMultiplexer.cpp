/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "PTYMultiplexer.h"
#include "MasterPTY.h"
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

//#define PTMX_DEBUG

namespace Kernel {

static const unsigned s_max_pty_pairs = 8;
static PTYMultiplexer* s_the;

PTYMultiplexer& PTYMultiplexer::the()
{
    ASSERT(s_the);
    return *s_the;
}

PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(5, 2)
{
    s_the = this;
    m_freelist.ensure_capacity(s_max_pty_pairs);
    for (int i = s_max_pty_pairs; i > 0; --i)
        m_freelist.unchecked_append(i - 1);
}

PTYMultiplexer::~PTYMultiplexer()
{
}

KResultOr<NonnullRefPtr<FileDescription>> PTYMultiplexer::open(int options)
{
    LOCKER(m_lock);
    if (m_freelist.is_empty())
        return KResult(-EBUSY);
    auto master_index = m_freelist.take_last();
    auto master = adopt(*new MasterPTY(master_index));
#ifdef PTMX_DEBUG
    dbg() << "PTYMultiplexer::open: Vending master " << master->index();
#endif
    auto description = FileDescription::create(move(master));
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

void PTYMultiplexer::notify_master_destroyed(Badge<MasterPTY>, unsigned index)
{
    LOCKER(m_lock);
    m_freelist.append(index);
#ifdef PTMX_DEBUG
    dbg() << "PTYMultiplexer: " << index << " added to freelist";
#endif
}

}
