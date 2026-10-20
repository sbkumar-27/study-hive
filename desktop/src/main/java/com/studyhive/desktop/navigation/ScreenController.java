package com.studyhive.desktop.navigation;

/** Implemented by any screen controller that needs navigation arguments or a refresh hook. */
public interface ScreenController {

    default void onShow(NavParams params) {
    }
}
