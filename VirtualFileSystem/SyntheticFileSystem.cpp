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
    root->m_parent = { id(), RootInodeIndex };
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

RetainPtr<SynthFSInode> SynthFS::create_text_file(String&& name, ByteBuffer&& contents, Unix::mode_t mode)
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

RetainPtr<SynthFSInode> SynthFS::create_generated_file(String&& name, Function<ByteBuffer()>&& generator, Unix::mode_t mode)
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

InodeIdentifier SynthFS::add_file(RetainPtr<SynthFSInode>&& file, InodeIndex parent)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(file);
    auto it = m_inodes.find(parent);
    ASSERT(it != m_inodes.end());
    auto new_inode_id = file->identifier();
    file->m_metadata.inode = new_inode_id;
    file->m_parent = { id(), parent };
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

    for (auto& child : file.m_children)
        remove_file(child->m_metadata.inode.index());
    m_inodes.remove(inode);
    return true;
}

const char* SynthFS::class_name() const
{
    return "synthfs";
}

InodeIdentifier SynthFS::root_inode() const
{
    return { id(), 1 };
}

RetainPtr<Inode> SynthFS::create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t mode, unsigned size, int& error)
{
    (void) parentInode;
    (void) name;
    (void) mode;
    (void) size;
    (void) error;
    kprintf("FIXME: Implement SyntheticFileSystem::create_inode().\n");
    return { };
}

RetainPtr<Inode> SynthFS::create_directory(InodeIdentifier, const String&, Unix::mode_t, int& error)
{
    error = -EROFS;
    return nullptr;
}

auto SynthFS::generate_inode_index() -> InodeIndex
{
    return m_next_inode_index++;
}

InodeIdentifier SynthFS::find_parent_of_inode(InodeIdentifier inode) const
{
    auto it = m_inodes.find(inode.index());
    if (it == m_inodes.end())
        return { };
    return (*it).value->m_parent;
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
    m_metadata.inode = { fs.id(), index };
}

SynthFSInode::~SynthFSInode()
{
}

void SynthFSInode::populate_metadata() const
{
    // Already done when SynthFS created the file.
}

ssize_t SynthFSInode::read_bytes(Unix::off_t offset, size_t count, byte* buffer, FileDescriptor* descriptor)
{
#ifdef SYNTHFS_DEBUG
    kprintf("SynthFS: read_bytes %u\n", index());
#endif
    ASSERT(offset >= 0);
    ASSERT(buffer);

    ByteBuffer generatedData;
    if (m_generator) {
        if (!descriptor) {
            generatedData = m_generator();
        } else {
            if (!descriptor->generator_cache())
                descriptor->generator_cache() = m_generator();
            generatedData = descriptor->generator_cache();
        }
    }

    auto* data = generatedData ? &generatedData : &m_data;
    ssize_t nread = min(static_cast<Unix::off_t>(data->size() - offset), static_cast<Unix::off_t>(count));
    memcpy(buffer, data->pointer() + offset, nread);
    if (nread == 0 && descriptor && descriptor->generator_cache())
        descriptor->generator_cache().clear();
    return nread;
}

bool SynthFSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback)
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

bool SynthFSInode::write(const ByteBuffer&)
{
    ASSERT_NOT_REACHED();
    return false;
}
