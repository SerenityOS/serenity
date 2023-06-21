/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <AK/Weakable.h>
#include <LibGPU/Device.h>
#include <LibGfx/Size.h>

namespace GPU {

class Driver final
    : public RefCounted<Driver>
    , public Weakable<Driver> {
public:
    static ErrorOr<NonnullRefPtr<Driver>> try_create(StringView driver_name);
    ~Driver();

    ErrorOr<NonnullOwnPtr<Device>> try_create_device(Gfx::IntSize size);

private:
    Driver(void* dlopen_result, serenity_gpu_create_device_t device_creation_function)
        : m_dlopen_result { dlopen_result }
        , m_serenity_gpu_create_device { device_creation_function }
    {
        VERIFY(dlopen_result);
        VERIFY(device_creation_function);
    }

    void* const m_dlopen_result { nullptr };
    serenity_gpu_create_device_t m_serenity_gpu_create_device { nullptr };
};

}
