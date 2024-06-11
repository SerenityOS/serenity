/*
 * Copyright (c) 2021-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/SOCKSProxyClient.h>
#include <LibCore/Timer.h>
#include <LibTLS/TLSv12.h>
#include <LibThreading/RWLockProtected.h>
#include <LibURL/URL.h>

#undef REQUESTSERVER_DEBUG
#define REQUESTSERVER_DEBUG 0

namespace RequestServer {

enum class CacheLevel {
    ResolveOnly,
    CreateConnection,
};

}

namespace RequestServer::ConnectionCache {

struct Proxy {
    Core::ProxyData data;
    OwnPtr<Core::SOCKSProxyClient> proxy_client_storage {};

    template<typename SocketType, typename StorageType, typename... Args>
    Coroutine<ErrorOr<NonnullOwnPtr<StorageType>>> tunnel(URL::URL const& url, Args&&... args)
    {
        if (data.type == Core::ProxyData::Direct) {
            co_return CO_TRY(co_await SocketType::async_connect(CO_TRY(url.serialized_host()).to_byte_string(), url.port_or_default(), forward<Args>(args)...));
        }

        if (data.type == Core::ProxyData::SOCKS5) {
            if constexpr (requires { SocketType::connect(declval<ByteString>(), *proxy_client_storage, forward<Args>(args)...); }) {
                proxy_client_storage = CO_TRY(co_await Core::SOCKSProxyClient::async_connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, CO_TRY(url.serialized_host()).to_byte_string(), url.port_or_default()));
                co_return CO_TRY(co_await SocketType::async_connect(CO_TRY(url.serialized_host()).to_byte_string(), *proxy_client_storage, forward<Args>(args)...));
            } else if constexpr (IsSame<SocketType, Core::TCPSocket>) {
                co_return CO_TRY(co_await Core::SOCKSProxyClient::async_connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, CO_TRY(url.serialized_host()).to_byte_string(), url.port_or_default()));
            } else {
                co_return Error::from_string_literal("SOCKS5 not supported for this socket type");
            }
        }
        VERIFY_NOT_REACHED();
    }
};

struct JobData {
    Function<void(Core::BufferedSocketBase&)> start {};
    Function<void(Core::NetworkJob::Error)> fail {};
    Function<Vector<TLS::Certificate>()> provide_client_certificates {};
#if REQUESTSERVER_DEBUG
    struct {
        bool valid { true };
        Core::ElapsedTimer timer {};
        URL::URL url {};
        Duration waiting_in_queue {};
        Duration starting_connection {};
        Duration performing_request {};
    } timing_info {};

    ~JobData()
    {
        if (!timing_info.valid)
            return;
        dbgln("[RSTIMING] JobData for {} timings:", timing_info.url);
        dbgln("[RSTIMING]   - Waiting in queue: {}ms", timing_info.waiting_in_queue.to_milliseconds());
        dbgln("[RSTIMING]   - Starting connection: {}ms", timing_info.starting_connection.to_milliseconds());
        dbgln("[RSTIMING]   - Performing request: {}ms", timing_info.performing_request.to_milliseconds());
    }

    JobData(JobData&& other)
        : start(move(other.start))
        , fail(move(other.fail))
        , provide_client_certificates(move(other.provide_client_certificates))
        , timing_info(move(other.timing_info))
    {
        other.timing_info.valid = false;
    }

    JobData(
        Function<void(Core::BufferedSocketBase&)> start,
        Function<void(Core::NetworkJob::Error)> fail,
        Function<Vector<TLS::Certificate>()> provide_client_certificates,
        decltype(timing_info) timing_info)
        : start(move(start))
        , fail(move(fail))
        , provide_client_certificates(move(provide_client_certificates))
        , timing_info(move(timing_info))
    {
    }
#endif

    template<typename T>
    static JobData create(NonnullRefPtr<T> job)
    {
        return JobData {
            /* .start = */ [job](auto& socket) { job->start(socket); },
            /* .fail = */ [job](auto error) { job->fail(error); },
            /* .provide_client_certificates = */ [job] {
                if constexpr (requires { job->on_certificate_requested; }) {
                    if (job->on_certificate_requested)
                        return job->on_certificate_requested();
                } else {
                    // "use" `job`, otherwise clang gets sad.
                    (void)job;
                }
                return Vector<TLS::Certificate> {}; },
#if REQUESTSERVER_DEBUG
            /* .timing_info = */ {
                .timer = Core::ElapsedTimer::start_new(Core::TimerType::Precise),
                .url = job->url(),
            },
#endif
        };
    }
};

template<typename Socket, typename SocketStorageType = Socket>
struct Connection {
    using QueueType = Vector<JobData>;
    using SocketType = Socket;
    using StorageType = SocketStorageType;

    OwnPtr<Core::BufferedSocket<SocketStorageType>> socket;
    Threading::RWLockProtected<QueueType> request_queue;
    NonnullRefPtr<Core::Timer> removal_timer;
    Atomic<bool> is_being_started { false };
    bool has_started { false };
    URL::URL current_url {};
    Core::ElapsedTimer timer {};
    Optional<JobData> job_data {};
    Proxy proxy {};
    size_t max_queue_length { 0 };
};

