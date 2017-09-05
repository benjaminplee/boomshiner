package com.yardspoon.boomshiner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class Images {
    public static void draw(Bitmap bitmap, List<Box> boxes) {
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);

        for (Box box : boxes) {
            paint.setColor(box.colorSignature.getAColor());
            canvas.drawRect(new Rect(box.x1, box.y1, box.x2, box.y2), paint);
        }
    }

    public static void write(Bitmap bitmap, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file.getPath());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Timber.e(e,"Couldn't write out bitmap");
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Timber.e(e, "Couldn't close file stream for bitmap");
            }
        }
    }

    public static void draw(Bitmap bitmap, Box startingPoint, Vector vector, int color) {
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(color);
        paint.setAntiAlias(false);

        int x1 = startingPoint.cx;
        int y1 = startingPoint.cy;
        int x2 = x1 + vector.deltaX;
        int y2 = y1 + vector.deltaY;

        Timber.v("Images line: (%s,%s) to (%s,%s)", x1, y1, x2, y2);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    public static Bitmap getBitmap(File file) {
        Timber.d("decoding image");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inScaled = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);
    }
}
