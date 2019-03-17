#include <LibGUI/GIODevice.h>
#include <unistd.h>

GIODevice::GIODevice(GObject* parent)
    : GObject(parent)
{
}

GIODevice::~GIODevice()
{
}

const char* GIODevice::error_string() const
{
    return strerror(m_error);
}

ByteBuffer GIODevice::read(int max_size)
{
    if (m_fd < 0)
        return { };
    if (!max_size)
        return { };
    auto buffer = ByteBuffer::create_uninitialized(max_size);
    auto* buffer_ptr = (char*)buffer.pointer();
    int remaining_buffer_space = buffer.size();
    if (!m_buffered_data.is_empty()) {
        int taken_from_buffered = min(remaining_buffer_space, m_buffered_data.size());
        memcpy(buffer_ptr, m_buffered_data.data(), taken_from_buffered);
        Vector<byte> new_buffered_data;
        new_buffered_data.append(m_buffered_data.data() + taken_from_buffered, m_buffered_data.size() - taken_from_buffered);
        m_buffered_data = move(new_buffered_data);
        remaining_buffer_space -= taken_from_buffered;
        buffer_ptr += taken_from_buffered;
    }
    if (!remaining_buffer_space)
        return buffer;
    int nread = ::read(m_fd, buffer_ptr, remaining_buffer_space);
    if (nread < 0) {
        set_error(errno);
        return { };
    }
    buffer.trim(nread);
    return buffer;
}

ByteBuffer GIODevice::read_line(int max_size)
{
    if (m_fd < 0)
        return { };
    if (!max_size)
        return { };
    auto line = ByteBuffer::create_uninitialized(max_size);
    int line_index = 0;
    while (line_index < line.size()) {
        if (line_index >= m_buffered_data.size()) {
            if (!populate_read_buffer())
                return { };
        }
        byte ch = m_buffered_data[line_index];
        line[line_index++] = ch;
        if (ch == '\n') {
            Vector<byte> new_buffered_data;
            new_buffered_data.append(m_buffered_data.data() + line_index, m_buffered_data.size() - line_index);
            m_buffered_data = move(new_buffered_data);
            line.trim(line_index);
            return line;
        }
    }
    return { };
}

bool GIODevice::populate_read_buffer()
{
    if (m_fd < 0)
        return false;
    auto buffer = ByteBuffer::create_uninitialized(1024);
    int nread = ::read(m_fd, buffer.pointer(), buffer.size());
    if (nread < 0) {
        set_error(errno);
        return false;
    }
    if (nread == 0) {
        set_eof(true);
        return false;
    }
    m_buffered_data.append(buffer.pointer(), buffer.size());
    return true;
}
