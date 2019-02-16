#include <Kernel/BochsVGADevice.h>
#include <Kernel/IO.h>
#include <Kernel/PCI.h>
#include <Kernel/MemoryManager.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

#define VBE_DISPI_IOPORT_INDEX           0x01CE
#define VBE_DISPI_IOPORT_DATA            0x01CF

#define VBE_DISPI_INDEX_ID               0x0
#define VBE_DISPI_INDEX_XRES             0x1
#define VBE_DISPI_INDEX_YRES             0x2
#define VBE_DISPI_INDEX_BPP              0x3
#define VBE_DISPI_INDEX_ENABLE           0x4
#define VBE_DISPI_INDEX_BANK             0x5
#define VBE_DISPI_INDEX_VIRT_WIDTH       0x6
#define VBE_DISPI_INDEX_VIRT_HEIGHT      0x7
#define VBE_DISPI_INDEX_X_OFFSET         0x8
#define VBE_DISPI_INDEX_Y_OFFSET         0x9
#define VBE_DISPI_DISABLED               0x00
#define VBE_DISPI_ENABLED                0x01
#define VBE_DISPI_LFB_ENABLED            0x40

#define BXVGA_DEV_IOCTL_SET_Y_OFFSET     1982
#define BXVGA_DEV_IOCTL_SET_RESOLUTION   1985
struct BXVGAResolution {
    int width;
    int height;
};

static BochsVGADevice* s_the;

BochsVGADevice& BochsVGADevice::the()
{
    return *s_the;
}

BochsVGADevice::BochsVGADevice()
    : BlockDevice(82, 413)
{
    s_the = this;
    m_framebuffer_address = PhysicalAddress(find_framebuffer_address());
}

Region* BochsVGADevice::mmap(Process& process, LinearAddress preferred_laddr, size_t offset, size_t size)
{
    ASSERT(offset == 0);
    ASSERT(size == framebuffer_size_in_bytes());
    auto framebuffer_vmo = VMObject::create_framebuffer_wrapper(framebuffer_address(), framebuffer_size_in_bytes());
    auto* region = process.allocate_region_with_vmo(preferred_laddr, framebuffer_size_in_bytes(), move(framebuffer_vmo), 0, "BochsVGADevice Framebuffer", true, true);
    kprintf("BochsVGADevice::mmap for %s(%u) mapped region %p for fb addr %p\n",
            process.name().characters(), process.pid(),
            region, framebuffer_address());
    ASSERT(region);
    return region;
}

void BochsVGADevice::set_register(word index, word data)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    IO::out16(VBE_DISPI_IOPORT_DATA, data);
}

void BochsVGADevice::set_resolution(int width, int height)
{
    set_register(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_DISABLED);
    set_register(VBE_DISPI_INDEX_XRES, width);
    set_register(VBE_DISPI_INDEX_YRES, height);
    set_register(VBE_DISPI_INDEX_VIRT_WIDTH, width);
    set_register(VBE_DISPI_INDEX_VIRT_HEIGHT, height * 2);
    set_register(VBE_DISPI_INDEX_BPP, 32);
    set_register(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED);
    set_register(VBE_DISPI_INDEX_BANK, 0);

    m_framebuffer_size = { width, height };
}

void BochsVGADevice::set_y_offset(int offset)
{
    set_register(VBE_DISPI_INDEX_Y_OFFSET, offset);
}

dword BochsVGADevice::find_framebuffer_address()
{
    static const PCI::ID bochs_vga_id = { 0x1234, 0x1111 };
    static const PCI::ID virtualbox_vga_id = { 0x80ee, 0xbeef };
    dword framebuffer_address = 0;
    PCI::enumerate_all([&framebuffer_address] (const PCI::Address& address, PCI::ID id) {
        if (id == bochs_vga_id || id == virtualbox_vga_id) {
            framebuffer_address = PCI::get_BAR0(address) & 0xfffffff0;
            kprintf("BochsVGA: framebuffer @ P%x\n", framebuffer_address);
        }
    });
    return framebuffer_address;
}

int BochsVGADevice::ioctl(Process& process, unsigned int request, unsigned int arg)
{
    switch (request) {
    case BXVGA_DEV_IOCTL_SET_Y_OFFSET:
        set_y_offset(arg);
        return 0;
    case BXVGA_DEV_IOCTL_SET_RESOLUTION: {
        auto* resolution = (const BXVGAResolution*)arg;
        if (!process.validate_read_typed(resolution))
            return -EFAULT;
        set_resolution(resolution->width, resolution->height);
        return 0;
    }
    default:
        return -EINVAL;
    };
}

bool BochsVGADevice::can_read(Process&) const
{
    ASSERT_NOT_REACHED();
}

bool BochsVGADevice::can_write(Process&) const
{
    ASSERT_NOT_REACHED();
}

ssize_t BochsVGADevice::read(Process&, byte*, size_t)
{
    ASSERT_NOT_REACHED();
}

ssize_t BochsVGADevice::write(Process&, const byte*, size_t)
{
    ASSERT_NOT_REACHED();
}
