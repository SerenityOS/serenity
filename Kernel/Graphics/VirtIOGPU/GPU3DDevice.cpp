/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/VirGL.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/GPU3DDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>
#include <Kernel/Random.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel::Graphics::VirtIOGPU {

GPU3DDevice::GPU3DDevice(GraphicsAdapter& graphics_adapter)
    : CharacterDevice(28, 0)
    , m_graphics_adapter(graphics_adapter)
{
    m_kernel_context_id = m_graphics_adapter.create_context();

    // Setup memory transfer region
    auto region_result = MM.allocate_kernel_region(
        NUM_TRANSFER_REGION_PAGES * PAGE_SIZE,
        "VIRGL3D upload buffer",
        Memory::Region::Access::ReadWrite,
        AllocationStrategy::AllocateNow);
    VERIFY(!region_result.is_error());
    m_transfer_buffer_region = region_result.release_value();
}

ErrorOr<void> GPU3DDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    // TODO: We really should have ioctls for destroying resources as well
    switch (request) {
    case VIRGL_IOCTL_TRANSFER_DATA: {
        auto user_transfer_descriptor = static_ptr_cast<VirGLTransferDescriptor const*>(arg);
        auto transfer_descriptor = TRY(copy_typed_from_user(user_transfer_descriptor));
        if (transfer_descriptor.direction == VIRGL_DATA_DIR_GUEST_TO_HOST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto target = m_transfer_buffer_region->vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_from_user(target, transfer_descriptor.data, transfer_descriptor.num_bytes);
        } else if (transfer_descriptor.direction == VIRGL_DATA_DIR_HOST_TO_GUEST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto source = m_transfer_buffer_region->vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_to_user(transfer_descriptor.data, source, transfer_descriptor.num_bytes);
        } else {
            return EINVAL;
        }
    }
    case VIRGL_IOCTL_SUBMIT_CMD: {
        MutexLocker locker(m_graphics_adapter.operation_lock());
        auto user_command_buffer = static_ptr_cast<VirGLCommandBuffer const*>(arg);
        auto command_buffer = TRY(copy_typed_from_user(user_command_buffer));
        m_graphics_adapter.submit_command_buffer(m_kernel_context_id, [&](Bytes buffer) {
            auto num_bytes = command_buffer.num_elems * sizeof(u32);
            VERIFY(num_bytes <= buffer.size());
            MUST(copy_from_user(buffer.data(), command_buffer.data, num_bytes));
            return num_bytes;
        });
        return {};
    }
    case VIRGL_IOCTL_CREATE_RESOURCE: {
        auto user_spec = static_ptr_cast<VirGL3DResourceSpec const*>(arg);
        VirGL3DResourceSpec spec = TRY(copy_typed_from_user(user_spec));

        Protocol::Resource3DSpecification const resource_spec = {
            .target = static_cast<Protocol::Gallium::PipeTextureTarget>(spec.target),
            .format = spec.format,
            .bind = spec.bind,
            .width = spec.width,
            .height = spec.height,
            .depth = spec.depth,
            .array_size = spec.array_size,
            .last_level = spec.last_level,
            .nr_samples = spec.nr_samples,
            .flags = spec.flags
        };
        MutexLocker locker(m_graphics_adapter.operation_lock());
        auto resource_id = m_graphics_adapter.create_3d_resource(resource_spec).value();
        m_graphics_adapter.attach_resource_to_context(resource_id, m_kernel_context_id);
        m_graphics_adapter.ensure_backing_storage(resource_id, *m_transfer_buffer_region, 0, NUM_TRANSFER_REGION_PAGES * PAGE_SIZE);
        spec.created_resource_id = resource_id;
        // FIXME: We should delete the resource we just created if we fail to copy the resource id out
        return copy_to_user(static_ptr_cast<VirGL3DResourceSpec*>(arg), &spec);
    }
    }
    return EINVAL;
}

}
