/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FlyString.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/DOM/Node.h>

namespace Web {
namespace Bindings {

NodeWrapper* wrap(JS::Heap& heap, Node& node)
{
    if (is<Document>(node))
        return static_cast<NodeWrapper*>(wrap_impl(heap, to<Document>(node)));
    if (is<HTMLCanvasElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(heap, to<HTMLCanvasElement>(node)));
    if (is<HTMLImageElement>(node))
        return static_cast<NodeWrapper*>(wrap_impl(heap, to<HTMLImageElement>(node)));
    if (is<Element>(node))
        return static_cast<NodeWrapper*>(wrap_impl(heap, to<Element>(node)));
    return static_cast<NodeWrapper*>(wrap_impl(heap, node));
}

NodeWrapper::NodeWrapper(JS::GlobalObject& global_object, Node& node)
    : EventTargetWrapper(global_object, node)
{
}

void NodeWrapper::initialize(JS::Interpreter& interpreter, JS::GlobalObject&)
{
    put("nodeName", JS::js_string(interpreter.heap(), node().node_name()));
}

NodeWrapper::~NodeWrapper()
{
}

Node& NodeWrapper::node()
{
    return static_cast<Node&>(EventTargetWrapper::impl());
}

const Node& NodeWrapper::node() const
{
    return static_cast<const Node&>(EventTargetWrapper::impl());
}

}
}
