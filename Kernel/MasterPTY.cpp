#include "MasterPTY.h"
#include "SlavePTY.h"

MasterPTY::MasterPTY(unsigned index)
    : CharacterDevice(10, index)
    , m_slave(*new SlavePTY(*this, index))
    , m_index(index)
{
}

MasterPTY::~MasterPTY()
{
}

String MasterPTY::pts_name() const
{
    char buffer[32];
    ksprintf(buffer, "/dev/pts/%u", m_index);
    return buffer;
}

ssize_t MasterPTY::read(Process&, byte* buffer, size_t size)
{
    return m_buffer.read(buffer, size);
}

ssize_t MasterPTY::write(Process&, const byte* buffer, size_t size)
{
    m_slave.on_master_write(buffer, size);
    return size;
}

bool MasterPTY::can_read(Process&) const
{
    return !m_buffer.is_empty();
}

bool MasterPTY::can_write(Process&) const
{
    return true;
}

void MasterPTY::on_slave_write(const byte* data, size_t size)
{
    m_buffer.write(data, size);
}

bool MasterPTY::can_write_from_slave() const
{
    return m_buffer.bytes_in_write_buffer() < 4096;
}
