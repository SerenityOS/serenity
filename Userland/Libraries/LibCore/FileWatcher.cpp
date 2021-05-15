/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileWatcher.h"
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/API/InodeWatcherFlags.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Notifier.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

// Only supported in serenity mode because we use InodeWatcher syscalls
#ifdef __serenity__

static Optional<FileWatcherEvent> get_event_from_fd(int fd, HashMap<unsigned, String> const& wd_to_path)
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
    String const& path = it->value;

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
        String child_name { event->name, event->name_length - 1 };
        auto lexical_path = LexicalPath::join(path, child_name);
        if (!lexical_path.is_valid()) {
            dbgln_if(FILE_WATCHER_DEBUG, "get_event_from_fd: Reading from wd {}: Invalid child name '{}'", fd, child_name);
            return {};
        }

        result.event_path = lexical_path.string();
    } else {
        result.event_path = path;
    }

    dbgln_if(FILE_WATCHER_DEBUG, "get_event_from_fd: got event from wd {} on '{}' type {}", fd, result.event_path, result.type);
    return result;
}

Result<bool, String> FileWatcherBase::add_watch(String path, FileWatcherEvent::Type event_mask)
{
    LexicalPath lexical_path;
    if (path.length() > 0 && path[0] == '/') {
        lexical_path = LexicalPath { path };
    } else {
        char* buf = getcwd(nullptr, 0);
        lexical_path = LexicalPath::join(String(buf), path);
        free(buf);
    }

    if (!lexical_path.is_valid()) {
        dbgln_if(FILE_WATCHER_DEBUG, "add_watch: path '{}' invalid", path);
        return false;
    }

    auto const& canonical_path = lexical_path.string();
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
        return String::formatted("Could not watch file '{}' : {}", canonical_path, strerror(errno));

    m_path_to_wd.set(canonical_path, wd);
    m_wd_to_path.set(wd, canonical_path);

    dbgln_if(FILE_WATCHER_DEBUG, "add_watch: watching path '{}' on InodeWatcher {} wd {}", canonical_path, m_watcher_fd, wd);
    return true;
}

Result<bool, String> FileWatcherBase::remove_watch(String path)
{
    LexicalPath lexical_path;
    if (path.length() > 0 && path[0] == '/') {
        lexical_path = LexicalPath { path };
    } else {
        char* buf = getcwd(nullptr, 0);
        lexical_path = LexicalPath::join(String(buf), path);
        free(buf);
    }

    if (!lexical_path.is_valid()) {
        dbgln_if(FILE_WATCHER_DEBUG, "remove_watch: path '{}' invalid", path);
        return false;
    }

    auto const& canonical_path = lexical_path.string();
    auto it = m_path_to_wd.find(canonical_path);
    if (it == m_path_to_wd.end()) {
        dbgln_if(FILE_WATCHER_DEBUG, "remove_watch: path '{}' is not being watched", canonical_path);
        return false;
    }

    int rc = inode_watcher_remove_watch(m_watcher_fd, it->value);
    if (rc < 0) {
        return String::formatted("Could not stop watching file '{}' : {}", path, strerror(errno));
    }

    m_path_to_wd.remove(it);
    m_wd_to_path.remove(it->value);

    dbgln_if(FILE_WATCHER_DEBUG, "remove_watch: stopped watching path '{}' on InodeWatcher {}", canonical_path, m_watcher_fd);
    return true;
}

BlockingFileWatcher::BlockingFileWatcher(InodeWatcherFlags flags)
    : FileWatcherBase(create_inode_watcher(static_cast<unsigned>(flags)))
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

Result<NonnullRefPtr<FileWatcher>, String> FileWatcher::create(InodeWatcherFlags flags)
{
    auto watcher_fd = create_inode_watcher(static_cast<unsigned>(flags | InodeWatcherFlags::CloseOnExec));
    if (watcher_fd < 0) {
        return String::formatted("FileWatcher: Could not create InodeWatcher: {}", strerror(errno));
    }

    auto notifier = Notifier::construct(watcher_fd, Notifier::Event::Read);
    return adopt_ref(*new FileWatcher(watcher_fd, move(notifier)));
}

FileWatcher::FileWatcher(int watcher_fd, NonnullRefPtr<Notifier> notifier)
    : FileWatcherBase(watcher_fd)
    , m_notifier(move(notifier))
{
    m_notifier->on_ready_to_read = [this] {
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
    m_notifier->on_ready_to_read = nullptr;
    close(m_notifier->fd());
    dbgln_if(FILE_WATCHER_DEBUG, "Stopped watcher at fd {}", m_notifier->fd());
}

#endif

}
