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

#include <AK/Demangle.h>
#include <AK/FileSystemPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/Devices/PIT.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KSyms.h>
#include <Kernel/KernelInfoPage.h>
#include <Kernel/Module.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessTracer.h>
#include <Kernel/Profiling.h>
#include <Kernel/RTC.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/SharedBuffer.h>
#include <Kernel/Syscall.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PurgeableVMObject.h>
#include <LibBareMetal/IO.h>
#include <LibBareMetal/Output/Console.h>
#include <LibBareMetal/StdLib.h>
#include <LibC/errno_numbers.h>
#include <LibC/limits.h>
#include <LibC/signal_numbers.h>
#include <LibELF/ELFLoader.h>

//#define PROCESS_DEBUG
//#define DEBUG_POLL_SELECT
//#define DEBUG_IO
//#define TASK_DEBUG
//#define FORK_DEBUG
//#define EXEC_DEBUG
//#define SIGNAL_DEBUG
//#define SHARED_BUFFER_DEBUG

namespace Kernel {

static void create_signal_trampolines();
static void create_kernel_info_page();

Process* Process::current;

static pid_t next_pid;
InlineLinkedList<Process>* g_processes;
static String* s_hostname;
static Lock* s_hostname_lock;
static VirtualAddress s_info_page_address_for_userspace;
static VirtualAddress s_info_page_address_for_kernel;
VirtualAddress g_return_to_ring3_from_signal_trampoline;
HashMap<String, OwnPtr<Module>>* g_modules;

pid_t Process::allocate_pid()
{
    InterruptDisabler disabler;
    return next_pid++;
}

void Process::initialize()
{
    g_modules = new HashMap<String, OwnPtr<Module>>;

    next_pid = 0;
    g_processes = new InlineLinkedList<Process>;
    s_hostname = new String("courage");
    s_hostname_lock = new Lock;

    create_signal_trampolines();
    create_kernel_info_page();
}

void Process::update_info_page_timestamp(const timeval& tv)
{
    auto* info_page = (KernelInfoPage*)s_info_page_address_for_kernel.as_ptr();
    info_page->serial++;
    const_cast<timeval&>(info_page->now) = tv;
}

Vector<pid_t> Process::all_pids()
{
    Vector<pid_t> pids;
    InterruptDisabler disabler;
    pids.ensure_capacity((int)g_processes->size_slow());
    for (auto& process : *g_processes)
        pids.append(process.pid());
    return pids;
}

Vector<Process*> Process::all_processes()
{
    Vector<Process*> processes;
    InterruptDisabler disabler;
    processes.ensure_capacity((int)g_processes->size_slow());
    for (auto& process : *g_processes)
        processes.append(&process);
    return processes;
}

bool Process::in_group(gid_t gid) const
{
    return m_gid == gid || m_extra_gids.contains(gid);
}

Range Process::allocate_range(VirtualAddress vaddr, size_t size, size_t alignment)
{
    vaddr.mask(PAGE_MASK);
    size = PAGE_ROUND_UP(size);
    if (vaddr.is_null())
        return page_directory().range_allocator().allocate_anywhere(size, alignment);
    return page_directory().range_allocator().allocate_specific(vaddr, size);
}

static unsigned prot_to_region_access_flags(int prot)
{
    unsigned access = 0;
    if (prot & PROT_READ)
        access |= Region::Access::Read;
    if (prot & PROT_WRITE)
        access |= Region::Access::Write;
    if (prot & PROT_EXEC)
        access |= Region::Access::Execute;
    return access;
}

Region& Process::allocate_split_region(const Region& source_region, const Range& range, size_t offset_in_vmobject)
{
    auto& region = add_region(Region::create_user_accessible(range, source_region.vmobject(), offset_in_vmobject, source_region.name(), source_region.access()));
    region.set_mmap(source_region.is_mmap());
    region.set_stack(source_region.is_stack());
    size_t page_offset_in_source_region = (offset_in_vmobject - source_region.offset_in_vmobject()) / PAGE_SIZE;
    for (size_t i = 0; i < region.page_count(); ++i) {
        if (source_region.should_cow(page_offset_in_source_region + i))
            region.set_should_cow(i, true);
    }
    return region;
}

Region* Process::allocate_region(const Range& range, const String& name, int prot, bool commit)
{
    ASSERT(range.is_valid());
    auto& region = add_region(Region::create_user_accessible(range, name, prot_to_region_access_flags(prot)));
    region.map(page_directory());
    if (commit)
        region.commit();
    return &region;
}

Region* Process::allocate_region(VirtualAddress vaddr, size_t size, const String& name, int prot, bool commit)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    return allocate_region(range, name, prot, commit);
}

Region* Process::allocate_file_backed_region(VirtualAddress vaddr, size_t size, NonnullRefPtr<Inode> inode, const String& name, int prot)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    auto& region = add_region(Region::create_user_accessible(range, inode, name, prot_to_region_access_flags(prot)));
    region.map(page_directory());
    return &region;
}

Region* Process::allocate_region_with_vmobject(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, int prot, bool user_accessible)
{
    ASSERT(range.is_valid());
    size_t end_in_vmobject = offset_in_vmobject + range.size();
    if (end_in_vmobject <= offset_in_vmobject) {
        dbgprintf("allocate_region_with_vmobject: Overflow (offset + size)\n");
        return nullptr;
    }
    if (offset_in_vmobject >= vmobject->size()) {
        dbgprintf("allocate_region_with_vmobject: Attempt to allocate a region with an offset past the end of its VMObject.\n");
        return nullptr;
    }
    if (end_in_vmobject > vmobject->size()) {
        dbgprintf("allocate_region_with_vmobject: Attempt to allocate a region with an end past the end of its VMObject.\n");
        return nullptr;
    }
    offset_in_vmobject &= PAGE_MASK;
    Region* region;
    if (user_accessible)
        region = &add_region(Region::create_user_accessible(range, move(vmobject), offset_in_vmobject, name, prot_to_region_access_flags(prot)));
    else
        region = &add_region(Region::create_kernel_only(range, move(vmobject), offset_in_vmobject, name, prot_to_region_access_flags(prot)));
    region->map(page_directory());
    return region;
}


Region* Process::allocate_region_with_vmobject(VirtualAddress vaddr, size_t size, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, int prot, bool user_accessible)
{
    auto range = allocate_range(vaddr, size);
    if (!range.is_valid())
        return nullptr;
    return allocate_region_with_vmobject(range, move(vmobject), offset_in_vmobject, name, prot, user_accessible);
}

bool Process::deallocate_region(Region& region)
{
    InterruptDisabler disabler;
    if (m_region_lookup_cache.region == &region)
        m_region_lookup_cache.region = nullptr;
    for (int i = 0; i < m_regions.size(); ++i) {
        if (&m_regions[i] == &region) {
            m_regions.remove(i);
            return true;
        }
    }
    return false;
}

Region* Process::region_from_range(const Range& range)
{
    if (m_region_lookup_cache.range == range && m_region_lookup_cache.region)
        return m_region_lookup_cache.region;

    size_t size = PAGE_ROUND_UP(range.size());
    for (auto& region : m_regions) {
        if (region.vaddr() == range.base() && region.size() == size) {
            m_region_lookup_cache.range = range;
            m_region_lookup_cache.region = region.make_weak_ptr();
            return &region;
        }
    }
    return nullptr;
}

Region* Process::region_containing(const Range& range)
{
    for (auto& region : m_regions) {
        if (region.contains(range))
            return &region;
    }
    return nullptr;
}

int Process::sys$set_mmap_name(const Syscall::SC_set_mmap_name_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_set_mmap_name_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (params.name.length > PATH_MAX)
        return -ENAMETOOLONG;

    auto name = validate_and_copy_string_from_user(params.name);
    if (name.is_null())
        return -EFAULT;

    auto* region = region_from_range({ VirtualAddress(params.addr), params.size });
    if (!region)
        return -EINVAL;
    if (!region->is_mmap())
        return -EPERM;
    region->set_name(name);
    return 0;
}

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

static bool validate_inode_mmap_prot(const Process& process, int prot, const Inode& inode)
{
    auto metadata = inode.metadata();
    if ((prot & PROT_WRITE) && !metadata.may_write(process))
        return false;
    if ((prot & PROT_READ) && !metadata.may_read(process))
        return false;
    InterruptDisabler disabler;
    if (inode.vmobject()) {
        if ((prot & PROT_EXEC) && inode.vmobject()->writable_mappings())
            return false;
        if ((prot & PROT_WRITE) && inode.vmobject()->executable_mappings())
            return false;
    }
    return true;
}

// Carve out a virtual address range from a region and return the two regions on either side
Vector<Region*, 2> Process::split_region_around_range(const Region& source_region, const Range& desired_range)
{
    Range old_region_range = source_region.range();
    auto remaining_ranges_after_unmap = old_region_range.carve(desired_range);

    ASSERT(!remaining_ranges_after_unmap.is_empty());
    auto make_replacement_region = [&](const Range& new_range) -> Region& {
        ASSERT(new_range.base() >= old_region_range.base());
        ASSERT(new_range.end() <= old_region_range.end());
        size_t new_range_offset_in_vmobject = source_region.offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
        return allocate_split_region(source_region, new_range, new_range_offset_in_vmobject);
    };
    Vector<Region*, 2> new_regions;
    for (auto& new_range : remaining_ranges_after_unmap) {
        new_regions.unchecked_append(&make_replacement_region(new_range));
    }
    return new_regions;
}

void* Process::sys$mmap(const Syscall::SC_mmap_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_mmap_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
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
        name = validate_and_copy_string_from_user(params.name);
        if (name.is_null())
            return (void*)-EFAULT;
    }

    if (size == 0)
        return (void*)-EINVAL;
    if ((uintptr_t)addr & ~PAGE_MASK)
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
        // FIXME: Implement MAP_PRIVATE for FileDescription-backed mmap
        if (map_private)
            return (void*)-ENOTSUP;
        auto description = file_description(fd);
        if (!description)
            return (void*)-EBADF;
        if (description->is_directory())
            return (void*)-ENODEV;
        if ((prot & PROT_READ) && !description->is_readable())
            return (void*)-EACCES;
        if ((prot & PROT_WRITE) && !description->is_writable())
            return (void*)-EACCES;
        if (description->inode()) {
            if (!validate_inode_mmap_prot(*this, prot, *description->inode()))
                return (void*)-EACCES;
        }
        auto region_or_error = description->mmap(*this, VirtualAddress(addr), static_cast<size_t>(offset), size, prot);
        if (region_or_error.is_error()) {
            // Fail if MAP_FIXED or address is 0, retry otherwise
            if (map_fixed || addr == 0)
                return (void*)(int)region_or_error.error();
            region_or_error = description->mmap(*this, {}, static_cast<size_t>(offset), size, prot);
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

int Process::sys$munmap(void* addr, size_t size)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (!is_user_range(VirtualAddress(addr), size))
        return -EFAULT;

    Range range_to_unmap { VirtualAddress(addr), size };
    if (auto* whole_region = region_from_range(range_to_unmap)) {
        if (!whole_region->is_mmap())
            return -EPERM;
        bool success = deallocate_region(*whole_region);
        ASSERT(success);
        return 0;
    }

    if (auto* old_region = region_containing(range_to_unmap)) {
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

int Process::sys$mprotect(void* addr, size_t size, int prot)
{
    REQUIRE_PROMISE(stdio);

    if (!size)
        return -EINVAL;

    if (!is_user_range(VirtualAddress(addr), size))
        return -EFAULT;

    Range range_to_mprotect = { VirtualAddress(addr), size };

    if (auto* whole_region = region_from_range(range_to_mprotect)) {
        if (!whole_region->is_mmap())
            return -EPERM;
        if (!validate_mmap_prot(prot, whole_region->is_stack()))
            return -EINVAL;
        if (whole_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (whole_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(whole_region->vmobject()).inode())) {
            return -EACCES;
        }
        whole_region->set_readable(prot & PROT_READ);
        whole_region->set_writable(prot & PROT_WRITE);
        whole_region->set_executable(prot & PROT_EXEC);
        whole_region->remap();
        return 0;
    }

    // Check if we can carve out the desired range from an existing region
    if (auto* old_region = region_containing(range_to_mprotect)) {
        if (!old_region->is_mmap())
            return -EPERM;
        if (!validate_mmap_prot(prot, old_region->is_stack()))
            return -EINVAL;
        if (old_region->access() == prot_to_region_access_flags(prot))
            return 0;
        if (old_region->vmobject().is_inode()
            && !validate_inode_mmap_prot(*this, prot, static_cast<const InodeVMObject&>(old_region->vmobject()).inode())) {
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

    auto* region = region_from_range({ VirtualAddress(address), size });
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

int Process::sys$purge(int mode)
{
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return -EPERM;
    int purged_page_count = 0;
    if (mode & PURGE_ALL_VOLATILE) {
        NonnullRefPtrVector<PurgeableVMObject> vmobjects;
        {
            InterruptDisabler disabler;
            MM.for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_purgeable())
                    vmobjects.append(static_cast<PurgeableVMObject&>(vmobject));
                return IterationDecision::Continue;
            });
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.purge();
        }
    }
    if (mode & PURGE_ALL_CLEAN_INODE) {
        NonnullRefPtrVector<InodeVMObject> vmobjects;
        {
            InterruptDisabler disabler;
            MM.for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_inode())
                    vmobjects.append(static_cast<InodeVMObject&>(vmobject));
                return IterationDecision::Continue;
            });
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.release_all_clean_pages();
        }
    }
    return purged_page_count;
}

int Process::sys$gethostname(char* buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    LOCKER(*s_hostname_lock);
    if ((size_t)size < (s_hostname->length() + 1))
        return -ENAMETOOLONG;
    copy_to_user(buffer, s_hostname->characters(), s_hostname->length() + 1);
    return 0;
}

pid_t Process::sys$fork(RegisterState& regs)
{
    REQUIRE_PROMISE(proc);
    Thread* child_first_thread = nullptr;
    auto* child = new Process(child_first_thread, m_name, m_uid, m_gid, m_pid, m_ring, m_cwd, m_executable, m_tty, this);
    child->m_root_directory = m_root_directory;
    child->m_root_directory_relative_to_global_root = m_root_directory_relative_to_global_root;
    child->m_promises = m_promises;
    child->m_execpromises = m_execpromises;
    child->m_veil_state = m_veil_state;
    child->m_unveiled_paths = m_unveiled_paths;
    child->m_fds = m_fds;
    child->m_sid = m_sid;
    child->m_pgid = m_pgid;
    child->m_umask = m_umask;

#ifdef FORK_DEBUG
    dbgprintf("fork: child=%p\n", child);
#endif

    for (auto& region : m_regions) {
#ifdef FORK_DEBUG
        dbg() << "fork: cloning Region{" << &region << "} '" << region.name() << "' @ " << region.vaddr();
#endif
        auto& child_region = child->add_region(region.clone());
        child_region.map(child->page_directory());

        if (&region == m_master_tls_region)
            child->m_master_tls_region = &child_region;
    }

    child->m_extra_gids = m_extra_gids;

    auto& child_tss = child_first_thread->m_tss;
    child_tss.eax = 0; // fork() returns 0 in the child :^)
    child_tss.ebx = regs.ebx;
    child_tss.ecx = regs.ecx;
    child_tss.edx = regs.edx;
    child_tss.ebp = regs.ebp;
    child_tss.esp = regs.userspace_esp;
    child_tss.esi = regs.esi;
    child_tss.edi = regs.edi;
    child_tss.eflags = regs.eflags;
    child_tss.eip = regs.eip;
    child_tss.cs = regs.cs;
    child_tss.ds = regs.ds;
    child_tss.es = regs.es;
    child_tss.fs = regs.fs;
    child_tss.gs = regs.gs;
    child_tss.ss = regs.userspace_ss;

#ifdef FORK_DEBUG
    dbgprintf("fork: child will begin executing at %w:%x with stack %w:%x, kstack %w:%x\n", child_tss.cs, child_tss.eip, child_tss.ss, child_tss.esp, child_tss.ss0, child_tss.esp0);
#endif

    {
        InterruptDisabler disabler;
        g_processes->prepend(child);
    }
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) forked from %u @ %p\n", child->pid(), child->name().characters(), m_pid, child_tss.eip);
#endif

    child_first_thread->set_state(Thread::State::Skip1SchedulerPass);
    return child->pid();
}

