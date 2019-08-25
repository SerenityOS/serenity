#pragma once

#include <AK/CircularQueue.h>
#include <LibThread/Thread.h>
#include <stdio.h>

class Painter;
class Rect;

class WSCPUMonitor {
public:
    WSCPUMonitor();

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }
    int capacity() const { return m_cpu_history.capacity(); }
    void paint(Painter&, const Rect&);

private:
    void monitor();

    void get_cpu_usage(unsigned& busy, unsigned& idle);

    CircularQueue<float, 30> m_cpu_history;
    bool m_dirty { false };
    LibThread::Thread m_thread;
};
