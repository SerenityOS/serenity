#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
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

String Custody::absolute_path() const
{
    Vector<const Custody*, 32> custody_chain;
    for (auto* custody = this; custody; custody = custody->parent())
        custody_chain.append(custody);
    StringBuilder builder;
    for (int i = custody_chain.size() - 2; i >= 0; --i) {
        builder.append('/');
        builder.append(custody_chain[i]->name().characters());
    }
    return builder.to_string();
}
