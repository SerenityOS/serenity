/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/VirtIO/VirtIOBlockDevice.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

namespace VirtIO {

// From Virtual I/O Device (VIRTIO) Version 1.2 spec:
// https://docs.oasis-open.org/virtio/virtio/v1.2/csd01/virtio-v1.2-csd01.html#x1-2740002

static constexpr u64 VIRTIO_BLK_F_BARRIER = 1ull << 0;       // Device supports request barriers.
static constexpr u64 VIRTIO_BLK_F_SIZE_MAX = 1ull << 1;      // Maximum size of any single segment is in size_max.
static constexpr u64 VIRTIO_BLK_F_SEG_MAX = 1ull << 2;       // Maximum number of segments in a request is in seg_max.
static constexpr u64 VIRTIO_BLK_F_GEOMETRY = 1ull << 4;      // Disk-style geometry specified in geometry.
static constexpr u64 VIRTIO_BLK_F_RO = 1ull << 5;            // Device is read-only.
static constexpr u64 VIRTIO_BLK_F_BLK_SIZE = 1ull << 6;      // Block size of disk is in blk_size.
static constexpr u64 VIRTIO_BLK_F_SCSI = 1ull << 7;          // Device supports scsi packet commands.
static constexpr u64 VIRTIO_BLK_F_FLUSH = 1ull << 9;         // Cache flush command support.
static constexpr u64 VIRTIO_BLK_F_TOPOLOGY = 1ull << 10;     // Device exports information on optimal I/O alignment.
static constexpr u64 VIRTIO_BLK_F_CONFIG_WCE = 1ull << 11;   // Device can toggle its cache between writeback and writethrough modes.
static constexpr u64 VIRTIO_BLK_F_DISCARD = 1ull << 13;      // Device can support discard command, maximum discard sectors size in max_discard_sectors and maximum discard segment number in max_discard_seg.
static constexpr u64 VIRTIO_BLK_F_WRITE_ZEROES = 1ull << 14; // Device can support write zeroes command, maximum write zeroes sectors size in max_write_zeroes_sectors and maximum write zeroes segment number in max_write_zeroes_seg.

static constexpr u64 VIRTIO_BLK_T_IN = 0;
static constexpr u64 VIRTIO_BLK_T_OUT = 1;
static constexpr u64 VIRTIO_BLK_T_FLUSH = 4;
static constexpr u64 VIRTIO_BLK_T_GET_ID = 8;
static constexpr u64 VIRTIO_BLK_T_GET_LIFETIME = 10;
static constexpr u64 VIRTIO_BLK_T_DISCARD = 11;
static constexpr u64 VIRTIO_BLK_T_WRITE_ZEROES = 13;
static constexpr u64 VIRTIO_BLK_T_SECURE_ERASE = 14;

static constexpr u64 VIRTIO_BLK_S_OK = 0;
static constexpr u64 VIRTIO_BLK_S_IOERR = 1;
static constexpr u64 VIRTIO_BLK_S_UNSUPP = 2;

struct [[gnu::packed]] VirtIOBlkConfig {
    LittleEndian<u64> capacity;
    LittleEndian<u32> size_max;
    LittleEndian<u32> seg_max;
    struct [[gnu::packed]] VirtIOBlkGeometry {
        LittleEndian<u16> cylinders;
        u8 heads;
        u8 sectors;
    } geometry;
    LittleEndian<u32> blk_size;
    struct [[gnu::packed]] VirtIOBlkTopology {
        // # of logical blocks per physical block (log2)
        u8 physical_block_exp;
        // offset of first aligned logical block
        u8 alignment_offset;
        // suggested minimum I/O size in blocks
        LittleEndian<u16> min_io_size;
        // optimal (suggested maximum) I/O size in blocks
        LittleEndian<u32> opt_io_size;
    } topology;
    u8 writeback;
    u8 unused0[3];
    LittleEndian<u32> max_discard_sectors;
    LittleEndian<u32> max_discard_seg;
    LittleEndian<u32> discard_sector_alignment;
    LittleEndian<u32> max_write_zeroes_sectors;
    LittleEndian<u32> max_write_zeroes_seg;
    u8 write_zeroes_may_unmap;
    u8 unused1[3];
};

struct [[gnu::packed]] VirtIOBlkReqHeader {
    LittleEndian<u32> type;
    LittleEndian<u32> reserved;
    LittleEndian<u64> sector;
};

struct [[gnu::packed]] VirtIOBlkReqTrailer {
    u8 status;
};

struct [[gnu::packed]] VirtIOBlkReq {
    VirtIOBlkReqHeader header;
    VirtIOBlkReqTrailer trailer;
};

}

