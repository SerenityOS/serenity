#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/File.h>

namespace Media::Reader {

class BitStreamReader : public RefCounted<BitStreamReader> {
public:
    enum class Endian {
        BIG,
        LITTLE
    };

    BitStreamReader(AK::String filePath, Endian const endianess);

    bool is_open() const;

    // FIXME: redo all the present reads but this time taking into actual bitstream reading
    //        i.e. don't use IODevice directly to get a byte.
    //        This'll be needed for MPEG-PS/TS, h264, etc

    template<typename T>
    [[nodiscard]] bool read_be(T& out)
    {
        constexpr size_t bytes = sizeof(T);

        VERIFY(m_file.ptr() != nullptr);
        auto const buf = m_file->read(bytes);
        if (buf.size() != bytes) {
            return false;
        }

        for (size_t ix = 0; ix < bytes; ++ix) {
            out |= static_cast<T>(buf[ix]) << (8 * (bytes - ix - 1));
        }
        m_offset += bytes;
        return true;
    }

    template<typename T>
    [[nodiscard]] bool read_le(T& out)
    {
        constexpr size_t bytes = sizeof(T);

        VERIFY(m_file.ptr() != nullptr);
        auto const buf = m_file->read(bytes);
        if (buf.size() != bytes) {
            return false;
        }

        for (size_t ix = 0; ix < bytes; ++ix) {
            out |= static_cast<T>(buf[ix]) << (8 * ix);
        }
        m_offset += bytes;
        return true;
    }

    template<typename T>
    [[nodiscard]] bool read(T& out)
    {
        if (m_endian == Endian::BIG) {
            return read_be<T>(out);
        }
        return read_le<T>(out);
    }

    // Have fourccs read only 1 way (the same way Media::fourcc() works - LE)
    [[nodiscard]] bool read_fourcc(u32& value);

    [[nodiscard]] bool read(ByteBuffer& buf, size_t const bytes);

    template<typename T>
    T read_bits(u32 const count)
    {
        // FIXME: return value based on partial byte 'reading'
        return 0;
    }

    [[nodiscard]] bool read_flag();

    u32 read_exp_golomb();

    i32 read_signed_exp_golomb();

    u64 offset() const;

    bool at_eof() const;

    [[nodiscard]] bool seek(u64 const to, Core::SeekMode const mode = Core::SeekMode::FromCurrentPosition);

private:
    AK::String m_file_path;
    RefPtr<Core::File> m_file;
    u64 m_offset { 0 };
    Endian m_endian;
    bool m_open;
    // For reading bits
    u8 m_last_byte;
    i8 m_last_byte_index { -1 };
};

}