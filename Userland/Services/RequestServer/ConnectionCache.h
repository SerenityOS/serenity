/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
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
#include <LibCore/TCPSocket.h>
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
    using QueueType = Vector<Function<void(Core::Socket&)>>;
    using SocketType = Socket;

    NonnullRefPtr<Socket> socket;
    QueueType request_queue;
    NonnullRefPtr<Core::Timer> removal_timer;
    bool has_started { false };
    URL current_url {};
    Core::ElapsedTimer timer {};
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

extern HashMap<ConnectionKey, NonnullOwnPtr<NonnullOwnPtrVector<Connection<Core::TCPSocket>>>> g_tcp_connection_cache;
extern HashMap<ConnectionKey, NonnullOwnPtr<NonnullOwnPtrVector<Connection<TLS::TLSv12>>>> g_tls_connection_cache;

void request_did_finish(URL const&, Core::Socket const*);
void dump_jobs();

constexpr static inline size_t MaxConcurrentConnectionsPerURL = 2;
constexpr static inline size_t ConnectionKeepAliveTimeMilliseconds = 10'000;

decltype(auto) get_or_create_connection(auto& cache, URL const& url, auto& job)
{
    using CacheEntryType = RemoveCVReference<decltype(*cache.begin()->value)>;
    auto start_job = [&job](auto& socket) {
        job.start(socket);
    };
    auto& sockets_for_url = *cache.ensure({ url.host(), url.port_or_default() }, [] { return make<CacheEntryType>(); });
    auto it = sockets_for_url.find_if([](auto& connection) { return connection->request_queue.is_empty(); });
    auto did_add_new_connection = false;
    if (it.is_end() && sockets_for_url.size() < ConnectionCache::MaxConcurrentConnectionsPerURL) {
        using ConnectionType = RemoveCVReference<decltype(cache.begin()->value->at(0))>;
        sockets_for_url.append(make<ConnectionType>(
            ConnectionType::SocketType::construct(nullptr),
            typename ConnectionType::QueueType {},
            Core::Timer::create_single_shot(ConnectionKeepAliveTimeMilliseconds, nullptr)));
        did_add_new_connection = true;
    }
    size_t index;
    if (it.is_end()) {
        if (did_add_new_connection) {
            index = sockets_for_url.size() - 1;
        } else {
            // Find the least backed-up connection (based on how many entries are in their request queue.
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
    auto& connection = sockets_for_url[index];
    if (!connection.has_started) {
        dbgln("Immediately start request for url {} in {} - {}", url, &connection, connection.socket);
        connection.has_started = true;
        connection.removal_timer->stop();
        connection.timer.start();
        connection.current_url = url;
        start_job(*connection.socket);
    } else {
        dbgln("Enqueue request for URL {} in {} - {}", url, &connection, connection.socket);
        connection.request_queue.append(move(start_job));
    }
    return connection;
}

}