int Process::do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description)
{
    ASSERT(is_ring3());
    auto path = main_program_description->absolute_path();
    dbg() << "do_exec(" << path << ")";
    // FIXME(Thread): Kill any threads the moment we commit to the exec().
    if (thread_count() != 1) {
        dbgprintf("Gonna die because I have many threads! These are the threads:\n");
        for_each_thread([](Thread& thread) {
            dbgprintf("Thread{%p}: TID=%d, PID=%d\n", &thread, thread.tid(), thread.pid());
            return IterationDecision::Continue;
        });
        ASSERT(thread_count() == 1);
        ASSERT_NOT_REACHED();
    }

    size_t total_blob_size = 0;
    for (auto& a : arguments)
        total_blob_size += a.length() + 1;
    for (auto& e : environment)
        total_blob_size += e.length() + 1;

    size_t total_meta_size = sizeof(char*) * (arguments.size() + 1) + sizeof(char*) * (environment.size() + 1);

    // FIXME: How much stack space does process startup need?
    if ((total_blob_size + total_meta_size) >= Thread::default_userspace_stack_size)
        return -E2BIG;

    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    RefPtr<InodeVMObject> vmobject;
    if (interpreter_description) {
        vmobject = InodeVMObject::create_with_inode(*interpreter_description->inode());
    } else {
        vmobject = InodeVMObject::create_with_inode(*main_program_description->inode());
    }

    if (static_cast<const InodeVMObject&>(*vmobject).writable_mappings()) {
        dbg() << "Refusing to execute a write-mapped program";
        return -ETXTBSY;
    }

    // Disable profiling temporarily in case it's running on this process.
    bool was_profiling = is_profiling();
    TemporaryChange profiling_disabler(m_profiling, false);

    auto old_page_directory = move(m_page_directory);
    auto old_regions = move(m_regions);
    m_page_directory = PageDirectory::create_for_userspace(*this);
#ifdef MM_DEBUG
    dbgprintf("Process %u exec: PD=%x created\n", pid(), m_page_directory.ptr());
#endif

    MM.enter_process_paging_scope(*this);

    Region* region { nullptr };

    InodeMetadata loader_metadata;

    // FIXME: Hoooo boy this is a hack if I ever saw one.
    //      This is the 'random' offset we're giving to our ET_DYN exectuables to start as.
    //      It also happens to be the static Virtual Addresss offset every static exectuable gets :)
    //      Without this, some assumptions by the ELF loading hooks below are severely broken.
    //      0x08000000 is a verified random number chosen by random dice roll https://xkcd.com/221/
    u32 totally_random_offset = interpreter_description ? 0x08000000 : 0;

    // FIXME: We should be able to load both the PT_INTERP interpreter and the main program... once the RTLD is smart enough
    if (interpreter_description) {
        loader_metadata = interpreter_description->metadata();
        region = allocate_region_with_vmobject(VirtualAddress(), loader_metadata.size, *vmobject, 0, interpreter_description->absolute_path(), PROT_READ, false);
        // we don't need the interpreter file desciption after we've loaded (or not) it into memory
        interpreter_description = nullptr;
    } else {
        loader_metadata = main_program_description->metadata();
        region = allocate_region_with_vmobject(VirtualAddress(), loader_metadata.size, *vmobject, 0, main_program_description->absolute_path(), PROT_READ, false);
    }

    ASSERT(region);

    Region* master_tls_region { nullptr };
    size_t master_tls_size = 0;
    size_t master_tls_alignment = 0;
    u32 entry_eip = 0;

    OwnPtr<ELFLoader> loader;
    {
        ArmedScopeGuard rollback_regions_guard([&]() {
            ASSERT(Process::current == this);
            m_page_directory = move(old_page_directory);
            m_regions = move(old_regions);
            MM.enter_process_paging_scope(*this);
        });

        loader = make<ELFLoader>(region->vaddr().as_ptr(), loader_metadata.size);

        // Load the correct executable -- either interp or main program.
        // FIXME: Once we actually load both interp and main, we'll need to be more clever about this.
        //     In that case, both will be ET_DYN objects, so they'll both be completely relocatable.
        //     That means, we can put them literally anywhere in User VM space (ASLR anyone?).
        // ALSO FIXME: Reminder to really really fix that 'totally random offset' business.
        loader->map_section_hook = [&](VirtualAddress vaddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable, bool is_executable, const String& name) -> u8* {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            int prot = 0;
            if (is_readable)
                prot |= PROT_READ;
            if (is_writable)
                prot |= PROT_WRITE;
            if (is_executable)
                prot |= PROT_EXEC;
            if (auto* region = allocate_region_with_vmobject(vaddr.offset(totally_random_offset), size, *vmobject, offset_in_image, String(name), prot))
                return region->vaddr().as_ptr();
            return nullptr;
        };
        loader->alloc_section_hook = [&](VirtualAddress vaddr, size_t size, size_t alignment, bool is_readable, bool is_writable, const String& name) -> u8* {
            ASSERT(size);
            ASSERT(alignment == PAGE_SIZE);
            int prot = 0;
            if (is_readable)
                prot |= PROT_READ;
            if (is_writable)
                prot |= PROT_WRITE;
            if (auto* region = allocate_region(vaddr.offset(totally_random_offset), size, String(name), prot))
                return region->vaddr().as_ptr();
            return nullptr;
        };

        // FIXME: Move TLS region allocation to userspace: LibC and the dynamic loader.
        //     LibC if we end up with a statically linked executable, and the
        //     dynamic loader so that it can create new TLS blocks for each shared libarary
        //     that gets loaded as part of DT_NEEDED processing, and via dlopen()
        //     If that doesn't happen quickly, at least pass the location of the TLS region
        //     some ELF Auxilliary Vector so the loader can use it/create new ones as necessary.
        loader->tls_section_hook = [&](size_t size, size_t alignment) {
            ASSERT(size);
            master_tls_region = allocate_region({}, size, String(), PROT_READ | PROT_WRITE);
            master_tls_size = size;
            master_tls_alignment = alignment;
            return master_tls_region->vaddr().as_ptr();
        };
        bool success = loader->load();
        if (!success) {
            kprintf("do_exec: Failure loading %s\n", path.characters());
            return -ENOEXEC;
        }
        // FIXME: Validate that this virtual address is within executable region,
        //     instead of just non-null. You could totally have a DSO with entry point of
        //     the beginning of the text segement.
        if (!loader->entry().offset(totally_random_offset).get()) {
            kprintf("do_exec: Failure loading %s, entry pointer is invalid! (%p)\n", path.characters(), loader->entry().offset(totally_random_offset).get());
            return -ENOEXEC;
        }

        rollback_regions_guard.disarm();

        // NOTE: At this point, we've committed to the new executable.
        entry_eip = loader->entry().offset(totally_random_offset).get();

#ifdef EXEC_DEBUG
        kprintf("Memory layout after ELF load:");
        dump_regions();
#endif
    }

    m_elf_loader = move(loader);
    m_executable = main_program_description->custody();

    m_promises = m_execpromises;

    m_veil_state = VeilState::None;
    m_unveiled_paths.clear();

    // Copy of the master TLS region that we will clone for new threads
    m_master_tls_region = master_tls_region;

    auto main_program_metadata = main_program_description->metadata();

    if (!(main_program_description->custody()->mount_flags() & MS_NOSUID)) {
        if (main_program_metadata.is_setuid())
            m_euid = main_program_metadata.uid;
        if (main_program_metadata.is_setgid())
            m_egid = main_program_metadata.gid;
    }

    Thread::current->set_default_signal_dispositions();
    Thread::current->m_signal_mask = 0;
    Thread::current->m_pending_signals = 0;

    m_futex_queues.clear();

    m_region_lookup_cache = {};

    disown_all_shared_buffers();

    for (int i = 0; i < m_fds.size(); ++i) {
        auto& daf = m_fds[i];
        if (daf.description && daf.flags & FD_CLOEXEC) {
            daf.description->close();
            daf = {};
        }
    }

    Thread* new_main_thread = nullptr;
    if (Process::current == this) {
        new_main_thread = Thread::current;
    } else {
        for_each_thread([&](auto& thread) {
            new_main_thread = &thread;
            return IterationDecision::Break;
        });
    }
    ASSERT(new_main_thread);

    // NOTE: We create the new stack before disabling interrupts since it will zero-fault
    //       and we don't want to deal with faults after this point.
    u32 new_userspace_esp = new_main_thread->make_userspace_stack_for_main_thread(move(arguments), move(environment));

    // We cli() manually here because we don't want to get interrupted between do_exec() and Schedule::yield().
    // The reason is that the task redirection we've set up above will be clobbered by the timer IRQ.
    // If we used an InterruptDisabler that sti()'d on exit, we might timer tick'd too soon in exec().
    if (Process::current == this)
        cli();

    // NOTE: Be careful to not trigger any page faults below!

    Scheduler::prepare_to_modify_tss(*new_main_thread);

    m_name = parts.take_last();
    new_main_thread->set_name(m_name);

    auto& tss = new_main_thread->m_tss;

    u32 old_esp0 = tss.esp0;

    m_master_tls_size = master_tls_size;
    m_master_tls_alignment = master_tls_alignment;

    new_main_thread->make_thread_specific_region({});
    new_main_thread->reset_fpu_state();

    memset(&tss, 0, sizeof(TSS32));
    tss.iomapbase = sizeof(TSS32);

    tss.eflags = 0x0202;
    tss.eip = entry_eip;
    tss.cs = 0x1b;
    tss.ds = 0x23;
    tss.es = 0x23;
    tss.fs = 0x23;
    tss.gs = thread_specific_selector() | 3;
    tss.ss = 0x23;
    tss.cr3 = page_directory().cr3();
    tss.esp = new_userspace_esp;
    tss.ss0 = 0x10;
    tss.esp0 = old_esp0;
    tss.ss2 = m_pid;

#ifdef TASK_DEBUG
    kprintf("Process %u (%s) exec'd %s @ %p\n", pid(), name().characters(), path.characters(), tss.eip);
#endif

    if (was_profiling)
        Profiling::did_exec(path);

    new_main_thread->set_state(Thread::State::Skip1SchedulerPass);
    big_lock().force_unlock_if_locked();
    return 0;
}

static KResultOr<Vector<String>> find_shebang_interpreter_for_executable(const char first_page[], int nread)
{
    int word_start = 2;
    int word_length = 0;
    if (nread > 2 && first_page[0] == '#' && first_page[1] == '!') {
        Vector<String> interpreter_words;

        for (int i = 2; i < nread; ++i) {
            if (first_page[i] == '\n') {
                break;
            }

            if (first_page[i] != ' ') {
                ++word_length;
            }

            if (first_page[i] == ' ') {
                if (word_length > 0) {
                    interpreter_words.append(String(&first_page[word_start], word_length));
                }
                word_length = 0;
                word_start = i + 1;
            }
        }

        if (word_length > 0)
            interpreter_words.append(String(&first_page[word_start], word_length));

        if (!interpreter_words.is_empty())
            return interpreter_words;
    }

    return KResult(-ENOEXEC);
}

KResultOr<NonnullRefPtr<FileDescription>> Process::find_elf_interpreter_for_executable(const String& path, char (&first_page)[PAGE_SIZE], int nread, size_t file_size)
{
    if (nread < (int)sizeof(Elf32_Ehdr))
        return KResult(-ENOEXEC);

    auto elf_header = (Elf32_Ehdr*)first_page;
    if (!ELFImage::validate_elf_header(*elf_header, file_size)) {
        dbgprintf("%s(%d) exec(%s): File has invalid ELF header\n", m_name.characters(), m_pid, path.characters());
        return KResult(-ENOEXEC);
    }

    // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
    String interpreter_path;
    if (!ELFImage::validate_program_headers(*elf_header, file_size, (u8*)first_page, nread, interpreter_path)) {
        dbgprintf("%s(%d) exec(%s): File has invalid ELF Program headers\n", m_name.characters(), m_pid, path.characters());
        return KResult(-ENOEXEC);
    }

    if (!interpreter_path.is_empty()) {
        // Programs with an interpreter better be relocatable executables or we don't know what to do...
        if (elf_header->e_type != ET_DYN)
            return KResult(-ENOEXEC);

        dbgprintf("%s(%d) exec(%s): Using program interpreter %s\n", m_name.characters(), m_pid, path.characters(), interpreter_path.characters());
        auto interp_result = VFS::the().open(interpreter_path, O_EXEC, 0, current_directory());
        if (interp_result.is_error()) {
            dbgprintf("%s(%d) exec(%s): Unable to open program interpreter %s\n", m_name.characters(), m_pid, path.characters(), interpreter_path.characters());
            return interp_result.error();
        }
        auto interpreter_description = interp_result.value();
        auto interp_metadata = interpreter_description->metadata();

        ASSERT(interpreter_description->inode());

        // Validate the program interpreter as a valid elf binary.
        // If your program interpreter is a #! file or something, it's time to stop playing games :)
        if (interp_metadata.size < (int)sizeof(Elf32_Ehdr))
            return KResult(-ENOEXEC);

        memset(first_page, 0, sizeof(first_page));
        nread = interpreter_description->read((u8*)&first_page, sizeof(first_page));

        if (nread < (int)sizeof(Elf32_Ehdr))
            return KResult(-ENOEXEC);

        elf_header = (Elf32_Ehdr*)first_page;
        if (!ELFImage::validate_elf_header(*elf_header, interp_metadata.size)) {
            dbgprintf("%s(%d) exec(%s): Interpreter (%s) has invalid ELF header\n", m_name.characters(), m_pid, path.characters(), interpreter_description->absolute_path().characters());
            return KResult(-ENOEXEC);
        }

        // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
        String interpreter_interpreter_path;
        if (!ELFImage::validate_program_headers(*elf_header, interp_metadata.size, (u8*)first_page, nread, interpreter_interpreter_path)) {
            dbgprintf("%s(%d) exec(%s): Interpreter (%s) has invalid ELF Program headers\n", m_name.characters(), m_pid, path.characters(), interpreter_description->absolute_path().characters());
            return KResult(-ENOEXEC);
        }

        if (!interpreter_interpreter_path.is_empty()) {
            dbgprintf("%s(%d) exec(%s): Interpreter (%s) has its own interpreter (%s)! No thank you!\n",
                m_name.characters(), m_pid, path.characters(), interpreter_description->absolute_path().characters(), interpreter_interpreter_path.characters());
            return KResult(-ELOOP);
        }

        return interpreter_description;
    }

    if (elf_header->e_type != ET_EXEC) {
        // We can't exec an ET_REL, that's just an object file from the compiler
        // If it's ET_DYN with no PT_INTERP, then we can't load it properly either
        return KResult(-ENOEXEC);
    }

    // No interpreter, but, path refers to a valid elf image
    return KResult(KSuccess);
}

