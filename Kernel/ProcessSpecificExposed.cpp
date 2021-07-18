/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

class ProcFSProcessStacks;
class ProcFSThreadStack final : public ProcFSProcessInformation {
public:
    // Note: We pass const ProcFSProcessStacks& to enforce creation with this type of directory
    static NonnullRefPtr<ProcFSThreadStack> create(const ProcFSProcessDirectory& process_directory, const ProcFSProcessStacks&, const Thread& thread)
    {
        return adopt_ref(*new ProcFSThreadStack(process_directory, thread));
    }

private:
    explicit ProcFSThreadStack(const ProcFSProcessDirectory& process_directory, const Thread& thread)
        : ProcFSProcessInformation(String::formatted("{}", thread.tid()), process_directory)
        , m_associated_thread(thread)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        JsonArraySerializer array { builder };
        bool show_kernel_addresses = Process::current()->is_superuser();
        bool kernel_address_added = false;
        for (auto address : Processor::capture_stack_trace(*m_associated_thread, 1024)) {
            if (!show_kernel_addresses && !is_user_address(VirtualAddress { address })) {
                if (kernel_address_added)
                    continue;
                address = 0xdeadc0de;
                kernel_address_added = true;
            }
            array.add(address);
        }

        array.finish();
        return true;
    }

    NonnullRefPtr<Thread> m_associated_thread;
};

class ProcFSProcessStacks final : public ProcFSExposedDirectory {
    // Note: This directory is special, because everything that is created here is dynamic!
    // This means we don't register anything in the m_components Vector, and every inode
    // is created in runtime when called to get it
    // Every ProcFSThreadStack (that represents a thread stack) is created only as a temporary object
    // therefore, we don't use m_components so when we are done with the ProcFSThreadStack object,
    // It should be deleted (as soon as possible)
public:
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    static NonnullRefPtr<ProcFSProcessStacks> create(const ProcFSProcessDirectory& parent_directory)
    {
        auto directory = adopt_ref(*new ProcFSProcessStacks(parent_directory));
        return directory;
    }

    virtual void prepare_for_deletion() override
    {
        ProcFSExposedDirectory::prepare_for_deletion();
        m_process_directory.clear();
    }

private:
    ProcFSProcessStacks(const ProcFSProcessDirectory& parent_directory)
        : ProcFSExposedDirectory("stacks"sv, parent_directory)
        , m_process_directory(parent_directory)
    {
    }
    WeakPtr<ProcFSProcessDirectory> m_process_directory;
    mutable Mutex m_lock;
};

KResult ProcFSProcessStacks::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_lock);
    auto parent_directory = m_process_directory.strong_ref();
    if (parent_directory.is_null())
        return KResult(EINVAL);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, parent_directory->component_index() }, 0 });

    auto process = parent_directory->associated_process();
    if (process.is_null())
        return KResult(ESRCH);
    process->for_each_thread([&](const Thread& thread) {
        int tid = thread.tid().value();
        InodeIdentifier identifier = { fsid, thread.global_procfs_inode_index() };
        callback({ String::number(tid), identifier, 0 });
    });
    return KSuccess;
}

RefPtr<ProcFSExposedComponent> ProcFSProcessStacks::lookup(StringView name)
{
    MutexLocker locker(m_lock);
    auto parent_directory = m_process_directory.strong_ref();
    if (parent_directory.is_null())
        return nullptr;
    auto process = parent_directory->associated_process();
    if (process.is_null())
        return nullptr;
    RefPtr<ProcFSThreadStack> procfd_stack;
    // FIXME: Try to exit the loop earlier
    process->for_each_thread([&](const Thread& thread) {
        int tid = thread.tid().value();
        if (name == String::number(tid)) {
            procfd_stack = ProcFSThreadStack::create(*parent_directory, *this, thread);
        }
    });
    return procfd_stack;
}

class ProcFSProcessFileDescriptions;
class ProcFSProcessFileDescription final : public ProcFSExposedLink {
public:
    // Note: we pass const ProcFSProcessFileDescriptions& just to enforce creation of this in the correct directory.
    static NonnullRefPtr<ProcFSProcessFileDescription> create(unsigned fd_number, const FileDescription& fd, InodeIndex preallocated_index, const ProcFSProcessFileDescriptions&)
    {
        return adopt_ref(*new ProcFSProcessFileDescription(fd_number, fd, preallocated_index));
    }

private:
    explicit ProcFSProcessFileDescription(unsigned fd_number, const FileDescription& fd, InodeIndex preallocated_index)
        : ProcFSExposedLink(String::formatted("{}", fd_number), preallocated_index)
        , m_associated_file_description(fd)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        builder.append_bytes(m_associated_file_description->absolute_path().bytes());
        return true;
    }

    NonnullRefPtr<FileDescription> m_associated_file_description;
};

