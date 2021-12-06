/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/x86/MSR.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <LibC/limits.h>
#include <LibELF/Validation.h>

namespace Kernel {

static bool should_make_executable_exception_for_dynamic_loader(bool make_readable, bool make_writable, bool make_executable, Memory::Region const& region)
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

    auto& inode_vm = static_cast<Memory::InodeVMObject const&>(region.vmobject());
    auto& inode = inode_vm.inode();

    ElfW(Ehdr) header;
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

static bool validate_mmap_prot(int prot, bool map_stack, bool map_anonymous, Memory::Region const* region = nullptr)
{
    bool make_readable = prot & PROT_READ;
    bool make_writable = prot & PROT_WRITE;
    bool make_executable = prot & PROT_EXEC;

    if (map_anonymous && make_executable)
        return false;

    if (make_writable && make_executable)
        return false;

    if (map_stack && make_executable)
        return false;

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
        if (auto shared_vmobject = inode.shared_vmobject()) {
            if ((prot & PROT_EXEC) && shared_vmobject->writable_mappings())
                return false;
            if ((prot & PROT_WRITE) && shared_vmobject->executable_mappings())
                return false;
        }
    }
    return true;
}

ErrorOr<FlatPtr> Process::sys$mmap(Userspace<const Syscall::SC_mmap_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto params = TRY(copy_typed_from_user(user_params));

    FlatPtr addr = params.addr;
    auto size = params.size;
    auto alignment = params.alignment ? params.alignment : PAGE_SIZE;
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

    if (Memory::page_round_up_would_wrap(size))
        return EINVAL;

    if (!Memory::is_user_range(VirtualAddress(addr), Memory::page_round_up(size)))
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

    Memory::Region* region = nullptr;

    auto range = TRY([&]() -> ErrorOr<Memory::VirtualRange> {
        if (map_randomized) {
            return address_space().page_directory().range_allocator().try_allocate_randomized(Memory::page_round_up(size), alignment);
        }
        auto range = address_space().try_allocate_range(VirtualAddress(addr), size, alignment);
        if (range.is_error()) {
            if (addr && !map_fixed) {
                // If there's an address but MAP_FIXED wasn't specified, the address is just a hint.
                range = address_space().try_allocate_range({}, size, alignment);
            }
        }
        return range;
    }());

    if (map_anonymous) {
        auto strategy = map_noreserve ? AllocationStrategy::None : AllocationStrategy::Reserve;
        RefPtr<Memory::AnonymousVMObject> vmobject;
        if (flags & MAP_PURGEABLE) {
            vmobject = TRY(Memory::AnonymousVMObject::try_create_purgeable_with_size(Memory::page_round_up(size), strategy));
        } else {
            vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(Memory::page_round_up(size), strategy));
        }

        region = TRY(address_space().allocate_region_with_vmobject(range, vmobject.release_nonnull(), 0, {}, prot, map_shared));
    } else {
        if (offset < 0)
            return EINVAL;
        if (static_cast<size_t>(offset) & ~PAGE_MASK)
            return EINVAL;
        auto description = TRY(fds().open_file_description(fd));
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

        region = TRY(description->mmap(*this, range, static_cast<u64>(offset), prot, map_shared));
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

ErrorOr<FlatPtr> Process::sys$mprotect(Userspace<void*> addr, size_t size, int prot)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);

    if (prot & PROT_EXEC) {
        REQUIRE_PROMISE(prot_exec);
    }

    auto range_to_mprotect = TRY(Memory::expand_range_to_page_boundaries(addr.ptr(), size));
    if (!range_to_mprotect.size())
        return EINVAL;

    if (!is_user_range(range_to_mprotect))
        return EFAULT;

    if (auto* whole_region = address_space().find_region_from_range(range_to_mprotect)) {
        if (!whole_region->is_mmap())
            return EPERM;
        if (!validate_mmap_prot(prot, whole_region->is_stack(), whole_region->vmobject().is_anonymous(), whole_region))
            return EINVAL;
        if (whole_region->access() == Memory::prot_to_region_access_flags(prot))
            return 0;
        if (whole_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<Memory::InodeVMObject const&>(whole_region->vmobject()).inode(), whole_region->is_shared())) {
            return EACCES;
        }
        whole_region->set_readable(prot & PROT_READ);
        whole_region->set_writable(prot & PROT_WRITE);
        whole_region->set_executable(prot & PROT_EXEC);

        whole_region->remap();
        return 0;
    }

    // Check if we can carve out the desired range from an existing region
    if (auto* old_region = address_space().find_region_containing(range_to_mprotect)) {
        if (!old_region->is_mmap())
            return EPERM;
        if (!validate_mmap_prot(prot, old_region->is_stack(), old_region->vmobject().is_anonymous(), old_region))
            return EINVAL;
        if (old_region->access() == Memory::prot_to_region_access_flags(prot))
            return 0;
        if (old_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<Memory::InodeVMObject const&>(old_region->vmobject()).inode(), old_region->is_shared())) {
            return EACCES;
        }

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address, but do not deallocate it yet
        auto region = address_space().take_region(*old_region);

        // Unmap the old region here, specifying that we *don't* want the VM deallocated.
        region->unmap(Memory::Region::ShouldDeallocateVirtualRange::No);

        // This vector is the region(s) adjacent to our range.
        // We need to allocate a new region for the range we wanted to change permission bits on.
        auto adjacent_regions = TRY(address_space().try_split_region_around_range(*region, range_to_mprotect));

        size_t new_range_offset_in_vmobject = region->offset_in_vmobject() + (range_to_mprotect.base().get() - region->range().base().get());
        auto new_region = TRY(address_space().try_allocate_split_region(*region, range_to_mprotect, new_range_offset_in_vmobject));
        new_region->set_readable(prot & PROT_READ);
        new_region->set_writable(prot & PROT_WRITE);
        new_region->set_executable(prot & PROT_EXEC);

        // Map the new regions using our page directory (they were just allocated and don't have one).
        for (auto* adjacent_region : adjacent_regions) {
            TRY(adjacent_region->map(address_space().page_directory()));
        }
        TRY(new_region->map(address_space().page_directory()));
        return 0;
    }

    if (const auto& regions = address_space().find_regions_intersecting(range_to_mprotect); regions.size()) {
        size_t full_size_found = 0;
        // Check that all intersecting regions are compatible.
        for (const auto* region : regions) {
            if (!region->is_mmap())
                return EPERM;
            if (!validate_mmap_prot(prot, region->is_stack(), region->vmobject().is_anonymous(), region))
                return EINVAL;
            if (region->vmobject().is_inode()
                && !validate_inode_mmap_prot(*this, prot, static_cast<Memory::InodeVMObject const&>(region->vmobject()).inode(), region->is_shared())) {
                return EACCES;
            }
            full_size_found += region->range().intersect(range_to_mprotect).size();
        }

        if (full_size_found != range_to_mprotect.size())
            return ENOMEM;

        // Finally, iterate over each region, either updating its access flags if the range covers it wholly,
        // or carving out a new subregion with the appropriate access flags set.
        for (auto* old_region : regions) {
            if (old_region->access() == Memory::prot_to_region_access_flags(prot))
                continue;

            const auto intersection_to_mprotect = range_to_mprotect.intersect(old_region->range());
            // If the region is completely covered by range, simply update the access flags
            if (intersection_to_mprotect == old_region->range()) {
                old_region->set_readable(prot & PROT_READ);
                old_region->set_writable(prot & PROT_WRITE);
                old_region->set_executable(prot & PROT_EXEC);

                old_region->remap();
                continue;
            }
            // Remove the old region from our regions tree, since were going to add another region
            // with the exact same start address, but dont deallocate it yet
            auto region = address_space().take_region(*old_region);

            // Unmap the old region here, specifying that we *don't* want the VM deallocated.
            region->unmap(Memory::Region::ShouldDeallocateVirtualRange::No);

            // This vector is the region(s) adjacent to our range.
            // We need to allocate a new region for the range we wanted to change permission bits on.
            auto adjacent_regions = TRY(address_space().try_split_region_around_range(*old_region, intersection_to_mprotect));

            // Since the range is not contained in a single region, it can only partially cover its starting and ending region,
            // therefore carving out a chunk from the region will always produce a single extra region, and not two.
            VERIFY(adjacent_regions.size() == 1);

            size_t new_range_offset_in_vmobject = old_region->offset_in_vmobject() + (intersection_to_mprotect.base().get() - old_region->range().base().get());
            auto* new_region = TRY(address_space().try_allocate_split_region(*region, intersection_to_mprotect, new_range_offset_in_vmobject));

            new_region->set_readable(prot & PROT_READ);
            new_region->set_writable(prot & PROT_WRITE);
            new_region->set_executable(prot & PROT_EXEC);

            // Map the new region using our page directory (they were just allocated and don't have one) if any.
            if (adjacent_regions.size())
                TRY(adjacent_regions[0]->map(address_space().page_directory()));

            TRY(new_region->map(address_space().page_directory()));
        }

        return 0;
    }

    return EINVAL;
}