int Process::exec(String path, Vector<String> arguments, Vector<String> environment, int recursion_depth)
{
    if (recursion_depth > 2) {
        dbgprintf("%s(%d) exec(%s): SHENANIGANS! recursed too far trying to find #! interpreter\n", m_name.characters(), m_pid, path.characters());
        return -ELOOP;
    }

    // Open the file to check what kind of binary format it is
    // Currently supported formats:
    //    - #! interpreted file
    //    - ELF32
    //        * ET_EXEC binary that just gets loaded
    //        * ET_DYN binary that requires a program interpreter
    //
    auto result = VFS::the().open(path, O_EXEC, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();
    auto metadata = description->metadata();

    // Always gonna need at least 3 bytes. these are for #!X
    if (metadata.size < 3)
        return -ENOEXEC;

    ASSERT(description->inode());

    // Read the first page of the program into memory so we can validate the binfmt of it
    char first_page[PAGE_SIZE];
    int nread = description->read((u8*)&first_page, sizeof(first_page));

    // 1) #! interpreted file
    auto shebang_result = find_shebang_interpreter_for_executable(first_page, nread);
    if (!shebang_result.is_error()) {
        Vector<String> new_arguments(shebang_result.value());

        new_arguments.append(path);

        arguments.remove(0);
        new_arguments.append(move(arguments));

        return exec(shebang_result.value().first(), move(new_arguments), move(environment), ++recursion_depth);
    }

    // #2) ELF32 for i386
    auto elf_result = find_elf_interpreter_for_executable(path, first_page, nread, metadata.size);
    RefPtr<FileDescription> interpreter_description;
    // We're getting either an interpreter, an error, or KSuccess (i.e. no interpreter but file checks out)
    if (!elf_result.is_error())
        interpreter_description = elf_result.value();
    else if (elf_result.error().is_error())
        return elf_result.error();

    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    int rc = do_exec(move(description), move(arguments), move(environment), move(interpreter_description));
    if (rc < 0)
        return rc;

    if (Process::current == this) {
        Scheduler::yield();
        ASSERT_NOT_REACHED();
    }
    return 0;
}

int Process::sys$execve(const Syscall::SC_execve_params* user_params)
{
    REQUIRE_PROMISE(exec);
    // NOTE: Be extremely careful with allocating any kernel memory in exec().
    //       On success, the kernel stack will be lost.
    Syscall::SC_execve_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (params.arguments.length > ARG_MAX || params.environment.length > ARG_MAX)
        return -E2BIG;

    String path;
    {
        auto path_arg = get_syscall_path_argument(params.path);
        if (path_arg.is_error())
            return path_arg.error();
        path = path_arg.value();
    }

    auto copy_user_strings = [&](const auto& list, auto& output) {
        if (!list.length)
            return true;
        if (!validate_read_typed(list.strings, list.length))
            return false;
        Vector<Syscall::StringArgument, 32> strings;
        strings.resize(list.length);
        copy_from_user(strings.data(), list.strings, list.length * sizeof(Syscall::StringArgument));
        for (size_t i = 0; i < list.length; ++i) {
            auto string = validate_and_copy_string_from_user(strings[i]);
            if (string.is_null())
                return false;
            output.append(move(string));
        }
        return true;
    };

    Vector<String> arguments;
    if (!copy_user_strings(params.arguments, arguments))
        return -EFAULT;

    Vector<String> environment;
    if (!copy_user_strings(params.environment, environment))
        return -EFAULT;

    int rc = exec(move(path), move(arguments), move(environment));
    ASSERT(rc < 0); // We should never continue after a successful exec!
    return rc;
}

Process* Process::create_user_process(Thread*& first_thread, const String& path, uid_t uid, gid_t gid, pid_t parent_pid, int& error, Vector<String>&& arguments, Vector<String>&& environment, TTY* tty)
{
    auto parts = path.split('/');
    if (arguments.is_empty()) {
        arguments.append(parts.last());
    }
    RefPtr<Custody> cwd;
    RefPtr<Custody> root;
    {
        InterruptDisabler disabler;
        if (auto* parent = Process::from_pid(parent_pid)) {
            cwd = parent->m_cwd;
            root = parent->m_root_directory;
        }
    }

    if (!cwd)
        cwd = VFS::the().root_custody();

    if (!root)
        root = VFS::the().root_custody();

    auto* process = new Process(first_thread, parts.take_last(), uid, gid, parent_pid, Ring3, move(cwd), nullptr, tty);
    process->m_fds.resize(m_max_open_file_descriptors);
    auto& device_to_use_as_tty = tty ? (CharacterDevice&)*tty : NullDevice::the();
    auto description = device_to_use_as_tty.open(O_RDWR).value();
    process->m_fds[0].set(*description);
    process->m_fds[1].set(*description);
    process->m_fds[2].set(*description);

    error = process->exec(path, move(arguments), move(environment));
    if (error != 0) {
        delete process;
        return nullptr;
    }

    {
        InterruptDisabler disabler;
        g_processes->prepend(process);
    }
#ifdef TASK_DEBUG
    kprintf("Process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), first_thread->tss().eip);
#endif
    error = 0;
    return process;
}

Process* Process::create_kernel_process(Thread*& first_thread, String&& name, void (*e)())
{
    auto* process = new Process(first_thread, move(name), (uid_t)0, (gid_t)0, (pid_t)0, Ring0);
    first_thread->tss().eip = (uintptr_t)e;

    if (process->pid() != 0) {
        InterruptDisabler disabler;
        g_processes->prepend(process);
#ifdef TASK_DEBUG
        kprintf("Kernel process %u (%s) spawned @ %p\n", process->pid(), process->name().characters(), first_thread->tss().eip);
#endif
    }

    first_thread->set_state(Thread::State::Runnable);
    return process;
}

Process::Process(Thread*& first_thread, const String& name, uid_t uid, gid_t gid, pid_t ppid, RingLevel ring, RefPtr<Custody> cwd, RefPtr<Custody> executable, TTY* tty, Process* fork_parent)
    : m_name(move(name))
    , m_pid(allocate_pid())
    , m_uid(uid)
    , m_gid(gid)
    , m_euid(uid)
    , m_egid(gid)
    , m_ring(ring)
    , m_executable(move(executable))
    , m_cwd(move(cwd))
    , m_tty(tty)
    , m_ppid(ppid)
{
#ifdef PROCESS_DEBUG
    dbg() << "Created new process " << m_name << "(" << m_pid << ")";
#endif

    m_page_directory = PageDirectory::create_for_userspace(*this, fork_parent ? &fork_parent->page_directory().range_allocator() : nullptr);
#ifdef MM_DEBUG
    dbgprintf("Process %u ctor: PD=%x created\n", pid(), m_page_directory.ptr());
#endif

    if (fork_parent) {
        // NOTE: fork() doesn't clone all threads; the thread that called fork() becomes the only thread in the new process.
        first_thread = Thread::current->clone(*this);
    } else {
        // NOTE: This non-forked code path is only taken when the kernel creates a process "manually" (at boot.)
        first_thread = new Thread(*this);
    }
}

Process::~Process()
{
    ASSERT(thread_count() == 0);
}

void Process::dump_regions()
{
    kprintf("Process %s(%u) regions:\n", name().characters(), pid());
    kprintf("BEGIN       END         SIZE        ACCESS  NAME\n");
    for (auto& region : m_regions) {
        kprintf("%08x -- %08x    %08x    %c%c%c%c%c%c    %s\n",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.is_readable() ? 'R' : ' ',
            region.is_writable() ? 'W' : ' ',
            region.is_executable() ? 'X' : ' ',
            region.is_shared() ? 'S' : ' ',
            region.is_stack() ? 'T' : ' ',
            region.vmobject().is_purgeable() ? 'P' : ' ',
            region.name().characters());
    }

    MM.dump_kernel_regions();
}

void Process::sys$exit(int status)
{
    cli();
#ifdef TASK_DEBUG
    kprintf("sys$exit: %s(%u) exit with status %d\n", name().characters(), pid(), status);
#endif

    if (status != 0)
        dump_backtrace();

    m_termination_status = status;
    m_termination_signal = 0;
    die();
    Thread::current->die_if_needed();
    ASSERT_NOT_REACHED();
}

void signal_trampoline_dummy(void)
{
    // The trampoline preserves the current eax, pushes the signal code and
    // then calls the signal handler. We do this because, when interrupting a
    // blocking syscall, that syscall may return some special error code in eax;
    // This error code would likely be overwritten by the signal handler, so it's
    // neccessary to preserve it here.
    asm(
        ".intel_syntax noprefix\n"
        "asm_signal_trampoline:\n"
        "push ebp\n"
        "mov ebp, esp\n"
        "push eax\n"          // we have to store eax 'cause it might be the return value from a syscall
        "sub esp, 4\n"        // align the stack to 16 bytes
        "mov eax, [ebp+12]\n" // push the signal code
        "push eax\n"
        "call [ebp+8]\n" // call the signal handler
        "add esp, 8\n"
        "mov eax, %P0\n"
        "int 0x82\n" // sigreturn syscall
        "asm_signal_trampoline_end:\n"
        ".att_syntax" ::"i"(Syscall::SC_sigreturn));
}

extern "C" void asm_signal_trampoline(void);
extern "C" void asm_signal_trampoline_end(void);

void create_signal_trampolines()
{
    InterruptDisabler disabler;
    // NOTE: We leak this region.
    auto* trampoline_region = MM.allocate_user_accessible_kernel_region(PAGE_SIZE, "Signal trampolines", Region::Access::Read | Region::Access::Write | Region::Access::Execute, false).leak_ptr();
    g_return_to_ring3_from_signal_trampoline = trampoline_region->vaddr();

    u8* trampoline = (u8*)asm_signal_trampoline;
    u8* trampoline_end = (u8*)asm_signal_trampoline_end;
    size_t trampoline_size = trampoline_end - trampoline;

    {
        SmapDisabler disabler;
        u8* code_ptr = (u8*)trampoline_region->vaddr().as_ptr();
        memcpy(code_ptr, trampoline, trampoline_size);
    }

    trampoline_region->set_writable(false);
    trampoline_region->remap();
}

void create_kernel_info_page()
{
    auto* info_page_region_for_userspace = MM.allocate_user_accessible_kernel_region(PAGE_SIZE, "Kernel info page", Region::Access::Read).leak_ptr();
    auto* info_page_region_for_kernel = MM.allocate_kernel_region_with_vmobject(info_page_region_for_userspace->vmobject(), PAGE_SIZE, "Kernel info page", Region::Access::Read | Region::Access::Write).leak_ptr();
    s_info_page_address_for_userspace = info_page_region_for_userspace->vaddr();
    s_info_page_address_for_kernel = info_page_region_for_kernel->vaddr();
    memset(s_info_page_address_for_kernel.as_ptr(), 0, PAGE_SIZE);
}

int Process::sys$sigreturn(RegisterState& registers)
{
    REQUIRE_PROMISE(stdio);
    SmapDisabler disabler;

    //Here, we restore the state pushed by dispatch signal and asm_signal_trampoline.
    u32* stack_ptr = (u32*)registers.userspace_esp;
    u32 smuggled_eax = *stack_ptr;

    //pop the stored eax, ebp, return address, handler and signal code
    stack_ptr += 5;

    Thread::current->m_signal_mask = *stack_ptr;
    stack_ptr++;

    //pop edi, esi, ebp, esp, ebx, edx, ecx and eax
    memcpy(&registers.edi, stack_ptr, 8 * sizeof(uintptr_t));
    stack_ptr += 8;

    registers.eip = *stack_ptr;
    stack_ptr++;

    registers.eflags = *stack_ptr;
    stack_ptr++;

    registers.userspace_esp = registers.esp;
    return smuggled_eax;
}

void Process::crash(int signal, u32 eip)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!is_dead());
    ASSERT(Process::current == this);

    if (eip >= 0xc0000000 && ksyms_ready) {
        auto* ksym = ksymbolicate(eip);
        dbgprintf("\033[31;1m%p  %s +%d\033[0m\n", eip, ksym ? demangle(ksym->name).characters() : "(k?)", ksym ? eip - ksym->address : 0);
    } else if (m_elf_loader) {
        dbgprintf("\033[31;1m%p  %s\033[0m\n", eip, m_elf_loader->symbolicate(eip).characters());
    } else {
        dbgprintf("\033[31;1m%p  (?)\033[0m\n", eip);
    }
    dump_backtrace();

    m_termination_signal = signal;
    dump_regions();
    ASSERT(is_ring3());
    die();
    // We can not return from here, as there is nowhere
    // to unwind to, so die right away.
    Thread::current->die_if_needed();
    ASSERT_NOT_REACHED();
}

Process* Process::from_pid(pid_t pid)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto& process : *g_processes) {
        if (process.pid() == pid)
            return &process;
    }
    return nullptr;
}

RefPtr<FileDescription> Process::file_description(int fd) const
{
    if (fd < 0)
        return nullptr;
    if (fd < m_fds.size())
        return m_fds[fd].description.ptr();
    return nullptr;
}

int Process::fd_flags(int fd) const
{
    if (fd < 0)
        return -1;
    if (fd < m_fds.size())
        return m_fds[fd].flags;
    return -1;
}

ssize_t Process::sys$get_dir_entries(int fd, void* buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->get_dir_entries((u8*)buffer, size);
}

int Process::sys$lseek(int fd, off_t offset, int whence)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->seek(offset, whence);
}

int Process::sys$ttyname_r(int fd, char* buffer, ssize_t size)
{
    REQUIRE_PROMISE(tty);
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_tty())
        return -ENOTTY;
    String tty_name = description->tty()->tty_name();
    if ((size_t)size < tty_name.length() + 1)
        return -ERANGE;
    copy_to_user(buffer, tty_name.characters(), tty_name.length() + 1);
    return 0;
}

int Process::sys$ptsname_r(int fd, char* buffer, ssize_t size)
{
    REQUIRE_PROMISE(tty);
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    auto* master_pty = description->master_pty();
    if (!master_pty)
        return -ENOTTY;
    auto pts_name = master_pty->pts_name();
    if ((size_t)size < pts_name.length() + 1)
        return -ERANGE;
    copy_to_user(buffer, pts_name.characters(), pts_name.length() + 1);
    return 0;
}

ssize_t Process::sys$writev(int fd, const struct iovec* iov, int iov_count)
{
    REQUIRE_PROMISE(stdio);
    if (iov_count < 0)
        return -EINVAL;

    if (!validate_read_typed(iov, iov_count))
        return -EFAULT;

    u64 total_length = 0;
    Vector<iovec, 32> vecs;
    vecs.resize(iov_count);
    copy_from_user(vecs.data(), iov, iov_count * sizeof(iovec));
    for (auto& vec : vecs) {
        if (!validate_read(vec.iov_base, vec.iov_len))
            return -EFAULT;
        total_length += vec.iov_len;
        if (total_length > INT32_MAX)
            return -EINVAL;
    }

    auto description = file_description(fd);
    if (!description)
        return -EBADF;

    if (!description->is_writable())
        return -EBADF;

    int nwritten = 0;
    for (auto& vec : vecs) {
        int rc = do_write(*description, (const u8*)vec.iov_base, vec.iov_len);
        if (rc < 0) {
            if (nwritten == 0)
                return rc;
            return nwritten;
        }
        nwritten += rc;
    }

    return nwritten;
}

ssize_t Process::do_write(FileDescription& description, const u8* data, int data_size)
{
    ssize_t nwritten = 0;
    if (!description.is_blocking()) {
        if (!description.can_write())
            return -EAGAIN;
    }

    if (description.should_append()) {
#ifdef IO_DEBUG
        dbgprintf("seeking to end (O_APPEND)\n");
#endif
        description.seek(0, SEEK_END);
    }

    while (nwritten < data_size) {
#ifdef IO_DEBUG
        dbgprintf("while %u < %u\n", nwritten, size);
#endif
        if (!description.can_write()) {
#ifdef IO_DEBUG
            dbgprintf("block write on %d\n", fd);
#endif
            if (Thread::current->block<Thread::WriteBlocker>(description) != Thread::BlockResult::WokeNormally) {
                if (nwritten == 0)
                    return -EINTR;
            }
        }
        ssize_t rc = description.write(data + nwritten, data_size - nwritten);
#ifdef IO_DEBUG
        dbgprintf("   -> write returned %d\n", rc);
#endif
        if (rc < 0) {
            // FIXME: Support returning partial nwritten with errno.
            ASSERT(nwritten == 0);
            return rc;
        }
        if (rc == 0)
            break;
        nwritten += rc;
    }
    return nwritten;
}

ssize_t Process::sys$write(int fd, const u8* data, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;
    if (!validate_read(data, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u): sys$write(%d, %p, %u)\n", name().characters(), pid(), fd, data, size);
#endif
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_writable())
        return -EBADF;

    return do_write(*description, data, size);
}

