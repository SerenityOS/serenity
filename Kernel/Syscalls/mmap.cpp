/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/VirtualMemoryAnnotations.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <LibELF/Validation.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/MSR.h>
#endif

namespace Kernel {

ErrorOr<void> Process::validate_mmap_prot(int prot, bool map_stack, bool map_anonymous, Memory::Region const* region) const
{
    bool make_writable = prot & PROT_WRITE;
    bool make_executable = prot & PROT_EXEC;

    if (map_anonymous && make_executable && !(executable()->mount_flags() & MS_AXALLOWED))
        return EINVAL;

    if (map_stack && make_executable)
        return EINVAL;

    if (executable()->mount_flags() & MS_WXALLOWED)
        return {};

    if (make_writable && make_executable)
        return EINVAL;

    if (region) {
        if (make_writable && region->has_been_executable())
            return EINVAL;

        if (make_executable && region->has_been_writable() && should_reject_transition_to_executable_from_writable_prot())
            return EINVAL;
    }

    return {};
}

ErrorOr<void> Process::validate_inode_mmap_prot(int prot, bool readable_description, bool description_writable, bool map_shared) const
{
    if ((prot & PROT_READ) && !readable_description)
        return EACCES;

    if (map_shared) {
        // FIXME: What about readonly filesystem mounts? We cannot make a
        // decision here without knowing the mount flags, so we would need to
        // keep a Custody or something from mmap time.
        if ((prot & PROT_WRITE) && !description_writable)
            return EACCES;
    }
    return {};
}

ErrorOr<FlatPtr> Process::sys$mmap(Userspace<Syscall::SC_mmap_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto params = TRY(copy_typed_from_user(user_params));

    auto addr = (FlatPtr)params.addr;
    auto size = params.size;
    auto alignment = params.alignment ? params.alignment : PAGE_SIZE;
    auto prot = params.prot;
    auto flags = params.flags;
    auto fd = params.fd;
    auto offset = params.offset;

    if (prot & PROT_EXEC) {
        TRY(require_promise(Pledge::prot_exec));
    }

    if (flags & MAP_FIXED || flags & MAP_FIXED_NOREPLACE) {
        TRY(require_promise(Pledge::map_fixed));
    }

    if (alignment & ~PAGE_MASK)
        return EINVAL;

    size_t rounded_size = TRY(Memory::page_round_up(size));
    if (!Memory::is_user_range(VirtualAddress(addr), rounded_size))
        return EFAULT;

    OwnPtr<KString> name;
    if (params.name.characters) {
        if (params.name.length > PATH_MAX)
            return ENAMETOOLONG;
        name = TRY(try_copy_kstring_from_user(params.name));
    }

    if (size == 0)
        return EINVAL;
    if ((FlatPtr)addr & ~PAGE_MASK)
        return EINVAL;

    bool map_shared = flags & MAP_SHARED;
    bool map_anonymous = flags & MAP_ANONYMOUS;
    bool map_private = flags & MAP_PRIVATE;
    bool map_stack = flags & MAP_STACK;
    bool map_fixed = flags & MAP_FIXED;
    bool map_noreserve = flags & MAP_NORESERVE;
    bool map_randomized = flags & MAP_RANDOMIZED;
    bool map_fixed_noreplace = flags & MAP_FIXED_NOREPLACE;

    if (map_shared && map_private)
        return EINVAL;

    if (!map_shared && !map_private)
        return EINVAL;

    if ((map_fixed || map_fixed_noreplace) && map_randomized)
        return EINVAL;

    TRY(validate_mmap_prot(prot, map_stack, map_anonymous));

    if (map_stack && (!map_private || !map_anonymous))
        return EINVAL;

    Memory::VirtualRange requested_range { VirtualAddress { addr }, rounded_size };
    if (addr && !(map_fixed || map_fixed_noreplace)) {
        // If there's an address but MAP_FIXED wasn't specified, the address is just a hint.
        requested_range = { {}, rounded_size };
    }

    Memory::Region* region = nullptr;

    RefPtr<OpenFileDescription> description;
    LockRefPtr<Memory::VMObject> vmobject;
    u64 used_offset = 0;
    auto memory_type = Memory::MemoryType::Normal;

    if (map_anonymous) {
        auto strategy = map_noreserve ? AllocationStrategy::None : AllocationStrategy::Reserve;

        if (flags & MAP_PURGEABLE) {
            vmobject = TRY(Memory::AnonymousVMObject::try_create_purgeable_with_size(rounded_size, strategy));
        } else {
            vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(rounded_size, strategy));
        }
    } else {
        if (offset < 0)
            return EINVAL;
        used_offset = static_cast<u64>(offset);
        if (static_cast<size_t>(offset) & ~PAGE_MASK)
            return EINVAL;
        description = TRY(open_file_description(fd));
        if (description->is_directory())
            return ENODEV;
        // Require read access even when read protection is not requested.
        if (!description->is_readable())
            return EACCES;
        if (map_shared) {
            if ((prot & PROT_WRITE) && !description->is_writable())
                return EACCES;
        }
        if (description->inode())
            TRY(validate_inode_mmap_prot(prot, description->is_readable(), description->is_writable(), map_shared));

        auto vmobject_and_memory_type = TRY(description->vmobject_for_mmap(*this, requested_range, used_offset, map_shared));
        vmobject = vmobject_and_memory_type.vmobject;
        memory_type = vmobject_and_memory_type.memory_type;
    }

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        // If MAP_FIXED is specified, existing mappings that intersect the requested range are removed.
        if (map_fixed)
            TRY(space->unmap_mmap_range(VirtualAddress(addr), size));

