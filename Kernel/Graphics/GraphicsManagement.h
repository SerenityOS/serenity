/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/PCI/Definitions.h>

namespace Kernel {

class GraphicsManagement {
    AK_MAKE_ETERNAL;

public:
    static GraphicsManagement& the();
    static bool is_initialized();
    bool initialize();

    unsigned current_minor_number() { return m_current_minor_number++; };
    GraphicsManagement();

    bool is_text_mode_enabled() const { return m_textmode_enabled; }

private:
    RefPtr<GraphicsDevice> determine_graphics_device(PCI::Address address, PCI::ID id) const;

    NonnullRefPtrVector<GraphicsDevice> m_graphics_devices;
    unsigned m_current_minor_number { 0 };
    bool m_textmode_enabled;
};

}
