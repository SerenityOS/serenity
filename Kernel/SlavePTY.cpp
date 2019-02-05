#include "SlavePTY.h"
#include "MasterPTY.h"
#include "DevPtsFS.h"
#include <Kernel/Process.h>

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(11, index)
    , m_master(master)
    , m_index(index)
{
    set_uid(current->uid());
    set_gid(current->gid());
    VFS::the().register_character_device(*this);
    DevPtsFS::the().register_slave_pty(*this);
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
    dbgprintf("~SlavePTY(%u)\n", m_index);
    DevPtsFS::the().unregister_slave_pty(*this);
    VFS::the().unregister_character_device(*this);
}

String SlavePTY::tty_name() const
{
    return String::format("/dev/pts/%u", m_index);
}

void SlavePTY::on_master_write(const byte* buffer, size_t size)
{
    for (size_t i = 0; i < size; ++i)
        emit(buffer[i]);
}

ssize_t SlavePTY::on_tty_write(const byte* data, size_t size)
{
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(Process&) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(Process& process) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(process);
}

ssize_t SlavePTY::read(Process& process, byte* buffer, size_t size)
{
    if (m_master->is_closed())
        return 0;
    return TTY::read(process, buffer, size);
}

void SlavePTY::close()
{
    m_master->notify_slave_closed(Badge<SlavePTY>());
}
