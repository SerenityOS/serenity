/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Checker.h"

namespace BitTorrent {

Checker::Checker()
{
    m_thread = Threading::Thread::construct([this]() -> intptr_t {
        main_loop();
        return 0;
    },
        "Checker"sv);

    m_thread->start();
}

void Checker::check(InfoHash info_hash, NonnullOwnPtr<TorrentDataFileMap> data_file_map, u64 piece_count, Function<void(ErrorOr<BitField>)> on_complete)
{
    if (!m_shutting_down) {
        Threading::MutexLocker queue_locker(m_queue_access_lock);
        m_queue.set(info_hash, { info_hash, move(data_file_map), piece_count, move(on_complete) });
    }
}

void Checker::cancel(InfoHash info_hash)
{
    if (!m_shutting_down) {
        Threading::MutexLocker queue_locker(m_queue_access_lock);

        auto maybe_entry = m_queue.get(info_hash);
        if (maybe_entry.has_value()) {
            maybe_entry->cancelled = true;
        }
    }
}

void Checker::shutdown()
{
    m_shutting_down = true;
    auto err = m_thread->join();
    if (err.is_error())
        dbgln("Error joining the checker thread at shut down: {}", err.release_error());
}

void Checker::main_loop()
{
    int const batch_size = 10;

    for (;;) {
        m_queue_access_lock.lock();
        if (m_shutting_down) {
            m_queue.clear();
            m_queue_access_lock.unlock();
            return;
        }
        if (m_queue.is_empty()) {
            m_queue_access_lock.unlock();
            sleep(1);
            continue;
        } else {
            auto& entry = m_queue.begin()->value;
            m_queue_access_lock.unlock();

            auto retvalue = ErrorOr<BitField>(BitField(entry.piece_count));
            auto& bitfield = retvalue.value();
            for (u64 i = 0; i < entry.piece_count; i++) {
                auto err_or_result = entry.data_file_map->check_piece(i, i == entry.piece_count - 1);

                if (err_or_result.is_error()) {
                    dbgln("Checker: error while checking piece {}: {}", i, err_or_result.error());
                    retvalue = Error::copy(err_or_result.error());
                    break;
                }
                bitfield.set(i, err_or_result.release_value());

                if (i % batch_size == 0) {
                    if (entry.cancelled || m_shutting_down) {
                        dbgln("Checker: cancelled");
                        retvalue = Error::from_errno(ECANCELED);
                        break;
                    }

                    if (on_stats_update) {
                        CheckerStats stats;
                        stats.set(entry.info_hash, (float)i * 100 / (float)entry.piece_count);
                        on_stats_update(stats);
                    }
                }
            }

            Threading::MutexLocker queue_locker(m_queue_access_lock);
            entry.on_complete(move(retvalue));
            m_queue.remove(entry.info_hash);
        }
    }
}

}
