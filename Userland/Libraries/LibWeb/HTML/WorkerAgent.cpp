/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HostDefined.h>
#include <LibWeb/HTML/WorkerAgent.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Worker/WebWorkerClient.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(WorkerAgent);

WorkerAgent::WorkerAgent(AK::URL url, WorkerOptions const& options, JS::GCPtr<MessagePort> outside_port)
    : m_worker_options(options)
    , m_url(move(url))
    , m_outside_port(outside_port)
{
}

void WorkerAgent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    m_message_port = MessagePort::create(realm);
    m_message_port->entangle_with(*m_outside_port);

    TransferDataHolder data_holder;
    MUST(m_message_port->transfer_steps(data_holder));

    // NOTE: This blocking IPC call may launch another process.
    //    If spinning the event loop for this can cause other javascript to execute, we're in trouble.
    auto worker_ipc_sockets = Bindings::host_defined_page(realm).client().request_worker_agent();
    auto worker_socket = MUST(Core::LocalSocket::adopt_fd(worker_ipc_sockets.socket.take_fd()));
    MUST(worker_socket->set_blocking(true));

    auto fd_passing_socket = MUST(Core::LocalSocket::adopt_fd(worker_ipc_sockets.fd_passing_socket.take_fd()));

    m_worker_ipc = make_ref_counted<WebWorkerClient>(move(worker_socket));
    m_worker_ipc->set_fd_passing_socket(move(fd_passing_socket));

    m_worker_ipc->async_start_dedicated_worker(m_url, m_worker_options.type, m_worker_options.credentials, m_worker_options.name, move(data_holder));
}

void WorkerAgent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_message_port);
    visitor.visit(m_outside_port);
}

}
