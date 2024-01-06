/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibWeb/Worker/WebWorkerClient.h>

namespace Web::HTML {

void WebWorkerClient::die()
{
    // FIXME: Notify WorkerAgent that the worker is ded
}

WebWorkerClient::WebWorkerClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<WebWorkerClientEndpoint, WebWorkerServerEndpoint>(*this, move(socket))
{
}

WebView::SocketPair WebWorkerClient::dup_sockets()
{
    WebView::SocketPair pair;
    pair.socket = MUST(Core::System::dup(socket().fd().value()));
    pair.fd_passing_socket = MUST(Core::System::dup(fd_passing_socket().fd().value()));
    return pair;
}

}
