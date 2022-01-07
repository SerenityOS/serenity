/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Plan9FS>> Plan9FS::try_create(OpenFileDescription& file_description)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Plan9FS(file_description));
}

Plan9FS::Plan9FS(OpenFileDescription& file_description)
    : FileBackedFileSystem(file_description)
    , m_completion_blocker(*this)
{
}

Plan9FS::~Plan9FS()
{
    // Make sure to destroy the root inode before the FS gets destroyed.
    if (m_root_inode) {
        VERIFY(m_root_inode->ref_count() == 1);
        m_root_inode = nullptr;
    }
}

class Plan9FS::Message {
public:
    enum class Type : u8 {
        // 9P2000.L
        Tlerror = 6,
        Rlerror = 7,
        Tstatfs = 8,
        Rstatfs = 9,

        Tlopen = 12,
        Rlopen = 13,
        Tlcreate = 14,
        Rlcreate = 15,
        Tsymlink = 16,
        Rsymlink = 17,
        Tmknod = 18,
        Rmknod = 19,
        Trename = 20,
        Rrename = 21,
        Treadlink = 22,
        Rreadlink = 23,
        Tgetattr = 24,
        Rgetattr = 25,
        Tsetattr = 26,
        Rsetattr = 27,

        Txattrwalk = 30,
        Rxattrwalk = 31,
        Txattrcreate = 32,
        Rxattrcreate = 33,

        Treaddir = 40,
        Rreaddir = 41,

        Tfsync = 50,
        Rfsync = 51,
        Tlock = 52,
        Rlock = 53,
        Tgetlock = 54,
        Rgetlock = 55,

        Tlink = 70,
        Rlink = 71,
        Tmkdir = 72,
        Rmkdir = 73,
        Trenameat = 74,
        Rrenameat = 75,
        Tunlinkat = 76,
        Runlinkat = 77,

        // 9P2000
        Tversion = 100,
        Rversion = 101,
        Tauth = 102,
        Rauth = 103,
        Tattach = 104,
        Rattach = 105,
        Terror = 106,
        Rerror = 107,
        Tflush = 108,
        Rflush = 109,
        Twalk = 110,
        Rwalk = 111,
        Topen = 112,
        Ropen = 113,
        Tcreate = 114,
        Rcreate = 115,
        Tread = 116,
        Rread = 117,
        Twrite = 118,
        Rwrite = 119,
        Tclunk = 120,
        Rclunk = 121,
        Tremove = 122,
        Rremove = 123,
        Tstat = 124,
        Rstat = 125,
        Twstat = 126,
        Rwstat = 127
    };

    class Decoder {
    public:
        explicit Decoder(StringView data)
            : m_data(data)
        {
        }

        Decoder& operator>>(u8&);
        Decoder& operator>>(u16&);
        Decoder& operator>>(u32&);
        Decoder& operator>>(u64&);
        Decoder& operator>>(StringView&);
        Decoder& operator>>(qid&);
        StringView read_data();

        bool has_more_data() const { return !m_data.is_empty(); }

    private:
        StringView m_data;

        template<typename N>
        Decoder& read_number(N& number)
        {
            VERIFY(sizeof(number) <= m_data.length());
            memcpy(&number, m_data.characters_without_null_termination(), sizeof(number));
            m_data = m_data.substring_view(sizeof(number), m_data.length() - sizeof(number));
            return *this;
        }
    };

    Message& operator<<(u8);
    Message& operator<<(u16);
    Message& operator<<(u32);
    Message& operator<<(u64);
    Message& operator<<(StringView);
    void append_data(StringView);

    template<typename T>
    Message& operator>>(T& t)
    {
        VERIFY(m_have_been_built);
        m_built.decoder >> t;
        return *this;
    }

    StringView read_data()
    {
        VERIFY(m_have_been_built);
        return m_built.decoder.read_data();
    }

    Type type() const { return m_type; }
    u16 tag() const { return m_tag; }

    Message(Plan9FS&, Type);
    Message(NonnullOwnPtr<KBuffer>&&);
    ~Message();
    Message& operator=(Message&&);

    const KBuffer& build();

    static constexpr size_t max_header_size = 24;

private:
    template<typename N>
    Message& append_number(N number)
    {
        VERIFY(!m_have_been_built);
        // FIXME: Handle append failure.
        (void)m_builder.append(reinterpret_cast<const char*>(&number), sizeof(number));
        return *this;
    }

