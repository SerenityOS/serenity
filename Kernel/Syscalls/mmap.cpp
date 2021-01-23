/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PrivateInodeVMObject.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/limits.h>

namespace Kernel {

static bool validate_mmap_prot(int prot, bool map_stack)
{
    bool readable = prot & PROT_READ;
    bool writable = prot & PROT_WRITE;
    bool executable = prot & PROT_EXEC;

    if (writable && executable)
        return false;

    if (map_stack) {
        if (executable)
            return false;
        if (!readable || !writable)
            return false;
    }

    return true;
}

static bool validate_inode_mmap_prot(const Process& process, int prot, const Inode& inode, bool map_shared)
{
    auto metadata = inode.metadata();
    if ((prot & PROT_READ) && !metadata.may_read(process))
        return false;

    if (map_shared) {
        // FIXME: What about readonly filesystem mounts? We cannot make a
        // decision here without knowing the mount flags, so we would need to
        // keep a Custody or something from mmap time.
        if ((prot & PROT_WRITE) && !metadata.may_write(process))
            return false;
        InterruptDisabler disabler;
        if (auto shared_vmobject = inode.shared_vmobject()) {
            if ((prot & PROT_EXEC) && shared_vmobject->writable_mappings())
                return false;
            if ((prot & PROT_WRITE) && shared_vmobject->executable_mappings())
                return false;
        }
    }
    return true;
}

void* Process::sys$mmap(Userspace<const Syscall::SC_mmap_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_mmap_params params;
    if (!copy_from_user(&params, user_params))
        return (void*)-EFAULT;

    void* addr = (void*)params.addr;
    size_t size = params.size;
    size_t alignment = params.alignment;
    int prot = params.prot;
    int flags = params.flags;
    int fd = params.fd;
    int offset = params.offset;

    if (alignment & ~PAGE_MASK)
        return (void*)-EINVAL;

    if (!is_user_range(VirtualAddress(addr), size))
        return (void*)-EFAULT;

    String name;
    if (params.name.characters) {
        if (params.name.length > PATH_MAX)
            return (void*)-ENAMETOOLONG;
        name = copy_string_from_user(params.name);
        if (name.is_null())
            return (void*)-EFAULT;
    }

    if (size == 0)
        return (void*)-EINVAL;
    if ((FlatPtr)addr & ~PAGE_MASK)
        return (void*)-EINVAL;

    bool map_shared = flags & MAP_SHARED;
    bool map_anonymous = flags & MAP_ANONYMOUS;
    bool map_private = flags & MAP_PRIVATE;
    bool map_stack = flags & MAP_STACK;
    bool map_fixed = flags & MAP_FIXED;
    bool map_noreserve = flags & MAP_NORESERVE;

    if (map_shared && map_private)
        return (void*)-EINVAL;

    if (!map_shared && !map_private)
        return (void*)-EINVAL;

    if (!validate_mmap_prot(prot, map_stack))
        return (void*)-EINVAL;

    if (map_stack && (!map_private || !map_anonymous))
        return (void*)-EINVAL;

    Region* region = nullptr;
    Optional<Range> range;
    if (map_noreserve || map_anonymous) {
        range = allocate_range(VirtualAddress(addr), size, alignment);
        if (!range.value().is_valid())
            return (void*)-ENOMEM;
    }

    if (map_anonymous) {
        auto strategy = map_noreserve ? AllocationStrategy::None : AllocationStrategy::Reserve;
        region = allocate_region(range.value(), !name.is_null() ? name : "mmap", prot, strategy);
        if (!region && (!map_fixed && addr != 0))
            region = allocate_region(allocate_range({}, size), !name.is_null() ? name : "mmap", prot, strategy);
    } else {
        if (offset < 0)
            return (void*)-EINVAL;
        if (static_cast<size_t>(offset) & ~PAGE_MASK)
            return (void*)-EINVAL;
        auto description = file_description(fd);
        if (!description)
            return (void*)-EBADF;
        if (description->is_directory())
            return (void*)-ENODEV;
        // Require read access even when read protection is not requested.
        if (!description->is_readable())
            return (void*)-EACCES;
        if (map_shared) {
            if ((prot & PROT_WRITE) && !description->is_writable())
                return (void*)-EACCES;
        }
        if (description->inode()) {
            if (!validate_inode_mmap_prot(*this, prot, *description->inode(), map_shared))
                return (void*)-EACCES;
        }
        auto region_or_error = description->mmap(*this, VirtualAddress(addr), static_cast<size_t>(offset), size, prot, map_shared);
        if (region_or_error.is_error()) {
            // Fail if MAP_FIXED or address is 0, retry otherwise
            if (map_fixed || addr == 0)
                return (void*)(int)region_or_error.error();
            region_or_error = description->mmap(*this, {}, static_cast<size_t>(offset), size, prot, map_shared);
        }
        if (region_or_error.is_error())
            return (void*)(int)region_or_error.error();
        region = region_or_error.value();
    }

    if (!region)
        return (void*)-ENOMEM;
    region->set_mmap(true);
    if (map_shared)
        region->set_shared(true);
    if (map_stack)
        region->set_stack(true);
    if (!name.is_null())
        region->set_name(name);
    return region->vaddr().as_ptr();
}

int Process::sys$mprotect(void* addr, size_t size, int prot)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (!is_user_range(VirtualAddress(addr), size))
        return -EFAULT;

