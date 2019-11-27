#include "SlavePTY.h"
#include "MasterPTY.h"
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/Process.h>

//#define SLAVEPTY_DEBUG

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(11, index)
    , m_master(master)
    , m_index(index)
{
    sprintf(m_tty_name, "/dev/pts/%u", m_index);
    set_uid(current->process().uid());
    set_gid(current->process().gid());
    DevPtsFS::register_slave_pty(*this);
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
#ifdef SLAVEPTY_DEBUG
    dbgprintf("~SlavePTY(%u)\n", m_index);
#endif
    DevPtsFS::unregister_slave_pty(*this);
}

StringView SlavePTY::tty_name() const
{
    return m_tty_name;
}

void SlavePTY::echo(u8 ch)
{
    if (should_echo_input()) {
        m_master->on_slave_write(&ch, 1);
    }
}

void SlavePTY::on_master_write(const u8* buffer, ssize_t size)
{
    for (ssize_t i = 0; i < size; ++i)
        emit(buffer[i]);
}

ssize_t SlavePTY::on_tty_write(const u8* data, ssize_t size)
{
    return m_master->on_slave_write(data, size);
}

bool SlavePTY::can_write(const FileDescription&) const
{
    return m_master->can_write_from_slave();
}

bool SlavePTY::can_read(const FileDescription& description) const
{
    if (m_master->is_closed())
        return true;
    return TTY::can_read(description);
}

ssize_t SlavePTY::read(FileDescription& description, u8* buffer, ssize_t size)
{
    if (m_master->is_closed())
        return 0;
    return TTY::read(description, buffer, size);
}

void SlavePTY::close()
{
    m_master->notify_slave_closed({});
}
