/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <FileSystemAccessServer/FileSystemAccessClientEndpoint.h>
#include <FileSystemAccessServer/FileSystemAccessServerEndpoint.h>
#include <LibCore/File.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Window.h>
#include <LibIPC/ServerConnection.h>

namespace FileSystemAccessClient {

using Result = ErrorOr<NonnullRefPtr<Core::File>>;

class Client final
    : public IPC::ServerConnection<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>
    , public FileSystemAccessClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/portal/filesystemaccess")

public:
    Result try_request_file_read_only_approved(GUI::Window* parent_window, String const& path);
    Result try_request_file(GUI::Window* parent_window, String const& path, Core::OpenMode mode);
    Result try_open_file(GUI::Window* parent_window, String const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), Core::OpenMode requested_access = Core::OpenMode::ReadOnly);
    Result try_save_file(GUI::Window* parent_window, String const& name, String const ext, Core::OpenMode requested_access = Core::OpenMode::WriteOnly | Core::OpenMode::Truncate);

    static Client& the();

protected:
    void die() override;

private:
    explicit Client(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ServerConnection<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>(*this, move(socket))
    {
    }

    virtual void handle_prompt_end(i32 error, Optional<IPC::File> const& fd, Optional<String> const& chosen_file) override;

    RefPtr<Core::Promise<Result>> m_promise;
    GUI::Window* m_parent_window { nullptr };
};

}
