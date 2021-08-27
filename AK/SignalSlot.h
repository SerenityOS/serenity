/*
 * Copyright (c) 2021, Arlen Keshabyan <arlen.albert@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/PairedPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Tuple.h>
#include <LibThreading/Mutex.h>

namespace AK {

class SlotBase
{
protected:
    template<typename... Args> friend class Signal;

    PairedPtr<> m_connection {};
    bool m_enabled { true };

public:
    void enabled(bool is_enabled) { m_enabled = is_enabled; }
    bool enabled() const { return m_enabled; }
};

template<typename... Args>
class Slot : public SlotBase
{
protected:
    Function<void (Args...)> m_functor {};

    friend class Connection;

public:
    Slot() = default;
    Slot(Function<void(Args...)> &&f) : m_functor{ forward<Function<void(Args...)>>(f) }{}
    void operator()(const Args &... args) { m_functor(args...); }
    bool is_disconnected() const { return !m_connection; }
    void clear() { m_connection.disconnect(); }

    bool operator == (const PairedPtr<> &s) const
    {
        return &m_connection == s.connected_ptr();
    }

    bool operator == (const Slot &s) const
    {
        return operator ==(s.m_connection);
    }
};

class SignalBase
{
public:
    virtual ~SignalBase() = default;
    virtual StringView name() const = 0;
    virtual bool enabled() const = 0;
    virtual void enabled(bool) const = 0;
    virtual size_t size() const = 0;
    virtual void enable_slot(const SlotBase&, bool enabled) = 0;
    virtual void enable_slot(const PairedPtr<>&, bool enabled) = 0;
    virtual bool is_slot_enabled(const SlotBase&) const = 0;
    virtual bool is_slot_enabled(const PairedPtr<>&) const = 0;
};

class Connection
{
public:
    Connection() = default;

    template<typename... Args>
    Connection(const SignalBase *signal, Slot<Args...> &s) : m_slot{ &s.m_connection }, m_signal { signal }
    {
    }

    Connection(Connection &&other) noexcept = default;
    Connection(const Connection &other) = delete;
    Connection &operator=(const Connection &other) = delete;
    Connection &operator=(Connection &&other) noexcept
    {
        disconnect();

        m_slot = move(other.m_slot);
        m_signal = move(other.m_signal);

        return *this;
    }

    ~Connection()
    {
        disconnect();
    }

    void disconnect()
    {
        if (m_slot) m_slot.disconnect();

        m_signal = nullptr;
    }

    bool is_disconnected() const { return !m_slot; }

    const SignalBase &signal() const
    {
        return *m_signal;
    }

    void enabled(bool enabled)
    {
        const_cast<SignalBase*>(m_signal)->enable_slot(m_slot, enabled);
    }

    bool enabled() const
    {
        return m_signal->is_slot_enabled(m_slot);
    }

protected:
    PairedPtr<> m_slot {};
    const SignalBase *m_signal { nullptr };
};

template<typename... Args>
class Signal : public SignalBase
{
public:
    Signal() = default;
    Signal(const String &name) : m_name{ name } {}
    Signal(Signal &&other) noexcept = default;
    Signal &operator=(Signal &&other) noexcept = default;
    virtual ~Signal() override = default;

    void emit(const Args &... args)
    {
        if (!m_enabled) return;

        Threading::MutexLocker lock { m_emit_lock };

        if (!m_enabled) return;

        {
            Threading::MutexLocker lock { m_connect_lock };

            m_slots.consume_from(m_pending_connections);
        }

        if (!m_slots.is_empty())
        {
            for (auto begin { m_slots.begin() }, end { m_slots.end() }; begin != end; ++begin)
            {
               auto &callable { *begin };

               if (callable.is_disconnected())
               {
                    begin.remove(m_slots);

                    continue;
               }

               if (callable.enabled()) callable(args...);
            }
        }
    }

    void operator() (const Args &... args)
    {
        emit(args...);
    }

    virtual Connection connect(Function<void(Args...)> &&callable)
    {
        Threading::MutexLocker lock { m_connect_lock };

        for (auto begin { m_pending_connections.begin() }, end { m_pending_connections.end() }; begin != end; ++begin)
           if ((*begin).is_disconnected()) begin.remove(m_pending_connections);

        m_pending_connections.append(forward<Function<void(Args...)>>(callable));

        return { this, m_pending_connections.last() };
    }

    virtual Connection operator += (Function<void(Args...)> &&callable)
    {
        return connect(forward<Function<void(Args...)>>(callable));
    }

    template<typename T>
    Connection connect(T *instance, void (T::*member_function)(Args...))
    {
        return connect([instance, member_function](Args... args) { (instance->*member_function)(args...); });
    }

    virtual void clear()
    {
        Threading::MutexLocker lock_emit { m_emit_lock };
        Threading::MutexLocker lock_conn { m_connect_lock };

        m_pending_connections.clear();
        m_slots.clear();
    }

    virtual size_t size() const override
    {
        Threading::MutexLocker lock_emit { m_emit_lock };

        return m_slots.size_slow();
    }

    void name(const String &name)
    {
        m_name = name;
    }

    virtual StringView name() const override
    {
        return m_name;
    }

    virtual bool enabled() const override
    {
        return m_enabled;
    }

    virtual void enabled(bool enabled) const override
    {
        m_enabled = enabled;
    }

    virtual void enable_slot(const SlotBase &Slot, bool enabled) override
    {
        enable_slot(Slot.m_connection, enabled);
    }

    virtual void enable_slot(const PairedPtr<> &slot, bool enabled) override
    {
        auto end { m_slots.end() };
        auto it { m_slots.find_if([&slot](auto &&s) { return s.m_connection == slot; }) };

        if (it != end) it->enabled(enabled);
    }

    virtual bool is_slot_enabled(const SlotBase &Slot) const override
    {
        return is_slot_enabled(Slot.m_connection);
    }

    virtual bool is_slot_enabled(const PairedPtr<> &slot) const override
    {
        auto end { m_slots.end() };
        auto it { m_slots.find_if([&slot](auto &&s) { return s.m_connection == slot; }) };

        if (it != end) return it->enabled();

        return false;
    }

protected:
    String m_name {};
    SinglyLinkedList<Slot<Args...>> m_slots {}, m_pending_connections {};
    mutable Atomic<bool> m_enabled { true };
    mutable Threading::Mutex m_emit_lock, m_connect_lock;
};

template<typename... Args>
class SignalEx : public Signal<SignalBase*, Args...>
{
public:
    using BaseClass = Signal<SignalBase*, Args...>;

    SignalEx() = default;
    SignalEx(const String &name) : BaseClass(name) {}
    SignalEx(SignalEx &&other) noexcept = default;
    SignalEx &operator=(SignalEx &&other) noexcept = default;
    virtual ~SignalEx() override = default;

    void emit(const Args&... args)
    {
        BaseClass::emit(this, args...);
    }

    void operator() (const Args&... args)
    {
        emit(args...);
    }
};

template<typename Key, template <typename...> typename SigType, typename... Args>
requires IsBaseOf<SignalBase, SigType<Args...>>
class SignalSetBase
{
public:
    using SignalType = SigType<Args...>;
    using KeyType = Key;

    SignalSetBase() = default;
    virtual ~SignalSetBase() = default;

    void emit(const Args &... args) const
    {
        for (auto &&s : m_signals) s.second->emit(args...);
    }

    void operator() (const Args &... args) const
    {
        emit(args...);
    }

    virtual SignalType &get_signal(const KeyType &key)
    {
        auto signal { m_signals.find(key) };

        if (signal != m_signals.end()) return *(*signal).value.ptr();

        if constexpr (IsSame<RemoveCVReference<KeyType>, String> || IsSame<RemoveCVReference<KeyType>, StringView>)
            m_signals.set(key, make<SignalType>(key));
        else
            m_signals.set(key, make<SignalType>());

        return *m_signals.get(key).value();
    }

    bool exists(const KeyType &key) const
    {
        return m_signals.contains(key);
    }

    auto get_signal_keys() const
    {
        return m_signals.keys();
    }

    auto get_signal_count() const
    {
        return m_signals.size();
    }

    SignalType &operator[](const KeyType &key)
    {
        return get_signal(key);
    }

    auto begin() const
    {
        return m_signals.begin();
    }

    auto end() const
    {
        return m_signals.end();
    }

protected:
    HashMap<KeyType, OwnPtr<SignalType>> m_signals {};
};

template<typename Key, typename... Args> using SignalSet = SignalSetBase<Key, Signal, Args...>;
template<typename Key, typename... Args> using SignalExSet = SignalSetBase<Key, SignalEx, Args...>;

template<template <typename...> typename SignalType, typename... Args>
requires IsBaseOf<SignalBase, SignalType<Args...>>
class BridgedSignalBase : public SignalType<Args...>
{
public:
    using BaseClass = SignalType<Args...>;
    using BridgedSignalType = BridgedSignalBase;
    using EmitFunctionType = Function<bool(BridgedSignalBase*)>;

    BridgedSignalBase() = default;
    BridgedSignalBase(const String &name) : BaseClass { name } {}
    BridgedSignalBase(const String &name, EmitFunctionType&& emit_functor) : BaseClass { name }, m_emit_notification_functor { forward<EmitFunctionType>(emit_functor) } {}
    BridgedSignalBase(EmitFunctionType&& emit_functor) : BridgedSignalBase { String {}, forward<EmitFunctionType>(emit_functor) } {}
    BridgedSignalBase(const String &name, bool bridge_enabled) : BaseClass { name }, m_bridge_enabled { bridge_enabled } {}
    BridgedSignalBase(bool bridge_enabled) : BridgedSignalBase { String {}, bridge_enabled } {}
    BridgedSignalBase(BridgedSignalBase &&other) noexcept = default;
    BridgedSignalBase &operator=(BridgedSignalBase &&other) noexcept = default;
    virtual ~BridgedSignalBase() override = default;

    void emit(const Args&... args)
    {
        if (!BaseClass::m_enabled) return;

        if (m_bridge_enabled)
        {
            {
                Threading::MutexLocker lock { m_queue_lock };

                m_signal_queue.append(Tuple(args...));
            }

            if (!m_emit_notification_functor || !m_emit_notification_functor(this)) invoke_next();
        }
        else BaseClass::emit(args...);
    }

    void operator() (const Args&... args)
    {
        emit(args...);
    }

    void emit_sync(const Args&... args)
    {
        BaseClass::emit(args...);
    }

    virtual bool invoke_next()
    {
        if (m_signal_queue.is_empty()) return false;

        Threading::MutexLocker lock { m_queue_lock };

        if (m_signal_queue.is_empty()) return false;

        m_signal_queue.take_first().apply_as_args([this](const Args&... a){ BaseClass::emit(a...); });

        return !m_signal_queue.is_empty();
    }

    virtual void invoke_all()
    {
        if (m_signal_queue.is_empty()) return;

        Threading::MutexLocker lock { m_queue_lock };

        if (m_signal_queue.is_empty()) return;

        for (auto &&args : m_signal_queue) args.apply_as_args([this](const Args&... a){ BaseClass::emit(a...); });

        m_signal_queue.clear();
    }

    virtual void invoke_last_and_clear()
    {
        if (m_signal_queue.is_empty()) return;

        Threading::MutexLocker lock { m_queue_lock };

        if (m_signal_queue.is_empty()) return;

        m_signal_queue.last().apply_as_args([this](const Args&... a){ BaseClass::emit(a...); });

        m_signal_queue.clear();
    }

    void set_emit_functor(EmitFunctionType&& emit_functor)
    {
        m_emit_notification_functor = forward<EmitFunctionType>(emit_functor);
    }

    const EmitFunctionType &get_emit_functor() const
    {
        return m_emit_notification_functor;
    }

    uint64_t get_queue_size() const
    {
        Threading::MutexLocker lock { m_queue_lock };

        return m_signal_queue.size_slow();
    }

    void set_bridge_enabled(bool enabled)
    {
        m_bridge_enabled = enabled;
    }

    bool get_bridge_enabled() const
    {
        return m_bridge_enabled;
    }

    void clear_queue()
    {
        if (m_signal_queue.is_empty()) return;

        Threading::MutexLocker lock { m_queue_lock };

        if (m_signal_queue.is_empty()) return;

        m_signal_queue.clear();
    }

protected:
    Atomic<bool> m_bridge_enabled { true };
    Threading::Mutex m_queue_lock {};
    SinglyLinkedList<Tuple<Args...>> m_signal_queue {};
    EmitFunctionType m_emit_notification_functor { nullptr };
};

template<typename... Args> using BridgedSignal = BridgedSignalBase<Signal, Args...>;
template<typename... Args> using BridgedSignalEx = BridgedSignalBase<SignalEx, Args...>;

struct ConnectionBag
{
    SinglyLinkedList<Connection> connections;

    ConnectionBag &operator= (Connection &&c) { connections.append(forward<Connection>(c)); return *this; }
};

}
