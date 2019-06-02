#pragma once

#include <Kernel/File.h>

class Inode;

class InodeFile final : public File {
public:
    static Retained<InodeFile> create(Retained<Inode>&& inode)
    {
        return adopt(*new InodeFile(move(inode)));
    }

    virtual ~InodeFile() override;

    const Inode& inode() const { return *m_inode; }
    Inode& inode() { return *m_inode; }

    virtual bool can_read(FileDescriptor&) const override { return true; }
    virtual bool can_write(FileDescriptor&) const override { return true; }

    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescriptor&, LinearAddress preferred_laddr, size_t offset, size_t size, int prot) override;

    virtual String absolute_path(const FileDescriptor&) const override;

    virtual KResult truncate(off_t) override;

    virtual const char* class_name() const override { return "InodeFile"; }

    virtual bool is_seekable() const override { return true; }
    virtual bool is_inode() const override { return true; }

private:
    explicit InodeFile(Retained<Inode>&&);
    Retained<Inode> m_inode;
};
