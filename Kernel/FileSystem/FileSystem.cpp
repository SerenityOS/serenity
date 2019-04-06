#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibC/errno_numbers.h>
#include "FileSystem.h"
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/Net/LocalSocket.h>

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

FS::FS()
    : m_lock("FS")
    , m_fsid(++s_lastFileSystemID)
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

ByteBuffer Inode::read_entire(FileDescriptor* descriptor) const
{
    size_t initial_size = metadata().size ? metadata().size : 4096;
    StringBuilder builder(initial_size);

    ssize_t nread;
    byte buffer[4096];
    off_t offset = 0;
    for (;;) {
        nread = read_bytes(offset, sizeof(buffer), buffer, descriptor);
        ASSERT(nread <= (ssize_t)sizeof(buffer));
        if (nread <= 0)
            break;
        builder.append((const char*)buffer, nread);
        offset += nread;
    }
    if (nread < 0) {
        kprintf("Inode::read_entire: ERROR: %d\n", nread);
        return nullptr;
    }

    return builder.to_byte_buffer();
}

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, byte ft)
    : name_length(strlen(n))
    , inode(i)
    , file_type(ft)
{
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, size_t nl, InodeIdentifier i, byte ft)
    : name_length(nl)
    , inode(i)
    , file_type(ft)
{
    memcpy(name, n, nl);
    name[nl] = '\0';
}

Inode::Inode(FS& fs, unsigned index)
    : m_lock("Inode")
    , m_fs(fs)
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

void Inode::inode_contents_changed(off_t offset, ssize_t size, const byte* data)
{
    if (m_vmo)
        m_vmo->inode_contents_changed(Badge<Inode>(), offset, size, data);
}

void Inode::inode_size_changed(size_t old_size, size_t new_size)
{
    if (m_vmo)
        m_vmo->inode_size_changed(Badge<Inode>(), old_size, new_size);
}

int Inode::set_atime(time_t)
{
    return -ENOTIMPL;
}

int Inode::set_ctime(time_t)
{
    return -ENOTIMPL;
}

int Inode::set_mtime(time_t)
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
    Vector<Retained<Inode>> inodes;
    {
        InterruptDisabler disabler;
        for (auto* inode : all_inodes()) {
            if (inode->is_metadata_dirty())
                inodes.append(*inode);
        }
    }

    for (auto& inode : inodes) {
        ASSERT(inode->is_metadata_dirty());
        inode->flush_metadata();
    }
}

void Inode::set_vmo(VMObject& vmo)
{
    m_vmo = vmo.make_weak_ptr();
}

bool Inode::bind_socket(LocalSocket& socket)
{
    ASSERT(!m_socket);
    m_socket = socket;
    return true;
}

bool Inode::unbind_socket()
{
    ASSERT(m_socket);
    m_socket = nullptr;
    return true;
}