    union {
        KBufferBuilder m_builder;
        struct {
            NonnullOwnPtr<KBuffer> buffer;
            Decoder decoder;
        } m_built;
    };

    u16 m_tag { 0 };
    Type m_type { 0 };
    bool m_have_been_built { false };
};

ErrorOr<void> Plan9FS::initialize()
{
    ensure_thread();

    Message version_message { *this, Message::Type::Tversion };
    version_message << (u32)m_max_message_size << "9P2000.L";

    TRY(post_message_and_wait_for_a_reply(version_message));

    u32 msize;
    StringView remote_protocol_version;
    version_message >> msize >> remote_protocol_version;
    dbgln("Remote supports msize={} and protocol version {}", msize, remote_protocol_version);
    m_remote_protocol_version = parse_protocol_version(remote_protocol_version);
    m_max_message_size = min(m_max_message_size, (size_t)msize);

    // TODO: auth

    u32 root_fid = allocate_fid();
    Message attach_message { *this, Message::Type::Tattach };
    // FIXME: This needs a user name and an "export" name; but how do we get them?
    // Perhaps initialize() should accept a string of FS-specific options...
    attach_message << root_fid << (u32)-1 << "sergey"
                   << "/";
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

Plan9FS::Message& Plan9FS::Message::operator<<(u8 number)
{
    return append_number(number);
}

Plan9FS::Message& Plan9FS::Message::operator<<(u16 number)
{
    return append_number(number);
}

Plan9FS::Message& Plan9FS::Message::operator<<(u32 number)
{
    return append_number(number);
}

Plan9FS::Message& Plan9FS::Message::operator<<(u64 number)
{
    return append_number(number);
}

Plan9FS::Message& Plan9FS::Message::operator<<(StringView string)
{
    *this << static_cast<u16>(string.length());
    // FIXME: Handle append failure.
    (void)m_builder.append(string);
    return *this;
}

void Plan9FS::Message::append_data(StringView data)
{
    *this << static_cast<u32>(data.length());
    // FIXME: Handle append failure.
    (void)m_builder.append(data);
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(u8& number)
{
    return read_number(number);
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(u16& number)
{
    return read_number(number);
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(u32& number)
{
    return read_number(number);
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(u64& number)
{
    return read_number(number);
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(qid& qid)
{
    return *this >> qid.type >> qid.version >> qid.path;
}

Plan9FS::Message::Decoder& Plan9FS::Message::Decoder::operator>>(StringView& string)
{
    u16 length;
    *this >> length;
    VERIFY(length <= m_data.length());
    string = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(string);
    return *this;
}

StringView Plan9FS::Message::Decoder::read_data()
{
    u32 length;
    *this >> length;
    VERIFY(length <= m_data.length());
    auto data = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(data);
    return data;
}

Plan9FS::Message::Message(Plan9FS& fs, Type type)
    : m_builder(KBufferBuilder::try_create().release_value()) // FIXME: Don't assume KBufferBuilder allocation success.
    , m_tag(fs.allocate_tag())
    , m_type(type)
    , m_have_been_built(false)
{
    u32 size_placeholder = 0;
    *this << size_placeholder << (u8)type << m_tag;
}

Plan9FS::Message::Message(NonnullOwnPtr<KBuffer>&& buffer)
    : m_built { move(buffer), Decoder({ buffer->bytes() }) }
    , m_have_been_built(true)
{
    u32 size;
    u8 raw_type;
    *this >> size >> raw_type >> m_tag;
    m_type = (Type)raw_type;
}

Plan9FS::Message::~Message()
{
    if (m_have_been_built) {
        m_built.buffer.~NonnullOwnPtr<KBuffer>();
        m_built.decoder.~Decoder();
    } else {
        m_builder.~KBufferBuilder();
    }
}

Plan9FS::Message& Plan9FS::Message::operator=(Message&& message)
{
    m_tag = message.m_tag;
    m_type = message.m_type;

    if (m_have_been_built) {
        m_built.buffer.~NonnullOwnPtr<KBuffer>();
        m_built.decoder.~Decoder();
    } else {
        m_builder.~KBufferBuilder();
    }

    m_have_been_built = message.m_have_been_built;
    if (m_have_been_built) {
        new (&m_built.buffer) NonnullOwnPtr<KBuffer>(move(message.m_built.buffer));
        new (&m_built.decoder) Decoder(move(message.m_built.decoder));
    } else {
        new (&m_builder) KBufferBuilder(move(message.m_builder));
    }

    return *this;
}

const KBuffer& Plan9FS::Message::build()
{
    VERIFY(!m_have_been_built);

    auto tmp_buffer = m_builder.build();

    // FIXME: We should not assume success here.
    VERIFY(tmp_buffer);

    m_have_been_built = true;
    m_builder.~KBufferBuilder();

    new (&m_built.buffer) NonnullOwnPtr<KBuffer>(tmp_buffer.release_nonnull());
    new (&m_built.decoder) Decoder({ m_built.buffer->data(), m_built.buffer->size() });
    u32* size = reinterpret_cast<u32*>(m_built.buffer->data());
    *size = m_built.buffer->size();
    return *m_built.buffer;
}

Plan9FS::ReceiveCompletion::ReceiveCompletion(u16 tag)
    : tag(tag)
{
}

Plan9FS::ReceiveCompletion::~ReceiveCompletion()
{
}

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

bool Plan9FS::is_complete(const ReceiveCompletion& completion)
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

ErrorOr<void> Plan9FS::post_message(Message& message, RefPtr<ReceiveCompletion> completion)
{
    auto const& buffer = message.build();
    const u8* data = buffer.data();
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

    auto buffer = TRY(KBuffer::try_create_with_size(header.size, Memory::Region::Access::ReadWrite));
    // Copy the already read header into the buffer.
    memcpy(buffer->data(), &header, sizeof(header));
    TRY(do_read(buffer->data() + sizeof(header), header.size - sizeof(header)));

    MutexLocker locker(m_lock);

    auto optional_completion = m_completions.get(header.tag);
    if (optional_completion.has_value()) {
        auto* completion = optional_completion.value();
        SpinlockLocker lock(completion->lock);
        completion->result = {};
        completion->message = adopt_own_if_nonnull(new (nothrow) Message { move(buffer) });
        completion->completed = true;

        m_completions.remove(header.tag);
        m_completion_blocker.unblock_completed(header.tag);
    } else {
        dbgln("Received a 9p message of type {} with an unexpected tag {}, dropping", header.type, header.tag);
    }

    return {};
}

ErrorOr<void> Plan9FS::post_message_and_explicitly_ignore_reply(Message& message)
{
    return post_message(message, {});
}

ErrorOr<void> Plan9FS::post_message_and_wait_for_a_reply(Message& message)
{
    auto request_type = message.type();
    auto tag = message.tag();
    auto completion = adopt_ref(*new ReceiveCompletion(tag));
    TRY(post_message(message, completion));
    if (Thread::current()->block<Plan9FS::Blocker>({}, *this, message, completion).was_interrupted())
        return EINTR;

    if (completion->result.is_error()) {
        dbgln("Plan9FS: Message was aborted with error {}", completion->result.error());
        return EIO;
    }

    auto reply_type = message.type();

    if (reply_type == Message::Type::Rlerror) {
        // Contains a numerical Linux errno; hopefully our errno numbers match.
        u32 error_code;
        message >> error_code;
        return Error::from_errno((ErrnoCode)error_code);
    }
    if (reply_type == Message::Type::Rerror) {
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
    size_t max_size = m_max_message_size - Message::max_header_size;
    return min(size, max_size);
}

void Plan9FS::thread_main()
{
    dbgln("Plan9FS: Thread running");
    do {
        auto result = read_and_dispatch_one_message();
        if (result.is_error()) {
            // If we fail to read, wake up everyone with an error.
            MutexLocker locker(m_lock);

            for (auto& it : m_completions) {
                it.value->result = result;
                it.value->completed = true;
            }
            m_completions.clear();
            m_completion_blocker.unblock_all();
            dbgln("Plan9FS: Thread terminating, error reading");
            return;
        }
    } while (!m_thread_shutdown);
    dbgln("Plan9FS: Thread terminating");
}

void Plan9FS::ensure_thread()
{
    SpinlockLocker lock(m_thread_lock);
    if (!m_thread_running.exchange(true, AK::MemoryOrder::memory_order_acq_rel)) {
        auto process_name = KString::try_create("Plan9FS");
        if (process_name.is_error())
            TODO();
        (void)Process::create_kernel_process(m_thread, process_name.release_value(), [&]() {
            thread_main();
            m_thread_running.store(false, AK::MemoryOrder::memory_order_release);
        });
    }
}

Plan9FSInode::Plan9FSInode(Plan9FS& fs, u32 fid)
    : Inode(fs, fid)
{
}

ErrorOr<NonnullRefPtr<Plan9FSInode>> Plan9FSInode::try_create(Plan9FS& fs, u32 fid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Plan9FSInode(fs, fid));
}

Plan9FSInode::~Plan9FSInode()
{
    Plan9FS::Message clunk_request { fs(), Plan9FS::Message::Type::Tclunk };
    clunk_request << fid();
    // FIXME: Should we observe this  error somehow?
    [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(clunk_request);
}

ErrorOr<void> Plan9FSInode::ensure_open_for_mode(int mode)
{
    bool use_lopen = fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L;
    u32 l_mode = 0;
    u8 p9_mode = 0;

    {
        MutexLocker locker(m_inode_lock);

        // If it's already open in this mode, we're done.
        if ((m_open_mode & mode) == mode)
            return {};

        m_open_mode |= mode;

        if ((m_open_mode & O_RDWR) == O_RDWR) {
            l_mode |= 2;
            p9_mode |= 2;
        } else if (m_open_mode & O_WRONLY) {
            l_mode |= 1;
            p9_mode |= 1;
        } else if (m_open_mode & O_RDONLY) {
            // Leave the values at 0.
        }
    }

    if (use_lopen) {
        Plan9FS::Message message { fs(), Plan9FS::Message::Type::Tlopen };
        message << fid() << l_mode;
        return fs().post_message_and_wait_for_a_reply(message);
    }

    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Topen };
    message << fid() << p9_mode;
    return fs().post_message_and_wait_for_a_reply(message);
}

ErrorOr<size_t> Plan9FSInode::read_bytes(off_t offset, size_t size, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    TRY(const_cast<Plan9FSInode&>(*this).ensure_open_for_mode(O_RDONLY));

    size = fs().adjust_buffer_size(size);

    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Treadlink };
    StringView data;

    // Try readlink first.
    bool readlink_succeeded = false;
    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L && offset == 0) {
        message << fid();
        if (auto result = fs().post_message_and_wait_for_a_reply(message); !result.is_error()) {
            readlink_succeeded = true;
            message >> data;
        }
    }

    if (!readlink_succeeded) {
        message = Plan9FS::Message { fs(), Plan9FS::Message::Type::Tread };
        message << fid() << (u64)offset << (u32)size;
        TRY(fs().post_message_and_wait_for_a_reply(message));
        data = message.read_data();
    }

    // Guard against the server returning more data than requested.
    size_t nread = min(data.length(), size);
    TRY(buffer.write(data.characters_without_null_termination(), nread));
    return nread;
}

ErrorOr<size_t> Plan9FSInode::write_bytes(off_t offset, size_t size, const UserOrKernelBuffer& data, OpenFileDescription*)
{
    TRY(ensure_open_for_mode(O_WRONLY));
    size = fs().adjust_buffer_size(size);

    auto data_copy = TRY(data.try_copy_into_kstring(size)); // FIXME: this seems ugly

    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Twrite };
    message << fid() << (u64)offset;
    message.append_data(data_copy->view());
    TRY(fs().post_message_and_wait_for_a_reply(message));

    u32 nwritten;
    message >> nwritten;
    return nwritten;
}

InodeMetadata Plan9FSInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = identifier();

    // 9P2000.L; TODO: 9P2000 & 9P2000.u
    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Tgetattr };
    message << fid() << (u64)GetAttrMask::Basic;
    auto result = fs().post_message_and_wait_for_a_reply(message);
    if (result.is_error()) {
        // Just return blank metadata; hopefully that's enough to result in an
        // error at some upper layer. Ideally, there would be a way for
        // Inode::metadata() to return failure.
        return metadata;
    }

    u64 valid;
    Plan9FS::qid qid;
    u32 mode;
    u32 uid;
    u32 gid;
    u64 nlink;
    u64 rdev;
    u64 size;
    u64 blksize;
    u64 blocks;
    message >> valid >> qid >> mode >> uid >> gid >> nlink >> rdev >> size >> blksize >> blocks;
    // TODO: times...

    if (valid & (u64)GetAttrMask::Mode)
        metadata.mode = mode;
    if (valid & (u64)GetAttrMask::NLink)
        metadata.link_count = nlink;

#if 0
    // FIXME: Map UID/GID somehow? Or what do we do?
    if (valid & (u64)GetAttrMask::UID)
        metadata.uid = uid;
    if (valid & (u64)GetAttrMask::GID)
        metadata.uid = gid;
    // FIXME: What about device nodes?
    if (valid & (u64)GetAttrMask::RDev)
        metadata.encoded_device = 0; // TODO
#endif

    if (valid & (u64)GetAttrMask::Size)
        metadata.size = size;
    if (valid & (u64)GetAttrMask::Blocks) {
        metadata.block_size = blksize;
        metadata.block_count = blocks;
    }

    return metadata;
}

ErrorOr<void> Plan9FSInode::flush_metadata()
{
    // Do nothing.
    return {};
}

ErrorOr<void> Plan9FSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    // TODO: Should we synthesize "." and ".." here?

    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L) {
        // Start by cloning the fid and opening it.
        auto clone_fid = fs().allocate_fid();
        {
            Plan9FS::Message clone_message { fs(), Plan9FS::Message::Type::Twalk };
            clone_message << fid() << clone_fid << (u16)0;
            TRY(fs().post_message_and_wait_for_a_reply(clone_message));
            Plan9FS::Message open_message { fs(), Plan9FS::Message::Type::Tlopen };
            open_message << clone_fid << (u32)0;
            auto result = fs().post_message_and_wait_for_a_reply(open_message);
            if (result.is_error()) {
                Plan9FS::Message close_message { fs(), Plan9FS::Message::Type::Tclunk };
                close_message << clone_fid;
                // FIXME: Should we observe this error?
                [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(close_message);
                return result;
            }
        }

        u64 offset = 0;
        u32 count = fs().adjust_buffer_size(8 * MiB);
        ErrorOr<void> result;

        while (true) {
            Plan9FS::Message message { fs(), Plan9FS::Message::Type::Treaddir };
            message << clone_fid << offset << count;
            result = fs().post_message_and_wait_for_a_reply(message);
            if (result.is_error())
                break;

            StringView data = message.read_data();
            if (data.is_empty()) {
                // We've reached the end.
                break;
            }

            for (Plan9FS::Message::Decoder decoder { data }; decoder.has_more_data();) {
                Plan9FS::qid qid;
                u8 type;
                StringView name;
                decoder >> qid >> offset >> type >> name;
                result = callback({ name, { fsid(), fs().allocate_fid() }, 0 });
                if (result.is_error())
                    break;
            }

            if (result.is_error())
                break;
        }

        Plan9FS::Message close_message { fs(), Plan9FS::Message::Type::Tclunk };
        close_message << clone_fid;
        // FIXME: Should we observe this error?
        [[maybe_unused]] auto rc = fs().post_message_and_explicitly_ignore_reply(close_message);
        return result;
    }

    // TODO
    return ENOTIMPL;
}

