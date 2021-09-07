/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/SlavePTY.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

KResultOr<NonnullRefPtr<MasterPTY>> MasterPTY::try_create(unsigned int index)
{
    auto buffer = TRY(DoubleBuffer::try_create());
    auto master_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MasterPTY(index, move(buffer))));
    auto slave_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SlavePTY(*master_pty, index)));
    master_pty->m_slave = slave_pty;
    return master_pty;
}

MasterPTY::MasterPTY(unsigned index, NonnullOwnPtr<DoubleBuffer> buffer)
    : CharacterDevice(200, index)
    , m_index(index)
    , m_buffer(move(buffer))
{
    m_pts_name = String::formatted("/dev/pts/{}", m_index);
    auto& process = Process::current();
    set_uid(process.uid());
    set_gid(process.gid());

    m_buffer->set_unblock_callback([this]() {
        if (m_slave)
            evaluate_block_conditions();
    });
}

MasterPTY::~MasterPTY()
{
    dbgln_if(MASTERPTY_DEBUG, "~MasterPTY({})", m_index);
    PTYMultiplexer::the().notify_master_destroyed({}, m_index);
}

String MasterPTY::pts_name() const
{
    return m_pts_name;
}

KResultOr<size_t> MasterPTY::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_slave && m_buffer->is_empty())
        return 0;
    return m_buffer->read(buffer, size);
}

KResultOr<size_t> MasterPTY::write(OpenFileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_slave)
        return EIO;
    m_slave->on_master_write(buffer, size);
    return size;
}

bool MasterPTY::can_read(const OpenFileDescription&, size_t) const
{
    if (!m_slave)
        return true;
    return !m_buffer->is_empty();
}

bool MasterPTY::can_write(const OpenFileDescription&, size_t) const
{
    return true;
}

void MasterPTY::notify_slave_closed(Badge<SlavePTY>)
{
    dbgln_if(MASTERPTY_DEBUG, "MasterPTY({}): slave closed, my retains: {}, slave retains: {}", m_index, ref_count(), m_slave->ref_count());
    // +1 ref for my MasterPTY::m_slave
    // +1 ref for OpenFileDescription::m_device
    if (m_slave->ref_count() == 2)
        m_slave = nullptr;
}

KResultOr<size_t> MasterPTY::on_slave_write(const UserOrKernelBuffer& data, size_t size)
{
    if (m_closed)
        return EIO;
    return m_buffer->write(data, size);
}

bool MasterPTY::can_write_from_slave() const
{
    if (m_closed)
        return true;
    return m_buffer->space_for_writing();
}

KResult MasterPTY::close()
{
    InterruptDisabler disabler;
    // After the closing OpenFileDescription dies, slave is the only thing keeping me alive.
    // From this point, let's consider ourselves closed.
    m_closed = true;

    if (m_slave)
        m_slave->hang_up();

    return KSuccess;
}

KResult MasterPTY::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    REQUIRE_PROMISE(tty);
    if (!m_slave)
        return EIO;
    if (request == TIOCSWINSZ || request == TIOCGPGRP)
        return m_slave->ioctl(description, request, arg);
    return EINVAL;
}

String MasterPTY::absolute_path(const OpenFileDescription&) const
{
    return String::formatted("ptm:{}", m_pts_name);
}

String MasterPTY::device_name() const
{
    return String::formatted("{}", minor());
}

}
