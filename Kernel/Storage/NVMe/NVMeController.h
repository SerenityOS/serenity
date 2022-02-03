/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <AK/Tuple.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Storage/NVMe/NVMeNameSpace.h>
#include <Kernel/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class NVMeController : public PCI::Device
    , public StorageController {
public:
    static ErrorOr<NonnullRefPtr<NVMeController>> try_initialize(PCI::DeviceIdentifier const&, bool is_queue_polled);
    ErrorOr<void> initialize(bool is_queue_polled);
    explicit NVMeController(PCI::DeviceIdentifier const&);
    RefPtr<StorageDevice> device(u32 index) const override;
    size_t devices_count() const override;

protected:
    bool reset() override;
    bool shutdown() override;
    void complete_current_request(AsyncDeviceRequest::RequestResult result) override;

public:
    bool reset_controller();
    bool start_controller();
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

    bool is_admin_queue_ready() { return m_admin_queue_ready; };
    void set_admin_queue_ready_flag() { m_admin_queue_ready = true; };

private:
    ErrorOr<void> identify_and_init_namespaces();
    Tuple<u64, u8> get_ns_features(IdentifyNamespace& identify_data_struct);
    ErrorOr<void> create_admin_queue(Optional<u8> irq);
    ErrorOr<void> create_io_queue(u8 qid, Optional<u8> irq);
    void calculate_doorbell_stride()
    {
        m_dbl_stride = (m_controller_regs->cap >> CAP_DBL_SHIFT) & CAP_DBL_MASK;
    }
    bool wait_for_ready(bool);

private:
    PCI::DeviceIdentifier m_pci_device_id;
    RefPtr<NVMeQueue> m_admin_queue;
    NonnullRefPtrVector<NVMeQueue> m_queues;
    NonnullRefPtrVector<NVMeNameSpace> m_namespaces;
    Memory::TypedMapping<volatile ControllerRegister> m_controller_regs;
    bool m_admin_queue_ready { false };
    size_t m_device_count {};
    AK::Time m_ready_timeout;
    u32 m_bar;
    u8 m_dbl_stride;
    static Atomic<u8> controller_id;
};
}