ssize_t Process::sys$read(int fd, u8* buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;
    if (!validate_write(buffer, size))
        return -EFAULT;
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$read(%d, %p, %u)\n", name().characters(), pid(), fd, buffer, size);
#endif
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_readable())
        return -EBADF;
    if (description->is_directory())
        return -EISDIR;
    if (description->is_blocking()) {
        if (!description->can_read()) {
            if (Thread::current->block<Thread::ReadBlocker>(*description) != Thread::BlockResult::WokeNormally)
                return -EINTR;
            if (!description->can_read())
                return -EAGAIN;
        }
    }
    return description->read(buffer, size);
}

int Process::sys$close(int fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
#ifdef DEBUG_IO
    dbgprintf("%s(%u) sys$close(%d) %p\n", name().characters(), pid(), fd, description.ptr());
#endif
    if (!description)
        return -EBADF;
    int rc = description->close();
    m_fds[fd] = {};
    return rc;
}

int Process::sys$utime(const char* user_path, size_t path_length, const utimbuf* user_buf)
{
    REQUIRE_PROMISE(fattr);
    if (user_buf && !validate_read_typed(user_buf))
        return -EFAULT;
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    utimbuf buf;
    if (user_buf) {
        copy_from_user(&buf, user_buf);
    } else {
        auto now = kgettimeofday();
        buf = { now.tv_sec, now.tv_sec };
    }
    return VFS::the().utime(path.value(), current_directory(), buf.actime, buf.modtime);
}

int Process::sys$access(const char* user_path, size_t path_length, int mode)
{
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    return VFS::the().access(path.value(), mode, current_directory());
}

int Process::sys$fcntl(int fd, int cmd, u32 arg)
{
    REQUIRE_PROMISE(stdio);
#ifdef DEBUG_IO
    dbgprintf("sys$fcntl: fd=%d, cmd=%d, arg=%u\n", fd, cmd, arg);
#endif
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    // NOTE: The FD flags are not shared between FileDescription objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return -EINVAL;
        int new_fd = alloc_fd(arg_fd);
        if (new_fd < 0)
            return new_fd;
        m_fds[new_fd].set(*description);
        return new_fd;
    }
    case F_GETFD:
        return m_fds[fd].flags;
    case F_SETFD:
        m_fds[fd].flags = arg;
        break;
    case F_GETFL:
        return description->file_flags();
    case F_SETFL:
        description->set_file_flags(arg);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
    return 0;
}

int Process::sys$fstat(int fd, stat* statbuf)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(statbuf))
        return -EFAULT;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->fstat(*statbuf);
}

int Process::sys$stat(const Syscall::SC_stat_params* user_params)
{
    REQUIRE_PROMISE(rpath);
    Syscall::SC_stat_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    if (!validate_write_typed(params.statbuf))
        return -EFAULT;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    auto metadata_or_error = VFS::the().lookup_metadata(path.value(), current_directory(), params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR);
    if (metadata_or_error.is_error())
        return metadata_or_error.error();
    stat statbuf;
    auto result = metadata_or_error.value().stat(statbuf);
    if (result.is_error())
        return result;
    copy_to_user(params.statbuf, &statbuf);
    return 0;
}

template<typename DataType, typename SizeType>
bool Process::validate(const Syscall::MutableBufferArgument<DataType, SizeType>& buffer)
{
    return validate_write(buffer.data, buffer.size);
}

template<typename DataType, typename SizeType>
bool Process::validate(const Syscall::ImmutableBufferArgument<DataType, SizeType>& buffer)
{
    return validate_read(buffer.data, buffer.size);
}

String Process::validate_and_copy_string_from_user(const char* user_characters, size_t user_length) const
{
    if (user_length == 0)
        return String::empty();
    if (!user_characters)
        return {};
    if (!validate_read(user_characters, user_length))
        return {};
    SmapDisabler disabler;
    size_t measured_length = strnlen(user_characters, user_length);
    return String(user_characters, measured_length);
}

String Process::validate_and_copy_string_from_user(const Syscall::StringArgument& string) const
{
    return validate_and_copy_string_from_user(string.characters, string.length);
}

int Process::sys$readlink(const Syscall::SC_readlink_params* user_params)
{
    REQUIRE_PROMISE(rpath);

    Syscall::SC_readlink_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (!validate(params.buffer))
        return -EFAULT;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    auto result = VFS::the().open(path.value(), O_RDONLY | O_NOFOLLOW_NOERROR, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.value();

    if (!description->metadata().is_symlink())
        return -EINVAL;

    auto contents = description->read_entire_file();
    if (!contents)
        return -EIO; // FIXME: Get a more detailed error from VFS.

    auto link_target = String::copy(contents);
    if (link_target.length() + 1 > params.buffer.size)
        return -ENAMETOOLONG;
    copy_to_user(params.buffer.data, link_target.characters(), link_target.length() + 1);
    return link_target.length() + 1;
}

int Process::sys$chdir(const char* user_path, size_t path_length)
{
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto directory_or_error = VFS::the().open_directory(path.value(), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    m_cwd = *directory_or_error.value();
    return 0;
}

int Process::sys$fchdir(int fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
    if (!description)
        return -EBADF;

    if (!description->is_directory())
        return -ENOTDIR;

    if (!description->metadata().may_execute(*this))
        return -EACCES;

    m_cwd = description->custody();
    return 0;
}

int Process::sys$getcwd(char* buffer, ssize_t size)
{
    REQUIRE_PROMISE(rpath);
    if (size < 0)
        return -EINVAL;
    if (!validate_write(buffer, size))
        return -EFAULT;
    auto path = current_directory().absolute_path();
    if ((size_t)size < path.length() + 1)
        return -ERANGE;
    copy_to_user(buffer, path.characters(), path.length() + 1);
    return 0;
}

int Process::number_of_open_file_descriptors() const
{
    int count = 0;
    for (auto& description : m_fds) {
        if (description)
            ++count;
    }
    return count;
}

int Process::sys$open(const Syscall::SC_open_params* user_params)
{
    Syscall::SC_open_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    int dirfd = params.dirfd;
    int options = params.options;
    u16 mode = params.mode;

    if (options & O_NOFOLLOW_NOERROR)
        return -EINVAL;

    if (options & O_UNLINK_INTERNAL)
        return -EINVAL;

    if (options & O_WRONLY)
        REQUIRE_PROMISE(wpath);
    else if (options & O_RDONLY)
        REQUIRE_PROMISE(rpath);

    if (options & O_CREAT)
        REQUIRE_PROMISE(cpath);

    // Ignore everything except permission bits.
    mode &= 04777;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
#ifdef DEBUG_IO
    dbg() << "sys$open(dirfd=" << dirfd << ", path=\"" << path.value() << "\", options=" << options << ", mode=" << mode << ")";
#endif
    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = file_description(dirfd);
        if (!base_description)
            return -EBADF;
        if (!base_description->is_directory())
            return -ENOTDIR;
        if (!base_description->custody())
            return -EINVAL;
        base = base_description->custody();
    }

    auto result = VFS::the().open(path.value(), options, mode & ~umask(), *base);
    if (result.is_error())
        return result.error();
    auto description = result.value();
    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(description), fd_flags);
    return fd;
}

int Process::alloc_fd(int first_candidate_fd)
{
    for (int i = first_candidate_fd; i < (int)m_max_open_file_descriptors; ++i) {
        if (!m_fds[i])
            return i;
    }
    return -EMFILE;
}

int Process::sys$pipe(int pipefd[2], int flags)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(pipefd))
        return -EFAULT;
    if (number_of_open_file_descriptors() + 2 > max_open_file_descriptors())
        return -EMFILE;
    // Reject flags other than O_CLOEXEC.
    if ((flags & O_CLOEXEC) != flags)
        return -EINVAL;

    u32 fd_flags = (flags & O_CLOEXEC) ? FD_CLOEXEC : 0;
    auto fifo = FIFO::create(m_uid);

    int reader_fd = alloc_fd();
    m_fds[reader_fd].set(fifo->open_direction(FIFO::Direction::Reader), fd_flags);
    m_fds[reader_fd].description->set_readable(true);
    copy_to_user(&pipefd[0], &reader_fd);

    int writer_fd = alloc_fd();
    m_fds[writer_fd].set(fifo->open_direction(FIFO::Direction::Writer), fd_flags);
    m_fds[writer_fd].description->set_writable(true);
    copy_to_user(&pipefd[1], &writer_fd);

    return 0;
}

int Process::sys$killpg(int pgrp, int signum)
{
    REQUIRE_PROMISE(proc);
    if (signum < 1 || signum >= 32)
        return -EINVAL;
    if (pgrp < 0)
        return -EINVAL;

    InterruptDisabler disabler;
    return do_killpg(pgrp, signum);
}

int Process::sys$setuid(uid_t uid)
{
    REQUIRE_PROMISE(id);
    if (uid != m_uid && !is_superuser())
        return -EPERM;
    m_uid = uid;
    m_euid = uid;
    return 0;
}

int Process::sys$setgid(gid_t gid)
{
    REQUIRE_PROMISE(id);
    if (gid != m_gid && !is_superuser())
        return -EPERM;
    m_gid = gid;
    m_egid = gid;
    return 0;
}

unsigned Process::sys$alarm(unsigned seconds)
{
    REQUIRE_PROMISE(stdio);
    unsigned previous_alarm_remaining = 0;
    if (m_alarm_deadline && m_alarm_deadline > g_uptime) {
        previous_alarm_remaining = (m_alarm_deadline - g_uptime) / TICKS_PER_SECOND;
    }
    if (!seconds) {
        m_alarm_deadline = 0;
        return previous_alarm_remaining;
    }
    m_alarm_deadline = g_uptime + seconds * TICKS_PER_SECOND;
    return previous_alarm_remaining;
}

int Process::sys$uname(utsname* buf)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(buf))
        return -EFAULT;
    LOCKER(*s_hostname_lock);
    if (s_hostname->length() + 1 > sizeof(utsname::nodename))
        return -ENAMETOOLONG;
    copy_to_user(buf->sysname, "SerenityOS", 11);
    copy_to_user(buf->release, "1.0-dev", 8);
    copy_to_user(buf->version, "FIXME", 6);
    copy_to_user(buf->machine, "i686", 5);
    copy_to_user(buf->nodename, s_hostname->characters(), s_hostname->length() + 1);
    return 0;
}

KResult Process::do_kill(Process& process, int signal)
{
    // FIXME: Allow sending SIGCONT to everyone in the process group.
    // FIXME: Should setuid processes have some special treatment here?
    if (!is_superuser() && m_euid != process.m_uid && m_uid != process.m_uid)
        return KResult(-EPERM);
    if (process.is_ring0() && signal == SIGKILL) {
        kprintf("%s(%u) attempted to send SIGKILL to ring 0 process %s(%u)\n", name().characters(), m_pid, process.name().characters(), process.pid());
        return KResult(-EPERM);
    }
    if (signal != 0)
        process.send_signal(signal, this);
    return KSuccess;
}

KResult Process::do_killpg(pid_t pgrp, int signal)
{
    ASSERT(pgrp >= 0);

    // Send the signal to all processes in the given group.
    if (pgrp == 0) {
        // Send the signal to our own pgrp.
        pgrp = pgid();
    }

    bool group_was_empty = true;
    bool any_succeeded = false;
    KResult error = KSuccess;

    Process::for_each_in_pgrp(pgrp, [&](auto& process) {
        group_was_empty = false;

        KResult res = do_kill(process, signal);
        if (res.is_success())
            any_succeeded = true;
        else
            error = res;

        return IterationDecision::Continue;
    });

    if (group_was_empty)
        return KResult(-ESRCH);
    if (any_succeeded)
        return KSuccess;
    return error;
}

int Process::sys$kill(pid_t pid, int signal)
{
    if (pid == m_pid)
        REQUIRE_PROMISE(stdio);
    else
        REQUIRE_PROMISE(proc);

    if (signal < 0 || signal >= 32)
        return -EINVAL;
    if (pid <= 0)
        return do_killpg(-pid, signal);
    if (pid == -1) {
        // FIXME: Send to all processes.
        return -ENOTIMPL;
    }
    if (pid == m_pid) {
        if (signal == 0)
            return 0;
        if (!Thread::current->should_ignore_signal(signal)) {
            Thread::current->send_signal(signal, this);
            (void)Thread::current->block<Thread::SemiPermanentBlocker>(Thread::SemiPermanentBlocker::Reason::Signal);
        }
        return 0;
    }
    InterruptDisabler disabler;
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    return do_kill(*peer, signal);
}

int Process::sys$usleep(useconds_t usec)
{
    REQUIRE_PROMISE(stdio);
    if (!usec)
        return 0;
    u64 wakeup_time = Thread::current->sleep(usec / 1000);
    if (wakeup_time > g_uptime)
        return -EINTR;
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    REQUIRE_PROMISE(stdio);
    if (!seconds)
        return 0;
    u64 wakeup_time = Thread::current->sleep(seconds * TICKS_PER_SECOND);
    if (wakeup_time > g_uptime) {
        u32 ticks_left_until_original_wakeup_time = wakeup_time - g_uptime;
        return ticks_left_until_original_wakeup_time / TICKS_PER_SECOND;
    }
    return 0;
}

timeval kgettimeofday()
{
    return const_cast<const timeval&>(((KernelInfoPage*)s_info_page_address_for_kernel.as_ptr())->now);
}

void kgettimeofday(timeval& tv)
{
    tv = kgettimeofday();
}

int Process::sys$gettimeofday(timeval* tv)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(tv))
        return -EFAULT;
    *tv = kgettimeofday();
    return 0;
}

uid_t Process::sys$getuid()
{
    REQUIRE_PROMISE(stdio);
    return m_uid;
}

gid_t Process::sys$getgid()
{
    REQUIRE_PROMISE(stdio);
    return m_gid;
}

uid_t Process::sys$geteuid()
{
    REQUIRE_PROMISE(stdio);
    return m_euid;
}

gid_t Process::sys$getegid()
{
    REQUIRE_PROMISE(stdio);
    return m_egid;
}

pid_t Process::sys$getpid()
{
    REQUIRE_PROMISE(stdio);
    return m_pid;
}

pid_t Process::sys$getppid()
{
    REQUIRE_PROMISE(stdio);
    return m_ppid;
}

mode_t Process::sys$umask(mode_t mask)
{
    REQUIRE_PROMISE(stdio);
    auto old_mask = m_umask;
    m_umask = mask & 0777;
    return old_mask;
}

siginfo_t Process::reap(Process& process)
{
    siginfo_t siginfo;
    memset(&siginfo, 0, sizeof(siginfo));
    siginfo.si_signo = SIGCHLD;
    siginfo.si_pid = process.pid();
    siginfo.si_uid = process.uid();

    if (process.m_termination_signal) {
        siginfo.si_status = process.m_termination_signal;
        siginfo.si_code = CLD_KILLED;
    } else {
        siginfo.si_status = process.m_termination_status;
        siginfo.si_code = CLD_EXITED;
    }

    {
        InterruptDisabler disabler;

        if (process.ppid()) {
            auto* parent = Process::from_pid(process.ppid());
            if (parent) {
                parent->m_ticks_in_user_for_dead_children += process.m_ticks_in_user + process.m_ticks_in_user_for_dead_children;
                parent->m_ticks_in_kernel_for_dead_children += process.m_ticks_in_kernel + process.m_ticks_in_kernel_for_dead_children;
            }
        }

#ifdef PROCESS_DEBUG
        dbg() << "Reaping process " << process;
#endif
        ASSERT(process.is_dead());
        g_processes->remove(&process);
    }
    delete &process;
    return siginfo;
}

