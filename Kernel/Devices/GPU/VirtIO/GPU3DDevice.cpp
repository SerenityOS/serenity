/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/API/VirGL.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/GPU/VirtIO/Console.h>
#include <Kernel/Devices/GPU/VirtIO/GPU3DDevice.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/VirtIO/Protocol.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

VirtIOGPU3DDevice::PerContextState::PerContextState(OpenFileDescription& description, Graphics::VirtIOGPU::ContextID context_id, OwnPtr<Memory::Region> transfer_buffer_region)
    : m_context_id(context_id)
    , m_transfer_buffer_region(move(transfer_buffer_region))
    , m_attached_file_description(description)
{
}

ErrorOr<NonnullRefPtr<VirtIOGPU3DDevice>> VirtIOGPU3DDevice::create(VirtIOGraphicsAdapter& adapter)
{
    // Setup memory transfer region
    auto region_result = TRY(MM.allocate_kernel_region(
        NUM_TRANSFER_REGION_PAGES * PAGE_SIZE,
        "VIRGL3D kernel upload buffer"sv,
        Memory::Region::Access::ReadWrite,
        AllocationStrategy::AllocateNow));
    auto kernel_context_id = TRY(adapter.create_context());
    return TRY(Device::try_create_device<VirtIOGPU3DDevice>(adapter, move(region_result), kernel_context_id));
}

VirtIOGPU3DDevice::VirtIOGPU3DDevice(VirtIOGraphicsAdapter const& graphics_adapter, NonnullOwnPtr<Memory::Region> transfer_buffer_region, Graphics::VirtIOGPU::ContextID kernel_context_id)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::GPURender, 0)
    , m_graphics_adapter(graphics_adapter)
    , m_kernel_context_id(kernel_context_id)
    , m_transfer_buffer_region(move(transfer_buffer_region))
{
}

void VirtIOGPU3DDevice::detach(OpenFileDescription& description)
{
    m_context_state_list.with([&description](auto& list) {
        for (auto& context : list) {
            if (&context.description() == &description) {
                context.m_list_node.remove();
                // NOTE: We break here because there shouldn't be another context
                // being attached to this OpenFileDescription.
                break;
            }
        }
    });
    CharacterDevice::detach(description);
}

ErrorOr<void> VirtIOGPU3DDevice::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{

    auto get_context_for_description = [](ContextList& list, OpenFileDescription& description) -> ErrorOr<NonnullRefPtr<PerContextState>> {
        for (auto& context : list) {
            if (&context.description() == &description)
                return context;
        }
        return EBADF;
    };

    // TODO: We really should have ioctls for destroying resources as well
    switch (request) {
    case VIRGL_IOCTL_CREATE_CONTEXT: {
        return m_context_state_list.with([get_context_for_description, &description, this](auto& list) -> ErrorOr<void> {
            if (auto result = get_context_for_description(list, description); !result.is_error())
                return EEXIST;
            SpinlockLocker locker(m_graphics_adapter->operation_lock());
            // TODO: Delete the context if it fails to be set in m_context_state_lookup
            auto context_id = TRY(m_graphics_adapter->create_context());
            auto per_context_state = TRY(PerContextState::try_create(description, context_id));
            list.append(per_context_state);
            return {};
        });
    }
    case VIRGL_IOCTL_TRANSFER_DATA: {
        auto user_transfer_descriptor = static_ptr_cast<VirGLTransferDescriptor const*>(arg);
        auto transfer_descriptor = TRY(copy_typed_from_user(user_transfer_descriptor));
        if (Checked<size_t>::addition_would_overflow(transfer_descriptor.offset_in_region, transfer_descriptor.num_bytes)) {
            return EOVERFLOW;
        }
        if (transfer_descriptor.offset_in_region + transfer_descriptor.num_bytes > NUM_TRANSFER_REGION_PAGES * PAGE_SIZE) {
            return EOVERFLOW;
        }

        return m_context_state_list.with([get_context_for_description, &description, &transfer_descriptor](auto& list) -> ErrorOr<void> {
            auto context = TRY(get_context_for_description(list, description));
            auto& transfer_buffer_region = context->transfer_buffer_region();
            if (transfer_descriptor.direction == VIRGL_DATA_DIR_GUEST_TO_HOST) {
                auto target = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
                return copy_from_user(target, transfer_descriptor.data, transfer_descriptor.num_bytes);
            } else if (transfer_descriptor.direction == VIRGL_DATA_DIR_HOST_TO_GUEST) {
                auto source = transfer_buffer_region.vaddr().offset(transfer_descriptor.offset_in_region).as_ptr();
                return copy_to_user(transfer_descriptor.data, source, transfer_descriptor.num_bytes);
            } else {
                return EINVAL;
            }
        });
    }
    case VIRGL_IOCTL_SUBMIT_CMD: {
        auto user_command_buffer = static_ptr_cast<VirGLCommandBuffer const*>(arg);
        auto command_buffer = TRY(copy_typed_from_user(user_command_buffer));
        return m_context_state_list.with([get_context_for_description, &description, &command_buffer, this](auto& list) -> ErrorOr<void> {
            auto context = TRY(get_context_for_description(list, description));
            auto context_id = context->context_id();
            SpinlockLocker locker(m_graphics_adapter->operation_lock());
            TRY(m_graphics_adapter->submit_command_buffer(context_id, [&](Bytes buffer) {
                auto num_bytes = command_buffer.num_elems * sizeof(u32);
                VERIFY(num_bytes <= buffer.size());
                MUST(copy_from_user(buffer.data(), command_buffer.data, num_bytes));
                return num_bytes;
            }));
            return {};
        });
    }
    case VIRGL_IOCTL_CREATE_RESOURCE: {
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
        return m_context_state_list.with([get_context_for_description, &description, &resource_spec, &spec, &arg, this](auto& list) -> ErrorOr<void> {
            auto context = TRY(get_context_for_description(list, description));
            SpinlockLocker locker(m_graphics_adapter->operation_lock());
            // FIXME: What would be an appropriate resource free-ing mechanism to use in case anything
            // after this fails?
            auto resource_id = TRY(m_graphics_adapter->create_3d_resource(resource_spec));
            TRY(m_graphics_adapter->attach_resource_to_context(resource_id, context->context_id()));
            TRY(m_graphics_adapter->ensure_backing_storage(resource_id, context->transfer_buffer_region(), 0, NUM_TRANSFER_REGION_PAGES * PAGE_SIZE));
            spec.created_resource_id = resource_id.value();
            // FIXME: We should delete the resource we just created if we fail to copy the resource id out
            return copy_to_user(static_ptr_cast<VirGL3DResourceSpec*>(arg), &spec);
        });
    }
    }
    return EINVAL;
}

}
