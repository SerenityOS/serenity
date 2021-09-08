/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/DeviceFile.h>

namespace Kernel {

KResult DeviceFile::attach_new_file_blocker()
{
    VERIFY(!m_blocker_set);
    auto device = m_device.strong_ref();
    if (!device)
        return KResult(EIO);
    m_device_blocker_set = device->blocker_set();
    VERIFY(m_device_blocker_set);
    return KSuccess;
}

DeviceFile::DeviceFile(const Device& device)
    : m_is_block_device(device.is_block_device())
    , m_is_character_device(device.is_character_device())
    , m_is_master_pty(device.is_master_pty())
    , m_is_tty(device.is_tty())
    , m_seekable(device.is_seekable())
    , m_class_name(device.class_name())
    , m_absolute_path(device.absolute_path())
{
    VERIFY(m_is_block_device || m_is_character_device);
    m_device = device;
}

KResultOr<NonnullRefPtr<DeviceFile>> DeviceFile::try_create(const Device& device)
{
    auto file = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DeviceFile(device)));
    TRY(file->attach_new_file_blocker());
    return file;
}

bool DeviceFile::is_block_device() const
{
    return m_is_block_device;
}

bool DeviceFile::is_seekable() const
{
    return m_seekable;
}
bool DeviceFile::is_tty() const
{
    return m_is_tty;
}

bool DeviceFile::is_character_device() const
{
    return m_is_character_device;
}

bool DeviceFile::is_master_pty() const
{
    return m_is_master_pty;
}

FileBlockerSet& DeviceFile::blocker_set()
{
    return *m_device_blocker_set;
}
void DeviceFile::did_seek(OpenFileDescription& description, off_t offset)
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: did_seek on device failed", m_class_name);
        return;
    }
    device->did_seek(description, offset);
}
String DeviceFile::absolute_path(const OpenFileDescription&) const
{
    return m_absolute_path;
}
KResultOr<size_t> DeviceFile::read(OpenFileDescription& description, u64 offset, UserOrKernelBuffer& buffer, size_t length)
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: read from device failed", m_class_name);
        return KResult(EIO);
    }
    return device->read(description, offset, buffer, length);
}
bool DeviceFile::can_read(const OpenFileDescription&, size_t) const
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: can_read on device failed", m_class_name);
        return false;
    }
    return device->can_read();
}
KResultOr<size_t> DeviceFile::write(OpenFileDescription& description, u64 offset, const UserOrKernelBuffer& buffer, size_t length)
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: write to device failed", m_class_name);
        return KResult(EIO);
    }
    return device->write(description, offset, buffer, length);
}
bool DeviceFile::can_write(const OpenFileDescription&, size_t) const
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: can_write on device failed", m_class_name);
        return false;
    }
    return device->can_write();
}
KResult DeviceFile::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: ioctl device failed", m_class_name);
        return KResult(EIO);
    }
    return device->ioctl(description, request, arg);
}
KResultOr<Memory::Region*> DeviceFile::mmap(Process& process, OpenFileDescription& description, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    auto device = m_device.strong_ref();
    if (!device)
        return KResult(EIO);
    return device->mmap(process, description, range, offset, prot, shared);
}
KResultOr<NonnullRefPtr<OpenFileDescription>> DeviceFile::open(int options)
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: open device failed", m_class_name);
        return KResult(EIO);
    }
    return device->open(options);
}
KResult DeviceFile::close()
{
    auto device = m_device.strong_ref();
    if (!device) {
        dbgln("{}: close device failed", m_class_name);
        return KResult(EIO);
    }
    return device->close();
}
StringView DeviceFile::class_name() const
{
    return m_class_name;
}

const Device* DeviceFile::as_device() const
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device.ptr();
}
Device* DeviceFile::as_device()
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device.ptr();
}
const TTY* DeviceFile::as_tty() const
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device->as_tty();
}
TTY* DeviceFile::as_tty()
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device->as_tty();
}
const MasterPTY* DeviceFile::as_master_pty() const
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device->as_master_pty();
}
MasterPTY* DeviceFile::as_master_pty()
{
    auto device = m_device.strong_ref();
    if (!device)
        return nullptr;
    return device->as_master_pty();
}

}
