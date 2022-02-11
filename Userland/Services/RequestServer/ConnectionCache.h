/*
 * Copyright (c) 2021-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/NetworkJob.h>
#include <LibCore/Timer.h>
#include <LibTLS/TLSv12.h>

namespace RequestServer {

enum class CacheLevel {
    ResolveOnly,
    CreateConnection,
};

}

namespace RequestServer::ConnectionCache {

template<typename Socket>
struct Connection {
    struct JobData {
        Function<void(Core::Stream::Socket&)> start {};
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

    NonnullOwnPtr<Core::Stream::BufferedSocket<Socket>> socket;
    QueueType request_queue;
    NonnullRefPtr<Core::Timer> removal_timer;
    bool has_started { false };
    URL current_url {};
    Core::ElapsedTimer timer {};
    JobData job_data {};
};

struct ConnectionKey {
    String hostname;
    u16 port { 0 };

    bool operator==(ConnectionKey const&) const = default;
};

};

template<>
struct AK::Traits<RequestServer::ConnectionCache::ConnectionKey> : public AK::GenericTraits<RequestServer::ConnectionCache::ConnectionKey> {
    static u32 hash(RequestServer::ConnectionCache::ConnectionKey const& key)
    {
        return pair_int_hash(key.hostname.hash(), key.port);
    }
};

namespace RequestServer::ConnectionCache {

extern HashMap<ConnectionKey, NonnullOwnPtr<NonnullOwnPtrVector<Connection<Core::Stream::TCPSocket>>>> g_tcp_connection_cache;
extern HashMap<ConnectionKey, NonnullOwnPtr<NonnullOwnPtrVector<Connection<TLS::TLSv12>>>> g_tls_connection_cache;

void request_did_finish(URL const&, Core::Stream::Socket const*);
void dump_jobs();

constexpr static size_t MaxConcurrentConnectionsPerURL = 2;
constexpr static size_t ConnectionKeepAliveTimeMilliseconds = 10'000;

template<typename T>
ErrorOr<void> recreate_socket_if_needed(T& connection, URL const& url)
{
    using SocketType = typename T::SocketType;
    if (!connection.socket->is_open() || connection.socket->is_eof()) {
        // Create another socket for the connection.
        auto set_socket = [&](auto socket) -> ErrorOr<void> {
            connection.socket = TRY(Core::Stream::BufferedSocket<SocketType>::create(move(socket)));
            return {};
        };

        if constexpr (IsSame<TLS::TLSv12, SocketType>) {
            TLS::Options options;
            options.set_alert_handler([&connection](TLS::AlertDescription alert) {
                Core::NetworkJob::Error reason;
                if (alert == TLS::AlertDescription::HandshakeFailure)
                    reason = Core::NetworkJob::Error::ProtocolFailed;
                else if (alert == TLS::AlertDescription::DecryptError)
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
            TRY(set_socket(TRY(SocketType::connect(url.host(), url.port_or_default(), move(options)))));
        } else {
            TRY(set_socket(TRY(SocketType::connect(url.host(), url.port_or_default()))));
        }
        dbgln_if(REQUESTSERVER_DEBUG, "Creating a new socket for {} -> {}", url, connection.socket);
    }
    return {};
}

decltype(auto) get_or_create_connection(auto& cache, URL const& url, auto& job)
{
    using CacheEntryType = RemoveCVReference<decltype(*cache.begin()->value)>;
    auto& sockets_for_url = *cache.ensure({ url.host(), url.port_or_default() }, [] { return make<CacheEntryType>(); });

    using ReturnType = decltype(&sockets_for_url[0]);
    auto it = sockets_for_url.find_if([](auto& connection) { return connection->request_queue.is_empty(); });
    auto did_add_new_connection = false;
    auto failed_to_find_a_socket = it.is_end();
    if (failed_to_find_a_socket && sockets_for_url.size() < ConnectionCache::MaxConcurrentConnectionsPerURL) {
        using ConnectionType = RemoveCVReference<decltype(cache.begin()->value->at(0))>;
        auto connection_result = ConnectionType::SocketType::connect(url.host(), url.port_or_default());
        if (connection_result.is_error()) {
            dbgln("ConnectionCache: Connection to {} failed: {}", url, connection_result.error());
            Core::deferred_invoke([&job] {
                job.fail(Core::NetworkJob::Error::ConnectionFailed);
            });
            return ReturnType { nullptr };
        }
        auto socket_result = Core::Stream::BufferedSocket<typename ConnectionType::SocketType>::create(connection_result.release_value());
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
            Core::Timer::create_single_shot(ConnectionKeepAliveTimeMilliseconds, nullptr)));
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
                if (auto queue_size = it->request_queue.size(); min_queue_size > queue_size) {
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

    auto& connection = sockets_for_url[index];
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
