/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

NonnullRefPtr<Plan9FS> Plan9FS::create(FileDescription& file_description)
{
    return adopt(*new Plan9FS(file_description));
}

Plan9FS::Plan9FS(FileDescription& file_description)
    : FileBackedFS(file_description)
{
}

Plan9FS::~Plan9FS()
{
    // Make sure to destroy the root inode before the FS gets destroyed.
    if (m_root_inode) {
        ASSERT(m_root_inode->ref_count() == 1);
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
        explicit Decoder(const StringView& data)
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
            ASSERT(sizeof(number) <= m_data.length());
            memcpy(&number, m_data.characters_without_null_termination(), sizeof(number));
            m_data = m_data.substring_view(sizeof(number), m_data.length() - sizeof(number));
            return *this;
        }
    };

    Message& operator<<(u8);
    Message& operator<<(u16);
    Message& operator<<(u32);
    Message& operator<<(u64);
    Message& operator<<(const StringView&);
    void append_data(const StringView&);

    template<typename T>
    Message& operator>>(T& t)
    {
        ASSERT(m_have_been_built);
        m_built.decoder >> t;
        return *this;
    }

    StringView read_data()
    {
        ASSERT(m_have_been_built);
        return m_built.decoder.read_data();
    }

    Type type() const { return m_type; }
    u16 tag() const { return m_tag; }

    Message(Plan9FS&, Type);
    Message(KBuffer&&);
    ~Message();
    Message& operator=(Message&&);

    const KBuffer& build();

    static constexpr ssize_t max_header_size = 24;

private:
    template<typename N>
    Message& append_number(N number)
    {
        ASSERT(!m_have_been_built);
        m_builder.append(reinterpret_cast<const char*>(&number), sizeof(number));
        return *this;
    }

    union {
        KBufferBuilder m_builder;
        struct {
            KBuffer buffer;
            Decoder decoder;
        } m_built;
    };

    u16 m_tag { 0 };
    Type m_type { 0 };
    bool m_have_been_built { false };
};

bool Plan9FS::initialize()
{
    Message version_message { *this, Message::Type::Tversion };
    version_message << (u32)m_max_message_size << "9P2000.L";

    auto result = post_message_and_wait_for_a_reply(version_message);
    if (result.is_error())
        return false;

    u32 msize;
    StringView remote_protocol_version;
    version_message >> msize >> remote_protocol_version;
    dbg() << "Remote supports msize=" << msize << " and protocol version " << remote_protocol_version;
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

    result = post_message_and_wait_for_a_reply(attach_message);
    if (result.is_error()) {
        dbg() << "Attaching failed";
        return false;
    }

    m_root_inode = Plan9FSInode::create(*this, root_fid);
    return true;
}

Plan9FS::ProtocolVersion Plan9FS::parse_protocol_version(const StringView& s) const
{
    if (s == "9P2000.L")
        return ProtocolVersion::v9P2000L;
    if (s == "9P2000.u")
        return ProtocolVersion::v9P2000u;
    return ProtocolVersion::v9P2000;
}

NonnullRefPtr<Inode> Plan9FS::root_inode() const
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

Plan9FS::Message& Plan9FS::Message::operator<<(const StringView& string)
{
    *this << static_cast<u16>(string.length());
    m_builder.append(string);
    return *this;
}

void Plan9FS::Message::append_data(const StringView& data)
{
    *this << static_cast<u32>(data.length());
    m_builder.append(data);
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
    ASSERT(length <= m_data.length());
    string = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(string);
    return *this;
}

StringView Plan9FS::Message::Decoder::read_data()
{
    u32 length;
    *this >> length;
    ASSERT(length <= m_data.length());
    auto data = m_data.substring_view(0, length);
    m_data = m_data.substring_view_starting_after_substring(data);
    return data;
}

Plan9FS::Message::Message(Plan9FS& fs, Type type)
    : m_builder()
    , m_tag(fs.allocate_tag())
    , m_type(type)
    , m_have_been_built(false)
{
    u32 size_placeholder = 0;
    *this << size_placeholder << (u8)type << m_tag;
}

Plan9FS::Message::Message(KBuffer&& buffer)
    : m_built { buffer, Decoder({ buffer.data(), buffer.size() }) }
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
        m_built.buffer.~KBuffer();
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
        m_built.buffer.~KBuffer();
        m_built.decoder.~Decoder();
    } else {
        m_builder.~KBufferBuilder();
    }

    m_have_been_built = message.m_have_been_built;
    if (m_have_been_built) {
        new (&m_built.buffer) KBuffer(move(message.m_built.buffer));
        new (&m_built.decoder) Decoder(move(message.m_built.decoder));
    } else {
        new (&m_builder) KBufferBuilder(move(message.m_builder));
    }

    return *this;
}

