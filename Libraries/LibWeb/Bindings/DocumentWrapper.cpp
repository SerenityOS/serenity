#include <LibJS/PrimitiveString.h>
#include <LibJS/Value.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web {
namespace Bindings {

DocumentWrapper::DocumentWrapper(Document& document)
    : NodeWrapper(document)
{
    put_native_function("getElementById", [this](JS::Interpreter&, Vector<JS::Value> arguments) -> JS::Value {
        if (arguments.is_empty())
            return JS::js_null();
        auto id = arguments[0].to_string();
        auto* element = node().get_element_by_id(id);
        if (!element)
            return JS::js_null();
        return wrap(heap(), const_cast<Element&>(*element));
    });
}

DocumentWrapper::~DocumentWrapper()
{
}

Document& DocumentWrapper::node()
{
    return static_cast<Document&>(NodeWrapper::node());
}

const Document& DocumentWrapper::node() const
{
    return static_cast<const Document&>(NodeWrapper::node());
}

}
}
