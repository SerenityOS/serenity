set(SOURCES
    Database.cpp
)

serenity_lib(LibPCIDB pcidb)
target_link_libraries(LibPCIDB LibC)
