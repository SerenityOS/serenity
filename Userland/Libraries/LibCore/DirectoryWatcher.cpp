/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "DirectoryWatcher.h"
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <LibCore/DirIterator.h>
#include <fcntl.h>
#include <sys/stat.h>

namespace Core {

// Only supported in serenity mode because we use `watch_file`
#ifdef __serenity__

DirectoryWatcher::DirectoryWatcher(const String& path)
    : m_path(path)
{
    m_watcher_fd = watch_file(path.characters(), path.length());
    ASSERT(m_watcher_fd != -1);
}

DirectoryWatcher::~DirectoryWatcher()
{
    close(m_watcher_fd);
}

Optional<DirectoryWatcher::Event> DirectoryWatcher::wait_for_event()
{
    InodeWatcherEvent event {};
    int rc = read(m_watcher_fd, &event, sizeof(event));
    if (rc <= 0)
        return {};

    Event result;
    if (event.type == InodeWatcherEvent::Type::ChildAdded)
        result.type = Event::Type::ChildAdded;
    else if (event.type == InodeWatcherEvent::Type::ChildRemoved)
        result.type = Event::Type::ChildRemoved;
    else
        return {};

    auto child_path = get_child_with_inode_index(event.inode_index);
    if (!LexicalPath(child_path).is_valid())
        return {};

    result.child_path = child_path;
    return result;
}

String DirectoryWatcher::get_child_with_inode_index(unsigned child_inode_index) const
{
    DirIterator iterator(m_path, Core::DirIterator::SkipDots);
    if (iterator.has_error()) {
        return {};
    }

    while (iterator.has_next()) {
        auto child_full_path = String::formatted("{}/{}", m_path, iterator.next_path());
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

#endif

}