    Range range_to_mprotect = { VirtualAddress(addr), size };

    if (auto* whole_region = find_region_from_range(range_to_mprotect)) {
        if (!whole_region->is_mmap())
            return -EPERM;
        if (!validate_mmap_prot(prot, whole_region->is_stack()))
            return -EINVAL;
        if (whole_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (whole_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(whole_region->vmobject()).inode(), whole_region->is_shared())) {
            return -EACCES;
        }
        whole_region->set_readable(prot & PROT_READ);
        whole_region->set_writable(prot & PROT_WRITE);
        whole_region->set_executable(prot & PROT_EXEC);
        whole_region->remap();
        return 0;
    }

    // Check if we can carve out the desired range from an existing region
    if (auto* old_region = find_region_containing(range_to_mprotect)) {
        if (!old_region->is_mmap())
            return -EPERM;
        if (!validate_mmap_prot(prot, old_region->is_stack()))
            return -EINVAL;
        if (old_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (old_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(old_region->vmobject()).inode(), old_region->is_shared())) {
            return -EACCES;
        }

        // This vector is the region(s) adjacent to our range.
        // We need to allocate a new region for the range we wanted to change permission bits on.
        auto adjacent_regions = split_region_around_range(*old_region, range_to_mprotect);

        size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (range_to_mprotect.base().get() - old_region->range().base().get());
        auto& new_region = allocate_split_region(*old_region, range_to_mprotect, new_range_offset_in_vmobject);
        new_region.set_readable(prot & PROT_READ);
        new_region.set_writable(prot & PROT_WRITE);
        new_region.set_executable(prot & PROT_EXEC);

        // Unmap the old region here, specifying that we *don't* want the VM deallocated.
        old_region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);
        deallocate_region(*old_region);

        // Map the new regions using our page directory (they were just allocated and don't have one).
        for (auto* adjacent_region : adjacent_regions) {
            adjacent_region->map(page_directory());
        }
        new_region.map(page_directory());
        return 0;
    }

    // FIXME: We should also support mprotect() across multiple regions. (#175) (#964)

    return -EINVAL;
}

int Process::sys$madvise(void* address, size_t size, int advice)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (!is_user_range(VirtualAddress(address), size))
        return -EFAULT;

    auto* region = find_region_from_range({ VirtualAddress(address), size });
    if (!region)
        return -EINVAL;
    if (!region->is_mmap())
        return -EPERM;
    bool set_volatile = advice & MADV_SET_VOLATILE;
    bool set_nonvolatile = advice & MADV_SET_NONVOLATILE;
    if (set_volatile && set_nonvolatile)
        return -EINVAL;
    if (set_volatile || set_nonvolatile) {
        if (!region->vmobject().is_anonymous())
            return -EPERM;
        bool was_purged = false;
        switch (region->set_volatile(VirtualAddress(address), size, set_volatile, was_purged)) {
        case Region::SetVolatileError::Success:
            break;
        case Region::SetVolatileError::NotPurgeable:
            return -EPERM;
        case Region::SetVolatileError::OutOfMemory:
            return -ENOMEM;
        }
        if (set_nonvolatile)
            return was_purged ? 1 : 0;
        return 0;
    }
    if (advice & MADV_GET_VOLATILE) {
        if (!region->vmobject().is_anonymous())
            return -EPERM;
        return region->is_volatile(VirtualAddress(address), size) ? 0 : 1;
    }
    return -EINVAL;
}

int Process::sys$minherit(void* address, size_t size, int inherit)
{
    REQUIRE_PROMISE(stdio);

    auto* region = find_region_from_range({ VirtualAddress(address), size });
    if (!region)
        return -EINVAL;

    if (!region->is_mmap())
        return -EINVAL;

    if (region->is_shared())
        return -EINVAL;

    if (!region->vmobject().is_anonymous())
        return -EINVAL;

    switch (inherit) {
    case MAP_INHERIT_ZERO:
        region->set_inherit_mode(Region::InheritMode::ZeroedOnFork);
        return 0;
    }

    return -EINVAL;
}

int Process::sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_set_mmap_name_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    if (params.name.length > PATH_MAX)
        return -ENAMETOOLONG;

    auto name = copy_string_from_user(params.name);
    if (name.is_null())
        return -EFAULT;

    auto* region = find_region_from_range({ VirtualAddress(params.addr), params.size });
    if (!region)
        return -EINVAL;
    if (!region->is_mmap())
        return -EPERM;
    region->set_name(move(name));
    return 0;
}