const KBuffer& Plan9FS::Message::build()
{
    ASSERT(!m_have_been_built);

    auto tmp_buffer = m_builder.build();

    m_have_been_built = true;
    m_builder.~KBufferBuilder();

    new (&m_built.buffer) KBuffer(move(tmp_buffer));
    new (&m_built.decoder) Decoder({ m_built.buffer.data(), m_built.buffer.size() });
    u32* size = reinterpret_cast<u32*>(m_built.buffer.data());
    *size = m_built.buffer.size();
    return m_built.buffer;
}

KResult Plan9FS::post_message(Message& message)
{
    auto& buffer = message.build();
    const u8* data = buffer.data();
    size_t size = buffer.size();
    auto& description = file_description();

    LOCKER(m_send_lock);

    while (size > 0) {
        if (!description.can_write()) {
            if (Thread::current()->block<Thread::WriteBlocker>(nullptr, description).was_interrupted())
                return KResult(-EINTR);
        }
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>(data));
        auto nwritten_or_error = description.write(data_buffer, size);
        if (nwritten_or_error.is_error())
            return nwritten_or_error.error();
        auto nwritten = nwritten_or_error.value();
        data += nwritten;
        size -= nwritten;
    }

    return KSuccess;
}

KResult Plan9FS::do_read(u8* data, size_t size)
{
    auto& description = file_description();
    while (size > 0) {
        if (!description.can_read()) {
            if (Thread::current()->block<Thread::ReadBlocker>(nullptr, description).was_interrupted())
                return KResult(-EINTR);
        }
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(data);
        auto nread_or_error = description.read(data_buffer, size);
        if (nread_or_error.is_error())
            return nread_or_error.error();
        auto nread = nread_or_error.value();
        if (nread == 0)
            return KResult(-EIO);
        data += nread;
        size -= nread;
    }
    return KSuccess;
}

KResult Plan9FS::read_and_dispatch_one_message()
{
    ASSERT(m_someone_is_reading);
    // That someone is us.

    struct [[gnu::packed]] Header
    {
        u32 size;
        u8 type;
        u16 tag;
    };
    Header header;
    KResult result = do_read(reinterpret_cast<u8*>(&header), sizeof(header));
    if (result.is_error())
        return result;

    auto buffer = KBuffer::create_with_size(header.size, Region::Access::Read | Region::Access::Write);
    // Copy the already read header into the buffer.
    memcpy(buffer.data(), &header, sizeof(header));
    result = do_read(buffer.data() + sizeof(header), header.size - sizeof(header));
    if (result.is_error())
        return result;

    LOCKER(m_lock);

    auto optional_completion = m_completions.get(header.tag);
    if (!optional_completion.has_value()) {
        if (m_tags_to_ignore.contains(header.tag)) {
            m_tags_to_ignore.remove(header.tag);
        } else {
            dbg() << "Received a 9p message of type " << header.type << " with an unexpected tag " << header.tag << ", dropping";
        }
        return KSuccess;
    }
    ReceiveCompletion& completion = *optional_completion.value();
    completion.result = KSuccess;
    completion.message = Message { move(buffer) };
    completion.completed = true;
    m_completions.remove(header.tag);

    return KSuccess;
}

bool Plan9FS::Blocker::should_unblock(Thread&)
{
    if (m_completion.completed)
        return true;

    bool someone_else_is_reading = m_completion.fs.m_someone_is_reading.exchange(true);
    if (!someone_else_is_reading) {
        // We're gonna start reading ourselves; unblock.
        return true;
    }
    return false;
}

