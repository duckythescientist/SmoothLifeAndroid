package ninja.duck.preference;


import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;

import ninja.duck.smoothlife.R;

/**
 * A {@link SeekBarPreference} that stores its value in a percentual relation (between 0 and 1) of {@link #getMax()}
 */
public class PercentSeekBarPreference extends SeekBarPreference {

    public PercentSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PercentSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PercentSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PercentSeekBarPreference(Context context) {
        super(context);
    }

    private int adaptValue(float value) {
        return Math.round(value * getMax());
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        float val = getPercentValue();
        float persisted = (float)getPersistedFloat(val);
        int adapted = adaptValue(persisted);
        int defaultInt = (Integer) defaultValue;
        setValue(restoreValue ? adapted
                : defaultInt);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        setMax(a.getInt(R.styleable.SeekBarPreference_android_max, 100));
        return adaptValue(a.getFloat(index, 0));
    }

    @Override
    protected boolean persistInt(int value) {
        return persistFloat(getPercentValue());
    }

    /**
     * Returns a <code>float</code> value calculated as <code>({@link #getValue()} - {@link #getMin()}) / ({@link #getMax()} - {@link #getMin()})) </code>
     *
     * @return a percentiual value (between 0 and 1)
     */
    @FloatRange(from = 0, to = 1)
    public float getPercentValue() {
        float min = getMin();
        return (getValue() - min) / (getMax() - min);
    }

    /**
     * Sets the percent value between {@link #getMax()} and {@link #getMin()}
     *
     * @param value
     */
    public void setPercentValue(@FloatRange(from = 0, to = 1) float value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("value must be between 0 and 1");
        }
        setValue(adaptValue(value));
    }

}