        region = TRY(space->allocate_region_with_vmobject(
            map_randomized ? Memory::RandomizeVirtualAddress::Yes : Memory::RandomizeVirtualAddress::No,
            requested_range.base(),
            requested_range.size(),
            alignment,
            vmobject.release_nonnull(),
            used_offset,
            {},
            prot,
            map_shared,
            memory_type));

        if (!region)
            return ENOMEM;

        if (description)
            region->set_mmap(true, description->is_readable(), description->is_writable());
        else
            region->set_mmap(true, false, false);

        if (map_shared)
            region->set_shared(true);
        if (map_stack)
            region->set_stack(true);
        if (name)
            region->set_name(move(name));

        PerformanceManager::add_mmap_perf_event(*this, *region);

        return region->vaddr().get();
    });
}

ErrorOr<FlatPtr> Process::sys$mprotect(Userspace<void*> addr, size_t size, int prot)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    if (prot & PROT_EXEC) {
        TRY(require_promise(Pledge::prot_exec));
    }

    auto range_to_mprotect = TRY(Memory::expand_range_to_page_boundaries(addr.ptr(), size));
    if (!range_to_mprotect.size())
        return EINVAL;

    if (!is_user_range(range_to_mprotect))
        return EFAULT;

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        if (auto* whole_region = space->find_region_from_range(range_to_mprotect)) {
            if (!whole_region->is_mmap())
                return EPERM;
            if (whole_region->is_immutable())
                return EPERM;
            TRY(validate_mmap_prot(prot, whole_region->is_stack(), whole_region->vmobject().is_anonymous(), whole_region));
            if (whole_region->access() == Memory::prot_to_region_access_flags(prot))
                return 0;
            if (whole_region->vmobject().is_inode())
                TRY(validate_inode_mmap_prot(prot, whole_region->mmapped_from_readable(), whole_region->mmapped_from_writable(), whole_region->is_shared()));
            whole_region->set_readable(prot & PROT_READ);
            whole_region->set_writable(prot & PROT_WRITE);
            whole_region->set_executable(prot & PROT_EXEC);

            whole_region->remap();
            return 0;
        }

        // Check if we can carve out the desired range from an existing region
        if (auto* old_region = space->find_region_containing(range_to_mprotect)) {
            if (!old_region->is_mmap())
                return EPERM;
            if (old_region->is_immutable())
                return EPERM;
            TRY(validate_mmap_prot(prot, old_region->is_stack(), old_region->vmobject().is_anonymous(), old_region));
            if (old_region->access() == Memory::prot_to_region_access_flags(prot))
                return 0;
            if (old_region->vmobject().is_inode())
                TRY(validate_inode_mmap_prot(prot, old_region->mmapped_from_readable(), old_region->mmapped_from_writable(), old_region->is_shared()));

            // Remove the old region from our regions tree, since were going to add another region
            // with the exact same start address.
            auto region = space->take_region(*old_region);
            region->unmap();

            // This vector is the region(s) adjacent to our range.
            // We need to allocate a new region for the range we wanted to change permission bits on.
            auto adjacent_regions = TRY(space->try_split_region_around_range(*region, range_to_mprotect));

            size_t new_range_offset_in_vmobject = region->offset_in_vmobject() + (range_to_mprotect.base().get() - region->range().base().get());
            auto* new_region = TRY(space->try_allocate_split_region(*region, range_to_mprotect, new_range_offset_in_vmobject));
            new_region->set_readable(prot & PROT_READ);
            new_region->set_writable(prot & PROT_WRITE);
            new_region->set_executable(prot & PROT_EXEC);

            // Map the new regions using our page directory (they were just allocated and don't have one).
            for (auto* adjacent_region : adjacent_regions) {
                TRY(adjacent_region->map(space->page_directory()));
            }
            TRY(new_region->map(space->page_directory()));
            return 0;
        }

        if (auto const& regions = TRY(space->find_regions_intersecting(range_to_mprotect)); regions.size()) {
            size_t full_size_found = 0;
            // Check that all intersecting regions are compatible.
            for (auto const* region : regions) {
                if (!region->is_mmap())
                    return EPERM;
                if (region->is_immutable())
                    return EPERM;
                TRY(validate_mmap_prot(prot, region->is_stack(), region->vmobject().is_anonymous(), region));
                if (region->vmobject().is_inode())
                    TRY(validate_inode_mmap_prot(prot, region->mmapped_from_readable(), region->mmapped_from_writable(), region->is_shared()));

                full_size_found += region->range().intersect(range_to_mprotect).size();
            }

            if (full_size_found != range_to_mprotect.size())
                return ENOMEM;

            // Finally, iterate over each region, either updating its access flags if the range covers it wholly,
            // or carving out a new subregion with the appropriate access flags set.
            for (auto* old_region : regions) {
                if (old_region->access() == Memory::prot_to_region_access_flags(prot))
                    continue;

                auto const intersection_to_mprotect = range_to_mprotect.intersect(old_region->range());
                // If the region is completely covered by range, simply update the access flags
                if (intersection_to_mprotect == old_region->range()) {
                    old_region->set_readable(prot & PROT_READ);
                    old_region->set_writable(prot & PROT_WRITE);
                    old_region->set_executable(prot & PROT_EXEC);

                    old_region->remap();
                    continue;
                }
                // Remove the old region from our regions tree, since were going to add another region
                // with the exact same start address.
                auto region = space->take_region(*old_region);
                region->unmap();

                // This vector is the region(s) adjacent to our range.
                // We need to allocate a new region for the range we wanted to change permission bits on.
                auto adjacent_regions = TRY(space->try_split_region_around_range(*old_region, intersection_to_mprotect));

                // Since the range is not contained in a single region, it can only partially cover its starting and ending region,
                // therefore carving out a chunk from the region will always produce a single extra region, and not two.
                VERIFY(adjacent_regions.size() == 1);

                size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (intersection_to_mprotect.base().get() - old_region->range().base().get());
                auto* new_region = TRY(space->try_allocate_split_region(*region, intersection_to_mprotect, new_range_offset_in_vmobject));

                new_region->set_readable(prot & PROT_READ);
                new_region->set_writable(prot & PROT_WRITE);
                new_region->set_executable(prot & PROT_EXEC);

                // Map the new region using our page directory (they were just allocated and don't have one) if any.
                if (adjacent_regions.size())
                    TRY(adjacent_regions[0]->map(space->page_directory()));

                TRY(new_region->map(space->page_directory()));
            }

            return 0;
        }

        return EINVAL;
    });
}

