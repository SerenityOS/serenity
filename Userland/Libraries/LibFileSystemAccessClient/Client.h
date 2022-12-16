/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <FileSystemAccessServer/FileSystemAccessClientEndpoint.h>
#include <FileSystemAccessServer/FileSystemAccessServerEndpoint.h>
#include <LibCore/File.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Window.h>
#include <LibIPC/ConnectionToServer.h>

namespace FileSystemAccessClient {

using DeprecatedResult = ErrorOr<NonnullRefPtr<Core::File>>;
using Result = ErrorOr<NonnullOwnPtr<Core::Stream::File>>;

class Client final
    : public IPC::ConnectionToServer<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>
    , public FileSystemAccessClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/session/%sid/portal/filesystemaccess"sv)

public:
    DeprecatedResult try_request_file_read_only_approved_deprecated(GUI::Window* parent_window, DeprecatedString const& path);
    DeprecatedResult try_request_file_deprecated(GUI::Window* parent_window, DeprecatedString const& path, Core::OpenMode mode);
    DeprecatedResult try_open_file_deprecated(GUI::Window* parent_window, DeprecatedString const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), Core::OpenMode requested_access = Core::OpenMode::ReadOnly);
    DeprecatedResult try_save_file_deprecated(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::OpenMode requested_access = Core::OpenMode::WriteOnly | Core::OpenMode::Truncate);

    Result save_file(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::Stream::OpenMode requested_access = Core::Stream::OpenMode::Write | Core::Stream::OpenMode::Truncate);

    static Client& the();

protected:
    void die() override;

private:
    explicit Client(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>(*this, move(socket))
    {
    }

    virtual void handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& fd, Optional<DeprecatedString> const& chosen_file) override;

    int get_new_id();
    template<typename AnyResult>
    AnyResult handle_promise(int);

    template<typename T>
    using PromiseType = RefPtr<Core::Promise<T>>;

    struct PromiseAndWindow {
        Variant<PromiseType<DeprecatedResult>, PromiseType<Result>> promise;
        GUI::Window* parent_window { nullptr };
    };

    HashMap<int, PromiseAndWindow> m_promises {};
    int m_last_id { 0 };
};

}
