/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibCore/Object.h>
#include <LibCore/Proxy.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

#if ARCH(I386)
#    define CPU_STRING "x86"
#else
#    define CPU_STRING "x86_64"
#endif

constexpr auto default_user_agent = "Mozilla/5.0 (SerenityOS; " CPU_STRING ") LibWeb+LibJS/1.0 Browser/1.0";

class ResourceLoaderConnectorRequest : public RefCounted<ResourceLoaderConnectorRequest> {
public:
    virtual ~ResourceLoaderConnectorRequest();

    struct CertificateAndKey {
        String certificate;
        String key;
    };

    virtual void set_should_buffer_all_input(bool) = 0;
    virtual bool stop() = 0;

    virtual void stream_into(Core::Stream::Stream&) = 0;

    Function<void(bool success, u32 total_size, HashMap<String, String, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> response_code, ReadonlyBytes payload)> on_buffered_request_finish;
    Function<void(bool success, u32 total_size)> on_finish;
    Function<void(Optional<u32> total_size, u32 downloaded_size)> on_progress;
    Function<CertificateAndKey()> on_certificate_requested;

protected:
    explicit ResourceLoaderConnectorRequest();
};

class ResourceLoaderConnector : public RefCounted<ResourceLoaderConnector> {
public:
    virtual ~ResourceLoaderConnector();

    virtual void prefetch_dns(AK::URL const&) = 0;
    virtual void preconnect(AK::URL const&) = 0;

    virtual RefPtr<ResourceLoaderConnectorRequest> start_request(String const& method, AK::URL const&, HashMap<String, String> const& request_headers = {}, ReadonlyBytes request_body = {}, Core::ProxyData const& = {}) = 0;

protected:
    explicit ResourceLoaderConnector();
};

class ResourceLoader : public Core::Object {
    C_OBJECT_ABSTRACT(ResourceLoader)
public:
    static void initialize(RefPtr<ResourceLoaderConnector>);
    static ResourceLoader& the();

    RefPtr<Resource> load_resource(Resource::Type, LoadRequest&);

    void load(LoadRequest&, Function<void(ReadonlyBytes, HashMap<String, String, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> status_code)> success_callback, Function<void(String const&, Optional<u32> status_code)> error_callback = nullptr);
    void load(const AK::URL&, Function<void(ReadonlyBytes, HashMap<String, String, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> status_code)> success_callback, Function<void(String const&, Optional<u32> status_code)> error_callback = nullptr);

    ResourceLoaderConnector& connector() { return *m_connector; }

    void prefetch_dns(AK::URL const&);
    void preconnect(AK::URL const&);

    Function<void()> on_load_counter_change;

    int pending_loads() const { return m_pending_loads; }

    String const& user_agent() const { return m_user_agent; }
    void set_user_agent(String const& user_agent) { m_user_agent = user_agent; }

    void clear_cache();
    void evict_from_cache(LoadRequest const&);

private:
    ResourceLoader(NonnullRefPtr<ResourceLoaderConnector>);
    static ErrorOr<NonnullRefPtr<ResourceLoader>> try_create(NonnullRefPtr<ResourceLoaderConnector>);

    static bool is_port_blocked(int port);

    int m_pending_loads { 0 };

    HashTable<NonnullRefPtr<ResourceLoaderConnectorRequest>> m_active_requests;
    NonnullRefPtr<ResourceLoaderConnector> m_connector;
    String m_user_agent;
};

}
