/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <ali.mpfard@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/PrintfImplementation.h>
#include <LibCore/FileLikeIODevice.h>
#include <LibCore/Notifier.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <unistd.h>

namespace Core {

FileLikeIODevice::FileLikeIODevice(Object* parent)
    : IODevice(parent)
{
}

FileLikeIODevice::~FileLikeIODevice()
{
}

const char* FileLikeIODevice::error_string() const
{
    return strerror(m_error);
}

size_t FileLikeIODevice::read(Bytes bytes)
{
    if (m_fd < 0) {
        set_fatal_error();
        return 0;
    }

    if (bytes.is_empty())
        return 0;

    auto buffer_ptr = bytes.data();
    size_t remaining_buffer_space = bytes.size();

    auto nread = ::read(m_fd, buffer_ptr, remaining_buffer_space);
    if (nread < 0) {
        set_error(errno);
        return 0;
    }

    if (nread == 0) {
        set_eof(true);
        return 0;
    }

    return static_cast<size_t>(nread);
}

bool FileLikeIODevice::discard_or_error(size_t count)
{
    // FIXME: This can probably be implemented in a less wasteful way...
    auto buffer = ByteBuffer::create_uninitialized(count);
    if (read(buffer.bytes()) < count) {
        set_recoverable_error();
        return false;
    }
    return true;
}

ByteBuffer FileLikeIODevice::read(size_t max_size)
{
    auto buffer = ByteBuffer::create_uninitialized(max_size);
    buffer.trim(read(buffer.bytes()));
    return buffer;
}

bool FileLikeIODevice::can_read_from_fd() const
{
    // FIXME: Can we somehow remove this once Core::Socket is implemented using non-blocking sockets?
    fd_set rfds;
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

bool FileLikeIODevice::can_read() const
{
    return can_read_from_fd();
}

ByteBuffer FileLikeIODevice::read_all()
{
    off_t file_size = 0;
    struct stat st;
    int rc = fstat(fd(), &st);
    if (rc == 0)
        file_size = st.st_size;

    Vector<u8> data;
    data.ensure_capacity(file_size);

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

bool FileLikeIODevice::close()
{
    if (fd() < 0 || mode() == OpenMode::NotOpen)
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

bool FileLikeIODevice::seek(i64 offset, SeekMode mode, off_t* pos)
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
    m_eof = false;
    if (pos)
        *pos = rc;
    return true;
}

bool FileLikeIODevice::truncate(off_t size)
{
    int rc = ftruncate(m_fd, size);
    if (rc < 0) {
        set_error(errno);
        return false;
    }
    return true;
}

size_t FileLikeIODevice::write(ReadonlyBytes bytes)
{
    auto rc = ::write(m_fd, bytes.data(), bytes.size());
    if (rc < 0) {
        set_error(errno);
        perror("FileLikeIODevice::write: write");
        return 0;
    }
    return static_cast<size_t>(rc);
}

void FileLikeIODevice::set_fd(int fd)
{
    if (m_fd == fd)
        return;

    m_fd = fd;
    did_update_fd(fd);
}

RefPtr<AbstractNotifier> FileLikeIODevice::make_notifier(unsigned event_mask)
{
    return Notifier::construct(m_fd, event_mask, this);
}

}
