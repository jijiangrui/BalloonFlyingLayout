package com.dongao.balloonflyinglayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * @author jjr
 * @date 2019-04-21
 */
public class BalloonFlyingLayout extends RelativeLayout {

    private Context context;
    private Drawable[] icons;
    //插值器
    private Interpolator[] interpolators = new Interpolator[4];
    private BlockingQueue<ImageView> mImageViewBlockingQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Animator> mAnimatorBlockingQueue = new LinkedBlockingQueue<>();
    private int mWidth;
    private int mHeight;

    public BalloonFlyingLayout(Context context) {
        this(context, null);
    }

    public BalloonFlyingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    /**
     * 1、可在资源文件中定义图片资源：
     *     <string-array name="balloon_flying_resource_ids">
     *         <item>@mipmap/heart0</item>
     *         <item>@mipmap/heart1</item>
     *         <item>@mipmap/heart2</item>
     *         <item>@mipmap/heart3</item>
     *         <item>@mipmap/heart4</item>
     *         <item>@mipmap/heart5</item>
     *     </string-array>
     *  2、也可调用 {@link #setIcons(Drawable[] icons)}
     *  或{@link #setIconsResourceIds(int[] resIds)}设置图片资源。
     */
    private void init() {
        //获取图片资源
        TypedArray arrays = null;
        try {
            arrays = context.getResources().obtainTypedArray(R.array.balloon_flying_resource_ids);
            int length = arrays.length();
            icons = new Drawable[length];
            for (int i = 0; i < length; i++) {
                icons[i]  = ContextCompat.getDrawable(context, arrays.getResourceId(i, 0));
            }
        } catch (Exception e) {

        } finally {
            if (arrays != null)
                arrays.recycle();
        }

        // 在动画开始与结束的地方速率改变比较慢，在中间的时候加速
        interpolators[0] = new AccelerateDecelerateInterpolator();
        // 在动画开始的地方速率改变比较慢，然后开始加速
        interpolators[1] = new AccelerateInterpolator();
        // 在动画开始的地方快然后慢
        interpolators[2] = new DecelerateInterpolator();
        // 以常量速率改变
        interpolators[3] = new LinearInterpolator();
    }

    /**
     * 设置图片资源数组
     */
    public void setIcons(Drawable[] icons) {
        this.icons = icons;
    }

    /**
     * 设置图片资源Id数组
     */
    public void setIconsResourceIds(int[] resIds) {
        int length = resIds.length;
        icons = new Drawable[length];
        for (int i = 0; i < length; i++) {
            icons[i]  = ContextCompat.getDrawable(context, resIds[i]);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mImageViewBlockingQueue != null) {
            mImageViewBlockingQueue.clear();
        }
        if (mAnimatorBlockingQueue != null) {
            mAnimatorBlockingQueue.clear();
        }
    }

