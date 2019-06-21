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

Custody* Custody::get_if_cached(Custody* parent, const String& name)
{
    LOCKER(all_custodies().lock());
    for (auto& custody : all_custodies().resource()) {
        if (custody->is_deleted())
            continue;
        if (custody->is_mounted_on())
            continue;
        if (custody->parent() == parent && custody->name() == name)
            return custody;
    }
    return nullptr;
}

NonnullRefPtr<Custody> Custody::get_or_create(Custody* parent, const String& name, Inode& inode)
{
    if (RefPtr<Custody> cached_custody = get_if_cached(parent, name)) {
        if (&cached_custody->inode() != &inode) {
            dbgprintf("WTF! cached custody for name '%s' has inode=%s, new inode=%s\n",
                name.characters(),
                cached_custody->inode().identifier().to_string().characters(),
                inode.identifier().to_string().characters());
        }
        ASSERT(&cached_custody->inode() == &inode);
        return *cached_custody;
    }
    return create(parent, name, inode);
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

void Custody::did_delete(Badge<VFS>)
{
    m_deleted = true;
}

void Custody::did_mount_on(Badge<VFS>)
{
    m_mounted_on = true;
}

void Custody::did_rename(Badge<VFS>, const String& name)
{
    m_name = name;
}
