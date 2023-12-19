/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Socket.h>
#include <LibCore/System.h>
#include <LibIPC/File.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MessagePortPrototype.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/MessagePort.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>

namespace Web::HTML {

constexpr u8 IPC_FILE_TAG = 0xA5;

JS_DEFINE_ALLOCATOR(MessagePort);

JS::NonnullGCPtr<MessagePort> MessagePort::create(JS::Realm& realm)
{
    return realm.heap().allocate<MessagePort>(realm, realm);
}

MessagePort::MessagePort(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

MessagePort::~MessagePort() = default;

void MessagePort::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MessagePortPrototype>(realm, "MessagePort"_fly_string));
}

void MessagePort::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_remote_port);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#message-ports:transfer-steps
WebIDL::ExceptionOr<void> MessagePort::transfer_steps(HTML::TransferDataHolder& data_holder)
{
    // 1. Set value's has been shipped flag to true.
    m_has_been_shipped = true;

    // FIXME: 2. Set dataHolder.[[PortMessageQueue]] to value's port message queue.
    // FIXME: Support delivery of messages that haven't been delivered yet on the other side

    // 3. If value is entangled with another port remotePort, then:
    if (is_entangled()) {
        // 1. Set remotePort's has been shipped flag to true.
        m_remote_port->m_has_been_shipped = true;

        // 2. Set dataHolder.[[RemotePort]] to remotePort.
        auto fd = MUST(m_socket->release_fd());
        m_socket = nullptr;
        data_holder.fds.append(fd);
        data_holder.data.append(IPC_FILE_TAG);
    }

    // 4. Otherwise, set dataHolder.[[RemotePort]] to null.
    else {
        data_holder.data.append(0);
    }

    return {};
}

WebIDL::ExceptionOr<void> MessagePort::transfer_receiving_steps(HTML::TransferDataHolder& data_holder)
{
    // 1. Set value's has been shipped flag to true.
    m_has_been_shipped = true;

    // FIXME 2. Move all the tasks that are to fire message events in dataHolder.[[PortMessageQueue]] to the port message queue of value,
    //     if any, leaving value's port message queue in its initial disabled state, and, if value's relevant global object is a Window,
    //     associating the moved tasks with value's relevant global object's associated Document.

    // 3. If dataHolder.[[RemotePort]] is not null, then entangle dataHolder.[[RemotePort]] and value.
    //     (This will disentangle dataHolder.[[RemotePort]] from the original port that was transferred.)
    auto fd_tag = data_holder.data.take_first();
    if (fd_tag == IPC_FILE_TAG) {
        auto fd = data_holder.fds.take_first();
        m_socket = MUST(Core::LocalSocket::adopt_fd(fd.take_fd()));
    } else if (fd_tag != 0) {
        dbgln("Unexpected byte {:x} in MessagePort transfer data", fd_tag);
        VERIFY_NOT_REACHED();
    }

    return {};
}

