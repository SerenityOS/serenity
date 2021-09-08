/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/DeviceFile.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

NonnullRefPtr<SysFSDeviceComponent> SysFSDeviceComponent::must_create(Device const& device)
{
    return adopt_ref_if_nonnull(new SysFSDeviceComponent(device)).release_nonnull();
}
SysFSDeviceComponent::SysFSDeviceComponent(Device const& device)
    : SysFSComponent(String::formatted("{}:{}", device.major(), device.minor()))
    , m_block_device(device.is_block_device())
{
    VERIFY(device.is_block_device() || device.is_character_device());
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDevicesDirectory> SysFSDevicesDirectory::must_create(SysFSRootDirectory const& root_directory)
{
    auto devices_directory = adopt_ref_if_nonnull(new SysFSDevicesDirectory(root_directory)).release_nonnull();
    devices_directory->m_components.append(SysFSBlockDevicesDirectory::must_create(*devices_directory));
    devices_directory->m_components.append(SysFSCharacterDevicesDirectory::must_create(*devices_directory));
    return devices_directory;
}
SysFSDevicesDirectory::SysFSDevicesDirectory(SysFSRootDirectory const& root_directory)
    : SysFSDirectory("dev"sv, root_directory)
{
}

NonnullRefPtr<SysFSBlockDevicesDirectory> SysFSBlockDevicesDirectory::must_create(SysFSDevicesDirectory const& devices_directory)
{
    return adopt_ref_if_nonnull(new SysFSBlockDevicesDirectory(devices_directory)).release_nonnull();
}
SysFSBlockDevicesDirectory::SysFSBlockDevicesDirectory(SysFSDevicesDirectory const& devices_directory)
    : SysFSDirectory("block"sv, devices_directory)
{
}
KResult SysFSBlockDevicesDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(m_parent_directory);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_directory->component_index() }, 0 });

    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        for (auto& exposed_device : list) {
            if (!exposed_device.is_block_device())
                continue;
            callback({ exposed_device.name(), { fsid, exposed_device.component_index() }, 0 });
        }
    });
    return KSuccess;
}
RefPtr<SysFSComponent> SysFSBlockDevicesDirectory::lookup(StringView name)
{
    return SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& exposed_device : list) {
            if (!exposed_device.is_block_device())
                continue;
            if (exposed_device.name() == name)
                return exposed_device;
        }
        return nullptr;
    });
}

NonnullRefPtr<SysFSCharacterDevicesDirectory> SysFSCharacterDevicesDirectory::must_create(SysFSDevicesDirectory const& devices_directory)
{
    return adopt_ref_if_nonnull(new SysFSCharacterDevicesDirectory(devices_directory)).release_nonnull();
}
SysFSCharacterDevicesDirectory::SysFSCharacterDevicesDirectory(SysFSDevicesDirectory const& devices_directory)
    : SysFSDirectory("char"sv, devices_directory)
{
}
KResult SysFSCharacterDevicesDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(m_parent_directory);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_directory->component_index() }, 0 });

    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        for (auto& exposed_device : list) {
            if (exposed_device.is_block_device())
                continue;
            callback({ exposed_device.name(), { fsid, exposed_device.component_index() }, 0 });
        }
    });
    return KSuccess;
}
RefPtr<SysFSComponent> SysFSCharacterDevicesDirectory::lookup(StringView name)
{
    return SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& exposed_device : list) {
            if (exposed_device.is_block_device())
                continue;
            if (exposed_device.name() == name)
                return exposed_device;
        }
        return nullptr;
    });
}

bool Device::unref() const
{
    if (deref_base())
        return false;
    delete this;
    return true;
}

KResult Device::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return ENOTTY;
}

KResultOr<Memory::Region*> Device::mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64, int, bool)
{
    return ENODEV;
}

KResultOr<NonnullRefPtr<OpenFileDescription>> Device::open(int options)
{
    auto device_file = TRY(DeviceFile::try_create(*this));
    auto description = TRY(OpenFileDescription::try_create(*device_file));
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

const TTY* Device::as_tty() const
{
    return is_tty() ? static_cast<const TTY*>(this) : nullptr;
}
TTY* Device::as_tty()
{
    return is_tty() ? static_cast<TTY*>(this) : nullptr;
}
const MasterPTY* Device::as_master_pty() const
{
    return is_master_pty() ? static_cast<const MasterPTY*>(this) : nullptr;
}
MasterPTY* Device::as_master_pty()
{
    return is_master_pty() ? static_cast<MasterPTY*>(this) : nullptr;
}
const BlockDevice* Device::as_block_device() const
{
    return is_block_device() ? static_cast<const BlockDevice*>(this) : nullptr;
}
BlockDevice* Device::as_block_device()
{
    return is_block_device() ? static_cast<BlockDevice*>(this) : nullptr;
}
const CharacterDevice* Device::as_character_device() const
{
    return is_character_device() ? static_cast<const CharacterDevice*>(this) : nullptr;
}
CharacterDevice* Device::as_character_device()
{
    return is_character_device() ? static_cast<CharacterDevice*>(this) : nullptr;
}

Device::Device(unsigned major, unsigned minor)
    : m_major(major)
    , m_minor(minor)
{
}

void Device::after_inserting()
{
    m_blocker_set = adopt_ref_if_nonnull(new FileBlockerSet).release_nonnull();
    DeviceManagement::the().after_inserting_device({}, *this);
    VERIFY(!m_sysfs_component);
    auto sys_fs_component = SysFSDeviceComponent::must_create(*this);
    m_sysfs_component = sys_fs_component;
    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        list.append(sys_fs_component);
    });
}

void Device::before_removing()
{
    VERIFY(m_sysfs_component);
    SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> void {
        list.remove(*m_sysfs_component);
    });
    DeviceManagement::the().before_device_removal({}, *this);
    m_state = State::BeingRemoved;
}

Device::~Device()
{
    VERIFY(m_state == State::BeingRemoved);
}

String Device::absolute_path() const
{
    return String::formatted("device:{},{}", major(), minor());
}

void Device::process_next_queued_request(Badge<AsyncDeviceRequest>, const AsyncDeviceRequest& completed_request)
{
    SpinlockLocker lock(m_requests_lock);
    VERIFY(!m_requests.is_empty());
    VERIFY(m_requests.first().ptr() == &completed_request);
    m_requests.remove(m_requests.begin());
    if (!m_requests.is_empty()) {
        auto* next_request = m_requests.first().ptr();
        next_request->do_start(move(lock));
    }

    do_evaluate_block_conditions();
}

}
