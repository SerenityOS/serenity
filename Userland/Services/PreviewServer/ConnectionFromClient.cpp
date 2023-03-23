/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <PreviewServer/ConnectionFromClient.h>

namespace PreviewServer {

static HashMap<int, NonnullRefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<PreviewClientEndpoint, PreviewServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

void ConnectionFromClient::preview_for(String const& path)
{
    auto maybe_file_hash = Cache::hash_for(path);
    // File I/O error of some sorts; no need to request preview.
    if (maybe_file_hash.is_error()) {
        dbgln_if(PREVIEW_SERVER_DEBUG, "Preview for {} failed: {}", path, maybe_file_hash.release_error());
        async_preview_failed(path, Error::FileNotFound);
        return;
    }

    auto file_hash = maybe_file_hash.release_value();

    // Lookup the preview in the pending preview promises.
    if (m_requested_previews.contains(file_hash)) {
        // Callback will run eventually.
        dbgln_if(PREVIEW_SERVER_DEBUG, "{} already requested", path);
        return;
    }
    dbgln_if(PREVIEW_SERVER_DEBUG, "Requesting preview for {}", path);

    // Create a new preview promise and make a request to the cache.
    auto maybe_file_preview_promise = CachePromise::try_create();
    if (maybe_file_preview_promise.is_error()) {
        async_preview_failed(path, Error::OutOfMemory);
        return;
    }
    auto file_preview_promise = maybe_file_preview_promise.release_value();
    file_preview_promise->on_resolved = [weak_generic_this = this->make_weak_ptr(), path = path, file_hash](auto const& preview_or_error) -> ErrorOr<void> {
        // If the client is gone, no need to invoke any callbacks.
        if (auto strong_generic_this = weak_generic_this.strong_ref(); strong_generic_this != nullptr) {
            auto strong_this = static_ptr_cast<ConnectionFromClient>(strong_generic_this);
            strong_this->send_preview_response(path, file_hash, preview_or_error);
        }
        return {};
    };

    m_requested_previews.set(file_hash, file_preview_promise);
    auto result = Cache::the().request_preview(path, file_hash, file_preview_promise);
    // EINVAL indicates that the file is excluded from caching.
    if (result.is_error())
        async_preview_failed(path, result.error().code() == EINVAL ? Error::PreviewCreationError : Error::OutOfMemory);
}

void ConnectionFromClient::send_preview_response(String const& path, Hash file_hash, CacheEntry const& preview_or_error)
{
    if (preview_or_error.entry.is_error())
        async_preview_failed(path, from_generic_error(preview_or_error.entry.error()));
    else
        async_preview_rendered(path, preview_or_error.entry.value().preview);

    // Since we now sent a callback, remove the promise if necessary.
    m_requested_previews.remove(file_hash);
}

}
