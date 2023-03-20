/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../FixedSizeByteString.h"
#include <AK/NonnullOwnPtr.h>
#include <AK/Stream.h>

namespace BitTorrent {

struct HandshakeMessage {
    u8 pstrlen;
    u8 pstr[19];
    u8 reserved[8];
    u8 info_hash_data[20];
    u8 peer_id_data[20];

    HandshakeMessage(InfoHash info_hash, PeerId peer_id)
    {
        pstrlen = 19;
        memcpy(pstr, "BitTorrent protocol", 19);
        memset(reserved, 0, 8);
        memcpy(this->info_hash_data, info_hash.bytes().data(), 20);
        memcpy(this->peer_id_data, peer_id.bytes().data(), 20);
    }

    HandshakeMessage(ReadonlyBytes bytes)
    {
        memcpy(this, bytes.data(), sizeof(HandshakeMessage));
    }

    InfoHash info_hash() const
    {
        return InfoHash(ReadonlyBytes { info_hash_data, 20 });
    }

    PeerId peer_id() const
    {
        return PeerId(ReadonlyBytes { peer_id_data, 20 });
    }

    DeprecatedString
    to_string() const
    {
        return DeprecatedString::formatted("{:.19}, Reserved: {:08b} {:08b} {:08b} {:08b} {:08b} {:08b} {:08b} {:08b}, info_hash: {}, peer_id: {}",
            pstr,
            reserved[0],
            reserved[1],
            reserved[2],
            reserved[3],
            reserved[4],
            reserved[5],
            reserved[6],
            reserved[7],
            info_hash(),
            peer_id());
    }
};
}
