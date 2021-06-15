file(GLOB TEST_SOURCES  CONFIGURE_DEPENDS "*.cpp")

foreach(source ${TEST_SOURCES})
    serenity_test(${source} LibTLS LIBS LibTLS LibCrypto)
    get_filename_component(test_name ${source} NAME_WE)
endforeach()


