/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace BitTorrent {

struct Configuration {
    // About the optimal number of connections per torrent:
    // Taken from: https://wiki.theory.org/BitTorrentSpecification#Tracker_Response
    // Even 30 peers is plenty, the official client version 3 in fact only actively forms
    // new connections if it has less than 30 peers and will refuse connections if it has 55.
    // This value is important to performance. When a new piece has completed download,
    // HAVE messages (see below) will need to be sent to most active peers. As a result the cost
    // of broadcast traffic grows in direct proportion to the number of peers. Above 25, new peers are
    // highly unlikely to increase download speed. UI designers are strongly advised to make this
    // obscure and hard to change as it is very rare to be useful to do so.
    // -- End of quote --
    //
    // Until we are more clever in how we select which peer to stay connected with (speed, location, mutual interest, etc.)
    // we're bound to be a wasteful peer on the network. Because we're not doing a good job at selecting peers, we need more
    // connections than what would be optimal. These default values aren't wasteful, but they're less useful while testing
    // and developing, hence their easy configurability for now.
    static u64 constexpr DEFAULT_MAX_TOTAL_CONNECTIONS = 100;
    static u64 constexpr DEFAULT_MAX_CONNECTIONS_PER_TORRENT = 10;
    static u16 constexpr DEFAULT_LISTEN_PORT = 27007;

    u64 const max_total_connections;
    u64 const max_connections_per_torrent;
    u16 const listen_port;
};

}
