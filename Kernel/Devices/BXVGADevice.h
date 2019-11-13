#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/VM/PhysicalAddress.h>

class BXVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static BXVGADevice& the();

    BXVGADevice();

    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t, int prot) override;

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(const FileDescription&) const override { return true; }
    virtual bool can_write(const FileDescription&) const override { return true; }
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override { return -EINVAL; }
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override { return -EINVAL; }

    void set_register(u16 index, u16 value);
    u32 find_framebuffer_address();
    size_t framebuffer_size_in_bytes() const { return m_framebuffer_pitch * m_framebuffer_height * 2; }
    void set_resolution(int width, int height);
    void set_y_offset(int);

    PhysicalAddress m_framebuffer_address;
    int m_framebuffer_pitch { 0 };
    int m_framebuffer_width { 0 };
    int m_framebuffer_height { 0 };
    int m_y_offset { 0 };
};
