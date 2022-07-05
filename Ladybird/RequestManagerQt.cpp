/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RequestManagerQt.h"
#include <AK/JsonArray.h>

RequestManagerQt::RequestManagerQt()
{
    m_qnam = new QNetworkAccessManager(this);

    QObject::connect(m_qnam, &QNetworkAccessManager::finished, this, &RequestManagerQt::reply_finished);
}

void RequestManagerQt::reply_finished(QNetworkReply* reply)
{
    auto request = m_pending.get(reply).value();
    m_pending.remove(reply);
    request->did_finish();
}

RefPtr<Web::ResourceLoaderConnectorRequest> RequestManagerQt::start_request(String const& method, AK::URL const& url, HashMap<String, String> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const& proxy)
{
    if (!url.protocol().is_one_of_ignoring_case("http"sv, "https"sv)) {
        return nullptr;
    }
    auto request_or_error = Request::create(*m_qnam, method, url, request_headers, request_body, proxy);
    if (request_or_error.is_error()) {
        return nullptr;
    }
    auto request = request_or_error.release_value();
    m_pending.set(&request->reply(), *request);
    return request;
}

ErrorOr<NonnullRefPtr<RequestManagerQt::Request>> RequestManagerQt::Request::create(QNetworkAccessManager& qnam, String const& method, AK::URL const& url, HashMap<String, String> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&)
{
    QNetworkRequest request { QString(url.to_string().characters()) };

    QNetworkReply* reply = nullptr;

    if (method.equals_ignoring_case("head"sv)) {
        reply = qnam.head(request);
    } else if (method.equals_ignoring_case("get"sv)) {
        reply = qnam.get(request);
    } else if (method.equals_ignoring_case("post"sv)) {
        reply = qnam.post(request, QByteArray((char const*)request_body.data(), request_body.size()));
    }

    for (auto& it : request_headers) {
        request.setRawHeader(it.key.characters(), it.value.characters());
    }

    return adopt_ref(*new Request(*reply));
}

RequestManagerQt::Request::Request(QNetworkReply& reply)
    : m_reply(reply)
{
}

RequestManagerQt::Request::~Request() = default;

void RequestManagerQt::Request::did_finish()
{
    bool success = m_reply.error() == QNetworkReply::NetworkError::NoError;
    auto buffer = m_reply.readAll();
    auto http_status_code = m_reply.attribute(QNetworkRequest::Attribute::HttpStatusCodeAttribute).toInt();
    HashMap<String, String, CaseInsensitiveStringTraits> response_headers;
    Vector<String> set_cookie_headers;
    for (auto& it : m_reply.rawHeaderPairs()) {
        auto name = String(it.first.data(), it.first.length());
        auto value = String(it.second.data(), it.second.length());
        if (name.equals_ignoring_case("set-cookie")) {
            set_cookie_headers.append(value);
        } else {
            response_headers.set(name, value);
        }
    }
    if (!set_cookie_headers.is_empty()) {
        response_headers.set("set-cookie", JsonArray { set_cookie_headers }.to_string());
    }
    on_buffered_request_finish(success, buffer.length(), response_headers, http_status_code, ReadonlyBytes { buffer.data(), (size_t)buffer.size() });
}
