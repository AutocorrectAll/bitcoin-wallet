/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.util.GenericUtils;
import de.schildbach.wallet.util.MonetarySpannable;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

/**
 * @author Andreas Schildbach
 */
public final class CurrencyAmountView extends FrameLayout {
    public interface Listener {
        void changed();

        void focusChanged(final boolean hasFocus);
    }

    private int significantColor, lessSignificantColor, errorColor;
    private Drawable deleteButtonDrawable, contextButtonDrawable;
    private Drawable currencySymbolDrawable;
    private String localCurrencyCode = null;
    private MonetaryFormat inputFormat;
    private Monetary hint = null;
    private MonetaryFormat hintFormat = new MonetaryFormat().noCode();
    private boolean amountSigned = false;
    private boolean validateAmount = true;

    private TextView textView;
    private View contextButton;
    private Listener listener;
    private OnClickListener contextButtonClickListener;

    public CurrencyAmountView(final Context context) {
        super(context);
        init(context);
    }

    public CurrencyAmountView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {
        significantColor = context.getColor(R.color.fg_significant);
        lessSignificantColor = context.getColor(R.color.fg_less_significant);
        errorColor = context.getColor(R.color.fg_error);
        deleteButtonDrawable = context.getDrawable(R.drawable.ic_clear_grey600_24dp);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        textView = (TextView) getChildAt(0);
        textView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        textView.setHintTextColor(lessSignificantColor);
        textView.setHorizontalFadingEdgeEnabled(true);
        textView.setSingleLine();
        setValidateAmount(textView instanceof EditText);
        textView.addTextChangedListener(textViewListener);
        textView.setOnFocusChangeListener(textViewListener);

        contextButton = new View(context) {
            @Override
            protected void onMeasure(final int wMeasureSpec, final int hMeasureSpec) {
                setMeasuredDimension(textView.getCompoundPaddingRight(), textView.getMeasuredHeight());
            }
        };
        final LayoutParams chooseViewParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        chooseViewParams.gravity = Gravity.RIGHT;
        contextButton.setLayoutParams(chooseViewParams);
        this.addView(contextButton);

        updateAppearance();
    }

