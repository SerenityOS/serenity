#pragma once

#include "FileSystem.h"
#include "UnixTypes.h"
#include <AK/HashMap.h>

class SyntheticFileSystem : public FileSystem {
public:
    virtual ~SyntheticFileSystem() override;
    static RetainPtr<SyntheticFileSystem> create();

    virtual bool initialize() override;
    virtual const char* className() const override;
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual bool enumerateDirectoryInode(InodeIdentifier, Function<bool(const DirectoryEntry&)>) const override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool setModificationTime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) override;
    virtual Unix::ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer, FileDescriptor*) const override;
    virtual InodeIdentifier makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t) override;
    virtual InodeIdentifier findParentOfInode(InodeIdentifier) const override;

protected:
    typedef unsigned InodeIndex;

    InodeIndex generateInodeIndex();
    static constexpr InodeIndex RootInodeIndex = 1;

    SyntheticFileSystem();

    struct File {
        String name;
        InodeMetadata metadata;
        InodeIdentifier parent;
        ByteBuffer data;
        Function<ByteBuffer()> generator;
        Vector<File*> children;
    };

    OwnPtr<File> createDirectory(String&& name);
    OwnPtr<File> createTextFile(String&& name, ByteBuffer&&, Unix::mode_t = 0010644);
    OwnPtr<File> createGeneratedFile(String&& name, Function<ByteBuffer()>&&, Unix::mode_t = 0100644);

    InodeIdentifier addFile(OwnPtr<File>&&, InodeIndex parent = RootInodeIndex);
    bool removeFile(InodeIndex);

private:
    InodeIndex m_nextInodeIndex { 2 };
    HashMap<InodeIndex, OwnPtr<File>> m_inodes;
};