using namespace VirtIO;

static constexpr u16 REQUESTQ = 0;
static constexpr u64 SECTOR_SIZE = 512;
static constexpr u64 INFLIGHT_BUFFER_SIZE = PAGE_SIZE * 16; // 128 blocks
static constexpr u64 MAX_ADDRESSABLE_BLOCK = 1ull << 32;    // FIXME: Supply effective device size.

UNMAP_AFTER_INIT VirtIOBlockDevice::VirtIOBlockDevice(
    NonnullOwnPtr<VirtIO::TransportEntity> transport,
    StorageDevice::LUNAddress lun,
    u32 hardware_relative_controller_id)
    : StorageDevice(lun, hardware_relative_controller_id, SECTOR_SIZE, MAX_ADDRESSABLE_BLOCK)
    , VirtIO::Device(move(transport))
{
}

UNMAP_AFTER_INIT ErrorOr<void> VirtIOBlockDevice::initialize_virtio_resources()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOBlockDevice::initialize_virtio_resources");
    TRY(VirtIO::Device::initialize_virtio_resources());

    m_header_buf = TRY(MM.allocate_contiguous_kernel_region(
        PAGE_SIZE, "VirtIOBlockDevice header_buf"sv, Memory::Region::Access::Read | Memory::Region::Access::Write));
    m_data_buf = TRY(MM.allocate_contiguous_kernel_region(
        INFLIGHT_BUFFER_SIZE, "VirtIOBlockDevice data_buf"sv, Memory::Region::Access::Read | Memory::Region::Access::Write));

    TRY(negotiate_features([&](u64) {
        return 0; // We rely on the basic feature set.
    }));
    TRY(setup_queues(1)); // REQUESTQ
    finish_init();
    return {};
}

ErrorOr<void> VirtIOBlockDevice::handle_device_config_change()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOBlockDevice::handle_device_config_change");
    return {};
}

void VirtIOBlockDevice::start_request(AsyncBlockDeviceRequest& request)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOBlockDevice::start_request type={}", (int)request.request_type());

    m_current_request.with([&](auto& current_request) {
        VERIFY(current_request.is_null());
        current_request = request;
    });

    if (maybe_start_request(request).is_error()) {
        m_current_request.with([&](auto& current_request) {
            VERIFY(current_request == request);
            current_request.clear();
        });
        request.complete(AsyncDeviceRequest::Failure);
    }
}

