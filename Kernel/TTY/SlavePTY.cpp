/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

static Singleton<SpinlockProtected<SlavePTY::List>> s_all_instances;

SpinlockProtected<SlavePTY::List>& SlavePTY::all_instances()
{
    return s_all_instances;
}

bool SlavePTY::unref() const
{
    bool did_hit_zero = SlavePTY::all_instances().with([&](auto&) {
        if (deref_base())
            return false;
        m_list_node.remove();
        return true;
    });
    if (did_hit_zero)
        delete this;
    return did_hit_zero;
}

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(201, index)
    , m_master(master)
    , m_index(index)
{
    m_tty_name = String::formatted("/dev/pts/{}", m_index);
    auto& process = Process::current();
    set_uid(process.uid());
    set_gid(process.gid());
    set_size(80, 25);

    SlavePTY::all_instances().with([&](auto& list) { list.append(*this); });
}

SlavePTY::~SlavePTY()
{
    dbgln_if(SLAVEPTY_DEBUG, "~SlavePTY({})", m_index);
}

String const& SlavePTY::tty_name() const
{
    return m_tty_name;
}

void SlavePTY::echo(u8 ch)
{
    if (should_echo_input()) {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(&ch);
        [[maybe_unused]] auto result = m_master->on_slave_write(buffer, 1);
    }
}

void SlavePTY::on_master_write(const UserOrKernelBuffer& buffer, size_t size)
{
    auto result = buffer.read_buffered<128>(size, [&](ReadonlyBytes data) {
        for (const auto& byte : data)
            emit(byte, false);
        return data.size();
    });
    if (!result.is_error())
        evaluate_block_conditions();
}

KResultOr<size_t> SlavePTY::on_tty_write(const UserOrKernelBuffer& data, size_t size)
{
    m_time_of_last_write = kgettimeofday().to_truncated_seconds();
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(const OpenFileDescription&, size_t) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(const OpenFileDescription& description, size_t offset) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(description, offset);
}

KResultOr<size_t> SlavePTY::read(OpenFileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t size)
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

FileBlockerSet& SlavePTY::blocker_set()
{
    return m_master->blocker_set();
}

}
