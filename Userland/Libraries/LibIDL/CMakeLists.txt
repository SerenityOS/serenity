set(SOURCES
    IDLParser.cpp
)

serenity_lib(LibIDL idl)
target_link_libraries(LibIDL LibCore)
