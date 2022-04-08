#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Heap/Heap.h>
#include <LibJVM/Thread.h>
#include <LibJVM/Forward.h>

namespace JVM {

class JVM {
 
public:

private:
    AK::NonnullOwnPtrVector<Thread> m_threads;
    Heap m_heap;
        
};

}
