/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PrivateInodeVMObject.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/limits.h>
#include <LibELF/Validation.h>

namespace Kernel {

static bool should_make_executable_exception_for_dynamic_loader(bool make_readable, bool make_writable, bool make_executable, const Region& region)
{
    // Normally we don't allow W -> X transitions, but we have to make an exception
    // for the dynamic loader, which needs to do this after performing text relocations.

    // FIXME: Investigate whether we could get rid of all text relocations entirely.

    // The exception is only made if all the following criteria is fulfilled:

    // The region must be RW
    if (!(region.is_readable() && region.is_writable() && !region.is_executable()))
        return false;

    // The region wants to become RX
    if (!(make_readable && !make_writable && make_executable))
        return false;

    // The region is backed by a file
    if (!region.vmobject().is_inode())
        return false;

    // The file mapping is private, not shared (no relocations in a shared mapping!)
    if (!region.vmobject().is_private_inode())
        return false;

    auto& inode_vm = static_cast<const InodeVMObject&>(region.vmobject());
    auto& inode = inode_vm.inode();

    Elf32_Ehdr header;
    auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&header);
    auto result = inode.read_bytes(0, sizeof(header), buffer, nullptr);
    if (result.is_error() || result.value() != sizeof(header))
        return false;

    // The file is a valid ELF binary
    if (!ELF::validate_elf_header(header, inode.size()))
        return false;

    // The file is an ELF shared object
    if (header.e_type != ET_DYN)
        return false;

    // FIXME: Are there any additional checks/validations we could do here?
    return true;
}

