#pragma once

#include "FileSystem.h"
#include "UnixTypes.h"
#include <AK/HashMap.h>

class SyntheticFileSystem final : public FileSystem {
public:
    virtual ~SyntheticFileSystem() override;
    static RetainPtr<SyntheticFileSystem> create();

    virtual bool initialize() override;
    virtual const char* className() const override;
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual bool enumerateDirectoryInode(InodeIdentifier, std::function<bool(const DirectoryEntry&)>) const override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool setModificationTime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, word mode) override;
    virtual ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, size_t count, byte* buffer) const override;

private:
    SyntheticFileSystem();

    struct File {
        String name;
        InodeMetadata metadata;
        ByteBuffer data;
    };

    OwnPtr<File> createTextFile(String&& name, String&& text);
    void addFile(OwnPtr<File>&&);

    Vector<OwnPtr<File>> m_files;
};

