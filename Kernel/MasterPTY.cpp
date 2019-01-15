#include "MasterPTY.h"
#include "SlavePTY.h"

MasterPTY::MasterPTY(unsigned index)
    : CharacterDevice(10, index)
    , m_index(index)
{

}

MasterPTY::~MasterPTY()
{
}

String MasterPTY::pts_name() const
{
    dbgprintf("MasterPTY::pts_name requested for index %u!\n", m_index);
    char buffer[32];
    ksprintf(buffer, "/dev/pts%u", m_index);
    return buffer;
}

ssize_t MasterPTY::read(byte* buffer, size_t size)
{
    return m_buffer.read(buffer, size);
}

ssize_t MasterPTY::write(const byte* buffer, size_t size)
{
    m_slave->on_master_write(buffer, size);
    return size;
}

bool MasterPTY::has_data_available_for_reading(Process&) const
{
    return !m_buffer.is_empty();
}

void MasterPTY::on_slave_write(const byte* data, size_t size)
{
    m_buffer.write(data, size);
}
