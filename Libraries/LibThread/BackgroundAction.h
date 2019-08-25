#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CObject.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

namespace LibThread {

template<typename Result>
class BackgroundAction;

class BackgroundActionBase {
    template<typename Result>
    friend class BackgroundAction;

private:
    BackgroundActionBase() {}

    static Lockable<Queue<Function<void()>>>& all_actions();
    static Thread& background_thread();
};

template<typename Result>
class BackgroundAction final : public CObject
    , public RefCounted<BackgroundAction<Result>>
    , private BackgroundActionBase {

    C_OBJECT(BackgroundAction);

public:
    static NonnullRefPtr<BackgroundAction<Result>> create(
        Function<Result()> action,
        Function<void(Result)> on_complete = nullptr
    )
    {
        return adopt(*new BackgroundAction(move(action), move(on_complete)));
    }

    virtual ~BackgroundAction() {}

private:

    BackgroundAction(Function<Result()> action, Function<void(Result)> on_complete)
        : CObject(background_thread())
        , m_action(move(action))
        , m_on_complete(move(on_complete))
    {
        LOCKER(all_actions().lock());

        this->ref();
        all_actions().resource().enqueue([this] {
            m_result = m_action();
            if (m_on_complete) {
                CEventLoop::main().post_event(*this, make<CDeferredInvocationEvent>([this](CObject&) {
                    m_on_complete(m_result.release_value());
                    this->deref();
                }));
                CEventLoop::main().wake();
            } else
                this->deref();
        });
    }

    Function<Result()> m_action;
    Function<void(Result)> m_on_complete;
    Optional<Result> m_result;
};

}
