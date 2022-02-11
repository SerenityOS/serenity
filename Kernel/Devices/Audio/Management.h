/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <Kernel/Devices/Audio/Controller.h>

namespace Kernel {

class AudioManagement {

public:
    AudioManagement();
    static AudioManagement& the();

    static MajorNumber audio_type_major_number();
    static MinorNumber generate_storage_minor_number();

    bool initialize();

private:
    void enumerate_hardware_controllers();
    void enumerate_hardware_audio_channels();

    IntrusiveList<&AudioController::m_node> m_controllers_list;
};

}
