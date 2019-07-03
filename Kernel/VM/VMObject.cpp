#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/VMObject.h>

NonnullRefPtr<VMObject> VMObject::create_file_backed(RefPtr<Inode>&& inode)
{
    InterruptDisabler disabler;
    if (inode->vmo())
        return *inode->vmo();
    auto vmo = adopt(*new VMObject(move(inode)));
    vmo->inode()->set_vmo(*vmo);
    return vmo;
}

NonnullRefPtr<VMObject> VMObject::create_anonymous(size_t size)
{
    size = ceil_div(size, PAGE_SIZE) * PAGE_SIZE;
    return adopt(*new VMObject(size));
}

NonnullRefPtr<VMObject> VMObject::create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    size = ceil_div(size, PAGE_SIZE) * PAGE_SIZE;
    auto vmo = adopt(*new VMObject(paddr, size));
    vmo->m_allow_cpu_caching = false;
    return vmo;
}

NonnullRefPtr<VMObject> VMObject::clone()
{
    return adopt(*new VMObject(*this));
}

VMObject::VMObject(VMObject& other)
    : m_name(other.m_name)
    , m_inode_offset(other.m_inode_offset)
    , m_size(other.m_size)
    , m_inode(other.m_inode)
    , m_physical_pages(other.m_physical_pages)
{
    MM.register_vmo(*this);
}

VMObject::VMObject(size_t size)
    : m_size(size)
{
    MM.register_vmo(*this);
    m_physical_pages.resize(page_count());
}

VMObject::VMObject(PhysicalAddress paddr, size_t size)
    : m_size(size)
{
    MM.register_vmo(*this);
    for (size_t i = 0; i < size; i += PAGE_SIZE) {
        m_physical_pages.append(PhysicalPage::create(paddr.offset(i), false, false));
    }
    ASSERT(m_physical_pages.size() == page_count());
}

VMObject::VMObject(RefPtr<Inode>&& inode)
    : m_inode(move(inode))
{
    ASSERT(m_inode);
    m_size = ceil_div(m_inode->size(), PAGE_SIZE) * PAGE_SIZE;
    m_physical_pages.resize(page_count());
    MM.register_vmo(*this);
}

VMObject::~VMObject()
{
    if (m_inode)
        ASSERT(m_inode->vmo() == this);
    MM.unregister_vmo(*this);
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

void VMObject::inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size)
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

void VMObject::inode_contents_changed(Badge<Inode>, off_t offset, ssize_t size, const u8* data)
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
