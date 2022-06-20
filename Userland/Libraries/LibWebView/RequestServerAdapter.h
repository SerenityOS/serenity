/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Protocol {
class Request;
class RequestClient;
}

namespace WebView {

class RequestServerRequestAdapter
    : public Web::ResourceLoaderConnectorRequest
    , public Weakable<RequestServerRequestAdapter> {
public:
    static ErrorOr<NonnullRefPtr<RequestServerRequestAdapter>> try_create(NonnullRefPtr<Protocol::Request>);
    virtual ~RequestServerRequestAdapter() override;

    virtual void set_should_buffer_all_input(bool) override;
    virtual bool stop() override;

    virtual void stream_into(Core::Stream::Stream&) override;

private:
    RequestServerRequestAdapter(NonnullRefPtr<Protocol::Request>);
    NonnullRefPtr<Protocol::Request> m_request;
};

class RequestServerAdapter : public Web::ResourceLoaderConnector {
public:
    static ErrorOr<NonnullRefPtr<RequestServerAdapter>> try_create();
    virtual ~RequestServerAdapter() override;

    virtual void prefetch_dns(AK::URL const& url) override;
    virtual void preconnect(AK::URL const& url) override;

    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(String const& method, URL const&, HashMap<String, String> const& request_headers = {}, ReadonlyBytes request_body = {}, Core::ProxyData const& = {}) override;

private:
    RequestServerAdapter(NonnullRefPtr<Protocol::RequestClient> protocol_client);

    RefPtr<Protocol::RequestClient> m_protocol_client;
};

}
