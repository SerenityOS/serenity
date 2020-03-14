#include <LibJS/PrimitiveString.h>
#include <LibJS/Value.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/DOM/Document.h>

namespace Web {
namespace Bindings {

DocumentWrapper::DocumentWrapper(Document& document)
    : NodeWrapper(document)
{
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
