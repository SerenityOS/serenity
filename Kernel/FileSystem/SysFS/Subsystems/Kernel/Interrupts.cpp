/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
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

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSInterrupts> SysFSInterrupts::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSInterrupts(parent_directory)).release_nonnull();
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
            TRY(obj.add("cpu_handler"sv, 0)); // FIXME: Determine the responsible CPU for each interrupt handler.
            TRY(obj.add("device_sharing"sv, (unsigned)handler.sharing_devices_count()));
            TRY(obj.add("call_count"sv, (unsigned)handler.get_invoking_count()));
            TRY(obj.finish());
            return {};
        })();
    });
    TRY(result);
    TRY(array.finish());
    return {};
}

}
