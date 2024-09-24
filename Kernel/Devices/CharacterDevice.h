/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>

namespace Kernel {

class CharacterDevice : public Device {
public:
    virtual ~CharacterDevice() override;

protected:
    CharacterDevice(MajorAllocation::CharacterDeviceFamily character_device_family, MinorNumber minor);

    virtual void after_inserting_add_symlink_to_device_identifier_directory() override final;
    virtual void before_will_be_destroyed_remove_symlink_from_device_identifier_directory() override final;

private:
    virtual bool is_character_device() const final { return true; }

    // FIXME: These methods will be eventually removed after all nodes in /sys/dev/char/ are symlinks
    virtual void after_inserting_add_to_device_identifier_directory() override final;
    virtual void before_will_be_destroyed_remove_from_device_identifier_directory() override final;
};

}
