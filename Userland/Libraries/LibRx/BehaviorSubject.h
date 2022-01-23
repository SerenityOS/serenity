#pragma once

#include <LibRx/Observable.h>
#include <LibRx/CallbackObserver.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>

namespace Rx {

template<typename T>
class BehaviorSubject final : public Subject<T> {
    C_OBJECT(BehaviorSubject);

public:
    T value() const
    {
        return m_value;
    }

    virtual void set_value(T value) override
    {
        if (m_value == value)
            return;
        m_value = value;
        Subject<T>::set_value(value);
    }

    virtual void subscribe(NonnullRefPtr<Observer<T>> observer) override
    {
        Subject<T>::subscribe(observer);
        observer->call(m_value, Observable<T>::originator());
    }

    virtual void subscribe(Function<void(T const&)> callback) override
    {
        auto observer = Rx::CallbackObserver<T>::construct(move(callback));
        Subject<T>::subscribe(observer);
        observer->call(m_value, Observable<T>::originator());
    }

    template<typename TTarget>
    NonnullRefPtr<BehaviorSubject<TTarget>> transform(Function<TTarget(T const&)> converter)
    {
        auto observable = BehaviorSubject<TTarget>::construct(converter(m_value), String::formatted("{} | {}", Observable<T>::originator(), "transform"));
        subscribe([observable, converter = move(converter)](T const& source_value) mutable {
            observable->set_value(converter(source_value));
        });
        return observable;
    }

    NonnullRefPtr<BehaviorSubject<T>> filter(Function<bool(T const&)> predicate)
    {
        auto observable = BehaviorSubject<T>::construct(m_value, String::formatted("{} | {}", Observable<T>::originator(), "filter"));
        subscribe([observable, predicate = move(predicate)](T const& source_value) mutable {
            if (predicate(source_value)) {
                observable->set_value(source_value);
            }
        });
        return observable;
    }
private:
    BehaviorSubject(T initial_value, String originator)
        : Subject<T>(originator)
        , m_value(initial_value)
    {
    }

    BehaviorSubject(String originator)
        : Subject<T>(originator)
    {
    }

    T m_value;
};

}
