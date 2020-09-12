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

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PurgeableVMObject.h>
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
        if (inode.shared_vmobject()) {
            if ((prot & PROT_EXEC) && inode.shared_vmobject()->writable_mappings())
                return false;
            if ((prot & PROT_WRITE) && inode.shared_vmobject()->executable_mappings())
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
    bool map_purgeable = flags & MAP_PURGEABLE;
    bool map_private = flags & MAP_PRIVATE;
    bool map_stack = flags & MAP_STACK;
    bool map_fixed = flags & MAP_FIXED;

    if (map_shared && map_private)
        return (void*)-EINVAL;

    if (!map_shared && !map_private)
        return (void*)-EINVAL;

    if (!validate_mmap_prot(prot, map_stack))
        return (void*)-EINVAL;

    if (map_stack && (!map_private || !map_anonymous))
        return (void*)-EINVAL;

    Region* region = nullptr;

    auto range = allocate_range(VirtualAddress(addr), size, alignment);
    if (!range.is_valid())
        return (void*)-ENOMEM;

    if (map_purgeable) {
        auto vmobject = PurgeableVMObject::create_with_size(size);
        region = allocate_region_with_vmobject(range, vmobject, 0, !name.is_null() ? name : "mmap (purgeable)", prot);
        if (!region && (!map_fixed && addr != 0))
            region = allocate_region_with_vmobject({}, size, vmobject, 0, !name.is_null() ? name : "mmap (purgeable)", prot);
    } else if (map_anonymous) {
        region = allocate_region(range, !name.is_null() ? name : "mmap", prot, false);
        if (!region && (!map_fixed && addr != 0))
            region = allocate_region(allocate_range({}, size), !name.is_null() ? name : "mmap", prot, false);
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
    if ((advice & MADV_SET_VOLATILE) && (advice & MADV_SET_NONVOLATILE))
        return -EINVAL;
    if (advice & MADV_SET_VOLATILE) {
        if (!region->vmobject().is_purgeable())
            return -EPERM;
        auto& vmobject = static_cast<PurgeableVMObject&>(region->vmobject());
        vmobject.set_volatile(true);
        return 0;
    }
    if (advice & MADV_SET_NONVOLATILE) {
        if (!region->vmobject().is_purgeable())
            return -EPERM;
        auto& vmobject = static_cast<PurgeableVMObject&>(region->vmobject());
        if (!vmobject.is_volatile())
            return 0;
        vmobject.set_volatile(false);
        bool was_purged = vmobject.was_purged();
        vmobject.set_was_purged(false);
        return was_purged ? 1 : 0;
    }
    if (advice & MADV_GET_VOLATILE) {
        if (!region->vmobject().is_purgeable())
            return -EPERM;
        auto& vmobject = static_cast<PurgeableVMObject&>(region->vmobject());
        return vmobject.is_volatile() ? 0 : 1;
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

}
