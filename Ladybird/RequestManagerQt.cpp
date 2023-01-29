/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RequestManagerQt.h"
#include <AK/JsonObject.h>
#include <QNetworkCookie>

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

RefPtr<Web::ResourceLoaderConnectorRequest> RequestManagerQt::start_request(DeprecatedString const& method, AK::URL const& url, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const& proxy)
{
    if (!url.scheme().is_one_of_ignoring_case("http"sv, "https"sv)) {
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

ErrorOr<NonnullRefPtr<RequestManagerQt::Request>> RequestManagerQt::Request::create(QNetworkAccessManager& qnam, DeprecatedString const& method, AK::URL const& url, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&)
{
    QNetworkRequest request { QString(url.to_deprecated_string().characters()) };
    request.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::ManualRedirectPolicy);
    request.setAttribute(QNetworkRequest::HttpPipeliningAllowedAttribute, true);
    request.setAttribute(QNetworkRequest::CookieLoadControlAttribute, QNetworkRequest::Manual);
    request.setAttribute(QNetworkRequest::CookieSaveControlAttribute, QNetworkRequest::Manual);

    // NOTE: We disable HTTP2 as it's significantly slower (up to 5x, possibly more)
    request.setAttribute(QNetworkRequest::Http2AllowedAttribute, false);

    QNetworkReply* reply = nullptr;

    for (auto& it : request_headers) {
        // FIXME: We currently strip the Accept-Encoding header on outgoing requests from LibWeb
        //        since otherwise it'll ask for compression without Qt being aware of it.
        //        This is very hackish and I'm sure we can do it in concert with Qt somehow.
        if (it.key == "Accept-Encoding")
            continue;
        request.setRawHeader(QByteArray(it.key.characters()), QByteArray(it.value.characters()));
    }

    if (method.equals_ignoring_case("head"sv)) {
        reply = qnam.head(request);
    } else if (method.equals_ignoring_case("get"sv)) {
        reply = qnam.get(request);
    } else if (method.equals_ignoring_case("post"sv)) {
        reply = qnam.post(request, QByteArray((char const*)request_body.data(), request_body.size()));
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
    HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
    Vector<DeprecatedString> set_cookie_headers;
    for (auto& it : m_reply.rawHeaderPairs()) {
        auto name = DeprecatedString(it.first.data(), it.first.length());
        auto value = DeprecatedString(it.second.data(), it.second.length());
        if (name.equals_ignoring_case("set-cookie"sv)) {
            // NOTE: Qt may have bundled multiple Set-Cookie headers into a single one.
            //       We have to extract the full list of cookies via QNetworkReply::header().
            auto set_cookie_list = m_reply.header(QNetworkRequest::SetCookieHeader).value<QList<QNetworkCookie>>();
            for (auto const& cookie : set_cookie_list) {
                set_cookie_headers.append(cookie.toRawForm().data());
            }
        } else {
            response_headers.set(name, value);
        }
    }
    if (!set_cookie_headers.is_empty()) {
        response_headers.set("set-cookie"sv, JsonArray { set_cookie_headers }.to_deprecated_string());
    }
    on_buffered_request_finish(success, buffer.length(), response_headers, http_status_code, ReadonlyBytes { buffer.data(), (size_t)buffer.size() });
}
