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

 // modified by sppmacd

#include <LibCore/Gzip.h>
#include <LibCore/TCPSocket.h>
#include <LibFTP/FTPJob.h>
#include <LibFTP/FTPResponse.h>
#include <stdio.h>
#include <unistd.h>

//#define FTPJOB_DEBUG

namespace FTP {
void FTPJob::start()
{
    ASSERT(!m_socket);
    m_socket = Core::TCPSocket::construct(this);
    m_socket->on_connected = [this] {
#ifdef CFTPJOB_DEBUG
        dbg() << "FTPJob: on_connected callback";
#endif
        on_socket_connected();
    };
    bool success = m_socket->connect(m_request.url().host(), m_request.url().port());
    if (!success) {
        deferred_invoke([this](auto&) {
            return did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    }
}

void FTPJob::shutdown()
{
    if (!m_socket)
        return;
    m_socket->on_ready_to_read = nullptr;
    m_socket->on_connected = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}

void FTPJob::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_ready_to_read = move(callback);
}

void FTPJob::register_on_ready_to_write(Function<void()> callback)
{
    // There is no need to wait, the connection is already established
    callback();
}

bool FTPJob::can_read_line() const
{
    return m_socket->can_read_line();
}

ByteBuffer FTPJob::read_line(size_t size)
{
    return m_socket->read_line(size);
}

ByteBuffer FTPJob::receive(size_t size)
{
    return m_socket->receive(size);
}

bool FTPJob::can_read() const
{
    return m_socket->can_read();
}

bool FTPJob::eof() const
{
    return m_socket->eof();
}

bool FTPJob::write(const ByteBuffer& data)
{
    return m_socket->write(data);
}

}
*/
