/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/PTYMultiplexer.h>
#include <Kernel/TTY/SlavePTY.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<MasterPTY>> MasterPTY::try_create(unsigned int index)
{
    auto pts_name = TRY(KString::formatted("/dev/pts/{}", index));
    auto tty_name = TRY(pts_name->try_clone());

    auto buffer = TRY(DoubleBuffer::try_create());
    auto master_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MasterPTY(index, move(buffer), move(pts_name))));
    auto slave_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SlavePTY(*master_pty, index, move(tty_name))));
    master_pty->m_slave = slave_pty;
    master_pty->after_inserting();
    slave_pty->after_inserting();
    return master_pty;
}

MasterPTY::MasterPTY(unsigned index, NonnullOwnPtr<DoubleBuffer> buffer, NonnullOwnPtr<KString> pts_name)
    : CharacterDevice(200, index)
    , m_index(index)
    , m_buffer(move(buffer))
    , m_pts_name(move(pts_name))
{
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

KString const& MasterPTY::pts_name() const
{
    return *m_pts_name;
}

ErrorOr<size_t> MasterPTY::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_slave && m_buffer->is_empty())
        return 0;
    return m_buffer->read(buffer, size);
}

ErrorOr<size_t> MasterPTY::write(OpenFileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_slave)
        return EIO;
    m_slave->on_master_write(buffer, size);
    return size;
}

bool MasterPTY::can_read(const OpenFileDescription&, u64) const
{
    if (!m_slave)
        return true;
    return !m_buffer->is_empty();
}

bool MasterPTY::can_write(const OpenFileDescription&, u64) const
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

ErrorOr<size_t> MasterPTY::on_slave_write(const UserOrKernelBuffer& data, size_t size)
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
    if (request == TIOCSWINSZ || request == TIOCGPGRP)
        return m_slave->ioctl(description, request, arg);
    return EINVAL;
}

ErrorOr<NonnullOwnPtr<KString>> MasterPTY::pseudo_path(const OpenFileDescription&) const
{
    return KString::formatted("ptm:{}", m_pts_name);
}

}
