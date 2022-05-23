#include "Value.h"
#include <AK/Utf8View.h>

namespace JVM {

void Value::init_from_descriptor(AK::Utf8View desc) {
    if (!desc.contains('[')) {
        //just a base type
        switch (desc.as_string()[0]) {
            case 'B':
                this->m_type = Type::Byte;
                break;
            case 'C':
                this->m_type = Type::Char;
                break;
            case 'D':
                this->m_type = Type::Double;
                break;
            case 'F':
                this->m_type = Type::Float;
                break;
            case 'I':
                this->m_type = Type::Int;
                break;
            case 'J':
                this->m_type = Type::Long;
                break;
            case 'S':
                this->m_type = Type::Short;
                break;
            case 'Z':
                this->m_type = Type::Boolean;
                break;
            case 'L':
                //I think the give string is a path to follow, but this needs verification
                //FIXME: Actually implement this class lookup
                //This will need a reference to the JVM so we can look up an index
                break;
        }
    }

    //Think about how to represent Arrays better.

}

}
