/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Torrent.h"
#include "Peer.h"
#include "PeerSession.h"

namespace BitTorrent {
DeprecatedString state_to_string(TorrentState state)
{
    switch (state) {
    case TorrentState::ERROR:
        return "Error";
    case TorrentState::STOPPED:
        return "Stopped";
    case TorrentState::STARTED:
        return "Started";
    case TorrentState::SEEDING:
        return "Seeding";
    default:
        VERIFY_NOT_REACHED();
    }
}

Torrent::Torrent(DeprecatedString display_name,DeprecatedString data_path, InfoHash info_hash, PeerId local_peer_id, u64 total_length, u64 nominal_piece_length)
    : display_name(display_name)
    , data_path(move(data_path))
    , info_hash(info_hash)
    , local_peer_id(local_peer_id)
    , tracker_session_key(get_random<u64>())
    , piece_count(ceil_div(total_length, nominal_piece_length))
    , nominal_piece_length(nominal_piece_length)
    , total_length(total_length)
    , local_bitfield(BitField(piece_count))
{
}

u64 Torrent::piece_length(u64 piece_index) const
{
    if (piece_index == piece_count - 1 && total_length % nominal_piece_length > 0)
        return total_length % nominal_piece_length;
    else
        return nominal_piece_length;
}

}
