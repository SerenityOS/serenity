#pragma once

#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/VM/PhysicalAddress.h>

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

    size_t framebuffer_size_in_bytes() const { return m_framebuffer_width * m_framebuffer_height * sizeof(u32) * 2; }
    int framebuffer_width() const { return m_framebuffer_width; }
    int framebuffer_height() const { return m_framebuffer_height; }

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;

    void set_register(u16 index, u16 value);
    u32 find_framebuffer_address();

    PhysicalAddress m_framebuffer_address;
    int m_framebuffer_width { 0 };
    int m_framebuffer_height { 0 };
};
