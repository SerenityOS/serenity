/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <LibCore/DirIterator.h>
#include <LibCore/Notifier.h>
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>

namespace Core {

// Only supported in serenity mode because we use `watch_file`
#ifdef __serenity__

static String get_child_path_from_inode_index(const String& path, unsigned child_inode_index)
{
    DirIterator iterator(path, Core::DirIterator::SkipDots);
    if (iterator.has_error()) {
        return {};
    }

    while (iterator.has_next()) {
        auto child_full_path = String::formatted("{}/{}", path, iterator.next_path());
        struct stat st;

        if (lstat(child_full_path.characters(), &st)) {
            return {};
        }

        if (st.st_ino == child_inode_index) {
            return child_full_path;
        }
    }
    return {};
}

BlockingFileWatcher::BlockingFileWatcher(const String& path)
    : m_path(path)
{
    m_watcher_fd = watch_file(path.characters(), path.length());
    VERIFY(m_watcher_fd != -1);
}

BlockingFileWatcher::~BlockingFileWatcher()
{
    close(m_watcher_fd);
}

Optional<FileWatcherEvent> BlockingFileWatcher::wait_for_event()
{
    InodeWatcherEvent event {};
    int rc = read(m_watcher_fd, &event, sizeof(event));
    if (rc <= 0)
        return {};

    FileWatcherEvent result;
    if (event.type == InodeWatcherEvent::Type::ChildAdded)
        result.type = FileWatcherEvent::Type::ChildAdded;
    else if (event.type == InodeWatcherEvent::Type::ChildRemoved)
        result.type = FileWatcherEvent::Type::ChildRemoved;
    else if (event.type == InodeWatcherEvent::Type::Modified)
        result.type = FileWatcherEvent::Type::Modified;
    else
        return {};

    if (result.type == FileWatcherEvent::Type::ChildAdded || result.type == FileWatcherEvent::Type::ChildRemoved) {
        auto child_path = get_child_path_from_inode_index(m_path, event.inode_index);
        if (!LexicalPath(child_path).is_valid())
            return {};

        result.child_path = child_path;
    }

    return result;
}

Result<NonnullRefPtr<FileWatcher>, String> FileWatcher::watch(const String& path)
{
    auto watch_fd = watch_file(path.characters(), path.length());
    if (watch_fd < 0) {
        return String::formatted("Could not watch file '{}' : {}", path.characters(), strerror(errno));
    }

    fcntl(watch_fd, F_SETFD, FD_CLOEXEC);
    if (watch_fd < 0) {
        return String::formatted("Could not watch file '{}' : {}", path.characters(), strerror(errno));
    }

    dbgln_if(FILE_WATCHER_DEBUG, "Started watcher for file '{}'", path.characters());
    auto notifier = Notifier::construct(watch_fd, Notifier::Event::Read);
    return adopt(*new FileWatcher(move(notifier), move(path)));
}

FileWatcher::FileWatcher(NonnullRefPtr<Notifier> notifier, const String& path)
    : m_notifier(move(notifier))
    , m_path(path)
{
    m_notifier->on_ready_to_read = [this] {
        InodeWatcherEvent event {};
        int rc = read(m_notifier->fd(), &event, sizeof(event));
        if (rc <= 0)
            return;

        FileWatcherEvent result;
        if (event.type == InodeWatcherEvent::Type::ChildAdded) {
            result.type = FileWatcherEvent::Type::ChildAdded;
        } else if (event.type == InodeWatcherEvent::Type::ChildRemoved) {
            result.type = FileWatcherEvent::Type::ChildRemoved;
        } else if (event.type == InodeWatcherEvent::Type::Modified) {
            result.type = FileWatcherEvent::Type::Modified;
        } else {
            warnln("Unknown event type {} returned by the watch_file descriptor for {}", (unsigned)event.type, m_path.characters());
            return;
        }

        if (result.type == FileWatcherEvent::Type::ChildAdded || result.type == FileWatcherEvent::Type::ChildRemoved) {
            auto child_path = get_child_path_from_inode_index(m_path, event.inode_index);
            if (!LexicalPath(child_path).is_valid())
                return;

            result.child_path = child_path;
        }

        on_change(result);
    };
}

FileWatcher::~FileWatcher()
{
    m_notifier->on_ready_to_read = nullptr;
    close(m_notifier->fd());
    dbgln_if(FILE_WATCHER_DEBUG, "Ended watcher for file '{}'", m_path.characters());
}

#endif

}
