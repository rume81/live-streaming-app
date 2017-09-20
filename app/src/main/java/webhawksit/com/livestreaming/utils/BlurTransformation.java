package webhawksit.com.livestreaming.utils;

import android.graphics.Bitmap;

import com.commit451.nativestackblur.NativeStackBlur;
import com.squareup.picasso.Transformation;

/**
 * Example usage with Picasso
 */
public class BlurTransformation implements Transformation {

    private int mBlurRadius;

    public BlurTransformation(int blurRadius) {
        mBlurRadius = blurRadius;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap bm = NativeStackBlur.process(source, mBlurRadius);
        source.recycle();
        return bm;
    }

    @Override
    public String key() {
        return getClass().getCanonicalName() + "-" + mBlurRadius;
    }
}
