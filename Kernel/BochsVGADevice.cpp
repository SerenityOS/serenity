#include <Kernel/BochsVGADevice.h>
#include <Kernel/IO.h>
#include <Kernel/PCI.h>

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
#define VBE_DISPI_DISABLED               0x00
#define VBE_DISPI_ENABLED                0x01
#define VBE_DISPI_LFB_ENABLED            0x40

static BochsVGADevice* s_the;

BochsVGADevice& BochsVGADevice::the()
{
    return *s_the;
}

BochsVGADevice::BochsVGADevice()
{
    s_the = this;
    m_framebuffer_address = PhysicalAddress(find_framebuffer_address());
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
    set_register(VBE_DISPI_INDEX_VIRT_HEIGHT, height);
    set_register(VBE_DISPI_INDEX_BPP, 32);
    set_register(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED);
    set_register(VBE_DISPI_INDEX_BANK, 0);
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
