/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>

#include <camutils/Manipulator.h>

using namespace filament::camutils;

using Builder = Manipulator<float>::Config;

extern "C" JNIEXPORT jlong nCreateBuilder(JNIEnv*, jclass) {
    return (jlong) new Builder {};
}

extern "C" JNIEXPORT void nDestroyBuilder(JNIEnv*, jclass, jlong nativeBuilder) {
    Builder* builder = (Builder*) nativeBuilder;
    delete builder;
}

extern "C" JNIEXPORT void nBuilderViewport(JNIEnv*, jclass, jlong nativeBuilder, int width, int height) {
    Builder* builder = (Builder*) nativeBuilder;
    builder->viewport[0] = width;
    builder->viewport[1] = height;
}

extern "C" JNIEXPORT long nBuilderBuild(JNIEnv*, jclass, jlong nativeBuilder, int mode) {
    Builder* builder = (Builder*) nativeBuilder;
    return (jlong) Manipulator<float>::create((Mode) mode, *builder);
}


extern "C" JNIEXPORT void JNICALL
Java_com_google_android_filament_gltfio_Manipulator_nDestroyManipulator(JNIEnv*, jclass, jlong nativeManip) {
    auto manip = (Manipulator<float>*) nativeManip;
    delete manip;
}
