/*
 * Copyright (c) 2021-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <ContentAccessServer/ContentAccessClientEndpoint.h>
#include <ContentAccessServer/ContentAccessServerEndpoint.h>
#include <LibCore/Forward.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Forward.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibProtocol/RequestClient.h>

namespace ContentAccessServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<ContentAccessClientEndpoint, ContentAccessServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void request_url_read_only(i32, i32, i32, URL const&, bool) override;
    virtual void prompt_open_file(i32, i32, i32, DeprecatedString const&, DeprecatedString const&, Core::Stream::OpenMode) override;

    void prompt_helper(i32, URL const&, Core::Stream::OpenMode);
    RefPtr<GUI::Window> create_dummy_child_window(i32, i32);

    struct ProgressObject {
        NonnullRefPtr<GUI::Window> main_window;
        NonnullRefPtr<GUI::Window> dialog;
        Function<void(URL const&, u32, Optional<u32>)> update;
    };
    ProgressObject create_download_progress_window(i32, i32);

    enum class ShouldPrompt {
        No,
        Yes
    };

    void request_url_handler(i32, i32, i32, URL const&, ShouldPrompt);

    bool ensure_request_client();

    virtual Messages::ContentAccessServer::ExposeWindowServerClientIdResponse expose_window_server_client_id() override;

    HashMap<URL, Core::Stream::OpenMode> m_approved_files;
    HashMap<URL, NonnullRefPtr<Protocol::Request>> m_active_requests;
    HashMap<u64, NonnullRefPtr<GUI::Window>> m_active_windows;
    RefPtr<Protocol::RequestClient> m_request_client;
};

}
