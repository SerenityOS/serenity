#pragma once

#include "FileSystem.h"
#include "UnixTypes.h"
#include <AK/HashMap.h>

class SynthFSInode;

class SynthFS : public FS {
public:
    virtual ~SynthFS() override;
    static RetainPtr<SynthFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;
    virtual InodeIdentifier root_inode() const override;
    virtual bool write_inode(InodeIdentifier, const ByteBuffer&) override;
    virtual InodeMetadata inode_metadata(InodeIdentifier) const override;
    virtual int set_atime_and_mtime(InodeIdentifier, dword atime, dword mtime) override;
    virtual InodeIdentifier create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size, int& error) override;
    virtual ssize_t read_inode_bytes(InodeIdentifier, Unix::off_t offset, size_t count, byte* buffer, FileDescriptor*) const override;
    virtual InodeIdentifier create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t, int& error) override;
    virtual InodeIdentifier find_parent_of_inode(InodeIdentifier) const override;
    virtual RetainPtr<CoreInode> get_inode(InodeIdentifier) const override;

protected:
    typedef unsigned InodeIndex;

    InodeIndex generate_inode_index();
    static constexpr InodeIndex RootInodeIndex = 1;

    SynthFS();

    RetainPtr<SynthFSInode> create_directory(String&& name);
    RetainPtr<SynthFSInode> create_text_file(String&& name, ByteBuffer&&, Unix::mode_t = 0010644);
    RetainPtr<SynthFSInode> create_generated_file(String&& name, Function<ByteBuffer()>&&, Unix::mode_t = 0100644);

    InodeIdentifier add_file(RetainPtr<SynthFSInode>&&, InodeIndex parent = RootInodeIndex);
    bool remove_file(InodeIndex);

private:
    InodeIndex m_next_inode_index { 2 };
    HashMap<InodeIndex, RetainPtr<SynthFSInode>> m_inodes;
};

class SynthFSInode final : public CoreInode {
    friend class SynthFS;
public:
    virtual ~SynthFSInode() override;

private:
    // ^CoreInode
    virtual ssize_t read_bytes(Unix::off_t, size_t, byte* buffer, FileDescriptor*) override;
    virtual void populate_metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) override;
    virtual InodeIdentifier lookup(const String& name) override;
    virtual String reverse_lookup(InodeIdentifier) override;

    SynthFS& fs();
    const SynthFS& fs() const;
    SynthFSInode(SynthFS&, unsigned index);

    String m_name;
    InodeIdentifier m_parent;
    ByteBuffer m_data;
    Function<ByteBuffer()> m_generator;
    Vector<SynthFSInode*> m_children;
};