static bool validate_mmap_prot(int prot, bool map_stack, bool map_anonymous, const Region* region = nullptr)
{
    bool make_readable = prot & PROT_READ;
    bool make_writable = prot & PROT_WRITE;
    bool make_executable = prot & PROT_EXEC;

    if (map_anonymous && make_executable)
        return false;

    if (make_writable && make_executable)
        return false;

    if (map_stack) {
        if (make_executable)
            return false;
        if (!make_readable || !make_writable)
            return false;
    }

    if (region) {
        if (make_writable && region->has_been_executable())
            return false;

        if (make_executable && region->has_been_writable()) {
            if (should_make_executable_exception_for_dynamic_loader(make_readable, make_writable, make_executable, *region))
                return true;

            return false;
        }
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

KResultOr<FlatPtr> Process::sys$mmap(Userspace<const Syscall::SC_mmap_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_mmap_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    FlatPtr addr = params.addr;
    auto size = params.size;
    auto alignment = params.alignment;
    auto prot = params.prot;
    auto flags = params.flags;
    auto fd = params.fd;
    auto offset = params.offset;

    if (prot & PROT_EXEC) {
        REQUIRE_PROMISE(prot_exec);
    }

    if (prot & MAP_FIXED) {
        REQUIRE_PROMISE(map_fixed);
    }

    if (alignment & ~PAGE_MASK)
        return EINVAL;

    if (page_round_up_would_wrap(size))
        return EINVAL;

    if (!is_user_range(VirtualAddress(addr), page_round_up(size)))
        return EFAULT;

    OwnPtr<KString> name;
    if (params.name.characters) {
        if (params.name.length > PATH_MAX)
            return ENAMETOOLONG;
        auto name_or_error = try_copy_kstring_from_user(params.name);
        if (name_or_error.is_error())
            return name_or_error.error();
        name = name_or_error.release_value();
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

    if (map_shared && map_private)
        return EINVAL;

    if (!map_shared && !map_private)
        return EINVAL;

    if (map_fixed && map_randomized)
        return EINVAL;

    if (!validate_mmap_prot(prot, map_stack, map_anonymous))
        return EINVAL;

    if (map_stack && (!map_private || !map_anonymous))
        return EINVAL;

    Region* region = nullptr;
    Optional<Range> range;

    if (map_randomized) {
        range = space().page_directory().range_allocator().allocate_randomized(page_round_up(size), alignment);
    } else {
        range = space().allocate_range(VirtualAddress(addr), size, alignment);
        if (!range.has_value()) {
            if (addr && !map_fixed) {
                // If there's an address but MAP_FIXED wasn't specified, the address is just a hint.
                range = space().allocate_range({}, size, alignment);
            }
        }
    }

    if (!range.has_value())
        return ENOMEM;

    if (map_anonymous) {
        auto strategy = map_noreserve ? AllocationStrategy::None : AllocationStrategy::Reserve;
        auto region_or_error = space().allocate_region(range.value(), {}, prot, strategy);
        if (region_or_error.is_error())
            return region_or_error.error().error();
        region = region_or_error.value();
    } else {
        if (offset < 0)
            return EINVAL;
        if (static_cast<size_t>(offset) & ~PAGE_MASK)
            return EINVAL;
        auto description = file_description(fd);
        if (!description)
            return EBADF;
        if (description->is_directory())
            return ENODEV;
        // Require read access even when read protection is not requested.
        if (!description->is_readable())
            return EACCES;
        if (map_shared) {
            if ((prot & PROT_WRITE) && !description->is_writable())
                return EACCES;
        }
        if (description->inode()) {
            if (!validate_inode_mmap_prot(*this, prot, *description->inode(), map_shared))
                return EACCES;
        }

        auto region_or_error = description->mmap(*this, range.value(), static_cast<u64>(offset), prot, map_shared);
        if (region_or_error.is_error())
            return region_or_error.error().error();
        region = region_or_error.value();
    }

    if (!region)
        return ENOMEM;

    region->set_mmap(true);
    if (map_shared)
        region->set_shared(true);
    if (map_stack)
        region->set_stack(true);
    region->set_name(move(name));

    PerformanceManager::add_mmap_perf_event(*this, *region);

    return region->vaddr().get();
}

static KResultOr<Range> expand_range_to_page_boundaries(FlatPtr address, size_t size)
{
    if (page_round_up_would_wrap(size))
        return EINVAL;

    if ((address + size) < address)
        return EINVAL;

    if (page_round_up_would_wrap(address + size))
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = page_round_up(address + size);

    return Range { base, end - base.get() };
}

KResultOr<int> Process::sys$mprotect(Userspace<void*> addr, size_t size, int prot)
{
    REQUIRE_PROMISE(stdio);

    if (prot & PROT_EXEC) {
        REQUIRE_PROMISE(prot_exec);
    }

    auto range_or_error = expand_range_to_page_boundaries(addr, size);
    if (range_or_error.is_error())
        return range_or_error.error();

    auto range_to_mprotect = range_or_error.value();
    if (!range_to_mprotect.size())
        return EINVAL;

    if (!is_user_range(range_to_mprotect))
        return EFAULT;

    if (auto* whole_region = space().find_region_from_range(range_to_mprotect)) {
        if (!whole_region->is_mmap())
            return EPERM;
        if (!validate_mmap_prot(prot, whole_region->is_stack(), whole_region->vmobject().is_anonymous(), whole_region))
            return EINVAL;
        if (whole_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (whole_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(whole_region->vmobject()).inode(), whole_region->is_shared())) {
            return EACCES;
        }
        whole_region->set_readable(prot & PROT_READ);
        whole_region->set_writable(prot & PROT_WRITE);
        whole_region->set_executable(prot & PROT_EXEC);

        whole_region->remap();
        return 0;
    }

    // Check if we can carve out the desired range from an existing region
    if (auto* old_region = space().find_region_containing(range_to_mprotect)) {
        if (!old_region->is_mmap())
            return EPERM;
        if (!validate_mmap_prot(prot, old_region->is_stack(), old_region->vmobject().is_anonymous(), old_region))
            return EINVAL;
        if (old_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (old_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(old_region->vmobject()).inode(), old_region->is_shared())) {
            return EACCES;
        }

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address, but dont deallocate it yet
        auto region = space().take_region(*old_region);
        VERIFY(region);

        // Unmap the old region here, specifying that we *don't* want the VM deallocated.
        region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);

        // This vector is the region(s) adjacent to our range.
        // We need to allocate a new region for the range we wanted to change permission bits on.
        auto adjacent_regions = space().split_region_around_range(*region, range_to_mprotect);

        size_t new_range_offset_in_vmobject = region->offset_in_vmobject() + (range_to_mprotect.base().get() - region->range().base().get());
        auto& new_region = space().allocate_split_region(*region, range_to_mprotect, new_range_offset_in_vmobject);
        new_region.set_readable(prot & PROT_READ);
        new_region.set_writable(prot & PROT_WRITE);
        new_region.set_executable(prot & PROT_EXEC);

        // Map the new regions using our page directory (they were just allocated and don't have one).
        for (auto* adjacent_region : adjacent_regions) {
            adjacent_region->map(space().page_directory());
        }
        new_region.map(space().page_directory());
        return 0;
    }

    if (const auto& regions = space().find_regions_intersecting(range_to_mprotect); regions.size()) {
        size_t full_size_found = 0;
        // first check before doing anything
        for (const auto* region : regions) {
            if (!region->is_mmap())
                return EPERM;
            if (!validate_mmap_prot(prot, region->is_stack(), region->vmobject().is_anonymous(), region))
                return EINVAL;
            if (region->access() == prot_to_region_access_flags(prot))
                return 0;
            if (region->vmobject().is_inode()
                && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(region->vmobject()).inode(), region->is_shared())) {
                return EACCES;
            }
            full_size_found += region->range().intersect(range_to_mprotect).size();
        }

        if (full_size_found != range_to_mprotect.size())
            return ENOMEM;

        // then do all the other stuff
        for (auto* old_region : regions) {
            const auto intersection_to_mprotect = range_to_mprotect.intersect(old_region->range());
            // full sub region
            if (intersection_to_mprotect == old_region->range()) {
                old_region->set_readable(prot & PROT_READ);
                old_region->set_writable(prot & PROT_WRITE);
                old_region->set_executable(prot & PROT_EXEC);

                old_region->remap();
                continue;
            }
            // Remove the old region from our regions tree, since were going to add another region
            // with the exact same start address, but dont deallocate it yet
            auto region = space().take_region(*old_region);
            VERIFY(region);

            // Unmap the old region here, specifying that we *don't* want the VM deallocated.
            region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);

            // This vector is the region(s) adjacent to our range.
            // We need to allocate a new region for the range we wanted to change permission bits on.
            auto adjacent_regions = space().split_region_around_range(*old_region, intersection_to_mprotect);
            // there should only be one
            VERIFY(adjacent_regions.size() == 1);

            size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (intersection_to_mprotect.base().get() - old_region->range().base().get());
            auto& new_region = space().allocate_split_region(*region, intersection_to_mprotect, new_range_offset_in_vmobject);
            new_region.set_readable(prot & PROT_READ);
            new_region.set_writable(prot & PROT_WRITE);
            new_region.set_executable(prot & PROT_EXEC);

            // Map the new region using our page directory (they were just allocated and don't have one) if any.
            if (adjacent_regions.size())
                adjacent_regions[0]->map(space().page_directory());

            new_region.map(space().page_directory());
        }

        return 0;
    }

    return EINVAL;
}

