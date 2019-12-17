#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/VMObject.h>

VMObject::VMObject(const VMObject& other)
    : m_physical_pages(other.m_physical_pages)
{
    MM.register_vmo(*this);
}

VMObject::VMObject(size_t size)
    : m_physical_pages(ceil_div(size, PAGE_SIZE))
{
    MM.register_vmo(*this);
}

VMObject::~VMObject()
{
    MM.unregister_vmo(*this);
}

int VMObject::do_purge()
{
    LOCKER(m_paging_lock);

    int purged_page_count = 0;
    for (size_t i = 0; i < m_physical_pages.size(); ++i) {
        if (m_physical_pages[i])
            ++purged_page_count;
        m_physical_pages[i] = nullptr;
    }

    for_each_region([&](auto& region) {
        region.remap();
    });

    return purged_page_count;
}
