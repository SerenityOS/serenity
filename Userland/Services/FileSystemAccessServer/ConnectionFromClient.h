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

    virtual void request_file_read_only_approved(i32, i32, i32, DeprecatedString const&) override;
    virtual void request_file(i32, i32, i32, DeprecatedString const&, Core::Stream::OpenMode) override;
    virtual void prompt_open_file(i32, i32, i32, DeprecatedString const&, DeprecatedString const&, Core::Stream::OpenMode) override;
    virtual void prompt_save_file(i32, i32, i32, DeprecatedString const&, DeprecatedString const&, DeprecatedString const&, Core::Stream::OpenMode) override;

    void prompt_helper(i32, Optional<DeprecatedString> const&, Core::Stream::OpenMode);
    RefPtr<GUI::Window> create_dummy_child_window(i32, i32);

    enum class ShouldPrompt {
        No,
        Yes
    };
    void request_file_handler(i32, i32, i32, DeprecatedString const&, Core::Stream::OpenMode, ShouldPrompt);

    virtual Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse expose_window_server_client_id() override;

    HashMap<DeprecatedString, Core::Stream::OpenMode> m_approved_files;
};

}
