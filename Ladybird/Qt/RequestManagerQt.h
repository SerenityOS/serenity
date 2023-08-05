/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Loader/ResourceLoader.h>
#include <QtNetwork/QNetworkAccessManager>
#include <QtNetwork/QNetworkReply>

namespace Ladybird {

class RequestManagerQt
    : public QObject
    , public Web::ResourceLoaderConnector {
    Q_OBJECT
public:
    static NonnullRefPtr<RequestManagerQt> create()
    {
        return adopt_ref(*new RequestManagerQt());
    }

    virtual ~RequestManagerQt() override { }

    virtual void prefetch_dns(AK::URL const&) override { }
    virtual void preconnect(AK::URL const&) override { }

    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(DeprecatedString const& method, AK::URL const&, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&) override;

private slots:
    void reply_finished(QNetworkReply*);

private:
    RequestManagerQt();

    class Request
        : public Web::ResourceLoaderConnectorRequest {
    public:
        static ErrorOr<NonnullRefPtr<Request>> create(QNetworkAccessManager& qnam, DeprecatedString const& method, AK::URL const& url, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&);

        virtual ~Request() override;

        virtual void set_should_buffer_all_input(bool) override { }
        virtual bool stop() override { return false; }
        virtual void stream_into(Stream&) override { }

        void did_finish();

        QNetworkReply& reply() { return m_reply; }

    private:
        Request(QNetworkReply&);

        QNetworkReply& m_reply;
    };

    HashMap<QNetworkReply*, NonnullRefPtr<Request>> m_pending;
    QNetworkAccessManager* m_qnam { nullptr };
};

}
