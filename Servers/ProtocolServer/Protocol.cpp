#include <AK/HashMap.h>
#include <ProtocolServer/Protocol.h>

static HashMap<String, Protocol*>& all_protocols()
{
    static HashMap<String, Protocol*> map;
    return map;
}

Protocol* Protocol::find_by_name(const String& name)
{
    return all_protocols().get(name).value_or(nullptr);
}

Protocol::Protocol(const String& name)
{
    all_protocols().set(name, this);
}

Protocol::~Protocol()
{
    ASSERT_NOT_REACHED();
}
