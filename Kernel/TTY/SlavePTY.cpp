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
    snprintf(m_tty_name, sizeof(m_tty_name), "/dev/pts/%u", m_index);
    auto process = Process::current();
    set_uid(process->uid());
    set_gid(process->gid());
    DevPtsFS::register_slave_pty(*this);
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
#ifdef SLAVEPTY_DEBUG
    dbg() << "~SlavePTY(" << m_index << ")";
#endif
    DevPtsFS::unregister_slave_pty(*this);
}

String SlavePTY::tty_name() const
{
    return m_tty_name;
}

void SlavePTY::echo(u8 ch)
{
    if (should_echo_input()) {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(&ch);
        m_master->on_slave_write(buffer, 1);
    }
}

void SlavePTY::on_master_write(const UserOrKernelBuffer& buffer, ssize_t size)
{
    ssize_t nread = buffer.read_buffered<128>(size, [&](const u8* data, size_t data_size) {
        for (size_t i = 0; i < data_size; ++i)
            emit(data[i]);
        return (ssize_t)data_size;
    });
    (void)nread;
}

ssize_t SlavePTY::on_tty_write(const UserOrKernelBuffer& data, ssize_t size)
{
    m_time_of_last_write = kgettimeofday().tv_sec;
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(const FileDescription&, size_t) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(const FileDescription& description, size_t offset) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(description, offset);
}

KResultOr<size_t> SlavePTY::read(FileDescription& description, size_t offset, UserOrKernelBuffer& buffer, size_t size)
{
    if (m_master->is_closed())
        return 0;
    return TTY::read(description, offset, buffer, size);
}

KResult SlavePTY::close()
{
    m_master->notify_slave_closed({});
    return KSuccess;
}

}