KResultOr<siginfo_t> Process::do_waitid(idtype_t idtype, int id, int options)
{
    if (idtype == P_PID) {
        InterruptDisabler disabler;
        if (idtype == P_PID && !Process::from_pid(id))
            return KResult(-ECHILD);
    }

    if (options & WNOHANG) {
        // FIXME: Figure out what WNOHANG should do with stopped children.
        if (idtype == P_ALL) {
            InterruptDisabler disabler;
            siginfo_t siginfo;
            memset(&siginfo, 0, sizeof(siginfo));
            for_each_child([&siginfo](Process& process) {
                if (process.is_dead())
                    siginfo = reap(process);
                return IterationDecision::Continue;
            });
            return siginfo;
        } else if (idtype == P_PID) {
            InterruptDisabler disabler;
            auto* waitee_process = Process::from_pid(id);
            if (!waitee_process)
                return KResult(-ECHILD);
            if (waitee_process->is_dead())
                return reap(*waitee_process);
        } else {
            // FIXME: Implement other PID specs.
            return KResult(-EINVAL);
        }
    }

    pid_t waitee_pid;

    // FIXME: WaitBlocker should support idtype/id specs directly.
    if (idtype == P_ALL) {
        waitee_pid = -1;
    } else if (idtype == P_PID) {
        waitee_pid = id;
    } else {
        // FIXME: Implement other PID specs.
        return KResult(-EINVAL);
    }

    if (Thread::current->block<Thread::WaitBlocker>(options, waitee_pid) != Thread::BlockResult::WokeNormally)
        return KResult(-EINTR);

    InterruptDisabler disabler;

    // NOTE: If waitee was -1, m_waitee_pid will have been filled in by the scheduler.
    Process* waitee_process = Process::from_pid(waitee_pid);
    if (!waitee_process)
        return KResult(-ECHILD);

    ASSERT(waitee_process);
    if (waitee_process->is_dead()) {
        return reap(*waitee_process);
    } else {
        auto* waitee_thread = Thread::from_tid(waitee_pid);
        if (!waitee_thread)
            return KResult(-ECHILD);
        ASSERT(waitee_thread->state() == Thread::State::Stopped);
        siginfo_t siginfo;
        memset(&siginfo, 0, sizeof(siginfo));
        siginfo.si_signo = SIGCHLD;
        siginfo.si_pid = waitee_process->pid();
        siginfo.si_uid = waitee_process->uid();
        siginfo.si_status = CLD_STOPPED;
        siginfo.si_code = waitee_thread->m_stop_signal;
        return siginfo;
    }
}

pid_t Process::sys$waitid(const Syscall::SC_waitid_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_waitid_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (!validate_write_typed(params.infop))
        return -EFAULT;

#ifdef PROCESS_DEBUG
    dbg() << "sys$waitid(" << params.idtype << ", " << params.id << ", " << params.infop << ", " << params.options << ")";
#endif

    auto siginfo_or_error = do_waitid(static_cast<idtype_t>(params.idtype), params.id, params.options);
    if (siginfo_or_error.is_error())
        return siginfo_or_error.error();

    copy_to_user(params.infop, &siginfo_or_error.value());
    return 0;
}

bool Process::validate_read_from_kernel(VirtualAddress vaddr, size_t size) const
{
    if (vaddr.is_null())
        return false;
    return MM.validate_kernel_read(*this, vaddr, size);
}

bool Process::validate_read(const void* address, size_t size) const
{
    if (!size)
        return false;
    return MM.validate_user_read(*this, VirtualAddress(address), size);
}

bool Process::validate_write(void* address, size_t size) const
{
    if (!size)
        return false;
    return MM.validate_user_write(*this, VirtualAddress(address), size);
}

pid_t Process::sys$getsid(pid_t pid)
{
    REQUIRE_PROMISE(stdio);
    if (pid == 0)
        return m_sid;
    InterruptDisabler disabler;
    auto* process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (m_sid != process->m_sid)
        return -EPERM;
    return process->m_sid;
}

pid_t Process::sys$setsid()
{
    REQUIRE_PROMISE(proc);
    InterruptDisabler disabler;
    bool found_process_with_same_pgid_as_my_pid = false;
    Process::for_each_in_pgrp(pid(), [&](auto&) {
        found_process_with_same_pgid_as_my_pid = true;
        return IterationDecision::Break;
    });
    if (found_process_with_same_pgid_as_my_pid)
        return -EPERM;
    m_sid = m_pid;
    m_pgid = m_pid;
    m_tty = nullptr;
    return m_sid;
}

pid_t Process::sys$getpgid(pid_t pid)
{
    REQUIRE_PROMISE(stdio);
    if (pid == 0)
        return m_pgid;
    InterruptDisabler disabler; // FIXME: Use a ProcessHandle
    auto* process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    return process->m_pgid;
}

pid_t Process::sys$getpgrp()
{
    REQUIRE_PROMISE(stdio);
    return m_pgid;
}

static pid_t get_sid_from_pgid(pid_t pgid)
{
    InterruptDisabler disabler;
    auto* group_leader = Process::from_pid(pgid);
    if (!group_leader)
        return -1;
    return group_leader->sid();
}

int Process::sys$setpgid(pid_t specified_pid, pid_t specified_pgid)
{
    REQUIRE_PROMISE(proc);
    InterruptDisabler disabler; // FIXME: Use a ProcessHandle
    pid_t pid = specified_pid ? specified_pid : m_pid;
    if (specified_pgid < 0) {
        // The value of the pgid argument is less than 0, or is not a value supported by the implementation.
        return -EINVAL;
    }
    auto* process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (process != this && process->ppid() != m_pid) {
        // The value of the pid argument does not match the process ID
        // of the calling process or of a child process of the calling process.
        return -ESRCH;
    }
    if (process->pid() == process->sid()) {
        // The process indicated by the pid argument is a session leader.
        return -EPERM;
    }
    if (process->ppid() == m_pid && process->sid() != sid()) {
        // The value of the pid argument matches the process ID of a child
        // process of the calling process and the child process is not in
        // the same session as the calling process.
        return -EPERM;
    }

    pid_t new_pgid = specified_pgid ? specified_pgid : process->m_pid;
    pid_t current_sid = get_sid_from_pgid(process->m_pgid);
    pid_t new_sid = get_sid_from_pgid(new_pgid);
    if (current_sid != new_sid) {
        // Can't move a process between sessions.
        return -EPERM;
    }
    // FIXME: There are more EPERM conditions to check for here..
    process->m_pgid = new_pgid;
    return 0;
}

int Process::sys$ioctl(int fd, unsigned request, unsigned arg)
{
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    SmapDisabler disabler;
    return description->file().ioctl(*description, request, arg);
}

int Process::sys$getdtablesize()
{
    REQUIRE_PROMISE(stdio);
    return m_max_open_file_descriptors;
}

int Process::sys$dup(int old_fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(old_fd);
    if (!description)
        return -EBADF;
    int new_fd = alloc_fd();
    if (new_fd < 0)
        return new_fd;
    m_fds[new_fd].set(*description);
    return new_fd;
}

int Process::sys$dup2(int old_fd, int new_fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(old_fd);
    if (!description)
        return -EBADF;
    if (new_fd < 0 || new_fd >= m_max_open_file_descriptors)
        return -EINVAL;
    m_fds[new_fd].set(*description);
    return new_fd;
}

int Process::sys$sigprocmask(int how, const sigset_t* set, sigset_t* old_set)
{
    REQUIRE_PROMISE(stdio);
    if (old_set) {
        if (!validate_write_typed(old_set))
            return -EFAULT;
        copy_to_user(old_set, &Thread::current->m_signal_mask);
    }
    if (set) {
        if (!validate_read_typed(set))
            return -EFAULT;
        sigset_t set_value;
        copy_from_user(&set_value, set);
        switch (how) {
        case SIG_BLOCK:
            Thread::current->m_signal_mask &= ~set_value;
            break;
        case SIG_UNBLOCK:
            Thread::current->m_signal_mask |= set_value;
            break;
        case SIG_SETMASK:
            Thread::current->m_signal_mask = set_value;
            break;
        default:
            return -EINVAL;
        }
    }
    return 0;
}

int Process::sys$sigpending(sigset_t* set)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(set))
        return -EFAULT;
    copy_to_user(set, &Thread::current->m_pending_signals);
    return 0;
}

int Process::sys$sigaction(int signum, const sigaction* act, sigaction* old_act)
{
    REQUIRE_PROMISE(stdio);
    if (signum < 1 || signum >= 32 || signum == SIGKILL || signum == SIGSTOP)
        return -EINVAL;
    if (!validate_read_typed(act))
        return -EFAULT;
    InterruptDisabler disabler; // FIXME: This should use a narrower lock. Maybe a way to ignore signals temporarily?
    auto& action = Thread::current->m_signal_action_data[signum];
    if (old_act) {
        if (!validate_write_typed(old_act))
            return -EFAULT;
        copy_to_user(&old_act->sa_flags, &action.flags);
        copy_to_user(&old_act->sa_sigaction, &action.handler_or_sigaction, sizeof(action.handler_or_sigaction));
    }
    copy_from_user(&action.flags, &act->sa_flags);
    copy_from_user(&action.handler_or_sigaction, &act->sa_sigaction, sizeof(action.handler_or_sigaction));
    return 0;
}

int Process::sys$getgroups(ssize_t count, gid_t* user_gids)
{
    REQUIRE_PROMISE(stdio);
    if (count < 0)
        return -EINVAL;
    if (!count)
        return m_extra_gids.size();
    if (count != (int)m_extra_gids.size())
        return -EINVAL;
    if (!validate_write_typed(user_gids, m_extra_gids.size()))
        return -EFAULT;

    Vector<gid_t> gids;
    for (auto gid : m_extra_gids)
        gids.append(gid);

    copy_to_user(user_gids, gids.data(), sizeof(gid_t) * count);
    return 0;
}

int Process::sys$setgroups(ssize_t count, const gid_t* user_gids)
{
    REQUIRE_PROMISE(id);
    if (count < 0)
        return -EINVAL;
    if (!is_superuser())
        return -EPERM;
    if (count && !validate_read(user_gids, count))
        return -EFAULT;

    Vector<gid_t> gids;
    gids.resize(count);
    copy_from_user(gids.data(), user_gids, sizeof(gid_t) * count);

    HashTable<gid_t> unique_extra_gids;
    for (auto& gid : gids) {
        if (gid != m_gid)
            unique_extra_gids.set(gid);
    }

    m_extra_gids.resize(unique_extra_gids.size());
    size_t i = 0;
    for (auto& gid : unique_extra_gids) {
        if (gid == m_gid)
            continue;
        m_extra_gids[i++] = gid;
    }
    return 0;
}

int Process::sys$mkdir(const char* user_path, size_t path_length, mode_t mode)
{
    REQUIRE_PROMISE(cpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    return VFS::the().mkdir(path.value(), mode & ~umask(), current_directory());
}

int Process::sys$realpath(const Syscall::SC_realpath_params* user_params)
{
    REQUIRE_PROMISE(rpath);

    Syscall::SC_realpath_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (!validate_write(params.buffer.data, params.buffer.size))
        return -EFAULT;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    auto custody_or_error = VFS::the().resolve_path(path.value(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = custody_or_error.value();
    auto absolute_path = custody->absolute_path();

    if (absolute_path.length() + 1 > params.buffer.size)
        return -ENAMETOOLONG;

    copy_to_user(params.buffer.data, absolute_path.characters(), absolute_path.length() + 1);
    return 0;
};

clock_t Process::sys$times(tms* times)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(times))
        return -EFAULT;
    copy_to_user(&times->tms_utime, &m_ticks_in_user);
    copy_to_user(&times->tms_stime, &m_ticks_in_kernel);
    copy_to_user(&times->tms_cutime, &m_ticks_in_user_for_dead_children);
    copy_to_user(&times->tms_cstime, &m_ticks_in_kernel_for_dead_children);
    return g_uptime & 0x7fffffff;
}

int Process::sys$select(const Syscall::SC_select_params* params)
{
    REQUIRE_PROMISE(stdio);
    // FIXME: Return -EINVAL if timeout is invalid.
    if (!validate_read_typed(params))
        return -EFAULT;

    SmapDisabler disabler;

    int nfds = params->nfds;
    fd_set* readfds = params->readfds;
    fd_set* writefds = params->writefds;
    fd_set* exceptfds = params->exceptfds;
    timeval* timeout = params->timeout;

    if (writefds && !validate_write_typed(writefds))
        return -EFAULT;
    if (readfds && !validate_write_typed(readfds))
        return -EFAULT;
    if (exceptfds && !validate_write_typed(exceptfds))
        return -EFAULT;
    if (timeout && !validate_read_typed(timeout))
        return -EFAULT;
    if (nfds < 0)
        return -EINVAL;

    timeval computed_timeout;
    bool select_has_timeout = false;
    if (timeout && (timeout->tv_sec || timeout->tv_usec)) {
        timeval_add(kgettimeofday(), *timeout, computed_timeout);
        select_has_timeout = true;
    }

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;
    Thread::SelectBlocker::FDVector efds;

    auto transfer_fds = [&](auto* fds, auto& vector) -> int {
        vector.clear_with_capacity();
        if (!fds)
            return 0;
        for (int fd = 0; fd < nfds; ++fd) {
            if (FD_ISSET(fd, fds)) {
                if (!file_description(fd)) {
                    dbg() << "sys$select: Bad fd number " << fd;
                    return -EBADF;
                }
                vector.append(fd);
            }
        }
        return 0;
    };
    if (int error = transfer_fds(writefds, wfds))
        return error;
    if (int error = transfer_fds(readfds, rfds))
        return error;
    if (int error = transfer_fds(exceptfds, efds))
        return error;

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> selecting on (read:%u, write:%u), timeout=%p\n", name().characters(), pid(), rfds.size(), wfds.size(), timeout);
#endif

    if (!timeout || select_has_timeout) {
        if (Thread::current->block<Thread::SelectBlocker>(computed_timeout, select_has_timeout, rfds, wfds, efds) != Thread::BlockResult::WokeNormally)
            return -EINTR;
    }

    int marked_fd_count = 0;
    auto mark_fds = [&](auto* fds, auto& vector, auto should_mark) {
        if (!fds)
            return;
        FD_ZERO(fds);
        for (int fd : vector) {
            if (auto description = file_description(fd); description && should_mark(*description)) {
                FD_SET(fd, fds);
                ++marked_fd_count;
            }
        }
    };
    mark_fds(readfds, rfds, [](auto& description) { return description.can_read(); });
    mark_fds(writefds, wfds, [](auto& description) { return description.can_write(); });
    // FIXME: We should also mark exceptfds as appropriate.

    return marked_fd_count;
}

int Process::sys$poll(pollfd* fds, int nfds, int timeout)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_read_typed(fds))
        return -EFAULT;

    SmapDisabler disabler;

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;

    for (int i = 0; i < nfds; ++i) {
        if (fds[i].events & POLLIN)
            rfds.append(fds[i].fd);
        if (fds[i].events & POLLOUT)
            wfds.append(fds[i].fd);
    }

    timeval actual_timeout;
    bool has_timeout = false;
    if (timeout >= 0) {
        // poll is in ms, we want s/us.
        struct timeval tvtimeout;
        tvtimeout.tv_sec = 0;
        while (timeout >= 1000) {
            tvtimeout.tv_sec += 1;
            timeout -= 1000;
        }
        tvtimeout.tv_usec = timeout * 1000;
        timeval_add(kgettimeofday(), tvtimeout, actual_timeout);
        has_timeout = true;
    }

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbgprintf("%s<%u> polling on (read:%u, write:%u), timeout=%d\n", name().characters(), pid(), rfds.size(), wfds.size(), timeout);
#endif

    if (has_timeout || timeout < 0) {
        if (Thread::current->block<Thread::SelectBlocker>(actual_timeout, has_timeout, rfds, wfds, Thread::SelectBlocker::FDVector()) != Thread::BlockResult::WokeNormally)
            return -EINTR;
    }

    int fds_with_revents = 0;

    for (int i = 0; i < nfds; ++i) {
        auto description = file_description(fds[i].fd);
        if (!description) {
            fds[i].revents = POLLNVAL;
            continue;
        }
        fds[i].revents = 0;
        if (fds[i].events & POLLIN && description->can_read())
            fds[i].revents |= POLLIN;
        if (fds[i].events & POLLOUT && description->can_write())
            fds[i].revents |= POLLOUT;

        if (fds[i].revents)
            ++fds_with_revents;
    }

    return fds_with_revents;
}

