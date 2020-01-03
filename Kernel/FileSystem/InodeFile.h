#pragma once

#include <Kernel/FileSystem/File.h>

class Inode;

class InodeFile final : public File {
public:
    static NonnullRefPtr<InodeFile> create(NonnullRefPtr<Inode>&& inode)
    {
        return adopt(*new InodeFile(move(inode)));
    }

    virtual ~InodeFile() override;

    const Inode& inode() const { return *m_inode; }
    Inode& inode() { return *m_inode; }

    virtual bool can_read(const FileDescription&) const override { return true; }
    virtual bool can_write(const FileDescription&) const override { return true; }

    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot) override;

    virtual String absolute_path(const FileDescription&) const override;

    virtual KResult truncate(off_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult chmod(mode_t) override;

    virtual const char* class_name() const override { return "InodeFile"; }

    virtual bool is_seekable() const override { return true; }
    virtual bool is_inode() const override { return true; }

private:
    explicit InodeFile(NonnullRefPtr<Inode>&&);
    NonnullRefPtr<Inode> m_inode;
};
