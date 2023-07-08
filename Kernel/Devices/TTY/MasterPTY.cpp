/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/MasterPTY.h>
#include <Kernel/Devices/TTY/PTYMultiplexer.h>
#include <Kernel/Devices/TTY/SlavePTY.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<MasterPTY>> MasterPTY::try_create(unsigned int index)
{
    auto buffer = TRY(DoubleBuffer::try_create("MasterPTY: Buffer"sv));
    auto master_pty = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) MasterPTY(index, move(buffer))));
    auto credentials = Process::current().credentials();
    auto slave_pty = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SlavePTY(*master_pty, credentials->uid(), credentials->gid(), index)));
    master_pty->m_slave = slave_pty;
    TRY(master_pty->after_inserting());
    TRY(slave_pty->after_inserting());
    return master_pty;
}

MasterPTY::MasterPTY(unsigned index, NonnullOwnPtr<DoubleBuffer> buffer)
    : CharacterDevice(200, index)
    , m_index(index)
    , m_buffer(move(buffer))
{
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

ErrorOr<size_t> MasterPTY::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_slave && m_buffer->is_empty())
        return 0;
    return m_buffer->read(buffer, size);
}

ErrorOr<size_t> MasterPTY::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    if (!m_slave)
        return EIO;
    m_slave->on_master_write(buffer, size);
    return size;
}

bool MasterPTY::can_read(OpenFileDescription const&, u64) const
{
    if (!m_slave)
        return true;
    return !m_buffer->is_empty();
}

bool MasterPTY::can_write(OpenFileDescription const&, u64) const
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

ErrorOr<size_t> MasterPTY::on_slave_write(UserOrKernelBuffer const& data, size_t size)
{
    if (m_closed)
        return EIO;
    return m_buffer->write(data, size);
}

bool MasterPTY::can_write_from_slave() const
{
    if (m_closed)
        return true;
    return m_buffer->space_for_writing() >= 2;
}

ErrorOr<void> MasterPTY::close()
{
    InterruptDisabler disabler;
    // After the closing OpenFileDescription dies, slave is the only thing keeping me alive.
    // From this point, let's consider ourselves closed.
    m_closed = true;

    if (m_slave)
        m_slave->hang_up();

    return {};
}

ErrorOr<void> MasterPTY::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    TRY(Process::current().require_promise(Pledge::tty));
    if (!m_slave)
        return EIO;
    switch (request) {
    case TIOCGPTN: {
        int master_pty_index = index();
        return copy_to_user(static_ptr_cast<int*>(arg), &master_pty_index);
    }
    case TIOCSWINSZ:
    case TIOCGPGRP:
        return m_slave->ioctl(description, request, arg);
    default:
        return EINVAL;
    }
}

ErrorOr<NonnullOwnPtr<KString>> MasterPTY::pseudo_path(OpenFileDescription const&) const
{
    return KString::formatted("ptm:{}", m_index);
}

}