ErrorOr<FlatPtr> Process::sys$madvise(Userspace<void*> address, size_t size, int advice)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);

    auto range_to_madvise = TRY(Memory::expand_range_to_page_boundaries(address.ptr(), size));

    if (!range_to_madvise.size())
        return EINVAL;

    if (!is_user_range(range_to_madvise))
        return EFAULT;

    auto* region = address_space().find_region_from_range(range_to_madvise);
    if (!region)
        return EINVAL;
    if (!region->is_mmap())
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
}

ErrorOr<FlatPtr> Process::sys$set_mmap_name(Userspace<const Syscall::SC_set_mmap_name_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto params = TRY(copy_typed_from_user(user_params));

    if (params.name.length > PATH_MAX)
        return ENAMETOOLONG;

    auto name = TRY(try_copy_kstring_from_user(params.name));
    auto range = TRY(Memory::expand_range_to_page_boundaries((FlatPtr)params.addr, params.size));

    auto* region = address_space().find_region_from_range(range);
    if (!region)
        return EINVAL;
    if (!region->is_mmap())
        return EPERM;

    region->set_name(move(name));
    PerformanceManager::add_mmap_perf_event(*this, *region);

    return 0;
}

ErrorOr<FlatPtr> Process::sys$munmap(Userspace<void*> addr, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    TRY(address_space().unmap_mmap_range(addr.vaddr(), size));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$mremap(Userspace<const Syscall::SC_mremap_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto params = TRY(copy_typed_from_user(user_params));

    auto old_range = TRY(Memory::expand_range_to_page_boundaries((FlatPtr)params.old_address, params.old_size));

    auto* old_region = address_space().find_region_from_range(old_range);
    if (!old_region)
        return EINVAL;

    if (!old_region->is_mmap())
        return EPERM;

    if (old_region->vmobject().is_shared_inode() && params.flags & MAP_PRIVATE && !(params.flags & (MAP_ANONYMOUS | MAP_NORESERVE))) {
        auto range = old_region->range();
        auto old_prot = region_access_flags_to_prot(old_region->access());
        auto old_offset = old_region->offset_in_vmobject();
        NonnullRefPtr inode = static_cast<Memory::SharedInodeVMObject&>(old_region->vmobject()).inode();

        auto new_vmobject = TRY(Memory::PrivateInodeVMObject::try_create_with_inode(inode));
        auto old_name = old_region->take_name();

        // Unmap without deallocating the VM range since we're going to reuse it.
        old_region->unmap(Memory::Region::ShouldDeallocateVirtualRange::No);
        address_space().deallocate_region(*old_region);

        auto new_region = TRY(address_space().allocate_region_with_vmobject(range, move(new_vmobject), old_offset, old_name->view(), old_prot, false));
        new_region->set_mmap(true);
        return new_region->vaddr().get();
    }

    dbgln("sys$mremap: Unimplemented remap request (flags={})", params.flags);
    return ENOTIMPL;
}

ErrorOr<FlatPtr> Process::sys$allocate_tls(Userspace<const char*> initial_data, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);

    if (!size || size % PAGE_SIZE != 0)
        return EINVAL;

    if (!m_master_tls_region.is_null())
        return EEXIST;

    if (thread_count() != 1)
        return EFAULT;

    Thread* main_thread = nullptr;
    bool multiple_threads = false;
    for_each_thread([&main_thread, &multiple_threads](auto& thread) {
        if (main_thread)
            multiple_threads = true;
        main_thread = &thread;
        return IterationDecision::Break;
    });
    VERIFY(main_thread);

    if (multiple_threads)
        return EINVAL;

    auto range = TRY(address_space().try_allocate_range({}, size));
    auto region = TRY(address_space().allocate_region(range, String("Master TLS"), PROT_READ | PROT_WRITE));

    m_master_tls_region = region->make_weak_ptr();
    m_master_tls_size = size;
    m_master_tls_alignment = PAGE_SIZE;

    {
        Kernel::SmapDisabler disabler;
        void* fault_at;
        if (!Kernel::safe_memcpy((char*)m_master_tls_region.unsafe_ptr()->vaddr().as_ptr(), (char*)initial_data.ptr(), size, fault_at))
            return EFAULT;
    }

    TRY(main_thread->make_thread_specific_region({}));

#if ARCH(I386)
    auto& tls_descriptor = Processor::current().get_gdt_entry(GDT_SELECTOR_TLS);
    tls_descriptor.set_base(main_thread->thread_specific_data());
    tls_descriptor.set_limit(main_thread->thread_specific_region_size());
#else
    MSR fs_base_msr(MSR_FS_BASE);
    fs_base_msr.set(main_thread->thread_specific_data().get());
#endif

    return m_master_tls_region.unsafe_ptr()->vaddr().get();
}

