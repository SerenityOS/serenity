/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/PrintfImplementation.h>
#include <LibCore/IODevice.h>
#include <LibCore/SyscallUtils.h>
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

IODevice::~IODevice()
{
}

const char* IODevice::error_string() const
{
    return strerror(m_error);
}

int IODevice::read(u8* buffer, int length)
{
    auto read_buffer = read(length);
    if (read_buffer.is_null())
        return 0;
    memcpy(buffer, read_buffer.data(), length);
    return read_buffer.size();
}

ByteBuffer IODevice::read(size_t max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
    auto buffer = ByteBuffer::create_uninitialized(max_size);
    auto* buffer_ptr = (char*)buffer.data();
    size_t remaining_buffer_space = buffer.size();
    size_t taken_from_buffered = 0;
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

bool IODevice::can_read_from_fd() const
{
    // FIXME: Can we somehow remove this once Core::Socket is implemented using non-blocking sockets?
    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(m_fd, &rfds);
    struct timeval timeout {
        0, 0
    };
    int rc = Core::safe_syscall(select, m_fd + 1, &rfds, nullptr, nullptr, &timeout);
    if (rc < 0) {
        // NOTE: We don't set m_error here.
        perror("IODevice::can_read: select");
        return false;
    }
    return FD_ISSET(m_fd, &rfds);
}

bool IODevice::can_read_line()
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

bool IODevice::can_read() const
{
    return !m_buffered_data.is_empty() || can_read_from_fd();
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
        data.append((const u8*)read_buffer, nread);
    }
    if (data.is_empty())
        return {};
    return ByteBuffer::copy(data.data(), data.size());
}

ByteBuffer IODevice::read_line(size_t max_size)
{
    if (m_fd < 0)
        return {};
    if (!max_size)
        return {};
    if (!can_read_line())
        return {};
    if (m_eof) {
        if (m_buffered_data.size() > max_size) {
            dbgprintf("IODevice::read_line: At EOF but there's more than max_size(%d) buffered\n", max_size);
            return {};
        }
        auto buffer = ByteBuffer::copy(m_buffered_data.data(), m_buffered_data.size());
        m_buffered_data.clear();
        return buffer;
    }
    auto line = ByteBuffer::create_uninitialized(max_size + 1);
    size_t line_index = 0;
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

bool IODevice::populate_read_buffer()
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

bool IODevice::close()
{
    if (fd() < 0 || mode() == NotOpen)
        return false;
    int rc = ::close(fd());
    if (rc < 0) {
        set_error(errno);
        return false;
    }
    set_fd(-1);
    set_mode(IODevice::NotOpen);
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

bool IODevice::write(const u8* data, int size)
{
    int rc = ::write(m_fd, data, size);
    if (rc < 0) {
        perror("IODevice::write: write");
        set_error(errno);
        return false;
    }
    return rc == size;
}

int IODevice::printf(const char* format, ...)
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

void IODevice::set_fd(int fd)
{
    if (m_fd == fd)
        return;

    m_fd = fd;
    did_update_fd(fd);
}

bool IODevice::write(const StringView& v)
{
    return write((const u8*)v.characters_without_null_termination(), v.length());
}

}
