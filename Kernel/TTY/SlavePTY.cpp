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

#include "SlavePTY.h"
#include "MasterPTY.h"
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/Process.h>

//#define SLAVEPTY_DEBUG

namespace Kernel {

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(201, index)
    , m_master(master)
    , m_index(index)
{
    sprintf(m_tty_name, "/dev/pts/%u", m_index);
    set_uid(Process::current->uid());
    set_gid(Process::current->gid());
    DevPtsFS::register_slave_pty(*this);
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
#ifdef SLAVEPTY_DEBUG
    dbgprintf("~SlavePTY(%u)\n", m_index);
#endif
    DevPtsFS::unregister_slave_pty(*this);
}

StringView SlavePTY::tty_name() const
{
    return m_tty_name;
}

void SlavePTY::echo(u8 ch)
{
    if (should_echo_input()) {
        m_master->on_slave_write(&ch, 1);
    }
}

void SlavePTY::on_master_write(const u8* buffer, ssize_t size)
{
    for (ssize_t i = 0; i < size; ++i)
        emit(buffer[i]);
}

ssize_t SlavePTY::on_tty_write(const u8* data, ssize_t size)
{
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(const FileDescription&) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(const FileDescription& description) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(description);
}

ssize_t SlavePTY::read(FileDescription& description, u8* buffer, ssize_t size)
{
    if (m_master->is_closed())
        return 0;
    return TTY::read(description, buffer, size);
}

void SlavePTY::close()
{
    m_master->notify_slave_closed({});
}

}