    public void setCurrencySymbol(@Nullable final String currencyCode) {
        final String bitcoinSymbol = Character.toString(Constants.CHAR_BITCOIN);
        final boolean hasBitcoinSymbol = textView.getPaint().hasGlyph(bitcoinSymbol);
        final float textSize = textView.getTextSize();
        final float smallerTextSize = textSize * (20f / 24f);
        final float offset = textSize * 0.37f;
        if (MonetaryFormat.CODE_BTC.equals(currencyCode)) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = new CurrencySymbolDrawable(bitcoinSymbol, smallerTextSize,
                        lessSignificantColor, offset);
            else
                currencySymbolDrawable = getContext().getDrawable(R.drawable.currency_symbol_btc);
            localCurrencyCode = null;
        } else if (MonetaryFormat.CODE_MBTC.equals(currencyCode)) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = new CurrencySymbolDrawable("m" + bitcoinSymbol, smallerTextSize,
                        lessSignificantColor, offset);
            else
                currencySymbolDrawable = getContext().getDrawable(R.drawable.currency_symbol_mbtc);
            localCurrencyCode = null;
        } else if (MonetaryFormat.CODE_UBTC.equals(currencyCode)) {
            if (hasBitcoinSymbol)
                currencySymbolDrawable = new CurrencySymbolDrawable("µ" + bitcoinSymbol, smallerTextSize,
                        lessSignificantColor, offset);
            else
                currencySymbolDrawable = getContext().getDrawable(R.drawable.currency_symbol_ubtc);
            localCurrencyCode = null;
        } else if (MonetaryFormat.CODE_SAT.equals(currencyCode)) {
            currencySymbolDrawable = new CurrencySymbolDrawable(MonetaryFormat.SYMBOL_SAT, smallerTextSize,
                    lessSignificantColor, offset);
            localCurrencyCode = null;
        } else if (currencyCode != null) {
            currencySymbolDrawable = new CurrencySymbolDrawable(GenericUtils.currencySymbol(currencyCode),
                    smallerTextSize, lessSignificantColor, offset);
            localCurrencyCode = currencyCode;
        } else {
            currencySymbolDrawable = null;
            localCurrencyCode = null;
        }

        updateAppearance();
    }

    public void setInputFormat(final MonetaryFormat inputFormat) {
        this.inputFormat = inputFormat.noCode();
    }

    public void setHintFormat(final MonetaryFormat hintFormat) {
        this.hintFormat = hintFormat.noCode();
        updateAppearance();
    }

    public void setHint(@Nullable final Monetary hint) {
        this.hint = hint;
        updateAppearance();
    }

    public void setAmountSigned(final boolean amountSigned) {
        this.amountSigned = amountSigned;
    }

    public void setValidateAmount(final boolean validateAmount) {
        this.validateAmount = validateAmount;
    }

    public void setContextButton(final int contextButtonResId, final OnClickListener contextButtonClickListener) {
        this.contextButtonDrawable = getContext().getDrawable(contextButtonResId);
        this.contextButtonClickListener = contextButtonClickListener;

        updateAppearance();
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    @Nullable
    public Monetary getAmount() {
        if (!isValidAmount(false))
            return null;

        final String amountStr = textView.getText().toString().trim();
        if (localCurrencyCode == null)
            return inputFormat.parse(amountStr);
        else
            return inputFormat.parseFiat(localCurrencyCode, amountStr);
    }

    public void setAmount(@Nullable final Monetary amount, final boolean fireListener) {
        if (!fireListener)
            textViewListener.setFire(false);

        if (amount != null)
            textView.setText(new MonetarySpannable(inputFormat, amountSigned, amount));
        else
            textView.setText(null);

        if (!fireListener)
            textViewListener.setFire(true);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        textView.setEnabled(enabled);

        updateAppearance();
    }

    public void setTextColor(final int color) {
        significantColor = color;

        updateAppearance();
    }

    public void setStrikeThru(final boolean strikeThru) {
        if (strikeThru)
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    public TextView getTextView() {
        return textView;
    }

    public void setNextFocusId(final int nextFocusId) {
        textView.setNextFocusDownId(nextFocusId);
        textView.setNextFocusForwardId(nextFocusId);
    }

    private boolean isValidAmount(final boolean zeroIsValid) {
        final String str = textView.getText().toString().trim();

        try {
            if (!str.isEmpty()) {
                final Monetary amount;
                if (localCurrencyCode == null) {
                    amount = inputFormat.parse(str);
                    if (((Coin) amount).isGreaterThan(Constants.NETWORK_PARAMETERS.getMaxMoney()))
                        return false;
                } else {
                    amount = inputFormat.parseFiat(localCurrencyCode, str);
                }

                // exactly zero
                return zeroIsValid || amount.signum() > 0;
            }
        } catch (final Exception x) {
        }

        return false;
    }

    private final OnClickListener deleteClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            setAmount(null, true);
            textView.requestFocus();
        }
    };

    private void updateAppearance() {
        final boolean enabled = textView.isEnabled();
        final String amount = textView.getText().toString().trim();

        if (enabled && !amount.isEmpty()) {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, deleteButtonDrawable, null);
            contextButton.setOnClickListener(deleteClickListener);
            contextButton.setVisibility(View.VISIBLE);
        } else if (enabled && contextButtonDrawable != null) {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, contextButtonDrawable, null);
            contextButton.setOnClickListener(contextButtonClickListener);
            contextButton.setVisibility(View.VISIBLE);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(currencySymbolDrawable, null, null, null);
            contextButton.setOnClickListener(null);
            contextButton.setVisibility(View.GONE);
        }

        contextButton.requestLayout();

        textView.setTextColor(!validateAmount || isValidAmount(true) ? significantColor : errorColor);

        final Spannable hintSpannable = new MonetarySpannable(hintFormat, hint != null ? hint : Coin.ZERO)
                .applyMarkup(null, MonetarySpannable.STANDARD_INSIGNIFICANT_SPANS);
        textView.setHint(hintSpannable);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable("super_state", super.onSaveInstanceState());
        state.putParcelable("child_textview", textView.onSaveInstanceState());
        final Monetary amount = getAmount();
        if (amount instanceof Coin) {
            state.putLong("coin_amount", amount.getValue());
        } else if (amount instanceof Fiat) {
            state.putLong("fiat_amount", amount.getValue());
            state.putString("fiat_currency", ((Fiat) amount).getCurrencyCode());
        }
        return state;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("super_state"));
            textView.onRestoreInstanceState(bundle.getParcelable("child_textview"));
            if (bundle.containsKey("coin_amount"))
                setAmount(Coin.valueOf(bundle.getLong("coin_amount")), false);
            else if (bundle.containsKey("fiat_amount"))
                setAmount(Fiat.valueOf(bundle.getString("fiat_currency"), bundle.getLong("fiat_amount")), false);
            else
                setAmount(null, false);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private final TextViewListener textViewListener = new TextViewListener();

    private final class TextViewListener implements TextWatcher, OnFocusChangeListener {
        private boolean fire = true;

        public void setFire(final boolean fire) {
            this.fire = fire;
        }

        @Override
        public void afterTextChanged(final Editable s) {
            // workaround for German keyboards
            final String original = s.toString();
            final String replaced = original.replace(',', '.');
            if (!replaced.equals(original)) {
                s.clear();
                s.append(replaced);
            }

            MonetarySpannable.applyMarkup(s, null, MonetarySpannable.STANDARD_SIGNIFICANT_SPANS,
                    MonetarySpannable.STANDARD_INSIGNIFICANT_SPANS);
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            updateAppearance();
            if (listener != null && fire)
                listener.changed();
        }

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                final Monetary amount = getAmount();
                if (amount != null)
                    setAmount(amount, false);
            }

            if (listener != null && fire)
                listener.focusChanged(hasFocus);
        }
    }
}
