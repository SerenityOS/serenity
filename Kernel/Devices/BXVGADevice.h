#pragma once

#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <LibDraw/Size.h>

class BXVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static BXVGADevice& the();

    BXVGADevice();

    PhysicalAddress framebuffer_address() const { return m_framebuffer_address; }
    void set_resolution(int width, int height);
    void set_y_offset(int);

    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t, int prot) override;

    size_t framebuffer_size_in_bytes() const { return m_framebuffer_size.area() * sizeof(u32) * 2; }
    Size framebuffer_size() const { return m_framebuffer_size; }

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;

    void set_register(u16 index, u16 value);
    u32 find_framebuffer_address();

    PhysicalAddress m_framebuffer_address;
    Size m_framebuffer_size;
};