struct ConnectionKey {
    ByteString hostname;
    u16 port { 0 };
    Core::ProxyData proxy_data {};

    bool operator==(ConnectionKey const&) const = default;
};
};

template<>
struct AK::Traits<RequestServer::ConnectionCache::ConnectionKey> : public AK::DefaultTraits<RequestServer::ConnectionCache::ConnectionKey> {
    static u32 hash(RequestServer::ConnectionCache::ConnectionKey const& key)
    {
        return pair_int_hash(pair_int_hash(key.proxy_data.host_ipv4, key.proxy_data.port), pair_int_hash(key.hostname.hash(), key.port));
    }
};

namespace RequestServer::ConnectionCache {

struct InferredServerProperties {
    size_t requests_served_per_connection { NumericLimits<size_t>::max() };
};

extern Threading::RWLockProtected<HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<Core::TCPSocket, Core::Socket>>>>>> g_tcp_connection_cache;
extern Threading::RWLockProtected<HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<TLS::TLSv12>>>>>> g_tls_connection_cache;
extern Threading::RWLockProtected<HashMap<ByteString, InferredServerProperties>> g_inferred_server_properties;

void request_did_finish(URL::URL const&, Core::Socket const*);
void dump_jobs();

constexpr static size_t MaxConcurrentConnectionsPerURL = 4;
constexpr static size_t ConnectionKeepAliveTimeMilliseconds = 10'000;
constexpr static size_t ConnectionCacheQueueHighWatermark = 4;

template<typename T>
Coroutine<ErrorOr<void>> recreate_socket_if_needed(T& connection, URL::URL const& url)
{
    using SocketType = typename T::SocketType;
    using SocketStorageType = typename T::StorageType;

    if (!connection.socket || !connection.socket->is_open() || connection.socket->is_eof()) {
        connection.socket = nullptr;
        // Create another socket for the connection.
        auto set_socket = [&](NonnullOwnPtr<SocketStorageType>&& socket) -> ErrorOr<void> {
            connection.socket = TRY(Core::BufferedSocket<SocketStorageType>::create(move(socket)));
            return {};
        };

        if constexpr (IsSame<TLS::TLSv12, SocketType>) {
            TLS::Options options;
            options.set_alert_handler([&connection](TLS::AlertDescription alert) {
                Core::NetworkJob::Error reason;
                if (alert == TLS::AlertDescription::HANDSHAKE_FAILURE)
                    reason = Core::NetworkJob::Error::ProtocolFailed;
                else if (alert == TLS::AlertDescription::DECRYPT_ERROR)
                    reason = Core::NetworkJob::Error::ConnectionFailed;
                else
                    reason = Core::NetworkJob::Error::TransmissionFailed;

                if (connection.job_data->fail)
                    connection.job_data->fail(reason);
            });
            options.set_certificate_provider([&connection]() -> Vector<TLS::Certificate> {
                if (connection.job_data->provide_client_certificates)
                    return connection.job_data->provide_client_certificates();
                return {};
            });
            CO_TRY(set_socket(CO_TRY(co_await (connection.proxy.template tunnel<SocketType, SocketStorageType>(url, move(options))))));
        } else {
            CO_TRY(set_socket(CO_TRY(co_await (connection.proxy.template tunnel<SocketType, SocketStorageType>(url)))));
        }
        dbgln_if(REQUESTSERVER_DEBUG, "Creating a new socket for {} -> {}", url, connection.socket);
    }
    co_return {};
}

Coroutine<void> async_get_or_create_connection(auto& cache, URL::URL url, auto job, Core::ProxyData proxy_data = {})
{
    using CacheEntryType = RemoveCVReference<decltype(*declval<typename RemoveCVReference<decltype(cache)>::ProtectedType>().begin()->value)>;

    auto hostname = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_byte_string();
    auto& properties = g_inferred_server_properties.with_write_locked([&](auto& map) -> InferredServerProperties& { return map.ensure(hostname); });

    auto& sockets_for_url = *cache.with_write_locked([&](auto& map) -> CacheEntryType* {
        return map.ensure({ move(hostname), url.port_or_default(), proxy_data }, [] { return make<CacheEntryType>(); }).ptr();
    });

    // Find the connection with an empty queue; if none exist, we'll find the least backed-up connection later.
    // Note that servers that are known to serve a single request per connection (e.g. HTTP/1.0) usually have
    // issues with concurrent connections, so we'll only allow one connection per URL in that case to avoid issues.
    // This is a bit too aggressive, but there's no way to know if the server can handle concurrent connections
    // without trying it out first, and that's not worth the effort as HTTP/1.0 is a legacy protocol anyway.
    auto it = cache.with_read_locked([&](auto&) {
        return sockets_for_url.find_if([&](auto& connection) {
            return properties.requests_served_per_connection < 2
                || connection->request_queue.with_read_locked([&](auto const& queue) { return queue.size(); }) < ConnectionCacheQueueHighWatermark;
        });
    });
    auto did_add_new_connection = false;
    auto failed_to_find_a_socket = it.is_end();
    size_t index;
    Proxy proxy { proxy_data };

    auto start_timer = Core::ElapsedTimer::start_new();
    if (failed_to_find_a_socket && sockets_for_url.size() < ConnectionCache::MaxConcurrentConnectionsPerURL) {
        using ConnectionType = RemoveCVReference<decltype(*declval<CacheEntryType>().at(0))>;
        cache.with_write_locked([&](auto&) {
            sockets_for_url.append(make<ConnectionType>(
                nullptr,
                typename ConnectionType::QueueType {},
                Core::Timer::create_single_shot(ConnectionKeepAliveTimeMilliseconds, nullptr),
                true));
            index = sockets_for_url.size() - 1;
        });
        auto& socket_for_url = sockets_for_url[index];
        ScopeGuard created = [&] {
            socket_for_url->is_being_started = false;
        };

        auto connection_result = co_await proxy.tunnel<typename ConnectionType::SocketType, typename ConnectionType::StorageType>(url);
        if (connection_result.is_error()) {
            dbgln("ConnectionCache: Connection to {} failed: {}", url, connection_result.error());
            Core::deferred_invoke([job] {
                job->fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            co_return;
        }
        auto socket_result = Core::BufferedSocket<typename ConnectionType::StorageType>::create(connection_result.release_value());
        if (socket_result.is_error()) {
            dbgln("ConnectionCache: Failed to make a buffered socket for {}: {}", url, socket_result.error());
            Core::deferred_invoke([job] {
                job->fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            co_return;
        }

        socket_for_url->socket = socket_result.release_value();
        socket_for_url->proxy = move(proxy);
        did_add_new_connection = true;
    }
    if (failed_to_find_a_socket) {
        if (!did_add_new_connection) {
            // Find the least backed-up connection (based on how many entries are in their request queue).
            index = 0;
            auto min_queue_size = (size_t)-1;
            for (auto it = sockets_for_url.begin(); it != sockets_for_url.end(); ++it) {
                if (auto queue_size = (*it)->request_queue.with_read_locked([](auto& queue) { return queue.size(); }); min_queue_size > queue_size) {
                    index = it.index();
                    min_queue_size = queue_size;
                }
            }
        }
    } else {
        index = it.index();
    }
    if (sockets_for_url.is_empty()) {
        Core::deferred_invoke([job] {
            job->fail(Core::NetworkJob::Error::ConnectionFailed);
        });
        co_return;
    }

    auto& connection = *sockets_for_url[index];
    if (connection.is_being_started) {
        dbgln_if(REQUESTSERVER_DEBUG, "Enqueue request for URL {} in {} - {}", url, &connection, connection.socket);
        connection.request_queue.with_write_locked([&](auto& queue) {
            queue.append(JobData::create(job));
            connection.max_queue_length = max(connection.max_queue_length, queue.size());
        });
        co_return;
    }

    auto connection_time = start_timer.elapsed_milliseconds();

    if (!connection.has_started) {
        connection.has_started = true;
        Core::deferred_invoke([&connection, url, job = move(job), connection_time] {
            Core::run_async_in_current_event_loop([&connection, url = move(url), job = move(job), connection_time] -> Coroutine<void> {
                auto timer = Core::ElapsedTimer::start_new();
                // if !REQUESTSERVER_DEBUG, this is unused.
                (void)connection_time;
                (void)timer;

                if (auto result = co_await recreate_socket_if_needed(connection, url); result.is_error()) {
                    if constexpr (REQUESTSERVER_DEBUG) {
                        connection.job_data->timing_info.starting_connection += Duration::from_milliseconds(timer.elapsed_milliseconds());
                    }
                    dbgln("ConnectionCache: request failed to start, failed to make a socket: {}", result.error());
                    Core::deferred_invoke([job] {
                        job->fail(Core::NetworkJob::Error::ConnectionFailed);
                    });
                } else {
                    dbgln_if(REQUESTSERVER_DEBUG, "Immediately start request for url {} in {} - {}", url, &connection, connection.socket);
                    connection.removal_timer->stop();
                    connection.timer.start();
                    connection.current_url = url;
                    connection.job_data = JobData::create(job);
                    if constexpr (REQUESTSERVER_DEBUG)
                        connection.job_data->timing_info.starting_connection += Duration::from_milliseconds(timer.elapsed_milliseconds() + connection_time);
                    connection.socket->set_notifications_enabled(true);
                    connection.job_data->start(*connection.socket);
                }
            });
        });
    } else {
        dbgln_if(REQUESTSERVER_DEBUG, "Enqueue request for URL {} in {} - {}", url, &connection, connection.socket);
        connection.request_queue.with_write_locked([&](auto& queue) {
            queue.append(JobData::create(job));
            connection.max_queue_length = max(connection.max_queue_length, queue.size());
        });
    }
}

void ensure_connection(auto& cache, URL::URL const& url, auto job, Core::ProxyData proxy_data = {})
{
    Core::EventLoop::current().adopt_coroutine(async_get_or_create_connection(cache, url, move(job), proxy_data));
}
}