class ProcFSProcessFileDescriptions final : public ProcFSExposedDirectory {
    // Note: This directory is special, because everything that is created here is dynamic!
    // This means we don't register anything in the m_components Vector, and every inode
    // is created in runtime when called to get it
    // Every ProcFSProcessFileDescription (that represents a file descriptor) is created only as a temporary object
    // therefore, we don't use m_components so when we are done with the ProcFSProcessFileDescription object,
    // It should be deleted (as soon as possible)
public:
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    static NonnullRefPtr<ProcFSProcessFileDescriptions> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessFileDescriptions(parent_directory));
    }

    virtual void prepare_for_deletion() override
    {
        ProcFSExposedDirectory::prepare_for_deletion();
        m_process_directory.clear();
    }

private:
    explicit ProcFSProcessFileDescriptions(const ProcFSProcessDirectory& parent_directory)
        : ProcFSExposedDirectory("fd"sv, parent_directory)
        , m_process_directory(parent_directory)
    {
    }
    WeakPtr<ProcFSProcessDirectory> m_process_directory;
    mutable Mutex m_lock;
};

KResult ProcFSProcessFileDescriptions::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_lock);
    auto parent_directory = m_process_directory.strong_ref();
    if (parent_directory.is_null())
        return KResult(EINVAL);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, parent_directory->component_index() }, 0 });

    auto process = parent_directory->associated_process();
    if (process.is_null())
        return KResult(ESRCH);
    size_t count = 0;
    process->fds().enumerate([&](auto& file_description_metadata) {
        if (!file_description_metadata.is_valid()) {
            count++;
            return;
        }
        InodeIdentifier identifier = { fsid, file_description_metadata.global_procfs_inode_index() };
        callback({ String::number(count), identifier, 0 });
        count++;
    });
    return KSuccess;
}
RefPtr<ProcFSExposedComponent> ProcFSProcessFileDescriptions::lookup(StringView name)
{
    MutexLocker locker(m_lock);
    auto parent_directory = m_process_directory.strong_ref();
    if (parent_directory.is_null())
        return nullptr;
    auto process = parent_directory->associated_process();
    if (process.is_null())
        return nullptr;
    RefPtr<ProcFSProcessFileDescription> procfd_fd;
    // FIXME: Try to exit the loop earlier
    size_t count = 0;
    process->fds().enumerate([&](auto& file_description_metadata) {
        if (!file_description_metadata.is_valid()) {
            count++;
            return;
        }
        if (name == String::number(count)) {
            procfd_fd = ProcFSProcessFileDescription::create(count, *file_description_metadata.description(), file_description_metadata.global_procfs_inode_index(), *this);
        }
        count++;
    });
    return procfd_fd;
}

class ProcFSProcessPledge final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessPledge> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessPledge(parent_directory));
    }

private:
    explicit ProcFSProcessPledge(const ProcFSProcessDirectory& parent_directory)
        : ProcFSProcessInformation("pledge"sv, parent_directory)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        JsonObjectSerializer obj { builder };
#define __ENUMERATE_PLEDGE_PROMISE(x)       \
    if (process->has_promised(Pledge::x)) { \
        if (!builder.is_empty())            \
            builder.append(' ');            \
        builder.append(#x);                 \
    }
        if (process->has_promises()) {
            StringBuilder builder;
            ENUMERATE_PLEDGE_PROMISES
            obj.add("promises", builder.build());
        }
#undef __ENUMERATE_PLEDGE_PROMISE
        obj.finish();
        return true;
    }
};

class ProcFSProcessUnveil final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessUnveil> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessUnveil(parent_directory));
    }

