#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/UnixTypes.h>
#include <AK/HashMap.h>

class SynthFSInode;

class SynthFS : public FS {
public:
    virtual ~SynthFS() override;
    static Retained<SynthFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;
    virtual InodeIdentifier root_inode() const override;
    virtual RetainPtr<Inode> create_inode(InodeIdentifier parentInode, const String& name, mode_t, off_t size, dev_t, int& error) override;
    virtual RetainPtr<Inode> create_directory(InodeIdentifier parentInode, const String& name, mode_t, int& error) override;
    virtual RetainPtr<Inode> get_inode(InodeIdentifier) const override;

protected:
    typedef unsigned InodeIndex;

    InodeIndex generate_inode_index();
    static constexpr InodeIndex RootInodeIndex = 1;

    SynthFS();

    Retained<SynthFSInode> create_directory(String&& name);
    Retained<SynthFSInode> create_text_file(String&& name, ByteBuffer&&, mode_t = 0010644);
    Retained<SynthFSInode> create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&&, mode_t = 0100644);
    Retained<SynthFSInode> create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&&, Function<ssize_t(SynthFSInode&, const ByteBuffer&)>&&, mode_t = 0100644);

    InodeIdentifier add_file(RetainPtr<SynthFSInode>&&, InodeIndex parent = RootInodeIndex);
    bool remove_file(InodeIndex);

private:
    InodeIndex m_next_inode_index { 2 };
    HashMap<InodeIndex, RetainPtr<SynthFSInode>> m_inodes;
};

struct SynthFSInodeCustomData {
    virtual ~SynthFSInodeCustomData();
};

class SynthFSInode final : public Inode {
    friend class SynthFS;
    friend class DevPtsFS;
public:
    virtual ~SynthFSInode() override;

    void set_custom_data(OwnPtr<SynthFSInodeCustomData>&& custom_data) { m_custom_data = move(custom_data); }
    SynthFSInodeCustomData* custom_data() { return m_custom_data.ptr(); }
    const SynthFSInodeCustomData* custom_data() const { return m_custom_data.ptr(); }

private:
    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, byte* buffer, FileDescriptor*) const override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const override;
    virtual InodeIdentifier lookup(const String& name) override;
    virtual String reverse_lookup(InodeIdentifier) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const byte* buffer, FileDescriptor*) override;
    virtual KResult add_child(InodeIdentifier child_id, const String& name, byte file_type) override;
    virtual KResult remove_child(const String& name) override;
    virtual RetainPtr<Inode> parent() const override;
    virtual size_t directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;

    SynthFS& fs();
    const SynthFS& fs() const;
    SynthFSInode(SynthFS&, unsigned index);

    String m_name;
    InodeIdentifier m_parent;
    ByteBuffer m_data;
    Function<ByteBuffer(SynthFSInode&)> m_generator;
    Function<ssize_t(SynthFSInode&, const ByteBuffer&)> m_write_callback;
    Vector<SynthFSInode*> m_children;
    InodeMetadata m_metadata;
    OwnPtr<SynthFSInodeCustomData> m_custom_data;
};

inline SynthFS& SynthFSInode::fs()
{
    return static_cast<SynthFS&>(Inode::fs());
}

inline const SynthFS& SynthFSInode::fs() const
{
    return static_cast<const SynthFS&>(Inode::fs());
}