Custody& Process::current_directory()
{
    if (!m_cwd)
        m_cwd = VFS::the().root_custody();
    return *m_cwd;
}

int Process::sys$link(const Syscall::SC_link_params* user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_link_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    auto old_path = validate_and_copy_string_from_user(params.old_path);
    auto new_path = validate_and_copy_string_from_user(params.new_path);
    if (old_path.is_null() || new_path.is_null())
        return -EFAULT;
    return VFS::the().link(old_path, new_path, current_directory());
}

int Process::sys$unlink(const char* user_path, size_t path_length)
{
    REQUIRE_PROMISE(cpath);
    if (!validate_read(user_path, path_length))
        return -EFAULT;
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    return VFS::the().unlink(path.value(), current_directory());
}

int Process::sys$symlink(const Syscall::SC_symlink_params* user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_symlink_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    auto target = get_syscall_path_argument(params.target);
    if (target.is_error())
        return target.error();
    auto linkpath = get_syscall_path_argument(params.linkpath);
    if (linkpath.is_error())
        return linkpath.error();
    return VFS::the().symlink(target.value(), linkpath.value(), current_directory());
}

KResultOr<String> Process::get_syscall_path_argument(const char* user_path, size_t path_length) const
{
    if (path_length == 0)
        return KResult(-EINVAL);
    if (path_length > PATH_MAX)
        return KResult(-ENAMETOOLONG);
    if (!validate_read(user_path, path_length))
        return KResult(-EFAULT);
    return copy_string_from_user(user_path, path_length);
}

KResultOr<String> Process::get_syscall_path_argument(const Syscall::StringArgument& path) const
{
    return get_syscall_path_argument(path.characters, path.length);
}

int Process::sys$rmdir(const char* user_path, size_t path_length)
{
    REQUIRE_PROMISE(cpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    return VFS::the().rmdir(path.value(), current_directory());
}

int Process::sys$chmod(const char* user_path, size_t path_length, mode_t mode)
{
    REQUIRE_PROMISE(fattr);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    return VFS::the().chmod(path.value(), mode, current_directory());
}

int Process::sys$fchmod(int fd, mode_t mode)
{
    REQUIRE_PROMISE(fattr);
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->chmod(mode);
}

int Process::sys$fchown(int fd, uid_t uid, gid_t gid)
{
    REQUIRE_PROMISE(chown);
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    return description->chown(uid, gid);
}

int Process::sys$chown(const Syscall::SC_chown_params* user_params)
{
    REQUIRE_PROMISE(chown);
    Syscall::SC_chown_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().chown(path.value(), params.uid, params.gid, current_directory());
}

void Process::finalize()
{
    ASSERT(Thread::current == g_finalizer);
#ifdef PROCESS_DEBUG
    dbg() << "Finalizing process " << *this;
#endif

    if (m_perf_event_buffer) {
        auto description_or_error = VFS::the().open("perfcore", O_CREAT | O_EXCL, 0400, current_directory(), UidAndGid { m_uid, m_gid });
        if (!description_or_error.is_error()) {
            auto& description = description_or_error.value();
            auto json = m_perf_event_buffer->to_json(m_pid, m_executable ? m_executable->absolute_path() : "");
            description->write(json.data(), json.size());
        }
    }

    m_fds.clear();
    m_tty = nullptr;
    m_executable = nullptr;
    m_cwd = nullptr;
    m_root_directory = nullptr;
    m_root_directory_relative_to_global_root = nullptr;
    m_elf_loader = nullptr;

    disown_all_shared_buffers();
    {
        InterruptDisabler disabler;
        if (auto* parent_thread = Thread::from_tid(m_ppid)) {
            if (parent_thread->m_signal_action_data[SIGCHLD].flags & SA_NOCLDWAIT) {
                // NOTE: If the parent doesn't care about this process, let it go.
                m_ppid = 0;
            } else {
                parent_thread->send_signal(SIGCHLD, this);
            }
        }
    }

    m_regions.clear();

    m_dead = true;
}

void Process::die()
{
    // Let go of the TTY, otherwise a slave PTY may keep the master PTY from
    // getting an EOF when the last process using the slave PTY dies.
    // If the master PTY owner relies on an EOF to know when to wait() on a
    // slave owner, we have to allow the PTY pair to be torn down.
    m_tty = nullptr;

    if (m_tracer)
        m_tracer->set_dead();

    {
        // Tell the threads to unwind and die.
        InterruptDisabler disabler;
        for_each_thread([](Thread& thread) {
            thread.set_should_die();
            return IterationDecision::Continue;
        });
    }
}

size_t Process::amount_dirty_private() const
{
    // FIXME: This gets a bit more complicated for Regions sharing the same underlying VMObject.
    //        The main issue I'm thinking of is when the VMObject has physical pages that none of the Regions are mapping.
    //        That's probably a situation that needs to be looked at in general.
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (!region.is_shared())
            amount += region.amount_dirty();
    }
    return amount;
}

size_t Process::amount_clean_inode() const
{
    HashTable<const InodeVMObject*> vmobjects;
    for (auto& region : m_regions) {
        if (region.vmobject().is_inode())
            vmobjects.set(&static_cast<const InodeVMObject&>(region.vmobject()));
    }
    size_t amount = 0;
    for (auto& vmobject : vmobjects)
        amount += vmobject->amount_clean();
    return amount;
}

size_t Process::amount_virtual() const
{
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.size();
    }
    return amount;
}

size_t Process::amount_resident() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.amount_resident();
    }
    return amount;
}

size_t Process::amount_shared() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage ref counts,
    //        and each PhysicalPage is only reffed by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 ref to each of its PhysicalPages.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.amount_shared();
    }
    return amount;
}

size_t Process::amount_purgeable_volatile() const
{
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (region.vmobject().is_purgeable() && static_cast<const PurgeableVMObject&>(region.vmobject()).is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

size_t Process::amount_purgeable_nonvolatile() const
{
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (region.vmobject().is_purgeable() && !static_cast<const PurgeableVMObject&>(region.vmobject()).is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

#define REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain) \
    do {                                          \
        if (domain == AF_INET)                    \
            REQUIRE_PROMISE(inet);                \
        else if (domain == AF_LOCAL)              \
            REQUIRE_PROMISE(unix);                \
    } while (0)

int Process::sys$socket(int domain, int type, int protocol)
{
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain);

    if ((type & SOCK_TYPE_MASK) == SOCK_RAW && !is_superuser())
        return -EACCES;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto result = Socket::create(domain, type, protocol);
    if (result.is_error())
        return result.error();
    auto description = FileDescription::create(*result.value());
    description->set_readable(true);
    description->set_writable(true);
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        description->set_blocking(false);
    m_fds[fd].set(move(description), flags);
    return fd;
}

int Process::sys$bind(int sockfd, const sockaddr* address, socklen_t address_length)
{
    if (!validate_read(address, address_length))
        return -EFAULT;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.bind(address, address_length);
}

int Process::sys$listen(int sockfd, int backlog)
{
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    if (socket.is_connected())
        return -EINVAL;
    return socket.listen(backlog);
}

int Process::sys$accept(int accepting_socket_fd, sockaddr* user_address, socklen_t* user_address_size)
{
    REQUIRE_PROMISE(accept);
    if (!validate_write_typed(user_address_size))
        return -EFAULT;
    socklen_t address_size = 0;
    copy_from_user(&address_size, user_address_size);
    if (!validate_write(user_address, address_size))
        return -EFAULT;
    int accepted_socket_fd = alloc_fd();
    if (accepted_socket_fd < 0)
        return accepted_socket_fd;
    auto accepting_socket_description = file_description(accepting_socket_fd);
    if (!accepting_socket_description)
        return -EBADF;
    if (!accepting_socket_description->is_socket())
        return -ENOTSOCK;
    auto& socket = *accepting_socket_description->socket();
    if (!socket.can_accept()) {
        if (accepting_socket_description->is_blocking()) {
            if (Thread::current->block<Thread::AcceptBlocker>(*accepting_socket_description) != Thread::BlockResult::WokeNormally)
                return -EINTR;
        } else {
            return -EAGAIN;
        }
    }
    auto accepted_socket = socket.accept();
    ASSERT(accepted_socket);

    u8 address_buffer[sizeof(sockaddr_un)];
    address_size = min(sizeof(sockaddr_un), static_cast<size_t>(address_size));
    accepted_socket->get_peer_address((sockaddr*)address_buffer, &address_size);
    copy_to_user(user_address, address_buffer, address_size);
    copy_to_user(user_address_size, &address_size);

    auto accepted_socket_description = FileDescription::create(*accepted_socket);
    accepted_socket_description->set_readable(true);
    accepted_socket_description->set_writable(true);
    // NOTE: The accepted socket inherits fd flags from the accepting socket.
    //       I'm not sure if this matches other systems but it makes sense to me.
    accepted_socket_description->set_blocking(accepting_socket_description->is_blocking());
    m_fds[accepted_socket_fd].set(move(accepted_socket_description), m_fds[accepting_socket_fd].flags);

    // NOTE: Moving this state to Completed is what causes connect() to unblock on the client side.
    accepted_socket->set_setup_state(Socket::SetupState::Completed);
    return accepted_socket_fd;
}

int Process::sys$connect(int sockfd, const sockaddr* address, socklen_t address_size)
{
    if (!validate_read(address, address_size))
        return -EFAULT;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    SmapDisabler disabler;
    return socket.connect(*description, address, address_size, description->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No);
}

int Process::sys$shutdown(int sockfd, int how)
{
    REQUIRE_PROMISE(stdio);
    if (how & ~SHUT_RDWR)
        return -EINVAL;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.shutdown(how);
}

ssize_t Process::sys$sendto(const Syscall::SC_sendto_params* user_params)
{
    REQUIRE_PROMISE(stdio);
    Syscall::SC_sendto_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    int flags = params.flags;
    const sockaddr* addr = params.addr;
    socklen_t addr_length = params.addr_length;

    if (!validate(params.data))
        return -EFAULT;
    if (addr && !validate_read(addr, addr_length))
        return -EFAULT;
    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    if (socket.is_shut_down_for_writing())
        return -EPIPE;
    SmapDisabler disabler;
    return socket.sendto(*description, params.data.data, params.data.size, flags, addr, addr_length);
}

ssize_t Process::sys$recvfrom(const Syscall::SC_recvfrom_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_recvfrom_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    int flags = params.flags;
    sockaddr* addr = params.addr;
    socklen_t* addr_length = params.addr_length;

    SmapDisabler disabler;
    if (!validate(params.buffer))
        return -EFAULT;
    if (addr_length) {
        if (!validate_write_typed(addr_length))
            return -EFAULT;
        if (!validate_write(addr, *addr_length))
            return -EFAULT;
    } else if (addr) {
        return -EINVAL;
    }
    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();

    if (socket.is_shut_down_for_reading())
        return 0;

    bool original_blocking = description->is_blocking();
    if (flags & MSG_DONTWAIT)
        description->set_blocking(false);

    auto nrecv = socket.recvfrom(*description, params.buffer.data, params.buffer.size, flags, addr, addr_length);
    if (flags & MSG_DONTWAIT)
        description->set_blocking(original_blocking);

    return nrecv;
}

template<bool sockname, typename Params>
int Process::get_sock_or_peer_name(const Params& params)
{
    socklen_t addrlen_value;
    if (!validate_read_and_copy_typed(&addrlen_value, params.addrlen))
        return -EFAULT;

    if (addrlen_value <= 0)
        return -EINVAL;

    if (!validate_write(params.addr, addrlen_value))
        return -EFAULT;

    if (!validate_write_typed(params.addrlen))
        return -EFAULT;

    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;

    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    u8 address_buffer[sizeof(sockaddr_un)];
    addrlen_value = min(sizeof(sockaddr_un), static_cast<size_t>(addrlen_value));
    if constexpr (sockname)
        socket.get_local_address((sockaddr*)address_buffer, &addrlen_value);
    else
        socket.get_peer_address((sockaddr*)address_buffer, &addrlen_value);
    copy_to_user(params.addr, address_buffer, addrlen_value);
    copy_to_user(params.addrlen, &addrlen_value);
    return 0;
}

int Process::sys$getsockname(const Syscall::SC_getsockname_params* user_params)
{
    Syscall::SC_getsockname_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    return get_sock_or_peer_name<true>(params);
}

int Process::sys$getpeername(const Syscall::SC_getpeername_params* user_params)
{
    Syscall::SC_getpeername_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    return get_sock_or_peer_name<false>(params);
}

int Process::sys$sched_setparam(int tid, const struct sched_param* param)
{
    REQUIRE_PROMISE(proc);
    if (!validate_read_typed(param))
        return -EFAULT;

    int desired_priority;
    copy_from_user(&desired_priority, &param->sched_priority);

    InterruptDisabler disabler;
    auto* peer = Thread::current;
    if (tid != 0)
        peer = Thread::from_tid(tid);

    if (!peer)
        return -ESRCH;

    if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
        return -EPERM;

    if (desired_priority < THREAD_PRIORITY_MIN || desired_priority > THREAD_PRIORITY_MAX)
        return -EINVAL;

    peer->set_priority((u32)desired_priority);
    return 0;
}

int Process::sys$sched_getparam(pid_t pid, struct sched_param* param)
{
    REQUIRE_PROMISE(proc);
    if (!validate_write_typed(param))
        return -EFAULT;

    InterruptDisabler disabler;
    auto* peer = Thread::current;
    if (pid != 0)
        peer = Thread::from_tid(pid);

    if (!peer)
        return -ESRCH;

    if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
        return -EPERM;

    int priority = peer->priority();
    copy_to_user(&param->sched_priority, &priority);
    return 0;
}

int Process::sys$getsockopt(const Syscall::SC_getsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    SmapDisabler disabler;

    int sockfd = params->sockfd;
    int level = params->level;
    int option = params->option;
    void* value = params->value;
    socklen_t* value_size = params->value_size;

    if (!validate_write_typed(value_size))
        return -EFAULT;
    if (!validate_write(value, *value_size))
        return -EFAULT;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();

    if (has_promised(Pledge::accept) && socket.is_local() && level == SOL_SOCKET && option == SO_PEERCRED) {
        // We make an exception for SOL_SOCKET::SO_PEERCRED on local sockets if you've pledged "accept"
    } else {
        REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    }
    return socket.getsockopt(*description, level, option, value, value_size);
}

int Process::sys$setsockopt(const Syscall::SC_setsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    SmapDisabler disabler;

    int sockfd = params->sockfd;
    int level = params->level;
    int option = params->option;
    const void* value = params->value;
    socklen_t value_size = params->value_size;

    if (!validate_read(value, value_size))
        return -EFAULT;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.setsockopt(level, option, value, value_size);
}

void Process::disown_all_shared_buffers()
{
    LOCKER(shared_buffers().lock());
    Vector<SharedBuffer*, 32> buffers_to_disown;
    for (auto& it : shared_buffers().resource())
        buffers_to_disown.append(it.value.ptr());
    for (auto* shared_buffer : buffers_to_disown)
        shared_buffer->disown(m_pid);
}

int Process::sys$create_shared_buffer(int size, void** buffer)
{
    REQUIRE_PROMISE(shared_buffer);
    if (!size || size < 0)
        return -EINVAL;
    size = PAGE_ROUND_UP(size);
    if (!validate_write_typed(buffer))
        return -EFAULT;

    LOCKER(shared_buffers().lock());
    static int s_next_shared_buffer_id;
    int shared_buffer_id = ++s_next_shared_buffer_id;
    auto shared_buffer = make<SharedBuffer>(shared_buffer_id, size);
    shared_buffer->share_with(m_pid);

    void* address = shared_buffer->ref_for_process_and_get_address(*this);
    copy_to_user(buffer, &address);
    ASSERT((int)shared_buffer->size() >= size);
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Created shared buffer %d @ %p (%u bytes, vmobject is %u)\n", name().characters(), pid(), shared_buffer_id, *buffer, size, shared_buffer->size());
#endif
    shared_buffers().resource().set(shared_buffer_id, move(shared_buffer));

    return shared_buffer_id;
}

int Process::sys$share_buffer_with(int shared_buffer_id, pid_t peer_pid)
{
    REQUIRE_PROMISE(shared_buffer);
    if (!peer_pid || peer_pid < 0 || peer_pid == m_pid)
        return -EINVAL;
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    {
        InterruptDisabler disabler;
        auto* peer = Process::from_pid(peer_pid);
        if (!peer)
            return -ESRCH;
    }
    shared_buffer.share_with(peer_pid);
    return 0;
}

int Process::sys$share_buffer_globally(int shared_buffer_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    shared_buffer.share_globally();
    return 0;
}

int Process::sys$release_shared_buffer(int shared_buffer_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Releasing shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    shared_buffer.deref_for_process(*this);
    return 0;
}

void* Process::sys$get_shared_buffer(int shared_buffer_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return (void*)-EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return (void*)-EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Retaining shared buffer %d, buffer count: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.ref_for_process_and_get_address(*this);
}

int Process::sys$seal_shared_buffer(int shared_buffer_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Sealing shared buffer %d\n", name().characters(), pid(), shared_buffer_id);
#endif
    shared_buffer.seal();
    return 0;
}

int Process::sys$get_shared_buffer_size(int shared_buffer_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Get shared buffer %d size: %u\n", name().characters(), pid(), shared_buffer_id, shared_buffers().resource().size());
#endif
    return shared_buffer.size();
}

int Process::sys$set_shared_buffer_volatile(int shared_buffer_id, bool state)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shared_buffer_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    kprintf("%s(%u): Set shared buffer %d volatile: %u\n", name().characters(), pid(), shared_buffer_id, state);
#endif
    if (!state) {
        bool was_purged = shared_buffer.vmobject().was_purged();
        shared_buffer.vmobject().set_volatile(state);
        shared_buffer.vmobject().set_was_purged(false);
        return was_purged ? 1 : 0;
    }
    shared_buffer.vmobject().set_volatile(true);
    return 0;
}

void Process::terminate_due_to_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);
    dbgprintf("terminate_due_to_signal %s(%u) <- %u\n", name().characters(), pid(), signal);
    m_termination_status = 0;
    m_termination_signal = signal;
    die();
}

