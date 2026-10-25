package com.glassos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** PexyGlassOS home screen. AerialViews/Projectivy wallpaper remains the device wallpaper. */
public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 42;
    private static final int GLASS = Color.argb(185, 17, 30, 46);
    private static final int WHITE = Color.WHITE;
    private static final String[] DEFAULT_APPS = {
        "Smarters IPTV", "Plex", "YouTube", "Artemis", "X-plore", "NordVPN"
    };
    private static final String[] DEFAULT_PACKAGES = {
        "com.nst.iptvsmarterstvbox", "com.plexapp.android", "com.google.android.youtube.tv",
        "", "com.lonelycatgames.Xplore", "com.nordvpn.android"
    };

    private final ArrayList<AppEntry> shelf = new ArrayList<>();
    private LinearLayout dock;
    private FrameLayout root;
    private TextView clock;
    private int focusedApp = 0;
    private boolean movingApp;
    private boolean suppressCenterUp;
    private boolean settingsOpen;
    private String editingPackage;
    private final HandlerClock clockUpdater = new HandlerClock();
    private SharedPreferences prefs;

    private static final class AppEntry {
        String title;
        String packageName;
        AppEntry(String title, String packageName) { this.title = title; this.packageName = packageName; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        prefs = getPreferences(MODE_PRIVATE);
        loadShelf();
        showHome();
    }

    private void loadShelf() {
        shelf.clear();
        String stored = prefs.getString("shelf_order", null);
        if (stored == null) {
            for (int i = 0; i < DEFAULT_APPS.length; i++) shelf.add(new AppEntry(DEFAULT_APPS[i], DEFAULT_PACKAGES[i]));
            return;
        }
        Set<String> hidden = prefs.getStringSet("hidden_apps", new HashSet<>());
        for (String pkg : stored.split("\\n")) {
            if (pkg.isEmpty()) continue;
            if (hidden.contains(pkg)) continue;
            int index = Arrays.asList(DEFAULT_PACKAGES).indexOf(pkg);
            if (index >= 0) shelf.add(new AppEntry(prefs.getString("name:" + pkg, DEFAULT_APPS[index]), pkg));
        }
        for (int i = 0; i < DEFAULT_APPS.length; i++) {
            if (!hidden.contains(DEFAULT_PACKAGES[i]) && !containsPackage(DEFAULT_PACKAGES[i])) shelf.add(new AppEntry(DEFAULT_APPS[i], DEFAULT_PACKAGES[i]));
        }
    }

    private boolean containsPackage(String pkg) {
        for (AppEntry app : shelf) if (app.packageName.equals(pkg)) return true;
        return false;
    }

    private void showHome() {
        settingsOpen = false;
        movingApp = false;
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.argb(58, 8, 15, 25));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(34), dp(22), dp(34), dp(18));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.setOrientation(LinearLayout.HORIZONTAL);
        clock = label("", 46, WHITE, true);
        clock.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(clock, new LinearLayout.LayoutParams(-2, dp(62)));
        ImageView profile = new ImageView(this);
        profile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profile.setBackground(round(Color.argb(190, 255, 255, 255), dp(30)));
        profile.setClipToOutline(true);
        String profileUri = prefs.getString("profile_uri", null);
        if (profileUri != null) try { profile.setImageURI(Uri.parse(profileUri)); } catch (Exception ignored) { }
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        profileParams.leftMargin = dp(12);
        header.addView(profile, profileParams);
        profile.setFocusable(true);
        profile.setOnClickListener(v -> pickImage("profile_uri"));
        TextView settings = label("⚙", 24, WHITE, true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(round(Color.argb(90, 255, 255, 255), dp(24)));
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        settingsParams.leftMargin = dp(12);
        header.addView(settings, settingsParams);
        settings.setFocusable(true);
        settings.setContentDescription("Settings");
        settings.setOnClickListener(v -> showSettings());
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));

        LinearLayout widgets = buildWidgetArea();
        page.addView(widgets, new LinearLayout.LayoutParams(-1, 0, 1));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dp(12), dp(12), dp(12), dp(12));
        dock.setBackground(round(Color.argb(185, 17, 30, 46), dp(24)));
        buildDock();
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1, dp(146));
        dockParams.bottomMargin = dp(6);
        page.addView(dock, dockParams);
        setContentView(root);
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
        clockUpdater.tick();
    }

    private View widget(String heading, String body, String launchTitle, String packageName) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        card.setBackground(round(Color.argb(115, 15, 29, 45), dp(22)));
        TextView title = label(heading, 14, Color.argb(210, 255, 255, 255), true);
        TextView content = label(body, 21, WHITE, true);
        content.setMaxLines(3);
        card.addView(title);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.topMargin = dp(11);
        card.addView(content, bodyParams);
        card.setFocusable(true);
        card.setOnClickListener(v -> launchPackage(packageName, launchTitle));
        return card;
    }

    private LinearLayout buildWidgetArea() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        row.setPadding(0, dp(8), 0, dp(20));
        String order = prefs.getString("widget_order", "bible\nplexamp\nweather\nsugarglass");
        for (String id : order.split("\\n")) {
            String heading, content, target;
            if ("bible".equals(id)) { heading = "DAILY BIBLE VERSE"; content = dailyVerse(); target = "BIBLE"; }
            else if ("plexamp".equals(id)) { heading = "PLEXAMP"; content = "Tap to open Plexamp and play music"; target = "Plexamp"; }
            else if ("weather".equals(id)) { heading = "WEATHER"; content = "Connect a weather provider in Settings"; target = "Weather"; }
            else if ("sugarglass".equals(id)) { heading = "SUGARGLASS"; content = "Connect SugarGlass to show readings"; target = "SugarGlass"; }
            else continue;
            int span = Math.max(1, Math.min(3, prefs.getInt("widget_size:" + id, 1)));
            row.addView(widget(heading, content, target, ""), new LinearLayout.LayoutParams(0, dp(142 + (span - 1) * 22), span));
            row.addView(spacer(dp(12)));
        }
        return row;
    }

    private String dailyVerse() {
        String[] verses = {
            "For God so loved the world…  John 3:16",
            "The Lord is my shepherd…  Psalm 23:1",
            "Be still, and know that I am God.  Psalm 46:10",
            "I can do all things through Christ…  Philippians 4:13",
            "Thy word is a lamp unto my feet…  Psalm 119:105",
            "Trust in the Lord with all thine heart…  Proverbs 3:5",
            "Love one another.  John 13:34"
        };
        int day = (int) (System.currentTimeMillis() / 86400000L);
        return verses[Math.floorMod(day, verses.length)];
    }

    private void buildDock() {
        dock.removeAllViews();
        for (int i = 0; i < shelf.size(); i++) {
            final int index = i;
            AppEntry app = shelf.get(i);
            LinearLayout tile = new LinearLayout(this);
            tile.setGravity(Gravity.CENTER);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(8), dp(7), dp(8), dp(6));
            tile.setFocusable(true);
            tile.setBackground(round(Color.TRANSPARENT, dp(14)));

            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            String custom = prefs.getString("icon:" + app.packageName, null);
            boolean hasIcon = false;
            if (custom != null) try { icon.setImageURI(Uri.parse(custom)); hasIcon = true; } catch (Exception ignored) { }
            if (!hasIcon) {
                try {
                    if (!app.packageName.isEmpty()) icon.setImageDrawable(getPackageManager().getApplicationIcon(app.packageName));
                    else icon.setImageDrawable(findAppIcon(app.title));
                } catch (Exception ignored) { icon.setImageDrawable(findAppIcon(app.title)); }
            }
            tile.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));
            TextView name = label(prefs.getString("name:" + app.packageName, app.title), 15, WHITE, true);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            tile.addView(name, new LinearLayout.LayoutParams(-1, dp(30)));
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -1, 1);
            tileParams.setMargins(dp(5), 0, dp(5), 0);
            dock.addView(tile, tileParams);
            tile.setOnFocusChangeListener((v, focused) -> v.setBackground(round(
                focused ? Color.argb(85, 255, 255, 255) : Color.TRANSPARENT, dp(15))));
            tile.setOnClickListener(v -> {
                if (movingApp) {
                    movingApp = false;
                    saveOrder();
                    Toast.makeText(this, "App position saved", Toast.LENGTH_SHORT).show();
                } else launchPackage(app.packageName, app.title);
            });
        }
        if (dock.getChildCount() > 0) dock.getChildAt(Math.max(0, Math.min(focusedApp, dock.getChildCount() - 1))).requestFocus();
    }

    private android.graphics.drawable.Drawable findAppIcon(String title) {
        try {
            List<ResolveInfo> apps = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
            for (ResolveInfo info : apps) if (info.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT))) return info.loadIcon(getPackageManager());
        } catch (Exception ignored) { }
        return new ColorDrawable(Color.TRANSPARENT);
    }

    private void launchPackage(String packageName, String label) {
        try {
            Intent intent = packageName == null || packageName.isEmpty() ? null : getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null && label != null) {
                for (ResolveInfo app : getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)) {
                    if (app.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT).contains(label.toLowerCase(Locale.ROOT))) {
                        intent = new Intent(Intent.ACTION_MAIN).setClassName(app.activityInfo.packageName, app.activityInfo.name);
                        break;
                    }
                }
            }
            if (intent != null) startActivity(intent);
            else Toast.makeText(this, label + " is not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Could not open " + label, Toast.LENGTH_SHORT).show(); }
    }

    private void showSettings() {
        settingsOpen = true;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.argb(225, 9, 17, 29));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(36), dp(24), dp(36), dp(24));
        scroll.addView(panel);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Settings", 32, WHITE, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView close = label("✕", 24, WHITE, true);
        close.setGravity(Gravity.CENTER);
        close.setFocusable(true);
        heading.addView(close, new LinearLayout.LayoutParams(dp(52), dp(52)));
        close.setOnClickListener(v -> showHome());
        panel.addView(heading);

        Button vpn = button("NordVPN  ·  " + (isVpnConnected() ? "Connected" : "Disconnected"));
        panel.addView(vpn);
        vpn.setOnClickListener(v -> launchPackage("com.nordvpn.android", "NordVPN"));
        panel.addView(label("Select to open NordVPN; connection changes are handled by the NordVPN app.", 14, Color.LTGRAY, false));

        panel.addView(sectionTitle("Apps on your home shelf"));
        for (AppEntry app : new ArrayList<>(shelf)) {
            LinearLayout row = settingsRow(app.title + "  ·  " + app.packageName, "Edit name/icon", "Hide");
            row.getChildAt(1).setOnClickListener(v -> editApp(app));
            row.getChildAt(2).setOnClickListener(v -> hideApp(app));
            panel.addView(row);
        }
        panel.addView(sectionTitle("Hidden apps"));
        Set<String> hidden = prefs.getStringSet("hidden_apps", new HashSet<>());
        if (hidden.isEmpty()) panel.addView(label("No hidden apps", 17, Color.LTGRAY, false));
        else for (String pkg : new ArrayList<>(hidden)) {
            String name = appLabel(pkg);
            LinearLayout row = settingsRow(name, "Open", "Unhide");
            row.getChildAt(1).setOnClickListener(v -> launchPackage(pkg, name));
            row.getChildAt(2).setOnClickListener(v -> unhideApp(pkg));
            panel.addView(row);
        }
        panel.addView(sectionTitle("Installed apps"));
        Set<String> known = new HashSet<>(Arrays.asList(DEFAULT_PACKAGES));
        for (ResolveInfo info : getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)) {
            String pkg = info.activityInfo.packageName;
            if (known.contains(pkg)) continue;
            LinearLayout row = settingsRow(info.loadLabel(getPackageManager()).toString(), "Open", "Hide");
            row.getChildAt(1).setOnClickListener(v -> launchPackage(pkg, info.loadLabel(getPackageManager()).toString()));
            row.getChildAt(2).setOnClickListener(v -> hidePackage(pkg));
            panel.addView(row);
        }
        panel.addView(sectionTitle("Device"));
        Button profile = button("Change profile picture");
        panel.addView(profile);
        profile.setOnClickListener(v -> pickImage("profile_uri"));
        Button homeLauncher = button("Return to Fire TV default launcher");
        panel.addView(homeLauncher);
        homeLauncher.setOnClickListener(v -> {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            try { startActivity(home); } catch (Exception e) { Toast.makeText(this, "Open Fire TV Home from the device Home button", Toast.LENGTH_LONG).show(); }
        });
        panel.addView(sectionTitle("Widgets"));
        String[] widgetIds = {"bible", "plexamp", "weather", "sugarglass"};
        String[] widgetNames = {"Daily Bible verse", "Plexamp", "Weather", "SugarGlass"};
        for (int i = 0; i < widgetIds.length; i++) {
            final String widgetId = widgetIds[i];
            LinearLayout row = widgetSettingsRow(widgetNames[i], widgetIds[i]);
            row.getChildAt(1).setOnClickListener(v -> moveWidget(widgetId, -1));
            row.getChildAt(2).setOnClickListener(v -> moveWidget(widgetId, 1));
            row.getChildAt(3).setOnClickListener(v -> resizeWidget(widgetId));
            panel.addView(row);
        }
        panel.addView(label("Widget order and size are saved on this device. Weather and glucose readings need their provider apps/services.", 15, Color.LTGRAY, false));
        panel.addView(sectionTitle("Device information"));
        panel.addView(label(android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "  ·  Android " + android.os.Build.VERSION.RELEASE, 17, WHITE, false));
        panel.addView(label("This home screen shows the device wallpaper. Set AerialViews as Projectivy’s wallpaper provider for aerial video backgrounds.", 14, Color.LTGRAY, false));
        Button back = button("Done");
        panel.addView(back);
        back.setOnClickListener(v -> showHome());
        setContentView(scroll);
        close.requestFocus();
    }

    private LinearLayout settingsRow(String text, String action1, String action2) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(7), dp(12), dp(7));
        TextView name = label(text, 17, WHITE, false);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button first = button(action1);
        Button second = button(action2);
        row.addView(first);
        row.addView(second);
        return row;
    }

    private LinearLayout widgetSettingsRow(String text, String id) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(7), dp(12), dp(7));
        row.addView(label(text, 17, WHITE, false), new LinearLayout.LayoutParams(0, dp(52), 1));
        row.addView(button("Move left"));
        row.addView(button("Move right"));
        int size = Math.max(1, Math.min(3, prefs.getInt("widget_size:" + id, 1)));
        row.addView(button("Size " + (size == 1 ? "S" : size == 2 ? "M" : "L")));
        return row;
    }

    private void editApp(AppEntry app) {
        editingPackage = app.packageName;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(8), dp(24), dp(8));
        android.widget.EditText name = new android.widget.EditText(this);
        name.setSingleLine(true);
        name.setText(prefs.getString("name:" + app.packageName, app.title));
        layout.addView(name);
        new AlertDialog.Builder(this).setTitle("Change app name")
            .setView(layout).setNegativeButton("Cancel", null)
            .setNeutralButton("Choose icon image", (d, w) -> pickImage("icon:" + editingPackage))
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit().putString("name:" + app.packageName, name.getText().toString().trim()).apply();
                for (AppEntry item : shelf) if (item.packageName.equals(app.packageName)) item.title = name.getText().toString().trim();
                showSettings();
            }).show();
    }

    private void hideApp(AppEntry app) { hidePackage(app.packageName); }
    private void hidePackage(String pkg) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet("hidden_apps", new HashSet<>()));
        hidden.add(pkg);
        prefs.edit().putStringSet("hidden_apps", hidden).apply();
        shelf.removeIf(app -> app.packageName.equals(pkg));
        saveOrder();
        showSettings();
    }
    private void unhideApp(String pkg) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet("hidden_apps", new HashSet<>()));
        hidden.remove(pkg);
        prefs.edit().putStringSet("hidden_apps", hidden).apply();
        int i = Arrays.asList(DEFAULT_PACKAGES).indexOf(pkg);
        if (i >= 0 && !containsPackage(pkg)) shelf.add(new AppEntry(DEFAULT_APPS[i], pkg));
        else if (!containsPackage(pkg) && shelf.size() < 12) shelf.add(new AppEntry(appLabel(pkg), pkg));
        saveOrder();
        showSettings();
    }

    private String appLabel(String pkg) {
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    private boolean isVpnConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } catch (Exception e) { return false; }
    }

    private void pickImage(String key) {
        editingPackage = key;
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        try { startActivityForResult(pick, PICK_IMAGE); }
        catch (Exception e) { Toast.makeText(this, "Image picker unavailable", Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_IMAGE && result == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { }
            prefs.edit().putString(editingPackage, uri.toString()).apply();
            if ("profile_uri".equals(editingPackage)) showHome(); else showSettings();
        }
    }

    private void saveOrder() {
        ArrayList<String> packages = new ArrayList<>();
        for (AppEntry app : shelf) packages.add(app.packageName);
        prefs.edit().putString("shelf_order", android.text.TextUtils.join("\n", packages)).apply();
    }

    private void moveWidget(String id, int direction) {
        ArrayList<String> order = new ArrayList<>(Arrays.asList(prefs.getString("widget_order", "bible\nplexamp\nweather\nsugarglass").split("\\n")));
        int i = order.indexOf(id);
        int next = i + direction;
        if (i >= 0 && next >= 0 && next < order.size()) Collections.swap(order, i, next);
        prefs.edit().putString("widget_order", android.text.TextUtils.join("\n", order)).apply();
        showSettings();
    }

    private void resizeWidget(String id) {
        int size = prefs.getInt("widget_size:" + id, 1);
        prefs.edit().putInt("widget_size:" + id, size >= 3 ? 1 : size + 1).apply();
        showSettings();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (dock != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!movingApp && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER && event.getRepeatCount() >= 1) {
                focusedApp = focusedDockIndex();
                if (focusedApp >= 0) {
                    movingApp = true;
                    Toast.makeText(this, "Move app: use left/right, press Select to place", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            if (movingApp) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int next = Math.max(0, Math.min(shelf.size() - 1, focusedApp + (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1)));
                    if (next != focusedApp) {
                        Collections.swap(shelf, focusedApp, next);
                        focusedApp = next;
                        buildDock();
                        Toast.makeText(this, "Position " + (next + 1), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getRepeatCount() > 0) return true;
                    movingApp = false;
                    suppressCenterUp = true;
                    saveOrder();
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) { movingApp = false; return true; }
            }
        }
        if ((movingApp || suppressCenterUp) && event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
            suppressCenterUp = false;
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private int focusedDockIndex() {
        if (dock == null) return -1;
        for (int i = 0; i < dock.getChildCount(); i++) if (dock.getChildAt(i).hasFocus()) return i;
        return -1;
    }

    private TextView sectionTitle(String value) {
        TextView v = label(value, 22, WHITE, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(46));
        p.topMargin = dp(16);
        v.setLayoutParams(p);
        return v;
    }
    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setBackground(round(Color.argb(78, 255, 255, 255), dp(10)));
        return b;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private View spacer(int width) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(width, 1)); return v; }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onBackPressed() {
        if (settingsOpen) showHome();
        else super.onBackPressed();
    }

    private final class HandlerClock {
        private final android.os.Handler handler = new android.os.Handler();
        private final Runnable runnable = this::tick;
        void tick() {
            handler.removeCallbacks(runnable);
            if (clock != null && clock.isShown()) {
                clock.setText(new SimpleDateFormat("h:mm a  EEE, d MMM", Locale.getDefault()).format(new Date()));
                handler.postDelayed(runnable, 30000);
            }
        }
    }

    @Override protected void onDestroy() {
        clockUpdater.handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
