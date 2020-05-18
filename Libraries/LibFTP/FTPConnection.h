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

#pragma once

#include <AK/String.h>
#include <LibCore/TCPSocket.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

#include "FTPRequest.h"
#include "FTPResponse.h"

namespace FTP {
class FTPConnection : public RefCounted<FTPConnection> {
public:
    // FIXME: passive/active connections

    enum DataRepresentation {
        ASCII,
        Binary
    };
    enum TransferMode {
        Stream,
        Block,
        Compressed
    };

    FTPConnection();
    ~FTPConnection();

    void connect(const URL&, Function<void()> callback);
    void disconnect();
    void login(const String& login, const String& password);

    void set_transfer_mode(TransferMode);
    TransferMode transfer_mode() const { return m_transfer_mode; }
    void set_data_representation(DataRepresentation);
    DataRepresentation data_representation() const { return m_data_representation; }
    void set_remote_directory(const String&);
    const String& ensure_remote_directory();
    void set_local_root_directory(const String&);
    const String& local_root_directory() const { return m_local_root_directory; }
    void set_callback(Function<void(const FTPResponse&)>);

    Vector<String> list_files();

    // local file = m_local_root_directory + m_remote_directory
    void download_file(const String& remote, const String& local);
    void upload_file(const String& local, const String& remote);

    // FIXME: rename file, remove file
    void rename_file(const String&);
    void remove_file(const String&);

private:
    Optional<FTPResponse> send_command(const FTPRequest&);
    void on_ready_to_read();

    NonnullRefPtr<Core::TCPSocket> m_socket;
    String m_local_root_directory;
    Optional<String> m_remote_directory;
    TransferMode m_transfer_mode;
    DataRepresentation m_data_representation;
    Function<void(const FTPRequest&, const FTPResponse&)> m_callback;
    Protocol::Client m_protocol_client;
};
}
