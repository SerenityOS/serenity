#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

NonnullRefPtr<InodeVMObject> InodeVMObject::create_with_inode(Inode& inode)
{
    InterruptDisabler disabler;
    if (inode.vmobject())
        return *inode.vmobject();
    auto vmobject = adopt(*new InodeVMObject(inode));
    vmobject->inode().set_vmobject(*vmobject);
    return vmobject;
}

NonnullRefPtr<VMObject> InodeVMObject::clone()
{
    return adopt(*new InodeVMObject(*this));
}

InodeVMObject::InodeVMObject(Inode& inode)
    : VMObject(inode.size())
    , m_inode(inode)
    , m_dirty_pages(page_count(), false)
{
}

InodeVMObject::InodeVMObject(const InodeVMObject& other)
    : VMObject(other)
    , m_inode(other.m_inode)
{
}

InodeVMObject::~InodeVMObject()
{
    ASSERT(inode().vmobject() == this);
}

size_t InodeVMObject::amount_dirty() const
{
    size_t count = 0;
    for (int i = 0; i < m_dirty_pages.size(); ++i) {
        if (m_dirty_pages.get(i))
            ++count;
    }
    return count * PAGE_SIZE;
}

void InodeVMObject::inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size)
{
    dbgprintf("VMObject::inode_size_changed: {%u:%u} %u -> %u\n",
        m_inode->fsid(), m_inode->index(),
        old_size, new_size);

    InterruptDisabler disabler;

    auto new_page_count = PAGE_ROUND_UP(new_size) / PAGE_SIZE;
    m_physical_pages.resize(new_page_count);

    m_dirty_pages.grow(new_page_count, false);

    // FIXME: Consolidate with inode_contents_changed() so we only do a single walk.
    for_each_region([](auto& region) {
        region.remap();
    });
}

void InodeVMObject::inode_contents_changed(Badge<Inode>, off_t offset, ssize_t size, const u8* data)
{
    (void)size;
    (void)data;
    InterruptDisabler disabler;
    ASSERT(offset >= 0);

    // FIXME: Only invalidate the parts that actually changed.
    for (auto& physical_page : m_physical_pages)
        physical_page = nullptr;

#if 0
    size_t current_offset = offset;
    size_t remaining_bytes = size;
    const u8* data_ptr = data;

    auto to_page_index = [] (size_t offset) -> size_t {
        return offset / PAGE_SIZE;
    };

    if (current_offset & PAGE_MASK) {
        size_t page_index = to_page_index(current_offset);
        size_t bytes_to_copy = min(size, PAGE_SIZE - (current_offset & PAGE_MASK));
        if (m_physical_pages[page_index]) {
            auto* ptr = MM.quickmap_page(*m_physical_pages[page_index]);
            memcpy(ptr, data_ptr, bytes_to_copy);
            MM.unquickmap_page();
        }
        current_offset += bytes_to_copy;
        data += bytes_to_copy;
        remaining_bytes -= bytes_to_copy;
    }

    for (size_t page_index = to_page_index(current_offset); page_index < m_physical_pages.size(); ++page_index) {
        size_t bytes_to_copy = PAGE_SIZE - (current_offset & PAGE_MASK);
        if (m_physical_pages[page_index]) {
            auto* ptr = MM.quickmap_page(*m_physical_pages[page_index]);
            memcpy(ptr, data_ptr, bytes_to_copy);
            MM.unquickmap_page();
        }
        current_offset += bytes_to_copy;
        data += bytes_to_copy;
    }
#endif

    // FIXME: Consolidate with inode_size_changed() so we only do a single walk.
    for_each_region([](auto& region) {
        region.remap();
    });
}
