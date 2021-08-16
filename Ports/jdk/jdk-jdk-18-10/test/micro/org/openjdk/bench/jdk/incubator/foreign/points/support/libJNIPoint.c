/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <jni.h>
#include <stdlib.h>
#include "jlong.h"
#include <math.h>

#include "points.h"

double distance(Point p1, Point p2) {
    int xDist = abs(p1.x - p2.x);
    int yDist = abs(p1.y - p2.y);
    return sqrt((xDist * xDist) + (yDist * yDist));
}

JNIEXPORT jlong JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_allocate
  (JNIEnv *env, jclass nativePointClass) {
    Point* p = malloc(sizeof *p);
    return ptr_to_jlong(p);
}

JNIEXPORT void JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_free
  (JNIEnv *env, jclass cls, jlong thisPoint) {
    free(jlong_to_ptr(thisPoint));
}

JNIEXPORT jint JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_getX
  (JNIEnv *env, jclass cls, jlong thisPoint) {
    Point* point = jlong_to_ptr(thisPoint);
    return point->x;
}

JNIEXPORT void JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_setX
  (JNIEnv *env, jclass cls, jlong thisPoint, jint value) {
    Point* point = jlong_to_ptr(thisPoint);
    point->x = value;
}

JNIEXPORT jint JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_getY
  (JNIEnv *env, jclass cls, jlong thisPoint) {
    Point* point = jlong_to_ptr(thisPoint);
    return point->y;
}

JNIEXPORT void JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_setY
  (JNIEnv *env, jclass cls, jlong thisPoint, jint value) {
    Point* point = jlong_to_ptr(thisPoint);
    point->y = value;
}

JNIEXPORT jdouble JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_JNIPoint_distance
  (JNIEnv *env, jclass cls, jlong thisPoint, jlong other) {
    Point* p1 = jlong_to_ptr(thisPoint);
    Point* p2 = jlong_to_ptr(other);
    return distance(*p1, *p2);
}

JNIEXPORT jdouble JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_points_support_BBPoint_distance
  (JNIEnv *env, jclass ignored, jobject buffP1, jobject buffP2) {
    Point* p1 = (Point*) (*env)->GetDirectBufferAddress(env, buffP1);
    Point* p2 = (Point*) (*env)->GetDirectBufferAddress(env, buffP2);
    return distance(*p1, *p2);
}
