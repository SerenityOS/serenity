/*
 * Copyright (c) 2021-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/SOCKSProxyClient.h>
#include <LibCore/Timer.h>
#include <LibTLS/TLSv12.h>

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
    ErrorOr<NonnullOwnPtr<StorageType>> tunnel(URL const& url, Args&&... args)
    {
        if (data.type == Core::ProxyData::Direct) {
            return TRY(SocketType::connect(url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), url.port_or_default(), forward<Args>(args)...));
        }
        if (data.type == Core::ProxyData::SOCKS5) {
            if constexpr (requires { SocketType::connect(declval<DeprecatedString>(), *proxy_client_storage, forward<Args>(args)...); }) {
                proxy_client_storage = TRY(Core::SOCKSProxyClient::connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), url.port_or_default()));
                return TRY(SocketType::connect(url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), *proxy_client_storage, forward<Args>(args)...));
            } else if constexpr (IsSame<SocketType, Core::TCPSocket>) {
                return TRY(Core::SOCKSProxyClient::connect(data.host_ipv4, data.port, Core::SOCKSProxyClient::Version::V5, url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), url.port_or_default()));
            } else {
                return Error::from_string_literal("SOCKS5 not supported for this socket type");
            }
        }
        VERIFY_NOT_REACHED();
    }
};

template<typename Socket, typename SocketStorageType = Socket>
struct Connection {
    struct JobData {
        Function<void(Core::BufferedSocketBase&)> start {};
        Function<void(Core::NetworkJob::Error)> fail {};
        Function<Vector<TLS::Certificate>()> provide_client_certificates {};

        template<typename T>
        static JobData create(T& job)
        {
            // Clang-format _really_ messes up formatting this, so just format it manually.
            // clang-format off
            return JobData {
                .start = [&job](auto& socket) {
                    job.start(socket);
                },
                .fail = [&job](auto error) {
                    job.fail(error);
                },
                .provide_client_certificates = [&job] {
                    if constexpr (requires { job.on_certificate_requested; }) {
                        if (job.on_certificate_requested)
                            return job.on_certificate_requested();
                    } else {
                        // "use" `job`, otherwise clang gets sad.
                        (void)job;
                    }
                    return Vector<TLS::Certificate> {};
                },
            };
            // clang-format on
        }
    };
    using QueueType = Vector<JobData>;
    using SocketType = Socket;
    using StorageType = SocketStorageType;

    NonnullOwnPtr<Core::BufferedSocket<SocketStorageType>> socket;
    QueueType request_queue;
    NonnullRefPtr<Core::Timer> removal_timer;
    bool has_started { false };
    URL current_url {};
    Core::ElapsedTimer timer {};
    JobData job_data {};
    Proxy proxy {};
};

struct ConnectionKey {
    DeprecatedString hostname;
    u16 port { 0 };
    Core::ProxyData proxy_data {};

    bool operator==(ConnectionKey const&) const = default;
};

};

template<>
struct AK::Traits<RequestServer::ConnectionCache::ConnectionKey> : public AK::GenericTraits<RequestServer::ConnectionCache::ConnectionKey> {
    static u32 hash(RequestServer::ConnectionCache::ConnectionKey const& key)
    {
        return pair_int_hash(pair_int_hash(key.proxy_data.host_ipv4, key.proxy_data.port), pair_int_hash(key.hostname.hash(), key.port));
    }
};

namespace RequestServer::ConnectionCache {

extern HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<Core::TCPSocket, Core::Socket>>>>> g_tcp_connection_cache;
extern HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<TLS::TLSv12>>>>> g_tls_connection_cache;

void request_did_finish(URL const&, Core::Socket const*);
void dump_jobs();

constexpr static size_t MaxConcurrentConnectionsPerURL = 4;
constexpr static size_t ConnectionKeepAliveTimeMilliseconds = 10'000;

template<typename T>
ErrorOr<void> recreate_socket_if_needed(T& connection, URL const& url)
{
    using SocketType = typename T::SocketType;
    using SocketStorageType = typename T::StorageType;

    if (!connection.socket->is_open() || connection.socket->is_eof()) {
        // Create another socket for the connection.
        auto set_socket = [&](auto socket) -> ErrorOr<void> {
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

                if (connection.job_data.fail)
                    connection.job_data.fail(reason);
            });
            options.set_certificate_provider([&connection]() -> Vector<TLS::Certificate> {
                if (connection.job_data.provide_client_certificates)
                    return connection.job_data.provide_client_certificates();
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

decltype(auto) get_or_create_connection(auto& cache, URL const& url, auto& job, Core::ProxyData proxy_data = {})
{
    using CacheEntryType = RemoveCVReference<decltype(*cache.begin()->value)>;
    auto& sockets_for_url = *cache.ensure({ url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), url.port_or_default(), proxy_data }, [] { return make<CacheEntryType>(); });

    Proxy proxy { proxy_data };

    using ReturnType = decltype(sockets_for_url[0].ptr());
    auto it = sockets_for_url.find_if([](auto& connection) { return connection->request_queue.is_empty(); });
    auto did_add_new_connection = false;
    auto failed_to_find_a_socket = it.is_end();
    if (failed_to_find_a_socket && sockets_for_url.size() < ConnectionCache::MaxConcurrentConnectionsPerURL) {
        using ConnectionType = RemoveCVReference<decltype(*cache.begin()->value->at(0))>;
        auto connection_result = proxy.tunnel<typename ConnectionType::SocketType, typename ConnectionType::StorageType>(url);
        if (connection_result.is_error()) {
            dbgln("ConnectionCache: Connection to {} failed: {}", url, connection_result.error());
            Core::deferred_invoke([&job] {
                job.fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        auto socket_result = Core::BufferedSocket<typename ConnectionType::StorageType>::create(connection_result.release_value());
        if (socket_result.is_error()) {
            dbgln("ConnectionCache: Failed to make a buffered socket for {}: {}", url, socket_result.error());
            Core::deferred_invoke([&job] {
                job.fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        sockets_for_url.append(make<ConnectionType>(
            socket_result.release_value(),
            typename ConnectionType::QueueType {},
            Core::Timer::create_single_shot(ConnectionKeepAliveTimeMilliseconds, nullptr).release_value_but_fixme_should_propagate_errors()));
        sockets_for_url.last()->proxy = move(proxy);
        did_add_new_connection = true;
    }
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
    }
    if (sockets_for_url.is_empty()) {
        Core::deferred_invoke([&job] {
            job.fail(Core::NetworkJob::Error::ConnectionFailed);
        });
        return ReturnType { nullptr };
    }

    auto& connection = *sockets_for_url[index];
    if (!connection.has_started) {
        if (auto result = recreate_socket_if_needed(connection, url); result.is_error()) {
            dbgln("ConnectionCache: request failed to start, failed to make a socket: {}", result.error());
            Core::deferred_invoke([&job] {
                job.fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        dbgln_if(REQUESTSERVER_DEBUG, "Immediately start request for url {} in {} - {}", url, &connection, connection.socket);
        connection.has_started = true;
        connection.removal_timer->stop();
        connection.timer.start();
        connection.current_url = url;
        connection.job_data = decltype(connection.job_data)::create(job);
        connection.socket->set_notifications_enabled(true);
        connection.job_data.start(*connection.socket);
    } else {
        dbgln_if(REQUESTSERVER_DEBUG, "Enqueue request for URL {} in {} - {}", url, &connection, connection.socket);
        connection.request_queue.append(decltype(connection.job_data)::create(job));
    }
    return &connection;
}

}
