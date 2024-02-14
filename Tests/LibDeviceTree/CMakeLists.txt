set(TEST_SOURCES
    TestLookup.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibDeviceTree LIBS LibDeviceTree LibFileSystem)
endforeach()

install(FILES dtb.dtb DESTINATION usr/Tests/LibDeviceTree)
