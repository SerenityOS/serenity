/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Checked.h>
#include <AK/CircularQueue.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

// A specific description of a watch.
struct WatchDescription {
    int wd;
    Inode& inode;
    unsigned event_mask;

    static ErrorOr<NonnullOwnPtr<WatchDescription>> create(int wd, Inode& inode, unsigned event_mask)
    {
        return adopt_nonnull_own_or_enomem(new (nothrow) WatchDescription(wd, inode, event_mask));
    }

private:
    WatchDescription(int wd, Inode& inode, unsigned event_mask)
        : wd(wd)
        , inode(inode)
        , event_mask(event_mask)
    {
    }
};

class InodeWatcher final : public File {
public:
    static ErrorOr<NonnullRefPtr<InodeWatcher>> try_create();
    virtual ~InodeWatcher() override;

    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    // Can't write to an inode watcher.
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return EIO; }
    virtual ErrorOr<void> close() override;

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual StringView class_name() const override { return "InodeWatcher"sv; }
    virtual bool is_inode_watcher() const override { return true; }

    void notify_inode_event(Badge<Inode>, InodeIdentifier, InodeWatcherEvent::Type, StringView name = {});

    ErrorOr<int> register_inode(Inode&, unsigned event_mask);
    ErrorOr<void> unregister_by_wd(int);
    void unregister_by_inode(Badge<Inode>, InodeIdentifier);

private:
    explicit InodeWatcher() { }

    struct Event {
        int wd { 0 };
        InodeWatcherEvent::Type type { InodeWatcherEvent::Type::Invalid };
        OwnPtr<KString> path;
    };
    SpinlockProtected<CircularQueue<Event, 32>, LockRank::None> m_queue;
    Checked<int> m_wd_counter { 1 };

    // NOTE: These two hashmaps provide two different ways of reaching the same
    // watch description, so they will overlap.
    struct WatchMaps {
        HashMap<int, NonnullOwnPtr<WatchDescription>> wd_to_watches;
        HashMap<InodeIdentifier, WatchDescription*> inode_to_watches;
    };

    mutable SpinlockProtected<WatchMaps, LockRank::None> m_watch_maps;
};

}
