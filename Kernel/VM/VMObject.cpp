#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/VMObject.h>

VMObject::VMObject(const VMObject& other)
    : m_size(other.m_size)
    , m_physical_pages(other.m_physical_pages)
{
    MM.register_vmo(*this);
}

VMObject::VMObject(size_t size, ShouldFillPhysicalPages should_fill_physical_pages)
    : m_size(size)
{
    MM.register_vmo(*this);
    if (should_fill_physical_pages == ShouldFillPhysicalPages::Yes)
        m_physical_pages.resize(page_count());
}

VMObject::~VMObject()
{
    MM.unregister_vmo(*this);
}
