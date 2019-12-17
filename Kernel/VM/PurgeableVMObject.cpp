#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PurgeableVMObject.h>

NonnullRefPtr<PurgeableVMObject> PurgeableVMObject::create_with_size(size_t size)
{
    return adopt(*new PurgeableVMObject(size));
}

PurgeableVMObject::PurgeableVMObject(size_t size)
    : AnonymousVMObject(size)
{
}

PurgeableVMObject::PurgeableVMObject(const PurgeableVMObject& other)
    : AnonymousVMObject(other)
{
}

PurgeableVMObject::~PurgeableVMObject()
{
}

NonnullRefPtr<VMObject> PurgeableVMObject::clone()
{
    return adopt(*new PurgeableVMObject(*this));
}

int PurgeableVMObject::purge()
{
    LOCKER(m_paging_lock);

    if (!m_volatile)
        return 0;
    m_was_purged = true;

    return do_purge();
}
