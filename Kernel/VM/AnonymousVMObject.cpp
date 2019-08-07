#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalPage.h>

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_with_size(size_t size)
{
    size = ceil_div(size, PAGE_SIZE) * PAGE_SIZE;
    return adopt(*new AnonymousVMObject(size));
}

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    size = ceil_div(size, PAGE_SIZE) * PAGE_SIZE;
    return adopt(*new AnonymousVMObject(paddr, size));
}

AnonymousVMObject::AnonymousVMObject(size_t size)
    : VMObject(size, ShouldFillPhysicalPages::Yes)
{
}

AnonymousVMObject::AnonymousVMObject(PhysicalAddress paddr, size_t size)
    : VMObject(size, ShouldFillPhysicalPages::No)
{
    for (size_t i = 0; i < size; i += PAGE_SIZE)
        m_physical_pages.append(PhysicalPage::create(paddr.offset(i), false, false));
    ASSERT(m_physical_pages.size() == page_count());
}

AnonymousVMObject::AnonymousVMObject(const AnonymousVMObject& other)
    : VMObject(other)
{
}

AnonymousVMObject::~AnonymousVMObject()
{
}

NonnullRefPtr<VMObject> AnonymousVMObject::clone()
{
    return adopt(*new AnonymousVMObject(*this));
}
