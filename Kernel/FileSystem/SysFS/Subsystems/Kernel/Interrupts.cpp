/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Interrupts.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSInterrupts::SysFSInterrupts(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSInterrupts> SysFSInterrupts::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSInterrupts(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSInterrupts::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    ErrorOr<void> result; // FIXME: Make this nicer
    InterruptManagement::the().enumerate_interrupt_handlers([&array, &result](GenericInterruptHandler& handler) {
        if (result.is_error())
            return;
        result = ([&]() -> ErrorOr<void> {
            auto obj = TRY(array.add_object());
            TRY(obj.add("purpose"sv, handler.purpose()));
            TRY(obj.add("interrupt_line"sv, handler.interrupt_number()));
            TRY(obj.add("controller"sv, handler.controller()));
            TRY(obj.add("device_sharing"sv, (unsigned)handler.sharing_devices_count()));
            auto per_cpu_call_counts = TRY(obj.add_array("per_cpu_call_counts"sv));
            for (auto call_count : handler.per_cpu_call_counts()) {
                TRY(per_cpu_call_counts.add(call_count));
            }
            TRY(per_cpu_call_counts.finish());
            TRY(obj.finish());
            return {};
        })();
    });
    TRY(result);
    TRY(array.finish());
    return {};
}

}