KResultOr<int> Process::sys$madvise(Userspace<void*> address, size_t size, int advice)
{
    REQUIRE_PROMISE(stdio);

    auto range_or_error = expand_range_to_page_boundaries(address, size);
    if (range_or_error.is_error())
        return range_or_error.error();

    auto range_to_madvise = range_or_error.value();

    if (!range_to_madvise.size())
        return EINVAL;

    if (!is_user_range(range_to_madvise))
        return EFAULT;

    auto* region = space().find_region_from_range(range_to_madvise);
    if (!region)
        return EINVAL;
    if (!region->is_mmap())
        return EPERM;
    bool set_volatile = advice & MADV_SET_VOLATILE;
    bool set_nonvolatile = advice & MADV_SET_NONVOLATILE;
    if (set_volatile && set_nonvolatile)
        return EINVAL;
    if (set_volatile || set_nonvolatile) {
        if (!region->vmobject().is_anonymous())
            return EPERM;
        bool was_purged = false;
        switch (region->set_volatile(VirtualAddress(address), size, set_volatile, was_purged)) {
        case Region::SetVolatileError::Success:
            break;
        case Region::SetVolatileError::NotPurgeable:
            return EPERM;
        case Region::SetVolatileError::OutOfMemory:
            return ENOMEM;
        }
        if (set_nonvolatile)
            return was_purged ? 1 : 0;
        return 0;
    }
    if (advice & MADV_GET_VOLATILE) {
        if (!region->vmobject().is_anonymous())
            return EPERM;
        return region->is_volatile(VirtualAddress(address), size) ? 0 : 1;
    }
    return EINVAL;
}

KResultOr<int> Process::sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_set_mmap_name_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    if (params.name.length > PATH_MAX)
        return ENAMETOOLONG;

    auto name_or_error = try_copy_kstring_from_user(params.name);
    if (name_or_error.is_error())
        return name_or_error.error();
    auto name = name_or_error.release_value();

    auto range_or_error = expand_range_to_page_boundaries((FlatPtr)params.addr, params.size);
    if (range_or_error.is_error())
        return range_or_error.error();

    auto range = range_or_error.value();

    auto* region = space().find_region_from_range(range);
    if (!region)
        return EINVAL;
    if (!region->is_mmap())
        return EPERM;

    region->set_name(move(name));
    PerformanceManager::add_mmap_perf_event(*this, *region);

    return 0;
}

