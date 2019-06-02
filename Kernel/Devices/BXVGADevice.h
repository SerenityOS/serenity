#pragma once

#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PhysicalAddress.h>
#include <SharedGraphics/Size.h>

class BXVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static BXVGADevice& the();

    BXVGADevice();

    PhysicalAddress framebuffer_address() const { return m_framebuffer_address; }
    void set_resolution(int width, int height);
    void set_y_offset(int);

    virtual int ioctl(FileDescriptor&, unsigned request, unsigned arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescriptor&, LinearAddress preferred_laddr, size_t offset, size_t, int prot) override;

    size_t framebuffer_size_in_bytes() const { return m_framebuffer_size.area() * sizeof(dword) * 2; }
    Size framebuffer_size() const { return m_framebuffer_size; }

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(FileDescriptor&) const override;
    virtual bool can_write(FileDescriptor&) const override;
    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;

    void set_register(word index, word value);
    dword find_framebuffer_address();

    PhysicalAddress m_framebuffer_address;
    Size m_framebuffer_size;
};
