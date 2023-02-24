/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Jails.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Jail.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSJails::SysFSJails(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSJails> SysFSJails::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSJails(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSJails::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(Jail::for_each_when_process_is_not_jailed([&array](Jail const& jail) -> ErrorOr<void> {
        auto obj = TRY(array.add_object());
        TRY(obj.add("index"sv, jail.index().value()));
        TRY(obj.add("name"sv, jail.name()));
        TRY(obj.finish());
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
