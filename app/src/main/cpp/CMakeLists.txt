cmake_minimum_required(VERSION 3.4.1)

add_library(main SHARED
            main.c)

target_include_directories(main PRIVATE)

target_link_libraries(main
                      android
                      log)