private:
    explicit ProcFSProcessUnveil(const ProcFSProcessDirectory& parent_directory)
        : ProcFSProcessInformation("unveil"sv, parent_directory)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        JsonArraySerializer array { builder };
        for (auto& unveiled_path : process->unveiled_paths()) {
            if (!unveiled_path.was_explicitly_unveiled())
                continue;
            auto obj = array.add_object();
            obj.add("path", unveiled_path.path());
            StringBuilder permissions_builder;
            if (unveiled_path.permissions() & UnveilAccess::Read)
                permissions_builder.append('r');
            if (unveiled_path.permissions() & UnveilAccess::Write)
                permissions_builder.append('w');
            if (unveiled_path.permissions() & UnveilAccess::Execute)
                permissions_builder.append('x');
            if (unveiled_path.permissions() & UnveilAccess::CreateOrRemove)
                permissions_builder.append('c');
            if (unveiled_path.permissions() & UnveilAccess::Browse)
                permissions_builder.append('b');
            obj.add("permissions", permissions_builder.to_string());
        }
        array.finish();
        return true;
    }
};

class ProcFSProcessPerformanceEvents final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessPerformanceEvents> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessPerformanceEvents(parent_directory));
    }

private:
    explicit ProcFSProcessPerformanceEvents(const ProcFSProcessDirectory& parent_directory)
        : ProcFSProcessInformation("perf_events"sv, parent_directory)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        InterruptDisabler disabler;
        auto parent_directory = m_parent_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        if (!process->perf_events()) {
            dbgln("ProcFS: No perf events for {}", process->pid());
            return false;
        }
        return process->perf_events()->to_json(builder);
    }
};

class ProcFSProcessOverallFileDescriptions final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessOverallFileDescriptions> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessOverallFileDescriptions(parent_directory));
    }

private:
    explicit ProcFSProcessOverallFileDescriptions(const ProcFSProcessDirectory& parent_directory)
        : ProcFSProcessInformation("fds"sv, parent_directory)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        JsonArraySerializer array { builder };
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        if (process->fds().open_count() == 0) {
            array.finish();
            return true;
        }

        size_t count = 0;
        process->fds().enumerate([&](auto& file_description_metadata) {
            if (!file_description_metadata.is_valid()) {
                count++;
                return;
            }
            bool cloexec = file_description_metadata.flags() & FD_CLOEXEC;
            RefPtr<FileDescription> description = file_description_metadata.description();
            auto description_object = array.add_object();
            description_object.add("fd", count);
            description_object.add("absolute_path", description->absolute_path());
            description_object.add("seekable", description->file().is_seekable());
            description_object.add("class", description->file().class_name());
            description_object.add("offset", description->offset());
            description_object.add("cloexec", cloexec);
            description_object.add("blocking", description->is_blocking());
            description_object.add("can_read", description->can_read());
            description_object.add("can_write", description->can_write());
            count++;
        });

        array.finish();
        return true;
    }
};

class ProcFSProcessRoot final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessRoot> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessRoot(parent_directory));
    }

private:
    explicit ProcFSProcessRoot(const ProcFSProcessDirectory& parent_directory)
        : ProcFSExposedLink("root"sv)
        , m_parent_process_directory(parent_directory)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_process_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        builder.append_bytes(process->root_directory_relative_to_global_root().absolute_path().to_byte_buffer());
        return true;
    }
    WeakPtr<ProcFSProcessDirectory> m_parent_process_directory;
};

class ProcFSProcessVirtualMemory final : public ProcFSProcessInformation {
public:
    static NonnullRefPtr<ProcFSProcessRoot> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessVirtualMemory(parent_directory));
    }

private:
    explicit ProcFSProcessVirtualMemory(const ProcFSProcessDirectory& parent_directory)
        : ProcFSProcessInformation("vm"sv, parent_directory)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        JsonArraySerializer array { builder };
        {
            ScopedSpinLock lock(process->space().get_lock());
            for (auto& region : process->space().regions()) {
                if (!region->is_user() && !Process::current()->is_superuser())
                    continue;
                auto region_object = array.add_object();
                region_object.add("readable", region->is_readable());
                region_object.add("writable", region->is_writable());
                region_object.add("executable", region->is_executable());
                region_object.add("stack", region->is_stack());
                region_object.add("shared", region->is_shared());
                region_object.add("syscall", region->is_syscall_region());
                region_object.add("purgeable", region->vmobject().is_anonymous());
                if (region->vmobject().is_anonymous()) {
                    region_object.add("volatile", static_cast<const AnonymousVMObject&>(region->vmobject()).is_any_volatile());
                }
                region_object.add("cacheable", region->is_cacheable());
                region_object.add("address", region->vaddr().get());
                region_object.add("size", region->size());
                region_object.add("amount_resident", region->amount_resident());
                region_object.add("amount_dirty", region->amount_dirty());
                region_object.add("cow_pages", region->cow_pages());
                region_object.add("name", region->name());
                region_object.add("vmobject", region->vmobject().class_name());

                StringBuilder pagemap_builder;
                for (size_t i = 0; i < region->page_count(); ++i) {
                    auto* page = region->physical_page(i);
                    if (!page)
                        pagemap_builder.append('N');
                    else if (page->is_shared_zero_page() || page->is_lazy_committed_page())
                        pagemap_builder.append('Z');
                    else
                        pagemap_builder.append('P');
                }
                region_object.add("pagemap", pagemap_builder.to_string());
            }
        }
        array.finish();
        return true;
    }
};

