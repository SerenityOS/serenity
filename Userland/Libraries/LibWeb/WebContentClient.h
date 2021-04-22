/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ServerConnection.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace Web {

class OutOfProcessWebView;

class WebContentClient final
    : public IPC::ServerConnection<WebContentClientEndpoint, WebContentServerEndpoint>
    , public WebContentClientEndpoint {
    C_OBJECT(WebContentClient);

public:
    virtual void handshake() override;

    Function<void()> on_web_content_process_crash;

private:
    WebContentClient(OutOfProcessWebView&);

    virtual void die() override;

    virtual void handle(const Messages::WebContentClient::DidPaint&) override;
    virtual void handle(const Messages::WebContentClient::DidFinishLoading&) override;
    virtual void handle(const Messages::WebContentClient::DidInvalidateContentRect&) override;
    virtual void handle(const Messages::WebContentClient::DidChangeSelection&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestCursorChange&) override;
    virtual void handle(const Messages::WebContentClient::DidLayout&) override;
    virtual void handle(const Messages::WebContentClient::DidChangeTitle&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestScroll&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestScrollIntoView&) override;
    virtual void handle(const Messages::WebContentClient::DidEnterTooltipArea&) override;
    virtual void handle(const Messages::WebContentClient::DidLeaveTooltipArea&) override;
    virtual void handle(const Messages::WebContentClient::DidHoverLink&) override;
    virtual void handle(const Messages::WebContentClient::DidUnhoverLink&) override;
    virtual void handle(const Messages::WebContentClient::DidClickLink&) override;
    virtual void handle(const Messages::WebContentClient::DidMiddleClickLink&) override;
    virtual void handle(const Messages::WebContentClient::DidStartLoading&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestContextMenu&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestLinkContextMenu&) override;
    virtual void handle(const Messages::WebContentClient::DidRequestImageContextMenu&) override;
    virtual void handle(const Messages::WebContentClient::DidGetSource&) override;
    virtual void handle(const Messages::WebContentClient::DidJSConsoleOutput&) override;
    virtual void handle(const Messages::WebContentClient::DidChangeFavicon&) override;
    virtual OwnPtr<Messages::WebContentClient::DidRequestAlertResponse> handle(const Messages::WebContentClient::DidRequestAlert&) override;
    virtual OwnPtr<Messages::WebContentClient::DidRequestConfirmResponse> handle(const Messages::WebContentClient::DidRequestConfirm&) override;
    virtual OwnPtr<Messages::WebContentClient::DidRequestPromptResponse> handle(const Messages::WebContentClient::DidRequestPrompt&) override;
    virtual OwnPtr<Messages::WebContentClient::DidRequestCookieResponse> handle(const Messages::WebContentClient::DidRequestCookie&) override;
    virtual void handle(const Messages::WebContentClient::DidSetCookie&) override;

    OutOfProcessWebView& m_view;
};

}
