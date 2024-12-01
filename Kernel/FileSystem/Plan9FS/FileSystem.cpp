/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/Inode.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> Plan9FS::try_create(OpenFileDescription& file_description, FileSystemSpecificOptions const&)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Plan9FS(file_description)));
}

Plan9FS::Plan9FS(OpenFileDescription& file_description)
    : FileBackedFileSystem(file_description)
    , m_completion_blocker(*this)
{
}

ErrorOr<void> Plan9FS::prepare_to_clear_last_mount(Inode&)
{
    // FIXME: Do proper cleaning here.
    return {};
}

Plan9FS::~Plan9FS()
{
    // Make sure to destroy the root inode before the FS gets destroyed.
    if (m_root_inode) {
        VERIFY(m_root_inode->ref_count() == 1);
        m_root_inode = nullptr;
    }
}

bool Plan9FS::is_initialized_while_locked()
{
    VERIFY(m_lock.is_locked());
    return !m_root_inode.is_null();
}

ErrorOr<void> Plan9FS::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());

    ensure_thread();

    Plan9FSMessage version_message { *this, Plan9FSMessage::Type::Tversion };
    version_message << (u32)m_max_message_size << "9P2000.L"sv;

    TRY(post_message_and_wait_for_a_reply(version_message));

    u32 msize;
    StringView remote_protocol_version;
    version_message >> msize >> remote_protocol_version;
    dbgln("Remote supports msize={} and protocol version {}", msize, remote_protocol_version);
    m_remote_protocol_version = parse_protocol_version(remote_protocol_version);
    m_max_message_size = min(m_max_message_size, (size_t)msize);

    // TODO: auth

    u32 root_fid = allocate_fid();
    Plan9FSMessage attach_message { *this, Plan9FSMessage::Type::Tattach };
    // FIXME: This needs a user name and an "export" name; but how do we get them?
    // Perhaps initialize() should accept a string of FS-specific options...
    attach_message << root_fid << (u32)-1 << "sergey"sv
                   << "/"sv;
    if (m_remote_protocol_version >= ProtocolVersion::v9P2000u)
        attach_message << (u32)-1;

    TRY(post_message_and_wait_for_a_reply(attach_message));
    m_root_inode = TRY(Plan9FSInode::try_create(*this, root_fid));
    return {};
}

Plan9FS::ProtocolVersion Plan9FS::parse_protocol_version(StringView s) const
{
    if (s == "9P2000.L")
        return ProtocolVersion::v9P2000L;
    if (s == "9P2000.u")
        return ProtocolVersion::v9P2000u;
    return ProtocolVersion::v9P2000;
}

Inode& Plan9FS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<void> Plan9FS::rename(Inode&, StringView, Inode&, StringView)
{
    // TODO
    return ENOTIMPL;
}

Plan9FS::ReceiveCompletion::ReceiveCompletion(u16 tag)
    : tag(tag)
{
}

Plan9FS::ReceiveCompletion::~ReceiveCompletion() = default;

bool Plan9FS::Blocker::unblock(u16 tag)
{
    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return false;
        m_did_unblock = true;

        if (m_completion->tag != tag)
            return false;
        if (!m_completion->result.is_error())
            m_message = move(*m_completion->message);
    }
    return unblock();
}

bool Plan9FS::Blocker::setup_blocker()
{
    return add_to_blocker_set(m_fs.m_completion_blocker);
}

void Plan9FS::Blocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason)
{
    {
        SpinlockLocker lock(m_lock);
        if (m_did_unblock)
            return;
    }

    m_fs.m_completion_blocker.try_unblock(*this);
}

bool Plan9FS::Blocker::is_completed() const
{
    SpinlockLocker lock(m_completion->lock);
    return m_completion->completed;
}

bool Plan9FS::Plan9FSBlockerSet::should_add_blocker(Thread::Blocker& b, void*)
{
    // NOTE: m_lock is held already!
    auto& blocker = static_cast<Blocker&>(b);
    return !blocker.is_completed();
}

void Plan9FS::Plan9FSBlockerSet::unblock_completed(u16 tag)
{
    unblock_all_blockers_whose_conditions_are_met([&](Thread::Blocker& b, void*, bool&) {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Plan9FS);
        auto& blocker = static_cast<Blocker&>(b);
        return blocker.unblock(tag);
    });
}

void Plan9FS::Plan9FSBlockerSet::unblock_all()
{
    unblock_all_blockers_whose_conditions_are_met([&](Thread::Blocker& b, void*, bool&) {
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Plan9FS);
        auto& blocker = static_cast<Blocker&>(b);
        return blocker.unblock();
    });
}

void Plan9FS::Plan9FSBlockerSet::try_unblock(Plan9FS::Blocker& blocker)
{
    if (m_fs.is_complete(*blocker.completion())) {
        SpinlockLocker lock(m_lock);
        blocker.unblock(blocker.completion()->tag);
    }
}

bool Plan9FS::is_complete(ReceiveCompletion const& completion)
{
    MutexLocker locker(m_lock);
    if (m_completions.contains(completion.tag)) {
        // If it's still in the map then it can't be complete
        VERIFY(!completion.completed);
        return false;
    }

    // if it's not in the map anymore, it must be complete. But we MUST
    // hold m_lock to be able to check completion.completed!
    VERIFY(completion.completed);
    return true;
}

