/*
 * Copyright (c) 2021-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/SOCKSProxyClient.h>
#include <LibCore/Timer.h>
#include <LibTLS/TLSv12.h>
#include <LibThreading/MutexProtected.h>
#include <LibURL/URL.h>

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
    ErrorOr<NonnullOwnPtr<StorageType>> tunnel(URL::URL const& url, Args&&... args)
    {
        if (data.type == Core::ProxyData::Direct) {
            return TRY(SocketType::connect(TRY(url.serialized_host()).to_byte_string(), url.port_or_default(), forward<Args>(args)...));
        }
        if (data.type == Core::ProxyData::SOCKS5) {
            if constexpr (requires { SocketType::connect(declval<ByteString>(), *proxy_client_storage, forward<Args>(args)...); }) {
                proxy_client_storage = TRY(Core::SOCKSProxyClient::connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, TRY(url.serialized_host()).to_byte_string(), url.port_or_default()));
                return TRY(SocketType::connect(TRY(url.serialized_host()).to_byte_string(), *proxy_client_storage, forward<Args>(args)...));
            } else if constexpr (IsSame<SocketType, Core::TCPSocket>) {
                return TRY(Core::SOCKSProxyClient::connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, TRY(url.serialized_host()).to_byte_string(), url.port_or_default()));
            } else {
                return Error::from_string_literal("SOCKS5 not supported for this socket type");
            }
        }
        VERIFY_NOT_REACHED();
    }
};

struct JobData {
    Function<void(Core::BufferedSocketBase&)> start {};
    Function<void(Core::NetworkJob::Error)> fail {};
    Function<Vector<TLS::Certificate>()> provide_client_certificates {};
    struct TimingInfo {
        bool valid { true };
        Core::ElapsedTimer timer {};
        URL::URL url {};
        Duration waiting_in_queue {};
        Duration starting_connection {};
        Duration performing_request {};
    } timing_info {};

    JobData(Function<void(Core::BufferedSocketBase&)> start, Function<void(Core::NetworkJob::Error)> fail, Function<Vector<TLS::Certificate>()> provide_client_certificates, TimingInfo timing_info)
        : start(move(start))
        , fail(move(fail))
        , provide_client_certificates(move(provide_client_certificates))
        , timing_info(move(timing_info))
    {
    }

    JobData(JobData&& other)
        : start(move(other.start))
        , fail(move(other.fail))
        , provide_client_certificates(move(other.provide_client_certificates))
        , timing_info(move(other.timing_info))
    {
        other.timing_info.valid = false;
    }

    ~JobData()
    {
        if (timing_info.valid) {
            dbgln("JobData for {} timings:", timing_info.url);
            dbgln("  - Waiting in queue: {}ms", timing_info.waiting_in_queue.to_milliseconds());
            dbgln("  - Starting connection: {}ms", timing_info.starting_connection.to_milliseconds());
            dbgln("  - Performing request: {}ms", timing_info.performing_request.to_milliseconds());
        }
    }

    template<typename T>
    static JobData create(NonnullRefPtr<T> job, URL::URL url)
    {
        return JobData {
            [job](auto& socket) { job->start(socket); },
            [job](auto error) { job->fail(error); },
            [job] {
                if constexpr (requires { job->on_certificate_requested; }) {
                    if (job->on_certificate_requested)
                        return job->on_certificate_requested();
                } else {
                    // "use" `job`, otherwise clang gets sad.
                    (void)job;
                }
                return Vector<TLS::Certificate> {};
            },
            {
                .timer = Core::ElapsedTimer::start_new(),
                .url = move(url),
                .waiting_in_queue = {},
                .starting_connection = {},
                .performing_request = {},
            },
        };
    }
};

template<typename Socket, typename SocketStorageType = Socket>
struct Connection {
    using QueueType = Vector<JobData>;
    using SocketType = Socket;
    using StorageType = SocketStorageType;

    OwnPtr<Core::BufferedSocket<SocketStorageType>> socket;
    QueueType request_queue;
    NonnullRefPtr<Core::Timer> removal_timer;
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

extern Threading::MutexProtected<HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<Core::TCPSocket, Core::Socket>>>>>> g_tcp_connection_cache;
extern Threading::MutexProtected<HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<TLS::TLSv12>>>>>> g_tls_connection_cache;
extern Threading::MutexProtected<HashMap<ByteString, InferredServerProperties>> g_inferred_server_properties;

void request_did_finish(URL::URL const&, Core::Socket const*);
void dump_jobs();

constexpr static size_t MaxConcurrentConnectionsPerURL = 4;
constexpr static size_t ConnectionKeepAliveTimeMilliseconds = 20'000;
constexpr static size_t ConnectionCacheQueueHighWatermark = 4;

template<typename T>
ErrorOr<void> recreate_socket_if_needed(T& connection, URL::URL const& url)
{
    using SocketType = typename T::SocketType;
    using SocketStorageType = typename T::StorageType;

    if (!connection.socket->is_open() || connection.socket->is_eof()) {
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
            TRY(set_socket(TRY((connection.proxy.template tunnel<SocketType, SocketStorageType>(url, move(options))))));
        } else {
            TRY(set_socket(TRY((connection.proxy.template tunnel<SocketType, SocketStorageType>(url)))));
        }
        dbgln_if(REQUESTSERVER_DEBUG, "Creating a new socket for {} -> {}", url, connection.socket);
    }
    return {};
}

extern size_t hits;
extern size_t misses;

template<typename ReturnType>
ReturnType start_connection(const URL::URL& url, auto job, auto& sockets_for_url, size_t index, Duration);

decltype(auto) get_or_create_connection(auto& cache, URL::URL const& url, auto job, Core::ProxyData proxy_data = {})
{
    using CacheEntryType = RemoveCVReference<decltype(*declval<typename RemoveCVReference<decltype(cache)>::ProtectedType>().begin()->value)>;

    auto hostname = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_byte_string();
    auto& properties = g_inferred_server_properties.with_locked([&](auto& map) -> InferredServerProperties& { return map.ensure(hostname); });

    auto& sockets_for_url = *cache.with_locked([&](auto& map) -> NonnullOwnPtr<CacheEntryType>& {
        return map.ensure({ move(hostname), url.port_or_default(), proxy_data }, [] { return make<CacheEntryType>(); });
    });

    using ReturnType = decltype(sockets_for_url[0].ptr());

    // Find the connection with an empty queue; if none exist, we'll find the least backed-up connection later.
    // Note that servers that are known to serve a single request per connection (e.g. HTTP/1.0) usually have
    // issues with concurrent connections, so we'll only allow one connection per URL in that case to avoid issues.
    // This is a bit too aggressive, but there's no way to know if the server can handle concurrent connections
    // without trying it out first, and that's not worth the effort as HTTP/1.0 is a legacy protocol anyway.
    auto it = sockets_for_url.find_if([&](auto& connection) { return properties.requests_served_per_connection < 2 || connection->request_queue.size() <= ConnectionCacheQueueHighWatermark; });
    auto did_add_new_connection = false;
    auto failed_to_find_a_socket = it.is_end();

    Proxy proxy { proxy_data };

    auto timer = Core::ElapsedTimer::start_new();
    if (failed_to_find_a_socket && sockets_for_url.size() < MaxConcurrentConnectionsPerURL) {
        using ConnectionType = RemoveCVReference<decltype(*AK::Detail::declval<CacheEntryType>().at(0))>;
        auto& connection = cache.with_locked([&](auto&) -> ConnectionType& {
            sockets_for_url.append(AK::make<ConnectionType>(
                nullptr,
                typename ConnectionType::QueueType {},
                Core::Timer::create_single_shot(ConnectionKeepAliveTimeMilliseconds, nullptr)));
            auto& connection = sockets_for_url.last();
            connection->proxy = move(proxy);
            return *connection;
        });
        dbgln("{} will start a connection for URL {}", pthread_self(), url);

        auto connection_result = proxy.tunnel<typename ConnectionType::SocketType, typename ConnectionType::StorageType>(url);
        misses++;
        if (connection_result.is_error()) {
            dbgln("ConnectionCache: Connection to {} failed: {}", url, connection_result.error());
            Core::deferred_invoke([job] {
                job->fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        auto socket_result = Core::BufferedSocket<typename ConnectionType::StorageType>::create(connection_result.release_value());
        if (socket_result.is_error()) {
            dbgln("ConnectionCache: Failed to make a buffered socket for {}: {}", url, socket_result.error());
            Core::deferred_invoke([job] {
                job->fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        did_add_new_connection = true;
        connection.socket = socket_result.release_value();
    }

    auto elapsed = Duration::from_milliseconds(timer.elapsed_milliseconds());

    size_t index;
    if (failed_to_find_a_socket) {
        if (did_add_new_connection) {
            index = sockets_for_url.size() - 1;
        } else {
            // Find the least backed-up connection (based on how many entries are in their request queue).
            index = 0;
            auto min_queue_size = (size_t)-1;
            for (auto it = sockets_for_url.begin(); it != sockets_for_url.end(); ++it) {
                if (auto queue_size = (*it)->request_queue.size(); min_queue_size > queue_size) {
                    index = it.index();
                    min_queue_size = queue_size;
                }
            }
        }
    } else {
        index = it.index();
        hits++;
    }

    dbgln("ConnectionCache: Hits: {}, Misses: {}", RequestServer::ConnectionCache::hits, RequestServer::ConnectionCache::misses);
    return start_connection<ReturnType>(url, job, sockets_for_url, index, elapsed);
}

template<typename ReturnType>
ReturnType start_connection(URL::URL const& url, auto job, auto& sockets_for_url, size_t index, Duration setup_time)
{
    if (sockets_for_url.is_empty()) {
        Core::deferred_invoke([job] {
            job->fail(Core::NetworkJob::Error::ConnectionFailed);
        });
        return ReturnType { nullptr };
    }

    auto& connection = *sockets_for_url[index];
    if (!connection.socket) {
        dbgln("{} waiting for connection to be created for URL {}", pthread_self(), url);
        Core::deferred_invoke_if([=, &sockets_for_url] {
            start_connection<ReturnType>(url, job, sockets_for_url, index, setup_time);
        }, [&connection]() -> bool { return connection.socket; });
        return nullptr;
    }

    if (!connection.has_started) {
        connection.has_started = true;
        Core::deferred_invoke([&connection, url, job, setup_time] {
            auto job_data = JobData::create(job, url);
            job_data.timing_info.waiting_in_queue = Duration::from_milliseconds(job_data.timing_info.timer.elapsed_milliseconds());
            job_data.timing_info.timer.start();
            if (auto result = recreate_socket_if_needed(connection, url); result.is_error()) {
                dbgln("ConnectionCache: request failed to start, failed to make a socket: {}", result.error());
                job_data.timing_info.starting_connection += Duration::from_milliseconds(job_data.timing_info.timer.elapsed_milliseconds()) + setup_time;
                job_data.timing_info.timer.start();
                Core::deferred_invoke([job] {
                    job->fail(Core::NetworkJob::Error::ConnectionFailed);
                });
            } else {
                dbgln_if(REQUESTSERVER_DEBUG, "Immediately start request for url {} in {} - {}", url, &connection, connection.socket);
                connection.job_data = move(job_data);
                connection.job_data->timing_info.starting_connection += Duration::from_milliseconds(connection.job_data->timing_info.timer.elapsed_milliseconds()) + setup_time;
                connection.job_data->timing_info.timer.start();
                connection.removal_timer->stop();
                connection.timer.start();
                connection.current_url = url;
                connection.socket->set_notifications_enabled(true);
                connection.job_data->start(*connection.socket);
            }
        });
    } else {
        dbgln_if(REQUESTSERVER_DEBUG, "Enqueue request for URL {} in {} - {}", url, &connection, connection.socket);
        connection.request_queue.append(JobData::create(job, url));
        connection.max_queue_length = max(connection.max_queue_length, connection.request_queue.size());
    }
    return &connection;
}

}
