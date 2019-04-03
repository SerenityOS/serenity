#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

Region::Region(LinearAddress a, size_t s, String&& n, bool r, bool w, bool cow)
    : m_laddr(a)
    , m_size(s)
    , m_vmo(VMObject::create_anonymous(s))
    , m_name(move(n))
    , m_readable(r)
    , m_writable(w)
    , m_cow_map(Bitmap::create(m_vmo->page_count(), cow))
{
    m_vmo->set_name(m_name);
    MM.register_region(*this);
}

Region::Region(LinearAddress a, size_t s, RetainPtr<Inode>&& inode, String&& n, bool r, bool w)
    : m_laddr(a)
    , m_size(s)
    , m_vmo(VMObject::create_file_backed(move(inode)))
    , m_name(move(n))
    , m_readable(r)
    , m_writable(w)
    , m_cow_map(Bitmap::create(m_vmo->page_count()))
{
    MM.register_region(*this);
}

Region::Region(LinearAddress a, size_t s, Retained<VMObject>&& vmo, size_t offset_in_vmo, String&& n, bool r, bool w, bool cow)
    : m_laddr(a)
    , m_size(s)
    , m_offset_in_vmo(offset_in_vmo)
    , m_vmo(move(vmo))
    , m_name(move(n))
    , m_readable(r)
    , m_writable(w)
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
        }
        MM.remap_region_page(*this, i, true);
    }
    return true;
}

Retained<Region> Region::clone()
{
    ASSERT(current);
    if (m_shared || (m_readable && !m_writable)) {
#ifdef MM_DEBUG
        dbgprintf("%s<%u> Region::clone(): sharing %s (L%x)\n",
                  current->process().name().characters(),
                  current->pid(),
                  m_name.characters(),
                  laddr().get());
#endif
        // Create a new region backed by the same VMObject.
        return adopt(*new Region(laddr(), size(), m_vmo.copy_ref(), m_offset_in_vmo, String(m_name), m_readable, m_writable));
    }

#ifdef MM_DEBUG
    dbgprintf("%s<%u> Region::clone(): cowing %s (L%x)\n",
              current->process().name().characters(),
              current->pid(),
              m_name.characters(),
              laddr().get());
#endif
    // Set up a COW region. The parent (this) region becomes COW as well!
    for (size_t i = 0; i < page_count(); ++i)
        m_cow_map.set(i, true);
    MM.remap_region(current->process().page_directory(), *this);
    return adopt(*new Region(laddr(), size(), m_vmo->clone(), m_offset_in_vmo, String(m_name), m_readable, m_writable, true));
}

int Region::commit()
{
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: commit %u pages in Region %p (VMO=%p) at L%x\n", vmo().page_count(), this, &vmo(), laddr().get());
#endif
    for (size_t i = first_page_index(); i <= last_page_index(); ++i) {
        if (!vmo().physical_pages()[i].is_null())
            continue;
        auto physical_page = MM.allocate_physical_page(MemoryManager::ShouldZeroFill::Yes);
        if (!physical_page) {
            kprintf("MM: commit was unable to allocate a physical page\n");
            return -ENOMEM;
        }
        vmo().physical_pages()[i] = move(physical_page);
        MM.remap_region_page(*this, i, true);
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
        if (physical_page && physical_page->retain_count() > 1)
            bytes += PAGE_SIZE;
    }
    return bytes;
}