KResult Plan9FS::wait_for_specific_message(u16 tag, Message& out_message)
{
    KResult result = KSuccess;
    ReceiveCompletion completion { *this, out_message, result, false };

    {
        LOCKER(m_lock);
        m_completions.set(tag, &completion);
    }

    // Block until either:
    // * Someone else reads the message we're waiting for, and hands it to us;
    // * Or we become the one to read and dispatch messages.
    if (Thread::current()->block<Plan9FS::Blocker>(nullptr, completion).was_interrupted()) {
        LOCKER(m_lock);
        m_completions.remove(tag);
        return KResult(-EINTR);
    }

    // See for which reason we woke up.
    if (completion.completed) {
        // Somebody else completed it for us; nothing further to do.
        return result;
    }

    while (!completion.completed && result.is_success()) {
        result = read_and_dispatch_one_message();
    }

    if (result.is_error()) {
        // If we fail to read, wake up everyone with an error.
        LOCKER(m_lock);

        for (auto& it : m_completions) {
            it.value->result = result;
            it.value->completed = true;
        }
        m_completions.clear();
    }

    // Wake up someone else, if anyone is interested...
    m_someone_is_reading = false;
    // ...and return.
    return result;
}

KResult Plan9FS::post_message_and_explicitly_ignore_reply(Message& message)
{
    auto tag = message.tag();
    {
        LOCKER(m_lock);
        m_tags_to_ignore.set(tag);
    }

    auto result = post_message(message);
    if (result.is_error()) {
        LOCKER(m_lock);
        m_tags_to_ignore.remove(tag);
    }

    return result;
}

KResult Plan9FS::post_message_and_wait_for_a_reply(Message& message, bool auto_convert_error_reply_to_error)
{
    auto request_type = message.type();
    auto tag = message.tag();
    auto result = post_message(message);
    if (result.is_error())
        return result;
    result = wait_for_specific_message(tag, message);
    if (result.is_error())
        return result;

    if (!auto_convert_error_reply_to_error)
        return KSuccess;

    auto reply_type = message.type();

    if (reply_type == Message::Type::Rlerror) {
        // Contains a numerical Linux errno; hopefully our errno numbers match.
        u32 error_code;
        message >> error_code;
        return KResult(-error_code);
    } else if (reply_type == Message::Type::Rerror) {
        // Contains an error message. We could attempt to parse it, but for now
        // we simply return -EIO instead. In 9P200.u, it can also contain a
        // numerical errno in an unspecified encoding; we ignore those too.
        StringView error_name;
        message >> error_name;
        dbg() << "Plan9FS: Received error name " << error_name;
        return KResult(-EIO);
    } else if ((u8)reply_type != (u8)request_type + 1) {
        // Other than those error messages. we only expect the matching reply
        // message type.
        dbg() << "Plan9FS: Received unexpected message type " << (u8)reply_type
              << " in response to " << (u8)request_type;
        return KResult(-EIO);
    } else {
        return KSuccess;
    }
}

ssize_t Plan9FS::adjust_buffer_size(ssize_t size) const
{
    ssize_t max_size = m_max_message_size - Message::max_header_size;
    return min(size, max_size);
}

Plan9FSInode::Plan9FSInode(Plan9FS& fs, u32 fid)
    : Inode(fs, fid)
{
}

NonnullRefPtr<Plan9FSInode> Plan9FSInode::create(Plan9FS& fs, u32 fid)
{
    return adopt(*new Plan9FSInode(fs, fid));
}

Plan9FSInode::~Plan9FSInode()
{
    Plan9FS::Message clunk_request { fs(), Plan9FS::Message::Type::Tclunk };
    clunk_request << fid();
    // FIXME: Should we observe this  error somehow?
    (void)fs().post_message_and_explicitly_ignore_reply(clunk_request);
}

KResult Plan9FSInode::ensure_open_for_mode(int mode)
{
    bool use_lopen = fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L;
    u32 l_mode = 0;
    u8 p9_mode = 0;

    {
        LOCKER(m_lock);

        // If it's already open in this mode, we're done.
        if ((m_open_mode & mode) == mode)
            return KSuccess;

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
    } else {
        Plan9FS::Message message { fs(), Plan9FS::Message::Type::Topen };
        message << fid() << p9_mode;
        return fs().post_message_and_wait_for_a_reply(message);
    }
}

ssize_t Plan9FSInode::read_bytes(off_t offset, ssize_t size, UserOrKernelBuffer& buffer, FileDescription*) const
{
    auto result = const_cast<Plan9FSInode&>(*this).ensure_open_for_mode(O_RDONLY);
    if (result.is_error())
        return result;

    size = fs().adjust_buffer_size(size);

    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Treadlink };
    StringView data;

    // Try readlink first.
    bool readlink_succeded = false;
    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L && offset == 0) {
        message << fid();
        result = fs().post_message_and_wait_for_a_reply(message);
        if (result.is_success()) {
            readlink_succeded = true;
            message >> data;
        }
    }

    if (!readlink_succeded) {
        message = Plan9FS::Message { fs(), Plan9FS::Message::Type::Tread };
        message << fid() << (u64)offset << (u32)size;
        result = fs().post_message_and_wait_for_a_reply(message);
        if (result.is_error())
            return result.error();
        data = message.read_data();
    }

    // Guard against the server returning more data than requested.
    size_t nread = min(data.length(), (size_t)size);
    if (!buffer.write(data.characters_without_null_termination(), nread))
        return -EFAULT;

    return nread;
}

