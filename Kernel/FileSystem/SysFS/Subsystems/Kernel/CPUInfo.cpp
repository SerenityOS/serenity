/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/Arch/x86/ProcessorInfo.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/CPUInfo.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSCPUInformation::SysFSCPUInformation(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSCPUInformation> SysFSCPUInformation::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSCPUInformation(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSCPUInformation::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(Processor::try_for_each(
        [&](Processor& proc) -> ErrorOr<void> {
            auto& info = proc.info();
            auto obj = TRY(array.add_object());
            TRY(obj.add("processor"sv, proc.id()));
            TRY(obj.add("vendor_id"sv, info.vendor_id_string()));
            TRY(obj.add("family"sv, info.display_family()));
            if (!info.hypervisor_vendor_id_string().is_null())
                TRY(obj.add("hypervisor_vendor_id"sv, info.hypervisor_vendor_id_string()));

            auto features_array = TRY(obj.add_array("features"sv));
            auto keep_empty = SplitBehavior::KeepEmpty;

            ErrorOr<void> result; // FIXME: Make this nicer
            info.features_string().for_each_split_view(' ', keep_empty, [&](StringView feature) {
                if (result.is_error())
                    return;
                result = features_array.add(feature);
            });
            TRY(result);

            TRY(features_array.finish());

            TRY(obj.add("model"sv, info.display_model()));
            TRY(obj.add("stepping"sv, info.stepping()));
            TRY(obj.add("type"sv, info.type()));
            TRY(obj.add("brand"sv, info.brand_string()));

            auto caches = TRY(obj.add_object("caches"sv));

            auto add_cache_info = [&](StringView name, ProcessorInfo::Cache const& cache) -> ErrorOr<void> {
                auto cache_object = TRY(caches.add_object(name));
                TRY(cache_object.add("size"sv, cache.size));
                TRY(cache_object.add("line_size"sv, cache.line_size));
                TRY(cache_object.finish());
                return {};
            };

            if (info.l1_data_cache().has_value())
                TRY(add_cache_info("l1_data"sv, *info.l1_data_cache()));
            if (info.l1_instruction_cache().has_value())
                TRY(add_cache_info("l1_instruction"sv, *info.l1_instruction_cache()));
            if (info.l2_cache().has_value())
                TRY(add_cache_info("l2"sv, *info.l2_cache()));
            if (info.l3_cache().has_value())
                TRY(add_cache_info("l3"sv, *info.l3_cache()));

            TRY(caches.finish());

            TRY(obj.finish());
            return {};
        }));
    TRY(array.finish());
    return {};
}

}