    /**
     * 显示一个漂浮上升的View
     */
    public void addFlyingView() {
        if (icons == null || icons.length == 0) {
            return;
        }
        ImageView iv = null;
        LayoutParams params = null;
        Drawable drawable = icons[new Random().nextInt(icons.length)];
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicWidth();
        if (mImageViewBlockingQueue != null) {
            iv = mImageViewBlockingQueue.poll();
            if (iv != null) {
                params = (LayoutParams) iv.getLayoutParams();
                params.width = width;
                params.height = height;
                iv.setLeft(0);
                iv.setTop(0);
                iv.setRight(0);
                iv.setBottom(0);
                iv.setX(0);
                iv.setY(0);
            }
        }
        if (iv == null) {
            iv = new ImageView(context);
            params = new LayoutParams(width, height);
        }
        params.addRule(CENTER_HORIZONTAL, TRUE);
        params.addRule(ALIGN_PARENT_BOTTOM, TRUE);
        iv.setLayoutParams(params);
        iv.setImageDrawable(drawable);

        final ImageView finalIv = iv;

        AnimatorSet set = getAnimatorSet(iv);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                super.onAnimationStart(animator);
                addView(finalIv);
                putAnimator2Queue(animator);
            }
            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                mAnimatorBlockingQueue.remove(animator);
                removeView(finalIv);
                putView2Queue(finalIv);
            }
        });
        set.start();
    }

    private void putAnimator2Queue(Animator animator) {
        try {
            mAnimatorBlockingQueue.put(animator);
        } catch (InterruptedException e) {

        }
    }

    private void putView2Queue(ImageView view) {
        try {
            mImageViewBlockingQueue.put(view);
        } catch (InterruptedException e) {

        }
    }

    private AnimatorSet getAnimatorSet(ImageView iv) {
        // 1.alpha动画
        ObjectAnimator alpha = ObjectAnimator.ofFloat(iv, "alpha", 0.3f, 1f);
        // 2.缩放动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv, "scaleX", 0.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv, "scaleY", 0.2f, 1f);

        // 动画集合
        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.setDuration(500);

        // 贝塞尔曲线动画
        ValueAnimator bzier = getBzierAnimator(iv);

        AnimatorSet set2 = new AnimatorSet();
        set2.playSequentially(set, bzier);
        set2.setTarget(iv);
        return set2;
    }

    /**
     * 贝塞尔动画
     */
    private ValueAnimator getBzierAnimator(final ImageView iv) {
        //4个点的坐标
        PointF[] PointFs = getPointFs(iv);
        BezierEvaluator evaluator = new BezierEvaluator(PointFs[1], PointFs[2]);
        ValueAnimator valueAnim = ValueAnimator.ofObject(evaluator, PointFs[0], PointFs[3]);
        valueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF p = (PointF) animation.getAnimatedValue();
                iv.setX(p.x);
                iv.setY(p.y);
                iv.setAlpha(1- animation.getAnimatedFraction());
            }
        });
        valueAnim.setTarget(iv);
        valueAnim.setDuration(3000);
        valueAnim.setInterpolator(interpolators[new Random().nextInt(4)]);
        return valueAnim;
    }

    private PointF[] getPointFs(ImageView iv) {
        LayoutParams params = (LayoutParams) iv.getLayoutParams();
        PointF[] PointFs = new PointF[4];
        PointFs[0] = new PointF();
        PointFs[0].x = (mWidth- params.width)/ 2;
        PointFs[0].y = mHeight - params.height;

        PointFs[1] = new PointF();
        PointFs[1].x = new Random().nextInt(mWidth);
        PointFs[1].y = new Random().nextInt(mHeight /2) + mHeight / 2 + params.height;

        PointFs[2] = new PointF();
        PointFs[2].x = new Random().nextInt(mWidth);
        PointFs[2].y = new Random().nextInt(mHeight /2);

        PointFs[3] = new PointF();
        PointFs[3].x = new Random().nextInt(mWidth);
        PointFs[3].y = 0;
        return PointFs;
    }

    /**
     * 清除动画及动画中的子控件
     */
    public void clear() {
        while (true) {
            Animator animator = mAnimatorBlockingQueue.poll();
            if (animator == null) {
                break;
            } else {
                animator.cancel();
            }
        }
    }

    class BezierEvaluator implements TypeEvaluator<PointF> {

        private PointF p1;
        private PointF p2;

        public BezierEvaluator(PointF p1, PointF p2) {
            super();
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public PointF evaluate(float fraction, PointF p0, PointF p3) {
            PointF pointf = new PointF();
            // 贝塞尔曲线公式  p0*(1-t)^3 + 3p1*t*(1-t)^2 + 3p2*t^2*(1-t) + p3^3
            pointf.x = p0.x * (1 - fraction) * (1 - fraction) * (1 - fraction)
                    + 3 * p1.x * fraction * (1 - fraction) * (1 - fraction)
                    + 3 * p2.x * fraction * fraction * (1 - fraction)
                    + p3.x * fraction * fraction * fraction;
            pointf.y = p0.y * (1 - fraction) * (1 - fraction) * (1 - fraction)
                    + 3 * p1.y * fraction * (1 - fraction) * (1 - fraction)
                    + 3 * p2.y * fraction * fraction * (1 - fraction)
                    + p3.y * fraction * fraction * fraction;
            return pointf;
        }
    }

}
