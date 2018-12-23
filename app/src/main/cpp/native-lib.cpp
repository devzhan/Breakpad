#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_android_zone_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
/**
 * 引起 crash
 */
void Crash() {
    volatile int *a = (int *) (NULL);
    *a = 1;
}
extern "C"
JNIEXPORT void JNICALL Java_com_android_zone_MainActivity_nativeCrash
        (JNIEnv *, jobject){
    Crash();

}