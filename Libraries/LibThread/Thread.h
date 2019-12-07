#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <LibCore/CObject.h>

namespace LibThread {

class Thread final : public CObject {
    C_OBJECT(Thread);

public:
    explicit Thread(Function<int()> action, StringView thread_name = nullptr);
    virtual ~Thread();

    void start();
    void quit(int code = 0);

private:
    Function<int()> m_action;
    int m_tid { -1 };
    String m_thread_name;
};

}
