/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/Plan9FS/Definitions.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

class Plan9FS;
class Plan9FSInode;

class Plan9FSMessage {
    friend class Plan9FS;
    friend class Plan9FSInode;

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
        Decoder& operator>>(Plan9FSQIdentifier&);
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

    Plan9FSMessage& operator<<(u8);
    Plan9FSMessage& operator<<(u16);
    Plan9FSMessage& operator<<(u32);
    Plan9FSMessage& operator<<(u64);
    Plan9FSMessage& operator<<(StringView);
    ErrorOr<void> append_data(StringView);

    template<typename T>
    Plan9FSMessage& operator>>(T& t)
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

    Plan9FSMessage(Plan9FS&, Type);
    Plan9FSMessage(NonnullOwnPtr<KBuffer>&&);
    ~Plan9FSMessage();
    Plan9FSMessage& operator=(Plan9FSMessage&&);

    KBuffer const& build();

    static constexpr size_t max_header_size = 24;

private:
    template<typename N>
    Plan9FSMessage& append_number(N number)
    {
        VERIFY(!m_have_been_built);
        // FIXME: Handle append failure.
        (void)m_builder.append(reinterpret_cast<char const*>(&number), sizeof(number));
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

}
