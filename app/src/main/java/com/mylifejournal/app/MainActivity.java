package com.mylifejournal.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
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
    private int bg = 0xFFF7F6FB, ink = 0xFF29243E, muted = 0xFF858197;
    private int accent = 0xFF7667D8, accentDark = 0xFF50439F;
    private int lavender = 0xFFECE9FF, peach = 0xFFFFEDE5, mint = 0xFFE5F4EC;
    private LinearLayout root, body;
    private JournalDb db;
    private JSONObject ui;
    private String page = "Home";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new JournalDb(this);
        loadUiConfig();
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        show("Home");
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
            bg = Color.parseColor(ui.optString("background", "#F7F6FB"));
            accent = Color.parseColor(ui.optString("accent", "#7667D8"));
            accentDark = Color.parseColor(ui.optString("accentDark", "#50439F"));
            ink = Color.parseColor(ui.optString("text", "#29243E"));
            muted = Color.parseColor(ui.optString("muted", "#858197"));
            lavender = Color.parseColor(ui.optString("lavender", "#ECE9FF"));
            peach = Color.parseColor(ui.optString("peach", "#FFEDE5"));
            mint = Color.parseColor(ui.optString("mint", "#E5F4EC"));
        } catch (Exception e) { ui = new JSONObject(); }
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable solid(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        else t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void padding(View v, int left, int top, int right, int bottom) { v.setPadding(dp(left), dp(top), dp(right), dp(bottom)); }
    private void space(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }

    private void show(String target) {
        page = target;
        root = column();
        root.setBackgroundColor(bg);
        setContentView(root);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        body = column();
        padding(body, 20, 16, 20, 20);
        scroller.addView(body);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("Home".equals(target)) home();
        else if ("Journal".equals(target)) journal();
        else if ("Tasks".equals(target)) tasks();
        else weekly();
        bottomNavigation();
    }

    private void bottomNavigation() {
        LinearLayout nav = horizontal();
        nav.setBackgroundColor(Color.WHITE);
        padding(nav, 8, 8, 8, 8);
        String[] fallback = {"Home", "Journal", "Tasks", "Weekly"};
        String[] icons = {"⌂", "✎", "✓", "▥"};
        JSONArray configured = ui.optJSONArray("navigation");
        String[] names = fallback;
        if (configured != null && configured.length() == 4) {
            names = new String[4];
            for (int i = 0; i < 4; i++) names[i] = configured.optString(i, fallback[i]);
        }
        for (int i = 0; i < names.length; i++) {
            final String destination = names[i];
            boolean active = destination.equals(page);
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setBackground(solid(active ? lavender : Color.TRANSPARENT, 16));
            padding(item, 5, 5, 5, 5);
            item.addView(text(icons[i], 21, active ? accentDark : muted, true));
            TextView label = text(destination, 10, active ? accentDark : muted, active);
            item.addView(label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            nav.addView(item, lp);
            item.setOnClickListener(v -> show(destination));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setBackground(solid(Color.WHITE, 22));
        padding(c, 17, 16, 17, 16);
        c.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        body.addView(c, lp);
        return c;
    }

    private TextView pill(String label, int color, int textColor) {
        TextView t = text(label, 11, textColor, true);
        t.setGravity(Gravity.CENTER);
        padding(t, 11, 6, 11, 6);
        t.setBackground(solid(color, 30));
        return t;
    }

    private void sectionHeading(String eyebrow, String heading, String subheading) {
        TextView e = text(eyebrow.toUpperCase(Locale.ROOT), 10, accentDark, true);
        e.setLetterSpacing(.12f);
        body.addView(e);
        TextView h = text(heading, 29, ink, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(4);
        body.addView(h, hp);
        TextView s = text(subheading, 13, muted, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4); sp.bottomMargin = dp(17);
        body.addView(s, sp);
    }

    private void home() {
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        LinearLayout top = horizontal();
        LinearLayout greeting = column();
        greeting.addView(text("A FRESH START", 10, accentDark, true));
        TextView name = text("Hello, Gaurav ✨", 25, ink, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(3);
        greeting.addView(name, np);
        greeting.addView(text(date, 12, muted, false));
        top.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        TextView avatar = text("✦", 23, accentDark, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(solid(lavender, 19));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));
        body.addView(top);
        space(body, 17);

        LinearLayout hero = column();
        hero.setBackground(gradient(0xFF7667D8, 0xFF9B8FEA, 25));
        padding(hero, 19, 18, 19, 18);
        hero.addView(pill("YOUR DAILY RESET", 0x44FFFFFF, Color.WHITE));
        TextView heroTitle = text("A little better, every single day.", 26, Color.WHITE, true);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(-1, -2);
        htp.topMargin = dp(14);
        hero.addView(heroTitle, htp);
        TextView heroSub = text("Small steps today create a life you feel good waking up to.", 13, 0xFFF4F1FF, false);
        LinearLayout.LayoutParams hsp = new LinearLayout.LayoutParams(-1, -2);
        hsp.topMargin = dp(8);
        hero.addView(heroSub, hsp);
        LinearLayout.LayoutParams herop = new LinearLayout.LayoutParams(-1, -2);
        herop.bottomMargin = dp(16);
        body.addView(hero, herop);

        LinearLayout mood = card();
        LinearLayout moodHead = horizontal();
        LinearLayout moodTitles = column();
        moodTitles.addView(text("How's your heart today?", 17, ink, true));
        moodTitles.addView(text("Take a moment to check in.", 12, muted, false));
        moodHead.addView(moodTitles, new LinearLayout.LayoutParams(0, -2, 1));
        moodHead.addView(pill("MOOD", peach, 0xFFB16C53));
        mood.addView(moodHead);
        LinearLayout moods = horizontal();
        String[] emojis = {"😞", "😕", "😐", "🙂", "😊"};
        String[] labels = {"Low", "Meh", "Okay", "Good", "Great"};
        for (int i = 0; i < emojis.length; i++) {
            final String selected = labels[i];
            LinearLayout option = column();
            option.setGravity(Gravity.CENTER);
            TextView face = text(emojis[i], 23, ink, false);
            face.setGravity(Gravity.CENTER);
            face.setBackground(solid(i == 4 ? mint : bg, 15));
            option.addView(face, new LinearLayout.LayoutParams(dp(43), dp(43)));
            TextView label = text(labels[i], 9, muted, false);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(5);
            option.addView(label, lp);
            moods.addView(option, new LinearLayout.LayoutParams(0, -2, 1));
            option.setOnClickListener(v -> {
                db.mood(selected);
                Toast.makeText(this, "Mood saved: " + selected, Toast.LENGTH_SHORT).show();
            });
        }
        LinearLayout.LayoutParams moodp = new LinearLayout.LayoutParams(-1, -2);
        moodp.topMargin = dp(15);
        mood.addView(moods, moodp);

        LinearLayout progress = card();
        LinearLayout prHead = horizontal();
        prHead.addView(text("Today's little wins", 17, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        prHead.addView(pill(db.done() + "/" + db.total() + " DONE", mint, 0xFF478563));
        progress.addView(prHead);
        TextView progressSub = text("Every completed step is worth celebrating.", 12, muted, false);
        LinearLayout.LayoutParams psp = new LinearLayout.LayoutParams(-1, -2);
        psp.topMargin = dp(6);
        progress.addView(progressSub, psp);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, db.total()));
        bar.setProgress(db.done());
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(8));
        bp.topMargin = dp(13);
        progress.addView(bar, bp);

        LinearLayout actions = horizontal();
        LinearLayout journalAction = actionTile("✎", "Write journal", "Clear your mind", lavender, () -> show("Journal"));
        LinearLayout taskAction = actionTile("✓", "Add a task", "One step at a time", peach, () -> show("Tasks"));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, dp(115), 1);
        ap.rightMargin = dp(7);
        actions.addView(journalAction, ap);
        LinearLayout.LayoutParams ap2 = new LinearLayout.LayoutParams(0, dp(115), 1);
        ap2.leftMargin = dp(7);
        actions.addView(taskAction, ap2);
        body.addView(actions);
        space(body, 5);
        LinearLayout quote = card();
        quote.setBackground(solid(0xFFFFF7F0, 20));
        quote.addView(text("A gentle reminder 💛", 14, 0xFF9A684F, true));
        TextView quoteText = text("You don't have to figure out your whole life today. Just take the next kind step.", 14, ink, false);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.topMargin = dp(7);
        quote.addView(quoteText, qp);
    }

    private LinearLayout actionTile(String icon, String title, String subtitle, int color, Runnable action) {
        LinearLayout tile = column();
        tile.setBackground(solid(color, 20));
        padding(tile, 14, 13, 12, 12);
        tile.addView(text(icon, 22, accentDark, true));
        TextView h = text(title, 15, ink, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(9);
        tile.addView(h, hp);
        TextView s = text(subtitle, 10, muted, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(2);
        tile.addView(s, sp);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private EditText input(String hint, int lines) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextSize(14); e.setMinLines(lines);
        e.setTextColor(ink); e.setHintTextColor(0xFFA7A3B4);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setBackground(solid(0xFFF7F6FA, 14));
        padding(e, 13, 12, 13, 12);
        return e;
    }

    private void primaryButton(LinearLayout parent, String label, Runnable action) {
        TextView b = text(label, 14, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        padding(b, 14, 13, 14, 13);
        b.setBackground(gradient(accent, accentDark, 15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        parent.addView(b, lp);
        b.setOnClickListener(v -> action.run());
    }

    private void field(LinearLayout parent, String label, EditText value) {
        parent.addView(text(label, 14, ink, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        parent.addView(value, lp);
    }

    private void journal() {
        sectionHeading("A QUIET MOMENT", "Dear journal…", "Put your thoughts down. This space is just for you.");
        LinearLayout form = card();
        EditText good = input("One thing I'm grateful for…", 2);
        field(form, "🌷  What went well today?", good);
        space(form, 15);
        EditText hard = input("Something that's on my mind…", 3);
        field(form, "☁️  What felt difficult?", hard);
        space(form, 15);
        EditText next = input("One small step for tomorrow…", 2);
        field(form, "🌱  What will I try tomorrow?", next);
        primaryButton(form, "Save my reflection  ✓", () -> {
            db.journal(good.getText().toString(), hard.getText().toString(), next.getText().toString());
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(next.getWindowToken(), 0);
            Toast.makeText(this, "Your reflection is saved on this phone", Toast.LENGTH_SHORT).show();
            show("Journal");
        });
        LinearLayout recent = card();
        recent.addView(text("Your latest reflection", 16, ink, true));
        TextView recentText = text(db.latest().isEmpty() ? "Your saved thoughts will find a home here." : db.latest(), 12, muted, false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(8);
        recent.addView(recentText, rp);
    }

    private void tasks() {
        sectionHeading("ONE STEP AT A TIME", "Today's focus", "A few small wins can make a lovely day.");
        LinearLayout add = card();
        EditText taskInput = input("What would you like to get done?", 1);
        add.addView(taskInput);
        primaryButton(add, "＋  Add to my list", () -> {
            String value = taskInput.getText().toString().trim();
            if (!value.isEmpty()) { db.add(value); show("Tasks"); }
        });
        LinearLayout list = card();
        LinearLayout listHead = horizontal();
        listHead.addView(text("Your checklist", 16, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        listHead.addView(pill(db.done() + " COMPLETE", mint, 0xFF478563));
        list.addView(listHead);
        JSONArray items = db.tasks();
        if (items.length() == 0) {
            TextView empty = text("Nothing on your list yet. Add one gentle goal above 🌼", 13, muted, false);
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
                check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
                check.setChecked(item.getInt("done") == 1);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                cp.topMargin = dp(7);
                list.addView(check, cp);
                check.setOnCheckedChangeListener((button, checked) -> db.check(id, checked));
            } catch (Exception ignored) { }
        }
    }

    private void metric(LinearLayout parent, String value, String label, int color) {
        LinearLayout box = column();
        box.setBackground(solid(color, 18));
        padding(box, 12, 13, 12, 13);
        box.addView(text(value, 24, ink, true));
        TextView l = text(label, 11, muted, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(3);
        box.addView(l, lp);
        parent.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void weekly() {
        sectionHeading("YOUR GROWTH, YOUR PACE", "A week to be proud of", "Look back with kindness, not judgement.");
        LinearLayout summary = card();
        summary.addView(text("Your last 7 days", 17, ink, true));
        LinearLayout metrics = horizontal();
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = dp(14);
        metric(metrics, String.valueOf(db.weekJournals()), "Journal days", lavender);
        View gap = new View(this);
        metrics.addView(gap, new LinearLayout.LayoutParams(dp(9), 1));
        metric(metrics, String.valueOf(db.weekDone()), "Tasks completed", mint);
        summary.addView(metrics, mp);
        LinearLayout metrics2 = horizontal();
        LinearLayout.LayoutParams m2p = new LinearLayout.LayoutParams(-1, -2);
        m2p.topMargin = dp(9);
        metric(metrics2, String.valueOf(db.weekTasks()), "Tasks added", peach);
        View gap2 = new View(this);
        metrics2.addView(gap2, new LinearLayout.LayoutParams(dp(9), 1));
        metric(metrics2, String.valueOf(db.weekTasks() == 0 ? 0 : Math.round(100f * db.weekDone() / db.weekTasks())) + "%", "Completion", 0xFFF1EFF8);
        summary.addView(metrics2, m2p);
        LinearLayout reflection = card();
        reflection.setBackground(solid(0xFFFFF6EF, 21));
        reflection.addView(text("A little reflection 💛", 16, 0xFF9A684F, true));
        TextView prompt = text("What made you smile this week? What felt challenging? What's one thing you want to do differently next week?", 13, ink, false);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = dp(8);
        reflection.addView(prompt, pp);
        LinearLayout latest = card();
        latest.addView(text("A note from you", 16, ink, true));
        TextView note = text(db.latest().isEmpty() ? "Your journal entries will show here as you write them." : db.latest(), 12, muted, false);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        latest.addView(note, np);
    }
}
