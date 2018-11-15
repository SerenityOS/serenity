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
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool set_mtime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) override;
    virtual Unix::ssize_t read_inode_bytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer, FileDescriptor*) const override;
    virtual InodeIdentifier create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t) override;
    virtual InodeIdentifier find_parent_of_inode(InodeIdentifier) const override;
    virtual RetainPtr<CoreInode> get_inode(InodeIdentifier) const override;

protected:
    typedef unsigned InodeIndex;

    InodeIndex generateInodeIndex();
    static constexpr InodeIndex RootInodeIndex = 1;

    SynthFS();

    RetainPtr<SynthFSInode> create_directory(String&& name);
    RetainPtr<SynthFSInode> create_text_file(String&& name, ByteBuffer&&, Unix::mode_t = 0010644);
    RetainPtr<SynthFSInode> create_generated_file(String&& name, Function<ByteBuffer()>&&, Unix::mode_t = 0100644);

    InodeIdentifier addFile(RetainPtr<SynthFSInode>&&, InodeIndex parent = RootInodeIndex);
    bool removeFile(InodeIndex);

private:
    InodeIndex m_nextInodeIndex { 2 };
    HashMap<InodeIndex, RetainPtr<SynthFSInode>> m_inodes;
};

class SynthFSInode final : public CoreInode {
    friend class SynthFS;
public:
    virtual ~SynthFSInode() override;

private:
    // ^CoreInode
    virtual Unix::ssize_t read_bytes(Unix::off_t, Unix::size_t, byte* buffer, FileDescriptor*) override;
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
