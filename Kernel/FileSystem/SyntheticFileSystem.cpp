#include <AK/StdLibExtras.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/SyntheticFileSystem.h>
#include <LibC/errno_numbers.h>

//#define SYNTHFS_DEBUG

Retained<SynthFS> SynthFS::create()
{
    return adopt(*new SynthFS);
}

SynthFS::SynthFS()
{
}

SynthFS::~SynthFS()
{
}

bool SynthFS::initialize()
{
    // Add a File for the root directory.
    // FIXME: This needs work.
    auto root = adopt(*new SynthFSInode(*this, RootInodeIndex));
    root->m_parent = { fsid(), RootInodeIndex };
    root->m_metadata.mode = 0040555;
    root->m_metadata.uid = 0;
    root->m_metadata.gid = 0;
    root->m_metadata.size = 0;
    root->m_metadata.mtime = mepoch;
    m_inodes.set(RootInodeIndex, move(root));
    return true;
}

Retained<SynthFSInode> SynthFS::create_directory(String&& name)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));
    file->m_name = move(name);
    file->m_metadata.size = 0;
    file->m_metadata.uid = 0;
    file->m_metadata.gid = 0;
    file->m_metadata.mode = 0040555;
    file->m_metadata.mtime = mepoch;
    return file;
}

Retained<SynthFSInode> SynthFS::create_text_file(String&& name, ByteBuffer&& contents, mode_t mode)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));
    file->m_data = contents;
    file->m_name = move(name);
    file->m_metadata.size = file->m_data.size();
    file->m_metadata.uid = 100;
    file->m_metadata.gid = 200;
    file->m_metadata.mode = mode;
    file->m_metadata.mtime = mepoch;
    return file;
}

Retained<SynthFSInode> SynthFS::create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&& generator, mode_t mode)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));
    file->m_generator = move(generator);
    file->m_name = move(name);
    file->m_metadata.size = 0;
    file->m_metadata.uid = 0;
    file->m_metadata.gid = 0;
    file->m_metadata.mode = mode;
    file->m_metadata.mtime = mepoch;
    return file;
}

Retained<SynthFSInode> SynthFS::create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&& read_callback, Function<ssize_t(SynthFSInode&, const ByteBuffer&)>&& write_callback, mode_t mode)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));
    file->m_generator = move(read_callback);
    file->m_write_callback = move(write_callback);
    file->m_name = move(name);
    file->m_metadata.size = 0;
    file->m_metadata.uid = 0;
    file->m_metadata.gid = 0;
    file->m_metadata.mode = mode;
    file->m_metadata.mtime = mepoch;
    return file;
}

InodeIdentifier SynthFS::add_file(RetainPtr<SynthFSInode>&& file, InodeIndex parent)
{
    LOCKER(m_lock);
    ASSERT(file);
    auto it = m_inodes.find(parent);
    ASSERT(it != m_inodes.end());
    auto new_inode_id = file->identifier();
    file->m_metadata.inode = new_inode_id;
    file->m_parent = { fsid(), parent };
    (*it).value->m_children.append(file.ptr());
    m_inodes.set(new_inode_id.index(), move(file));
    return new_inode_id;
}

bool SynthFS::remove_file(InodeIndex inode)
{
    LOCKER(m_lock);
    auto it = m_inodes.find(inode);
    if (it == m_inodes.end())
        return false;
    auto& file = *(*it).value;

    auto pit = m_inodes.find(file.m_parent.index());
    if (pit == m_inodes.end())
        return false;
    auto& parent = *(*pit).value;
    for (ssize_t i = 0; i < parent.m_children.size(); ++i) {
        if (parent.m_children[i]->m_metadata.inode.index() != inode)
            continue;
        parent.m_children.remove(i);
        break;
    }

    Vector<InodeIndex> indices_to_remove;
    indices_to_remove.ensure_capacity(file.m_children.size());
    for (auto& child : file.m_children)
        indices_to_remove.unchecked_append(child->m_metadata.inode.index());
    for (auto& index : indices_to_remove)
        remove_file(index);
    m_inodes.remove(inode);
    return true;
}

const char* SynthFS::class_name() const
{
    return "synthfs";
}

InodeIdentifier SynthFS::root_inode() const
{
    return { fsid(), 1 };
}

