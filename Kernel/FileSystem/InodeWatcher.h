/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/CircularQueue.h>
#include <AK/WeakPtr.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Lock.h>

namespace Kernel {

class Inode;

class InodeWatcher final : public File {
public:
    static NonnullRefPtr<InodeWatcher> create(Inode&);
    virtual ~InodeWatcher() override;

    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual String absolute_path(const FileDescription&) const override;
    virtual const char* class_name() const override { return "InodeWatcher"; };

    void notify_inode_event(Badge<Inode>, InodeWatcherEvent::Type);
    void notify_child_added(Badge<Inode>, const InodeIdentifier& child_id);
    void notify_child_removed(Badge<Inode>, const InodeIdentifier& child_id);

private:
    explicit InodeWatcher(Inode&);

    Lock m_lock;
    WeakPtr<Inode> m_inode;
    CircularQueue<InodeWatcherEvent, 32> m_queue;
};

}
