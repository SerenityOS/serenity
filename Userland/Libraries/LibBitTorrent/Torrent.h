/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitField.h"
#include "Files.h"
#include "FixedSizeByteString.h"
#include "PieceHeap.h"
#include "TorrentState.h"
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <LibCore/SocketAddress.h>

namespace BitTorrent {

struct PeerSession;
struct Peer;

DeprecatedString state_to_string(TorrentState state);

struct Torrent : public RefCounted<Torrent> {
    Torrent(DeprecatedString display_name, Vector<LocalFile> local_files, DeprecatedString data_path, InfoHash info_hash, PeerId local_peer_id, u64 total_length, u64 nominal_piece_length);

    DeprecatedString const display_name;
    Vector<LocalFile> const local_files;
    DeprecatedString const data_path;
    InfoHash const info_hash;
    PeerId const local_peer_id;
    u64 const tracker_session_key; // https://www.bittorrent.org/beps/bep_0007.html
    u64 const piece_count;
    u64 const nominal_piece_length; // Is "nominal" the right term?
    u64 const total_length;
    BitField local_bitfield;

    Vector<Vector<URL>> announce_urls;
    TorrentState state = TorrentState::STOPPED;

    // Active torrent members
    PieceHeap piece_heap;
    HashMap<u64, RefPtr<PieceStatus>> missing_pieces;
    HashMap<Core::SocketAddress, NonnullRefPtr<Peer>> peers;
    HashTable<NonnullRefPtr<PeerSession>> peer_sessions;
    u64 download_speed { 0 };
    u64 upload_speed { 0 };

    u64 piece_length(u64 piece_index) const;
};

}
