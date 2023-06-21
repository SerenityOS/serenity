/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<InodeWatcher>> InodeWatcher::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) InodeWatcher);
}

InodeWatcher::~InodeWatcher()
{
    (void)close();
}

bool InodeWatcher::can_read(OpenFileDescription const&, u64) const
{
    return m_queue.with([](auto& queue) { return !queue.is_empty(); });
}

ErrorOr<size_t> InodeWatcher::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t buffer_size)
{
    auto event = TRY(m_queue.with([](auto& queue) -> ErrorOr<Event> {
        if (queue.is_empty()) {
            // can_read will catch the blocking case.
            return EAGAIN;
        }

        return queue.dequeue();
    }));

    size_t bytes_to_write = sizeof(InodeWatcherEvent);
    if (event.path)
        bytes_to_write += event.path->length() + 1;

    if (buffer_size < bytes_to_write)
        return EINVAL;

    auto result = buffer.write_buffered<MAXIMUM_EVENT_SIZE>(bytes_to_write, [&](Bytes bytes) {
        size_t offset = 0;

        memcpy(bytes.offset(offset), &event.wd, sizeof(InodeWatcherEvent::watch_descriptor));
        offset += sizeof(InodeWatcherEvent::watch_descriptor);
        memcpy(bytes.offset(offset), &event.type, sizeof(InodeWatcherEvent::type));
        offset += sizeof(InodeWatcherEvent::type);

        if (event.path) {
            size_t name_length = event.path->length() + 1;
            memcpy(bytes.offset(offset), &name_length, sizeof(InodeWatcherEvent::name_length));
            offset += sizeof(InodeWatcherEvent::name_length);
            memcpy(bytes.offset(offset), event.path->characters(), name_length);
        } else {
            memset(bytes.offset(offset), 0, sizeof(InodeWatcherEvent::name_length));
        }

        return bytes.size();
    });
    evaluate_block_conditions();
    return result;
}

ErrorOr<void> InodeWatcher::close()
{
    m_watch_maps.with([this](auto& watch_maps) {
        for (auto& entry : watch_maps.wd_to_watches) {
            auto& inode = const_cast<Inode&>(entry.value->inode);
            inode.unregister_watcher({}, *this);
        }

        watch_maps.inode_to_watches.clear();
        watch_maps.wd_to_watches.clear();
    });
    return {};
}

ErrorOr<NonnullOwnPtr<KString>> InodeWatcher::pseudo_path(OpenFileDescription const&) const
{
    return m_watch_maps.with([](auto& watch_maps) -> ErrorOr<NonnullOwnPtr<KString>> {
        return KString::formatted("InodeWatcher:({})", watch_maps.wd_to_watches.size());
    });
}

void InodeWatcher::notify_inode_event(Badge<Inode>, InodeIdentifier inode_id, InodeWatcherEvent::Type event_type, StringView name)
{
    m_watch_maps.with([this, inode_id, event_type, name](auto& watch_maps) {
        auto it = watch_maps.inode_to_watches.find(inode_id);
        if (it == watch_maps.inode_to_watches.end())
            return;

        auto& watcher = *it->value;
        if (!(watcher.event_mask & static_cast<unsigned>(event_type)))
            return;

        m_queue.with([watcher, event_type, name](auto& queue) {
            OwnPtr<KString> path;
            if (!name.is_null())
                path = KString::try_create(name).release_value_but_fixme_should_propagate_errors();
            queue.enqueue({ watcher.wd, event_type, move(path) });
        });
    });

    evaluate_block_conditions();
}

ErrorOr<int> InodeWatcher::register_inode(Inode& inode, unsigned event_mask)
{
    return m_watch_maps.with([this, &inode, event_mask](auto& watch_maps) -> ErrorOr<int> {
        if (watch_maps.inode_to_watches.find(inode.identifier()) != watch_maps.inode_to_watches.end())
            return EEXIST;

        int wd = -1;
        do {
            wd = m_wd_counter.value();

            m_wd_counter++;
            if (m_wd_counter.has_overflow())
                m_wd_counter = 1;
        } while (watch_maps.wd_to_watches.find(wd) != watch_maps.wd_to_watches.end());

        auto description = TRY(WatchDescription::create(wd, inode, event_mask));

        TRY(watch_maps.inode_to_watches.try_set(inode.identifier(), description.ptr()));
        auto set_result = watch_maps.wd_to_watches.try_set(wd, move(description));
        if (set_result.is_error()) {
            watch_maps.inode_to_watches.remove(inode.identifier());
            return set_result.release_error();
        }

        auto register_result = inode.register_watcher({}, *this);
        if (register_result.is_error()) {
            watch_maps.inode_to_watches.remove(inode.identifier());
            watch_maps.wd_to_watches.remove(wd);
            return register_result.release_error();
        }

        return wd;
    });
}

ErrorOr<void> InodeWatcher::unregister_by_wd(int wd)
{
    TRY(m_watch_maps.with([this, wd](auto& watch_maps) -> ErrorOr<void> {
        auto it = watch_maps.wd_to_watches.find(wd);
        if (it == watch_maps.wd_to_watches.end())
            return ENOENT;

        auto& inode = it->value->inode;
        inode.unregister_watcher({}, *this);

        watch_maps.inode_to_watches.remove(inode.identifier());
        watch_maps.wd_to_watches.remove(it);
        return {};
    }));
    return {};
}

void InodeWatcher::unregister_by_inode(Badge<Inode>, InodeIdentifier identifier)
{
    m_watch_maps.with([identifier](auto& watch_maps) {
        auto it = watch_maps.inode_to_watches.find(identifier);
        if (it == watch_maps.inode_to_watches.end())
            return;

        // NOTE: no need to call unregister_watcher here, the Inode calls us.
        watch_maps.inode_to_watches.remove(identifier);
        watch_maps.wd_to_watches.remove(it->value->wd);
    });
}

}
