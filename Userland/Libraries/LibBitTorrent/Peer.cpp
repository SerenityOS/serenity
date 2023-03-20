/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Peer.h"
#include "PeerSession.h"
#include "Torrent.h"

namespace BitTorrent {

Peer::Peer(Core::SocketAddress address, NonnullRefPtr<Torrent> torrent)
    : address(move(address))
    , torrent(move(torrent))
{
}

DeprecatedString Peer::status_string(PeerStatus status)
{
    switch (status) {
    case PeerStatus::InUse:
        return "In use";
    case PeerStatus::Available:
        return "Available";
    case PeerStatus::Errored:
        return "Errored";
    default:
        VERIFY_NOT_REACHED();
    }
}

}