void Process::send_signal(u8 signal, Process* sender)
{
    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(m_pid);
    if (!thread)
        thread = &any_thread();
    thread->send_signal(signal, sender);
}

int Process::sys$create_thread(void* (*entry)(void*), void* argument, const Syscall::SC_create_thread_params* user_params)
{
    REQUIRE_PROMISE(thread);
    if (!validate_read((const void*)entry, sizeof(void*)))
        return -EFAULT;

    Syscall::SC_create_thread_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    unsigned detach_state = params.m_detach_state;
    int schedule_priority = params.m_schedule_priority;
    void* stack_location = params.m_stack_location;
    unsigned stack_size = params.m_stack_size;

    if (!validate_write(stack_location, stack_size))
        return -EFAULT;

    u32 user_stack_address = reinterpret_cast<u32>(stack_location) + stack_size;

    if (!MM.validate_user_stack(*this, VirtualAddress(user_stack_address - 4)))
        return -EFAULT;

    // FIXME: return EAGAIN if Thread::all_threads().size() is greater than PTHREAD_THREADS_MAX

    int requested_thread_priority = schedule_priority;
    if (requested_thread_priority < THREAD_PRIORITY_MIN || requested_thread_priority > THREAD_PRIORITY_MAX)
        return -EINVAL;

    bool is_thread_joinable = (0 == detach_state);

    // FIXME: Do something with guard pages?

    auto* thread = new Thread(*this);

    // We know this thread is not the main_thread,
    // So give it a unique name until the user calls $set_thread_name on it
    // length + 4 to give space for our extra junk at the end
    StringBuilder builder(m_name.length() + 4);
    builder.append(m_name);
    builder.appendf("[%d]", thread->tid());
    thread->set_name(builder.to_string());

    thread->set_priority(requested_thread_priority);
    thread->set_joinable(is_thread_joinable);

    auto& tss = thread->tss();
    tss.eip = (uintptr_t)entry;
    tss.eflags = 0x0202;
    tss.cr3 = page_directory().cr3();
    tss.esp = user_stack_address;

    // NOTE: The stack needs to be 16-byte aligned.
    thread->push_value_on_stack((uintptr_t)argument);
    thread->push_value_on_stack(0);

    thread->make_thread_specific_region({});
    thread->set_state(Thread::State::Runnable);
    return thread->tid();
}

void Process::sys$exit_thread(void* exit_value)
{
    REQUIRE_PROMISE(thread);
    cli();
    Thread::current->m_exit_value = exit_value;
    Thread::current->set_should_die();
    big_lock().force_unlock_if_locked();
    Thread::current->die_if_needed();
    ASSERT_NOT_REACHED();
}

int Process::sys$detach_thread(int tid)
{
    REQUIRE_PROMISE(thread);
    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    if (!thread->is_joinable())
        return -EINVAL;

    thread->set_joinable(false);
    return 0;
}

int Process::sys$join_thread(int tid, void** exit_value)
{
    REQUIRE_PROMISE(thread);
    if (exit_value && !validate_write_typed(exit_value))
        return -EFAULT;

    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    if (thread == Thread::current)
        return -EDEADLK;

    if (thread->m_joinee == Thread::current)
        return -EDEADLK;

    ASSERT(thread->m_joiner != Thread::current);
    if (thread->m_joiner)
        return -EINVAL;

    if (!thread->is_joinable())
        return -EINVAL;

    void* joinee_exit_value = nullptr;

    // NOTE: pthread_join() cannot be interrupted by signals. Only by death.
    for (;;) {
        auto result = Thread::current->block<Thread::JoinBlocker>(*thread, joinee_exit_value);
        if (result == Thread::BlockResult::InterruptedByDeath) {
            // NOTE: This cleans things up so that Thread::finalize() won't
            //       get confused about a missing joiner when finalizing the joinee.
            InterruptDisabler disabler;
            Thread::current->m_joinee->m_joiner = nullptr;
            Thread::current->m_joinee = nullptr;
            return 0;
        }
    }

    // NOTE: 'thread' is very possibly deleted at this point. Clear it just to be safe.
    thread = nullptr;

    if (exit_value)
        copy_to_user(exit_value, &joinee_exit_value);
    return 0;
}

int Process::sys$set_thread_name(int tid, const char* user_name, size_t user_name_length)
{
    REQUIRE_PROMISE(thread);
    auto name = validate_and_copy_string_from_user(user_name, user_name_length);
    if (name.is_null())
        return -EFAULT;

    const size_t max_thread_name_size = 64;
    if (name.length() > max_thread_name_size)
        return -EINVAL;

    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    thread->set_name(name);
    return 0;
}
int Process::sys$get_thread_name(int tid, char* buffer, size_t buffer_size)
{
    REQUIRE_PROMISE(thread);
    if (buffer_size == 0)
        return -EINVAL;

    if (!validate_write(buffer, buffer_size))
        return -EFAULT;

    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    if (thread->name().length() + 1 > (size_t)buffer_size)
        return -ENAMETOOLONG;

    copy_to_user(buffer, thread->name().characters(), thread->name().length() + 1);
    return 0;
}

int Process::sys$gettid()
{
    REQUIRE_PROMISE(stdio);
    return Thread::current->tid();
}

int Process::sys$donate(int tid)
{
    REQUIRE_PROMISE(stdio);
    if (tid < 0)
        return -EINVAL;
    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;
    Scheduler::donate_to(thread, "sys$donate");
    return 0;
}

int Process::sys$rename(const Syscall::SC_rename_params* user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_rename_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    auto old_path = get_syscall_path_argument(params.old_path);
    if (old_path.is_error())
        return old_path.error();
    auto new_path = get_syscall_path_argument(params.new_path);
    if (new_path.is_error())
        return new_path.error();
    return VFS::the().rename(old_path.value(), new_path.value(), current_directory());
}

int Process::sys$ftruncate(int fd, off_t length)
{
    REQUIRE_PROMISE(stdio);
    if (length < 0)
        return -EINVAL;
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_writable())
        return -EBADF;
    return description->truncate(static_cast<u64>(length));
}

int Process::sys$watch_file(const char* user_path, size_t path_length)
{
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();

    auto custody_or_error = VFS::the().resolve_path(path.value(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = custody_or_error.value();
    auto& inode = custody->inode();

    if (!inode.fs().supports_watchers())
        return -ENOTSUP;

    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    m_fds[fd].set(FileDescription::create(*InodeWatcher::create(inode)));
    m_fds[fd].description->set_readable(true);
    return fd;
}

int Process::sys$systrace(pid_t pid)
{
    REQUIRE_PROMISE(proc);
    InterruptDisabler disabler;
    auto* peer = Process::from_pid(pid);
    if (!peer)
        return -ESRCH;
    if (peer->uid() != m_euid)
        return -EACCES;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto description = FileDescription::create(peer->ensure_tracer());
    description->set_readable(true);
    m_fds[fd].set(move(description), 0);
    return fd;
}

int Process::sys$halt()
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    dbgprintf("acquiring FS locks...\n");
    FS::lock_all();
    dbgprintf("syncing mounted filesystems...\n");
    FS::sync();
    dbgprintf("attempting system shutdown...\n");
    IO::out16(0x604, 0x2000);

    return ESUCCESS;
}

int Process::sys$reboot()
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    dbgprintf("acquiring FS locks...\n");
    FS::lock_all();
    dbgprintf("syncing mounted filesystems...\n");
    FS::sync();
    dbgprintf("attempting reboot via KB Controller...\n");
    IO::out8(0x64, 0xFE);

    return ESUCCESS;
}

int Process::sys$mount(const Syscall::SC_mount_params* user_params)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    Syscall::SC_mount_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    auto source = validate_and_copy_string_from_user(params.source);
    auto target = validate_and_copy_string_from_user(params.target);
    auto fs_type = validate_and_copy_string_from_user(params.fs_type);

    if (source.is_null() || target.is_null() || fs_type.is_null())
        return -EFAULT;

    dbg() << "mount " << fs_type << ": source " << source << " @ " << target;

    auto custody_or_error = VFS::the().resolve_path(target, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& target_custody = custody_or_error.value();

    RefPtr<FS> fs;

    if (params.flags & MS_BIND) {
        // We're doing a bind mount.
        auto source_or_error = VFS::the().resolve_path(source, current_directory());
        if (source_or_error.is_error())
            return source_or_error.error();
        auto& source_custody = source_or_error.value();
        return VFS::the().bind_mount(source_custody, target_custody, params.flags);
    }

    if (fs_type == "ext2" || fs_type == "Ext2FS") {
        auto source_or_error = VFS::the().open(source, O_RDWR, 0, current_directory());
        if (source_or_error.is_error())
            return source_or_error.error();

        auto* device = source_or_error.value()->device();
        if (!device || !device->is_block_device()) {
            dbg() << "mount: this is not a BlockDevice";
            return -ENODEV;
        }
        auto& block_device = static_cast<BlockDevice&>(*device);

        dbg() << "mount: attempting to mount " << block_device.absolute_path() << " on " << target;

        fs = Ext2FS::create(block_device);
    } else if (fs_type == "proc" || fs_type == "ProcFS") {
        fs = ProcFS::create();
    } else if (fs_type == "devpts" || fs_type == "DevPtsFS") {
        fs = DevPtsFS::create();
    } else if (fs_type == "tmp" || fs_type == "TmpFS") {
        fs = TmpFS::create();
    } else {
        return -ENODEV;
    }

    if (!fs->initialize()) {
        dbg() << "mount: failed to initialize " << fs_type << " filesystem on " << source;
        return -ENODEV;
    }

    auto result = VFS::the().mount(fs.release_nonnull(), target_custody, params.flags);
    dbg() << "mount: successfully mounted " << source << " on " << target;
    return result;
}

int Process::sys$umount(const char* user_mountpoint, size_t mountpoint_length)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    if (!validate_read(user_mountpoint, mountpoint_length))
        return -EFAULT;

    auto mountpoint = get_syscall_path_argument(user_mountpoint, mountpoint_length);
    if (mountpoint.is_error())
        return mountpoint.error();

    auto metadata_or_error = VFS::the().lookup_metadata(mountpoint.value(), current_directory());
    if (metadata_or_error.is_error())
        return metadata_or_error.error();

    auto guest_inode_id = metadata_or_error.value().inode;
    return VFS::the().unmount(guest_inode_id);
}

ProcessTracer& Process::ensure_tracer()
{
    if (!m_tracer)
        m_tracer = ProcessTracer::create(m_pid);
    return *m_tracer;
}

void Process::FileDescriptionAndFlags::clear()
{
    description = nullptr;
    flags = 0;
}

void Process::FileDescriptionAndFlags::set(NonnullRefPtr<FileDescription>&& d, u32 f)
{
    description = move(d);
    flags = f;
}

int Process::sys$mknod(const Syscall::SC_mknod_params* user_params)
{
    REQUIRE_PROMISE(dpath);
    Syscall::SC_mknod_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    if (!is_superuser() && !is_regular_file(params.mode) && !is_fifo(params.mode) && !is_socket(params.mode))
        return -EPERM;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().mknod(path.value(), params.mode & ~umask(), params.dev, current_directory());
}

int Process::sys$dump_backtrace()
{
    dump_backtrace();
    return 0;
}

int Process::sys$dbgputch(u8 ch)
{
    IO::out8(0xe9, ch);
    return 0;
}

int Process::sys$dbgputstr(const u8* characters, int length)
{
    if (!length)
        return 0;
    if (!validate_read(characters, length))
        return -EFAULT;
    SmapDisabler disabler;
    for (int i = 0; i < length; ++i)
        IO::out8(0xe9, characters[i]);
    return 0;
}

KBuffer Process::backtrace(ProcessInspectionHandle& handle) const
{
    KBufferBuilder builder;
    for_each_thread([&](Thread& thread) {
        builder.appendf("Thread %d (%s):\n", thread.tid(), thread.name().characters());
        builder.append(thread.backtrace(handle));
        return IterationDecision::Continue;
    });
    return builder.build();
}

int Process::sys$set_process_icon(int icon_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(icon_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    m_icon_id = icon_id;
    return 0;
}

int Process::sys$get_process_name(char* buffer, int buffer_size)
{
    REQUIRE_PROMISE(stdio);
    if (buffer_size <= 0)
        return -EINVAL;

    if (!validate_write(buffer, buffer_size))
        return -EFAULT;

    if (m_name.length() + 1 > (size_t)buffer_size)
        return -ENAMETOOLONG;

    copy_to_user(buffer, m_name.characters(), m_name.length() + 1);
    return 0;
}

// We don't use the flag yet, but we could use it for distinguishing
// random source like Linux, unlike the OpenBSD equivalent. However, if we
// do, we should be able of the caveats that Linux has dealt with.
int Process::sys$getrandom(void* buffer, size_t buffer_size, unsigned int flags __attribute__((unused)))
{
    REQUIRE_PROMISE(stdio);
    if (buffer_size <= 0)
        return -EINVAL;

    if (!validate_write(buffer, buffer_size))
        return -EFAULT;

    SmapDisabler disabler;
    get_good_random_bytes((u8*)buffer, buffer_size);
    return 0;
}

int Process::sys$setkeymap(const Syscall::SC_setkeymap_params* user_params)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;
    Syscall::SC_setkeymap_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    const char* map = params.map;
    const char* shift_map = params.shift_map;
    const char* alt_map = params.alt_map;
    const char* altgr_map = params.altgr_map;

    if (!validate_read(map, 0x80))
        return -EFAULT;
    if (!validate_read(shift_map, 0x80))
        return -EFAULT;
    if (!validate_read(alt_map, 0x80))
        return -EFAULT;
    if (!validate_read(altgr_map, 0x80))
        return -EFAULT;

    SmapDisabler disabler;
    KeyboardDevice::the().set_maps(map, shift_map, alt_map, altgr_map);
    return 0;
}

