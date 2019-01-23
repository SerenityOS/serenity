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

    static RetainPtr<FIFO> create();

    void open(Direction);
    void close(Direction);

    ssize_t write(const byte*, size_t);
    ssize_t read(byte*, size_t);

    bool can_read() const;
    bool can_write() const;

private:
    FIFO();

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    DoubleBuffer m_buffer;
};
