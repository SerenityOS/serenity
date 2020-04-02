#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

Shape* Shape::create_put_transition(const FlyString& property_name, u8 property_attributes)
{
    auto* new_shape = m_forward_transitions.get(property_name).value_or(nullptr);
    if (new_shape && new_shape->m_property_attributes == property_attributes)
        return new_shape;
    new_shape = heap().allocate<Shape>(this, property_name, property_attributes);
    m_forward_transitions.set(property_name, new_shape);
    return new_shape;
}

Shape* Shape::create_prototype_transition(Object* new_prototype)
{
    return heap().allocate<Shape>(this, new_prototype);
}

Shape::Shape()
{
}

Shape::Shape(Shape* previous_shape, const FlyString& property_name, u8 property_attributes)
    : m_previous(previous_shape)
    , m_property_name(property_name)
    , m_property_attributes(property_attributes)
    , m_prototype(previous_shape->m_prototype)
{
}

Shape::Shape(Shape* previous_shape, Object* new_prototype)
    : m_previous(previous_shape)
    , m_prototype(new_prototype)
{
}

Shape::~Shape()
{
}

void Shape::visit_children(Cell::Visitor& visitor)
{
    Cell::visit_children(visitor);
    if (m_prototype)
        visitor.visit(m_prototype);
    if (m_previous)
        visitor.visit(m_previous);
    for (auto& it : m_forward_transitions)
        visitor.visit(it.value);
}

Optional<PropertyMetadata> Shape::lookup(const FlyString& property_name) const
{
    return property_table().get(property_name);
}

const HashMap<FlyString, PropertyMetadata>& Shape::property_table() const
{
    ensure_property_table();
    return *m_property_table;
}

size_t Shape::property_count() const
{
    return property_table().size();
}

void Shape::ensure_property_table() const
{
    if (m_property_table)
        return;
    m_property_table = make<HashMap<FlyString, PropertyMetadata>>();

    // FIXME: We need to make sure the GC doesn't collect the transition chain as we're building it.
    //        Maybe some kind of RAII "prevent GC for a moment" helper thingy?

    Vector<const Shape*> transition_chain;
    for (auto* shape = this; shape->m_previous; shape = shape->m_previous) {
        transition_chain.append(shape);
    }

    u32 next_offset = 0;
    for (ssize_t i = transition_chain.size() - 1; i >= 0; --i) {
        auto* shape = transition_chain[i];
        if (shape->m_property_name.is_null()) {
            // Ignore prototype transitions as they don't affect the key map.
            continue;
        }
        m_property_table->set(shape->m_property_name, { next_offset++, shape->m_property_attributes });
    }
}

}
