package com.sheempower.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class TouchCropView extends View {

    public enum Handle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT, MOVE
    }

    private Bitmap bitmap;
    private final Matrix imageMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private final RectF cropRect = new RectF();
    private final RectF imageBounds = new RectF();

    private Handle activeHandle = Handle.NONE;
    private final PointF lastTouch = new PointF();

    private Paint dimPaint;
    private Paint borderPaint;
    private Paint handlePaint;
    private Paint gridPaint;

    private float handleRadius;
    private float minCropSize;

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;

    public TouchCropView(Context context) {
        super(context);
        init(context);
    }

    public TouchCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float density = getResources().getDisplayMetrics().density;
        handleRadius = 24 * density;
        minCropSize = 60 * density;

        dimPaint = new Paint();
        dimPaint.setColor(Color.parseColor("#A0000000"));
        dimPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(2.5f * density);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);

        handlePaint = new Paint();
        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#80FFFFFF"));
        gridPaint.setStrokeWidth(1.0f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                scaleFactor *= scale;
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 4.0f));

                PointF center = new PointF(cropRect.centerX(), cropRect.centerY());
                imageMatrix.postScale(scale, scale, center.x, center.y);
                invalidate();
                return true;
            }
        });
    }

    public void setImageBitmap(Bitmap bmp) {
        this.bitmap = bmp;
        this.scaleFactor = 1.0f;
        this.activeHandle = Handle.NONE;

        if (getWidth() > 0 && getHeight() > 0) {
            setupImageAndCropRect();
        } else {
            post(this::setupImageAndCropRect);
        }
    }

    private void setupImageAndCropRect() {
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;

        int viewW = getWidth();
        int viewH = getHeight();
        int bmpW = bitmap.getWidth();
        int bmpH = bitmap.getHeight();

        float scale = Math.min((float) viewW / bmpW, (float) viewH / bmpH) * 0.85f;
        float dx = (viewW - bmpW * scale) / 2f;
        float dy = (viewH - bmpH * scale) / 2f;

        imageMatrix.reset();
        imageMatrix.postScale(scale, scale);
        imageMatrix.postTranslate(dx, dy);

        imageBounds.set(0, 0, bmpW, bmpH);
        imageMatrix.mapRect(imageBounds);

        // Initial crop rect = 80% of mapped image bounds
        float marginX = imageBounds.width() * 0.1f;
        float marginY = imageBounds.height() * 0.1f;
        cropRect.set(
                imageBounds.left + marginX,
                imageBounds.top + marginY,
                imageBounds.right - marginX,
                imageBounds.bottom - marginY
        );

        invalidate();
    }

    public void rotate90(boolean clockwise) {
        if (bitmap == null) return;
        Matrix rot = new Matrix();
        rot.postRotate(clockwise ? 90 : -90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rot, true);
        setupImageAndCropRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;

        // Draw image
        canvas.drawBitmap(bitmap, imageMatrix, null);

        // Draw dimmed outer mask
        canvas.drawRect(0, 0, getWidth(), cropRect.top, dimPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, dimPaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), dimPaint);

        // Draw crop border
        canvas.drawRect(cropRect, borderPaint);

        // Draw 3x3 grid lines
        float thirdW = cropRect.width() / 3f;
        float thirdH = cropRect.height() / 3f;
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left + thirdW * 2, cropRect.top, cropRect.left + thirdW * 2, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + thirdH * 2, cropRect.right, cropRect.top + thirdH * 2, gridPaint);

        // Draw Corner and Edge Handles (WhatsApp Style thick handles)
        float hLength = handleRadius * 0.8f;
        float hThickness = borderPaint.getStrokeWidth() * 1.8f;
        Paint hLinePaint = new Paint(borderPaint);
        hLinePaint.setStrokeWidth(hThickness);

        // Top-Left Corner
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + hLength, cropRect.top, hLinePaint);
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + hLength, hLinePaint);

        // Top-Right Corner
        canvas.drawLine(cropRect.right - hLength, cropRect.top, cropRect.right, cropRect.top, hLinePaint);
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + hLength, hLinePaint);

        // Bottom-Left Corner
        canvas.drawLine(cropRect.left, cropRect.bottom - hLength, cropRect.left, cropRect.bottom, hLinePaint);
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + hLength, cropRect.bottom, hLinePaint);

        // Bottom-Right Corner
        canvas.drawLine(cropRect.right - hLength, cropRect.bottom, cropRect.right, cropRect.bottom, hLinePaint);
        canvas.drawLine(cropRect.right, cropRect.bottom - hLength, cropRect.right, cropRect.bottom, hLinePaint);

        // Edge Handles
        float cX = cropRect.centerX();
        float cY = cropRect.centerY();
        canvas.drawCircle(cX, cropRect.top, hThickness * 1.2f, handlePaint);
        canvas.drawCircle(cX, cropRect.bottom, hThickness * 1.2f, handlePaint);
        canvas.drawCircle(cropRect.left, cY, hThickness * 1.2f, handlePaint);
        canvas.drawCircle(cropRect.right, cY, hThickness * 1.2f, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) return false;

        scaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = getHitHandle(x, y);
                lastTouch.set(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle != Handle.NONE && !scaleDetector.isInProgress()) {
                    float dx = x - lastTouch.x;
                    float dy = y - lastTouch.y;
                    moveHandle(dx, dy);
                    lastTouch.set(x, y);
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeHandle = Handle.NONE;
                break;
        }

        return super.onTouchEvent(event);
    }

    private Handle getHitHandle(float x, float y) {
        float r = handleRadius * 1.2f;

        // Corners
        if (Math.hypot(x - cropRect.left, y - cropRect.top) <= r) return Handle.TOP_LEFT;
        if (Math.hypot(x - cropRect.right, y - cropRect.top) <= r) return Handle.TOP_RIGHT;
        if (Math.hypot(x - cropRect.left, y - cropRect.bottom) <= r) return Handle.BOTTOM_LEFT;
        if (Math.hypot(x - cropRect.right, y - cropRect.bottom) <= r) return Handle.BOTTOM_RIGHT;

        // Edges
        float cX = cropRect.centerX();
        float cY = cropRect.centerY();

        if (Math.hypot(x - cX, y - cropRect.top) <= r || (Math.abs(y - cropRect.top) <= r && x >= cropRect.left && x <= cropRect.right)) return Handle.TOP;
        if (Math.hypot(x - cX, y - cropRect.bottom) <= r || (Math.abs(y - cropRect.bottom) <= r && x >= cropRect.left && x <= cropRect.right)) return Handle.BOTTOM;
        if (Math.hypot(x - cropRect.left, y - cY) <= r || (Math.abs(x - cropRect.left) <= r && y >= cropRect.top && y <= cropRect.bottom)) return Handle.LEFT;
        if (Math.hypot(x - cropRect.right, y - cY) <= r || (Math.abs(x - cropRect.right) <= r && y >= cropRect.top && y <= cropRect.bottom)) return Handle.RIGHT;

        if (cropRect.contains(x, y)) return Handle.MOVE;

        return Handle.NONE;
    }

    private void moveHandle(float dx, float dy) {
        float viewW = getWidth();
        float viewH = getHeight();

        switch (activeHandle) {
            case TOP_LEFT:
                cropRect.left = Math.min(cropRect.right - minCropSize, Math.max(0, cropRect.left + dx));
                cropRect.top = Math.min(cropRect.bottom - minCropSize, Math.max(0, cropRect.top + dy));
                break;

            case TOP_RIGHT:
                cropRect.right = Math.max(cropRect.left + minCropSize, Math.min(viewW, cropRect.right + dx));
                cropRect.top = Math.min(cropRect.bottom - minCropSize, Math.max(0, cropRect.top + dy));
                break;

            case BOTTOM_LEFT:
                cropRect.left = Math.min(cropRect.right - minCropSize, Math.max(0, cropRect.left + dx));
                cropRect.bottom = Math.max(cropRect.top + minCropSize, Math.min(viewH, cropRect.bottom + dy));
                break;

            case BOTTOM_RIGHT:
                cropRect.right = Math.max(cropRect.left + minCropSize, Math.min(viewW, cropRect.right + dx));
                cropRect.bottom = Math.max(cropRect.top + minCropSize, Math.min(viewH, cropRect.bottom + dy));
                break;

            case TOP:
                cropRect.top = Math.min(cropRect.bottom - minCropSize, Math.max(0, cropRect.top + dy));
                break;

            case BOTTOM:
                cropRect.bottom = Math.max(cropRect.top + minCropSize, Math.min(viewH, cropRect.bottom + dy));
                break;

            case LEFT:
                cropRect.left = Math.min(cropRect.right - minCropSize, Math.max(0, cropRect.left + dx));
                break;

            case RIGHT:
                cropRect.right = Math.max(cropRect.left + minCropSize, Math.min(viewW, cropRect.right + dx));
                break;

            case MOVE:
                float width = cropRect.width();
                float height = cropRect.height();
                cropRect.left = Math.max(0, Math.min(viewW - width, cropRect.left + dx));
                cropRect.top = Math.max(0, Math.min(viewH - height, cropRect.top + dy));
                cropRect.right = cropRect.left + width;
                cropRect.bottom = cropRect.top + height;
                break;
        }
    }

    public Bitmap getCroppedBitmap() {
        if (bitmap == null || cropRect.isEmpty()) return null;

        imageMatrix.invert(inverseMatrix);

        RectF bmpCropRect = new RectF();
        inverseMatrix.mapRect(bmpCropRect, cropRect);

        int x = Math.max(0, (int) bmpCropRect.left);
        int y = Math.max(0, (int) bmpCropRect.top);
        int width = Math.min(bitmap.getWidth() - x, (int) bmpCropRect.width());
        int height = Math.min(bitmap.getHeight() - y, (int) bmpCropRect.height());

        if (width <= 0 || height <= 0) return bitmap;

        try {
            return Bitmap.createBitmap(bitmap, x, y, width, height);
        } catch (Throwable t) {
            return bitmap;
        }
    }
}