KResultOr<int> Process::sys$munmap(Userspace<void*> addr, size_t size)
{
    REQUIRE_PROMISE(stdio);

    auto result = space().unmap_mmap_range(VirtualAddress { addr }, size);
    if (result.is_error())
        return result;
    return 0;
}

KResultOr<FlatPtr> Process::sys$mremap(Userspace<const Syscall::SC_mremap_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_mremap_params params {};
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    auto range_or_error = expand_range_to_page_boundaries((FlatPtr)params.old_address, params.old_size);
    if (range_or_error.is_error())
        return range_or_error.error().error();

    auto old_range = range_or_error.value();

    auto* old_region = space().find_region_from_range(old_range);
    if (!old_region)
        return EINVAL;

    if (!old_region->is_mmap())
        return EPERM;

    if (old_region->vmobject().is_shared_inode() && params.flags & MAP_PRIVATE && !(params.flags & (MAP_ANONYMOUS | MAP_NORESERVE))) {
        auto range = old_region->range();
        auto old_prot = region_access_flags_to_prot(old_region->access());
        auto old_offset = old_region->offset_in_vmobject();
        NonnullRefPtr inode = static_cast<SharedInodeVMObject&>(old_region->vmobject()).inode();

        auto new_vmobject = PrivateInodeVMObject::create_with_inode(inode);
        if (!new_vmobject)
            return ENOMEM;

        auto old_name = old_region->take_name();

        // Unmap without deallocating the VM range since we're going to reuse it.
        old_region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);
        bool success = space().deallocate_region(*old_region);
        VERIFY(success);

        auto new_region_or_error = space().allocate_region_with_vmobject(range, new_vmobject.release_nonnull(), old_offset, old_name->view(), old_prot, false);
        if (new_region_or_error.is_error())
            return new_region_or_error.error().error();
        auto& new_region = *new_region_or_error.value();
        new_region.set_mmap(true);
        return new_region.vaddr().get();
    }

    dbgln("sys$mremap: Unimplemented remap request (flags={})", params.flags);
    return ENOTIMPL;
}

KResultOr<FlatPtr> Process::sys$allocate_tls(Userspace<const char*> initial_data, size_t size)
{
    REQUIRE_PROMISE(stdio);

    if (!size || size % PAGE_SIZE != 0)
        return EINVAL;

    if (!m_master_tls_region.is_null())
        return EEXIST;

    if (thread_count() != 1)
        return EFAULT;

    Thread* main_thread = nullptr;
    for_each_thread([&main_thread](auto& thread) {
        main_thread = &thread;
        return IterationDecision::Break;
    });
    VERIFY(main_thread);

    auto range = space().allocate_range({}, size);
    if (!range.has_value())
        return ENOMEM;

    auto region_or_error = space().allocate_region(range.value(), String("Master TLS"), PROT_READ | PROT_WRITE);
    if (region_or_error.is_error())
        return region_or_error.error().error();

    m_master_tls_region = region_or_error.value()->make_weak_ptr();
    m_master_tls_size = size;
    m_master_tls_alignment = PAGE_SIZE;

    {
        Kernel::SmapDisabler disabler;
        void* fault_at;
        if (!Kernel::safe_memcpy((char*)m_master_tls_region.unsafe_ptr()->vaddr().as_ptr(), (char*)initial_data.ptr(), size, fault_at))
            return EFAULT;
    }

    auto tsr_result = main_thread->make_thread_specific_region({});
    if (tsr_result.is_error())
        return EFAULT;

    auto& tls_descriptor = Processor::current().get_gdt_entry(GDT_SELECTOR_TLS);
    tls_descriptor.set_base(main_thread->thread_specific_data());
    tls_descriptor.set_limit(main_thread->thread_specific_region_size());

    return m_master_tls_region.unsafe_ptr()->vaddr().get();
}

KResultOr<int> Process::sys$msyscall(Userspace<void*> address)
{
    if (space().enforces_syscall_regions())
        return EPERM;

    if (!address) {
        space().set_enforces_syscall_regions(true);
        return 0;
    }

    if (!is_user_address(VirtualAddress { address }))
        return EFAULT;

    auto* region = space().find_region_containing(Range { VirtualAddress { address }, 1 });
    if (!region)
        return EINVAL;

    if (!region->is_mmap())
        return EINVAL;

    region->set_syscall_region(true);
    return 0;
}

}
