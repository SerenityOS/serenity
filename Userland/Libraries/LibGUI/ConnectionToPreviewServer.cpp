/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/ConnectionToPreviewServer.h>
#include <Services/PreviewServer/Error.h>

namespace GUI {

ConnectionToPreviewServer::ConnectionToPreviewServer(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<PreviewClientEndpoint, PreviewServerEndpoint>(*this, move(socket))
{
}

void ConnectionToPreviewServer::get_preview_for(String const& file_path, Function<void(String const&, NonnullRefPtr<Gfx::Bitmap>)> success_callback, Function<void(String const&, PreviewServer::Error const&)> error_callback)
{
    m_success_callbacks.set(file_path, move(success_callback));
    m_failure_callbacks.set(file_path, move(error_callback));
    async_preview_for(file_path);
}

bool ConnectionToPreviewServer::is_preview_requested(String const& file_path)
{
    return m_success_callbacks.contains(file_path) || m_failure_callbacks.contains(file_path);
}

void ConnectionToPreviewServer::preview_rendered(String const& requested_path, Gfx::ShareableBitmap const& preview)
{
    // Remove the shareable bitmap as soon as possible, since large folders will quickly run us against the file descriptor limit otherwise.
    // A bitmap copy will never be shared by default.
    auto maybe_nonshared_preview = preview.bitmap()->clone();
    if (maybe_nonshared_preview.is_error())
        preview_failed(requested_path, PreviewServer::Error::OutOfMemory);
    auto nonshared_preview = maybe_nonshared_preview.release_value();

    if (auto callback = m_success_callbacks.get(requested_path); callback.has_value())
        callback.value()(requested_path, nonshared_preview);

    m_success_callbacks.remove(requested_path);
    m_failure_callbacks.remove(requested_path);
}
void ConnectionToPreviewServer::preview_failed(String const& requested_path, PreviewServer::Error const& reason)
{
    if (auto callback = m_failure_callbacks.get(requested_path); callback.has_value())
        callback.value()(requested_path, reason);

    m_success_callbacks.remove(requested_path);
    m_failure_callbacks.remove(requested_path);
}

}
