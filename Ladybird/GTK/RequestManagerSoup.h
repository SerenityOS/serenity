#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <libsoup/soup.h>

class RequestManagerSoup final : public Web::ResourceLoaderConnector {
public:
    static NonnullRefPtr<RequestManagerSoup> create();
    virtual ~RequestManagerSoup() override;

private:
    RequestManagerSoup();

    virtual void prefetch_dns(AK::URL const&) override { }
    virtual void preconnect(AK::URL const&) override { }
    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(
        DeprecatedString const& method, AK::URL const&,
        HashMap<DeprecatedString, DeprecatedString> const& request_headers,
        ReadonlyBytes request_body, Core::ProxyData const&) override;

    SoupSession* m_session { nullptr };
};

class RequestSoup final : public Web::ResourceLoaderConnectorRequest {
public:
    virtual ~RequestSoup() override;
    static ErrorOr<NonnullRefPtr<RequestSoup>> create(SoupSession* session,
        DeprecatedString const& method, AK::URL const&,
        HashMap<DeprecatedString, DeprecatedString> const& request_headers,
        ReadonlyBytes request_body, Core::ProxyData const&);

private:
    RequestSoup(SoupSession*, SoupMessage*);
    void complete(GBytes* bytes, GError* error);

    virtual void set_should_buffer_all_input(bool) override { }
    virtual bool stop() override;
    virtual void stream_into(Stream&) override { }

    SoupMessage* m_message { nullptr };
    GCancellable* m_cancellable { nullptr };
};
