/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/V3D.h>
#include <Kernel/Arch/aarch64/RPi/V3D/GPUMemoryRegion.h>
#include <Kernel/Arch/aarch64/RPi/V3D/GPUVirtualAddress.h>
#include <Kernel/Arch/aarch64/RPi/V3D/V3D.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel::RPi::V3D {

class DeviceNode final : public CharacterDevice {
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<DeviceNode>> create(V3D&);

    virtual bool can_read(OpenFileDescription const&, u64) const override { return false; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return false; }

    virtual ErrorOr<void> attach(OpenFileDescription&) override;
    virtual void detach(OpenFileDescription&) override;

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<VMObjectAndMemoryType> vmobject_and_memory_type_for_mmap(OpenFileDescription&, Memory::VirtualRange const&, u64&, bool) override;

    virtual StringView class_name() const override { return "V3D::DeviceNode"sv; }

private:
    DeviceNode(V3D&);

    // Exclude the first page to ensure that we never return null pointers when allocating buffers.
    // Additionally exclude the last page to make .end() representable with a u32.
    static constexpr auto USERSPACE_VIRTUAL_ADDRESS_RANGE = GPUVirtualRange { GPUVirtualAddress { V3D_PAGE_SIZE }, V3D_VIRTUAL_MEMORY_SIZE - 2 * V3D_PAGE_SIZE };

    struct Context {
        Context(OpenFileDescription& file_description, PageTable page_table)
            : page_table(move(page_table))
            , region_tree(USERSPACE_VIRTUAL_ADDRESS_RANGE)
            , associated_description(file_description)
        {
        }

        struct Buffer {
            NonnullLockRefPtr<Memory::AnonymousVMObject> vmobject;
            GPUVirtualAddress gpu_vaddr;
            NonnullOwnPtr<GPUMemoryRegion> region;
        };

        // Protects the entire Context state.
        Mutex mutex;

        Vector<Buffer> buffers;

        PageTable page_table;

        Memory::RegionTree<GPUMemoryRegion, V3D_PAGE_SIZE> region_tree;

        // Context is destroyed once OpenFileDescription's destructor calls File::detach() on DeviceNode,
        // so this struct will never outlive the lifetime of this associated OpenFileDescription.
        // It's therefore safe and necessary to use a raw reference here.
        // Otherwise we would leak a reference on the description here, causing OpenFileDescription's
        // destructor to never be called.
        // This also means we don't need any separate refcounting for Contexts and can simply let OpenFileDescription
        // handle this for us.
        OpenFileDescription& associated_description;

        IntrusiveListNode<Context> list_node;
        using List = IntrusiveList<&Context::list_node>;
    };

    ErrorOr<GPUVirtualAddress> allocate_buffer(Context&, size_t);
    ErrorOr<void> free_buffer(Context&, GPUVirtualAddress buffer_gpu_vaddr);

    Context& context_for_description(OpenFileDescription& description)
    {
        return m_context_list.with([&description](auto& context_list) -> Context& {
            for (auto& context : context_list) {
                if (&context.associated_description == &description) {
                    return context;
                }
            }

            VERIFY_NOT_REACHED();
        });
    }

    SpinlockProtected<Context::List, LockRank::None> m_context_list;
    NonnullRefPtr<V3D> m_v3d;
};

}
