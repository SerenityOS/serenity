#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

static dword s_lastFileSystemID;
static HashMap<dword, FS*>* s_fs_map;

static HashMap<dword, FS*>& all_fses()
{
    if (!s_fs_map)
        s_fs_map = new HashMap<dword, FS*>();
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

FS* FS::from_fsid(dword id)
{
    auto it = all_fses().find(id);
    if (it != all_fses().end())
        return (*it).value;
    return nullptr;
}

FS::DirectoryEntry::DirectoryEntry(const char* n, InodeIdentifier i, byte ft)
    : name_length(strlen(n))
    , inode(i)
    , file_type(ft)
{
    memcpy(name, n, name_length);
    name[name_length] = '\0';
}

FS::DirectoryEntry::DirectoryEntry(const char* n, int nl, InodeIdentifier i, byte ft)
    : name_length(nl)
    , inode(i)
    , file_type(ft)
{
    memcpy(name, n, nl);
    name[nl] = '\0';
}

void FS::sync()
{
    Inode::sync();

    Vector<Retained<FS>, 32> fses;
    {
        InterruptDisabler disabler;
        for (auto& it : all_fses())
            fses.append(*it.value);
    }

    for (auto fs : fses)
        fs->flush_writes();
}
