/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>
#include <LibWebView/WebSocketClientAdapter.h>

namespace WebView {

RefPtr<WebSocketClientSocketAdapter> WebSocketClientSocketAdapter::create(NonnullRefPtr<Protocol::WebSocket> websocket)
{
    return adopt_ref(*new WebSocketClientSocketAdapter(move(websocket)));
}

WebSocketClientSocketAdapter::WebSocketClientSocketAdapter(NonnullRefPtr<Protocol::WebSocket> websocket)
    : m_websocket(move(websocket))
{
    m_websocket->on_open = [weak_this = make_weak_ptr()] {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_open)
                strong_this->on_open();
    };
    m_websocket->on_message = [weak_this = make_weak_ptr()](auto message) {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_message) {
                strong_this->on_message(Web::WebSockets::WebSocketClientSocket::Message {
                    .data = move(message.data),
                    .is_text = message.is_text,
                });
            }
        }
    };
    m_websocket->on_error = [weak_this = make_weak_ptr()](auto error) {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_error) {
                switch (error) {
                case Protocol::WebSocket::Error::CouldNotEstablishConnection:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::CouldNotEstablishConnection);
                    return;
                case Protocol::WebSocket::Error::ConnectionUpgradeFailed:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ConnectionUpgradeFailed);
                    return;
                case Protocol::WebSocket::Error::ServerClosedSocket:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ServerClosedSocket);
                    return;
                }
                VERIFY_NOT_REACHED();
            }
        }
    };
    m_websocket->on_close = [weak_this = make_weak_ptr()](u16 code, String reason, bool was_clean) {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_close)
                strong_this->on_close(code, move(reason), was_clean);
    };
    m_websocket->on_certificate_requested = [weak_this = make_weak_ptr()] {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_certificate_requested) {
                auto certificate_and_key = weak_this->on_certificate_requested();
                return Protocol::WebSocket::CertificateAndKey {
                    .certificate = move(certificate_and_key.certificate),
                    .key = move(certificate_and_key.key),
                };
            }
        }
        return Protocol::WebSocket::CertificateAndKey {};
    };
}

WebSocketClientSocketAdapter::~WebSocketClientSocketAdapter() = default;

Web::WebSockets::WebSocket::ReadyState WebSocketClientSocketAdapter::ready_state()
{
    switch (m_websocket->ready_state()) {
    case Protocol::WebSocket::ReadyState::Connecting:
        return Web::WebSockets::WebSocket::ReadyState::Connecting;
    case Protocol::WebSocket::ReadyState::Open:
        return Web::WebSockets::WebSocket::ReadyState::Open;
    case Protocol::WebSocket::ReadyState::Closing:
        return Web::WebSockets::WebSocket::ReadyState::Closing;
    case Protocol::WebSocket::ReadyState::Closed:
        return Web::WebSockets::WebSocket::ReadyState::Closed;
    }
    VERIFY_NOT_REACHED();
}

void WebSocketClientSocketAdapter::send(ByteBuffer binary_or_text_message, bool is_text)
{
    m_websocket->send(binary_or_text_message, is_text);
}

void WebSocketClientSocketAdapter::send(StringView text_message)
{
    m_websocket->send(text_message);
}

void WebSocketClientSocketAdapter::close(u16 code, String reason)
{
    m_websocket->close(code, reason);
}

ErrorOr<NonnullRefPtr<WebSocketClientManagerAdapter>> WebSocketClientManagerAdapter::try_create()
{
    auto websocket_client = TRY(Protocol::WebSocketClient::try_create());
    return adopt_nonnull_ref_or_enomem(new (nothrow) WebSocketClientManagerAdapter(move(websocket_client)));
}

WebSocketClientManagerAdapter::WebSocketClientManagerAdapter(NonnullRefPtr<Protocol::WebSocketClient> websocket_client)
    : m_websocket_client(move(websocket_client))
{
}

WebSocketClientManagerAdapter::~WebSocketClientManagerAdapter() = default;

RefPtr<Web::WebSockets::WebSocketClientSocket> WebSocketClientManagerAdapter::connect(const AK::URL& url, String const& origin)
{
    auto underlying_websocket = m_websocket_client->connect(url, origin);
    if (!underlying_websocket)
        return {};
    return WebSocketClientSocketAdapter::create(underlying_websocket.release_nonnull());
}

}
