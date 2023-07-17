/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Announcer.h"
#include "Bencode/BDecoder.h"
#include <LibProtocol/Request.h>

namespace BitTorrent {

ErrorOr<NonnullRefPtr<Announcer>> Announcer::create(InfoHash info_hash, Vector<Vector<URL>> announce_urls, PeerId local_peer_id, u16 listen_port, u64 torrent_session_key, Function<AnnounceStats()> get_stats_for_announce, Function<void(Vector<Core::SocketAddress>)> on_success)
{
    auto http_client = TRY(Protocol::RequestClient::try_create());
    return adopt_nonnull_ref_or_enomem(new (nothrow) Announcer(move(http_client), info_hash, move(announce_urls), local_peer_id, listen_port, torrent_session_key, move(get_stats_for_announce), move(on_success)));
}

Announcer::Announcer(NonnullRefPtr<Protocol::RequestClient> http_client, InfoHash info_hash, Vector<Vector<URL>> announce_urls, PeerId local_peer_id, u16 listen_port, u64 torrent_session_key, Function<AnnounceStats()> get_stats_for_announce, Function<void(Vector<Core::SocketAddress>)> on_success)
    : m_http_client(move(http_client))
    , m_info_hash(info_hash)
    , m_announce_urls(move(announce_urls))
    , m_local_peer_id(local_peer_id)
    , m_listen_port(listen_port)
    , m_torrent_session_key(torrent_session_key)
    , m_get_stats_for_announce(move(get_stats_for_announce))
    , m_on_success(move(on_success))
{
    MUST(announce(EventType::Started));
}

void Announcer::completed()
{
    MUST(announce(EventType::Completed));
}
void Announcer::stopped()
{
    MUST(announce(EventType::Stopped));
}

void Announcer::timer_event(Core::TimerEvent&)
{
    MUST(announce());
}

// https://www.bittorrent.org/beps/bep_0003.html#trackers
// https://wiki.theory.org/BitTorrentSpecification#Tracker_Request_Parameters
ErrorOr<void> Announcer::announce(EventType event_type)
{
    // FIXME: Implement the tracker picking algorithm according to BEP 0012 http://bittorrent.org/beps/bep_0012.html
    auto flat_announce_urls = Vector<URL>();
    for (auto& tier : m_announce_urls) {
        for (auto& url : tier) {
            flat_announce_urls.append(url);
        }
    }

    if (m_current_announce_index >= flat_announce_urls.size())
        m_current_announce_index = 0;

    auto url = flat_announce_urls.at(m_current_announce_index);

    auto try_next_url = [&, event_type] {
        deferred_invoke([&, event_type] {
            m_current_announce_index++;
            MUST(announce(event_type));
        });
    };

    if (url.scheme() != "http" && url.scheme() != "https") {
        dbgln("Unsupported tracker protocol: {}", url.scheme());
        try_next_url();
        return {};
    }

    auto info_hash = url_encode_bytes(m_info_hash.bytes());
    auto my_peer_id = url_encode_bytes(m_local_peer_id.bytes());

    auto event_type_param = StringBuilder();
    switch (event_type) {
    case EventType::Started:
        event_type_param.append("started"sv);
        break;
    case EventType::Completed:
        event_type_param.append("completed"sv);
        break;
    case EventType::Stopped:
        event_type_param.append("stopped"sv);
        break;
    case EventType::None:
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    auto stats = m_get_stats_for_announce();
    url.set_query(TRY(String::formatted(
                          "info_hash={}&peer_id={}&port={}&uploaded={}&downloaded={}&left={}&key={}&event={}",
                          info_hash,
                          my_peer_id,
                          m_listen_port,
                          stats.uploaded,
                          stats.downloaded,
                          stats.left,
                          m_torrent_session_key,
                          event_type_param.to_deprecated_string()))
                      .to_deprecated_string());

    dbgln("Announcing: {}", url.to_deprecated_string());

    auto request = m_http_client->start_request("GET", url).release_nonnull();
    m_active_requests.set(request);

    request->on_buffered_request_finish = [&, url, request, event_type, try_next_url](bool success, auto total_size, auto&, auto status_code, ReadonlyBytes payload) {
        dbgln("Announce response: success:{} total_size:{} status_code:{}", success, total_size, status_code);

        if (!success) {
            dbgln("Announce failed with url: {} Retrying with the next one in the list.", url);
            try_next_url();
            return;
        }

        auto err = [&, &payload = payload]() -> ErrorOr<void> {
            auto response = TRY(BDecoder::parse<Dict>(payload));

            if (response.contains("failure reason")) {
                // TODO: Deactivate the announcer/torrent based on the error
                dbgln("Tracker returned failure:  {}", TRY(response.get_string("failure reason")));
                return {};
            }

            auto interval = response.get<i64>("interval");
            dbgln("Interval from response: {} seconds", interval);
            m_interval = interval * 1000;

            Vector<Core::SocketAddress> peers;
            if (response.has<List>("peers")) {
                for (auto peer : response.get<List>("peers")) {
                    auto peer_dict = peer.get<Dict>();
                    auto ip_address = IPv4Address::from_string(TRY(peer_dict.get_string("ip")));
                    // TODO: check if ip string is a host name and resolve it.
                    VERIFY(ip_address.has_value());
                    peers.append({ ip_address.value(), static_cast<u16>(peer_dict.get<i64>("port")) });
                }
            } else {
                // https://www.bittorrent.org/beps/bep_0023.html compact peers list
                auto peers_bytes = response.get<ByteBuffer>("peers");
                VERIFY(peers_bytes.size() % 6 == 0);
                auto stream = FixedMemoryStream(peers_bytes.bytes());
                while (!stream.is_eof())
                    peers.append({ TRY(stream.read_value<NetworkOrdered<u32>>()), TRY(stream.read_value<NetworkOrdered<u16>>()) });
            }

            dbgln("Peers ({}) from tracker:", peers.size());
            for (auto& peer : peers) {
                dbgln("{}", peer);
            }

            m_on_success(move(peers));
            return {};
        }.operator()();

        if (err.is_error()) {
            dbgln("Error parsing announce response: {}", err.error().string_literal());
        };

        deferred_invoke([this, request] {
            m_active_requests.remove(request);
        });

        if (has_timer())
            stop_timer();

        if (event_type != EventType::Stopped)
            start_timer(m_interval);
    };

    request->set_should_buffer_all_input(true);

    return {};
}

DeprecatedString Announcer::url_encode_bytes(ReadonlyBytes bytes)
{
    StringBuilder builder;
    for (size_t i = 0; i < bytes.size(); ++i) {
        builder.appendff("%{:02X}", bytes[i]);
    }
    return builder.to_deprecated_string();
}

}
