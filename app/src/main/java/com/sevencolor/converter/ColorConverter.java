package com.sevencolor.converter;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * 七色墨水屏 Floyd-Steinberg 抖动转换器
 * 调色板：白、黑、红、黄、橙、绿、蓝
 */
public class ColorConverter {

    public static final int TARGET_WIDTH = 800;
    public static final int TARGET_HEIGHT = 480;

    // 商家给的 7 色硬件固定色板
    private static final int[][] PALETTE = {
        {255, 255, 255},  // 0 白
        {0,   0,   0  },  // 1 黑
        {220, 20,  60 },  // 2 红
        {255, 210, 0  },  // 3 黄
        {255, 110, 0  },  // 4 橙
        {0,   155, 70 },  // 5 绿
        {0,   65,  175},  // 6 蓝
    };

    /**
     * 将 Bitmap 转换为七色抖动图
     * @param src 原图
     * @return 转换后的 800x480 Bitmap，像素颜色为最近的调色板色
     */
    public static Bitmap convert(Bitmap src) {
        // 缩放到目标尺寸
        Bitmap scaled = Bitmap.createScaledBitmap(src, TARGET_WIDTH, TARGET_HEIGHT, true);

        int w = TARGET_WIDTH;
        int h = TARGET_HEIGHT;

        // 读取像素到 float 数组（用于误差累积）
        float[][] rBuf = new float[h][w];
        float[][] gBuf = new float[h][w];
        float[][] bBuf = new float[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = scaled.getPixel(x, y);
                rBuf[y][x] = Color.red(pixel);
                gBuf[y][x] = Color.green(pixel);
                bBuf[y][x] = Color.blue(pixel);
            }
        }

        // 输出 Bitmap
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        // Floyd-Steinberg 误差扩散
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float oldR = rBuf[y][x];
                float oldG = gBuf[y][x];
                float oldB = bBuf[y][x];

                int[] nearest = nearestColor(oldR, oldG, oldB);
                out.setPixel(x, y, Color.rgb(nearest[0], nearest[1], nearest[2]));

                float errR = oldR - nearest[0];
                float errG = oldG - nearest[1];
                float errB = oldB - nearest[2];

                // 右 (7/16)
                diffuse(rBuf, gBuf, bBuf, x + 1, y,     w, h, errR, errG, errB, 7f / 16f);
                // 左下 (3/16)
                diffuse(rBuf, gBuf, bBuf, x - 1, y + 1, w, h, errR, errG, errB, 3f / 16f);
                // 下 (5/16)
                diffuse(rBuf, gBuf, bBuf, x,     y + 1, w, h, errR, errG, errB, 5f / 16f);
                // 右下 (1/16)
                diffuse(rBuf, gBuf, bBuf, x + 1, y + 1, w, h, errR, errG, errB, 1f / 16f);
            }
        }

        scaled.recycle();
        return out;
    }

    private static void diffuse(float[][] rBuf, float[][] gBuf, float[][] bBuf,
                                 int x, int y, int w, int h,
                                 float errR, float errG, float errB, float weight) {
        if (x < 0 || x >= w || y < 0 || y >= h) return;
        rBuf[y][x] += errR * weight;
        gBuf[y][x] += errG * weight;
        bBuf[y][x] += errB * weight;
    }

    /**
     * 找调色板中最近的颜色（欧氏距离）
     */
    private static int[] nearestColor(float r, float g, float b) {
        int bestIdx = 0;
        float bestDist = Float.MAX_VALUE;

        for (int i = 0; i < PALETTE.length; i++) {
            float dr = r - PALETTE[i][0];
            float dg = g - PALETTE[i][1];
            float db = b - PALETTE[i][2];
            float dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        return PALETTE[bestIdx];
    }
}
