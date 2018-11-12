#pragma once

#include <AK/CircularQueue.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <VirtualFileSystem/UnixTypes.h>

class FIFO : public Retainable<FIFO> {
public:
    enum Direction {
        Neither, Reader, Writer
    };

    static RetainPtr<FIFO> create();

    void open(Direction);
    void close(Direction);

    Unix::ssize_t write(const byte*, Unix::size_t);
    Unix::ssize_t read(byte*, Unix::size_t);

    bool can_read() const;
    bool can_write() const;

private:
    FIFO();

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    CircularQueue<byte, 16> m_queue;
};
