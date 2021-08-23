#include <LibMedia/readers/BitStreamReader.h>

using Media::Reader::BitStreamReader;

BitStreamReader::BitStreamReader(AK::String filePath, BitStreamReader::Endian const endianess)
    : m_file_path(std::move(filePath))
    , m_endian(endianess)
{
    auto result = Core::File::open(m_file_path, Core::OpenMode::ReadOnly);
    if (result.is_error() == true) {
        m_open = false;
        return;
    }
    VERIFY(result.value() != nullptr);
    m_file = result.value();
}

bool BitStreamReader::is_open() const
{
    return m_file.is_null() == false;
}

bool BitStreamReader::read_fourcc(u32& value)
{
    return read_le<u32>(value);
}

bool BitStreamReader::read(ByteBuffer& buf, size_t const bytes)
{
    VERIFY(m_file.is_null() == false);
    buf = m_file->read(bytes);
    m_offset += buf.size();
    return buf.size() >= bytes;
}

u64 BitStreamReader::offset() const
{
    return m_offset;
}

bool BitStreamReader::at_eof() const
{
    return m_file->eof();
}

bool BitStreamReader::seek(u64 const to, Core::SeekMode const mode)
{
    VERIFY(m_file.ptr() != nullptr);
    // FIXME: switch on mode to adjust offset. Currently assuming FromCurrentPos.
    m_offset += to;
    // Never seeking backwards. It's a bit-stream.
    return m_file->seek(static_cast<i64>(to), mode);
}