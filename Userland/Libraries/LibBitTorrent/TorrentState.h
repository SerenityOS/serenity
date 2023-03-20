/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace BitTorrent {

enum class TorrentState {
    ERROR,
    STOPPED,
    STARTED,
    SEEDING
};

}
