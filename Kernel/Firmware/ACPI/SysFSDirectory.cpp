/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SysFSDirectory.h"
#include "Parser.h"
#include "SysFSComponent.h"

namespace Kernel::ACPI {

UNMAP_AFTER_INIT void ACPISysFSDirectory::find_tables_and_register_them_as_components()
{
    size_t ssdt_count = 0;
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        Parser::the()->enumerate_static_tables([&](StringView signature, PhysicalAddress p_table, size_t length) {
            if (signature == "SSDT") {
                auto component_name = KString::formatted("{:4s}{}", signature, ssdt_count).release_value_but_fixme_should_propagate_errors();
                list.append(ACPISysFSComponent::create(component_name->view(), p_table, length));
                ssdt_count++;
                return;
            }
            list.append(ACPISysFSComponent::create(signature, p_table, length));
        });
        return {};
    }));

    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        auto rsdp = Memory::map_typed<::ACPI::Structures::RSDPDescriptor20>(ACPI::Parser::the()->rsdp()).release_value_but_fixme_should_propagate_errors();
        list.append(ACPISysFSComponent::create("RSDP"sv, ACPI::Parser::the()->rsdp(), rsdp->base.revision == 0 ? sizeof(::ACPI::Structures::RSDPDescriptor) : rsdp->length));
        auto main_system_description_table = Memory::map_typed<::ACPI::Structures::SDTHeader>(ACPI::Parser::the()->main_system_description_table()).release_value_but_fixme_should_propagate_errors();
        if (ACPI::Parser::the()->is_xsdt_supported()) {
            list.append(ACPISysFSComponent::create("XSDT"sv, ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
        } else {
            list.append(ACPISysFSComponent::create("RSDT"sv, ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
        }
        return {};
    }));
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ACPISysFSDirectory> ACPISysFSDirectory::must_create(SysFSFirmwareDirectory& firmware_directory)
{
    auto acpi_directory = MUST(adopt_nonnull_lock_ref_or_enomem(new (nothrow) ACPISysFSDirectory(firmware_directory)));
    acpi_directory->find_tables_and_register_them_as_components();
    return acpi_directory;
}

UNMAP_AFTER_INIT ACPISysFSDirectory::ACPISysFSDirectory(SysFSFirmwareDirectory& firmware_directory)
    : SysFSDirectory(firmware_directory)
{
}

}
