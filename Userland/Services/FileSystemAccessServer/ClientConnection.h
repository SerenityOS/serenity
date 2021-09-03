/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/HashMap.h>
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
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    virtual void request_file(i32, i32, String const&, Core::OpenMode const&) override;
    virtual void prompt_open_file(i32, i32, String const&, String const&, Core::OpenMode const&) override;
    virtual void prompt_save_file(i32, i32, String const&, String const&, String const&, Core::OpenMode const&) override;

    void prompt_helper(Optional<String> const&, Core::OpenMode const&);
    RefPtr<GUI::Window> create_dummy_child_window(i32, i32);

    virtual Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse expose_window_server_client_id() override;

    HashMap<String, Core::OpenMode> m_approved_files;
};

}
