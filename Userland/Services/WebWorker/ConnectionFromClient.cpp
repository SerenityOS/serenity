/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WebWorker/ConnectionFromClient.h>
#include <WebWorker/DedicatedWorkerHost.h>
#include <WebWorker/PageHost.h>

namespace WebWorker {

void ConnectionFromClient::die()
{
    // FIXME: Do something here (shutdown process/script gracefully?)
}

void ConnectionFromClient::request_file(Web::FileRequest request)
{
    // FIXME: Route this to FSAS or Brower chrome as appropriate instead of allowing
    //        the WebWorker process filesystem access
    auto path = request.path();
    auto request_id = ++last_id;

    m_requested_files.set(request_id, move(request));

    auto file = Core::File::open(path, Core::File::OpenMode::Read);

    if (file.is_error())
        handle_file_return(file.error().code(), {}, request_id);
    else
        handle_file_return(0, IPC::File(*file.value()), request_id);
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<WebWorkerClientEndpoint, WebWorkerServerEndpoint>(*this, move(socket), 1)
    , m_page_host(PageHost::create(*this))
{
}

ConnectionFromClient::~ConnectionFromClient() = default;

Web::Page& ConnectionFromClient::page()
{
    return m_page_host->page();
}

Web::Page const& ConnectionFromClient::page() const
{
    return m_page_host->page();
}

void ConnectionFromClient::start_dedicated_worker(AK::URL const& url, String const& type, String const&, String const&, IPC::File const&)
{
    m_worker_host = make_ref_counted<DedicatedWorkerHost>(page(), url, type);

    m_worker_host->run();
}

void ConnectionFromClient::handle_file_return(i32 error, Optional<IPC::File> const& file, i32 request_id)
{
    auto file_request = m_requested_files.take(request_id);

    VERIFY(file_request.has_value());
    VERIFY(file_request.value().on_file_request_finish);

    file_request.value().on_file_request_finish(error != 0 ? Error::from_errno(error) : ErrorOr<i32> { file->take_fd() });
}

}
