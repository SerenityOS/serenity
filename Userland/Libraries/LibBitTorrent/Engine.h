/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Announcer.h"
#include "Checker.h"
#include "Configuration.h"
#include "FixedSizeByteString.h"
#include "Message.h"
#include "MetaInfo.h"
#include "Net/ConnectionManager.h"
#include "Peer.h"
#include "PeerSession.h"
#include "Torrent.h"
#include "TorrentView.h"
#include <AK/HashMap.h>
#include <LibCore/Object.h>

namespace BitTorrent {

class Engine : public Core::Object {
    C_OBJECT(Engine);

public:
    static ErrorOr<NonnullRefPtr<Engine>> try_create(Configuration config);
    void add_torrent(NonnullOwnPtr<MetaInfo>, DeprecatedString);
    void start_torrent(InfoHash info_hash);
    void stop_torrent(InfoHash);
    void cancel_checking(InfoHash);
    void register_views_update_callback(int interval_ms, Function<void(NonnullOwnPtr<HashMap<InfoHash, TorrentView>>)> callback);

private:
    Engine(Configuration config, NonnullRefPtr<ConnectionManager> connection_manager);

    u64 const BLOCK_LENGTH = 16 * KiB;
    Configuration const m_config;

    OwnPtr<Core::EventLoop> m_event_loop;
    RefPtr<Threading::Thread> m_thread;

    Checker m_checker;
    NonnullRefPtr<ConnectionManager> m_connection_manager;

    HashMap<InfoHash, NonnullRefPtr<Announcer>> m_announcers;
    HashMap<InfoHash, NonnullRefPtr<Torrent>> m_torrents;
    HashMap<InfoHash, NonnullOwnPtr<TorrentDataFileMap>> m_torrent_data_file_maps;

    HashMap<ConnectionId, NonnullRefPtr<Peer>> m_connecting_peers;
    HashMap<ConnectionId, NonnullRefPtr<PeerSession>> m_all_sessions;

    NonnullOwnPtr<HashMap<ConnectionId, ConnectionStats>> m_connection_stats { make<HashMap<ConnectionId, ConnectionStats>>() };
    CheckerStats m_checker_stats;

    NonnullOwnPtr<HashMap<InfoHash, TorrentView>> torrents_views();
    void check_torrent(NonnullRefPtr<Torrent> err_or_checked_bitfield, Function<void()> on_success);

    u64 available_slots_for_torrent(NonnullRefPtr<Torrent> torrent) const;
    void connect_more_peers(NonnullRefPtr<Torrent>);
    ErrorOr<void> piece_downloaded(u64 index, ReadonlyBytes data, NonnullRefPtr<PeerSession> peer);
    ErrorOr<void> piece_or_peer_availability_updated(NonnullRefPtr<Torrent> torrent);
    ErrorOr<void> peer_has_piece(u64 piece_index, NonnullRefPtr<PeerSession> peer);
    void insert_piece_in_heap(NonnullRefPtr<Torrent> torrent, u64 piece_index);

    ErrorOr<void> parse_input_message(ConnectionId connection_id, ReadonlyBytes message_bytes);
    ErrorOr<void> handle_bitfield(NonnullOwnPtr<BitFieldMessage>, NonnullRefPtr<PeerSession>);
    ErrorOr<void> handle_have(NonnullOwnPtr<HaveMessage> have_message, NonnullRefPtr<PeerSession> session);
    ErrorOr<void> handle_interested(NonnullRefPtr<PeerSession> session);
    ErrorOr<void> handle_piece(NonnullOwnPtr<PieceMessage>, NonnullRefPtr<PeerSession> session);
    ErrorOr<void> handle_request(NonnullOwnPtr<RequestMessage>, NonnullRefPtr<PeerSession> session);
};

}
