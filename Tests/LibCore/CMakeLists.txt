set(
  TEST_SOURCES
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreFileWatcher.cpp
)

foreach(source ${TEST_SOURCES})
  serenity_test(${source} LibCore)
endforeach()
