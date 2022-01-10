/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Device.h>

namespace Kernel {

class CharacterDevice : public Device {
public:
    virtual ~CharacterDevice() override;

protected:
    CharacterDevice(DeviceID id)
        : Device(id)
    {
    }

private:
    virtual bool is_character_device() const final { return true; }
};

}
