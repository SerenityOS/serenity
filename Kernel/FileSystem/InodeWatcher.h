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

namespace Kernel {

class Inode;

// A specific description of a watch.
struct WatchDescription {
    int wd;
    Inode& inode;
    unsigned event_mask;

    static KResultOr<NonnullOwnPtr<WatchDescription>> create(int wd, Inode& inode, unsigned event_mask)
    {
        auto description = adopt_own_if_nonnull(new WatchDescription(wd, inode, event_mask));
        if (description)
            return description.release_nonnull();
        return ENOMEM;
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
    static KResultOr<NonnullRefPtr<InodeWatcher>> create();
    virtual ~InodeWatcher() override;

    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    // Can't write to an inode watcher.
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EIO; }
    virtual KResult close() override;

    virtual String absolute_path(const FileDescription&) const override;
    virtual const char* class_name() const override { return "InodeWatcher"; };
    virtual bool is_inode_watcher() const override { return true; }

    void notify_inode_event(Badge<Inode>, InodeIdentifier, InodeWatcherEvent::Type, String const& name = {});

    KResultOr<int> register_inode(Inode&, unsigned event_mask);
    KResult unregister_by_wd(int);
    void unregister_by_inode(Badge<Inode>, InodeIdentifier);

private:
    explicit InodeWatcher() { }

    mutable Lock m_lock;

    struct Event {
        int wd { 0 };
        InodeWatcherEvent::Type type { InodeWatcherEvent::Type::Invalid };
        String path;
    };
    CircularQueue<Event, 32> m_queue;
    Checked<int> m_wd_counter { 1 };

    // NOTE: These two hashmaps provide two different ways of reaching the same
    // watch description, so they will overlap.
    HashMap<int, NonnullOwnPtr<WatchDescription>> m_wd_to_watches;
    HashMap<InodeIdentifier, WatchDescription*> m_inode_to_watches;
};

}
