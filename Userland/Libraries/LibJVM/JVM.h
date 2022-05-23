#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/StringView.h>
#include <LibJS/Heap/Heap.h>
#include <LibJVM/Thread.h>
#include <LibJVM/Forward.h>

namespace JVM {

class JVM {
 
public:

    JVM();
    bool load_from_class_file(StringView path);
    int resolve_class_reference(StringView ref);
    Class get_class_from_index(int index);

private:
    AK::NonnullOwnPtrVector<Thread> m_threads;

        
};

}
