#include <Kernel/FileSystem/Inode.h>
#include <AK/StringBuilder.h>
#include <Kernel/VM/VMObject.h>
#include <Kernel/Net/LocalSocket.h>

HashTable<Inode*>& all_inodes()
{
    static HashTable<Inode*>* s_inode_set;
    if (!s_inode_set)
        s_inode_set = new HashTable<Inode*>();
    return *s_inode_set;
}

void Inode::sync()
{
    Vector<Retained<Inode>, 32> inodes;
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

unsigned Inode::fsid() const
{
    return m_fs.fsid();
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
