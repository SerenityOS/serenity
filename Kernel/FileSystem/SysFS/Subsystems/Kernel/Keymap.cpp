/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Keymap.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSKeymap::SysFSKeymap(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSKeymap> SysFSKeymap::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSKeymap(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSKeymap::try_generate(KBufferBuilder& builder)
{
    auto json = TRY(JsonObjectSerializer<>::try_create(builder));
    TRY(InputManagement::the().keymap_data().with([&](auto const& keymap_data) {
        return json.add("keymap"sv, keymap_data.character_map_name->view());
    }));
    TRY(json.finish());
    return {};
}

}
