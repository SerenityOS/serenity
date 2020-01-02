#include <AK/Optional.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/MMIOAccess.h>
#include <Kernel/VM/MemoryManager.h>

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096

uint32_t PCI::MMIOAccess::get_segments_count()
{
    return m_segments.size();
}
uint8_t PCI::MMIOAccess::get_segment_start_bus(u32 seg)
{
    ASSERT(m_segments.contains(seg));
    return m_segments.get(seg).value()->get_start_bus();
}
uint8_t PCI::MMIOAccess::get_segment_end_bus(u32 seg)
{
    ASSERT(m_segments.contains(seg));
    return m_segments.get(seg).value()->get_end_bus();
}

void PCI::MMIOAccess::initialize(ACPI_RAW::MCFG& mcfg)
{
    if (!PCI::Access::is_initialized())
        new PCI::MMIOAccess(mcfg);
}

PCI::MMIOAccess::MMIOAccess(ACPI_RAW::MCFG& raw_mcfg)
    : m_mcfg(raw_mcfg)
    , m_segments(*new HashMap<u16, MMIOSegment*>())
    , m_mapped_address(ChangeableAddress(0xFFFF, 0xFF, 0xFF, 0xFF))
{
    kprintf("PCI: Using MMIO Mechanism for PCI Configuartion Space Access\n");
    m_mmio_window = *AnonymousVMObject::create_with_size(PAGE_ROUND_UP(PCI_MMIO_CONFIG_SPACE_SIZE));
    m_mmio_window_region = MM.allocate_kernel_region_with_vmobject(*m_mmio_window, m_mmio_window->size(), "PCI MMIO", Region::Access::Read | Region::Access::Write);

    auto checkup_region = MM.allocate_kernel_region((PAGE_SIZE * 2), "PCI MCFG Checkup", Region::Access::Read | Region::Access::Write);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Checking MCFG Table length to choose the correct mapping size\n");
#endif
    mmap_region(*checkup_region, PhysicalAddress((u32)&raw_mcfg & 0xfffff000));
    ACPI_RAW::SDTHeader* sdt = (ACPI_RAW::SDTHeader*)(checkup_region->vaddr().get() + ((u32)&raw_mcfg & 0xfff));
    u32 length = sdt->length;
    u8 revision = sdt->revision;

    kprintf("PCI: MCFG, length - %u, revision %d\n", length, revision);
    checkup_region->unmap();

    auto mcfg_region = MM.allocate_kernel_region(PAGE_ROUND_UP(length) + PAGE_SIZE, "PCI Parsing MCFG", Region::Access::Read | Region::Access::Write);
    mmap_region(*mcfg_region, PhysicalAddress((u32)&raw_mcfg & 0xfffff000));

    ACPI_RAW::MCFG& mcfg = *((ACPI_RAW::MCFG*)(mcfg_region->vaddr().get() + ((u32)&raw_mcfg & 0xfff)));
#ifdef PCI_DEBUG
    dbgprintf("PCI: Checking MCFG @ V 0x%x, P 0x%x\n", &mcfg, &raw_mcfg);
#endif

    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI_RAW::MCFG)) / sizeof(ACPI_RAW::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u32 lower_addr = mcfg.descriptors[index].base_addr;

        m_segments.set(index, new PCI::MMIOSegment(PhysicalAddress(lower_addr), start_bus, end_bus));
        kprintf("PCI: New PCI segment @ P 0x%x, PCI buses (%d-%d)\n", lower_addr, start_bus, end_bus);
    }
    mcfg_region->unmap();
    kprintf("PCI: MMIO segments - %d\n", m_segments.size());
    InterruptDisabler disabler;
#ifdef PCI_DEBUG
    dbgprintf("PCI: mapped address (%w:%b:%b.%b)\n", m_mapped_address.seg(), m_mapped_address.bus(), m_mapped_address.slot(), m_mapped_address.function());
#endif
    map_device(Address(0, 0, 0, 0));
#ifdef PCI_DEBUG
    dbgprintf("PCI: Default mapped address (%w:%b:%b.%b)\n", m_mapped_address.seg(), m_mapped_address.bus(), m_mapped_address.slot(), m_mapped_address.function());
#endif
}

void PCI::MMIOAccess::map_device(Address address)
{
    if (m_mapped_address == address)
        return;
    // FIXME: Map and put some lock!
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(m_segments.contains(address.seg()));
    auto segment = m_segments.get(address.seg());
    PhysicalAddress segment_lower_addr = segment.value()->get_paddr();
    PhysicalAddress device_physical_mmio_space = segment_lower_addr.offset(
        PCI_MMIO_CONFIG_SPACE_SIZE * address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * address.slot() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS) * (address.bus() - segment.value()->get_start_bus()));

