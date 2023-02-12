
set(SOURCES
  FlattenedDeviceTree.cpp
  Validation.cpp
)

serenity_lib(LibDeviceTree DeviceTree)
target_link_libraries(LibDeviceTree PRIVATE LibCore)
