#include <AK/PrintfImplementation.h>
#include <LibCore/CIODevice.h>
#include <errno.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>

CIODevice::CIODevice(CObject* parent)
    : CObject(parent)
{
}

CIODevice::~CIODevice()
{
}

const char* CIODevice::error_string() const
{
    return strerror(m_error);
}

ByteBuffer CIODevice::read(int max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
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
        return {};
    }
    if (nread == 0) {
        set_eof(true);
        return {};
    }
    buffer.trim(nread);
    return buffer;
}

bool CIODevice::can_read_from_fd() const
{
    // FIXME: Can we somehow remove this once CSocket is implemented using non-blocking sockets?
    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(m_fd, &rfds);
    struct timeval timeout {
        0, 0
    };
    int rc = select(m_fd + 1, &rfds, nullptr, nullptr, &timeout);
    if (rc < 0) {
        // NOTE: We don't set m_error here.
        perror("CIODevice::can_read: select");
        return false;
    }
    return FD_ISSET(m_fd, &rfds);
}

bool CIODevice::can_read_line()
{
    if (m_eof && !m_buffered_data.is_empty())
        return true;
    if (m_buffered_data.contains_slow('\n'))
        return true;
    if (!can_read_from_fd())
        return false;
    populate_read_buffer();
    return m_buffered_data.contains_slow('\n');
}

bool CIODevice::can_read() const
{
    return !m_buffered_data.is_empty() || can_read_from_fd();
}

ByteBuffer CIODevice::read_all()
{
    ByteBuffer buffer;
    if (!m_buffered_data.is_empty()) {
        buffer = ByteBuffer::copy(m_buffered_data.data(), m_buffered_data.size());
        m_buffered_data.clear();
    }

    while (can_read_from_fd()) {
        char read_buffer[4096];
        int nread = ::read(m_fd, read_buffer, sizeof(read_buffer));
        if (nread < 0) {
            set_error(nread);
            return buffer;
        }
        if (nread == 0) {
            set_eof(true);
            break;
        }
        buffer.append(read_buffer, nread);
    }
    return buffer;
}

ByteBuffer CIODevice::read_line(int max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
    if (!can_read_line())
        return {};
    if (m_eof) {
        if (m_buffered_data.size() > max_size) {
            dbgprintf("CIODevice::read_line: At EOF but there's more than max_size(%d) buffered\n", max_size);
            return {};
        }
        auto buffer = ByteBuffer::copy(m_buffered_data.data(), m_buffered_data.size());
        m_buffered_data.clear();
        return buffer;
    }
    auto line = ByteBuffer::create_uninitialized(max_size + 1);
    int line_index = 0;
    while (line_index < max_size) {
        byte ch = m_buffered_data[line_index];
        line[line_index++] = ch;
        if (ch == '\n') {
            Vector<byte> new_buffered_data;
            new_buffered_data.append(m_buffered_data.data() + line_index, m_buffered_data.size() - line_index);
            m_buffered_data = move(new_buffered_data);
            line[line_index] = '\0';
            line.trim(line_index + 1);
            return line;
        }
    }
    return {};
}

bool CIODevice::populate_read_buffer()
{
    if (m_fd < 0)
        return false;
    byte buffer[1024];
    int nread = ::read(m_fd, buffer, sizeof(buffer));
    if (nread < 0) {
        set_error(errno);
        return false;
    }
    if (nread == 0) {
        set_eof(true);
        return false;
    }
    m_buffered_data.append(buffer, nread);
    return true;
}

bool CIODevice::close()
{
    if (fd() < 0 || mode() == NotOpen)
        return false;
    int rc = ::close(fd());
    if (rc < 0) {
        set_error(rc);
        return false;
    }
    set_fd(-1);
    set_mode(CIODevice::NotOpen);
    return true;
}

bool CIODevice::seek(signed_qword offset, SeekMode mode, off_t* pos)
{
    int m = SEEK_SET;
    switch (mode) {
    case SeekMode::SetPosition:
        m = SEEK_SET;
        break;
    case SeekMode::FromCurrentPosition:
        m = SEEK_CUR;
        break;
    case SeekMode::FromEndPosition:
        m = SEEK_END;
        break;
    }
    off_t rc = lseek(m_fd, offset, m);
    if (rc < 0) {
        set_error(errno);
        if (pos)
            *pos = -1;
        return false;
    }
    m_buffered_data.clear();
    m_eof = false;
    if (pos)
        *pos = rc;
    return true;
}

bool CIODevice::write(const byte* data, int size)
{
    int rc = ::write(m_fd, data, size);
    if (rc < 0) {
        perror("CIODevice::write: write");
        set_error(errno);
        return false;
    }
    return rc == size;
}

int CIODevice::printf(const char* format, ...)
{
    va_list ap;
    va_start(ap, format);
    // FIXME: We're not propagating write() failures to client here!
    int ret = printf_internal([this](char*&, char ch) {
        int rc = write((const byte*)&ch, 1);
        if (rc < 0)
            dbgprintf("CIODevice::printf: write: %s\n", strerror(errno));
    },
        nullptr, format, ap);
    va_end(ap);
    return ret;
}
