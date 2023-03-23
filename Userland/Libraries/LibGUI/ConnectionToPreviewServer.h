/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionToServer.h>
#include <Userland/Services/PreviewServer/PreviewClientEndpoint.h>
#include <Userland/Services/PreviewServer/PreviewServerEndpoint.h>

namespace GUI {

class ConnectionToPreviewServer final
    : public IPC::ConnectionToServer<PreviewClientEndpoint, PreviewServerEndpoint>
    , public PreviewClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToPreviewServer, "/tmp/session/%sid/portal/preview"sv)
public:
    virtual ~ConnectionToPreviewServer() = default;

    void get_preview_for(String const& file_path, Function<void(String const&, NonnullRefPtr<Gfx::Bitmap>)> success_callback, Function<void(String const&, PreviewServer::Error const&)> error_callback);
    bool is_preview_requested(String const& file_path);

private:
    ConnectionToPreviewServer(NonnullOwnPtr<Core::LocalSocket>);

    virtual void preview_rendered(String const& requested_path, Gfx::ShareableBitmap const& preview) override;
    virtual void preview_failed(String const& requested_path, PreviewServer::Error const& reason) override;

    HashMap<String, Function<void(String const&, NonnullRefPtr<Gfx::Bitmap>)>> m_success_callbacks;
    HashMap<String, Function<void(String const&, PreviewServer::Error const&)>> m_failure_callbacks;
};

}
