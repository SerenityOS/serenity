/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileWatcher.h"
#include <AK/ByteString.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/API/InodeWatcherFlags.h>
#include <LibCore/Notifier.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

#if !defined(AK_OS_SERENITY)
static_assert(false, "This file must only be used for SerenityOS");
#endif

namespace Core {

static constexpr unsigned file_watcher_flags_to_inode_watcher_flags(FileWatcherFlags flags)
{
    auto result = InodeWatcherFlags::None;

    if (has_flag(flags, FileWatcherFlags::Nonblock))
        result |= InodeWatcherFlags::Nonblock;
    if (has_flag(flags, FileWatcherFlags::CloseOnExec))
        result |= InodeWatcherFlags::CloseOnExec;

    return static_cast<unsigned>(result);
}

static Optional<FileWatcherEvent> get_event_from_fd(int fd, HashMap<unsigned, ByteString> const& wd_to_path)
{
    u8 buffer[MAXIMUM_EVENT_SIZE];
    int rc = read(fd, &buffer, MAXIMUM_EVENT_SIZE);
    if (rc == 0) {
        return {};
    } else if (rc < 0) {
        dbgln_if(FILE_WATCHER_DEBUG, "get_event_from_fd: Reading from wd {} failed: {}", fd, strerror(errno));
        return {};
    }

    InodeWatcherEvent* event = reinterpret_cast<InodeWatcherEvent*>(buffer);
    FileWatcherEvent result;

    auto it = wd_to_path.find(event->watch_descriptor);
    if (it == wd_to_path.end()) {
        dbgln_if(FILE_WATCHER_DEBUG, "get_event_from_fd: Got an event for a non-existent wd {}?!", event->watch_descriptor);
        return {};
    }
    ByteString const& path = it->value;

    switch (event->type) {
    case InodeWatcherEvent::Type::ChildCreated:
        result.type = FileWatcherEvent::Type::ChildCreated;
        break;
    case InodeWatcherEvent::Type::ChildDeleted:
        result.type = FileWatcherEvent::Type::ChildDeleted;
        break;
    case InodeWatcherEvent::Type::Deleted:
        result.type = FileWatcherEvent::Type::Deleted;
        break;
    case InodeWatcherEvent::Type::ContentModified:
        result.type = FileWatcherEvent::Type::ContentModified;
        break;
    case InodeWatcherEvent::Type::MetadataModified:
        result.type = FileWatcherEvent::Type::MetadataModified;
        break;
    default:
        warnln("Unknown event type {} returned by the watch_file descriptor for {}", static_cast<unsigned>(event->type), path);
        return {};
    }

    // We trust that the kernel only sends the name when appropriate.
    if (event->name_length > 0) {
        ByteString child_name { event->name, event->name_length - 1 };
        result.event_path = LexicalPath::join(path, child_name).string();
    } else {
        result.event_path = path;
    }

    dbgln_if(FILE_WATCHER_DEBUG, "get_event_from_fd: got event from wd {} on '{}' type {}", fd, result.event_path, result.type);
    return result;
}

static ByteString canonicalize_path(ByteString path)
{
    if (!path.is_empty() && path[0] == '/')
        return LexicalPath::canonicalized_path(move(path));
    char* cwd = getcwd(nullptr, 0);
    VERIFY(cwd);
    return LexicalPath::join({ cwd, strlen(cwd) }, move(path)).string();
}

ErrorOr<bool> FileWatcherBase::add_watch(ByteString path, FileWatcherEvent::Type event_mask)
{
    ByteString canonical_path = canonicalize_path(move(path));

    if (m_path_to_wd.find(canonical_path) != m_path_to_wd.end()) {
        dbgln_if(FILE_WATCHER_DEBUG, "add_watch: path '{}' is already being watched", canonical_path);
        return false;
    }

    auto kernel_mask = InodeWatcherEvent::Type::Invalid;
    if (has_flag(event_mask, FileWatcherEvent::Type::ChildCreated))
        kernel_mask |= InodeWatcherEvent::Type::ChildCreated;
    if (has_flag(event_mask, FileWatcherEvent::Type::ChildDeleted))
        kernel_mask |= InodeWatcherEvent::Type::ChildDeleted;
    if (has_flag(event_mask, FileWatcherEvent::Type::Deleted))
        kernel_mask |= InodeWatcherEvent::Type::Deleted;
    if (has_flag(event_mask, FileWatcherEvent::Type::ContentModified))
        kernel_mask |= InodeWatcherEvent::Type::ContentModified;
    if (has_flag(event_mask, FileWatcherEvent::Type::MetadataModified))
        kernel_mask |= InodeWatcherEvent::Type::MetadataModified;

    int wd = inode_watcher_add_watch(m_watcher_fd, canonical_path.characters(), canonical_path.length(), static_cast<unsigned>(kernel_mask));
    if (wd < 0)
        return Error::from_errno(errno);

    m_path_to_wd.set(canonical_path, wd);
    m_wd_to_path.set(wd, canonical_path);

    dbgln_if(FILE_WATCHER_DEBUG, "add_watch: watching path '{}' on InodeWatcher {} wd {}", canonical_path, m_watcher_fd, wd);
    return true;
}

ErrorOr<bool> FileWatcherBase::remove_watch(ByteString path)
{
    ByteString canonical_path = canonicalize_path(move(path));

    auto it = m_path_to_wd.find(canonical_path);
    if (it == m_path_to_wd.end()) {
        dbgln_if(FILE_WATCHER_DEBUG, "remove_watch: path '{}' is not being watched", canonical_path);
        return false;
    }

    if (inode_watcher_remove_watch(m_watcher_fd, it->value) < 0)
        return Error::from_errno(errno);

    m_path_to_wd.remove(it);
    m_wd_to_path.remove(it->value);

    dbgln_if(FILE_WATCHER_DEBUG, "remove_watch: stopped watching path '{}' on InodeWatcher {}", canonical_path, m_watcher_fd);
    return true;
}

BlockingFileWatcher::BlockingFileWatcher(FileWatcherFlags flags)
    : FileWatcherBase(create_inode_watcher(file_watcher_flags_to_inode_watcher_flags(flags)))
{
    VERIFY(m_watcher_fd != -1);
    dbgln_if(FILE_WATCHER_DEBUG, "BlockingFileWatcher created with InodeWatcher {}", m_watcher_fd);
}

BlockingFileWatcher::~BlockingFileWatcher()
{
    close(m_watcher_fd);
}

Optional<FileWatcherEvent> BlockingFileWatcher::wait_for_event()
{
    dbgln_if(FILE_WATCHER_DEBUG, "BlockingFileWatcher::wait_for_event()");

    auto maybe_event = get_event_from_fd(m_watcher_fd, m_wd_to_path);
    if (!maybe_event.has_value())
        return maybe_event;

    auto event = maybe_event.release_value();
    if (event.type == FileWatcherEvent::Type::Deleted) {
        auto result = remove_watch(event.event_path);
        if (result.is_error()) {
            dbgln_if(FILE_WATCHER_DEBUG, "wait_for_event: {}", result.error());
        }
    }

    return event;
}

ErrorOr<NonnullRefPtr<FileWatcher>> FileWatcher::create(FileWatcherFlags flags)
{
    auto watcher_fd = create_inode_watcher(file_watcher_flags_to_inode_watcher_flags(flags | FileWatcherFlags::CloseOnExec));
    if (watcher_fd < 0)
        return Error::from_errno(errno);

    auto notifier = Notifier::construct(watcher_fd, Notifier::Type::Read);
    return adopt_ref(*new FileWatcher(watcher_fd, move(notifier)));
}

FileWatcher::FileWatcher(int watcher_fd, NonnullRefPtr<Notifier> notifier)
    : FileWatcherBase(watcher_fd)
    , m_notifier(move(notifier))
{
    m_notifier->on_activation = [this] {
        auto maybe_event = get_event_from_fd(m_notifier->fd(), m_wd_to_path);
        if (maybe_event.has_value()) {
            auto event = maybe_event.value();
            on_change(event);

            if (event.type == FileWatcherEvent::Type::Deleted) {
                auto result = remove_watch(event.event_path);
                if (result.is_error()) {
                    dbgln_if(FILE_WATCHER_DEBUG, "on_ready_to_read: {}", result.error());
                }
            }
        }
    };
}

FileWatcher::~FileWatcher()
{
    m_notifier->on_activation = nullptr;
    close(m_notifier->fd());
    dbgln_if(FILE_WATCHER_DEBUG, "Stopped watcher at fd {}", m_notifier->fd());
}

}
