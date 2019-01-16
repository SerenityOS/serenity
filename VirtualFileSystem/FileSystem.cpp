#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <LibC/errno_numbers.h>
#include "FileSystem.h"
#include "MemoryManager.h"

static dword s_lastFileSystemID;
static HashMap<dword, FS*>* s_fs_map;
static HashTable<Inode*>* s_inode_set;

static HashMap<dword, FS*>& all_fses()
{
    if (!s_fs_map)
        s_fs_map = new HashMap<dword, FS*>();
    return *s_fs_map;
}

HashTable<Inode*>& all_inodes()
{
    if (!s_inode_set)
        s_inode_set = new HashTable<Inode*>();
    return *s_inode_set;
}

void FS::initialize_globals()
{
    s_lastFileSystemID = 0;
    s_fs_map = nullptr;
    s_inode_set = nullptr;
}

FS::FS()
    : m_fsid(++s_lastFileSystemID)
{
    all_fses().set(m_fsid, this);
}

FS::~FS()
{
    all_fses().remove(m_fsid);
}

FS* FS::from_fsid(dword id)
{
    auto it = all_fses().find(id);
    if (it != all_fses().end())
        return (*it).value;
    return nullptr;
}

ByteBuffer Inode::read_entire(FileDescriptor* descriptor)
{
    size_t initial_size = metadata().size ? metadata().size : 4096;
    auto contents = ByteBuffer::create_uninitialized(initial_size);

    ssize_t nread;
    byte buffer[4096];
    byte* out = contents.pointer();
    Unix::off_t offset = 0;
    for (;;) {
        nread = read_bytes(offset, sizeof(buffer), buffer, descriptor);
        ASSERT(nread <= (ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        memcpy(out, buffer, nread);
        out += nread;
        offset += nread;
        ASSERT(offset <= (ssize_t)initial_size); // FIXME: Support dynamically growing the buffer.
    }
    if (nread < 0) {
        kprintf("Inode::read_entire: ERROR: %d\n", nread);
        return nullptr;
    }

    contents.trim(offset);
    return contents;
}

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, byte ft)
    : name_length(strlen(n))
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, size_t nl, InodeIdentifier i, byte ft)
    : name_length(nl)
    , inode(i)
    , fileType(ft)
{
    memcpy(name, n, nl);
    name[nl] = '\0';
}

Inode::Inode(FS& fs, unsigned index)
    : m_fs(fs)
    , m_index(index)
{
    all_inodes().set(this);
}

Inode::~Inode()
{
    all_inodes().remove(this);
}

void Inode::will_be_destroyed()
{
    if (m_metadata_dirty)
        flush_metadata();
}

int Inode::set_atime(Unix::time_t)
{
    return -ENOTIMPL;
}

int Inode::set_ctime(Unix::time_t)
{
    return -ENOTIMPL;
}

int Inode::set_mtime(Unix::time_t)
{
    return -ENOTIMPL;
}

int Inode::increment_link_count()
{
    return -ENOTIMPL;
}

int Inode::decrement_link_count()
{
    return -ENOTIMPL;
}

void FS::sync()
{
    for (auto* inode : all_inodes()) {
        if (inode->is_metadata_dirty())
            inode->flush_metadata();
    }
}

void Inode::set_vmo(RetainPtr<VMObject>&& vmo)
{
    m_vmo = move(vmo);
}