ErrorOr<void> Plan9FS::post_message(Plan9FSMessage& message, LockRefPtr<ReceiveCompletion> completion)
{
    auto const& buffer = message.build();
    u8 const* data = buffer.data();
    size_t size = buffer.size();
    auto& description = file_description();

    MutexLocker locker(m_send_lock);

    if (completion) {
        // Save the completion record *before* we send the message. This
        // ensures that it exists when the thread reads the response
        MutexLocker locker(m_lock);
        auto tag = completion->tag;
        m_completions.set(tag, completion.release_nonnull());
        // TODO: What if there is a collision? Do we need to wait until
        // the existing record with the tag completes before queueing
        // this one?
    }

    while (size > 0) {
        if (!description.can_write()) {
            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::WriteBlocker>({}, description, unblock_flags).was_interrupted())
                return EINTR;
        }
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>(data));
        auto nwritten = TRY(description.write(data_buffer, size));
        data += nwritten;
        size -= nwritten;
    }

    return {};
}

ErrorOr<void> Plan9FS::do_read(u8* data, size_t size)
{
    auto& description = file_description();
    while (size > 0) {
        if (!description.can_read()) {
            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::ReadBlocker>({}, description, unblock_flags).was_interrupted())
                return EINTR;
        }
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(data);
        auto nread = TRY(description.read(data_buffer, size));
        if (nread == 0)
            return EIO;
        data += nread;
        size -= nread;
    }
    return {};
}

ErrorOr<void> Plan9FS::read_and_dispatch_one_message()
{
    struct [[gnu::packed]] Header {
        u32 size;
        u8 type;
        u16 tag;
    };
    Header header;
    TRY(do_read(reinterpret_cast<u8*>(&header), sizeof(header)));

    auto buffer = TRY(KBuffer::try_create_with_size("Plan9FS: Plan9FSMessage read buffer"sv, header.size, Memory::Region::Access::ReadWrite));
    // Copy the already read header into the buffer.
    memcpy(buffer->data(), &header, sizeof(header));
    TRY(do_read(buffer->data() + sizeof(header), header.size - sizeof(header)));

    MutexLocker locker(m_lock);

    auto optional_completion = m_completions.get(header.tag);
    if (optional_completion.has_value()) {
        auto* completion = optional_completion.value();
        SpinlockLocker lock(completion->lock);
        completion->result = {};
        completion->message = adopt_own_if_nonnull(new (nothrow) Plan9FSMessage { move(buffer) });
        completion->completed = true;

        m_completions.remove(header.tag);
        m_completion_blocker.unblock_completed(header.tag);
    } else {
        dbgln("Received a 9p message of type {} with an unexpected tag {}, dropping", header.type, header.tag);
    }

    return {};
}

ErrorOr<void> Plan9FS::post_message_and_explicitly_ignore_reply(Plan9FSMessage& message)
{
    return post_message(message, {});
}

ErrorOr<void> Plan9FS::post_message_and_wait_for_a_reply(Plan9FSMessage& message)
{
    auto request_type = message.type();
    auto tag = message.tag();
    auto completion = adopt_lock_ref(*new ReceiveCompletion(tag));
    TRY(post_message(message, completion));
    if (Thread::current()->block<Plan9FS::Blocker>({}, *this, message, completion).was_interrupted())
        return EINTR;

    if (completion->result.is_error()) {
        dbgln("Plan9FS: Plan9FSMessage was aborted with error {}", completion->result.error());
        return EIO;
    }

    auto reply_type = message.type();

    if (reply_type == Plan9FSMessage::Type::Rlerror) {
        // Contains a numerical Linux errno; hopefully our errno numbers match.
        u32 error_code;
        message >> error_code;
        return Error::from_errno((ErrnoCode)error_code);
    }
    if (reply_type == Plan9FSMessage::Type::Rerror) {
        // Contains an error message. We could attempt to parse it, but for now
        // we simply return EIO instead. In 9P200.u, it can also contain a
        // numerical errno in an unspecified encoding; we ignore those too.
        StringView error_name;
        message >> error_name;
        dbgln("Plan9FS: Received error name {}", error_name);
        return EIO;
    }
    if ((u8)reply_type != (u8)request_type + 1) {
        // Other than those error messages. we only expect the matching reply
        // message type.
        dbgln("Plan9FS: Received unexpected message type {} in response to {}", (u8)reply_type, (u8)request_type);
        return EIO;
    }

    return {};
}

size_t Plan9FS::adjust_buffer_size(size_t size) const
{
    size_t max_size = m_max_message_size - Plan9FSMessage::max_header_size;
    return min(size, max_size);
}

void Plan9FS::thread_main()
{
    dbgln("Plan9FS: Thread running");
    while (!Process::current().is_dying()) {
        auto result = read_and_dispatch_one_message();
        if (result.is_error()) {
            // If we fail to read, wake up everyone with an error.
            MutexLocker locker(m_lock);

            for (auto& it : m_completions) {
                it.value->result = Error::copy(result.error());
                it.value->completed = true;
            }
            m_completions.clear();
            m_completion_blocker.unblock_all();
            dbgln("Plan9FS: Thread terminating, error reading");
            return;
        }
    }
    dbgln("Plan9FS: Thread terminating");
}

void Plan9FS::ensure_thread()
{
    SpinlockLocker lock(m_thread_lock);
    if (!m_thread_running.exchange(true, AK::MemoryOrder::memory_order_acq_rel)) {
        auto [_, thread] = Process::create_kernel_process("Plan9FS"sv, [&]() {
            thread_main();
            m_thread_running.store(false, AK::MemoryOrder::memory_order_release);
            Process::current().sys$exit(0);
            VERIFY_NOT_REACHED();
        }).release_value_but_fixme_should_propagate_errors();
        m_thread = move(thread);
    }
}

}
