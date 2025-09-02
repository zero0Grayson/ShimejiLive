package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.win.NativeFactoryImpl;

import java.awt.image.BufferedImage;

/**
 * 为原生窗口和图像提供实例。
 * 在这个精简版中，它被修改为总是返回 Windows 的实现。
 */
public abstract class NativeFactory {

    private static final NativeFactory instance = new NativeFactoryImpl();

    /**
     * 获取原生工厂的唯一实例。
     * @return 原生工厂实例，这里总是 WindowsNativeFactory。
     */
    public static NativeFactory getInstance() {
        return instance;
    }

    /**
     * 创建一个原生环境的实现。
     * @return 原生环境对象。
     */
    public abstract Environment getEnvironment();

    public abstract NativeImage newNativeImage(BufferedImage src);

    public abstract TranslucentWindow newTransparentWindow();
}
