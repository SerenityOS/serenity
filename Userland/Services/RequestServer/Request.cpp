/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <RequestServer/ClientConnection.h>
#include <RequestServer/Request.h>

namespace RequestServer {

// FIXME: What about rollover?
static i32 s_next_id = 1;

Request::Request(ClientConnection& client, NonnullOwnPtr<Core::Stream::File>&& output_stream)
    : m_client(client)
    , m_id(s_next_id++)
    , m_output_stream(move(output_stream))
{
}

Request::~Request()
{
}

void Request::stop()
{
    m_client.did_finish_request({}, *this, false);
}

void Request::set_response_headers(const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)
{
    m_response_headers = response_headers;
    m_client.did_receive_headers({}, *this);
}

void Request::set_certificate(String, String)
{
}

void Request::did_finish(bool success)
{
    m_client.did_finish_request({}, *this, success);
}

void Request::did_progress(Optional<u32> total_size, u32 downloaded_size)
{
    m_total_size = total_size;
    m_downloaded_size = downloaded_size;
    m_client.did_progress_request({}, *this);
}

void Request::did_request_certificates()
{
    m_client.did_request_certificates({}, *this);
}

}