ErrorOr<FlatPtr> Process::sys$msyscall(Userspace<void*> address)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    if (address_space().enforces_syscall_regions())
        return EPERM;

    if (!address) {
        address_space().set_enforces_syscall_regions(true);
        return 0;
    }

    if (!Memory::is_user_address(address.vaddr()))
        return EFAULT;

    auto* region = address_space().find_region_containing(Memory::VirtualRange { address.vaddr(), 1 });
    if (!region)
        return EINVAL;

    if (!region->is_mmap())
        return EINVAL;

    region->set_syscall_region(true);
    return 0;
}

ErrorOr<FlatPtr> Process::sys$msync(Userspace<void*> address, size_t size, int flags)
{
    if ((flags & (MS_SYNC | MS_ASYNC | MS_INVALIDATE)) != flags)
        return EINVAL;

    bool is_async = (flags & MS_ASYNC) == MS_ASYNC;
    bool is_sync = (flags & MS_SYNC) == MS_SYNC;
    if (is_sync == is_async)
        return EINVAL;

    if (address.ptr() % PAGE_SIZE != 0)
        return EINVAL;

    // Note: This is not specified
    size = Memory::page_round_up(size);

    // FIXME: We probably want to sync all mappings in the address+size range.
    auto* region = address_space().find_region_containing(Memory::VirtualRange { address.vaddr(), size });
    // All regions from address upto address+size shall be mapped
    if (!region)
        return ENOMEM;

    auto& vmobject = region->vmobject();
    if (!vmobject.is_shared_inode())
        return 0;

    off_t offset = region->offset_in_vmobject() + address.ptr() - region->range().base().get();

    auto& inode_vmobject = static_cast<Memory::SharedInodeVMObject&>(vmobject);
    // FIXME: Handle MS_ASYNC
    TRY(inode_vmobject.sync(offset / PAGE_SIZE, size / PAGE_SIZE));
    // FIXME: Handle MS_INVALIDATE
    // FIXME: If msync() causes any write to a file, the file's st_ctime and st_mtime fields shall be marked for update.
    return 0;
}

}
