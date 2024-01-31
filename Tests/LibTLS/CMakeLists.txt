set(TEST_SOURCES
    TestTLSCertificateParser.cpp
    TestTLSHandshake.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibTLS LIBS LibTLS LibCrypto)
endforeach()

foreach(i RANGE 1 100)
    add_executable("TestTLSHandshake${i}" "$<TARGET_OBJECTS:LibTestMain>" TestTLSHandshake.cpp)
    add_dependencies(ComponentTests "TestTLSHandshake${i}")
    set_target_properties("TestTLSHandshake${i}" PROPERTIES EXCLUDE_FROM_ALL TRUE)
    serenity_set_implicit_links("TestTLSHandshake${i}")
    target_link_libraries("TestTLSHandshake${i}" PRIVATE LibTest LibCore LibFileSystem)
    foreach(lib LibTLS LibCrypto)
        target_link_libraries("TestTLSHandshake${i}" PRIVATE ${lib})
    endforeach()
    install(TARGETS "TestTLSHandshake${i}" RUNTIME DESTINATION usr/Tests/LibTLS OPTIONAL)
endforeach()
