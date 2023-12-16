/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <FileSystemAccessServer/FileSystemAccessClientEndpoint.h>
#include <FileSystemAccessServer/FileSystemAccessServerEndpoint.h>
#include <LibCore/Forward.h>
#include <LibGUI/FileTypeFilter.h>
#include <LibGUI/Forward.h>
#include <LibIPC/ConnectionFromClient.h>

namespace FileSystemAccessServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>);

    virtual void request_file_read_only_approved(i32, ByteString const&) override;
    virtual void request_file(i32, i32, i32, ByteString const&, Core::File::OpenMode) override;
    virtual void prompt_open_file(i32, i32, i32, ByteString const&, ByteString const&, Core::File::OpenMode, Optional<Vector<GUI::FileTypeFilter>> const&) override;
    virtual void prompt_save_file(i32, i32, i32, ByteString const&, ByteString const&, ByteString const&, Core::File::OpenMode) override;

    void prompt_helper(i32, Optional<ByteString> const&, Core::File::OpenMode);

    enum class ShouldPrompt {
        No,
        Yes
    };
    void request_file_handler(i32, i32, i32, ByteString const&, Core::File::OpenMode, ShouldPrompt);

    virtual Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse expose_window_server_client_id() override;

    HashMap<ByteString, Core::File::OpenMode> m_approved_files;
};

}
