#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

static u32 s_lastFileSystemID;
static HashMap<u32, FS*>* s_fs_map;

static HashMap<u32, FS*>& all_fses()
{
    if (!s_fs_map)
        s_fs_map = new HashMap<u32, FS*>();
    return *s_fs_map;
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

FS* FS::from_fsid(u32 id)
{
    auto it = all_fses().find(id);
    if (it != all_fses().end())
        return (*it).value;
    return nullptr;
}

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, u8 ft)
    : name_length(strlen(n))
    , inode(i)
    , file_type(ft)
{
    ASSERT(name_length < (int)sizeof(name));
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, size_t nl, InodeIdentifier i, u8 ft)
    : name_length(nl)
    , inode(i)
    , file_type(ft)
{
    ASSERT(name_length < (int)sizeof(name));
    memcpy(name, n, nl);
    name[nl] = '\0';
}

void FS::sync()
{
    Inode::sync();

    NonnullRefPtrVector<FS, 32> fses;
    {
        InterruptDisabler disabler;
        for (auto& it : all_fses())
            fses.append(*it.value);
    }

    for (auto& fs : fses)
        fs.flush_writes();
}

void FS::lock_all()
{
    for (auto& it : all_fses()) {
        it.value->m_lock.lock();
    }
}

void FS::set_block_size(int block_size)
{
    ASSERT(block_size > 0);
    if (block_size == m_block_size)
        return;
    m_block_size = block_size;
}
