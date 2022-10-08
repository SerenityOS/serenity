set(SOURCES "") # avoid pulling SOURCES from parent scope

lagom_tool(GenerateCSSEnums                SOURCES GenerateCSSEnums.cpp LIBS LibMain)
lagom_tool(GenerateCSSMediaFeatureID       SOURCES GenerateCSSMediaFeatureID.cpp LIBS LibMain)
lagom_tool(GenerateCSSPropertyID           SOURCES GenerateCSSPropertyID.cpp LIBS LibMain)
lagom_tool(GenerateCSSTransformFunctions   SOURCES GenerateCSSTransformFunctions.cpp LIBS LibMain)
lagom_tool(GenerateCSSValueID              SOURCES GenerateCSSValueID.cpp LIBS LibMain)
lagom_tool(GenerateWindowOrWorkerInterfaces SOURCES GenerateWindowOrWorkerInterfaces.cpp LIBS LibMain LibIDL)

add_subdirectory(BindingsGenerator)
