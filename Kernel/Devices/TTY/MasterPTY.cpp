/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/MasterPTY.h>
#include <Kernel/Devices/TTY/PTYMultiplexer.h>
#include <Kernel/Devices/TTY/SlavePTY.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<MasterPTY>> MasterPTY::try_create(unsigned int index)
{
    auto buffer = TRY(DoubleBuffer::try_create("MasterPTY: Buffer"sv));
    auto master_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MasterPTY(index, move(buffer))));
    auto credentials = Process::current().credentials();
    auto slave_pty = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SlavePTY(*master_pty, credentials->uid(), credentials->gid(), index)));
    master_pty->m_slave.with([&slave_pty](auto& slave) {
        slave = *slave_pty;
    });
    TRY(master_pty->after_inserting());
    TRY(slave_pty->after_inserting());
    return master_pty;
}

MasterPTY::MasterPTY(unsigned index, NonnullOwnPtr<DoubleBuffer> buffer)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::MasterPTY, index)
    , m_index(index)
    , m_buffer(move(buffer))
{
    m_buffer->set_unblock_callback([this]() {
        // Note that has_slave() takes and then releases the m_slave spinlock.
        // Not holding the spinlock while calling evaluate_block_conditions is legal,
        // as the call will trigger a check to see if waiters may be unblocked,
        // and if it was called spuriously (i.e. because the slave disappeared between
        // calling the unblock callback and the actual block condition evaluation),
        // the waiters will simply not unblock.
        if (has_slave())
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
    // Note that has_slave() takes and then releases the m_slave spinlock.
    // Not holding the spinlock while calling m_buffer->read is legal, because slave starts non-null,
    // and can only change its state to null once  (and never back to non-null) in notify_slave_closed.
    // So if the check happens, and it returns non-null, and then it turns null concurrently,
    // and we call m_buffer->read, the behaviour from the perspective of the read caller is
    // the same as if the slave turned null after we called m_buffer->read. On the other hand,
    // if the check happens and returns null, then it can't possibly change to non-null after.
    if (!has_slave() && m_buffer->is_empty())
        return 0;
    return m_buffer->read(buffer, size);
}

ErrorOr<size_t> MasterPTY::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    return m_slave.with([&](auto& slave) -> ErrorOr<size_t> {
        if (!slave)
            return EIO;
        slave->on_master_write(buffer, size);
        return size;
    });
}

bool MasterPTY::can_read(OpenFileDescription const&, u64) const
{
    return m_slave.with([this](auto& slave) -> bool {
        if (!slave)
            return true;
        return !m_buffer->is_empty();
    });
}

bool MasterPTY::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

void MasterPTY::notify_slave_closed(Badge<SlavePTY>)
{
    m_slave.with([this](auto& slave) {
        dbgln_if(MASTERPTY_DEBUG, "MasterPTY({}): slave closed, my retains: {}, slave retains: {}", m_index, ref_count(), slave->ref_count());
        // +1 ref for my MasterPTY::m_slave
        // +1 ref for OpenFileDescription::m_device
        if (slave->ref_count() == 2)
            slave = nullptr;
    });
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

    m_slave.with([](auto& slave) {
        if (slave)
            slave->hang_up();
    });

    return {};
}

ErrorOr<void> MasterPTY::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    TRY(Process::current().require_promise(Pledge::tty));
    return m_slave.with([&](auto& slave) -> ErrorOr<void> {
        if (!slave)
            return EIO;
        switch (request) {
        case TIOCGPTN: {
            int master_pty_index = index();
            return copy_to_user(static_ptr_cast<int*>(arg), &master_pty_index);
        }
        case TIOCSWINSZ:
        case TIOCGPGRP:
            return slave->ioctl(description, request, arg);
        default:
            return EINVAL;
        }
    });
}

ErrorOr<NonnullOwnPtr<KString>> MasterPTY::pseudo_path(OpenFileDescription const&) const
{
    return KString::formatted("ptm:{}", m_index);
}

}
