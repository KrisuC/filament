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

package com.google.android.filament.gltfio;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

/**
 * Helper that enables camera interaction similar to sketchfab or Google Maps.
 *
 * Clients notify the camera manipulator of various mouse or touch events, then periodically call
 * its getLookAt() method so that they can adjust their camera(s). Two modes are supported: ORBIT
 * and MAP. To construct a manipulator instance, the desired mode is passed into the create method.
 *
 * @see Bookmark
 */
public class Manipulator {
    private long mNativeObject;

    private Manipulator(long nativeIndexBuffer) {
        mNativeObject = nativeIndexBuffer;
    }

    public enum Mode { ORBIT, MAP };

    public enum Fov { VERTICAL, HORIZONTAL };

    public static class Builder {
        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
        // Keep to finalize native resources
        private final BuilderFinalizer mFinalizer;
        private final long mNativeBuilder;

        public Builder() {
            mNativeBuilder = nCreateBuilder();
            mFinalizer = new BuilderFinalizer(mNativeBuilder);
        }

        /**
         * Width and height of the viewing area.
         *
         * @return this <code>Builder</code> object for chaining calls
         */
        @NonNull
        public Builder viewport(@IntRange(from = 1) int width, @IntRange(from = 1) int height) {
            nBuilderViewport(mNativeBuilder, width, height);
            return this;
        }

        // public float targetPosition[] = new float[] {0, 0, 0}; //! World-space position of interest
        // public float upVector[] = new float[] {0, 1, 0};       //! Orientation for the home position
        // public float zoomSpeed = 0.01f;                        //! Multiplied with scroll delta
        // public float orbitHomePosition[] = new float[] {0, 0, 1}; //! Initial eye position in world space
        // public float orbitSpeed[] = new float[] {0.01f, 0.01f};   //! Multiplied with viewport delta
        // public Fov fovDirection = Fov.VERTICAL;           //! The FOV axis that's held constant when the viewport changes
        // public float fovDegrees = 33;                     //! The full FOV (not the half-angle)
        // public float farPlane = 5000;                     //! The distance to the far plane
        // public float mapExtent[] = new float[]{512, 512}; //! The ground plane size used to compute the home position
        // public float mapMinDistance = 0.0f;               //! Constrains the zoom-in level
        // public float groundPlane[] = new float[] {0, 0, 1, 0};

        /**
         * Creates and returns the <code>Manipulator</code> object.
         *
         * @return the newly created <code>Manipulator</code> object
         *
         * @exception IllegalStateException if the Manipulator could not be created
         *
         */
        @NonNull
        public Manipulator build(Mode mode) {
            long nativeManipulator = nBuilderBuild(mNativeBuilder, mode.ordinal());
            if (nativeManipulator == 0)
                throw new IllegalStateException("Couldn't create Manipulator");
            return new Manipulator(nativeManipulator);
        }

        private static class BuilderFinalizer {
            private final long mNativeObject;

            BuilderFinalizer(long nativeObject) {
                mNativeObject = nativeObject;
            }

            @Override
            public void finalize() {
                try {
                    super.finalize();
                } catch (Throwable t) { // Ignore
                } finally {
                    nDestroyBuilder(mNativeObject);
                }
            }
        }
    };

    @Override
    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable t) { // Ignore
        } finally {
            nDestroyManipulator(mNativeObject);
        }
    }

    /**
     * Gets the immutable mode of the manipulator.
     */
    public Mode getMode() { return Mode.values()[nGetMode(mNativeObject)]; }

    /**
     * Sets the viewport dimensions. The manipulator uses this processing grab events and raycasts.
     */
    void setViewport(int width, int height) {
        nSetViewport(mNativeObject, width, height);
    }

    /**
     * Gets the current orthonormal basis. This is usually called once per frame.
     */
    public void getLookAt(
            @NonNull @Size(min = 3) float[] eyePosition,
            @NonNull @Size(min = 3) float[] targetPosition,
            @NonNull @Size(min = 3) float[] upward) {
        nGetLookAt(mNativeObject, eyePosition, targetPosition, upward);
    }

    /**
     * Given a viewport coordinate, picks a point in the ground plane.
     */
    @Nullable @Size(min = 3)
    public float[] raycast(int x, int y) {
        float[] result = new float[3];
        nRaycast(mNativeObject, x, y, result);
        return result;
    }

    /**
     * Starts a grabbing session (i.e. the user is dragging around in the viewport).
     *
     * This starts a panning session in MAP mode, and start either rotating or strafing in ORBIT.
     *
     * @param x X-coordinate for point of interest in viewport space
     * @param y Y-coordinate for point of interest in viewport space
     * @param strafe ORBIT mode only; if true, starts a translation rather than a rotation.
     */
    public void grabBegin(int x, int y, Boolean strafe) {
        nGrabBegin(mNativeObject, x, y, strafe);
    }

    /**
     * Updates a grabbing session.
     *
     * This must be called at least once between grabBegin / grabEnd to dirty the camera.
     */
    public void grabUpdate(int x, int y) {
        nGrabUpdate(mNativeObject, x, y);
    }

    /**
     * Ends a grabbing session.
     */
    public void grabEnd(int x, int y) {
        nGrabEnd(mNativeObject, x, y);
    }

    /**
     * Dollys the camera along the viewing direction.
     *
     * @param x X-coordinate for point of interest in viewport space
     * @param y Y-coordinate for point of interest in viewport space
     * @param scrolldelta Positive means "zoom in", negative means "zoom out"
     */
    public void zoom(int x, int y, float scrolldelta) {
        nZoom(mNativeObject, x, y, scrolldelta);
    }

    /**
     * Gets a handle that can be used to reset the manipulator back to its current position.
     *
     * \see jumpToBookmark
     */
    public Bookmark getCurrentBookmark() {
        return new Bookmark(nGetCurrentBookmark(mNativeObject));
    }

    /**
     * Gets a handle that can be used to reset the manipulator back to its home position.
     *
     * see jumpToBookmark
     */
    public Bookmark getHomeBookmark() {
        return new Bookmark(nGetHomeBookmark(mNativeObject));
    }

    /**
     * Sets the manipulator position and orientation back to a stashed state.
     *
     * \see getCurrentBookmark, getHomeBookmark
     */
    public void jumpToBookmark(Bookmark bookmark) {
        nJumpToBookmark(mNativeObject, bookmark.getNativeObject());
    }

    private static native long nCreateBuilder();
    private static native void nDestroyBuilder(long nativeBuilder);
    private static native void nBuilderViewport(long nativeBuilder, int width, int height);
    private static native long nBuilderBuild(long nativeBuilder, int mode);

    private static native void nDestroyManipulator(long nativeManip);
    private static native int nGetMode(long nativeManip);
    private static native void nSetViewport(long nativeManip, int width, int height);
    private static native void nGetLookAt(long nativeManip, float[] eyePosition, float[] targetPosition, float[] upward);
    private static native void nRaycast(long nativeManip, int x, int y, float[] result);
    private static native void nGrabBegin(long nativeManip, int x, int y, Boolean strafe);
    private static native void nGrabUpdate(long nativeManip, int x, int y);
    private static native void nGrabEnd(long nativeManip, int x, int y);
    private static native void nZoom(long nativeManip, int x, int y, float scrolldelta);
    private static native long nGetCurrentBookmark(long nativeManip);
    private static native long nGetHomeBookmark(long nativeManip);
    private static native void nJumpToBookmark(long nativeManip, long nativeBookmark);
}
