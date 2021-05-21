set(
  TEST_SOURCES
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreFileWatcher.cpp
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreIODevice.cpp
)

foreach(source ${TEST_SOURCES})
  serenity_test(${source} LibCore)
endforeach()