ErrorOr<FlatPtr> Process::sys$madvise(Userspace<void*> address, size_t size, int advice)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto range_to_madvise = TRY(Memory::expand_range_to_page_boundaries(address.ptr(), size));

    if (!range_to_madvise.size())
        return EINVAL;

    if (!is_user_range(range_to_madvise))
        return EFAULT;

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        auto* region = space->find_region_from_range(range_to_madvise);
        if (!region)
            return EINVAL;
        if (!region->is_mmap())
            return EPERM;
        if (region->is_immutable())
            return EPERM;
        if (advice == MADV_SET_VOLATILE || advice == MADV_SET_NONVOLATILE) {
            if (!region->vmobject().is_anonymous())
                return EINVAL;
            auto& vmobject = static_cast<Memory::AnonymousVMObject&>(region->vmobject());
            if (!vmobject.is_purgeable())
                return EINVAL;
            bool was_purged = false;
            TRY(vmobject.set_volatile(advice == MADV_SET_VOLATILE, was_purged));
            return was_purged ? 1 : 0;
        }
        return EINVAL;
    });
}

ErrorOr<FlatPtr> Process::sys$set_mmap_name(Userspace<Syscall::SC_set_mmap_name_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto params = TRY(copy_typed_from_user(user_params));

    if (params.name.length > PATH_MAX)
        return ENAMETOOLONG;

    auto name = TRY(try_copy_kstring_from_user(params.name));
    auto range = TRY(Memory::expand_range_to_page_boundaries((FlatPtr)params.addr, params.size));

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        auto* region = space->find_region_from_range(range);
        if (!region)
            return EINVAL;
        if (!region->is_mmap())
            return EPERM;

        if (region->is_immutable())
            return EPERM;

        region->set_name(move(name));
        PerformanceManager::add_mmap_perf_event(*this, *region);

        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$munmap(Userspace<void*> addr, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    TRY(address_space().with([&](auto& space) {
        return space->unmap_mmap_range(addr.vaddr(), size);
    }));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$mremap(Userspace<Syscall::SC_mremap_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto params = TRY(copy_typed_from_user(user_params));

    auto old_range = TRY(Memory::expand_range_to_page_boundaries((FlatPtr)params.old_address, params.old_size));

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        auto* old_region = space->find_region_from_range(old_range);
        if (!old_region)
            return EINVAL;

        if (!old_region->is_mmap())
            return EPERM;

        if (old_region->is_immutable())
            return EPERM;

        if (old_region->vmobject().is_shared_inode() && params.flags & MAP_PRIVATE && !(params.flags & (MAP_ANONYMOUS | MAP_NORESERVE))) {
            auto range = old_region->range();
            auto old_prot = region_access_flags_to_prot(old_region->access());
            auto old_offset = old_region->offset_in_vmobject();
            NonnullLockRefPtr inode = static_cast<Memory::SharedInodeVMObject&>(old_region->vmobject()).inode();

            auto new_vmobject = TRY(Memory::PrivateInodeVMObject::try_create_with_inode(inode));
            auto old_name = old_region->take_name();

            bool old_region_was_mmapped_from_readable = old_region->mmapped_from_readable();
            bool old_region_was_mmapped_from_writable = old_region->mmapped_from_writable();

            old_region->unmap();
            space->deallocate_region(*old_region);

            auto* new_region = TRY(space->allocate_region_with_vmobject(range, move(new_vmobject), old_offset, old_name->view(), old_prot, false));
            new_region->set_mmap(true, old_region_was_mmapped_from_readable, old_region_was_mmapped_from_writable);
            return new_region->vaddr().get();
        }

        dbgln("sys$mremap: Unimplemented remap request (flags={})", params.flags);
        return ENOTIMPL;
    });
}

