#include "SyntheticFileSystem.h"
#include "FileDescriptor.h"
#include <LibC/errno_numbers.h>
#include <AK/StdLibExtras.h>

#ifndef SERENITY
typedef int InterruptDisabler;
#define ASSERT_INTERRUPTS_DISABLED()
#endif

//#define SYNTHFS_DEBUG

RetainPtr<SynthFS> SynthFS::create()
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

#ifndef SERENITY
    addFile(createTextFile("file", String("I'm a synthetic file!\n").toByteBuffer(), 0100644));
    addFile(createTextFile("message", String("Hey! This isn't my bottle!\n").toByteBuffer(), 0100644));
    addFile(createGeneratedFile("lunk", [] { return String("/home/andreas/file1").toByteBuffer(); }, 00120777));
#endif
    return true;
}

RetainPtr<SynthFSInode> SynthFS::create_directory(String&& name)
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

RetainPtr<SynthFSInode> SynthFS::create_text_file(String&& name, ByteBuffer&& contents, mode_t mode)
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

RetainPtr<SynthFSInode> SynthFS::create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&& generator, mode_t mode)
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

RetainPtr<SynthFSInode> SynthFS::create_generated_file(String&& name, Function<ByteBuffer(SynthFSInode&)>&& read_callback, Function<ssize_t(SynthFSInode&, const ByteBuffer&)>&& write_callback, mode_t mode)
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
    ASSERT_INTERRUPTS_DISABLED();
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
    ASSERT_INTERRUPTS_DISABLED();
    auto it = m_inodes.find(inode);
    if (it == m_inodes.end())
        return false;
    auto& file = *(*it).value;

    auto pit = m_inodes.find(file.m_parent.index());
    if (pit == m_inodes.end())
        return false;
    auto& parent = *(*pit).value;
    for (size_t i = 0; i < parent.m_children.size(); ++i) {
        if (parent.m_children[i]->m_metadata.inode.index() != inode) {
            continue;
        }
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

RetainPtr<Inode> SynthFS::create_inode(InodeIdentifier parentInode, const String& name, mode_t mode, unsigned size, int& error)
{
    (void) parentInode;
    (void) name;
    (void) mode;
    (void) size;
    (void) error;
    kprintf("FIXME: Implement SyntheticFileSystem::create_inode().\n");
    return { };
}

RetainPtr<Inode> SynthFS::create_directory(InodeIdentifier, const String&, mode_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

auto SynthFS::generate_inode_index() -> InodeIndex
{
    return m_next_inode_index++;
}

RetainPtr<Inode> SynthFSInode::parent() const
{
    return fs().get_inode(m_parent);
}

RetainPtr<Inode> SynthFS::get_inode(InodeIdentifier inode) const
{
    auto it = m_inodes.find(inode.index());
    if (it == m_inodes.end())
        return { };
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

ssize_t SynthFSInode::read_bytes(off_t offset, size_t count, byte* buffer, FileDescriptor* descriptor) const
{
#ifdef SYNTHFS_DEBUG
    kprintf("SynthFS: read_bytes %u\n", index());
#endif
    ASSERT(offset >= 0);
    ASSERT(buffer);

    ByteBuffer generatedData;
    if (m_generator) {
        if (!descriptor) {
            generatedData = m_generator(const_cast<SynthFSInode&>(*this));
        } else {
            if (!descriptor->generator_cache())
                descriptor->generator_cache() = m_generator(const_cast<SynthFSInode&>(*this));
            generatedData = descriptor->generator_cache();
        }
    }

    auto* data = generatedData ? &generatedData : &m_data;
    ssize_t nread = min(static_cast<off_t>(data->size() - offset), static_cast<off_t>(count));
    memcpy(buffer, data->pointer() + offset, nread);
    if (nread == 0 && descriptor && descriptor->generator_cache())
        descriptor->generator_cache().clear();
    return nread;
}

bool SynthFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    InterruptDisabler disabler;
#ifdef SYNTHFS_DEBUG
    kprintf("SynthFS: traverse_as_directory %u\n", index());
#endif

    if (!m_metadata.isDirectory())
        return false;

    callback({ ".", 1, m_metadata.inode, 2 });
    callback({ "..", 2, m_parent, 2 });

    for (auto& child : m_children)
        callback({ child->m_name.characters(), child->m_name.length(), child->m_metadata.inode, child->m_metadata.isDirectory() ? (byte)2 : (byte)1 });
    return true;
}

InodeIdentifier SynthFSInode::lookup(const String& name)
{
    ASSERT(is_directory());
    if (name == ".")
        return identifier();
    if (name == "..")
        return m_parent;
    for (auto& child : m_children) {
        if (child->m_name == name)
            return child->identifier();
    }
    return { };
}

String SynthFSInode::reverse_lookup(InodeIdentifier child_id)
{
    ASSERT(is_directory());
    for (auto& child : m_children) {
        if (child->identifier() == child_id)
            return child->m_name;
    }
    return { };
}

void SynthFSInode::flush_metadata()
{
}

ssize_t SynthFSInode::write_bytes(off_t offset, size_t size, const byte* buffer, FileDescriptor*)
{
    if (!m_write_callback)
        return -EPERM;
    // FIXME: Being able to write into SynthFS at a non-zero offset seems like something we should support..
    ASSERT(offset == 0);
    bool success = m_write_callback(*this, ByteBuffer::wrap((byte*)buffer, size));
    ASSERT(success);
    return 0;
}

bool SynthFSInode::add_child(InodeIdentifier child_id, const String& name, byte file_type, int& error)
{
    (void) child_id;
    (void) name;
    (void) file_type;
    (void) error;
    ASSERT_NOT_REACHED();
    return false;
}

bool SynthFSInode::remove_child(const String& name, int& error)
{
    (void) name;
    (void) error;
    ASSERT_NOT_REACHED();
    return false;
}

SynthFSInodeCustomData::~SynthFSInodeCustomData()
{
}

size_t SynthFSInode::directory_entry_count() const
{
    ASSERT(is_directory());
    // NOTE: The 2 is for '.' and '..'
    return m_children.size() + 2;
}
