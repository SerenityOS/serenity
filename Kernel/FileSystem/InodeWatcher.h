#pragma once

#include <AK/Badge.h>
#include <AK/CircularQueue.h>
#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/File.h>

class Inode;

class InodeWatcher final : public File {
public:
    static NonnullRefPtr<InodeWatcher> create(Inode&);
    virtual ~InodeWatcher() override;

    struct Event {
        enum class Type {
            Invalid = 0,
            Modified,
        };

        Type type { Type::Invalid };
    };

    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual String absolute_path(const FileDescription&) const override;
    virtual const char* class_name() const override { return "InodeWatcher"; };

    void notify_inode_event(Badge<Inode>, Event::Type);

private:
    explicit InodeWatcher(Inode&);

    WeakPtr<Inode> m_inode;
    CircularQueue<Event, 32> m_queue;
};