ErrorOr<FlatPtr> Process::sys$annotate_mapping(Userspace<void*> address, int flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if (flags == to_underlying(VirtualMemoryRangeFlags::None))
        return EINVAL;

    if (!address)
        return EINVAL;

    if (!Memory::is_user_address(address.vaddr()))
        return EFAULT;

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        if (space->enforces_syscall_regions() && (flags & to_underlying(VirtualMemoryRangeFlags::SyscallCode)))
            return EPERM;

        auto* region = space->find_region_containing(Memory::VirtualRange { address.vaddr(), 1 });
        if (!region)
            return EINVAL;

        if (!region->is_mmap() && !region->is_initially_loaded_executable_segment())
            return EINVAL;
        if (region->is_immutable())
            return EPERM;

        if (flags & to_underlying(VirtualMemoryRangeFlags::SyscallCode))
            region->set_syscall_region(true);
        if (flags & to_underlying(VirtualMemoryRangeFlags::Immutable))
            region->set_immutable();
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$msync(Userspace<void*> address, size_t size, int flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if ((flags & (MS_SYNC | MS_ASYNC | MS_INVALIDATE)) != flags)
        return EINVAL;

    bool is_async = (flags & MS_ASYNC) == MS_ASYNC;
    bool is_sync = (flags & MS_SYNC) == MS_SYNC;
    if (is_sync == is_async)
        return EINVAL;

    if (address.ptr() % PAGE_SIZE != 0)
        return EINVAL;

    // Note: This is not specified
    auto rounded_size = TRY(Memory::page_round_up(size));

    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        auto regions = TRY(space->find_regions_intersecting(Memory::VirtualRange { address.vaddr(), rounded_size }));
        // All regions from address up to address+size shall be mapped
        if (regions.is_empty())
            return ENOMEM;

        size_t total_intersection_size = 0;
        Memory::VirtualRange range_to_sync { address.vaddr(), rounded_size };
        for (auto const* region : regions) {
            // Region was not mapped
            if (!region->is_mmap())
                return ENOMEM;
            total_intersection_size += region->range().intersect(range_to_sync).size();
        }
        // Part of the indicated range was not mapped
        if (total_intersection_size != size)
            return ENOMEM;

        for (auto* region : regions) {
            auto& vmobject = region->vmobject();
            if (!vmobject.is_shared_inode())
                continue;

            off_t offset = region->offset_in_vmobject() + address.ptr() - region->range().base().get();

            auto& inode_vmobject = static_cast<Memory::SharedInodeVMObject&>(vmobject);
            // FIXME: If multiple regions belong to the same vmobject we might want to coalesce these writes
            // FIXME: Handle MS_ASYNC
            TRY(inode_vmobject.sync(offset / PAGE_SIZE, rounded_size / PAGE_SIZE));
            // FIXME: Handle MS_INVALIDATE
        }
        return 0;
    });
}

}
