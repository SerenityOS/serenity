/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibCore/Promise.h>
#include <LibIPC/ConnectionFromClient.h>
#include <PreviewServer/Cache.h>
#include <PreviewServer/PreviewClientEndpoint.h>
#include <PreviewServer/PreviewServerEndpoint.h>

namespace PreviewServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<PreviewClientEndpoint, PreviewServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id);

    virtual void preview_for(String const& path) override;
    void send_preview_response(String const& path, Hash file_hash, CacheEntry const& preview_or_error);

    HashMap<Hash, NonnullRefPtr<CachePromise>> m_requested_previews;
};

}
