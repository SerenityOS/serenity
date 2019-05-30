#include <AK/HashTable.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Lock.h>

static Lockable<HashTable<Custody*>>& all_custodies()
{
    static Lockable<HashTable<Custody*>>* table;
    if (!table)
        table = new Lockable<HashTable<Custody*>>;
    return *table;
}

Custody::Custody(Custody* parent, const String& name, Inode& inode)
    : m_parent(parent)
    , m_name(name)
    , m_inode(inode)
{
    LOCKER(all_custodies().lock());
    all_custodies().resource().set(this);
}

Custody::~Custody()
{
    LOCKER(all_custodies().lock());
    all_custodies().resource().remove(this);
}