ErrorOr<NonnullRefPtr<Inode>> Plan9FSInode::lookup(StringView name)
{
    u32 newfid = fs().allocate_fid();
    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Twalk };
    message << fid() << newfid << (u16)1 << name;
    TRY(fs().post_message_and_wait_for_a_reply(message));
    return TRY(Plan9FSInode::try_create(fs(), newfid));
}

ErrorOr<NonnullRefPtr<Inode>> Plan9FSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::add_child(Inode&, StringView, mode_t)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::remove_child(StringView)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::chmod(mode_t)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::chown(UserID, GroupID)
{
    // TODO
    return ENOTIMPL;
}

ErrorOr<void> Plan9FSInode::truncate(u64 new_size)
{
    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L) {
        Plan9FS::Message message { fs(), Plan9FS::Message::Type::Tsetattr };
        SetAttrMask valid = SetAttrMask::Size;
        u32 mode = 0;
        u32 uid = 0;
        u32 gid = 0;
        u64 atime_sec = 0;
        u64 atime_nsec = 0;
        u64 mtime_sec = 0;
        u64 mtime_nsec = 0;
        message << fid() << (u64)valid << mode << uid << gid << new_size << atime_sec << atime_nsec << mtime_sec << mtime_nsec;
        return fs().post_message_and_wait_for_a_reply(message);
    }

    // TODO: wstat version
    return {};
}

}
