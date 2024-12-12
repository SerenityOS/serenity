/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/NVMe/NVMeController.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NVMeController>> NVMeController::try_initialize(Kernel::PCI::DeviceIdentifier const& device_identifier, bool is_queue_polled)
{
    auto controller = TRY(adopt_nonnull_ref_or_enomem(new NVMeController(device_identifier, StorageManagement::generate_relative_nvme_controller_id({}))));
    TRY(controller->initialize(is_queue_polled));
    return controller;
}

UNMAP_AFTER_INIT NVMeController::NVMeController(const PCI::DeviceIdentifier& device_identifier, u32 hardware_relative_controller_id)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(device_identifier))
    , StorageController(hardware_relative_controller_id)
{
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::initialize(bool is_queue_polled)
{
    // Nr of queues = one queue per core
    auto nr_of_queues = Processor::count();
    auto queue_type = is_queue_polled ? QueueType::Polled : QueueType::IRQ;

    PCI::enable_memory_space(device_identifier());
    PCI::enable_bus_mastering(device_identifier());
    m_bar = TRY(PCI::get_bar_address(device_identifier(), PCI::HeaderType0BaseRegister::BAR0));
    static_assert(sizeof(ControllerRegister) == REG_SQ0TDBL_START);
    static_assert(sizeof(NVMeSubmission) == (1 << SQ_WIDTH));

    // Map only until doorbell register for the controller
    // Queues will individually map the doorbell register respectively
    m_controller_regs = TRY(Memory::map_typed_writable<ControllerRegister volatile>(m_bar));

    auto caps = m_controller_regs->cap;
    m_ready_timeout = Duration::from_milliseconds((CAP_TO(caps) + 1) * 500); // CAP.TO is in 500ms units

    calculate_doorbell_stride();
    if (queue_type == QueueType::IRQ) {
        // IO queues + 1 admin queue
        m_irq_type = TRY(reserve_irqs(nr_of_queues + 1, true));
    }

    TRY(create_admin_queue(queue_type));
    VERIFY(m_admin_queue_ready == true);

    VERIFY(IO_QUEUE_SIZE < MQES(caps));
    dbgln_if(NVME_DEBUG, "NVMe: IO queue depth is: {}", IO_QUEUE_SIZE);

    TRY(identify_and_init_controller());
    // Create an IO queue per core
    for (u32 cpuid = 0; cpuid < nr_of_queues; ++cpuid) {
        // qid is zero is used for admin queue
        TRY(create_io_queue(cpuid + 1, queue_type));
    }
    TRY(identify_and_init_namespaces());
    return {};
}

bool NVMeController::wait_for_ready(bool expected_ready_bit_value)
{
    constexpr size_t one_ms_io_delay = 1000;
    auto wait_iterations = m_ready_timeout.to_milliseconds();

    u32 expected_rdy = expected_ready_bit_value ? 1 : 0;
    while (((m_controller_regs->csts >> CSTS_RDY_BIT) & 0x1) != expected_rdy) {
        microseconds_delay(one_ms_io_delay);

        if (--wait_iterations == 0) {
            if (((m_controller_regs->csts >> CSTS_RDY_BIT) & 0x1) != expected_rdy) {
                dbgln_if(NVME_DEBUG, "NVMEController: CSTS.RDY still not set to {} after {} ms", expected_rdy, m_ready_timeout.to_milliseconds());
                return false;
            }
            break;
        }
    }
    return true;
}

ErrorOr<void> NVMeController::reset_controller()
{
    if ((m_controller_regs->cc & (1 << CC_EN_BIT)) != 0) {
        // If the EN bit is already set, we need to wait
        // until the RDY bit is 1, otherwise the behavior is undefined
        if (!wait_for_ready(true))
            return Error::from_errno(ETIMEDOUT);
    }

    auto cc = m_controller_regs->cc;

    cc = cc & ~(1 << CC_EN_BIT);

    m_controller_regs->cc = cc;

    full_memory_barrier();

    // Wait until the RDY bit is cleared
    if (!wait_for_ready(false))
        return Error::from_errno(ETIMEDOUT);

    return {};
}

ErrorOr<void> NVMeController::start_controller()
{
    if (!(m_controller_regs->cc & (1 << CC_EN_BIT))) {
        // If the EN bit is not already set, we need to wait
        // until the RDY bit is 0, otherwise the behavior is undefined
        if (!wait_for_ready(false))
            return Error::from_errno(ETIMEDOUT);
    }

    auto cc = m_controller_regs->cc;

    cc = cc | (1 << CC_EN_BIT);
    cc = cc | (CQ_WIDTH << CC_IOCQES_BIT);
    cc = cc | (SQ_WIDTH << CC_IOSQES_BIT);

    m_controller_regs->cc = cc;

    full_memory_barrier();

    // Wait until the RDY bit is set
    if (!wait_for_ready(true))
        return Error::from_errno(ETIMEDOUT);

    return {};
}

UNMAP_AFTER_INIT void NVMeController::set_admin_q_depth()
{
    // Queue depth is 0 based
    u16 queue_depth = ADMIN_QUEUE_SIZE - 1;
    m_controller_regs->aqa = queue_depth | (queue_depth << AQA_ACQ_SHIFT);
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::identify_and_init_namespaces()
{

    RefPtr<Memory::PhysicalRAMPage> prp_dma_buffer;
    OwnPtr<Memory::Region> prp_dma_region;
    auto namespace_data_struct = TRY(ByteBuffer::create_zeroed(NVMe_IDENTIFY_SIZE));
    u32 active_namespace_list[NVMe_IDENTIFY_SIZE / sizeof(u32)];

    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_page("Identify PRP"sv, Memory::Region::Access::ReadWrite, prp_dma_buffer, Memory::MemoryType::IO));
        prp_dma_region = move(buffer);
    }

    // Get the active namespace
    {
        NVMeSubmission sub {};
        u16 status = 0;
        sub.op = OP_ADMIN_IDENTIFY;
        sub.identify.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(prp_dma_buffer->paddr().as_ptr()));
        sub.identify.cns = NVMe_CNS_ID_ACTIVE_NS & 0xff;
        status = submit_admin_command(sub, true);
        if (status) {
            dmesgln_pci(*this, "Failed to identify active namespace command");
            return EFAULT;
        }
        if (void* fault_at; !safe_memcpy(active_namespace_list, prp_dma_region->vaddr().as_ptr(), NVMe_IDENTIFY_SIZE, fault_at)) {
            return EFAULT;
        }
    }
    // Get the NAMESPACE attributes
    {
        NVMeSubmission sub {};
        IdentifyNamespace id_ns {};
        u16 status = 0;
        for (auto nsid : active_namespace_list) {
            memset(prp_dma_region->vaddr().as_ptr(), 0, NVMe_IDENTIFY_SIZE);
            // Invalid NS
            if (nsid == 0)
                break;
            sub.op = OP_ADMIN_IDENTIFY;
            sub.identify.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(prp_dma_buffer->paddr().as_ptr()));
            sub.identify.cns = NVMe_CNS_ID_NS & 0xff;
            sub.identify.nsid = nsid;
            status = submit_admin_command(sub, true);
            if (status) {
                dmesgln_pci(*this, "Failed identify namespace with nsid {}", nsid);
                return EFAULT;
            }
            static_assert(sizeof(IdentifyNamespace) == NVMe_IDENTIFY_SIZE);
            if (void* fault_at; !safe_memcpy(&id_ns, prp_dma_region->vaddr().as_ptr(), NVMe_IDENTIFY_SIZE, fault_at)) {
                return EFAULT;
            }
            auto [block_counts, lba_size] = get_ns_features(id_ns);
            auto block_size = 1 << lba_size;

            dbgln_if(NVME_DEBUG, "NVMe: Block count is {} and Block size is {}", block_counts, block_size);

            m_namespaces.append(TRY(NVMeNameSpace::create(*this, m_queues, nsid, block_counts, block_size)));
            m_device_count++;
            dbgln_if(NVME_DEBUG, "NVMe: Initialized namespace with NSID: {}", nsid);
        }
    }
    return {};
}

