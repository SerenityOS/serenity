#pragma once

#include "DoubleBuffer.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <Kernel/UnixTypes.h>

class FIFO : public Retainable<FIFO> {
public:
    enum Direction {
        Neither, Reader, Writer
    };

    static RetainPtr<FIFO> from_fifo_id(dword);

    static Retained<FIFO> create(uid_t);
    ~FIFO();

    uid_t uid() const { return m_uid; }

    void open(Direction);
    void close(Direction);

    ssize_t write(const byte*, ssize_t);
    ssize_t read(byte*, ssize_t);

    bool can_read() const;
    bool can_write() const;

private:
    explicit FIFO(uid_t);

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    DoubleBuffer m_buffer;

    uid_t m_uid { 0 };
};
