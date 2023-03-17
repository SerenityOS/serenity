/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <FileSystemAccessServer/FileSystemAccessClientEndpoint.h>
#include <FileSystemAccessServer/FileSystemAccessServerEndpoint.h>
#include <LibCore/File.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FileTypeFilter.h>
#include <LibGUI/Window.h>
#include <LibIPC/ConnectionToServer.h>

namespace FileSystemAccessClient {

class Client;
class File {
public:
    File(Badge<Client>, NonnullOwnPtr<Core::File> stream, String filename)
        : m_stream(move(stream))
        , m_filename(filename)
    {
    }

    Core::File& stream() const { return *m_stream; }
    NonnullOwnPtr<Core::File> release_stream() { return move(m_stream); }
    String filename() const { return m_filename; }

private:
    NonnullOwnPtr<Core::File> m_stream;
    String m_filename;
};

using Result = ErrorOr<File>;

class Client final
    : public IPC::ConnectionToServer<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>
    , public FileSystemAccessClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/session/%sid/portal/filesystemaccess"sv)

public:
    Result request_file_read_only_approved(GUI::Window* parent_window, DeprecatedString const& path);
    Result request_file(GUI::Window* parent_window, DeprecatedString const& path, Core::File::OpenMode requested_access);
    Result open_file(GUI::Window* parent_window, DeprecatedString const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), Core::File::OpenMode requested_access = Core::File::OpenMode::Read, Optional<Vector<GUI::FileTypeFilter>> const& = {});
    Result save_file(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::File::OpenMode requested_access = Core::File::OpenMode::Write | Core::File::OpenMode::Truncate);

    static Client& the();

protected:
    void die() override;

private:
    explicit Client(NonnullOwnPtr<Core::LocalSocket> socket)
        : IPC::ConnectionToServer<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>(*this, move(socket))
    {
    }

    virtual void handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& fd, Optional<DeprecatedString> const& chosen_file) override;

    int get_new_id();
    Result handle_promise(int);

    template<typename T>
    using PromiseType = RefPtr<Core::Promise<T>>;

    struct PromiseAndWindow {
        PromiseType<Result> promise;
        GUI::Window* parent_window { nullptr };
    };

    HashMap<int, PromiseAndWindow> m_promises {};
    int m_last_id { 0 };
};

}
