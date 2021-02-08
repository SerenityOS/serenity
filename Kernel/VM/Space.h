/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/WeakPtr.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/PageDirectory.h>

namespace Kernel {

class Space {
public:
    static OwnPtr<Space> create(Process&, const Space* parent);
    ~Space();

    PageDirectory& page_directory() { return *m_page_directory; }
    const PageDirectory& page_directory() const { return *m_page_directory; }

    Region& add_region(NonnullOwnPtr<Region>);

    size_t region_count() const { return m_regions.size(); }

    NonnullOwnPtrVector<Region>& regions() { return m_regions; }
    const NonnullOwnPtrVector<Region>& regions() const { return m_regions; }

    void dump_regions();

    Optional<Range> allocate_range(VirtualAddress, size_t, size_t alignment = PAGE_SIZE);

    KResultOr<Region*> allocate_region_with_vmobject(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String& name, int prot, bool shared);
    KResultOr<Region*> allocate_region(const Range&, const String& name, int prot = PROT_READ | PROT_WRITE, AllocationStrategy strategy = AllocationStrategy::Reserve);
    bool deallocate_region(Region& region);

    Region& allocate_split_region(const Region& source_region, const Range&, size_t offset_in_vmobject);
    Vector<Region*, 2> split_region_around_range(const Region& source_region, const Range&);

    Region* find_region_from_range(const Range&);
    Region* find_region_containing(const Range&);

    bool enforces_syscall_regions() const { return m_enforces_syscall_regions; }
    void set_enforces_syscall_regions(bool b) { m_enforces_syscall_regions = b; }

    void remove_all_regions(Badge<Process>);

    SpinLock<u32>& get_lock() const { return m_lock; }

private:
    Space(Process&, NonnullRefPtr<PageDirectory>);

    Process* m_process { nullptr };
    mutable SpinLock<u32> m_lock;

    RefPtr<PageDirectory> m_page_directory;

    NonnullOwnPtrVector<Region> m_regions;

    struct RegionLookupCache {
        Optional<Range> range;
        WeakPtr<Region> region;
    };
    RegionLookupCache m_region_lookup_cache;

    bool m_enforces_syscall_regions { false };
};

}
