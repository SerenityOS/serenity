/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <FileSystemAccessServer/FileSystemAccessClientEndpoint.h>
#include <FileSystemAccessServer/FileSystemAccessServerEndpoint.h>
#include <LibCore/File.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibIPC/ServerConnection.h>

namespace FileSystemAccessClient {

struct Result {
    i32 error;
    Optional<i32> fd;
    Optional<String> chosen_file;
};

class Client final
    : public IPC::ServerConnection<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>
    , public FileSystemAccessClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/portal/filesystemaccess")

public:
    Result request_file_read_only_approved(i32 parent_window_id, String const& path);
    Result request_file(i32 parent_window_id, String const& path, Core::OpenMode mode);
    Result open_file(i32 parent_window_id, String const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), Core::OpenMode requested_access = Core::OpenMode::ReadOnly);
    Result save_file(i32 parent_window_id, String const& name, String const ext, Core::OpenMode requested_access = Core::OpenMode::WriteOnly | Core::OpenMode::Truncate);

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
};

}