ErrorOr<void> NVMeController::identify_and_init_controller()
{
    RefPtr<Memory::PhysicalRAMPage> prp_dma_buffer;
    OwnPtr<Memory::Region> prp_dma_region;
    IdentifyController ctrl {};

    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_page("Identify PRP"sv, Memory::Region::Access::ReadWrite, prp_dma_buffer, Memory::MemoryType::IO));
        prp_dma_region = move(buffer);
    }

    // Check if the controller supports shadow doorbell
    {
        NVMeSubmission sub {};
        u16 status = 0;
        sub.op = OP_ADMIN_IDENTIFY;
        sub.identify.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(prp_dma_buffer->paddr().as_ptr()));
        sub.identify.cns = NVMe_CNS_ID_CTRL & 0xff;
        status = submit_admin_command(sub, true);
        if (status) {
            dmesgln_pci(*this, "Failed to identify active namespace command");
            return EFAULT;
        }
        if (void* fault_at; !safe_memcpy(&ctrl, prp_dma_region->vaddr().as_ptr(), NVMe_IDENTIFY_SIZE, fault_at)) {
            return EFAULT;
        }
    }

    if (ctrl.oacs & ID_CTRL_SHADOW_DBBUF_MASK) {
        OwnPtr<Memory::Region> dbbuf_dma_region;
        OwnPtr<Memory::Region> eventidx_dma_region;

        {
            // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
            auto buffer = TRY(MM.allocate_dma_buffer_page("shadow dbbuf"sv, Memory::Region::Access::ReadWrite, m_dbbuf_shadow_page, Memory::MemoryType::IO));
            dbbuf_dma_region = move(buffer);
            memset(dbbuf_dma_region->vaddr().as_ptr(), 0, PAGE_SIZE);
        }

        {
            // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
            auto buffer = TRY(MM.allocate_dma_buffer_page("eventidx"sv, Memory::Region::Access::ReadWrite, m_dbbuf_eventidx_page, Memory::MemoryType::IO));
            eventidx_dma_region = move(buffer);
            memset(eventidx_dma_region->vaddr().as_ptr(), 0, PAGE_SIZE);
        }

        {
            NVMeSubmission sub {};
            sub.op = OP_ADMIN_DBBUF_CONFIG;
            sub.dbbuf_cmd.data_ptr.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_dbbuf_shadow_page->paddr().as_ptr()));
            sub.dbbuf_cmd.data_ptr.prp2 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(m_dbbuf_eventidx_page->paddr().as_ptr()));

            submit_admin_command(sub, true);
        }

        dbgln_if(NVME_DEBUG, "Shadow doorbell Enabled!");
    }

    return {};
}

