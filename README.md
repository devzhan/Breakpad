# 1.概述
最近在学习[极客时间Android开发高手课](https://time.geekbang.org/column/article/70602)老师推荐了Breakpad开源库来采集native 的crash日志，自己参照老师的讲解和[Demo](https://github.com/AndroidAdvanceWithGeektime/Chapter01)做了个封装库[Android_Breakpad](https://github.com/devzhan/Breakpad)，方便以后使用。该库主要是在发生native crash的时候生成文件保存到sd卡，然后分析文件，定位问题。
在我们开发过程中Android JNI层Crash问题是个比较头疼的问题。
相对Java层来说，由于c/c++造成的crash没有输出如同Java的Exception Strace，所以定位问题是个比较艰难的事情。
[Google Breakpad](https://chromium.googlesource.com/breakpad/breakpad)是一套完整的工具集，从crash的捕获到crash的dump，都提供了相对应的工具。
 Breakpad是一个库和工具套件可以让你发布的应用程序（把编译器提供的调试信息剥离掉的）给用户，记录了崩溃紧凑的“dump”文件，发送回您的服务器，并从这些minidump产生C和C++堆栈踪迹。Breakpad可以根据请求使没有崩溃的程序也可以写出minidump
Breakpad有三个主要组件：
![图片来源网络，侵删](https://upload-images.jianshu.io/upload_images/1594504-2b0f3afa713ca482.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
1. 客户端：是一个库，包含在您的应用程序中。 它可以获取当前线程的状态和当前加载的可执行文件和共享库的ID写转储文件。您可以配置客户端发生了崩溃时写入一个minidump时，或明确要求时。
2. 符号卸载器：是一个程序，读取由编译器产生的调试信息，并生成一个使用Breakpad格式的符号文件   。
3.  处理器（minidump processor）：是一个程序，读取一个minidump文件，找到相应的版本的符号文件的（可执行文件和共享库的转储提到的），并产生了一个人可读的C / C + +堆栈跟踪。
Breakpad是一个跨平台的开源库，我们也可以在其 [Breakpad Github](https://github.com/google/breakpad)上下载自己编译。接下来就是讲解如何自己下载编译，引用到项目中，方便以后的项目中使用。
#2. 源码下载和编译
如上文所说，我们从github上的地址自行下载下来源码。具体编译过程可以参照其[README](https://github.com/google/breakpad/blob/master/README.md)文件。整个过程是在mac下进行，其他平台可能稍微时间，但也是可以的。在这个仓库中有一个关于android平台如何引用的说明可以参照
[Android_README](https://github.com/google/breakpad/blob/master/README.ANDROID)
其中主要是要注意“Android.mk”文件依赖关系。我采用的是“CMakeLists.txt”和“Android.mk”大同小异，只是语法问题，具体可以参照[NDK](https://developer.android.google.cn/ndk/)。
整个编译过程相对来说算复杂的，因为中间涉及到很多知识点需要去掌握和查看，特别是对jni开发掌握不够的程序员，不过也可以趁机了解学习一下jni相关知识。

#3.项目结构
整个项目如下图所示：
![breakpad.png](https://upload-images.jianshu.io/upload_images/1594504-e8a36a750c98b33e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
可以看出是一个普通项目和library。我们先看libary：其gradle文件如下
```
apply plugin: 'com.android.library'
android {
    compileSdkVersion 28
    defaultConfig {
        minSdkVersion 14
        targetSdkVersion 28
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86"
        }
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }

}
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.android.support:appcompat-v7:28.0.0'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'com.android.support.test:runner:1.0.2'
    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
}
```
可以从中看到```src/main/cpp/CMakeLists.txt```指向编译配置文件，查看该文件
```
cmake_minimum_required(VERSION 3.4.1)
project(breakpad-core)

set(ENABLE_INPROCESS ON)
set(ENABLE_OUTOFPROCESS ON)
set(ENABLE_LIBCORKSCREW ON)
set(ENABLE_LIBUNWIND ON)
set(ENABLE_LIBUNWINDSTACK ON)
set(ENABLE_CXXABI ON)
set(ENABLE_STACKSCAN ON)
if (${ENABLE_INPROCESS})
    add_definitions(-DENABLE_INPROCESS)
endif ()
if (${ENABLE_OUTOFPROCESS})
    add_definitions(-DENABLE_OUTOFPROCESS)
endif ()
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=implicit-function-declaration")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 ")

# breakpad
include_directories(external/libbreakpad/src external/libbreakpad/src/common/android/include)
add_subdirectory(external/libbreakpad)
list(APPEND LINK_LIBRARIES breakpad)
add_library(breakpad-core SHARED
        breakpad.cpp)
target_link_libraries(breakpad-core ${LINK_LIBRARIES}
        log)
```
关于cmake相关的语法可以网上查得。对于只想使用该项目的来说指导这里就基本上可以了解到整个项目的流程了，其他的不做深入讲解。
#4. 使用在项目中应用该libary或者引用该libary打包出来的aar即可，进行初始化就行。如下：
```
apply plugin: 'com.android.application'
android {
    compileSdkVersion 28
    defaultConfig {
        applicationId "com.android.zone"
        minSdkVersion 21
        targetSdkVersion 28
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags "-std=c++11"
            }
        }
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86"
        }
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    externalNativeBuild {
        cmake {
            path "CMakeLists.txt"
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.android.support:appcompat-v7:28.0.0'
    implementation 'com.android.support.constraint:constraint-layout:1.1.3'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'com.android.support.test:runner:1.0.2'
    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
    implementation project(":breakpad-build")
}
```
 在application中初始化就行
``` 
BreakpadInit.initBreakpad(externalReportPath.getAbsolutePath());
```
测试的代码具体可以查看github上的项目[Android_Breakpad](https://github.com/devzhan/Breakpad)。
生成crash之后倒出对应的dump文件，采用上文 [Breakpad Github](https://github.com/google/breakpad)编译出的工具来解析该文件。
也就是本文tools下的工具，不同平台需要自行编译。
``` 
./tools/mac/minidump_stackwalk crashDump/***.dmp >1.txt 
```
得出日志结果过长，大体如下：
```
Operating system: Android
                  0.0.0 Linux 3.10.73-gd7193540482 #1 SMP PREEMPT Wed Dec 6 22:19:12 UTC 2017 aarch64
CPU: arm64
     8 CPUs
GPU: UNKNOWN
Crash reason:  SIGSEGV /SEGV_MAPERR
Crash address: 0x0
Process uptime: not available
Thread 0 (crashed)
 0  libnative-lib.so + 0x6fe0
     x0 = 0x0000007e4d8cb1c0    x1 = 0x0000007fd17ceb14
     x2 = 0x0000007fd17ceba0    x3 = 0x0000007e4d43ea74
     x4 = 0x0000007fd17ceda0    x5 = 0x0000007ecef7aa58
     x6 = 0x0000007fd17ce990    x7 = 0x0000007e4dcbe34c
     x8 = 0x0000000000000001    x9 = 0x0000000000000000
    x10 = 0x0000000000430000   x11 = 0x0000007e4d7d97a8
    x12 = 0x0000007ed1f3f790   x13 = 0x697871c6dea5b553
    x14 = 0x0000007ed2036000   
······
```
根据文章[Android 平台 Native 代码的崩溃捕获机制及实现
](https://mp.weixin.qq.com/s/g-WzYF3wWAljok1XjPoo7w?)的介绍，我们可知“Crash reason:  SIGSEGV /SEGV_MAPERR
”代表哪种类型的错误：
  ```
SIGSEGV 是当一个进程执行了一个无效的内存引用，或发生段错误时发送给它的信号。
Thread 0 (crashed) //crash 发生时候的线程
  0  libnative-lib.so + 0x6fe0 //发生 crash 的位置和寄存器信息
```
有了具体的寄存器信息，我们进行符号解析：
符号解析，可以使用 ndk 中提供的addr2line来根据地址进行一个符号反解的过程,该工具在
``` $NDK_HOME/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-addr2line```
输出结果如下：
 ```
arm-linux-androideabi-addr2line -f -C -e sample/build/intermediates/transforms/mergeJniLibs/debug/0/lib/armeabi-v7a/libcrash-lib.so 0x77e                           
//输出结果如下
Crash()
```
得出该崩溃为Crash方法所致，接下来就需要去修改该方法是后话了。
以上是一次学习的总结，结合相关demo，自己动手封装出一个工作中可以用到的公共库。
纸上得来终觉浅，绝知此事要躬行。
欢迎关注我的[简书](https://time.geekbang.org/column/article/70602)
参考内容：
·https://time.geekbang.org/column/article/70602
·https://github.com/AndroidAdvanceWithGeektime/Chapter01
·https://github.com/google/breakpad



