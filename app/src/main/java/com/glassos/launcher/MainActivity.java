
package com.glassos.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends Activity {

    private final Handler handler = new Handler();
    private TextView clockView;
    private LinearLayout dock;
    private final ArrayList<String> shelfApps = new ArrayList<>();
    private boolean reorderMode;
    private int movingIndex = -1;

    private final String[] appNames = {
        "Netflix", "Prime Video", "YouTube",
        "Disney+", "Apps", "Settings"
    };

    private final int[] colors = {
        Color.rgb(190, 20, 35),
        Color.rgb(20, 80, 155),
        Color.rgb(210, 35, 35),
        Color.rgb(30, 100, 190),
        Color.rgb(65, 70, 85),
        Color.rgb(85, 90, 105)
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shelfApps.addAll(Arrays.asList(appNames));
        String savedOrder = getPreferences(MODE_PRIVATE).getString("shelf_order", null);
        if (savedOrder != null) {
            ArrayList<String> restored = new ArrayList<>(Arrays.asList(savedOrder.split("\\n")));
            if (restored.size() == appNames.length && restored.containsAll(Arrays.asList(appNames))) {
                shelfApps.clear();
                shelfApps.addAll(restored);
            }
        }

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(12, 19, 34));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(48, 28, 48, 24);

        root.addView(page, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Top row: title and clock
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = text("PEXY", 30, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, 65, 1));

        clockView = text("", 19, Color.WHITE, false);
        clockView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(clockView);

        page.addView(top);
        updateClock();

        // Small settings button
        TextView settings = text("⚙", 22, Color.WHITE, false);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(round(Color.rgb(42, 52, 72), 18));

        LinearLayout.LayoutParams settingsParams =
            new LinearLayout.LayoutParams(54, 48);
        settingsParams.gravity = Gravity.RIGHT;
        settingsParams.topMargin = 8;

        page.addView(settings, settingsParams);

        settings.setFocusable(true);
        settings.setOnClickListener(v ->
            Toast.makeText(
                this, "Settings coming soon", Toast.LENGTH_SHORT
            ).show()
        );

        // Widget area
        LinearLayout widgets = new LinearLayout(this);
        widgets.setOrientation(LinearLayout.HORIZONTAL);
        widgets.setGravity(Gravity.CENTER);
        widgets.setPadding(0, 24, 0, 20);

        widgets.addView(
            widgetCard("GOOD EVENING", "Welcome home"),
            new LinearLayout.LayoutParams(0, 155, 1)
        );

        View gap = new View(this);
        widgets.addView(
            gap, new LinearLayout.LayoutParams(18, 1)
        );

        widgets.addView(
            widgetCard("WEATHER", "Weather unavailable"),
            new LinearLayout.LayoutParams(0, 155, 1)
        );

        page.addView(widgets, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // Six-icon bottom dock
        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(12, 12, 12, 12);
        dock.setBackground(round(Color.rgb(31, 41, 60), 24));

        buildDock();

        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 112
            );
        dockParams.bottomMargin = 8;

        page.addView(dock, dockParams);

        setContentView(root);
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
    }

    /** Recreate the shelf in its saved order. Holding Select enters move mode. */
    private void buildDock() {
        if (dock == null) return;
        dock.removeAllViews();
        for (int i = 0; i < shelfApps.size(); i++) {
            final int index = i;
            final String name = shelfApps.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setFocusable(true);
            item.setFocusableInTouchMode(false);
            item.setBackground(round(Color.TRANSPARENT, 14));

            String symbol = name.equals("Settings") ? "⚙" : name.equals("Apps") ? "▦" : "▶";
            TextView icon = text(symbol, 27, Color.WHITE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(round(colors[Arrays.asList(appNames).indexOf(name)], 16));
            item.addView(icon, new LinearLayout.LayoutParams(68, 58));

            TextView label = text(name, 13, Color.WHITE, false);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 30);
            labelParams.topMargin = 5;
            item.addView(label, labelParams);
            dock.addView(item, new LinearLayout.LayoutParams(0, 95, 1));

            item.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackground(round(Color.rgb(72, 91, 118), 14));
                    if (reorderMode) movingIndex = index;
                } else {
                    v.setBackground(round(Color.TRANSPARENT, 14));
                }
            });
            item.setOnClickListener(v -> {
                if (reorderMode) {
                    reorderMode = false;
                    saveShelfOrder();
                    Toast.makeText(this, name + " placed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, name + " selected", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (dock.getChildCount() > 0) dock.getChildAt(Math.max(0, movingIndex)).requestFocus();
    }

    private void saveShelfOrder() {
        getPreferences(MODE_PRIVATE).edit().putString("shelf_order", android.text.TextUtils.join("\n", shelfApps)).apply();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dock != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!reorderMode && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                && event.getRepeatCount() >= 1 && focusedDockIndex() >= 0) {
                reorderMode = true;
                movingIndex = focusedDockIndex();
                Toast.makeText(this, "Move mode: use ◀ ▶, press Select to place", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (reorderMode) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int step = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
                    int next = Math.max(0, Math.min(shelfApps.size() - 1, movingIndex + step));
                    if (next != movingIndex) {
                        Collections.swap(shelfApps, movingIndex, next);
                        movingIndex = next;
                        buildDock();
                        Toast.makeText(this, "Position " + (next + 1) + " — press Select to place", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // Ignore the auto-repeat events that initiated the long press.
                    if (event.getRepeatCount() > 0) return true;
                    reorderMode = false;
                    saveShelfOrder();
                    Toast.makeText(this, "App position saved", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    reorderMode = false;
                    return true;
                }
            }
        }
        if (reorderMode && event.getAction() == KeyEvent.ACTION_UP
            && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private int focusedDockIndex() {
        if (dock == null) return -1;
        for (int i = 0; i < dock.getChildCount(); i++) {
            if (dock.getChildAt(i).hasFocus()) return i;
        }
        return -1;
    }

    // FIXED: Return the complete card, not a child TextView.
    private View widgetCard(String heading, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(22, 18, 22, 18);
        card.setBackground(round(Color.rgb(29, 41, 61), 22));

        TextView h = text(heading, 14, Color.LTGRAY, true);
        TextView m = text(message, 22, Color.WHITE, true);

        card.addView(h);

        LinearLayout.LayoutParams messageParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
        messageParams.topMargin = 12;

        card.addView(m, messageParams);

        return card;
    }

    private TextView text(
        String value, int size, int color, boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private android.graphics.drawable.GradientDrawable round(
        int color, int radius
    ) {
        android.graphics.drawable.GradientDrawable shape =
            new android.graphics.drawable.GradientDrawable();

        shape.setColor(color);
        shape.setCornerRadius(radius);

        return shape;
    }

    private void updateClock() {
        if (clockView != null) {
            String time = new SimpleDateFormat(
                "HH:mm  EEE, d MMM", Locale.getDefault()
            ).format(new Date());

            clockView.setText(time);
        }

        handler.postDelayed(this::updateClock, 30000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