UNMAP_AFTER_INIT NVMeController::NSFeatures NVMeController::get_ns_features(IdentifyNamespace& identify_data_struct)
{
    auto flbas = identify_data_struct.flbas & FLBA_SIZE_MASK;
    auto namespace_size = identify_data_struct.nsze;
    auto lba_format = identify_data_struct.lbaf[flbas];

    auto lba_size = (lba_format & LBA_SIZE_MASK) >> 16;
    return { namespace_size, static_cast<u8>(lba_size) };
}

LockRefPtr<StorageDevice> NVMeController::device(u32 index) const
{
    // FIXME: Remove this once we get rid of this hacky method in the future.
    auto device = m_namespaces.at(index);
    return *device;
}

size_t NVMeController::devices_count() const
{
    return m_device_count;
}

ErrorOr<void> NVMeController::reset()
{
    TRY(reset_controller());
    TRY(start_controller());
    return {};
}

void NVMeController::complete_current_request([[maybe_unused]] AsyncDeviceRequest::RequestResult result)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::create_admin_queue(QueueType queue_type)
{
    OwnPtr<Memory::Region> cq_dma_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> cq_dma_pages;
    OwnPtr<Memory::Region> sq_dma_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> sq_dma_pages;
    set_admin_q_depth();
    auto cq_size = round_up_to_power_of_two(CQ_SIZE(ADMIN_QUEUE_SIZE), 4096);
    auto sq_size = round_up_to_power_of_two(SQ_SIZE(ADMIN_QUEUE_SIZE), 4096);
    auto maybe_error = reset_controller();
    if (maybe_error.is_error()) {
        dmesgln_pci(*this, "Failed to reset the NVMe controller");
        return maybe_error;
    }
    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_pages(cq_size, "Admin CQ queue"sv, Memory::Region::Access::ReadWrite, cq_dma_pages, Memory::MemoryType::IO));
        cq_dma_region = move(buffer);
    }

    // Phase bit is important to determine completion, so zero out the space
    // so that we don't get any garbage phase bit value
    memset(cq_dma_region->vaddr().as_ptr(), 0, cq_size);

    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_pages(sq_size, "Admin SQ queue"sv, Memory::Region::Access::ReadWrite, sq_dma_pages, Memory::MemoryType::IO));
        sq_dma_region = move(buffer);
    }
    auto doorbell_regs = TRY(Memory::map_typed_writable<DoorbellRegister volatile>(m_bar.offset(REG_SQ0TDBL_START)));
    Doorbell doorbell = {
        .mmio_reg = move(doorbell_regs),
        .dbbuf_shadow = {},
        .dbbuf_eventidx = {},
    };

    m_controller_regs->acq = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(cq_dma_pages.first()->paddr().as_ptr()));
    m_controller_regs->asq = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(sq_dma_pages.first()->paddr().as_ptr()));

    Optional<u8> irq;
    if (queue_type == QueueType::IRQ)
        irq = TRY(allocate_irq(0)); // Admin queue always uses the 0th index when using MSIx

    maybe_error = start_controller();
    if (maybe_error.is_error()) {
        dmesgln_pci(*this, "Failed to restart the NVMe controller");
        return maybe_error;
    }
    set_admin_queue_ready_flag();
    m_admin_queue = TRY(NVMeQueue::try_create(*this, 0, irq, ADMIN_QUEUE_SIZE, move(cq_dma_region), move(sq_dma_region), move(doorbell), queue_type));

    dbgln_if(NVME_DEBUG, "NVMe: Admin queue created");
    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::create_io_queue(u8 qid, QueueType queue_type)
{
    OwnPtr<Memory::Region> cq_dma_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> cq_dma_pages;
    OwnPtr<Memory::Region> sq_dma_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> sq_dma_pages;
    auto cq_size = round_up_to_power_of_two(CQ_SIZE(IO_QUEUE_SIZE), 4096);
    auto sq_size = round_up_to_power_of_two(SQ_SIZE(IO_QUEUE_SIZE), 4096);

    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_pages(cq_size, "IO CQ queue"sv, Memory::Region::Access::ReadWrite, cq_dma_pages, Memory::MemoryType::IO));
        cq_dma_region = move(buffer);
    }

    // Phase bit is important to determine completion, so zero out the space
    // so that we don't get any garbage phase bit value
    memset(cq_dma_region->vaddr().as_ptr(), 0, cq_size);

    {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        auto buffer = TRY(MM.allocate_dma_buffer_pages(sq_size, "IO SQ queue"sv, Memory::Region::Access::ReadWrite, sq_dma_pages, Memory::MemoryType::IO));
        sq_dma_region = move(buffer);
    }

    {
        NVMeSubmission sub {};
        sub.op = OP_ADMIN_CREATE_COMPLETION_QUEUE;
        sub.create_cq.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(cq_dma_pages.first()->paddr().as_ptr()));
        sub.create_cq.cqid = qid;
        // The queue size is 0 based
        sub.create_cq.qsize = AK::convert_between_host_and_little_endian(IO_QUEUE_SIZE - 1);
        auto flags = (queue_type == QueueType::IRQ) ? QUEUE_IRQ_ENABLED : QUEUE_IRQ_DISABLED;
        flags |= QUEUE_PHY_CONTIGUOUS;
        // When using MSIx interrupts, qid is used as an index into the interrupt table
        if (m_irq_type.has_value() && m_irq_type.value() != PCI::InterruptType::PIN)
            sub.create_cq.irq_vector = qid;
        else
            sub.create_cq.irq_vector = 0;
        sub.create_cq.cq_flags = AK::convert_between_host_and_little_endian(flags & 0xFFFF);
        submit_admin_command(sub, true);
    }
    {
        NVMeSubmission sub {};
        sub.op = OP_ADMIN_CREATE_SUBMISSION_QUEUE;
        sub.create_sq.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(sq_dma_pages.first()->paddr().as_ptr()));
        sub.create_sq.sqid = qid;
        // The queue size is 0 based
        sub.create_sq.qsize = AK::convert_between_host_and_little_endian(IO_QUEUE_SIZE - 1);
        auto flags = QUEUE_PHY_CONTIGUOUS;
        sub.create_sq.cqid = qid;
        sub.create_sq.sq_flags = AK::convert_between_host_and_little_endian(flags);
        submit_admin_command(sub, true);
    }

    auto queue_doorbell_offset = (2 * qid) * (4 << m_dbl_stride);
    auto doorbell_regs = TRY(Memory::map_typed_writable<DoorbellRegister volatile>(m_bar.offset(REG_SQ0TDBL_START + queue_doorbell_offset)));
    Memory::TypedMapping<DoorbellRegister> shadow_doorbell_regs {};
    Memory::TypedMapping<DoorbellRegister> eventidx_doorbell_regs {};

    if (!m_dbbuf_shadow_page.is_null()) {
        shadow_doorbell_regs = TRY(Memory::map_typed_writable<DoorbellRegister>(m_dbbuf_shadow_page->paddr().offset(queue_doorbell_offset)));
        eventidx_doorbell_regs = TRY(Memory::map_typed_writable<DoorbellRegister>(m_dbbuf_eventidx_page->paddr().offset(queue_doorbell_offset)));
    }

    Doorbell doorbell = {
        .mmio_reg = move(doorbell_regs),
        .dbbuf_shadow = move(shadow_doorbell_regs),
        .dbbuf_eventidx = move(eventidx_doorbell_regs),
    };

    auto irq = TRY(allocate_irq(qid));

    m_queues.append(TRY(NVMeQueue::try_create(*this, qid, irq, IO_QUEUE_SIZE, move(cq_dma_region), move(sq_dma_region), move(doorbell), queue_type)));
    dbgln_if(NVME_DEBUG, "NVMe: Created IO Queue with QID{}", m_queues.size());
    return {};
}
}
