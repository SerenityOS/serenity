/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Proxy.h>
#include <LibJS/SafeFunction.h>
#include <LibURL/URL.h>
#include <LibWeb/Loader/Resource.h>
#include <LibWeb/Page/Page.h>

namespace Web {

#if ARCH(X86_64)
#    define CPU_STRING "x86_64"
#elif ARCH(AARCH64)
#    define CPU_STRING "AArch64"
#elif ARCH(I386)
#    define CPU_STRING "x86"
#elif ARCH(RISCV64)
#    define CPU_STRING "RISC-V 64"
#else
#    error Unknown architecture
#endif

#if defined(AK_OS_SERENITY)
#    define OS_STRING "SerenityOS"
#elif defined(AK_OS_ANDROID)
#    define OS_STRING "Android 10"
#elif defined(AK_OS_LINUX)
#    define OS_STRING "Linux"
#elif defined(AK_OS_MACOS)
#    define OS_STRING "macOS"
#elif defined(AK_OS_IOS)
#    define OS_STRING "iOS"
#elif defined(AK_OS_WINDOWS)
#    define OS_STRING "Windows"
#elif defined(AK_OS_FREEBSD)
#    define OS_STRING "FreeBSD"
#elif defined(AK_OS_OPENBSD)
#    define OS_STRING "OpenBSD"
#elif defined(AK_OS_NETBSD)
#    define OS_STRING "NetBSD"
#elif defined(AK_OS_DRAGONFLY)
#    define OS_STRING "DragonFly"
#elif defined(AK_OS_SOLARIS)
#    define OS_STRING "SunOS"
#elif defined(AK_OS_HAIKU)
#    define OS_STRING "Haiku"
#elif defined(AK_OS_GNU_HURD)
#    define OS_STRING "GNU/Hurd"
#else
#    error Unknown OS
#endif

#define BROWSER_NAME "Ladybird"
#define BROWSER_VERSION "1.0"

constexpr auto default_user_agent = "Mozilla/5.0 (" OS_STRING "; " CPU_STRING ") " BROWSER_NAME "/" BROWSER_VERSION ""sv;
constexpr auto default_platform = OS_STRING " " CPU_STRING ""sv;

namespace WebSockets {
class WebSocketClientSocket;
}

class ResourceLoaderConnectorRequest : public RefCounted<ResourceLoaderConnectorRequest> {
public:
    virtual ~ResourceLoaderConnectorRequest();

    struct CertificateAndKey {
        ByteString certificate;
        ByteString key;
    };

    virtual void set_should_buffer_all_input(bool) = 0;
    virtual bool stop() = 0;

    virtual void stream_into(Stream&) = 0;

    Function<void(bool success, u64 total_size, HashMap<ByteString, ByteString, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> response_code, ReadonlyBytes payload)> on_buffered_request_finish;
    Function<void(bool success, u64 total_size)> on_finish;
    Function<void(Optional<u64> total_size, u64 downloaded_size)> on_progress;
    Function<CertificateAndKey()> on_certificate_requested;

protected:
    explicit ResourceLoaderConnectorRequest();
};

class ResourceLoaderConnector : public RefCounted<ResourceLoaderConnector> {
public:
    virtual ~ResourceLoaderConnector();

    virtual void prefetch_dns(URL::URL const&) = 0;
    virtual void preconnect(URL::URL const&) = 0;

    virtual RefPtr<ResourceLoaderConnectorRequest> start_request(ByteString const& method, URL::URL const&, HashMap<ByteString, ByteString> const& request_headers = {}, ReadonlyBytes request_body = {}, Core::ProxyData const& = {}) = 0;
    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> websocket_connect(const URL::URL&, ByteString const& origin, Vector<ByteString> const& protocols) = 0;

protected:
    explicit ResourceLoaderConnector();
};

class ResourceLoader : public Core::EventReceiver {
    C_OBJECT_ABSTRACT(ResourceLoader)
public:
    static void initialize(RefPtr<ResourceLoaderConnector>);
    static ResourceLoader& the();

    RefPtr<Resource> load_resource(Resource::Type, LoadRequest&);

    using SuccessCallback = JS::SafeFunction<void(ReadonlyBytes, HashMap<ByteString, ByteString, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> status_code)>;
    using ErrorCallback = JS::SafeFunction<void(ByteString const&, Optional<u32> status_code, ReadonlyBytes payload, HashMap<ByteString, ByteString, CaseInsensitiveStringTraits> const& response_headers)>;
    using TimeoutCallback = JS::SafeFunction<void()>;

    void load(LoadRequest&, SuccessCallback success_callback, ErrorCallback error_callback = nullptr, Optional<u32> timeout = {}, TimeoutCallback timeout_callback = nullptr);

    ResourceLoaderConnector& connector() { return *m_connector; }

    void prefetch_dns(URL::URL const&);
    void preconnect(URL::URL const&);

    Function<void()> on_load_counter_change;

    int pending_loads() const { return m_pending_loads; }

    String const& user_agent() const { return m_user_agent; }
    void set_user_agent(String user_agent) { m_user_agent = move(user_agent); }

    String const& platform() const { return m_platform; }
    void set_platform(String platform) { m_platform = move(platform); }

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
    String m_platform;
    Optional<JS::GCPtr<Page>> m_page {};
};

}
