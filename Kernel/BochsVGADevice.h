#pragma once

#include <AK/Types.h>
#include <AK/AKString.h>
#include <Kernel/types.h>

// FIXME: This should be a BlockDevice once we have BlockDevice.

class BochsVGADevice {
    AK_MAKE_ETERNAL
public:
    static BochsVGADevice& the();

    BochsVGADevice();

    PhysicalAddress framebuffer_address() const { return m_framebuffer_address; }
    void set_resolution(int width, int height);
    void set_y_offset(int);

private:
    void set_register(word index, word value);
    dword find_framebuffer_address();

    PhysicalAddress m_framebuffer_address;
};
