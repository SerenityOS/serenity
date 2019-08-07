#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

NonnullRefPtr<InodeVMObject> InodeVMObject::create_with_inode(Inode& inode)
{
    InterruptDisabler disabler;
    if (inode.vmo())
        return *inode.vmo();
    auto vmo = adopt(*new InodeVMObject(inode));
    vmo->inode().set_vmo(*vmo);
    return vmo;
}

NonnullRefPtr<VMObject> InodeVMObject::clone()
{
    return adopt(*new InodeVMObject(*this));
}

InodeVMObject::InodeVMObject(Inode& inode)
    : VMObject(ceil_div(inode.size(), PAGE_SIZE) * PAGE_SIZE, ShouldFillPhysicalPages::Yes)
    , m_inode(inode)
{
}

InodeVMObject::InodeVMObject(const InodeVMObject& other)
    : VMObject(other)
    , m_inode(other.m_inode)
{
}

InodeVMObject::~InodeVMObject()
{
    ASSERT(inode().vmo() == this);
}

void InodeVMObject::inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size)
{
    dbgprintf("VMObject::inode_size_changed: {%u:%u} %u -> %u\n",
        m_inode->fsid(), m_inode->index(),
        old_size, new_size);

    InterruptDisabler disabler;

    auto old_page_count = page_count();
    m_size = new_size;

    if (page_count() > old_page_count) {
        // Add null pages and let the fault handler page these in when that day comes.
        for (auto i = old_page_count; i < page_count(); ++i)
            m_physical_pages.append(nullptr);
    } else {
        // Prune the no-longer valid pages. I'm not sure this is actually correct behavior.
        for (auto i = page_count(); i < old_page_count; ++i)
            m_physical_pages.take_last();
    }

    // FIXME: Consolidate with inode_contents_changed() so we only do a single walk.
    for_each_region([](Region& region) {
        ASSERT(region.page_directory());
        MM.remap_region(*region.page_directory(), region);
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
    for_each_region([](Region& region) {
        ASSERT(region.page_directory());
        MM.remap_region(*region.page_directory(), region);
    });
}

template<typename Callback>
void VMObject::for_each_region(Callback callback)
{
    // FIXME: Figure out a better data structure so we don't have to walk every single region every time an inode changes.
    //        Perhaps VMObject could have a Vector<Region*> with all of his mappers?
    for (auto* region : MM.m_user_regions) {
        if (&region->vmo() == this)
            callback(*region);
    }
    for (auto* region : MM.m_kernel_regions) {
        if (&region->vmo() == this)
            callback(*region);
    }
}