ErrorOr<void> VirtIOBlockDevice::maybe_start_request(AsyncBlockDeviceRequest& request)
{
    auto& queue = get_queue(REQUESTQ);
    SpinlockLocker queue_lock(queue.lock());
    VirtIO::QueueChain chain(queue);

    u64 data_size = block_size() * request.block_count();
    if (request.buffer_size() < data_size) {
        dmesgln("VirtIOBlockDevice: not enough space in the request buffer.");
        return Error::from_errno(EINVAL);
    }
    if (m_data_buf->size() < data_size + sizeof(VirtIOBlkReqTrailer)) {
        // TODO: Supply the provider buffer instead to avoid copies.
        dmesgln("VirtIOBlockDevice: not enough space in the internal buffer.");
        return Error::from_errno(ENOMEM);
    }

    // m_header_buf contains VirtIOBlkReqHeader and VirtIOBlkReqTrailer contingously
    // When adding to chain we insert the parts of m_header_buf (as device-readable)
    // and the data buffer in between (as device-writable if needed).
    VirtIOBlkReq* device_req = (VirtIOBlkReq*)m_header_buf->vaddr().as_ptr();

    device_req->header.reserved = 0;
    device_req->header.sector = request.block_index();
    device_req->trailer.status = 0;
    BufferType buffer_type;
    if (request.request_type() == AsyncBlockDeviceRequest::Read) {
        device_req->header.type = VIRTIO_BLK_T_IN;
        buffer_type = BufferType::DeviceWritable;
    } else if (request.request_type() == AsyncBlockDeviceRequest::Write) {
        device_req->header.type = VIRTIO_BLK_T_OUT;
        buffer_type = BufferType::DeviceReadable;
        TRY(request.read_from_buffer(request.buffer(), m_data_buf->vaddr().as_ptr(), data_size));
    } else {
        return Error::from_errno(EINVAL);
    }

    chain.add_buffer_to_chain(m_header_buf->physical_page(0)->paddr(), sizeof(VirtIOBlkReqHeader), BufferType::DeviceReadable);
    chain.add_buffer_to_chain(m_data_buf->physical_page(0)->paddr(), data_size, buffer_type);
    chain.add_buffer_to_chain(m_header_buf->physical_page(0)->paddr().offset(sizeof(VirtIOBlkReqHeader)), sizeof(VirtIOBlkReqTrailer), BufferType::DeviceWritable);
    supply_chain_and_notify(REQUESTQ, chain);
    return {};
}

void VirtIOBlockDevice::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOBlockDevice::handle_queue_update {}", queue_index);

    if (queue_index == REQUESTQ) {
        auto& queue = get_queue(REQUESTQ);
        SpinlockLocker queue_lock(queue.lock());

        size_t used;
        VirtIO::QueueChain popped_chain = queue.pop_used_buffer_chain(used);
        // Exactly one request is completed.
        VERIFY(popped_chain.length() == 3);
        VERIFY(!queue.new_data_available());

        auto work_res = g_io_work->try_queue([this]() {
            respond();
        });
        if (work_res.is_error()) {
            dmesgln("VirtIOBlockDevice::handle_queue_update error starting response: {}", work_res.error());
        }
        popped_chain.release_buffer_slots_to_queue();
    } else {
        dmesgln("VirtIOBlockDevice::handle_queue_update unexpected update for queue {}", queue_index);
    }
}

void VirtIOBlockDevice::respond()
{
    RefPtr<AsyncBlockDeviceRequest> request;

    m_current_request.with([&](auto& current_request) {
        VERIFY(current_request);
        request = current_request;
    });

    u64 data_size = block_size() * request->block_count();
    VirtIOBlkReq* device_req = (VirtIOBlkReq*)(m_header_buf->vaddr().as_ptr());

    // The order is important:
    // * first we finish reading up the data buf;
    // * then we unblock new requests by clearing m_current_request (thus new requests will be free to use the data buf)
    // * then unblock the caller (who may immediately come with another request and need m_current_request cleared).

    if (device_req->trailer.status == VIRTIO_BLK_S_OK && request->request_type() == AsyncBlockDeviceRequest::Read) {
        if (auto res = request->write_to_buffer(request->buffer(), m_data_buf->vaddr().as_ptr(), data_size); res.is_error()) {
            dmesgln("VirtIOBlockDevice::respond failed to read buffer: {}", res.error());
        }
    }

    m_current_request.with([&](auto& current_request) {
        current_request.clear();
    });

    request->complete(device_req->trailer.status == VIRTIO_BLK_S_OK
            ? AsyncDeviceRequest::Success
            : AsyncDeviceRequest::Failure);
}

}