// Carve out a virtual address range from a region and return the two regions on either side
Vector<Region*, 2> Process::split_region_around_range(const Region& source_region, const Range& desired_range)
{
    Range old_region_range = source_region.range();
    auto remaining_ranges_after_unmap = old_region_range.carve(desired_range);

    ASSERT(!remaining_ranges_after_unmap.is_empty());
    auto make_replacement_region = [&](const Range& new_range) -> Region& {
        ASSERT(old_region_range.contains(new_range));
        size_t new_range_offset_in_vmobject = source_region.offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
        return allocate_split_region(source_region, new_range, new_range_offset_in_vmobject);
    };
    Vector<Region*, 2> new_regions;
    for (auto& new_range : remaining_ranges_after_unmap) {
        new_regions.unchecked_append(&make_replacement_region(new_range));
    }
    return new_regions;
}
int Process::sys$munmap(void* addr, size_t size)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (!is_user_range(VirtualAddress(addr), size))
        return -EFAULT;

    Range range_to_unmap { VirtualAddress(addr), size };
    if (auto* whole_region = find_region_from_range(range_to_unmap)) {
        if (!whole_region->is_mmap())
            return -EPERM;
        bool success = deallocate_region(*whole_region);
        ASSERT(success);
        return 0;
    }

    if (auto* old_region = find_region_containing(range_to_unmap)) {
        if (!old_region->is_mmap())
            return -EPERM;

        auto new_regions = split_region_around_range(*old_region, range_to_unmap);

        // We manually unmap the old region here, specifying that we *don't* want the VM deallocated.
        old_region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);
        deallocate_region(*old_region);

        // Instead we give back the unwanted VM manually.
        page_directory().range_allocator().deallocate(range_to_unmap);

        // And finally we map the new region(s) using our page directory (they were just allocated and don't have one).
        for (auto* new_region : new_regions) {
            new_region->map(page_directory());
        }
        return 0;
    }

    // FIXME: We should also support munmap() across multiple regions. (#175)

    return -EINVAL;
}

void* Process::sys$mremap(Userspace<const Syscall::SC_mremap_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_mremap_params params;
    if (!copy_from_user(&params, user_params))
        return (void*)-EFAULT;

    auto* old_region = find_region_from_range(Range { VirtualAddress(params.old_address), params.old_size });
    if (!old_region)
        return (void*)-EINVAL;

    if (!old_region->is_mmap())
        return (void*)-EPERM;

    if (old_region->vmobject().is_shared_inode() && params.flags & MAP_PRIVATE && !(params.flags & (MAP_ANONYMOUS | MAP_NORESERVE))) {
        auto range = old_region->range();
        auto old_name = old_region->name();
        auto old_prot = region_access_flags_to_prot(old_region->access());
        NonnullRefPtr inode = static_cast<SharedInodeVMObject&>(old_region->vmobject()).inode();
        deallocate_region(*old_region);

        auto new_vmobject = PrivateInodeVMObject::create_with_inode(inode);
        auto* new_region = allocate_region_with_vmobject(range.base(), range.size(), new_vmobject, 0, old_name, old_prot, false);
        new_region->set_mmap(true);

        if (!new_region)
            return (void*)-ENOMEM;

        return new_region->vaddr().as_ptr();
    }

    dbgln("sys$mremap: Unimplemented remap request (flags={})", params.flags);
    return (void*)-ENOTIMPL;
}

void* Process::sys$allocate_tls(size_t size)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return (void*)-EINVAL;

    if (!m_master_tls_region.is_null())
        return (void*)-EEXIST;

    if (thread_count() != 1)
        return (void*)-EFAULT;

    Thread* main_thread = nullptr;
    for_each_thread([&main_thread](auto& thread) {
        main_thread = &thread;
        return IterationDecision::Break;
    });
    ASSERT(main_thread);

    auto tls_region = allocate_region({}, size, String(), PROT_READ | PROT_WRITE);
    if (!tls_region)
        return (void*)-EFAULT;

    m_master_tls_region = tls_region->make_weak_ptr();
    m_master_tls_size = size;
    m_master_tls_alignment = PAGE_SIZE;

    auto tsr_result = main_thread->make_thread_specific_region({});
    if (tsr_result.is_error())
        return (void*)-EFAULT;

    auto& tls_descriptor = Processor::current().get_gdt_entry(GDT_SELECTOR_TLS);
    tls_descriptor.set_base(main_thread->thread_specific_data().as_ptr());
    tls_descriptor.set_limit(main_thread->thread_specific_region_size());

    return m_master_tls_region.unsafe_ptr()->vaddr().as_ptr();
}

}
