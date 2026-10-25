
package com.glassos.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler handler = new Handler();
    private TextView clockView;

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
        settings.setOnClickListener(v ->
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        );

        // Open widget area
        LinearLayout widgets = new LinearLayout(this);
        widgets.setOrientation(LinearLayout.HORIZONTAL);
        widgets.setGravity(Gravity.CENTER);
        widgets.setPadding(0, 24, 0, 20);

        widgets.addView(widgetCard("GOOD EVENING", "Welcome home"),
            new LinearLayout.LayoutParams(0, 155, 1));

        View gap = new View(this);
        widgets.addView(gap, new LinearLayout.LayoutParams(18, 1));

        widgets.addView(widgetCard("WEATHER", "Weather unavailable"),
            new LinearLayout.LayoutParams(0, 155, 1));

        page.addView(widgets, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // Six-icon bottom dock
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(12, 12, 12, 12);
        dock.setBackground(round(Color.rgb(31, 41, 60), 24));

        for (int i = 0; i < appNames.length; i++) {
            final String name = appNames[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);

            TextView icon = text(name.equals("Settings") ? "⚙" :
                name.equals("Apps") ? "▦" : "▶", 27, Color.WHITE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(round(colors[i], 16));

            LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(68, 58);
            item.addView(icon, iconParams);

            TextView label = text(name, 13, Color.WHITE, false);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 30);
            labelParams.topMargin = 5;
            item.addView(label, labelParams);

            LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(0, 95, 1);
            dock.addView(item, itemParams);

            item.setFocusable(true);
            item.setOnClickListener(v ->
                Toast.makeText(this, name + " selected",
                    Toast.LENGTH_SHORT).show()
            );
        }

        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 112);
        dockParams.bottomMargin = 8;
        page.addView(dock, dockParams);

        setContentView(root);
    }

    private TextView widgetCard(String heading, String message) {
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
                LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = 12;
        card.addView(m, messageParams);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.addView(card, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        return m;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private android.graphics.drawable.GradientDrawable round(
        int color, int radius) {
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
