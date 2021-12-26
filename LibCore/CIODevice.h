#pragma once

#include <LibCore/CObject.h>
#include <AK/ByteBuffer.h>

class CIODevice : public CObject {
public:
    enum OpenMode {
        NotOpen      = 0,
        ReadOnly     = 1,
        WriteOnly    = 2,
        ReadWrite    = 3,
        Append       = 4,
        Truncate     = 8,
        MustBeNew    = 16,
    };

    virtual ~CIODevice() override;

    int fd() const { return m_fd; }
    unsigned mode() const { return m_mode; }
    bool eof() const { return m_eof; }

    int error() const { return m_error; }
    const char* error_string() const;

    bool has_error() const { return m_error != 0; }

    ByteBuffer read(int max_size);
    ByteBuffer read_line(int max_size);
    ByteBuffer read_all();

    bool write(const byte*, int size);

    // FIXME: I would like this to be const but currently it needs to call populate_read_buffer().
    bool can_read_line();

    bool can_read() const;

    enum class SeekMode {
        SetPosition,
        FromCurrentPosition,
        FromEndPosition,
    };

    bool seek(signed_qword, SeekMode = SeekMode::SetPosition, off_t* = nullptr);

    virtual bool open(CIODevice::OpenMode) = 0;
    virtual bool close();

    int printf(const char*, ...);

    virtual const char* class_name() const override { return "CIODevice"; }

protected:
    explicit CIODevice(CObject* parent = nullptr);

    void set_fd(int fd) { m_fd = fd; }
    void set_mode(OpenMode mode) { m_mode = mode; }
    void set_error(int error) { m_error = error; }
    void set_eof(bool eof) { m_eof = eof; }

private:
    bool populate_read_buffer();
    bool can_read_from_fd() const;

    int m_fd { -1 };
    int m_error { 0 };
    bool m_eof { false };
    OpenMode m_mode { NotOpen };
    Vector<byte> m_buffered_data;
};
