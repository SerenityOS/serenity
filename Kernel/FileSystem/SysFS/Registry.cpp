/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<SysFSComponentRegistry> s_the;

SysFSComponentRegistry& SysFSComponentRegistry::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void SysFSComponentRegistry::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT SysFSComponentRegistry::SysFSComponentRegistry()
    : m_root_directory(SysFSRootDirectory::create())
{
}

UNMAP_AFTER_INIT void SysFSComponentRegistry::register_new_component(SysFSComponent& component)
{
    SpinlockLocker locker(m_root_directory_lock);
    MUST(m_root_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(component);
        return {};
    }));
}

SysFSBusDirectory& SysFSComponentRegistry::buses_directory()
{
    return *m_root_directory->m_buses_directory;
}

void SysFSComponentRegistry::register_new_bus_directory(SysFSDirectory& new_bus_directory)
{
    MUST(m_root_directory->m_buses_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_bus_directory);
        return {};
    }));
}

}
