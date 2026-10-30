package com.mylifejournal.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private int bg = 0xFFF3F6F5, surface = 0xFFFFFFFF, surfaceAlt = 0xFFEAF0EE;
    private int ink = 0xFF172A2C, muted = 0xFF718080, line = 0xFFE0E8E5;
    private int accent = 0xFF176B63, accentDark = 0xFF104D48;
    private int accentSoft = 0xFFDDEFEA, warm = 0xFFF7ECDD, warmInk = 0xFF8A6032;
    private LinearLayout root, body;
    private View bottomBar;
    private JournalDb db;
    private JSONObject ui;
    private String page = "Home";
    private String selectedMood = "";
    private int systemBottomInset = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new JournalDb(this);
        loadUiConfig();
        configureSystemBars();
        show("Home");
    }

    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void loadUiConfig() {
        try {
            InputStream in = getAssets().open("ui.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            in.close();
            ui = new JSONObject(out.toString("UTF-8"));
            bg = Color.parseColor(ui.optString("background", "#F3F6F5"));
            surface = Color.parseColor(ui.optString("surface", "#FFFFFF"));
            surfaceAlt = Color.parseColor(ui.optString("surfaceAlt", "#EAF0EE"));
            accent = Color.parseColor(ui.optString("accent", "#176B63"));
            accentDark = Color.parseColor(ui.optString("accentDark", "#104D48"));
            ink = Color.parseColor(ui.optString("text", "#172A2C"));
            muted = Color.parseColor(ui.optString("muted", "#718080"));
            line = Color.parseColor(ui.optString("line", "#E0E8E5"));
            accentSoft = Color.parseColor(ui.optString("accentSoft", "#DDEFEA"));
            warm = Color.parseColor(ui.optString("warm", "#F7ECDD"));
            warmInk = Color.parseColor(ui.optString("warmInk", "#8A6032"));
        } catch (Exception e) { ui = new JSONObject(); }
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radius));
        return d;
    }

    private RippleDrawable ripple(int color, int radius) {
        GradientDrawable content = shape(color, radius);
        GradientDrawable mask = shape(Color.WHITE, radius);
        return new RippleDrawable(ColorStateList.valueOf(0x22104D48), content, mask);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void padding(View v, int l, int t, int r, int b) {
        v.setPadding(dp(l), dp(t), dp(r), dp(b));
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout.LayoutParams margins(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(bottom);
        return p;
    }

    private void attachInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0;
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(0, top, 0, 0);
            systemBottomInset = bottom;
            if (bottomBar != null) padding(bottomBar, 18, 9, 18, 10 + (int)(bottom / getResources().getDisplayMetrics().density));
            return insets;
        });
    }

    private void show(String target) {
        page = target;
        root = column();
        root.setBackgroundColor(bg);
        root.setClipToPadding(false);
        bottomBar = null;
        setContentView(root);
        attachInsets();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        body = column();
        padding(body, 20, 13, 20, 24);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if ("Home".equals(target)) home();
        else if ("Journal".equals(target)) journal();
        else if ("Tasks".equals(target)) tasks();
        else weekly();

        bottomNavigation();
        root.post(() -> root.requestApplyInsets());
        body.setAlpha(0f);
        body.animate().alpha(1f).setDuration(190).start();
    }

    private void topBar(String title, String subtitle) {
        LinearLayout bar = row();
        LinearLayout brand = row();
        LinearLayout mark = new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(accent, 14));
        mark.addView(new IconView("leaf", Color.WHITE), new LinearLayout.LayoutParams(dp(22), dp(22)));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = column();
        TextView heading = text(title, 18, ink, true);
        TextView sub = text(subtitle.toUpperCase(Locale.ROOT), 9, muted, true);
        sub.setLetterSpacing(.13f);
        titles.addView(heading);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-2, -2);
        subp.topMargin = dp(1);
        titles.addView(sub, subp);
        LinearLayout.LayoutParams titlep = new LinearLayout.LayoutParams(0, -2, 1);
        titlep.leftMargin = dp(11);
        brand.addView(titles, titlep);
        bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView avatar = text("G", 16, accentDark, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(accentSoft, 15));
        bar.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));
        body.addView(bar);
        gap(body, 19);
    }

    private void bottomNavigation() {
        LinearLayout nav = row();
        nav.setBackgroundColor(surface);
        nav.setElevation(dp(10));
        nav.setClipToPadding(false);
        padding(nav, 18, 9, 18, 10);
        String[] labels = {"Home", "Journal", "Tasks", "Weekly"};
        String[] icons = {"home", "journal", "tasks", "chart"};
        for (int i = 0; i < labels.length; i++) {
            final String destination = labels[i];
            boolean active = destination.equals(page);
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setBackground(ripple(active ? accentSoft : surface, 18));
            padding(item, 7, 7, 7, 6);
            IconView icon = new IconView(icons[i], active ? accentDark : muted);
            item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = text(labels[i], 10, active ? accentDark : muted, active);
            LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(-2, -2);
            lpLabel.topMargin = dp(3);
            item.addView(label, lpLabel);
            LinearLayout.LayoutParams itemp = new LinearLayout.LayoutParams(0, -2, 1);
            itemp.setMargins(dp(3), 0, dp(3), 0);
            nav.addView(item, itemp);
            item.setOnClickListener(v -> show(destination));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        bottomBar = nav;
        padding(nav, 18, 9, 18, 10 + (int)(systemBottomInset / getResources().getDisplayMetrics().density));
        nav.setAlpha(0f);
        nav.animate().alpha(1f).setDuration(180).start();
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setBackground(shape(surface, 22));
        c.setElevation(dp(1));
        padding(c, 17, 16, 17, 16);
        c.setClipToOutline(true);
        body.addView(c, margins(13));
        return c;
    }

    private void sectionLabel(String label) {
        TextView t = text(label.toUpperCase(Locale.ROOT), 10, accent, true);
        t.setLetterSpacing(.14f);
        body.addView(t);
        gap(body, 6);
    }

    private void pageIntro(String eyebrow, String title, String sub) {
        sectionLabel(eyebrow);
        body.addView(text(title, 27, ink, true));
        TextView description = text(sub, 13, muted, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(5);
        p.bottomMargin = dp(18);
        body.addView(description, p);
    }

    private TextView pill(String value, int color, int foreground) {
        TextView p = text(value, 10, foreground, true);
        p.setGravity(Gravity.CENTER);
        p.setLetterSpacing(.05f);
        padding(p, 11, 7, 11, 7);
        p.setBackground(shape(color, 30));
        return p;
    }

    private void home() {
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        String greeting = "Good morning";
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 12 && hour < 17) greeting = "Good afternoon";
        else if (hour >= 17) greeting = "Good evening";

        topBar("My Life Journal", "Your personal space");
        LinearLayout hero = column();
        hero.setBackground(gradient(0xFF164F4B, 0xFF247C70, 26));
        padding(hero, 20, 19, 20, 19);
        LinearLayout heroTop = row();
        heroTop.addView(pill("YOUR DAILY RESET", 0x33FFFFFF, Color.WHITE));
        TextView dateText = text(date, 11, 0xFFE3F1ED, true);
        dateText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        heroTop.addView(dateText, new LinearLayout.LayoutParams(0, -2, 1));
        hero.addView(heroTop);

        TextView welcome = text(greeting + ", Gaurav.", 24, Color.WHITE, true);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.topMargin = dp(17);
        hero.addView(welcome, wp);
        TextView headline = text("Make space for what matters.", 16, 0xFFE7F4F0, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(5);
        hero.addView(headline, hp);
        TextView sub = text("Small, steady steps are still progress. Start with one thing today.", 12, 0xFFD3E8E2, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(7);
        hero.addView(sub, sp);
        body.addView(hero, margins(20));

        sectionLabel("DAILY CHECK-IN");
        LinearLayout mood = card();
        TextView moodTitle = text("How are you feeling today?", 17, ink, true);
        mood.addView(moodTitle);
        TextView moodSub = text("Choose what feels closest. No judgement.", 12, muted, false);
        LinearLayout.LayoutParams msp = new LinearLayout.LayoutParams(-1, -2);
        msp.topMargin = dp(4);
        mood.addView(moodSub, msp);

        LinearLayout moodRow = row();
        String[] faces = {"😞", "😕", "😐", "🙂", "😊"};
        String[] names = {"Low", "Meh", "Okay", "Good", "Great"};
        for (int i = 0; i < faces.length; i++) {
            final String selected = names[i];
            LinearLayout option = column();
            option.setGravity(Gravity.CENTER);
            TextView face = text(faces[i], 22, ink, false);
            face.setGravity(Gravity.CENTER);
            face.setBackground(shape(selectedMood.equals(selected) ? accentSoft : surfaceAlt, 14));
            option.addView(face, new LinearLayout.LayoutParams(dp(43), dp(43)));
            TextView label = text(names[i], 9, selectedMood.equals(selected) ? accentDark : muted, selectedMood.equals(selected));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
            flp.topMargin = dp(5);
            option.addView(label, flp);
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, -2, 1);
            moodRow.addView(option, op);
            option.setOnClickListener(v -> {
                selectedMood = selected;
                db.mood(selected);
                show("Home");
                Toast.makeText(this, "Mood saved: " + selected, Toast.LENGTH_SHORT).show();
            });
        }
        LinearLayout.LayoutParams mrp = new LinearLayout.LayoutParams(-1, -2);
        mrp.topMargin = dp(15);
        mood.addView(moodRow, mrp);

        sectionLabel("TODAY'S MOMENTUM");
        LinearLayout progress = card();
        LinearLayout progressHead = row();
        progressHead.addView(text("Your little wins", 17, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        progressHead.addView(pill(db.done() + " / " + db.total() + " DONE", accentSoft, accentDark));
        progress.addView(progressHead);
        TextView progressSub = text(db.total() == 0 ? "Add one small task to get started." : "Keep going. Every completed step counts.", 12, muted, false);
        LinearLayout.LayoutParams prsp = new LinearLayout.LayoutParams(-1, -2);
        prsp.topMargin = dp(5);
        progress.addView(progressSub, prsp);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, db.total()));
        bar.setProgress(db.done());
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(surfaceAlt));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(7));
        bp.topMargin = dp(14);
        progress.addView(bar, bp);

        sectionLabel("QUICK ACTIONS");
        LinearLayout actions = row();
        actions.addView(actionTile("journal", "Write a journal", "Clear your thoughts", accentSoft, () -> show("Journal")),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        View spacer = new View(this);
        actions.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
        actions.addView(actionTile("plus", "Add a task", "One step at a time", warm, () -> show("Tasks")),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        body.addView(actions, margins(14));

        LinearLayout reminder = row();
        reminder.setBackground(shape(0xFFEAF0EE, 18));
        padding(reminder, 15, 13, 15, 13);
        reminder.addView(new IconView("sparkle", accent), new LinearLayout.LayoutParams(dp(21), dp(21)));
        TextView reminderText = text("You don't need a perfect day. Just a little progress.", 12, ink, true);
        LinearLayout.LayoutParams rtp = new LinearLayout.LayoutParams(0, -2, 1);
        rtp.leftMargin = dp(10);
        reminder.addView(reminderText, rtp);
        body.addView(reminder);
    }

    private LinearLayout actionTile(String icon, String title, String subtitle, int color, Runnable action) {
        LinearLayout tile = column();
        tile.setBackground(ripple(color, 20));
        padding(tile, 14, 13, 13, 12);
        tile.addView(new IconView(icon, accentDark), new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView heading = text(title, 14, ink, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(9);
        tile.addView(heading, hp);
        TextView sub = text(subtitle, 10, muted, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(2);
        tile.addView(sub, sp);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private EditText input(String hint, int minLines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(14);
        e.setMinLines(minLines);
        e.setTextColor(ink);
        e.setHintTextColor(0xFF8A9896);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setBackground(shape(0xFFF3F6F5, 14));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setSingleLine(minLines == 1);
        return e;
    }

    private void primaryButton(LinearLayout parent, String label, Runnable action) {
        TextView button = text(label, 14, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        padding(button, 14, 13, 14, 13);
        button.setBackground(ripple(accent, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(13);
        parent.addView(button, p);
        button.setOnClickListener(v -> action.run());
    }

    private void field(LinearLayout parent, String label, EditText edit) {
        parent.addView(text(label, 13, ink, true));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(7);
        parent.addView(edit, p);
    }

    private void journal() {
        topBar("Journal", "A space for your thoughts");
        pageIntro("DAILY REFLECTION", "Put it into words.", "A few honest lines can help you understand your day.");
        LinearLayout form = card();
        field(form, "01  What went well today?", input("Something I'm grateful for…", 2));
        EditText good = (EditText)form.getChildAt(form.getChildCount() - 1);
        gap(form, 17);
        field(form, "02  What felt difficult?", input("Something that's on my mind…", 3));
        EditText hard = (EditText)form.getChildAt(form.getChildCount() - 1);
        gap(form, 17);
        field(form, "03  One step for tomorrow", input("A small thing I want to try…", 2));
        EditText next = (EditText)form.getChildAt(form.getChildCount() - 1);
        primaryButton(form, "Save my reflection", () -> {
            db.journal(good.getText().toString(), hard.getText().toString(), next.getText().toString());
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(next.getWindowToken(), 0);
            Toast.makeText(this, "Saved on this phone", Toast.LENGTH_SHORT).show();
            show("Journal");
        });
        LinearLayout latest = card();
        latest.addView(text("YOUR MOST RECENT NOTE", 10, accent, true));
        TextView note = text(db.latest().isEmpty() ? "Your saved reflections will appear here." : db.latest(), 12, muted, false);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        latest.addView(note, np);
    }

    private void tasks() {
        topBar("Tasks", "Plan one step at a time");
        pageIntro("YOUR DAILY FOCUS", "Make today count.", "Keep your list realistic. Small wins build momentum.");
        LinearLayout add = card();
        add.addView(text("Add a new task", 16, ink, true));
        EditText taskInput = input("What would you like to get done?", 1);
        LinearLayout.LayoutParams tip = new LinearLayout.LayoutParams(-1, -2);
        tip.topMargin = dp(11);
        add.addView(taskInput, tip);
        primaryButton(add, "＋   Add to my list", () -> {
            String value = taskInput.getText().toString().trim();
            if (!value.isEmpty()) { db.add(value); show("Tasks"); }
            else taskInput.setError("Enter a task first");
        });

        LinearLayout list = card();
        LinearLayout head = row();
        head.addView(text("Today's checklist", 16, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(pill(db.done() + " COMPLETE", accentSoft, accentDark));
        list.addView(head);
        JSONArray items = db.tasks();
        if (items.length() == 0) {
            TextView empty = text("Nothing here yet. Add one task above to get started.", 13, muted, false);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
            ep.topMargin = dp(13);
            list.addView(empty, ep);
        }
        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject item = items.getJSONObject(i);
                int id = item.getInt("id");
                CheckBox check = new CheckBox(this);
                check.setText(item.getString("name"));
                check.setTextSize(14);
                check.setTextColor(ink);
                check.setMinHeight(dp(48));
                check.setButtonTintList(ColorStateList.valueOf(accent));
                check.setChecked(item.getInt("done") == 1);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                cp.topMargin = dp(4);
                list.addView(check, cp);
                check.setOnCheckedChangeListener((button, checked) -> {
                    db.check(id, checked);
                    Toast.makeText(this, checked ? "Nice work!" : "Task reopened", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception ignored) { }
        }
    }

    private void metric(LinearLayout parent, String value, String label, int tint) {
        LinearLayout box = column();
        box.setBackground(shape(tint, 18));
        padding(box, 13, 13, 13, 13);
        box.addView(text(value, 25, ink, true));
        TextView caption = text(label, 11, muted, false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(3);
        box.addView(caption, cp);
        parent.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void weekly() {
        topBar("Weekly review", "Notice your progress");
        pageIntro("YOUR LAST 7 DAYS", "Look how far you've come.", "Use these numbers as information, not a judgement.");
        LinearLayout stats = card();
        LinearLayout first = row();
        metric(first, String.valueOf(db.weekJournals()), "Journal entries", accentSoft);
        first.addView(new View(this), new LinearLayout.LayoutParams(dp(9), 1));
        metric(first, String.valueOf(db.weekDone()), "Tasks completed", warm);
        stats.addView(first);
        LinearLayout second = row();
        LinearLayout.LayoutParams secondp = new LinearLayout.LayoutParams(-1, -2);
        secondp.topMargin = dp(9);
        metric(second, String.valueOf(db.weekTasks()), "Tasks added", surfaceAlt);
        second.addView(new View(this), new LinearLayout.LayoutParams(dp(9), 1));
        int completion = db.weekTasks() == 0 ? 0 : Math.round(100f * db.weekDone() / db.weekTasks());
        metric(second, completion + "%", "Completion", 0xFFE8EDF1);
        stats.addView(second, secondp);

        LinearLayout trend = card();
        LinearLayout th = row();
        th.addView(new IconView("chart", accent), new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView trendTitle = text("Your consistency", 16, ink, true);
        LinearLayout.LayoutParams thp = new LinearLayout.LayoutParams(-2, -2);
        thp.leftMargin = dp(8);
        th.addView(trendTitle, thp);
        trend.addView(th);
        TextView trendSub = text("You completed " + db.weekDone() + " of " + db.weekTasks() + " tasks this week.", 12, muted, false);
        LinearLayout.LayoutParams tsp = new LinearLayout.LayoutParams(-1, -2);
        tsp.topMargin = dp(8);
        trend.addView(trendSub, tsp);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, db.weekTasks()));
        bar.setProgress(db.weekDone());
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(surfaceAlt));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(7));
        bp.topMargin = dp(13);
        trend.addView(bar, bp);

        LinearLayout reflect = card();
        reflect.setBackground(shape(0xFFEAF0EE, 22));
        reflect.addView(text("PAUSE & REFLECT", 10, accent, true));
        TextView prompt = text("What gave you energy this week? What drained it? What's one thing you want to carry into next week?", 14, ink, true);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = dp(8);
        reflect.addView(prompt, pp);
        LinearLayout latest = card();
        latest.addView(text("A note from you", 15, ink, true));
        TextView note = text(db.latest().isEmpty() ? "Your journal entries will show here as you write them." : db.latest(), 12, muted, false);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        latest.addView(note, np);
    }

    private class IconView extends View {
        private final String name;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        IconView(String name, int color) {
            super(MainActivity.this);
            this.name = name;
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.8f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setContentDescription(name + " icon");
        }

        private void line(Canvas c, float x1, float y1, float x2, float y2) {
            c.drawLine(x1, y1, x2, y2, p);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float scale = Math.min(getWidth(), getHeight()) / 24f;
            if (scale <= 0) return;
            c.save();
            c.translate((getWidth() - 24 * scale) / 2f, (getHeight() - 24 * scale) / 2f);
            c.scale(scale, scale);
            switch (name) {
                case "home":
                    path.reset(); path.moveTo(3, 10); path.lineTo(12, 3); path.lineTo(21, 10);
                    c.drawPath(path, p);
                    c.drawRoundRect(5, 9, 19, 21, 2, 2, p);
                    line(c, 10, 21, 10, 15); line(c, 14, 21, 14, 15);
                    break;
                case "journal":
                    c.drawRoundRect(5, 3, 19, 21, 2, 2, p);
                    line(c, 8, 8, 16, 8); line(c, 8, 12, 16, 12); line(c, 8, 16, 13, 16);
                    break;
                case "tasks":
                    c.drawRoundRect(3, 4, 9, 10, 1, 1, p);
                    c.drawRoundRect(3, 14, 9, 20, 1, 1, p);
                    line(c, 4, 7, 6, 9); line(c, 6, 9, 9, 5);
                    line(c, 4, 17, 6, 19); line(c, 6, 19, 9, 15);
                    line(c, 12, 7, 21, 7); line(c, 12, 17, 21, 17);
                    break;
                case "chart":
                    line(c, 3, 21, 21, 21); line(c, 3, 21, 3, 4);
                    c.drawRoundRect(6, 13, 10, 20, 1, 1, p);
                    c.drawRoundRect(12, 9, 16, 20, 1, 1, p);
                    c.drawRoundRect(18, 5, 21, 20, 1, 1, p);
                    break;
                case "plus":
                    line(c, 12, 4, 12, 20); line(c, 4, 12, 20, 12);
                    break;
                case "sparkle":
                    path.reset(); path.moveTo(12, 2); path.quadTo(13, 10, 22, 12);
                    path.quadTo(13, 13, 12, 22); path.quadTo(11, 13, 2, 12);
                    path.quadTo(11, 10, 12, 2); c.drawPath(path, p);
                    break;
                case "leaf":
                    path.reset(); path.moveTo(20, 4); path.cubicTo(7, 4, 3, 10, 4, 18);
                    path.cubicTo(10, 21, 19, 16, 20, 4); c.drawPath(path, p);
                    path.reset(); path.moveTo(5, 19); path.quadTo(10, 12, 17, 7); c.drawPath(path, p);
                    break;
            }
            c.restore();
        }
    }
}
