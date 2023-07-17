/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitField.h"
#include "FixedSizeByteString.h"
#include "Net/Connection.h"
#include <AK/RefCounted.h>
#include <LibCore/DateTime.h>
#include <LibCore/Socket.h>

namespace BitTorrent {

struct Torrent;
struct Peer;

struct PeerSession : public RefCounted<PeerSession> {
    PeerSession(NonnullRefPtr<Peer> peer, ConnectionId connection_id, PeerId id);

    NonnullRefPtr<Peer> const peer;
    ConnectionId const connection_id;
    PeerId const id;

    bool active = false;

    // long variable names because it gets confusing easily.
    bool peer_is_choking_us { true };
    bool peer_is_interested_in_us { false };
    bool we_are_choking_peer { true };
    bool we_are_interested_in_peer { false };

    BitField bitfield;
    HashTable<u64> interesting_pieces;

    // TODO: move this to PieceStatus?
    struct {
        ByteBuffer data;
        Optional<size_t> index;
        size_t offset;
        size_t length;
    } incoming_piece;
};

}

template<>
struct AK::Formatter<BitTorrent::PeerSession> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, BitTorrent::PeerSession const& session)
    {
        return Formatter<FormatString>::format(builder,
            "{}/{}/{}{}{}{}{}/{}%"sv,
            session.connection_id,
            session.peer,
            session.peer_is_choking_us ? "c" : "",
            session.peer_is_interested_in_us ? "i" : "",
            session.we_are_choking_peer ? "C" : "",
            session.we_are_interested_in_peer ? "I" : "",
            session.active ? "A" : "",
            static_cast<int>(session.bitfield.progress()));
    }
};
