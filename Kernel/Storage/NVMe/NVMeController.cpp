/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NVMeController.h"
#include "AK/Format.h"
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/SafeMem.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Sections.h>

namespace Kernel {
Atomic<u8> NVMeController::controller_id {};

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NVMeController>> NVMeController::try_initialize(const Kernel::PCI::DeviceIdentifier& device_identifier, bool is_queue_polled)
{
    auto controller = TRY(adopt_nonnull_ref_or_enomem(new NVMeController(device_identifier)));
    TRY(controller->initialize(is_queue_polled));
    NVMeController::controller_id++;
    return controller;
}

UNMAP_AFTER_INIT NVMeController::NVMeController(const PCI::DeviceIdentifier& device_identifier)
    : PCI::Device(device_identifier.address())
    , m_pci_device_id(device_identifier)
{
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::initialize(bool is_queue_polled)
{
    // Nr of queues = one queue per core
    auto nr_of_queues = Processor::count();
    auto irq = is_queue_polled ? Optional<u8> {} : m_pci_device_id.interrupt_line().value();

    PCI::enable_memory_space(m_pci_device_id.address());
    PCI::enable_bus_mastering(m_pci_device_id.address());
    m_bar = PCI::get_BAR0(m_pci_device_id.address()) & BAR_ADDR_MASK;
    static_assert(sizeof(ControllerRegister) == REG_SQ0TDBL_START);
    static_assert(sizeof(NVMeSubmission) == (1 << SQ_WIDTH));

    // Map only until doorbell register for the controller
    // Queues will individually map the doorbell register respectively
    m_controller_regs = TRY(Memory::map_typed_writable<volatile ControllerRegister>(PhysicalAddress(m_bar)));

    auto caps = m_controller_regs->cap;
    m_ready_timeout = Time::from_milliseconds((CAP_TO(caps) + 1) * 500); // CAP.TO is in 500ms units

    calculate_doorbell_stride();
    TRY(create_admin_queue(irq));
    VERIFY(m_admin_queue_ready == true);

    VERIFY(IO_QUEUE_SIZE < MQES(caps));
    dbgln_if(NVME_DEBUG, "NVMe: IO queue depth is: {}", IO_QUEUE_SIZE);

    // Create an IO queue per core
    for (u32 cpuid = 0; cpuid < nr_of_queues; ++cpuid) {
        // qid is zero is used for admin queue
        TRY(create_io_queue(cpuid + 1, irq));
    }
    TRY(identify_and_init_namespaces());
    return {};
}

bool NVMeController::wait_for_ready(bool expected_ready_bit_value)
{
    static constexpr size_t one_ms_io_delay = 1000;
    auto wait_iterations = m_ready_timeout.to_milliseconds();

    u32 expected_rdy = expected_ready_bit_value ? 1 : 0;
    while (((m_controller_regs->csts >> CSTS_RDY_BIT) & 0x1) != expected_rdy) {
        IO::delay(one_ms_io_delay);

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

bool NVMeController::reset_controller()
{
    if ((m_controller_regs->cc & (1 << CC_EN_BIT)) != 0) {
        // If the EN bit is already set, we need to wait
        // until the RDY bit is 1, otherwise the behavior is undefined
        if (!wait_for_ready(true))
            return false;
    }

    auto cc = m_controller_regs->cc;

    cc = cc & ~(1 << CC_EN_BIT);

    m_controller_regs->cc = cc;

    full_memory_barrier();

    // Wait until the RDY bit is cleared
    if (!wait_for_ready(false))
        return false;

    return true;
}

bool NVMeController::start_controller()
{
    if (!(m_controller_regs->cc & (1 << CC_EN_BIT))) {
        // If the EN bit is not already set, we need to wait
        // until the RDY bit is 0, otherwise the behavior is undefined
        if (!wait_for_ready(false))
            return false;
    }

    auto cc = m_controller_regs->cc;

    cc = cc | (1 << CC_EN_BIT);
    cc = cc | (CQ_WIDTH << CC_IOCQES_BIT);
    cc = cc | (SQ_WIDTH << CC_IOSQES_BIT);

    m_controller_regs->cc = cc;

    full_memory_barrier();

    // Wait until the RDY bit is set
    if (!wait_for_ready(true))
        return false;

    return true;
}

UNMAP_AFTER_INIT u32 NVMeController::get_admin_q_dept()
{
    u32 aqa = m_controller_regs->aqa;
    // Queue depth is 0 based
    u32 q_depth = min(ACQ_SIZE(aqa), ASQ_SIZE(aqa)) + 1;
    dbgln_if(NVME_DEBUG, "NVMe: Admin queue depth is {}", q_depth);
    return q_depth;
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::identify_and_init_namespaces()
{

    RefPtr<Memory::PhysicalPage> prp_dma_buffer;
    OwnPtr<Memory::Region> prp_dma_region;
    auto namespace_data_struct = TRY(ByteBuffer::create_zeroed(NVMe_IDENTIFY_SIZE));
    u32 active_namespace_list[NVMe_IDENTIFY_SIZE / sizeof(u32)];

    {
        auto buffer = TRY(MM.allocate_dma_buffer_page("Identify PRP", Memory::Region::Access::ReadWrite, prp_dma_buffer));
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
            dmesgln("Failed to identify active namespace command");
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
                dmesgln("Failed identify namespace with nsid {}", nsid);
                return EFAULT;
            }
            static_assert(sizeof(IdentifyNamespace) == NVMe_IDENTIFY_SIZE);
            if (void* fault_at; !safe_memcpy(&id_ns, prp_dma_region->vaddr().as_ptr(), NVMe_IDENTIFY_SIZE, fault_at)) {
                return EFAULT;
            }
            auto val = get_ns_features(id_ns);
            auto block_counts = val.get<0>();
            auto block_size = 1 << val.get<1>();

            dbgln_if(NVME_DEBUG, "NVMe: Block count is {} and Block size is {}", block_counts, block_size);

            m_namespaces.append(TRY(NVMeNameSpace::try_create(m_queues, controller_id.load(), nsid, block_counts, block_size)));
            m_device_count++;
            dbgln_if(NVME_DEBUG, "NVMe: Initialized namespace with NSID: {}", nsid);
        }
    }
    return {};
}

UNMAP_AFTER_INIT Tuple<u64, u8> NVMeController::get_ns_features(IdentifyNamespace& identify_data_struct)
{
    auto flbas = identify_data_struct.flbas & FLBA_SIZE_MASK;
    auto namespace_size = identify_data_struct.nsze;
    auto lba_format = identify_data_struct.lbaf[flbas];

    auto lba_size = (lba_format & LBA_SIZE_MASK) >> 16;
    return Tuple<u64, u8>(namespace_size, lba_size);
}

RefPtr<StorageDevice> NVMeController::device(u32 index) const
{
    return m_namespaces.at(index);
}

size_t NVMeController::devices_count() const
{
    return m_device_count;
}

bool NVMeController::reset()
{
    if (!reset_controller())
        return false;
    if (!start_controller())
        return false;
    return true;
}

bool NVMeController::shutdown()
{
    TODO();
    return false;
}

void NVMeController::complete_current_request([[maybe_unused]] AsyncDeviceRequest::RequestResult result)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::create_admin_queue(Optional<u8> irq)
{
    auto qdepth = get_admin_q_dept();
    OwnPtr<Memory::Region> cq_dma_region;
    NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_pages;
    OwnPtr<Memory::Region> sq_dma_region;
    NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_pages;
    auto cq_size = round_up_to_power_of_two(CQ_SIZE(qdepth), 4096);
    auto sq_size = round_up_to_power_of_two(SQ_SIZE(qdepth), 4096);
    if (!reset_controller()) {
        dmesgln("Failed to reset the NVMe controller");
        return EFAULT;
    }
    {
        auto buffer = TRY(MM.allocate_dma_buffer_pages(cq_size, "Admin CQ queue", Memory::Region::Access::ReadWrite, cq_dma_pages));
        cq_dma_region = move(buffer);
    }

    // Phase bit is important to determine completion, so zero out the space
    // so that we don't get any garbage phase bit value
    memset(cq_dma_region->vaddr().as_ptr(), 0, cq_size);

    {
        auto buffer = TRY(MM.allocate_dma_buffer_pages(sq_size, "Admin SQ queue", Memory::Region::Access::ReadWrite, sq_dma_pages));
        sq_dma_region = move(buffer);
    }
    auto doorbell_regs = TRY(Memory::map_typed_writable<volatile DoorbellRegister>(PhysicalAddress(m_bar + REG_SQ0TDBL_START)));

    m_controller_regs->acq = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(cq_dma_pages.first().paddr().as_ptr()));
    m_controller_regs->asq = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(sq_dma_pages.first().paddr().as_ptr()));

    if (!start_controller()) {
        dmesgln("Failed to restart the NVMe controller");
        return EFAULT;
    }
    set_admin_queue_ready_flag();
    m_admin_queue = TRY(NVMeQueue::try_create(0, irq, qdepth, move(cq_dma_region), cq_dma_pages, move(sq_dma_region), sq_dma_pages, move(doorbell_regs)));

    dbgln_if(NVME_DEBUG, "NVMe: Admin queue created");
    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> NVMeController::create_io_queue(u8 qid, Optional<u8> irq)
{
    OwnPtr<Memory::Region> cq_dma_region;
    NonnullRefPtrVector<Memory::PhysicalPage> cq_dma_pages;
    OwnPtr<Memory::Region> sq_dma_region;
    NonnullRefPtrVector<Memory::PhysicalPage> sq_dma_pages;
    auto cq_size = round_up_to_power_of_two(CQ_SIZE(IO_QUEUE_SIZE), 4096);
    auto sq_size = round_up_to_power_of_two(SQ_SIZE(IO_QUEUE_SIZE), 4096);

    {
        auto buffer = TRY(MM.allocate_dma_buffer_pages(cq_size, "IO CQ queue", Memory::Region::Access::ReadWrite, cq_dma_pages));
        cq_dma_region = move(buffer);
    }

    // Phase bit is important to determine completion, so zero out the space
    // so that we don't get any garbage phase bit value
    memset(cq_dma_region->vaddr().as_ptr(), 0, cq_size);

    {
        auto buffer = TRY(MM.allocate_dma_buffer_pages(sq_size, "IO SQ queue", Memory::Region::Access::ReadWrite, sq_dma_pages));
        sq_dma_region = move(buffer);
    }

    {
        NVMeSubmission sub {};
        sub.op = OP_ADMIN_CREATE_COMPLETION_QUEUE;
        sub.create_cq.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(cq_dma_pages.first().paddr().as_ptr()));
        sub.create_cq.cqid = qid;
        // The queue size is 0 based
        sub.create_cq.qsize = AK::convert_between_host_and_little_endian(IO_QUEUE_SIZE - 1);
        auto flags = irq.has_value() ? QUEUE_IRQ_ENABLED : QUEUE_IRQ_DISABLED;
        flags |= QUEUE_PHY_CONTIGUOUS;
        // TODO: Eventually move to MSI.
        // For now using pin based interrupts. Clear the first 16 bits
        // to use pin-based interrupts.
        sub.create_cq.cq_flags = AK::convert_between_host_and_little_endian(flags & 0xFFFF);
        submit_admin_command(sub, true);
    }
    {
        NVMeSubmission sub {};
        sub.op = OP_ADMIN_CREATE_SUBMISSION_QUEUE;
        sub.create_sq.prp1 = reinterpret_cast<u64>(AK::convert_between_host_and_little_endian(sq_dma_pages.first().paddr().as_ptr()));
        sub.create_sq.sqid = qid;
        // The queue size is 0 based
        sub.create_sq.qsize = AK::convert_between_host_and_little_endian(IO_QUEUE_SIZE - 1);
        auto flags = QUEUE_PHY_CONTIGUOUS;
        sub.create_sq.cqid = qid;
        sub.create_sq.sq_flags = AK::convert_between_host_and_little_endian(flags);
        submit_admin_command(sub, true);
    }

    auto queue_doorbell_offset = REG_SQ0TDBL_START + ((2 * qid) * (4 << m_dbl_stride));
    auto doorbell_regs = TRY(Memory::map_typed_writable<volatile DoorbellRegister>(PhysicalAddress(m_bar + queue_doorbell_offset)));

    m_queues.append(TRY(NVMeQueue::try_create(qid, irq, IO_QUEUE_SIZE, move(cq_dma_region), cq_dma_pages, move(sq_dma_region), sq_dma_pages, move(doorbell_regs))));
    dbgln_if(NVME_DEBUG, "NVMe: Created IO Queue with QID{}", m_queues.size());
    return {};
}
}