int Process::sys$clock_gettime(clockid_t clock_id, timespec* user_ts)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(user_ts))
        return -EFAULT;

    timespec ts;
    memset(&ts, 0, sizeof(ts));

    switch (clock_id) {
    case CLOCK_MONOTONIC:
        ts.tv_sec = g_uptime / TICKS_PER_SECOND;
        ts.tv_nsec = (g_uptime % TICKS_PER_SECOND) * 1000000;
        break;
    default:
        return -EINVAL;
    }

    copy_to_user(user_ts, &ts);
    return 0;
}

int Process::sys$clock_nanosleep(const Syscall::SC_clock_nanosleep_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_clock_nanosleep_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (params.requested_sleep && !validate_read_typed(params.requested_sleep))
        return -EFAULT;

    timespec requested_sleep;
    copy_from_user(&requested_sleep, params.requested_sleep);

    if (params.remaining_sleep && !validate_write_typed(params.remaining_sleep))
        return -EFAULT;

    bool is_absolute = params.flags & TIMER_ABSTIME;

    switch (params.clock_id) {
    case CLOCK_MONOTONIC: {
        u64 wakeup_time;
        if (is_absolute) {
            u64 time_to_wake = (requested_sleep.tv_sec * 1000 + requested_sleep.tv_nsec / 1000000);
            wakeup_time = Thread::current->sleep_until(time_to_wake);
        } else {
            u32 ticks_to_sleep = (requested_sleep.tv_sec * 1000 + requested_sleep.tv_nsec / 1000000);
            if (!ticks_to_sleep)
                return 0;
            wakeup_time = Thread::current->sleep(ticks_to_sleep);
        }
        if (wakeup_time > g_uptime) {
            u32 ticks_left = wakeup_time - g_uptime;
            if (!is_absolute && params.remaining_sleep) {
                timespec remaining_sleep;
                memset(&remaining_sleep, 0, sizeof(timespec));
                remaining_sleep.tv_sec = ticks_left / TICKS_PER_SECOND;
                ticks_left -= remaining_sleep.tv_sec * TICKS_PER_SECOND;
                remaining_sleep.tv_nsec = ticks_left * 1000000;
                copy_to_user(params.remaining_sleep, &remaining_sleep);
            }
            return -EINTR;
        }
        return 0;
    }
    default:
        return -EINVAL;
    }
}

int Process::sys$sync()
{
    REQUIRE_PROMISE(stdio);
    VFS::the().sync();
    return 0;
}

int Process::sys$yield()
{
    REQUIRE_PROMISE(stdio);
    Thread::current->yield_without_holding_big_lock();
    return 0;
}

int Process::sys$beep()
{
    PCSpeaker::tone_on(440);
    u64 wakeup_time = Thread::current->sleep(100);
    PCSpeaker::tone_off();
    if (wakeup_time > g_uptime)
        return -EINTR;
    return 0;
}

int Process::sys$module_load(const char* user_path, size_t path_length)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto description_or_error = VFS::the().open(path.value(), O_RDONLY, 0, current_directory());
    if (description_or_error.is_error())
        return description_or_error.error();
    auto& description = description_or_error.value();
    auto payload = description->read_entire_file();
    auto storage = KBuffer::create_with_size(payload.size());
    memcpy(storage.data(), payload.data(), payload.size());
    payload.clear();

    auto elf_image = make<ELFImage>(storage.data(), storage.size());
    if (!elf_image->parse())
        return -ENOEXEC;

    HashMap<String, u8*> section_storage_by_name;

    auto module = make<Module>();

    elf_image->for_each_section_of_type(SHT_PROGBITS, [&](const ELFImage::Section& section) {
        auto section_storage = KBuffer::copy(section.raw_data(), section.size(), Region::Access::Read | Region::Access::Write | Region::Access::Execute);
        section_storage_by_name.set(section.name(), section_storage.data());
        module->sections.append(move(section_storage));
        return IterationDecision::Continue;
    });

    bool missing_symbols = false;

    elf_image->for_each_section_of_type(SHT_PROGBITS, [&](const ELFImage::Section& section) {
        auto* section_storage = section_storage_by_name.get(section.name()).value_or(nullptr);
        ASSERT(section_storage);
        section.relocations().for_each_relocation([&](const ELFImage::Relocation& relocation) {
            auto& patch_ptr = *reinterpret_cast<ptrdiff_t*>(section_storage + relocation.offset());
            switch (relocation.type()) {
            case R_386_PC32: {
                // PC-relative relocation
                dbg() << "PC-relative relocation: " << relocation.symbol().name();
                u32 symbol_address = address_for_kernel_symbol(relocation.symbol().name());
                if (symbol_address == 0)
                    missing_symbols = true;
                dbg() << "   Symbol address: " << (void*)symbol_address;
                ptrdiff_t relative_offset = (char*)symbol_address - ((char*)&patch_ptr + 4);
                patch_ptr = relative_offset;
                break;
            }
            case R_386_32: // Absolute relocation
                dbg() << "Absolute relocation: '" << relocation.symbol().name() << "' value:" << relocation.symbol().value() << ", index:" << relocation.symbol_index();

                if (relocation.symbol().bind() == STB_LOCAL) {
                    auto* section_storage_containing_symbol = section_storage_by_name.get(relocation.symbol().section().name()).value_or(nullptr);
                    ASSERT(section_storage_containing_symbol);
                    u32 symbol_address = (ptrdiff_t)(section_storage_containing_symbol + relocation.symbol().value());
                    if (symbol_address == 0)
                        missing_symbols = true;
                    dbg() << "   Symbol address: " << (void*)symbol_address;
                    patch_ptr += symbol_address;
                } else if (relocation.symbol().bind() == STB_GLOBAL) {
                    u32 symbol_address = address_for_kernel_symbol(relocation.symbol().name());
                    if (symbol_address == 0)
                        missing_symbols = true;
                    dbg() << "   Symbol address: " << (void*)symbol_address;
                    patch_ptr += symbol_address;
                } else {
                    ASSERT_NOT_REACHED();
                }
                break;
            }
            return IterationDecision::Continue;
        });

        return IterationDecision::Continue;
    });

    if (missing_symbols)
        return -EINVAL;

    auto* text_base = section_storage_by_name.get(".text").value_or(nullptr);
    if (!text_base) {
        dbg() << "No .text section found in module!";
        return -EINVAL;
    }

    elf_image->for_each_symbol([&](const ELFImage::Symbol& symbol) {
        dbg() << " - " << symbol.type() << " '" << symbol.name() << "' @ " << (void*)symbol.value() << ", size=" << symbol.size();
        if (symbol.name() == "module_init") {
            module->module_init = (ModuleInitPtr)(text_base + symbol.value());
        } else if (symbol.name() == "module_fini") {
            module->module_fini = (ModuleFiniPtr)(text_base + symbol.value());
        } else if (symbol.name() == "module_name") {
            const u8* storage = section_storage_by_name.get(symbol.section().name()).value_or(nullptr);
            if (storage)
                module->name = String((const char*)(storage + symbol.value()));
        }
        return IterationDecision::Continue;
    });

    if (!module->module_init)
        return -EINVAL;

    if (g_modules->contains(module->name)) {
        dbg() << "a module with the name " << module->name << " is already loaded; please unload it first";
        return -EEXIST;
    }

    module->module_init();

    auto name = module->name;
    g_modules->set(name, move(module));

    return 0;
}

int Process::sys$module_unload(const char* user_name, size_t name_length)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    auto module_name = validate_and_copy_string_from_user(user_name, name_length);
    if (module_name.is_null())
        return -EFAULT;

    auto it = g_modules->find(module_name);
    if (it == g_modules->end())
        return -ENOENT;

    if (it->value->module_fini)
        it->value->module_fini();

    g_modules->remove(it);
    return 0;
}

int Process::sys$profiling_enable(pid_t pid)
{
    REQUIRE_NO_PROMISES;
    InterruptDisabler disabler;
    auto* process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (!is_superuser() && process->uid() != m_uid)
        return -EPERM;
    Profiling::start(*process);
    process->set_profiling(true);
    return 0;
}

int Process::sys$profiling_disable(pid_t pid)
{
    InterruptDisabler disabler;
    auto* process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (!is_superuser() && process->uid() != m_uid)
        return -EPERM;
    process->set_profiling(false);
    Profiling::stop();
    return 0;
}

void* Process::sys$get_kernel_info_page()
{
    REQUIRE_PROMISE(stdio);
    return s_info_page_address_for_userspace.as_ptr();
}

Thread& Process::any_thread()
{
    Thread* found_thread = nullptr;
    for_each_thread([&](auto& thread) {
        found_thread = &thread;
        return IterationDecision::Break;
    });
    ASSERT(found_thread);
    return *found_thread;
}

WaitQueue& Process::futex_queue(i32* userspace_address)
{
    auto& queue = m_futex_queues.ensure((uintptr_t)userspace_address);
    if (!queue)
        queue = make<WaitQueue>();
    return *queue;
}

int Process::sys$futex(const Syscall::SC_futex_params* user_params)
{
    REQUIRE_PROMISE(thread);

    Syscall::SC_futex_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    i32* userspace_address = params.userspace_address;
    int futex_op = params.futex_op;
    i32 value = params.val;
    const timespec* user_timeout = params.timeout;

    if (!validate_read_typed(userspace_address))
        return -EFAULT;

    if (user_timeout && !validate_read_typed(user_timeout))
        return -EFAULT;

    timespec timeout { 0, 0 };
    if (user_timeout)
        copy_from_user(&timeout, user_timeout);

    i32 user_value;

    switch (futex_op) {
    case FUTEX_WAIT:
        copy_from_user(&user_value, userspace_address);
        if (user_value != value)
            return -EAGAIN;
        // FIXME: This is supposed to be interruptible by a signal, but right now WaitQueue cannot be interrupted.
        // FIXME: Support timeout!
        Thread::current->wait_on(futex_queue(userspace_address));
        break;
    case FUTEX_WAKE:
        if (value == 0)
            return 0;
        if (value == 1) {
            futex_queue(userspace_address).wake_one();
        } else {
            // FIXME: Wake exactly (value) waiters.
            futex_queue(userspace_address).wake_all();
        }
        break;
    }

    return 0;
}

int Process::sys$set_thread_boost(int tid, int amount)
{
    REQUIRE_PROMISE(proc);
    if (amount < 0 || amount > 20)
        return -EINVAL;
    InterruptDisabler disabler;
    auto* thread = Thread::from_tid(tid);
    if (!thread)
        return -ESRCH;
    if (thread->state() == Thread::State::Dead || thread->state() == Thread::State::Dying)
        return -ESRCH;
    if (!is_superuser() && thread->process().uid() != euid())
        return -EPERM;
    thread->set_priority_boost(amount);
    return 0;
}

int Process::sys$set_process_boost(pid_t pid, int amount)
{
    REQUIRE_PROMISE(proc);
    if (amount < 0 || amount > 20)
        return -EINVAL;
    InterruptDisabler disabler;
    auto* process = Process::from_pid(pid);
    if (!process || process->is_dead())
        return -ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return -EPERM;
    process->m_priority_boost = amount;
    return 0;
}

int Process::sys$chroot(const char* user_path, size_t path_length, int mount_flags)
{
    if (!is_superuser())
        return -EPERM;
    REQUIRE_PROMISE(chroot);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto directory_or_error = VFS::the().open_directory(path.value(), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    auto directory = directory_or_error.value();
    m_root_directory_relative_to_global_root = directory;
    int chroot_mount_flags = mount_flags == -1 ? directory->mount_flags() : mount_flags;
    set_root_directory(Custody::create(nullptr, "", directory->inode(), chroot_mount_flags));
    return 0;
}

Custody& Process::root_directory()
{
    if (!m_root_directory)
        m_root_directory = VFS::the().root_custody();
    return *m_root_directory;
}

Custody& Process::root_directory_relative_to_global_root()
{
    if (!m_root_directory_relative_to_global_root)
        m_root_directory_relative_to_global_root = root_directory();
    return *m_root_directory_relative_to_global_root;
}

void Process::set_root_directory(const Custody& root)
{
    m_root_directory = root;
}

int Process::sys$pledge(const Syscall::SC_pledge_params* user_params)
{
    Syscall::SC_pledge_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (params.promises.length > 1024 || params.execpromises.length > 1024)
        return -E2BIG;

    String promises;
    if (params.promises.characters) {
        promises = validate_and_copy_string_from_user(params.promises);
        if (promises.is_null())
            return -EFAULT;
    }

    String execpromises;
    if (params.execpromises.characters) {
        execpromises = validate_and_copy_string_from_user(params.execpromises);
        if (execpromises.is_null())
            return -EFAULT;
    }

    auto parse_pledge = [&](auto& pledge_spec, u32& mask) {
        auto parts = pledge_spec.split_view(' ');
        for (auto& part : parts) {
#define __ENUMERATE_PLEDGE_PROMISE(x)   \
    if (part == #x) {                   \
        mask |= (1u << (u32)Pledge::x); \
        continue;                       \
    }
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
            if (part == "dns") {
                // "dns" is an alias for "unix" since DNS queries go via LookupServer
                mask |= (1u << (u32)Pledge::unix);
                continue;
            }
            return false;
        }
        return true;
    };

    if (!promises.is_null()) {
        u32 new_promises = 0;
        if (!parse_pledge(promises, new_promises))
            return -EINVAL;
        if (m_promises && (!new_promises || new_promises & ~m_promises))
            return -EPERM;
        m_promises = new_promises;
    }

    if (!execpromises.is_null()) {
        u32 new_execpromises = 0;
        if (!parse_pledge(execpromises, new_execpromises))
            return -EINVAL;
        if (m_execpromises && (!new_execpromises || new_execpromises & ~m_execpromises))
            return -EPERM;
        m_execpromises = new_execpromises;
    }

    return 0;
}

Region& Process::add_region(NonnullOwnPtr<Region> region)
{
    auto* ptr = region.ptr();
    m_regions.append(move(region));
    return *ptr;
}

int Process::sys$unveil(const Syscall::SC_unveil_params* user_params)
{
    Syscall::SC_unveil_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (!params.path.characters && !params.permissions.characters) {
        m_veil_state = VeilState::Locked;
        return 0;
    }

    if (m_veil_state == VeilState::Locked)
        return -EPERM;

    if (!params.path.characters || !params.permissions.characters)
        return -EINVAL;

    if (params.permissions.length > 4)
        return -EINVAL;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    if (path.value().is_empty() || path.value().characters()[0] != '/')
        return -EINVAL;

    auto permissions = validate_and_copy_string_from_user(params.permissions);
    if (permissions.is_null())
        return -EFAULT;

    unsigned new_permissions = 0;
    for (size_t i = 0; i < permissions.length(); ++i) {
        switch (permissions[i]) {
        case 'r':
            new_permissions |= UnveiledPath::Access::Read;
            break;
        case 'w':
            new_permissions |= UnveiledPath::Access::Write;
            break;
        case 'x':
            new_permissions |= UnveiledPath::Access::Execute;
            break;
        case 'c':
            new_permissions |= UnveiledPath::Access::CreateOrRemove;
            break;
        default:
            return -EINVAL;
        }
    }

    for (int i = 0; i < m_unveiled_paths.size(); ++i) {
        auto& unveiled_path = m_unveiled_paths[i];
        if (unveiled_path.path == path.value()) {
            if (new_permissions & ~unveiled_path.permissions)
                return -EPERM;
            unveiled_path.permissions = new_permissions;
            return 0;
        }
    }

    m_unveiled_paths.append({ path.value(), new_permissions });
    ASSERT(m_veil_state != VeilState::Locked);
    m_veil_state = VeilState::Dropped;
    return 0;
}

int Process::sys$perf_event(int type, uintptr_t arg1, uintptr_t arg2)
{
    if (!m_perf_event_buffer)
        m_perf_event_buffer = make<PerformanceEventBuffer>();
    return m_perf_event_buffer->append(type, arg1, arg2);
}

void Process::set_tty(TTY* tty)
{
    m_tty = tty;
}

}
