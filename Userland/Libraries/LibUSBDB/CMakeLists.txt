set(SOURCES
    Database.cpp
)

serenity_lib(LibUSBDB usbdb)
target_link_libraries(LibUSBDB LibC)
