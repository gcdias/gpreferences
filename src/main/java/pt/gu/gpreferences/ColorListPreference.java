package pt.gu.gpreferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * Created by GU on 18-01-2015.
 */
public class ColorListPreference extends ListPreference {

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private boolean mSetSummary;
    private String mValue;
    private int mValueIndex;
    private int entryIndex = -1;
    private Resources mResources;
    private Drawable[] mDrawable;
    private float radBitmap;
    private float fillRatio;
    private BitmapShader shader;

    public ColorListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorListPreference);
        radBitmap = a.getDimension(R.styleable.ColorListPreference_paletteSize, 32);
        fillRatio = a.getFloat(R.styleable.ColorListPreference_paletteFillRatio, 0.8f);
        mSetSummary = a.getBoolean(R.styleable.ColorListPreference_autoSummary, false);
        a.recycle();

        //Fields
        mResources = context.getResources();
        shader = new BitmapShader(BitmapFactory.decodeResource(mResources, R.drawable.checkerboard), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        mEntries = getEntries();
        mEntryValues = getEntryValues();
        mValue = getValue();
        mValueIndex = getValueIndex();

        if (mEntries == null || mEntryValues == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mDrawable = new Drawable[mEntryValues.length];
        Drawable d;

        for (int i = 0; i < mEntryValues.length; i++) {
            d = mResources.getDrawable(R.drawable.prf_colorlist_radiobutton);
            ((LayerDrawable) d).setDrawableByLayerId(
                    R.id.bitmap,
                    new BitmapDrawable(
                            mResources, getBitmapColorIcon(i)));
            mDrawable[i] = d;
        }

        ListAdapter adapter = new ArrayAdapter<CharSequence>(getContext(), R.layout.prf_colorlist, mEntries) {

            ViewHolder holder;
            MyOnClickListener listener = new MyOnClickListener();

            public View getView(int position, View convertView, ViewGroup parent) {

                final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.prf_colorlist, null);
                    holder = new ViewHolder();
                    holder.title = (TextView) convertView.findViewById(R.id.textView);
                    holder.radioBtn = (RadioButton) convertView.findViewById(R.id.radiobutton);
                    convertView.setTag(holder);
                    holder.title.setOnClickListener(listener);
                    holder.radioBtn.setOnClickListener(listener);

                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.title.setText(mEntries[position]);
                holder.title.setTag(position);
                holder.radioBtn.setButtonDrawable(mDrawable[position]);
                holder.radioBtn.setChecked(mValueIndex == position);
                holder.radioBtn.setTag(position);
                return convertView;

            }

            class ViewHolder {
                TextView title;
                RadioButton radioBtn;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                entryIndex = index;
                ColorListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
        });

    /*
     * The typical interaction for list-based dialogs is to have
     * click-on-an-item dismiss the dialog instead of the user having to
     * press 'Ok'.
     */
        builder.setPositiveButton(null, null);
    }

    private Bitmap getBitmapColorIcon(int index) {
        Bitmap b = Bitmap.createBitmap((int) radBitmap, (int) radBitmap, Bitmap.Config.ARGB_8888);
        RectF r = new RectF(radBitmap * (1 - fillRatio), radBitmap * (1 - fillRatio), radBitmap * fillRatio, radBitmap * fillRatio);
        float rad = radBitmap * fillRatio / 2;
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        String[] colors = mEntryValues[index].toString().split(",");
        int len = colors.length;
        p.setShader(shader);
        c.drawArc(r, 0, 360, true, p);
        p.reset();
        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        if (len == 1) {
            p.setColor(Color.parseColor(colors[0]));
            c.drawArc(r, 0, 360, true, p);
        } else {
            float ang = 360 / len;
            //float pad = 6 * (len - 1) / len;
            for (int i = 0; i < len; i++) {
                p.setColor(Color.parseColor(colors[i]));
                c.drawArc(r, i * ang, ang, true, p);
            }
        }
        return b;
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (entryIndex >= 0 && mEntryValues != null) {
            String value = mEntryValues[entryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    protected void setResult() {
        this.getDialog().dismiss();
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int[] getSelectedColors() {
        return null;
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            entryIndex = (Integer) v.getTag();
            ColorListPreference.this.setIcon(mDrawable[entryIndex]);
            if (mSetSummary) {
                ColorListPreference.this.setSummary(mEntries[entryIndex]);
            }
            ColorListPreference.this.setResult();
        }
    }
}

