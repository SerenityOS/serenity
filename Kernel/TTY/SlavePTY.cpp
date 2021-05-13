/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SlavePTY.h"
#include "MasterPTY.h"
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/Process.h>

namespace Kernel {

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(201, index)
    , m_master(master)
    , m_index(index)
{
    m_tty_name = String::formatted("/dev/pts/{}", m_index);
    auto process = Process::current();
    set_uid(process->uid());
    set_gid(process->gid());
    DevPtsFS::register_slave_pty(*this);
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
    dbgln_if(SLAVEPTY_DEBUG, "~SlavePTY({})", m_index);
    DevPtsFS::unregister_slave_pty(*this);
}

String const& SlavePTY::tty_name() const
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
    auto result = buffer.read_buffered<128>(size, [&](u8 const* data, size_t data_size) {
        for (size_t i = 0; i < data_size; ++i)
            emit(data[i], false);
        return data_size;
    });
    if (!result.is_error())
        evaluate_block_conditions();
}

ssize_t SlavePTY::on_tty_write(const UserOrKernelBuffer& data, ssize_t size)
{
    m_time_of_last_write = kgettimeofday().to_truncated_seconds();
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

KResultOr<size_t> SlavePTY::read(FileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t size)
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

String SlavePTY::device_name() const
{
    return String::formatted("{}", minor());
}

FileBlockCondition& SlavePTY::block_condition()
{
    return m_master->block_condition();
}

}
