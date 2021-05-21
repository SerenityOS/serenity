/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibCore/Object.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/Fetch/FetchParams.h>

namespace Protocol {
class RequestClient;
class Request;
}

namespace Web::Fetch {

#if ARCH(I386)
#    define CPU_STRING "x86"
#else
#    define CPU_STRING "x86_64"
#endif

constexpr auto default_user_agent = "Mozilla/4.0 (SerenityOS; " CPU_STRING ") LibWeb+LibJS (Not KHTML, nor Gecko) LibWeb";
constexpr u8 maximum_redirects_allowed = 20; // "If request’s redirect count is twenty, return a network error." https://fetch.spec.whatwg.org/#concept-http-redirect-fetch

class ResourceLoader : public Core::Object {
    C_OBJECT_ABSTRACT(ResourceLoader)
public:
    static ResourceLoader& the();

    RefPtr<Response> load_resource(Response::Type, const LoadRequest&);

    void load(LoadRequest&, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback = nullptr);
    void load(const AK::URL&, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback = nullptr);
    void load_sync(LoadRequest&, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback = nullptr);

    void prefetch_dns(AK::URL const&);
    void preconnect(AK::URL const&);

    Function<void()> on_load_counter_change;

    int pending_loads() const { return m_pending_loads; }

    Protocol::RequestClient& protocol_client() { return *m_protocol_client; }

    const String& user_agent() const { return m_user_agent; }
    void set_user_agent(const String& user_agent) { m_user_agent = user_agent; }

    void clear_cache();

    void fetch(LoadRequest&, ProcessRequestBodyType, ProcessRequestEndOfBodyType, ProcessReponseType, ProcessResponseEndOfBodyType, ProcessResponseDoneType, bool use_parallel_queue = false);

private:
    ResourceLoader(NonnullRefPtr<Protocol::RequestClient> protocol_client);
    static ErrorOr<NonnullRefPtr<ResourceLoader>> try_create();

    static bool is_port_blocked(int port);

    RefPtr<Response> main_fetch(const FetchParams&, bool recursive = false);
    RefPtr<Response> scheme_fetch(const FetchParams&);
    RefPtr<Response> http_fetch(const FetchParams&, bool make_cors_preflight = false);
    RefPtr<Response> http_network_or_cache_fetch(const FetchParams&, bool is_authentication_fetch = false, bool is_new_connection_fetch = false);
    RefPtr<Response> http_network_fetch(const FetchParams&, bool include_credentials = false, bool force_new_connection = false);
    RefPtr<Response> http_redirect_fetch(const FetchParams&, RefPtr<Response>);
    void fetch_finale(const FetchParams&, NonnullRefPtr<Response>);
    void finalize_response(const FetchParams&, RefPtr<Response>);
    [[nodiscard]] bool cors_check(const LoadRequest&, RefPtr<Response>);
    [[nodiscard]] bool tao_check(const LoadRequest&, RefPtr<Response>);

    int m_pending_loads { 0 };

    HashTable<NonnullRefPtr<Protocol::Request>> m_active_requests;
    RefPtr<Protocol::RequestClient> m_protocol_client;
    String m_user_agent;
};

}
