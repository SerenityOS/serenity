#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/TTY/SlavePTY.h>

NonnullRefPtr<DevPtsFS> DevPtsFS::create()
{
    return adopt(*new DevPtsFS);
}

DevPtsFS::DevPtsFS()
{
}

DevPtsFS::~DevPtsFS()
{
}

static HashTable<unsigned>* ptys;

bool DevPtsFS::initialize()
{
    if (ptys == nullptr) {
        ptys = new HashTable<unsigned>();
    }

    m_root_inode = adopt(*new DevPtsFSInode(*this, 1));
    m_root_inode->m_metadata.inode = { fsid(), 1 };
    m_root_inode->m_metadata.mode = 0040555;
    m_root_inode->m_metadata.uid = 0;
    m_root_inode->m_metadata.gid = 0;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = mepoch;

    return true;
}

static unsigned inode_index_to_pty_index(unsigned inode_index)
{
    ASSERT(inode_index > 1);
    return inode_index - 2;
}

static unsigned pty_index_to_inode_index(unsigned pty_index)
{
    return pty_index + 2;
}

InodeIdentifier DevPtsFS::root_inode() const
{
    return { fsid(), 1 };
}

RefPtr<Inode> DevPtsFS::create_inode(InodeIdentifier, const String&, mode_t, off_t, dev_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

RefPtr<Inode> DevPtsFS::create_directory(InodeIdentifier, const String&, mode_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

RefPtr<Inode> DevPtsFS::get_inode(InodeIdentifier inode_id) const
{
    if (inode_id.index() == 1)
        return m_root_inode;

    unsigned pty_index = inode_index_to_pty_index(inode_id.index());
    auto* device = Device::get_device(201, pty_index);
    ASSERT(device);

    auto inode = adopt(*new DevPtsFSInode(const_cast<DevPtsFS&>(*this), inode_id.index()));
    inode->m_metadata.inode = inode_id;
    inode->m_metadata.size = 0;
    inode->m_metadata.uid = device->uid();
    inode->m_metadata.gid = device->gid();
    inode->m_metadata.mode = 0020644;
    inode->m_metadata.major_device = device->major();
    inode->m_metadata.minor_device = device->minor();
    inode->m_metadata.mtime = mepoch;

    return inode;
}

void DevPtsFS::register_slave_pty(SlavePTY& slave_pty)
{
    ptys->set(slave_pty.index());
}

void DevPtsFS::unregister_slave_pty(SlavePTY& slave_pty)
{
    ptys->remove(slave_pty.index());
}

DevPtsFSInode::DevPtsFSInode(DevPtsFS& fs, unsigned index)
    : Inode(fs, index)
{
}

DevPtsFSInode::~DevPtsFSInode()
{
}

ssize_t DevPtsFSInode::read_bytes(off_t, ssize_t, u8*, FileDescription*) const
{
    ASSERT_NOT_REACHED();
}

ssize_t DevPtsFSInode::write_bytes(off_t, ssize_t, const u8*, FileDescription*)
{
    ASSERT_NOT_REACHED();
}

InodeMetadata DevPtsFSInode::metadata() const
{
    return m_metadata;
}

bool DevPtsFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    if (identifier().index() > 1)
        return false;

    callback({ ".", 1, identifier(), 0 });
    callback({ "..", 2, identifier(), 0 });

    for (unsigned pty_index : *ptys) {
        String name = String::number(pty_index);
        InodeIdentifier identifier = { fsid(), pty_index_to_inode_index(pty_index) };
        callback({ name.characters(), name.length(), identifier, 0 });
    }

    return true;
}

size_t DevPtsFSInode::directory_entry_count() const
{
    ASSERT(identifier().index() == 1);

    return 2 + ptys->size();
}

InodeIdentifier DevPtsFSInode::lookup(StringView name)
{
    ASSERT(identifier().index() == 1);

    if (name == "." || name == "..")
        return identifier();

    bool ok;
    unsigned pty_index = name.to_uint(ok);
    if (ok && ptys->contains(pty_index)) {
        return { fsid(), pty_index_to_inode_index(pty_index) };
    }

    return {};
}

void DevPtsFSInode::flush_metadata()
{
}

KResult DevPtsFSInode::add_child(InodeIdentifier, const StringView&, mode_t)
{
    return KResult(-EROFS);
}

KResult DevPtsFSInode::remove_child(const StringView&)
{
    return KResult(-EROFS);
}

KResult DevPtsFSInode::chmod(mode_t)
{
    return KResult(-EPERM);
}

KResult DevPtsFSInode::chown(uid_t, gid_t)
{
    return KResult(-EPERM);
}
