#include <AK/PrintfImplementation.h>
#include <LibCore/CIODevice.h>
#include <LibCore/CSyscallUtils.h>
#include <errno.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/stat.h>
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

int CIODevice::read(u8* buffer, int length)
{
    auto read_buffer = read(length);
    if (read_buffer.is_null())
        return 0;
    memcpy(buffer, read_buffer.data(), length);
    return read_buffer.size();
}

ByteBuffer CIODevice::read(int max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
    auto buffer = ByteBuffer::create_uninitialized(max_size);
    auto* buffer_ptr = (char*)buffer.data();
    int remaining_buffer_space = buffer.size();
    int taken_from_buffered = 0;
    if (!m_buffered_data.is_empty()) {
        taken_from_buffered = min(remaining_buffer_space, m_buffered_data.size());
        memcpy(buffer_ptr, m_buffered_data.data(), taken_from_buffered);
        Vector<u8> new_buffered_data;
        new_buffered_data.append(m_buffered_data.data() + taken_from_buffered, m_buffered_data.size() - taken_from_buffered);
        m_buffered_data = move(new_buffered_data);
        remaining_buffer_space -= taken_from_buffered;
        buffer_ptr += taken_from_buffered;
    }
    if (!remaining_buffer_space)
        return buffer;
    int nread = ::read(m_fd, buffer_ptr, remaining_buffer_space);
    if (nread < 0) {
        if (taken_from_buffered) {
            buffer.trim(taken_from_buffered);
            return buffer;
        }
        set_error(errno);
        return {};
    }
    if (nread == 0) {
        set_eof(true);
        if (taken_from_buffered) {
            buffer.trim(taken_from_buffered);
            return buffer;
        }
        return {};
    }
    buffer.trim(taken_from_buffered + nread);
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
    int rc = CSyscallUtils::safe_syscall(select, m_fd + 1, &rfds, nullptr, nullptr, &timeout);
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
    off_t file_size = 0;
    struct stat st;
    int rc = fstat(fd(), &st);
    if (rc == 0)
        file_size = st.st_size;

    Vector<u8> data;
    data.ensure_capacity(file_size);

    if (!m_buffered_data.is_empty()) {
        data.append(m_buffered_data.data(), m_buffered_data.size());
        m_buffered_data.clear();
    }

    while (true) {
        char read_buffer[4096];
        int nread = ::read(m_fd, read_buffer, sizeof(read_buffer));
        if (nread < 0) {
            set_error(errno);
            break;
        }
        if (nread == 0) {
            set_eof(true);
            break;
        }
        data.append((const u8*)read_buffer, nread);
    }
    if (data.is_empty())
        return {};
    return ByteBuffer::copy(data.data(), data.size());
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
        u8 ch = m_buffered_data[line_index];
        line[line_index++] = ch;
        if (ch == '\n') {
            Vector<u8> new_buffered_data;
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
    u8 buffer[1024];
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
        set_error(errno);
        return false;
    }
    set_fd(-1);
    set_mode(CIODevice::NotOpen);
    return true;
}

bool CIODevice::seek(i64 offset, SeekMode mode, off_t* pos)
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

bool CIODevice::write(const u8* data, int size)
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
        write((const u8*)&ch, 1);
    },
        nullptr, format, ap);
    va_end(ap);
    return ret;
}

void CIODevice::set_fd(int fd)
{
    if (m_fd == fd)
        return;

    m_fd = fd;
    did_update_fd(fd);
}
