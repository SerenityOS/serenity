set(SOURCES
    Symbolication.cpp
)

serenity_lib(LibSymbolication symbolication)
target_link_libraries(LibSymbolication LibDebug)