void MessagePort::disentangle()
{
    m_remote_port->m_remote_port = nullptr;
    m_remote_port = nullptr;

    m_socket = nullptr;
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#entangle
void MessagePort::entangle_with(MessagePort& remote_port)
{
    if (m_remote_port.ptr() == &remote_port)
        return;

    // 1. If one of the ports is already entangled, then disentangle it and the port that it was entangled with.
    if (is_entangled())
        disentangle();
    if (remote_port.is_entangled())
        remote_port.disentangle();

    // 2. Associate the two ports to be entangled, so that they form the two parts of a new channel.
    //    (There is no MessageChannel object that represents this channel.)
    remote_port.m_remote_port = this;
    m_remote_port = &remote_port;

    int fds[2] = {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fds));
    auto socket0 = MUST(Core::LocalSocket::adopt_fd(fds[0]));
    MUST(socket0->set_blocking(false));
    MUST(socket0->set_close_on_exec(true));
    auto socket1 = MUST(Core::LocalSocket::adopt_fd(fds[1]));
    MUST(socket1->set_blocking(false));
    MUST(socket1->set_close_on_exec(true));

    m_socket = move(socket0);
    m_remote_port->m_socket = move(socket1);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-postmessage-options
WebIDL::ExceptionOr<void> MessagePort::post_message(JS::Value message, Vector<JS::Handle<JS::Object>> const& transfer)
{
    // 1. Let targetPort be the port with which this MessagePort is entangled, if any; otherwise let it be null.
    JS::GCPtr<MessagePort> target_port = m_remote_port;

    // 2. Let options be «[ "transfer" → transfer ]».
    auto options = StructuredSerializeOptions { transfer };

    // 3. Run the message port post message steps providing this, targetPort, message and options.
    return message_port_post_message_steps(target_port, message, options);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-postmessage
WebIDL::ExceptionOr<void> MessagePort::post_message(JS::Value message, StructuredSerializeOptions const& options)
{
    // 1. Let targetPort be the port with which this MessagePort is entangled, if any; otherwise let it be null.
    JS::GCPtr<MessagePort> target_port = m_remote_port;

    // 2. Run the message port post message steps providing targetPort, message and options.
    return message_port_post_message_steps(target_port, message, options);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#message-port-post-message-steps
WebIDL::ExceptionOr<void> MessagePort::message_port_post_message_steps(JS::GCPtr<MessagePort> target_port, JS::Value message, StructuredSerializeOptions const& options)
{
    auto& realm = this->realm();
    auto& vm = this->vm();

    // 1. Let transfer be options["transfer"].
    auto const& transfer = options.transfer;

    // 2. If transfer contains this MessagePort, then throw a "DataCloneError" DOMException.
    for (auto const& handle : transfer) {
        if (handle == this)
            return WebIDL::DataCloneError::create(realm, "Cannot transfer a MessagePort to itself"_fly_string);
    }

    // 3. Let doomed be false.
    bool doomed = false;

    // 4. If targetPort is not null and transfer contains targetPort, then set doomed to true and optionally report to a developer console that the target port was posted to itself, causing the communication channel to be lost.
    if (target_port) {
        for (auto const& handle : transfer) {
            if (handle == target_port.ptr()) {
                doomed = true;
                dbgln("FIXME: Report to a developer console that the target port was posted to itself, causing the communication channel to be lost");
            }
        }
    }

    // 5. Let serializeWithTransferResult be StructuredSerializeWithTransfer(message, transfer). Rethrow any exceptions.
    auto serialize_with_transfer_result = TRY(structured_serialize_with_transfer(vm, message, transfer));

    // 6. If targetPort is null, or if doomed is true, then return.
    if (!target_port || doomed)
        return {};

    // FIXME: 7. Add a task that runs the following steps to the port message queue of targetPort:
    // FIXME: Implement this using the port message queue/unshipped port message queue concept
    main_thread_event_loop().task_queue().add(HTML::Task::create(HTML::Task::Source::PostedMessage, nullptr, [target_port, serialize_with_transfer_result = move(serialize_with_transfer_result)]() mutable {
        // FIXME: 1. Let finalTargetPort be the MessagePort in whose port message queue the task now finds itself.
        // FIXME: NOTE: This can be different from targetPort, if targetPort itself was transferred and thus all its tasks moved along with it.
        auto final_target_port = target_port;

        // 2. Let targetRealm be finalTargetPort's relevant realm.
        auto& target_realm = relevant_realm(*final_target_port);
        auto& target_vm = target_realm.vm();

        // 3. Let deserializeRecord be StructuredDeserializeWithTransfer(serializeWithTransferResult, targetRealm).
        TemporaryExecutionContext context { relevant_settings_object(*final_target_port) };
        auto deserialize_record_or_error = structured_deserialize_with_transfer(target_vm, serialize_with_transfer_result);
        if (deserialize_record_or_error.is_error()) {
            // If this throws an exception, catch it, fire an event named messageerror at finalTargetPort, using MessageEvent, and then return.
            auto exception = deserialize_record_or_error.release_error();
            MessageEventInit event_init {};
            final_target_port->dispatch_event(MessageEvent::create(target_realm, HTML::EventNames::messageerror, event_init));
            return;
        }
        auto deserialize_record = deserialize_record_or_error.release_value();

        // 4. Let messageClone be deserializeRecord.[[Deserialized]].
        auto message_clone = deserialize_record.deserialized;

        // 5. Let newPorts be a new frozen array consisting of all MessagePort objects in deserializeRecord.[[TransferredValues]], if any, maintaining their relative order.
        // FIXME: Use a FrozenArray
        Vector<JS::Handle<JS::Object>> new_ports;
        for (auto const& object : deserialize_record.transferred_values) {
            if (is<HTML::MessagePort>(*object)) {
                new_ports.append(object);
            }
        }

        // 6. Fire an event named message at finalTargetPort, using MessageEvent, with the data attribute initialized to messageClone and the ports attribute initialized to newPorts.
        MessageEventInit event_init {};
        event_init.data = message_clone;
        event_init.ports = move(new_ports);
        final_target_port->dispatch_event(MessageEvent::create(target_realm, HTML::EventNames::message, event_init));
    }));

    return {};
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-start
void MessagePort::start()
{
    VERIFY(m_socket);

    // TODO: The start() method steps are to enable this's port message queue, if it is not already enabled.
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-close
void MessagePort::close()
{
    // 1. Set this MessagePort object's [[Detached]] internal slot value to true.
    set_detached(true);

    // 2. If this MessagePort object is entangled, disentangle it.
    if (is_entangled())
        disentangle();
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                         \
    void MessagePort::set_##attribute_name(WebIDL::CallbackType* value) \
    {                                                                   \
        set_event_handler_attribute(event_name, value);                 \
    }                                                                   \
    WebIDL::CallbackType* MessagePort::attribute_name()                 \
    {                                                                   \
        return event_handler_attribute(event_name);                     \
    }
ENUMERATE_MESSAGE_PORT_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
