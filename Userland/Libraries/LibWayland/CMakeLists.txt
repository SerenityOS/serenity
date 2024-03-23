set(SOURCES
    Connection.cpp
)

serenity_lib(LibWayland wayland)
target_link_libraries(LibWayland PRIVATE LibCore)

set(SOURCES
    Transpiler.cpp
)
serenity_bin(WaylandTranspiler)
target_link_libraries(WaylandTranspiler PRIVATE LibCore LibMain LibXML LibFileSystem)
install(TARGETS ${TARGET_NAME} RUNTIME DESTINATION bin OPTIONAL)
