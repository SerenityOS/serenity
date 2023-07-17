/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitField.h"
#include "FixedSizeByteString.h"
#include "TorrentDataFileMap.h"
#include <LibCore/EventLoop.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>

namespace BitTorrent {

using CheckerStats = HashMap<InfoHash, float>;
using CheckerStatsCallback = Function<void(CheckerStats)>;

struct Torrent;

class Checker {

public:
    Checker();
    void check(InfoHash info_hash, NonnullOwnPtr<TorrentDataFileMap> data_file_map, u64 piece_count, Function<void(ErrorOr<BitField>)> on_complete);
    void cancel(InfoHash info_hash);
    void shutdown();
    CheckerStatsCallback on_stats_update;

private:
    RefPtr<Threading::Thread> m_thread;
    void main_loop();
    Threading::Mutex m_queue_access_lock {};

    struct Entry {
        InfoHash info_hash;
        NonnullOwnPtr<TorrentDataFileMap> data_file_map;
        u64 piece_count;
        Function<void(ErrorOr<BitField>)> on_complete;
        bool cancelled { false };
    };

    OrderedHashMap<InfoHash, Entry> m_queue;
    bool m_shutting_down { false };
};

}