#ifdef PCI_DEBUG
    dbgprintf("PCI: Mapping device @ pci (%w:%b:%b.%b), V 0x%x, P 0x%x\n", address.seg(), address.bus(), address.slot(), address.function(), m_mmio_window_region->vaddr().get(), device_physical_mmio_space.get());
#endif
    MM.map_for_kernel(m_mmio_window_region->vaddr(), device_physical_mmio_space);
    m_mapped_address = address;
}

u8 PCI::MMIOAccess::read8_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Reading field %u, Address(%w:%b:%b.%b)\n", field, address.seg(), address.bus(), address.slot(), address.function());
#endif
    map_device(address);
    return *((u8*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

u16 PCI::MMIOAccess::read16_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Reading field %u, Address(%w:%b:%b.%b)\n", field, address.seg(), address.bus(), address.slot(), address.function());
#endif
    map_device(address);
    return *((u16*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

u32 PCI::MMIOAccess::read32_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Reading field %u, Address(%w:%b:%b.%b)\n", field, address.seg(), address.bus(), address.slot(), address.function());
#endif
    map_device(address);
    return *((u32*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

void PCI::MMIOAccess::write8_field(Address address, u32 field, u8 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Write to field %u, Address(%w:%b:%b.%b), value 0x%x\n", field, address.seg(), address.bus(), address.slot(), address.function(), value);
#endif
    map_device(address);
    *((u8*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}
void PCI::MMIOAccess::write16_field(Address address, u32 field, u16 value)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Write to field %u, Address(%w:%b:%b.%b), value 0x%x\n", field, address.seg(), address.bus(), address.slot(), address.function(), value);
#endif
    map_device(address);
    *((u16*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}
void PCI::MMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
#ifdef PCI_DEBUG
    dbgprintf("PCI: Write to field %u, Address(%w:%b:%b.%b), value 0x%x\n", field, address.seg(), address.bus(), address.slot(), address.function(), value);
#endif
    map_device(address);
    *((u32*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}

void PCI::MMIOAccess::enumerate_all(Function<void(Address, ID)>& callback)
{
    for (u16 seg = 0; seg < m_segments.size(); seg++) {
#ifdef PCI_DEBUG
        dbgprintf("PCI: Enumerating Memory mapped IO segment %u\n", seg);
#endif
        // Single PCI host controller.
        if ((read8_field(Address(seg), PCI_HEADER_TYPE) & 0x80) == 0) {
            enumerate_bus(-1, 0, callback);
            return;
        }

        // Multiple PCI host controllers.
        for (u8 function = 0; function < 8; ++function) {
            if (read16_field(Address(seg, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
                break;
            enumerate_bus(-1, function, callback);
        }
    }
}

void PCI::MMIOAccess::mmap(VirtualAddress vaddr, PhysicalAddress paddr, u32 length)
{
    unsigned i = 0;
    while (length >= PAGE_SIZE) {
        MM.map_for_kernel(VirtualAddress(vaddr.offset(i * PAGE_SIZE).get()), PhysicalAddress(paddr.offset(i * PAGE_SIZE).get()));
#ifdef PCI_DEBUG
        dbgprintf("PCI: map - V 0x%x -> P 0x%x\n", vaddr.offset(i * PAGE_SIZE).get(), paddr.offset(i * PAGE_SIZE).get());
#endif
        length -= PAGE_SIZE;
        i++;
    }
    if (length > 0) {
        MM.map_for_kernel(vaddr.offset(i * PAGE_SIZE), paddr.offset(i * PAGE_SIZE), true);
    }
#ifdef PCI_DEBUG
    dbgprintf("PCI: Finished mapping\n");
#endif
}

void PCI::MMIOAccess::mmap_region(Region& region, PhysicalAddress paddr)
{
#ifdef PCI_DEBUG
    dbgprintf("PCI: Mapping region, size - %u\n", region.size());
#endif
    mmap(region.vaddr(), paddr, region.size());
}

PCI::MMIOSegment::MMIOSegment(PhysicalAddress segment_base_addr, u8 start_bus, u8 end_bus)
    : m_base_addr(segment_base_addr)
    , m_start_bus(start_bus)
    , m_end_bus(end_bus)
{
}
u8 PCI::MMIOSegment::get_start_bus()
{
    return m_start_bus;
}
u8 PCI::MMIOSegment::get_end_bus()
{
    return m_end_bus;
}

size_t PCI::MMIOSegment::get_size()
{
    return (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS * (get_end_bus() - get_start_bus()));
}

PhysicalAddress PCI::MMIOSegment::get_paddr()
{
    return m_base_addr;
}
