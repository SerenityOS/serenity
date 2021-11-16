/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/API/InodeWatcherFlags.h>
#include <LibCore/Notifier.h>

namespace Core {

struct FileWatcherEvent {
    enum class Type {
        Invalid = 0,
        MetadataModified = 1 << 0,
        ContentModified = 1 << 1,
        Deleted = 1 << 2,
        ChildCreated = 1 << 3,
        ChildDeleted = 1 << 4,
    };
    Type type;
    String event_path;
};

AK_ENUM_BITWISE_OPERATORS(FileWatcherEvent::Type);

class FileWatcherBase {
public:
    virtual ~FileWatcherBase() { }

    ErrorOr<bool> add_watch(String path, FileWatcherEvent::Type event_mask);
    ErrorOr<bool> remove_watch(String path);
    bool is_watching(String const& path) const { return m_path_to_wd.find(path) != m_path_to_wd.end(); }

protected:
    FileWatcherBase(int watcher_fd)
        : m_watcher_fd(watcher_fd)
    {
    }

    int m_watcher_fd { -1 };
    HashMap<String, unsigned> m_path_to_wd;
    HashMap<unsigned, String> m_wd_to_path;
};

class BlockingFileWatcher final : public FileWatcherBase {
    AK_MAKE_NONCOPYABLE(BlockingFileWatcher);

public:
    explicit BlockingFileWatcher(InodeWatcherFlags = InodeWatcherFlags::None);
    ~BlockingFileWatcher();

    Optional<FileWatcherEvent> wait_for_event();
};

class FileWatcher final : public FileWatcherBase
    , public RefCounted<FileWatcher> {
    AK_MAKE_NONCOPYABLE(FileWatcher);

public:
    static ErrorOr<NonnullRefPtr<FileWatcher>> create(InodeWatcherFlags = InodeWatcherFlags::None);
    ~FileWatcher();

    Function<void(FileWatcherEvent const&)> on_change;

private:
    FileWatcher(int watcher_fd, NonnullRefPtr<Notifier>);

    NonnullRefPtr<Notifier> m_notifier;
};

}

namespace AK {

template<>
struct Formatter<Core::FileWatcherEvent> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Core::FileWatcherEvent const& value)
    {
        return Formatter<FormatString>::format(builder, "FileWatcherEvent(\"{}\", {})", value.event_path, value.type);
    }
};

template<>
struct Formatter<Core::FileWatcherEvent::Type> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Core::FileWatcherEvent::Type const& value)
    {
        char const* type;
        switch (value) {
        case Core::FileWatcherEvent::Type::ChildCreated:
            type = "ChildCreated";
            break;
        case Core::FileWatcherEvent::Type::ChildDeleted:
            type = "ChildDeleted";
            break;
        case Core::FileWatcherEvent::Type::Deleted:
            type = "Deleted";
            break;
        case Core::FileWatcherEvent::Type::ContentModified:
            type = "ContentModified";
            break;
        case Core::FileWatcherEvent::Type::MetadataModified:
            type = "MetadataModified";
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        return builder.put_string(type);
    }
};

}
