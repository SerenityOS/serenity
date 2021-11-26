
set(SOURCES
  Validation.cpp
)

serenity_lib(LibDeviceTree DeviceTree)
target_link_libraries(LibDeviceTree LibC LibCore)
