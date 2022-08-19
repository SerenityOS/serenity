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

namespace Kernel {

VirtIOGPU3DDevice::PerContextState::PerContextState(Graphics::VirtIOGPU::ContextID context_id, OwnPtr<Memory::Region> transfer_buffer_region)
    : m_context_id(context_id)
    , m_transfer_buffer_region(move(transfer_buffer_region))
{
}

NonnullLockRefPtr<VirtIOGPU3DDevice> VirtIOGPU3DDevice::must_create(VirtIOGraphicsAdapter const& adapter)
{
    // Setup memory transfer region
    auto region_result = MM.allocate_kernel_region(
        NUM_TRANSFER_REGION_PAGES * PAGE_SIZE,
        "VIRGL3D kernel upload buffer"sv,
        Memory::Region::Access::ReadWrite,
        AllocationStrategy::AllocateNow);
    VERIFY(!region_result.is_error());
    auto device = MUST(DeviceManagement::try_create_device<VirtIOGPU3DDevice>(adapter, region_result.release_value()));
    return device;
}

VirtIOGPU3DDevice::VirtIOGPU3DDevice(VirtIOGraphicsAdapter const& graphics_adapter, NonnullOwnPtr<Memory::Region> transfer_buffer_region)
    : CharacterDevice(28, 0)
    , m_graphics_adapter(graphics_adapter)
    , m_transfer_buffer_region(move(transfer_buffer_region))
{
    m_kernel_context_id = m_graphics_adapter->create_context();
}

void VirtIOGPU3DDevice::detach(OpenFileDescription& description)
{
    m_context_state_lookup.remove(&description);
    CharacterDevice::detach(description);
}

ErrorOr<LockRefPtr<VirtIOGPU3DDevice::PerContextState>> VirtIOGPU3DDevice::get_context_for_description(OpenFileDescription& description)
{
    auto res = m_context_state_lookup.get(&description);
    if (!res.has_value())
        return EBADF;
    return res.value();
}

ErrorOr<void> VirtIOGPU3DDevice::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    // TODO: We really should have ioctls for destroying resources as well
    switch (request) {
    case VIRGL_IOCTL_CREATE_CONTEXT: {
        if (m_context_state_lookup.contains(&description))
            return EEXIST;
        SpinlockLocker locker(m_graphics_adapter->operation_lock());
        // TODO: Delete the context if it fails to be set in m_context_state_lookup
        auto context_id = m_graphics_adapter->create_context();
        LockRefPtr<PerContextState> per_context_state = TRY(PerContextState::try_create(context_id));
        TRY(m_context_state_lookup.try_set(&description, per_context_state));
        return {};
    }
    case VIRGL_IOCTL_TRANSFER_DATA: {
        auto& transfer_buffer_region = TRY(get_context_for_description(description))->transfer_buffer_region();
        auto user_transfer_descriptor = static_ptr_cast<VirGLTransferDescriptor const*>(arg);
        auto transfer_descriptor = TRY(copy_typed_from_user(user_transfer_descriptor));
        if (transfer_descriptor.direction == VIRGL_DATA_DIR_GUEST_TO_HOST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto target = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_from_user(target, transfer_descriptor.data, transfer_descriptor.num_bytes);
        } else if (transfer_descriptor.direction == VIRGL_DATA_DIR_HOST_TO_GUEST) {
            if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
                return EOVERFLOW;
            }
            auto source = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
            return copy_to_user(transfer_descriptor.data, source, transfer_descriptor.num_bytes);
        } else {
            return EINVAL;
        }
    }
    case VIRGL_IOCTL_SUBMIT_CMD: {
        auto context_id = TRY(get_context_for_description(description))->context_id();
        SpinlockLocker locker(m_graphics_adapter->operation_lock());
        auto user_command_buffer = static_ptr_cast<VirGLCommandBuffer const*>(arg);
        auto command_buffer = TRY(copy_typed_from_user(user_command_buffer));
        m_graphics_adapter->submit_command_buffer(context_id, [&](Bytes buffer) {
            auto num_bytes = command_buffer.num_elems * sizeof(u32);
            VERIFY(num_bytes <= buffer.size());
            MUST(copy_from_user(buffer.data(), command_buffer.data, num_bytes));
            return num_bytes;
        });
        return {};
    }
    case VIRGL_IOCTL_CREATE_RESOURCE: {
        auto per_context_state = TRY(get_context_for_description(description));
        auto user_spec = static_ptr_cast<VirGL3DResourceSpec const*>(arg);
        VirGL3DResourceSpec spec = TRY(copy_typed_from_user(user_spec));

        Graphics::VirtIOGPU::Protocol::Resource3DSpecification const resource_spec = {
            .target = static_cast<Graphics::VirtIOGPU::Protocol::Gallium::PipeTextureTarget>(spec.target),
            .format = spec.format,
            .bind = spec.bind,
            .width = spec.width,
            .height = spec.height,
            .depth = spec.depth,
            .array_size = spec.array_size,
            .last_level = spec.last_level,
            .nr_samples = spec.nr_samples,
            .flags = spec.flags,
            .padding = 0,
        };
        SpinlockLocker locker(m_graphics_adapter->operation_lock());
        auto resource_id = m_graphics_adapter->create_3d_resource(resource_spec).value();
        m_graphics_adapter->attach_resource_to_context(resource_id, per_context_state->context_id());
        m_graphics_adapter->ensure_backing_storage(resource_id, per_context_state->transfer_buffer_region(), 0, NUM_TRANSFER_REGION_PAGES * PAGE_SIZE);
        spec.created_resource_id = resource_id;
        // FIXME: We should delete the resource we just created if we fail to copy the resource id out
        return copy_to_user(static_ptr_cast<VirGL3DResourceSpec*>(arg), &spec);
    }
    }
    return EINVAL;
}

}