RetainPtr<Inode> SynthFS::create_inode(InodeIdentifier parentInode, const String& name, mode_t mode, off_t size, dev_t, int& error)
{
    (void)parentInode;
    (void)name;
    (void)mode;
    (void)size;
    (void)error;
    kprintf("FIXME: Implement SyntheticFileSystem::create_inode().\n");
    return {};
}

RetainPtr<Inode> SynthFS::create_directory(InodeIdentifier, const String&, mode_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

auto SynthFS::generate_inode_index() -> InodeIndex
{
    LOCKER(m_lock);
    return m_next_inode_index++;
}

RetainPtr<Inode> SynthFS::get_inode(InodeIdentifier inode) const
{
    LOCKER(m_lock);
    auto it = m_inodes.find(inode.index());
    if (it == m_inodes.end())
        return {};
    return (*it).value;
}

SynthFSInode::SynthFSInode(SynthFS& fs, unsigned index)
    : Inode(fs, index)
{
    m_metadata.inode = { fs.fsid(), index };
}

SynthFSInode::~SynthFSInode()
{
}

InodeMetadata SynthFSInode::metadata() const
{
    return m_metadata;
}

ssize_t SynthFSInode::read_bytes(off_t offset, ssize_t count, byte* buffer, FileDescription* descriptor) const
{
    LOCKER(m_lock);
#ifdef SYNTHFS_DEBUG
    kprintf("SynthFS: read_bytes %u\n", index());
#endif
    ASSERT(offset >= 0);
    ASSERT(buffer);

    ByteBuffer generated_data;
    if (m_generator) {
        if (!descriptor) {
            generated_data = m_generator(const_cast<SynthFSInode&>(*this));
        } else {
            if (!descriptor->generator_cache())
                descriptor->generator_cache() = m_generator(const_cast<SynthFSInode&>(*this));
            generated_data = descriptor->generator_cache();
        }
    }

    auto* data = generated_data ? &generated_data : &m_data;
    ssize_t nread = min(static_cast<off_t>(data->size() - offset), static_cast<off_t>(count));
    memcpy(buffer, data->pointer() + offset, nread);
    if (nread == 0 && descriptor && descriptor->generator_cache())
        descriptor->generator_cache().clear();
    return nread;
}

bool SynthFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    LOCKER(m_lock);
#ifdef SYNTHFS_DEBUG
    kprintf("SynthFS: traverse_as_directory %u\n", index());
#endif

    if (!m_metadata.is_directory())
        return false;

    callback({ ".", 1, m_metadata.inode, 2 });
    callback({ "..", 2, m_parent, 2 });

    for (auto& child : m_children)
        callback({ child->m_name.characters(), child->m_name.length(), child->m_metadata.inode, child->m_metadata.is_directory() ? (byte)2 : (byte)1 });
    return true;
}

InodeIdentifier SynthFSInode::lookup(StringView name)
{
    LOCKER(m_lock);
    ASSERT(is_directory());
    if (name == ".")
        return identifier();
    if (name == "..")
        return m_parent;
    for (auto& child : m_children) {
        if (child->m_name == name)
            return child->identifier();
    }
    return {};
}

void SynthFSInode::flush_metadata()
{
}

ssize_t SynthFSInode::write_bytes(off_t offset, ssize_t size, const byte* buffer, FileDescription*)
{
    LOCKER(m_lock);
    if (!m_write_callback)
        return -EPERM;
    // FIXME: Being able to write into SynthFS at a non-zero offset seems like something we should support..
    ASSERT(offset == 0);
    bool success = m_write_callback(*this, ByteBuffer::wrap(buffer, size));
    ASSERT(success);
    return 0;
}

KResult SynthFSInode::add_child(InodeIdentifier child_id, const StringView& name, mode_t)
{
    (void)child_id;
    (void)name;
    ASSERT_NOT_REACHED();
}

KResult SynthFSInode::remove_child(const StringView& name)
{
    (void)name;
    ASSERT_NOT_REACHED();
}

SynthFSInodeCustomData::~SynthFSInodeCustomData()
{
}

size_t SynthFSInode::directory_entry_count() const
{
    LOCKER(m_lock);
    ASSERT(is_directory());
    // NOTE: The 2 is for '.' and '..'
    return m_children.size() + 2;
}

KResult SynthFSInode::chmod(mode_t)
{
    return KResult(-EPERM);
}

KResult SynthFSInode::chown(uid_t, gid_t)
{
    return KResult(-EPERM);
}