class ProcFSProcessCurrentWorkDirectory final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessCurrentWorkDirectory> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessCurrentWorkDirectory(parent_directory));
    }

private:
    explicit ProcFSProcessCurrentWorkDirectory(const ProcFSProcessDirectory& parent_directory)
        : ProcFSExposedLink("cwd"sv)
        , m_parent_process_directory(parent_directory)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_process_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        builder.append_bytes(process->current_directory().absolute_path().bytes());
        return true;
    }

    WeakPtr<ProcFSProcessDirectory> m_parent_process_directory;
};

class ProcFSProcessBinary final : public ProcFSExposedLink {
public:
    static NonnullRefPtr<ProcFSProcessBinary> create(const ProcFSProcessDirectory& parent_directory)
    {
        return adopt_ref(*new ProcFSProcessBinary(parent_directory));
    }

    virtual mode_t required_mode() const override
    {
        auto parent_directory = m_parent_process_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        if (!process->executable())
            return 0;
        return ProcFSExposedComponent::required_mode();
    }

private:
    explicit ProcFSProcessBinary(const ProcFSProcessDirectory& parent_directory)
        : ProcFSExposedLink("exe"sv)
        , m_parent_process_directory(parent_directory)
    {
    }
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        auto parent_directory = m_parent_process_directory.strong_ref();
        if (parent_directory.is_null())
            return false;
        auto process = parent_directory->associated_process();
        if (process.is_null())
            return false;
        auto* custody = process->executable();
        if (!custody)
            return false;
        builder.append(custody->absolute_path().bytes());
        return true;
    }

    WeakPtr<ProcFSProcessDirectory> m_parent_process_directory;
};

void ProcFSProcessDirectory::on_attach()
{
    VERIFY(m_components.size() == 0);
    m_components.append(ProcFSProcessPledge::create(*this));
    m_components.append(ProcFSProcessUnveil::create(*this));
    m_components.append(ProcFSProcessPerformanceEvents::create(*this));
    m_components.append(ProcFSProcessFileDescriptions::create(*this));
    m_components.append(ProcFSProcessOverallFileDescriptions::create(*this));
    m_components.append(ProcFSProcessRoot::create(*this));
    m_components.append(ProcFSProcessVirtualMemory::create(*this));
    m_components.append(ProcFSProcessCurrentWorkDirectory::create(*this));
    m_components.append(ProcFSProcessBinary::create(*this));
    m_components.append(ProcFSProcessStacks::create(*this));
}

RefPtr<ProcFSExposedComponent> ProcFSProcessDirectory::lookup(StringView name)
{
    // Note: we need to allocate all sub components when doing a lookup, because
    // for some reason, the caller may not call ProcFSInode::attach method before calling this.
    if (m_components.size() == 0)
        on_attach();
    return ProcFSExposedDirectory::lookup(name);
}

KResult ProcFSProcessDirectory::refresh_data(FileDescription&) const
{
    if (m_components.size() != 0)
        return KSuccess;
    const_cast<ProcFSProcessDirectory&>(*this).on_attach();
    return KSuccess;
}

NonnullRefPtr<ProcFSProcessDirectory> ProcFSProcessDirectory::create(const Process& process)
{
    return adopt_ref_if_nonnull(new (nothrow) ProcFSProcessDirectory(process)).release_nonnull();
}

void ProcFSProcessDirectory::prepare_for_deletion()
{
    ProcFSExposedDirectory::prepare_for_deletion();
    m_associated_process.clear();
}

ProcFSProcessDirectory::ProcFSProcessDirectory(const Process& process)
    : ProcFSExposedDirectory(String::formatted("{:d}", process.pid().value()), ProcFSComponentRegistry::the().root_directory())
    , m_associated_process(process)
{
}

}
