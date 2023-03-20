/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Engine.h"

namespace BitTorrent {

ErrorOr<NonnullRefPtr<Engine>> Engine::try_create(Configuration config)
{
    auto connection_manager = TRY(ConnectionManager::try_create(config.listen_port));
    return adopt_nonnull_ref_or_enomem(new Engine(move(config), move(connection_manager)));
}

void Engine::add_torrent(NonnullOwnPtr<MetaInfo> meta_info, DeprecatedString data_path)
{
    m_event_loop->deferred_invoke([&, meta_info = move(meta_info), data_path] {
        auto torrent_root_dir = meta_info->root_dir_name();
        DeprecatedString optional_root_dir = "";
        if (torrent_root_dir.has_value()) {
            optional_root_dir = DeprecatedString::formatted("/{}", torrent_root_dir.value());
        }

        auto root_data_path = DeprecatedString::formatted("{}{}", data_path, optional_root_dir);

        auto info_hash = InfoHash({ meta_info->info_hash() });
        NonnullRefPtr<Torrent> const& torrent = make_ref_counted<Torrent>(
            meta_info->root_dir_name().value_or(meta_info->files()[0].path),
            root_data_path,
            info_hash,
            PeerId::random(),
            meta_info->total_length(),
            meta_info->piece_length());

        m_torrents.set(info_hash, move(torrent));
    });
}

void Engine::start_torrent(InfoHash info_hash)
{
    m_event_loop->deferred_invoke([this, info_hash]() {
        NonnullRefPtr<Torrent> torrent = *m_torrents.get(info_hash).value();

        auto do_start_torrent = [this, torrent] {
            if (torrent->local_bitfield.progress() < 100) {
                torrent->state = TorrentState::STARTED;
            } else {
                torrent->state = TorrentState::SEEDING;
            }

            connect_more_peers(torrent);
        };

        do_start_torrent();
    });
}

void Engine::stop_torrent(InfoHash info_hash)
{
    m_event_loop->deferred_invoke([&, info_hash] {
        auto torrent = m_torrents.get(info_hash).value();
        torrent->state = TorrentState::STOPPED;
        for (auto& session : m_torrents.get(info_hash).value()->peer_sessions) {
            m_connection_manager->close_connection(session->connection_id, "Stopping torrent");
        }
    });
}

Engine::Engine(Configuration config, NonnullRefPtr<ConnectionManager> connection_manager)
    : m_config(move(config))
    , m_connection_manager(move(connection_manager))
{
    m_thread = Threading::Thread::construct([this]() -> intptr_t {
        m_event_loop = make<Core::EventLoop>();
        return m_event_loop->exec();
    },
        "Engine"sv);

    m_thread->start();

    m_connection_manager->on_connection_established = [&](ConnectionId connection_id) {
        m_event_loop->deferred_invoke([&, connection_id] {
            auto maybe_peer = m_connecting_peers.take(connection_id);
            VERIFY(maybe_peer.has_value());

            auto peer = maybe_peer.release_value();
            if (peer->torrent->state != TorrentState::STARTED && peer->torrent->state != TorrentState::SEEDING) {
                // FIXME: the peer status will end up errored even if it should be available to be reusable.
                m_connection_manager->close_connection(connection_id, "Connection established after torrent stopped");
                return;
            }

            auto session = make_ref_counted<PeerSession>(peer, connection_id, peer->id_from_handshake.value());
            m_all_sessions.set(connection_id, session);
            peer->torrent->peer_sessions.set(session);

            dbgln("Peer connected: {}", *peer);
            m_connection_manager->send_message(connection_id, make<BitFieldMessage>(peer->torrent->local_bitfield));
        });
    };

    m_connection_manager->on_peer_disconnect = [&](ConnectionId connection_id, DeprecatedString reason) {
        m_event_loop->deferred_invoke([&, connection_id, reason] {
            dbgln("Disconnected {}: {}", connection_id, reason);

            RefPtr<Peer> peer;
            if (m_connecting_peers.contains(connection_id)) {
                peer = m_connecting_peers.take(connection_id).release_value();
            } else {
                auto session = m_all_sessions.take(connection_id).release_value();
                peer = session->peer;

                auto torrent = peer->torrent;

                if (torrent->state == TorrentState::STARTED) {
                    auto& piece = session->incoming_piece;
                    if (piece.index.has_value()) {
                        piece.index = {};
                    }
                }

                torrent->peer_sessions.remove(session);
            }

            // FIXME: use an enum
            if (reason == "Stopping torrent")
                peer->status = PeerStatus::Available;
            else
                peer->status = PeerStatus::Errored;

            if (peer->torrent->local_bitfield.progress() < 100 && peer->torrent->state == TorrentState::STARTED)
                connect_more_peers(peer->torrent);
        });
    };

    m_connection_manager->on_handshake_from_outgoing_connection = [&](ConnectionId connection_id, HandshakeMessage handshake, auto accept_connection) {
        m_event_loop->deferred_invoke([&, connection_id, handshake, accept_connection = move(accept_connection)] {
            auto maybe_peer = m_connecting_peers.get(connection_id);
            VERIFY(maybe_peer.has_value());
            auto peer = maybe_peer.release_value();

            if (peer->torrent->info_hash != handshake.info_hash()) {
                dbgln("Peer sent a handshake with the wrong torrent info hash, disconnecting.");
                accept_connection(false);
                return;
            }

            peer->id_from_handshake = handshake.peer_id();
            accept_connection(true);
        });
    };

    m_connection_manager->on_handshake_from_incoming_connection = [&](ConnectionId connection_id, HandshakeMessage handshake, Core::SocketAddress address, auto accept_connection) {
        m_event_loop->deferred_invoke([&, connection_id, handshake, address, accept_connection = move(accept_connection)] {
            VERIFY(!m_connecting_peers.contains(connection_id));
            VERIFY(!m_all_sessions.contains(connection_id));

            auto maybe_torrent = m_torrents.get(handshake.info_hash());
            if (maybe_torrent.has_value()) {
                auto torrent = maybe_torrent.release_value();

                if (torrent->state != TorrentState::STARTED && torrent->state != TorrentState::SEEDING) {
                    dbgln("Refusing connection from {} for because torrent {} is currently {}", address, torrent->info_hash, state_to_string(torrent->state));
                    accept_connection({});
                    return;
                }

                if (torrent->local_peer_id == handshake.peer_id()) {
                    dbgln("Refusing connection from ourselves.");
                    accept_connection({});
                    return;
                }

                auto slots = available_slots_for_torrent(*torrent);
                if (slots == 0) {
                    dbgln("Refusing connection from {} for torrent {} because we have no available slots.", address, torrent->info_hash);
                    accept_connection({});
                    return;
                }

                // FIXME: The peer likely already exists in torrent->peers
                auto peer = make_ref_counted<Peer>(address, *torrent);
                peer->status = PeerStatus::InUse;
                peer->id_from_handshake = handshake.peer_id();
                m_connecting_peers.set(connection_id, peer);

                accept_connection(HandshakeMessage(torrent->info_hash, torrent->local_peer_id));
            } else {
                dbgln("Peer sent a handshake with an unknown torrent info hash, disconnecting.");
                accept_connection({});
            }
        });
    };

    m_connection_manager->on_message_receive = [&](ConnectionId connection_id, ReadonlyBytes message_bytes) {
        m_event_loop->deferred_invoke([&, connection_id, buffer = MUST(ByteBuffer::copy(message_bytes))] {
            auto err = parse_input_message(connection_id, buffer.bytes());
            if (err.is_error()) {
                m_connection_manager->close_connection(connection_id, DeprecatedString::formatted("Error parsing input message for connection id {}: {}", connection_id, err.error().string_literal()));
            }
        });
    };

    m_connection_manager->on_connection_stats_update = [&](NonnullOwnPtr<HashMap<ConnectionId, ConnectionStats>> stats) {
        m_event_loop->deferred_invoke([&, stats = move(stats)]() mutable {
            m_connection_stats = move(stats);
        });
    };
}

u64 Engine::available_slots_for_torrent(NonnullRefPtr<BitTorrent::Torrent> torrent) const
{
    u64 total_connections_for_torrent = torrent->peer_sessions.size();
    for (auto const& [_, peer] : m_connecting_peers) {
        if (peer->torrent == torrent)
            total_connections_for_torrent++;
    }

    return min(m_config.max_connections_per_torrent - total_connections_for_torrent, m_config.max_total_connections - m_all_sessions.size());
}

void Engine::connect_more_peers(NonnullRefPtr<Torrent> torrent)
{
    auto available_slots = available_slots_for_torrent(torrent);
    dbgln("We have {} available slots for new connections", available_slots);

    auto peer_it = torrent->peers.begin();
    while (available_slots > 0 && peer_it != torrent->peers.end()) {
        auto& peer = peer_it->value;
        dbgln("Peer {} status: {}", peer->address, Peer::status_string(peer->status));
        if (peer->status == PeerStatus::Available) {
            auto connection_id = m_connection_manager->connect(peer->address, HandshakeMessage(torrent->info_hash, torrent->local_peer_id));
            peer->status = PeerStatus::InUse;
            dbgln("Connecting to peer {} connection id: {}", peer->address, connection_id);
            m_connecting_peers.set(connection_id, peer);
            available_slots--;
        }
        ++peer_it;
    }
}

ErrorOr<void> Engine::piece_downloaded(u64 index, ReadonlyBytes, NonnullRefPtr<PeerSession> peer)
{
    auto torrent = peer->peer->torrent;

    torrent->local_bitfield.set(index, true);
    dbgln("We completed piece {}", index);

    for (auto const& session : torrent->peer_sessions) {
        m_connection_manager->send_message(session->connection_id, make<HaveMessage>(index));
    }

    if (torrent->local_bitfield.progress() == 100) {
        dbgln("Torrent download completed: {}", torrent->info_hash);

        torrent->state = TorrentState::SEEDING;

        for (auto const& session : torrent->peer_sessions) {
            if (session->bitfield.progress() == 100)
                m_connection_manager->close_connection(session->connection_id, "Torrent fully downloaded.");
        }

        return {};
    }

    TRY(piece_or_peer_availability_updated(torrent));
    return {};
}

ErrorOr<void> Engine::piece_or_peer_availability_updated(NonnullRefPtr<Torrent> torrent)
{
    size_t available_slots = 0;
    for (auto const& session : torrent->peer_sessions)
        available_slots += !session->active;

    dbgln("We have {} inactive peers out of {} connected peers.", available_slots, torrent->peer_sessions.size());
    for (size_t i = 0; i < available_slots; i++) {
        dbgln("Trying to start a piece download on a {}th peer", i);

        // TODO find out the rarest available piece, because the rarest piece might not be available right now.
        u64 next_piece_index = 0;
        for (u64 j = 0; j < torrent->piece_count; j++) {
            if (!torrent->local_bitfield.get(j)) {
                next_piece_index = j;
                break;
            }
        }

        dbgln("Picked next piece for download {}", next_piece_index);
        for (auto p : torrent->peer_sessions) {
            if (!p->active && !p->peer_is_choking_us && p->bitfield.get(next_piece_index)) {
                dbgln("Requesting piece {} from peer {}", next_piece_index, p);
                p->active = true;
                u32 block_length = min(BLOCK_LENGTH, torrent->piece_length(next_piece_index));
                m_connection_manager->send_message(p->connection_id, make<RequestMessage>(next_piece_index, 0, block_length));
                return {};
            }
        }
    }
    return {};
}

ErrorOr<void> Engine::parse_input_message(ConnectionId connection_id, ReadonlyBytes message_bytes)
{
    NonnullRefPtr<PeerSession> session = *m_all_sessions.get(connection_id).value();
    if (session->peer->torrent->state != TorrentState::STARTED && session->peer->torrent->state != TorrentState::SEEDING) {
        dbgln("Discarding message from peer {} because torrent is not started anymore", session->peer->address);
        return {};
    }
    auto stream = FixedMemoryStream(message_bytes);

    using MessageType = BitTorrent::Message::Type;
    auto message_type = TRY(stream.read_value<MessageType>());

    dbgln("Got message type {}", BitTorrent::Message::to_string(message_type));

    switch (message_type) {
    case MessageType::Choke:
        session->peer_is_choking_us = true;
        TRY(piece_or_peer_availability_updated(session->peer->torrent));
        break;
    case MessageType::Unchoke:
        session->peer_is_choking_us = false;
        TRY(piece_or_peer_availability_updated(session->peer->torrent));
        break;
    case MessageType::Interested:
        TRY(handle_interested(session));
        break;
    case MessageType::NotInterested:
        session->peer_is_interested_in_us = false;
        break;
    case MessageType::Have:
        TRY(handle_have(make<HaveMessage>(stream), session));
        break;
    case MessageType::Bitfield:
        TRY(handle_bitfield(make<BitFieldMessage>(stream, session->peer->torrent->piece_count), session));
        break;
    case MessageType::Request:
        TRY(handle_request(make<RequestMessage>(stream), session));
        break;
    case MessageType::Piece:
        TRY(handle_piece(make<PieceMessage>(stream), session));
        break;
    case MessageType::Cancel:
        // TODO implement this.
        dbgln("ERROR: message type Cancel is unsupported");
        break;
    default:
        dbgln("ERROR: Got unsupported message type: {:02X}: {}", (u8)message_type, Message::to_string(message_type));
        break;
    }
    return {};
}

ErrorOr<void> Engine::handle_bitfield(NonnullOwnPtr<BitFieldMessage> bitfield, NonnullRefPtr<PeerSession> peer)
{
    peer->bitfield = bitfield->bitfield;
    dbgln("Receiving BitField from peer: {}", peer->bitfield);

    bool interesting = false;
    auto torrent = peer->peer->torrent;
    for (u64 piece_index = 0; piece_index < torrent->local_bitfield.size(); piece_index++) {
        if (peer->bitfield.get(piece_index)) {
            interesting = true;
            break;
        }
    }

    VERIFY(!peer->we_are_interested_in_peer);

    if (interesting) {
        // TODO we need a (un)choking algo
        m_connection_manager->send_message(peer->connection_id, make<UnchokeMessage>());
        peer->we_are_choking_peer = false;

        m_connection_manager->send_message(peer->connection_id, make<InterestedMessage>());
        peer->we_are_interested_in_peer = true;

        TRY(piece_or_peer_availability_updated(torrent));
    } else {
        u64 available_peer_count = 0;
        for (auto const& [_, p] : torrent->peers) {
            if (p->status == PeerStatus::Available)
                available_peer_count++;
        }
        if (available_peer_count > 0) {
            // TODO: set error type so we can connect to it again later if we need to
            // TODO: we have no idea if other peers will be reacheable or have better piece availability.
            m_connection_manager->close_connection(peer->connection_id, "Peer has no interesting pieces, and other peers are out there, disconnecting.");
        } else {
            dbgln("Peer has no interesting pieces, but we have no other peers to connect to. Staying connected in the hope that it will get some interesting pieces.");
        }
    }

    return {};
}

ErrorOr<void> Engine::handle_have(NonnullOwnPtr<HaveMessage> have_message, NonnullRefPtr<PeerSession> peer)
{
    auto piece_index = have_message->piece_index;
    dbgln("Peer has piece {}, setting in peer bitfield, bitfield size: {}", piece_index, peer->bitfield.size());
    peer->bitfield.set(piece_index, true);

    if (!peer->peer->torrent->local_bitfield.get(piece_index)) {
        if (!peer->we_are_interested_in_peer) {
            m_connection_manager->send_message(peer->connection_id, make<UnchokeMessage>());
            peer->we_are_choking_peer = false;

            m_connection_manager->send_message(peer->connection_id, make<InterestedMessage>());
            peer->we_are_interested_in_peer = true;
        }
        TRY(piece_or_peer_availability_updated(peer->peer->torrent));
    } else {
        if (peer->bitfield.progress() == 100 && peer->peer->torrent->local_bitfield.progress() == 100) {
            m_connection_manager->close_connection(peer->connection_id, "Peer and us have all pieces, disconnecting");
        }
    }

    return {};
}

ErrorOr<void> Engine::handle_interested(NonnullRefPtr<BitTorrent::PeerSession> peer)
{
    peer->peer_is_interested_in_us = true;
    peer->we_are_choking_peer = false;
    m_connection_manager->send_message(peer->connection_id, make<UnchokeMessage>());
    return {};
}

ErrorOr<void> Engine::handle_piece(NonnullOwnPtr<PieceMessage> piece_message, NonnullRefPtr<PeerSession> peer)
{
    auto torrent = peer->peer->torrent;
    auto block_size = piece_message->block.size();
    auto index = piece_message->piece_index;
    auto begin = piece_message->begin_offset;

    auto& piece = peer->incoming_piece;
    if (piece.index.has_value()) {
        VERIFY(index == piece.index);
        VERIFY(begin == piece.offset);
    } else {
        VERIFY(begin == 0);
        piece.index = index;
        piece.offset = 0;
        piece.length = torrent->piece_length(index);
        piece.data.resize(piece.length);
    }

    piece.data.overwrite(begin, piece_message->block.bytes().data(), block_size);
    piece.offset = begin + block_size;
    if (piece.offset == (size_t)piece.length) {
        piece.index = {};
        peer->active = false;
        TRY(piece_downloaded(index, piece.data.bytes(), peer));
    } else {
        if (peer->peer_is_choking_us) {
            dbgln("Weren't done downloading the blocks for this piece {}, but peer is choking us, so we're giving up on it", index);
            piece.index = {};
            peer->active = false;
            TRY(piece_or_peer_availability_updated(torrent));
        } else {
            auto next_block_length = min((size_t)BLOCK_LENGTH, (size_t)piece.length - piece.offset);
            m_connection_manager->send_message(peer->connection_id, make<RequestMessage>(index, piece.offset, next_block_length));
        }
    }
    return {};
}

ErrorOr<void> Engine::handle_request(NonnullOwnPtr<RequestMessage>, NonnullRefPtr<PeerSession>)
{
    return {};
}

}
