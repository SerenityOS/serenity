set(SOURCES "") # avoid pulling SOURCES from parent scope

lagom_tool(GenerateCSSMediaFeatureID       SOURCES GenerateCSSMediaFeatureID.cpp LIBS LagomMain)
lagom_tool(Generate_CSS_PropertyID_h       SOURCES Generate_CSS_PropertyID_h.cpp LIBS LagomMain)
lagom_tool(Generate_CSS_PropertyID_cpp     SOURCES Generate_CSS_PropertyID_cpp.cpp LIBS LagomMain)
lagom_tool(Generate_CSS_ValueID_h          SOURCES Generate_CSS_ValueID_h.cpp LIBS LagomMain)
lagom_tool(Generate_CSS_ValueID_cpp        SOURCES Generate_CSS_ValueID_cpp.cpp LIBS LagomMain)

add_subdirectory(WrapperGenerator)
