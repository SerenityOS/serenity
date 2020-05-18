/*
 * Copyright (c) 2020, sppmacd <sppmacd@gmail.com>
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

#include "FTPConnection.h"

#include <LibCore/TCPSocket.h>

namespace FTP {
FTPConnection::FTPConnection()
    : m_socket(Core::TCPSocket::construct())
{
}
FTPConnection::~FTPConnection()
{
}

void FTPConnection::connect(const URL& url, Function<void()> callback)
{
    auto address = url.host();
    auto port = url.port();
    m_socket->on_connected = [&]() {
        // FIXME: auto login and set path
        callback();
    };
    m_socket->connect(address, port);
}
void FTPConnection::disconnect()
{
    m_socket->close();
}
void FTPConnection::set_transfer_mode(TransferMode mode)
{
    ASSERT_NOT_REACHED();
    return;

    m_transfer_mode = mode;
    // FIXME: error handling, mode to FTP code

    FTPRequest request;
    request.set_command(FTPRequest::TransferMode);
    //request.add_arg(mode);
    auto response = send_command(request);
}
void FTPConnection::set_data_representation(DataRepresentation data_representation)
{
    ASSERT_NOT_REACHED();
    return;

    m_data_representation = data_representation;
    // FIXME: error handling, mode to FTP code

    FTPRequest request;
    request.set_command(FTPRequest::TransferType);
    //request.add_arg(data_representation);
    auto response = send_command(request);
}
void FTPConnection::login(const String& login, const String& password)
{
    // FIXME: error handling

    FTPRequest request;
    request.set_command(FTPRequest::Username);
    request.add_arg(login);
    auto response = send_command(request);

    request.set_command(FTPRequest::Password);
    request.add_arg(password);
    response = send_command(request);
}
void FTPConnection::set_remote_directory(const String& remote_dir)
{
    FTPRequest request;
    request.set_command(FTPRequest::PrintWorkingDirectory);
    request.add_arg(remote_dir);
    send_command(request);

    m_remote_directory = remote_dir;
}
void FTPConnection::set_local_root_directory(const String& local_root)
{
    m_local_root_directory = local_root;
}
const String& FTPConnection::ensure_remote_directory()
{
    if (!m_remote_directory.has_value()) {
        FTPRequest request;
        request.set_command(FTPRequest::PrintWorkingDirectory);
        auto response = send_command(request).value();
        const String& message = response.message();

        // Format: "..." is the current directory
        //          -0- ------------1------------
        Vector<String> split = message.split('"');

        m_remote_directory = split[0];
    }
    return m_remote_directory.value();
}
Vector<String> FTPConnection::list_files()
{
    FTPRequest request;
    request.set_command(FTPRequest::ListFiles);
    auto response = send_command(request);
    // FIXME: error handling
    // FIXME: ProtocolServer implementation
    return Vector<String>();
}
void FTPConnection::download_file(const String& remote, const String& local)
{
    (void)local;
    FTPRequest request;
    request.set_command(FTPRequest::RetrieveFile);
    request.add_arg(remote);
    auto response = send_command(request);
    // FIXME: error handling
    // FIXME: ProtocolServer implementation
}
void FTPConnection::upload_file(const String& local, const String& remote)
{
    (void)local;
    FTPRequest request;
    request.set_command(FTPRequest::StoreFile);
    request.add_arg(remote);
    auto response = send_command(request);
    // FIXME: error handling
    // FIXME: ProtocolServer implementation
}
void FTPConnection::rename_file(const String&)
{
    ASSERT_NOT_REACHED();
}
void FTPConnection::remove_file(const String& remote)
{
    // FIXME: check if it's directory or file and select correct command
    FTPRequest request;
    request.set_command(FTPRequest::Delete);
    request.add_arg(remote);
    auto response = send_command(request);
    // FIXME: error handling
}
Optional<FTPResponse> FTPConnection::send_command(const FTPRequest& request)
{
    const ByteBuffer& data = request.to_raw_request();
    dbg() << "FTP::FTPConnection::send_command(): request: command=" << request.command();

    // FIXME: handle specially download and upload
    //m_socket->on_ready_to_read = move(on_ready_to_read);
    bool ok = m_socket->send(data);
    if (!ok) {
        //ASSERT_NOT_REACHED();
        dbg() << "FTPConnection::send_command(): error sending data via PI";
        return Optional<FTPResponse>();
    }

    ByteBuffer received = m_socket->receive(256);
    NonnullRefPtr<FTPResponse> response = FTPResponse::create(move(received));
    dbg() << "FTP::FTPConnection::send_command(): response: code=" << response->code() << ", message='" << response->message();
    m_callback(request, response);
    return response;
}
void FTPConnection::on_ready_to_read()
{
    ASSERT_NOT_REACHED();
}
}
