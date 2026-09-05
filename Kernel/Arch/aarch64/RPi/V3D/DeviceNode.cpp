/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/Arch/aarch64/RPi/V3D/DeviceNode.h>

namespace Kernel::RPi::V3D {

ErrorOr<NonnullRefPtr<DeviceNode>> DeviceNode::create(V3D& v3d)
{
    return TRY(Device::try_create_device<DeviceNode>(v3d));
}

ErrorOr<void> DeviceNode::attach(OpenFileDescription& description)
{
    auto page_table = TRY(PageTable::create());

    auto* context = new (nothrow) Context { description, move(page_table) };
    if (context == nullptr)
        return ENOMEM;

    m_context_list.with([&context](auto& context_list) {
        context_list.append(*context);
    });

    return CharacterDevice::attach(description);
}

void DeviceNode::detach(OpenFileDescription& description)
{
    auto& context = context_for_description(description);

    while (!context.buffers.is_empty()) {
        auto const& buffer = context.buffers.first();

        // Use MUST() because the gpu_vaddr should always be valid.
        MUST(free_buffer(context, buffer.gpu_vaddr));
    }

    m_context_list.with([&context](auto& context_list) {
        context_list.remove(context);
    });

    delete &context;

    CharacterDevice::detach(description);
}

ErrorOr<void> DeviceNode::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    auto& context = context_for_description(description);
    MutexLocker locker { context.mutex };

    switch (request) {
    case V3D_ALLOCATE_BUFFER: {
        auto buffer_allocation_info = TRY(copy_typed_from_user(static_ptr_cast<V3DBuffer const*>(arg)));

        auto gpu_vaddr = TRY(allocate_buffer(context, buffer_allocation_info.size));

        buffer_allocation_info.gpu_virtual_address = gpu_vaddr.get();
        TRY(copy_to_user(static_ptr_cast<V3DBuffer*>(arg), &buffer_allocation_info));

        return {};
    }

    case V3D_FREE_BUFFER: {
        auto buffer_gpu_vaddr = arg.ptr();
        return free_buffer(context, GPUVirtualAddress { static_cast<u32>(buffer_gpu_vaddr) });
    }

    case V3D_SUBMIT_JOB: {
        auto job = TRY(copy_typed_from_user(static_ptr_cast<V3DJob const*>(arg)));
        return m_v3d->submit_job(context.page_table, job);
    }
    }

    return EINVAL;
}

ErrorOr<File::VMObjectAndMemoryType> DeviceNode::vmobject_and_memory_type_for_mmap(OpenFileDescription& description, Memory::VirtualRange const&, u64& offset, bool)
{
    if ((offset % V3D_PAGE_SIZE) != 0)
        return EINVAL;

    auto& context = context_for_description(description);
    MutexLocker locker { context.mutex };

    for (auto const& buffer : context.buffers) {
        if (buffer.gpu_vaddr.get() == offset) {
            offset = 0;

            return VMObjectAndMemoryType {
                .vmobject = buffer.vmobject,
                .memory_type = Memory::MemoryType::NonCacheable,
            };
        }
    }

    return EFAULT;
}

DeviceNode::DeviceNode(V3D& v3d)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::GPURender, 0) // FIXME: Don't hardcode the minor number.
    , m_v3d(v3d)
{
}

ErrorOr<GPUVirtualAddress> DeviceNode::allocate_buffer(Context& context, size_t size)
{
    if ((size % V3D_PAGE_SIZE) != 0)
        return EINVAL;

    // We need to use AllocateNow since we don't want to (and can't even) lazily page in data for the GPU.
    // Page faults don't seem to be recoverable on the V3D.
    auto vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(size, AllocationStrategy::AllocateNow));

    auto region = TRY(GPUMemoryRegion::create_unplaced());

    // Do the fallible operation of growing the vector capacity before placing the Region to make error recovery a bit simpler.
    // Otherwise we would need to remove the Region again if appending to the Vector fails.
    TRY(context.buffers.try_grow_capacity(context.buffers.size() + 1));

    TRY(context.region_tree.place_anywhere(*region, Memory::RandomizeVirtualAddress::Yes, size, V3D_PAGE_SIZE));

    auto gpu_vaddr = GPUVirtualAddress { region->vaddr().get() };

    context.buffers.unchecked_append({
        .vmobject = move(vmobject),
        .gpu_vaddr = gpu_vaddr,
        .region = move(region),
    });

    // Only map the buffer after we are sure that this function can't fail anymore
    // so that we don't need to revert all previous changes.
    auto const& buffer = context.buffers.last();
    m_v3d->map_buffer(context.page_table, gpu_vaddr, buffer.vmobject);

    return gpu_vaddr;
}

ErrorOr<void> DeviceNode::free_buffer(Context& context, GPUVirtualAddress buffer_gpu_vaddr)
{
    // NOTE: This code doesn't check if the buffer is still mmap'ed.
    //       This means if userspace frees a buffer but still has it mmap'ed, the VMObject will stay alive
    //       because the mmap Region will keep a reference to VMObject.

    auto buffer_index = context.buffers.find_first_index_if([buffer_gpu_vaddr](Context::Buffer const& buffer) {
        return buffer.gpu_vaddr == buffer_gpu_vaddr;
    });

    if (!buffer_index.has_value())
        return EINVAL;

    auto const& buffer = context.buffers[*buffer_index];

    m_v3d->unmap_buffer(context.page_table, buffer.gpu_vaddr, *buffer.vmobject);

    context.region_tree.remove(*buffer.region);
    context.buffers.remove(*buffer_index);

    return {};
}

}
