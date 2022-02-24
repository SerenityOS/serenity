set(SOURCES "") # avoid pulling SOURCES from parent scope

lagom_tool(Generate_CSS_PropertyID_h   SOURCES Generate_CSS_PropertyID_h.cpp)
lagom_tool(Generate_CSS_PropertyID_cpp SOURCES Generate_CSS_PropertyID_cpp.cpp)
lagom_tool(Generate_CSS_ValueID_h      SOURCES Generate_CSS_ValueID_h.cpp)
lagom_tool(Generate_CSS_ValueID_cpp    SOURCES Generate_CSS_ValueID_cpp.cpp)

add_subdirectory(WrapperGenerator)
