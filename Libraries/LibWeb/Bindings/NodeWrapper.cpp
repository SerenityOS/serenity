#include <LibJS/PrimitiveString.h>
#include <LibJS/Value.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/DOM/Node.h>

namespace Web {
namespace Bindings {

NodeWrapper::NodeWrapper(Node& node)
    : m_node(node)
{
    put("nodeName", JS::js_string(heap(), node.tag_name()));
}

NodeWrapper::~NodeWrapper()
{
}

}
}
