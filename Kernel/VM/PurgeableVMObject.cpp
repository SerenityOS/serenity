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
    return purge_impl();
}

int PurgeableVMObject::purge_with_interrupts_disabled(Badge<MemoryManager>)
{
    ASSERT_INTERRUPTS_DISABLED();
    if (m_paging_lock.is_locked())
        return 0;
    return purge_impl();
}

int PurgeableVMObject::purge_impl()
{
    if (!m_volatile)
        return 0;
    int purged_page_count = 0;
    for (size_t i = 0; i < m_physical_pages.size(); ++i) {
        if (m_physical_pages[i])
            ++purged_page_count;
        m_physical_pages[i] = nullptr;
    }
    m_was_purged = true;

    for_each_region([&](auto& region) {
        region.remap();
    });

    return purged_page_count;
}