ssize_t Plan9FSInode::write_bytes(off_t offset, ssize_t size, const UserOrKernelBuffer& data, FileDescription*)
{
    auto result = ensure_open_for_mode(O_WRONLY);
    if (result.is_error())
        return result;

    size = fs().adjust_buffer_size(size);

    auto data_copy = data.copy_into_string(size); // FIXME: this seems ugly
    if (data_copy.is_null())
        return -EFAULT;

    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Twrite };
    message << fid() << (u64)offset;
    message.append_data(data_copy);
    result = fs().post_message_and_wait_for_a_reply(message);
    if (result.is_error())
        return result.error();

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

void Plan9FSInode::flush_metadata()
{
    // Do nothing.
}

KResultOr<size_t> Plan9FSInode::directory_entry_count() const
{
    size_t count = 0;
    KResult result = traverse_as_directory([&count](auto&) {
        count++;
        return true;
    });

    if (result.is_error())
        return result;

    return count;
}

KResult Plan9FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    KResult result = KSuccess;

    // TODO: Should we synthesize "." and ".." here?

    if (fs().m_remote_protocol_version >= Plan9FS::ProtocolVersion::v9P2000L) {
        // Start by cloning the fid and opening it.
        auto clone_fid = fs().allocate_fid();
        {
            Plan9FS::Message clone_message { fs(), Plan9FS::Message::Type::Twalk };
            clone_message << fid() << clone_fid << (u16)0;
            result = fs().post_message_and_wait_for_a_reply(clone_message);
            if (result.is_error())
                return result;
            Plan9FS::Message open_message { fs(), Plan9FS::Message::Type::Tlopen };
            open_message << clone_fid << (u32)0;
            result = fs().post_message_and_wait_for_a_reply(open_message);
            if (result.is_error()) {
                Plan9FS::Message close_message { fs(), Plan9FS::Message::Type::Tclunk };
                close_message << clone_fid;
                // FIXME: Should we observe this error?
                (void)fs().post_message_and_explicitly_ignore_reply(close_message);
                return result;
            }
        }

        u64 offset = 0;
        u32 count = fs().adjust_buffer_size(8 * MiB);

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
                callback({ name, { fsid(), fs().allocate_fid() }, 0 });
            }
        }

        Plan9FS::Message close_message { fs(), Plan9FS::Message::Type::Tclunk };
        close_message << clone_fid;
        // FIXME: Should we observe this error?
        (void)fs().post_message_and_explicitly_ignore_reply(close_message);
        return result;
    } else {
        // TODO
        return KResult(-ENOTIMPL);
    }
}

RefPtr<Inode> Plan9FSInode::lookup(StringView name)
{
    u32 newfid = fs().allocate_fid();
    Plan9FS::Message message { fs(), Plan9FS::Message::Type::Twalk };
    message << fid() << newfid << (u16)1 << name;
    auto result = fs().post_message_and_wait_for_a_reply(message);

    if (result.is_error())
        return nullptr;

    return Plan9FSInode::create(fs(), newfid);
}

KResultOr<NonnullRefPtr<Inode>> Plan9FSInode::create_child(const String&, mode_t, dev_t, uid_t, gid_t)
{
    // TODO
    return KResult(-ENOTIMPL);
}

KResult Plan9FSInode::add_child(Inode&, const StringView&, mode_t)
{
    // TODO
    return KResult(-ENOTIMPL);
}

KResult Plan9FSInode::remove_child(const StringView&)
{
    // TODO
    return KResult(-ENOTIMPL);
}

KResult Plan9FSInode::chmod(mode_t)
{
    // TODO
    return KResult(-ENOTIMPL);
}

KResult Plan9FSInode::chown(uid_t, gid_t)
{
    // TODO
    return KResult(-ENOTIMPL);
}

KResult Plan9FSInode::truncate(u64 new_size)
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
    } else {
        // TODO: wstat version
        return KSuccess;
    }
}

}
