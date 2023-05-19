/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibCore/IODevice.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <unistd.h>

namespace Core {

IODevice::IODevice(Object* parent)
    : Object(parent)
{
}

char const* IODevice::error_string() const
{
    return strerror(m_error);
}

int IODevice::read(u8* buffer, int length)
{
    auto read_buffer = read(length);
    memcpy(buffer, read_buffer.data(), length);
    return read_buffer.size();
}

ByteBuffer IODevice::read(size_t max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};

    if (m_buffered_data.size() < max_size)
        populate_read_buffer(max(max_size - m_buffered_data.size(), 1024));

    auto size = min(max_size, m_buffered_data.size());
    auto buffer_result = ByteBuffer::create_uninitialized(size);
    if (buffer_result.is_error()) {
        dbgln("IODevice::read: Not enough memory to allocate a buffer of {} bytes", size);
        return {};
    }
    auto buffer = buffer_result.release_value();
    auto* buffer_ptr = (char*)buffer.data();

    memcpy(buffer_ptr, m_buffered_data.data(), size);
    m_buffered_data.remove(0, size);

    return buffer;
}

bool IODevice::can_read_from_fd() const
{
    // FIXME: Can we somehow remove this once Core::Socket is implemented using non-blocking sockets?
    fd_set rfds {};
    FD_ZERO(&rfds);
    FD_SET(m_fd, &rfds);
    struct timeval timeout {
        0, 0
    };

    for (;;) {
        if (select(m_fd + 1, &rfds, nullptr, nullptr, &timeout) < 0) {
            if (errno == EINTR)
                continue;
            perror("IODevice::can_read_from_fd: select");
            return false;
        }
        break;
    }
    return FD_ISSET(m_fd, &rfds);
}

bool IODevice::can_read_line() const
{
    if (m_eof && !m_buffered_data.is_empty())
        return true;

    if (m_buffered_data.contains_slow('\n'))
        return true;

    if (!can_read_from_fd())
        return false;

    while (true) {
        // Populate buffer until a newline is found or we reach EOF.

        auto previous_buffer_size = m_buffered_data.size();
        populate_read_buffer();
        auto new_buffer_size = m_buffered_data.size();

        if (m_error)
            return false;

        if (m_eof)
            return !m_buffered_data.is_empty();

        if (m_buffered_data.contains_in_range('\n', previous_buffer_size, new_buffer_size - 1))
            return true;
    }
}

ByteBuffer IODevice::read_all()
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
        data.append((u8 const*)read_buffer, nread);
    }

    auto result = ByteBuffer::copy(data);
    if (!result.is_error())
        return result.release_value();

    set_error(ENOMEM);
    return {};
}

DeprecatedString IODevice::read_line(size_t max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
    if (!can_read_line())
        return {};
    if (m_eof) {
        if (m_buffered_data.size() > max_size) {
            dbgln("IODevice::read_line: At EOF but there's more than max_size({}) buffered", max_size);
            return {};
        }
        auto line = DeprecatedString((char const*)m_buffered_data.data(), m_buffered_data.size(), Chomp);
        m_buffered_data.clear();
        return line;
    }
    auto line_result = ByteBuffer::create_uninitialized(max_size + 1);
    if (line_result.is_error()) {
        dbgln("IODevice::read_line: Not enough memory to allocate a buffer of {} bytes", max_size + 1);
        return {};
    }
    auto line = line_result.release_value();
    size_t line_index = 0;
    while (line_index < max_size) {
        u8 ch = m_buffered_data[line_index];
        line[line_index++] = ch;
        if (ch == '\n') {
            Vector<u8> new_buffered_data;
            new_buffered_data.append(m_buffered_data.data() + line_index, m_buffered_data.size() - line_index);
            m_buffered_data = move(new_buffered_data);
            line.resize(line_index);
            return DeprecatedString::copy(line, Chomp);
        }
    }
    return {};
}

bool IODevice::populate_read_buffer(size_t size) const
{
    if (m_fd < 0)
        return false;
    if (!size)
        return false;

    auto buffer_result = ByteBuffer::create_uninitialized(size);
    if (buffer_result.is_error()) {
        dbgln("IODevice::populate_read_buffer: Not enough memory to allocate a buffer of {} bytes", size);
        return {};
    }
    auto buffer = buffer_result.release_value();
    auto* buffer_ptr = (char*)buffer.data();

    int nread = ::read(m_fd, buffer_ptr, size);
    if (nread < 0) {
        set_error(errno);
        return false;
    }
    if (nread == 0) {
        set_eof(true);
        return false;
    }
    m_buffered_data.append(buffer.data(), nread);
    return true;
}

bool IODevice::close()
{
    if (fd() < 0 || m_mode == OpenMode::NotOpen)
        return false;
    int rc = ::close(fd());
    if (rc < 0) {
        set_error(errno);
        return false;
    }
    set_fd(-1);
    set_mode(OpenMode::NotOpen);
    return true;
}

bool IODevice::seek(i64 offset, SeekMode mode, off_t* pos)
{
    int m = SEEK_SET;
    switch (mode) {
    case SeekMode::SetPosition:
        m = SEEK_SET;
        break;
    case SeekMode::FromCurrentPosition:
        m = SEEK_CUR;
        offset -= m_buffered_data.size();
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

bool IODevice::write(u8 const* data, int size)
{
    int rc = ::write(m_fd, data, size);
    if (rc < 0) {
        set_error(errno);
        perror("IODevice::write: write");
        return false;
    }
    return rc == size;
}

void IODevice::set_fd(int fd)
{
    if (m_fd == fd)
        return;

    m_fd = fd;
}

}
