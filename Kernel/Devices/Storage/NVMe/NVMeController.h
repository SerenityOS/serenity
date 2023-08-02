/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Time.h>
#include <AK/Tuple.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Devices/Storage/NVMe/NVMeNameSpace.h>
#include <Kernel/Devices/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Devices/Storage/StorageController.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class NVMeController : public PCI::Device
    , public StorageController {
public:
    static ErrorOr<NonnullRefPtr<NVMeController>> try_initialize(PCI::DeviceIdentifier const&, bool is_queue_polled);
    ErrorOr<void> initialize(bool is_queue_polled);
    LockRefPtr<StorageDevice> device(u32 index) const override;
    size_t devices_count() const override;
    virtual StringView device_name() const override { return "NVMeController"sv; }

protected:
    ErrorOr<void> reset() override;
    ErrorOr<void> shutdown() override;
    void complete_current_request(AsyncDeviceRequest::RequestResult result) override;

public:
    ErrorOr<void> reset_controller();
    ErrorOr<void> start_controller();
    u32 get_admin_q_dept();

    u16 submit_admin_command(NVMeSubmission& sub, bool sync = false)
    {
        // First queue is always the admin queue
        if (sync) {
            return m_admin_queue->submit_sync_sqe(sub);
        }
        m_admin_queue->submit_sqe(sub);
        return 0;
    }

    bool is_admin_queue_ready() { return m_admin_queue_ready; }
    void set_admin_queue_ready_flag() { m_admin_queue_ready = true; }

private:
    NVMeController(PCI::DeviceIdentifier const&, u32 hardware_relative_controller_id);

    ErrorOr<void> identify_and_init_namespaces();
    ErrorOr<void> identify_and_init_controller();
    Tuple<u64, u8> get_ns_features(IdentifyNamespace& identify_data_struct);
    ErrorOr<void> create_admin_queue(QueueType queue_type);
    ErrorOr<void> create_io_queue(u8 qid, QueueType queue_type);
    void calculate_doorbell_stride()
    {
        m_dbl_stride = (m_controller_regs->cap >> CAP_DBL_SHIFT) & CAP_DBL_MASK;
    }
    bool wait_for_ready(bool);

private:
    LockRefPtr<NVMeQueue> m_admin_queue;
    Vector<NonnullLockRefPtr<NVMeQueue>> m_queues;
    Vector<NonnullLockRefPtr<NVMeNameSpace>> m_namespaces;
    Memory::TypedMapping<ControllerRegister volatile> m_controller_regs;
    RefPtr<Memory::PhysicalPage> m_dbbuf_shadow_page;
    RefPtr<Memory::PhysicalPage> m_dbbuf_eventidx_page;
    bool m_admin_queue_ready { false };
    size_t m_device_count { 0 };
    AK::Duration m_ready_timeout;
    u32 m_bar { 0 };
    u8 m_dbl_stride { 0 };
    PCI::InterruptType m_irq_type;
    QueueType m_queue_type { QueueType::IRQ };
    static Atomic<u8> s_controller_id;
};
}
