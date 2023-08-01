/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>
#include <LibWebView/RequestServerAdapter.h>

namespace WebView {

ErrorOr<NonnullRefPtr<RequestServerRequestAdapter>> RequestServerRequestAdapter::try_create(NonnullRefPtr<Protocol::Request> request)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) RequestServerRequestAdapter(move(request)));
}

RequestServerRequestAdapter::RequestServerRequestAdapter(NonnullRefPtr<Protocol::Request> request)
    : m_request(request)
{
    request->on_buffered_request_finish = [weak_this = make_weak_ptr()](auto success, auto total_size, auto const& response_headers, auto response_code, auto payload) {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_buffered_request_finish)
                strong_this->on_buffered_request_finish(success, total_size, response_headers, response_code, move(payload));
    };

    request->on_finish = [weak_this = make_weak_ptr()](bool success, u64 total_size) {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_finish)
                strong_this->on_finish(success, total_size);
    };

    request->on_progress = [weak_this = make_weak_ptr()](Optional<u64> total_size, u64 downloaded_size) {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_progress)
                strong_this->on_progress(total_size, downloaded_size);
    };

    request->on_certificate_requested = [weak_this = make_weak_ptr()]() {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_certificate_requested) {
                auto certificate_and_key = strong_this->on_certificate_requested();
                return Protocol::Request::CertificateAndKey {
                    .certificate = move(certificate_and_key.certificate),
                    .key = move(certificate_and_key.key),
                };
            }
        }

        return Protocol::Request::CertificateAndKey {};
    };
}

RequestServerRequestAdapter::~RequestServerRequestAdapter() = default;

void RequestServerRequestAdapter::set_should_buffer_all_input(bool should_buffer_all_input)
{
    m_request->set_should_buffer_all_input(should_buffer_all_input);
}

bool RequestServerRequestAdapter::stop()
{
    return m_request->stop();
}

void RequestServerRequestAdapter::stream_into(Stream& stream)
{
    m_request->stream_into(stream);
}

ErrorOr<NonnullRefPtr<RequestServerAdapter>> RequestServerAdapter::try_create(NonnullRefPtr<Protocol::RequestClient> protocol_client)
{
    return try_make_ref_counted<RequestServerAdapter>(move(protocol_client));
}

ErrorOr<NonnullRefPtr<RequestServerAdapter>> RequestServerAdapter::try_create()
{
    auto protocol_client = TRY(Protocol::RequestClient::try_create());
    return try_make_ref_counted<RequestServerAdapter>(move(protocol_client));
}

RequestServerAdapter::RequestServerAdapter(NonnullRefPtr<Protocol::RequestClient> protocol_client)
    : m_protocol_client(protocol_client)
{
}

RequestServerAdapter::~RequestServerAdapter() = default;

RefPtr<Web::ResourceLoaderConnectorRequest> RequestServerAdapter::start_request(DeprecatedString const& method, URL const& url, HashMap<DeprecatedString, DeprecatedString> const& headers, ReadonlyBytes body, Core::ProxyData const& proxy)
{
    auto protocol_request = m_protocol_client->start_request(method, url, headers, body, proxy);
    if (!protocol_request)
        return {};
    return RequestServerRequestAdapter::try_create(protocol_request.release_nonnull()).release_value_but_fixme_should_propagate_errors();
}

void RequestServerAdapter::prefetch_dns(AK::URL const& url)
{
    m_protocol_client->ensure_connection(url, RequestServer::CacheLevel::ResolveOnly);
}

void RequestServerAdapter::preconnect(AK::URL const& url)
{
    m_protocol_client->ensure_connection(url, RequestServer::CacheLevel::CreateConnection);
}

}
