#include "SlavePTY.h"
#include "MasterPTY.h"

SlavePTY::SlavePTY(MasterPTY& master, unsigned index)
    : TTY(11, index)
    , m_master(master)
    , m_index(index)
{
    set_size(80, 25);
}

SlavePTY::~SlavePTY()
{
}

String SlavePTY::tty_name() const
{
    char buffer[32];
    ksprintf(buffer, "/dev/pts%u", m_index);
    return buffer;
}

void SlavePTY::on_master_write(const byte* buffer, size_t size)
{
    for (size_t i = 0; i < size; ++i)
        emit(buffer[i]);
}

void SlavePTY::on_tty_write(const byte* data, size_t size)
{
    m_master.on_slave_write(data, size);
}

bool SlavePTY::can_write(Process& process) const
{
    return m_master.can_write(process);
}
