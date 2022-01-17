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
#include <LibGUI/Forward.h>
#include <LibIPC/ClientConnection.h>

namespace FileSystemAccessServer {

class ClientConnection final
    : public IPC::ClientConnection<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void request_file_read_only_approved(i32, i32, String const&) override;
    virtual void request_file(i32, i32, String const&, Core::OpenMode const&) override;
    virtual void prompt_open_file(i32, i32, String const&, String const&, Core::OpenMode const&) override;
    virtual void prompt_save_file(i32, i32, String const&, String const&, String const&, Core::OpenMode const&) override;

    void prompt_helper(Optional<String> const&, Core::OpenMode const&);
    RefPtr<GUI::Window> create_dummy_child_window(i32, i32);

    enum class ShouldPrompt {
        No,
        Yes
    };
    void request_file_handler(i32, i32, String const&, Core::OpenMode const&, ShouldPrompt);

    virtual Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse expose_window_server_client_id() override;

    HashMap<String, Core::OpenMode> m_approved_files;
};

}
