/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Buffer.h"
#include "Definitions.h"
#include <Kernel/API/V3D.h>
#include <LibCore/System.h>
#include <sys/mman.h>

namespace V3DGPU {

extern int g_v3d_fd;

Buffer::~Buffer()
{
    // The size of moved-from buffers is set to 0.
    if (m_size == 0)
        return;

    VERIFY(m_mmap_address != nullptr);

    if (auto result = Core::System::munmap(m_mmap_address, m_size); result.is_error())
        dbgln("LibV3DGPU: ~Buffer(): munmap({}, {:#x}) failed: {}", m_mmap_address, m_size, result.release_error());

    if (auto result = Core::System::ioctl(g_v3d_fd, V3D_FREE_BUFFER, m_gpu_virtual_address); result.is_error())
        dbgln("LibV3DGPU: ~Buffer(): ioctl({}, V3D_FREE_BUFFER, {:#x}) failed: {}", g_v3d_fd, m_gpu_virtual_address, result.release_error());
}

ErrorOr<Buffer> Buffer::create(u32 size, StringView name)
{
    VERIFY(size != 0);
    VERIFY(size % V3D_PAGE_SIZE == 0);

    V3DBuffer buffer = {
        .size = size,

        // Will be filled by the kernel.
        .gpu_virtual_address = 0,
    };

    TRY(Core::System::ioctl(g_v3d_fd, V3D_ALLOCATE_BUFFER, &buffer));

    VERIFY(buffer.gpu_virtual_address != 0);

    auto* mmap_address = TRY(Core::System::mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_RANDOMIZED, g_v3d_fd, buffer.gpu_virtual_address, 0, name));

    return Buffer(buffer.gpu_virtual_address, buffer.size, mmap_address);
}

}
