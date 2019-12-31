#include <Kernel/Devices/MBVGADevice.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

static MBVGADevice* s_the;

MBVGADevice& MBVGADevice::the()
{
    return *s_the;
}

MBVGADevice::MBVGADevice(PhysicalAddress addr, int pitch, int width, int height)
    : BlockDevice(29, 0)
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
    dbg() << "MBVGADevice address=" << addr << ", pitch=" << pitch << ", width=" << width << ", height=" << height;
    s_the = this;
}

KResultOr<Region*> MBVGADevice::mmap(Process& process, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot)
{
    ASSERT(offset == 0);
    ASSERT(size == framebuffer_size_in_bytes());
    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, framebuffer_size_in_bytes());
    auto* region = process.allocate_region_with_vmobject(
        preferred_vaddr,
        framebuffer_size_in_bytes(),
        move(vmobject),
        0,
        "MBVGA Framebuffer",
        prot);
    dbgprintf("MBVGA: %s(%u) created Region{%p} with size %u for framebuffer P%x with vaddr V%p\n",
        process.name().characters(), process.pid(),
        region, region->size(), m_framebuffer_address.as_ptr(), region->vaddr().get());
    ASSERT(region);
    return region;
}

int MBVGADevice::ioctl(FileDescription&, unsigned request, unsigned arg)
{
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        if (!current->process().validate_write_typed(out))
            return -EFAULT;
        *out = framebuffer_size_in_bytes();
        return 0;
    }
    case FB_IOCTL_GET_BUFFER: {
        auto* index = (int*)arg;
        if (!current->process().validate_write_typed(index))
            return -EFAULT;
        *index = 0;
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* resolution = (FBResolution*)arg;
        if (!current->process().validate_write_typed(resolution))
            return -EFAULT;
        resolution->pitch = m_framebuffer_pitch;
        resolution->width = m_framebuffer_width;
        resolution->height = m_framebuffer_height;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* resolution = (FBResolution*)arg;
        if (!current->process().validate_read_typed(resolution) || !current->process().validate_write_typed(resolution))
            return -EFAULT;
        resolution->pitch = m_framebuffer_pitch;
        resolution->width = m_framebuffer_width;
        resolution->height = m_framebuffer_height;
        return 0;
    }
    default:
        return -EINVAL;
    };
}
