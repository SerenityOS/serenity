/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

}
