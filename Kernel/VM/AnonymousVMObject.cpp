#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalPage.h>

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_with_size(size_t size)
{
    return adopt(*new AnonymousVMObject(size));
}

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    return adopt(*new AnonymousVMObject(paddr, size));
}

AnonymousVMObject::AnonymousVMObject(size_t size)
    : VMObject(size)
{
}

AnonymousVMObject::AnonymousVMObject(PhysicalAddress paddr, size_t size)
    : VMObject(size)
{
    ASSERT(paddr.page_base() == paddr.get());
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = PhysicalPage::create(paddr.offset(i * PAGE_SIZE), false, false);
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
