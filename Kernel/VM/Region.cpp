#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

Region::Region(const Range& range, const String& name, u8 access, bool cow)
    : m_range(range)
    , m_vmo(VMObject::create_anonymous(size()))
    , m_name(name)
    , m_access(access)
    , m_cow_map(Bitmap::create(m_vmo->page_count(), cow))
{
    m_vmo->set_name(m_name);
    MM.register_region(*this);
}

Region::Region(const Range& range, RefPtr<Inode>&& inode, const String& name, u8 access, bool cow)
    : m_range(range)
    , m_vmo(VMObject::create_file_backed(move(inode)))
    , m_name(name)
    , m_access(access)
    , m_cow_map(Bitmap::create(m_vmo->page_count(), cow))
{
    MM.register_region(*this);
}

Region::Region(const Range& range, NonnullRefPtr<VMObject> vmo, size_t offset_in_vmo, const String& name, u8 access, bool cow)
    : m_range(range)
    , m_offset_in_vmo(offset_in_vmo)
    , m_vmo(move(vmo))
    , m_name(name)
    , m_access(access)
    , m_cow_map(Bitmap::create(m_vmo->page_count(), cow))
{
    MM.register_region(*this);
}

Region::~Region()
{
    if (m_page_directory) {
        MM.unmap_region(*this);
        ASSERT(!m_page_directory);
    }
    MM.unregister_region(*this);
}

bool Region::page_in()
{
    ASSERT(m_page_directory);
    ASSERT(!vmo().is_anonymous());
    ASSERT(vmo().inode());
#ifdef MM_DEBUG
    dbgprintf("MM: page_in %u pages\n", page_count());
#endif
    for (size_t i = 0; i < page_count(); ++i) {
        auto& vmo_page = vmo().physical_pages()[first_page_index() + i];
        if (vmo_page.is_null()) {
            bool success = MM.page_in_from_inode(*this, i);
            if (!success)
                return false;
            continue;
        }
        MM.remap_region_page(*this, i);
    }
    return true;
}

NonnullRefPtr<Region> Region::clone()
{
    ASSERT(current);

    // NOTE: Kernel-only regions should never be cloned.
    ASSERT(is_user_accessible());

    if (m_shared || (is_readable() && !is_writable())) {
#ifdef MM_DEBUG
        dbgprintf("%s<%u> Region::clone(): sharing %s (L%x)\n",
            current->process().name().characters(),
            current->pid(),
            m_name.characters(),
            vaddr().get());
#endif
        // Create a new region backed by the same VMObject.
        return Region::create_user_accessible(m_range, m_vmo, m_offset_in_vmo, m_name, m_access);
    }

#ifdef MM_DEBUG
    dbgprintf("%s<%u> Region::clone(): cowing %s (L%x)\n",
        current->process().name().characters(),
        current->pid(),
        m_name.characters(),
        vaddr().get());
#endif
    // Set up a COW region. The parent (this) region becomes COW as well!
    m_cow_map.fill(true);
    MM.remap_region(current->process().page_directory(), *this);
    return Region::create_user_accessible(m_range, m_vmo->clone(), m_offset_in_vmo, m_name, m_access, true);
}

int Region::commit()
{
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: commit %u pages in Region %p (VMO=%p) at L%x\n", vmo().page_count(), this, &vmo(), vaddr().get());
#endif
    for (size_t i = first_page_index(); i <= last_page_index(); ++i) {
        if (!vmo().physical_pages()[i].is_null())
            continue;
        auto physical_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
        if (!physical_page) {
            kprintf("MM: commit was unable to allocate a physical page\n");
            return -ENOMEM;
        }
        vmo().physical_pages()[i] = move(physical_page);
        MM.remap_region_page(*this, i);
    }
    return 0;
}

size_t Region::amount_resident() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        if (m_vmo->physical_pages()[first_page_index() + i])
            bytes += PAGE_SIZE;
    }
    return bytes;
}

size_t Region::amount_shared() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto& physical_page = m_vmo->physical_pages()[first_page_index() + i];
        if (physical_page && physical_page->ref_count() > 1)
            bytes += PAGE_SIZE;
    }
    return bytes;
}

NonnullRefPtr<Region> Region::create_user_accessible(const Range& range, const StringView& name, u8 access, bool cow)
{
    auto region = adopt(*new Region(range, name, access, cow));
    region->m_user_accessible = true;
    return region;
}

NonnullRefPtr<Region> Region::create_user_accessible(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const StringView& name, u8 access, bool cow)
{
    auto region = adopt(*new Region(range, move(vmobject), offset_in_vmobject, name, access, cow));
    region->m_user_accessible = true;
    return region;
}

NonnullRefPtr<Region> Region::create_user_accessible(const Range& range, NonnullRefPtr<Inode> inode, const StringView& name, u8 access, bool cow)
{
    auto region = adopt(*new Region(range, move(inode), name, access, cow));
    region->m_user_accessible = true;
    return region;
}

NonnullRefPtr<Region> Region::create_kernel_only(const Range& range, const StringView& name, u8 access, bool cow)
{
    auto region = adopt(*new Region(range, name, access, cow));
    region->m_user_accessible = false;
    return region;
}
