#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/VMObject.h>

VMObject::VMObject(const VMObject& other)
    : m_physical_pages(other.m_physical_pages)
{
    MM.register_vmobject(*this);
}

VMObject::VMObject(size_t size)
    : m_physical_pages(ceil_div(size, PAGE_SIZE))
{
    MM.register_vmobject(*this);
}

VMObject::~VMObject()
{
    MM.unregister_vmobject(*this);
}
