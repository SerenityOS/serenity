#pragma once

#include <Kernel/DoubleBuffer.h>
#include <Kernel/File.h>
#include <Kernel/UnixTypes.h>

class FileDescription;

class FIFO final : public File {
public:
    enum class Direction : byte {
        Neither,
        Reader,
        Writer
    };

    static RetainPtr<FIFO> from_fifo_id(dword);

    static Retained<FIFO> create(uid_t);
    virtual ~FIFO() override;

    uid_t uid() const { return m_uid; }

    Retained<FileDescription> open_direction(Direction);

    void attach(Direction);
    void detach(Direction);

private:
    // ^File
    virtual ssize_t write(FileDescription&, const byte*, ssize_t) override;
    virtual ssize_t read(FileDescription&, byte*, ssize_t) override;
    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override;
    virtual String absolute_path(const FileDescription&) const override;
    virtual const char* class_name() const override { return "FIFO"; }
    virtual bool is_fifo() const override { return true; }

    explicit FIFO(uid_t);

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    DoubleBuffer m_buffer;

    uid_t m_uid { 0 };
};
