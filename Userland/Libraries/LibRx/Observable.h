/*
 * Copyright (c) 2022, Timur Sultanov <sultanovts@yandex.ru>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/Object.h>
#include <LibRx/Observer.h>
#include <LibRx/SubjectObserver.h>
#include <LibRx/CallbackObserver.h>

namespace Rx {

template<typename T>
class Subject;

template<typename T>
class Observable : public Core::Object {
    C_OBJECT_ABSTRACT(Observable);

public:
    virtual void subscribe(NonnullRefPtr<Observer<T>> observer) = 0;
    virtual void subscribe(Function<void(T const&)> callback) = 0;

    String originator() const
    {
        return m_originator;
    }

    template<typename TTarget>
    NonnullRefPtr<Subject<TTarget>> transform(Function<TTarget(T const&)> converter)
    {
        auto observable = Subject<TTarget>::construct(String::formatted("{} | {}", originator(), "transform"));
        subscribe([observable, converter = move(converter)](T const& source_value) mutable {
            observable->set_value(converter(source_value));
        });
        return observable;
    }

    NonnullRefPtr<Subject<T>> filter(Function<bool(T const&)> predicate)
    {
        auto observable = Subject<T>::construct(String::formatted("{} | {}", originator(), "filter"));
        subscribe([observable, predicate = move(predicate)](T const& source_value) mutable {
            if (predicate(source_value)) {
                observable->set_value(source_value);
            }
        });
        return observable;
    }

    void bind_oneway(NonnullRefPtr<Subject<T>> target_property)
    {
        subscribe(SubjectObserver<T>::construct(target_property));
    }

protected:
    Observable(String originator)
        : m_originator(originator)
    {
    }

    String m_originator;
};

template<typename T>
class Subject : public Rx::Observable<T> {
    C_OBJECT(Subject);

public:
    virtual void set_value(T value)
    {
        notify_observers(value);
    }

    virtual void subscribe(NonnullRefPtr<Observer<T>> observer) override
    {
        m_observers.append(observer);
    }

    virtual void subscribe(Function<void(T const&)> callback) override
    {
        auto observer = CallbackObserver<T>::construct(move(callback));
        subscribe(observer);
    }

protected:
    Subject(String originator)
        : Observable<T>(originator)
    {
    }

private:
    Vector<NonnullRefPtr<Observer<T>>> m_observers;

    void notify_observers(T value) const
    {
        int i = 1;
        for (auto observer : m_observers) {
            dbgln("BehaviorSubject: {}: Notifying observer {} of {}", Observable<T>::originator(), i, m_observers.size());
            i++;
            observer->call(value, Observable<T>::originator());
        }
    }
};

template<typename T>
void bind(NonnullRefPtr<Subject<T>> source, NonnullRefPtr<Subject<T>> target)
{
    target->bind_oneway(source);
    source->bind_oneway(target);
}

}