/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <ContentAccessServer/ContentAccessClientEndpoint.h>
#include <ContentAccessServer/ContentAccessServerEndpoint.h>
#include <LibCore/Promise.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystemAccessClient/File.h>
#include <LibGUI/Window.h>
#include <LibIPC/ConnectionToServer.h>

namespace ContentAccessClient {

using File = FileSystemAccessClient::File;
using Result = ErrorOr<File>;

class Client final
    : public IPC::ConnectionToServer<ContentAccessClientEndpoint, ContentAccessServerEndpoint>
    , public ContentAccessClientEndpoint {
    IPC_CLIENT_CONNECTION(Client, "/tmp/session/%sid/portal/contentaccess"sv)

public:
    Result request_url_read_only(GUI::Window* parent_window, URL const&);
    Result request_url_read_only_approve_local(GUI::Window* parent_window, URL const&);
    Result open_file(GUI::Window* parent_window, DeprecatedString const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), Core::Stream::OpenMode requested_access = Core::Stream::OpenMode::Read);

    static Client& the();

protected:
    void die() override;

private:
    explicit Client(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<ContentAccessClientEndpoint, ContentAccessServerEndpoint>(*this, move(socket))
    {
    }

    virtual void handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& fd, Optional<URL> const& chosen_url) override;

    Result request_url_impl(GUI::Window* parent_window, URL const&, bool skip_prompt);

    int get_new_id();
    Result handle_promise(int);

    struct PromiseAndWindow {
        RefPtr<Core::Promise<Result>> promise;
        GUI::Window* parent_window { nullptr };
    };

    HashMap<int, PromiseAndWindow> m_promises {};
    int m_last_id { 0 };
};

}
