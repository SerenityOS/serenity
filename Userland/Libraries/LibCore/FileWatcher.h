/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibCore/Notifier.h>

namespace Core {

struct FileWatcherEvent {
    enum class Type {
        Modified,
        ChildAdded,
        ChildRemoved,
    };
    Type type;
    String child_path;
};

class BlockingFileWatcher {
    AK_MAKE_NONCOPYABLE(BlockingFileWatcher);

public:
    explicit BlockingFileWatcher(const String& path);
    ~BlockingFileWatcher();

    Optional<FileWatcherEvent> wait_for_event();

private:
    String m_path;
    int m_watcher_fd { -1 };
};

class FileWatcher : public RefCounted<FileWatcher> {
    AK_MAKE_NONCOPYABLE(FileWatcher);

public:
    static Result<NonnullRefPtr<FileWatcher>, String> watch(const String& path);
    ~FileWatcher();

    Function<void(FileWatcherEvent)> on_change;

private:
    FileWatcher(NonnullRefPtr<Notifier>, const String& path);

    NonnullRefPtr<Notifier> m_notifier;
    String m_path;
};

}
