/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/MasterPTY.h>
#include <Kernel/Devices/TTY/SlavePTY.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static Singleton<SpinlockProtected<SlavePTY::List, LockRank::None>> s_all_instances;

SpinlockProtected<SlavePTY::List, LockRank::None>& SlavePTY::all_instances()
{
    return s_all_instances;
}

bool SlavePTY::unref() const
{
    bool did_hit_zero = SlavePTY::all_instances().with([&](auto&) {
        if (deref_base())
            return false;
        m_list_node.remove();
        const_cast<SlavePTY&>(*this).revoke_weak_ptrs();
        return true;
    });
    if (did_hit_zero) {
        const_cast<SlavePTY&>(*this).will_be_destroyed();
        delete this;
    }
    return did_hit_zero;
}

SlavePTY::SlavePTY(NonnullRefPtr<MasterPTY> master, UserID uid, GroupID gid, unsigned index)
    : TTY(MajorAllocation::CharacterDeviceFamily::SlavePTY, index)
    , m_master(move(master))
    , m_index(index)
    , m_uid(uid)
    , m_gid(gid)
{
    set_size(80, 25);
    SlavePTY::all_instances().with([&](auto& list) { list.append(*this); });
}

SlavePTY::~SlavePTY()
{
    dbgln_if(SLAVEPTY_DEBUG, "~SlavePTY({})", m_index);
}

ErrorOr<NonnullOwnPtr<KString>> SlavePTY::pseudo_name() const
{
    return KString::formatted("pts:{}", m_index);
}

void SlavePTY::echo(u8 ch)
{
    if (should_echo_input()) {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(&ch);
        [[maybe_unused]] auto result = m_master->on_slave_write(buffer, 1);
    }
}

void SlavePTY::on_master_write(UserOrKernelBuffer const& buffer, size_t size)
{
    auto result = buffer.read_buffered<128>(size, [&](ReadonlyBytes data) {
        for (auto const& byte : data)
            emit(byte, false);
        return data.size();
    });
    if (!result.is_error())
        evaluate_block_conditions();
}

ErrorOr<size_t> SlavePTY::on_tty_write(UserOrKernelBuffer const& data, size_t size)
{
    m_time_of_last_write = kgettimeofday();
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(OpenFileDescription const&, u64) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(OpenFileDescription const& description, u64 offset) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(description, offset);
}

ErrorOr<size_t> SlavePTY::read(OpenFileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t size)
{
    if (m_master->is_closed())
        return 0;
    return TTY::read(description, offset, buffer, size);
}

ErrorOr<void> SlavePTY::close()
{
    m_master->notify_slave_closed({});
    return {};
}

FileBlockerSet& SlavePTY::blocker_set()
{
    return m_master->blocker_set();
}

}
