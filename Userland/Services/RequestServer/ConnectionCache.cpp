/*
 * Copyright (c) 2021-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionCache.h"
#include <AK/Debug.h>
#include <AK/Find.h>
#include <LibCore/EventLoop.h>

namespace RequestServer::ConnectionCache {

HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<Core::TCPSocket, Core::Socket>>>>> g_tcp_connection_cache {};
HashMap<ConnectionKey, NonnullOwnPtr<Vector<NonnullOwnPtr<Connection<TLS::TLSv12>>>>> g_tls_connection_cache {};

void request_did_finish(URL const& url, Core::Socket const* socket)
{
    if (!socket) {
        dbgln("Request with a null socket finished for URL {}", url);
        return;
    }

    dbgln_if(REQUESTSERVER_DEBUG, "Request for {} finished", url);

    ConnectionKey partial_key { url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string(), url.port_or_default() };
    auto fire_off_next_job = [&](auto& cache) {
        auto it = find_if(cache.begin(), cache.end(), [&](auto& connection) { return connection.key.hostname == partial_key.hostname && connection.key.port == partial_key.port; });
        if (it == cache.end()) {
            dbgln("Request for URL {} finished, but we don't own that!", url);
            return;
        }
        auto connection_it = it->value->find_if([&](auto& connection) { return connection->socket == socket; });
        if (connection_it.is_end()) {
            dbgln("Request for URL {} finished, but we don't have a socket for that!", url);
            return;
        }

        auto& connection = *connection_it;
        if (connection->request_queue.is_empty()) {
            Core::deferred_invoke([&connection, &cache_entry = *it->value, key = it->key, &cache] {
                connection->socket->set_notifications_enabled(false);
                connection->has_started = false;
                connection->current_url = {};
                connection->job_data = {};
                connection->removal_timer->on_timeout = [ptr = connection.ptr(), &cache_entry, key = move(key), &cache]() mutable {
                    Core::deferred_invoke([&, key = move(key), ptr] {
                        dbgln_if(REQUESTSERVER_DEBUG, "Removing no-longer-used connection {} (socket {})", ptr, ptr->socket);
                        auto did_remove = cache_entry.remove_first_matching([&](auto& entry) { return entry == ptr; });
                        VERIFY(did_remove);
                        if (cache_entry.is_empty())
                            cache.remove(key);
                    });
                };
                connection->removal_timer->start();
            });
        } else {
            if (auto result = recreate_socket_if_needed(*connection, url); result.is_error()) {
                dbgln("ConnectionCache request finish handler, reconnection failed with {}", result.error());
                connection->job_data.fail(Core::NetworkJob::Error::ConnectionFailed);
                return;
            }
            Core::deferred_invoke([&, url] {
                dbgln_if(REQUESTSERVER_DEBUG, "Running next job in queue for connection {} @{}", &connection, connection->socket);
                connection->timer.start();
                connection->current_url = url;
                connection->job_data = connection->request_queue.take_first();
                connection->socket->set_notifications_enabled(true);
                connection->job_data.start(*connection->socket);
            });
        }
    };

    if (is<Core::BufferedSocket<TLS::TLSv12>>(socket))
        fire_off_next_job(g_tls_connection_cache);
    else if (is<Core::BufferedSocket<Core::Socket>>(socket))
        fire_off_next_job(g_tcp_connection_cache);
    else
        dbgln("Unknown socket {} finished for URL {}", socket, url);
}

void dump_jobs()
{
    dbgln("=========== TLS Connection Cache ==========");
    for (auto& connection : g_tls_connection_cache) {
        dbgln(" - {}:{}", connection.key.hostname, connection.key.port);
        for (auto& entry : *connection.value) {
            dbgln("  - Connection {} (started={}) (socket={})", &entry, entry->has_started, entry->socket);
            dbgln("    Currently loading {} ({} elapsed)", entry->current_url, entry->timer.is_valid() ? entry->timer.elapsed() : 0);
            dbgln("    Request Queue:");
            for (auto& job : entry->request_queue)
                dbgln("    - {}", &job);
        }
    }
    dbgln("=========== TCP Connection Cache ==========");
    for (auto& connection : g_tcp_connection_cache) {
        dbgln(" - {}:{}", connection.key.hostname, connection.key.port);
        for (auto& entry : *connection.value) {
            dbgln("  - Connection {} (started={}) (socket={})", &entry, entry->has_started, entry->socket);
            dbgln("    Currently loading {} ({} elapsed)", entry->current_url, entry->timer.is_valid() ? entry->timer.elapsed() : 0);
            dbgln("    Request Queue:");
            for (auto& job : entry->request_queue)
                dbgln("    - {}", &job);
        }
    }
}

}
