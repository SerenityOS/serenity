/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/GPU/Intel/Plane/DisplayPlane.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

IntelDisplayPlane::IntelDisplayPlane(Memory::TypedMapping<PlaneRegisters volatile> plane_registers_mapping)
    : m_plane_registers(move(plane_registers_mapping))
{
}

IntelDisplayPlane::ShadowRegisters IntelDisplayPlane::shadow_registers() const
{
    SpinlockLocker locker(m_access_lock);
    return m_shadow_registers;
}

ErrorOr<void> IntelDisplayPlane::set_horizontal_active_pixels_count(Badge<IntelDisplayConnectorGroup>, size_t horizontal_active_pixels_count)
{
    SpinlockLocker locker(m_access_lock);
    m_horizontal_active_pixels_count = horizontal_active_pixels_count;
    return {};
}
ErrorOr<void> IntelDisplayPlane::set_vertical_active_pixels_count(Badge<IntelDisplayConnectorGroup>, size_t vertical_active_pixels_count)
{
    SpinlockLocker locker(m_access_lock);
    m_vertical_active_pixels_count = vertical_active_pixels_count;
    return {};
}
ErrorOr<void> IntelDisplayPlane::set_horizontal_stride(Badge<IntelDisplayConnectorGroup>, size_t horizontal_stride)
{
    SpinlockLocker locker(m_access_lock);
    m_horizontal_stride = horizontal_stride;
    return {};
}
ErrorOr<void> IntelDisplayPlane::set_aperture_base(Badge<IntelDisplayConnectorGroup>, PhysicalAddress aperture_start)
{
    SpinlockLocker locker(m_access_lock);
    m_aperture_start.set(aperture_start.get());
    return {};
}
ErrorOr<void> IntelDisplayPlane::set_pipe(Badge<IntelDisplayConnectorGroup>, PipeSelect pipe_select)
{
    SpinlockLocker locker(m_access_lock);
    m_pipe_select = pipe_select;
    return {};
}

bool IntelDisplayPlane::is_enabled(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    return m_shadow_registers.control & (1 << 31);
}

ErrorOr<void> IntelDisplayPlane::disable(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    // Note: We use the shadow register so we don't have the already set
    // settings being lost.
    m_shadow_registers.control &= ~(1 << 31);
    m_plane_registers->control = m_shadow_registers.control;
    return {};
}

}
