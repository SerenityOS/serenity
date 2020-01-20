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

#include "MasterPTY.h"
#include "PTYMultiplexer.h"
#include "SlavePTY.h"
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

//#define MASTERPTY_DEBUG

MasterPTY::MasterPTY(unsigned index)
    : CharacterDevice(200, index)
    , m_slave(adopt(*new SlavePTY(*this, index)))
    , m_index(index)
{
    m_pts_name = String::format("/dev/pts/%u", m_index);
    set_uid(current->process().uid());
    set_gid(current->process().gid());
}

MasterPTY::~MasterPTY()
{
#ifdef MASTERPTY_DEBUG
    dbgprintf("~MasterPTY(%u)\n", m_index);
#endif
    PTYMultiplexer::the().notify_master_destroyed({}, m_index);
}

String MasterPTY::pts_name() const
{
    return m_pts_name;
}

ssize_t MasterPTY::read(FileDescription&, u8* buffer, ssize_t size)
{
    if (!m_slave && m_buffer.is_empty())
        return 0;
    return m_buffer.read(buffer, size);
}

ssize_t MasterPTY::write(FileDescription&, const u8* buffer, ssize_t size)
{
    if (!m_slave)
        return -EIO;
    m_slave->on_master_write(buffer, size);
    return size;
}

bool MasterPTY::can_read(const FileDescription&) const
{
    if (!m_slave)
        return true;
    return !m_buffer.is_empty();
}

bool MasterPTY::can_write(const FileDescription&) const
{
    return true;
}

void MasterPTY::notify_slave_closed(Badge<SlavePTY>)
{
#ifdef MASTERPTY_DEBUG
    dbgprintf("MasterPTY(%u): slave closed, my retains: %u, slave retains: %u\n", m_index, ref_count(), m_slave->ref_count());
#endif
    // +1 ref for my MasterPTY::m_slave
    // +1 ref for FileDescription::m_device
    if (m_slave->ref_count() == 2)
        m_slave = nullptr;
}

ssize_t MasterPTY::on_slave_write(const u8* data, ssize_t size)
{
    if (m_closed)
        return -EIO;
    m_buffer.write(data, size);
    return size;
}

bool MasterPTY::can_write_from_slave() const
{
    if (m_closed)
        return true;
    return m_buffer.space_for_writing();
}

void MasterPTY::close()
{
    if (ref_count() == 2) {
        InterruptDisabler disabler;
        // After the closing FileDescription dies, slave is the only thing keeping me alive.
        // From this point, let's consider ourselves closed.
        m_closed = true;

        m_slave->hang_up();
    }
}

int MasterPTY::ioctl(FileDescription& description, unsigned request, unsigned arg)
{
    REQUIRE_PROMISE(tty);
    if (!m_slave)
        return -EIO;
    if (request == TIOCSWINSZ || request == TIOCGPGRP)
        return m_slave->ioctl(description, request, arg);
    return -EINVAL;
}

String MasterPTY::absolute_path(const FileDescription&) const
{
    return String::format("ptm:%s", m_pts_name.characters());
}
