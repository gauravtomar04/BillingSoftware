package com.mylifejournal.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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
    private int bg = 0xFFF7F5FA, surface = 0xFFFFFBFE, surfaceVariant = 0xFFF1ECF4;
    private int ink = 0xFF1D1B20, muted = 0xFF625B71, outline = 0xFFCAC4D0;
    private int accent = 0xFF6750A4, accentDark = 0xFF4F378B;
    private int lavender = 0xFFEADDFF, peach = 0xFFFFD8E4, mint = 0xFFDCECE0;
    private LinearLayout root, body;
    private JournalDb db;
    private JSONObject ui;
    private String page = "Home";
    private String selectedMood = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new JournalDb(this);
        loadUiConfig();
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(surface);
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
            bg = Color.parseColor(ui.optString("background", "#F7F5FA"));
            surface = Color.parseColor(ui.optString("surface", "#FFFBFE"));
            surfaceVariant = Color.parseColor(ui.optString("surfaceVariant", "#F1ECF4"));
            accent = Color.parseColor(ui.optString("accent", "#6750A4"));
            accentDark = Color.parseColor(ui.optString("accentDark", "#4F378B"));
            ink = Color.parseColor(ui.optString("text", "#1D1B20"));
            muted = Color.parseColor(ui.optString("muted", "#625B71"));
            outline = Color.parseColor(ui.optString("outline", "#CAC4D0"));
            lavender = Color.parseColor(ui.optString("lavender", "#EADDFF"));
            peach = Color.parseColor(ui.optString("peach", "#FFD8E4"));
            mint = Color.parseColor(ui.optString("mint", "#DCECE0"));
        } catch (Exception e) { ui = new JSONObject(); }
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable solid(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable outlined(int color, int radius) {
        GradientDrawable d = solid(color, radius);
        d.setStroke(dp(1), outline);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radius));
        return d;
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

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void padding(View v, int left, int top, int right, int bottom) {
        v.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void entrance(View v, int index) {
        v.setAlpha(0f);
        v.setTranslationY(dp(10));
        v.animate().alpha(1f).translationY(0f).setStartDelay(index * 35L)
            .setDuration(240).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void press(View v, Runnable action) {
        v.animate().cancel();
        v.animate().scaleX(.975f).scaleY(.975f).setDuration(85)
            .withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(130)
                    .setInterpolator(new DecelerateInterpolator()).start();
                action.run();
            }).start();
    }

    private void show(String target) {
        page = target;
        root = column();
        root.setBackgroundColor(bg);
        setContentView(root);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        body = column();
        padding(body, 20, 16, 20, 22);
        scroller.addView(body);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("Home".equals(target)) home();
        else if ("Journal".equals(target)) journal();
        else if ("Tasks".equals(target)) tasks();
        else weekly();
        for (int i = 0; i < body.getChildCount(); i++) entrance(body.getChildAt(i), i);
        bottomNavigation();
    }

    private void bottomNavigation() {
        LinearLayout nav = horizontal();
        nav.setBackgroundColor(surface);
        nav.setElevation(dp(8));
        padding(nav, 8, 8, 8, 8);
        String[] fallback = {"Home", "Journal", "Tasks", "Weekly"};
        String[] icons = {"home", "journal", "tasks", "chart"};
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
            item.setBackground(solid(active ? lavender : Color.TRANSPARENT, 18));
            padding(item, 5, 7, 5, 6);
            IconView icon = new IconView(icons[i], active ? accentDark : muted);
            item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = text(destination, 10, active ? accentDark : muted, active);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
            labelParams.topMargin = dp(2);
            item.addView(label, labelParams);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            nav.addView(item, lp);
            item.setOnClickListener(v -> press(v, () -> show(destination)));
        }
        nav.setAlpha(0f);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        nav.animate().alpha(1f).setDuration(220).start();
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setBackground( surface == Color.WHITE ? solid(surface, 24) : outlined(surface, 24));
        padding(c, 17, 16, 17, 16);
        c.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        body.addView(c, lp);
        return c;
    }

    private TextView pill(String label, int color, int textColor) {
        TextView t = text(label, 10, textColor, true);
        t.setGravity(Gravity.CENTER);
        padding(t, 11, 7, 11, 7);
        t.setBackground(solid(color, 30));
        return t;
    }

    private void sectionHeading(String eyebrow, String heading, String subheading) {
        TextView e = text(eyebrow.toUpperCase(Locale.ROOT), 10, accentDark, true);
        e.setLetterSpacing(.12f);
        body.addView(e);
        TextView h = text(heading, 28, ink, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(4);
        body.addView(h, hp);
        TextView s = text(subheading, 13, muted, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        sp.bottomMargin = dp(17);
        body.addView(s, sp);
    }

    private void home() {
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        LinearLayout top = horizontal();
        LinearLayout greeting = column();
        greeting.addView(text("A FRESH START", 10, accentDark, true));
        TextView name = text("Hello, Gaurav", 25, ink, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(3);
        greeting.addView(name, np);
        greeting.addView(text(date, 12, muted, false));
        top.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout avatar = new LinearLayout(this);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(solid(lavender, 18));
        avatar.addView(new IconView("sparkle", accentDark), new LinearLayout.LayoutParams(dp(24), dp(24)));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));
        body.addView(top);
        space(body, 17);

        LinearLayout hero = column();
        hero.setBackground(gradient(0xFF6750A4, 0xFF8A78C8, 25));
        padding(hero, 19, 18, 19, 18);
        hero.addView(pill("YOUR DAILY RESET", 0x33FFFFFF, Color.WHITE));
        TextView heroTitle = text("A little better, every single day.", 25, Color.WHITE, true);
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
        moodHead.addView(pill("MOOD", peach, 0xFF7D294B));
        mood.addView(moodHead);
        LinearLayout moods = horizontal();
        String[] emojis = {"😞", "😕", "😐", "🙂", "😊"};
        String[] labels = {"Low", "Meh", "Okay", "Good", "Great"};
        TextView[] faces = new TextView[emojis.length];
        TextView[] moodLabels = new TextView[labels.length];
        for (int i = 0; i < emojis.length; i++) {
            final String selected = labels[i];
            LinearLayout option = column();
            option.setGravity(Gravity.CENTER);
            TextView face = text(emojis[i], 23, ink, false);
            face.setGravity(Gravity.CENTER);
            face.setBackground(solid(selectedMood.equals(selected) ? lavender : surfaceVariant, 15));
            faces[i] = face;
            option.addView(face, new LinearLayout.LayoutParams(dp(43), dp(43)));
            TextView label = text(labels[i], 9, selectedMood.equals(selected) ? accentDark : muted, selectedMood.equals(selected));
            label.setGravity(Gravity.CENTER);
            moodLabels[i] = label;
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.topMargin = dp(5);
            option.addView(label, labelParams);
            moods.addView(option, new LinearLayout.LayoutParams(0, -2, 1));
            option.setOnClickListener(v -> press(v, () -> {
                selectedMood = selected;
                db.mood(selected);
                for (int j = 0; j < faces.length; j++) {
                    boolean chosen = labels[j].equals(selectedMood);
                    faces[j].setBackground(solid(chosen ? lavender : surfaceVariant, 15));
                    moodLabels[j].setTextColor(chosen ? accentDark : muted);
                    moodLabels[j].setTypeface(Typeface.create("sans-serif", chosen ? Typeface.BOLD : Typeface.NORMAL));
                }
                Toast.makeText(this, "Mood saved: " + selected, Toast.LENGTH_SHORT).show();
            }));
        }
        LinearLayout.LayoutParams moodp = new LinearLayout.LayoutParams(-1, -2);
        moodp.topMargin = dp(15);
        mood.addView(moods, moodp);

        LinearLayout progress = card();
        LinearLayout prHead = horizontal();
        prHead.addView(text("Today's little wins", 17, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        prHead.addView(pill(db.done() + "/" + db.total() + " DONE", mint, 0xFF315B3A));
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
        LinearLayout journalAction = actionTile("journal", "Write journal", "Clear your mind", lavender, () -> show("Journal"));
        LinearLayout taskAction = actionTile("plus", "Add a task", "One step at a time", peach, () -> show("Tasks"));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, dp(120), 1);
        ap.rightMargin = dp(7);
        actions.addView(journalAction, ap);
        LinearLayout.LayoutParams ap2 = new LinearLayout.LayoutParams(0, dp(120), 1);
        ap2.leftMargin = dp(7);
        actions.addView(taskAction, ap2);
        body.addView(actions);
        space(body, 5);
        LinearLayout quote = card();
        quote.setBackground(solid(0xFFFFF6F8, 22));
        LinearLayout quoteHead = horizontal();
        quoteHead.addView(new IconView("sparkle", 0xFF9A526C), new LinearLayout.LayoutParams(dp(19), dp(19)));
        TextView quoteTitle = text("A gentle reminder", 14, 0xFF7D294B, true);
        LinearLayout.LayoutParams qtp = new LinearLayout.LayoutParams(-2, -2);
        qtp.leftMargin = dp(7);
        quoteHead.addView(quoteTitle, qtp);
        quote.addView(quoteHead);
        TextView quoteText = text("You don't have to figure out your whole life today. Just take the next kind step.", 14, ink, false);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.topMargin = dp(7);
        quote.addView(quoteText, qp);
    }

    private LinearLayout actionTile(String icon, String title, String subtitle, int color, Runnable action) {
        LinearLayout tile = column();
        tile.setBackground(solid(color, 22));
        padding(tile, 14, 13, 12, 12);
        tile.addView(new IconView(icon, accentDark), new LinearLayout.LayoutParams(dp(25), dp(25)));
        TextView h = text(title, 15, ink, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(9);
        tile.addView(h, hp);
        TextView s = text(subtitle, 10, muted, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(2);
        tile.addView(s, sp);
        tile.setOnClickListener(v -> press(v, action));
        return tile;
    }

    private EditText input(String hint, int lines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(14);
        e.setMinLines(lines);
        e.setTextColor(ink);
        e.setHintTextColor(muted);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setBackground(solid(surfaceVariant, 16));
        padding(e, 13, 12, 13, 12);
        return e;
    }

    private void primaryButton(LinearLayout parent, String label, Runnable action) {
        TextView b = text(label, 14, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        padding(b, 14, 14, 14, 14);
        b.setBackground(solid(accent, 18));
        b.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        parent.addView(b, lp);
        b.setOnClickListener(v -> press(v, action));
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
        field(form, "What went well today?", good);
        space(form, 15);
        EditText hard = input("Something that's on my mind…", 3);
        field(form, "What felt difficult?", hard);
        space(form, 15);
        EditText next = input("One small step for tomorrow…", 2);
        field(form, "What will I try tomorrow?", next);
        primaryButton(form, "Save my reflection", () -> {
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
            else taskInput.setError("Please enter a task");
        });
        LinearLayout list = card();
        LinearLayout listHead = horizontal();
        listHead.addView(text("Your checklist", 16, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        listHead.addView(pill(db.done() + " COMPLETE", mint, 0xFF315B3A));
        list.addView(listHead);
        JSONArray items = db.tasks();
        if (items.length() == 0) {
            TextView empty = text("Nothing on your list yet. Add one gentle goal above.", 13, muted, false);
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
                check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
                check.setChecked(item.getInt("done") == 1);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                cp.topMargin = dp(5);
                list.addView(check, cp);
                check.setOnCheckedChangeListener((button, checked) -> db.check(id, checked));
            } catch (Exception ignored) { }
        }
    }

    private void metric(LinearLayout parent, String value, String label, int color) {
        LinearLayout box = column();
        box.setBackground(solid(color, 20));
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
        LinearLayout summaryHeading = horizontal();
        summaryHeading.addView(new IconView("chart", accentDark), new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView heading = text("Your last 7 days", 17, ink, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-2, -2);
        headingParams.leftMargin = dp(8);
        summaryHeading.addView(heading, headingParams);
        summary.addView(summaryHeading);
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
        metric(metrics2, String.valueOf(db.weekTasks() == 0 ? 0 : Math.round(100f * db.weekDone() / db.weekTasks())) + "%", "Completion", surfaceVariant);
        summary.addView(metrics2, m2p);
        LinearLayout reflection = card();
        reflection.setBackground(solid(0xFFFFF6F8, 22));
        LinearLayout reflectionHead = horizontal();
        reflectionHead.addView(new IconView("sparkle", 0xFF9A526C), new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView reflectionTitle = text("A little reflection", 16, 0xFF7D294B, true);
        LinearLayout.LayoutParams rtp = new LinearLayout.LayoutParams(-2, -2);
        rtp.leftMargin = dp(7);
        reflectionHead.addView(reflectionTitle, rtp);
        reflection.addView(reflectionHead);
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

    private class IconView extends View {
        private final String name;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        IconView(String name, int color) {
            super(MainActivity.this);
            this.name = name;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            setContentDescription(name + " icon");
        }

        private void line(Canvas c, float x1, float y1, float x2, float y2) {
            c.drawLine(x1, y1, x2, y2, paint);
        }

        private void check(Canvas c, float x, float y) {
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + 2.2f, y + 2.2f);
            path.lineTo(x + 6.5f, y - 2.8f);
            c.drawPath(path, paint);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth(), getHeight()) / 24f;
            if (scale <= 0) return;
            canvas.save();
            canvas.translate((getWidth() - 24 * scale) / 2f, (getHeight() - 24 * scale) / 2f);
            canvas.scale(scale, scale);
            switch (name) {
                case "home":
                    path.reset();
                    path.moveTo(3, 10);
                    path.lineTo(12, 3);
                    path.lineTo(21, 10);
                    canvas.drawPath(path, paint);
                    canvas.drawRoundRect(5, 9, 19, 21, 2, 2, paint);
                    line(canvas, 10, 21, 10, 15);
                    line(canvas, 14, 21, 14, 15);
                    break;
                case "journal":
                    canvas.drawRoundRect(5, 3, 19, 21, 2, 2, paint);
                    line(canvas, 8, 8, 16, 8);
                    line(canvas, 8, 12, 16, 12);
                    line(canvas, 8, 16, 13, 16);
                    break;
                case "tasks":
                    canvas.drawRoundRect(3, 4, 9, 10, 1.3f, 1.3f, paint);
                    canvas.drawRoundRect(3, 14, 9, 20, 1.3f, 1.3f, paint);
                    check(canvas, 4, 7);
                    check(canvas, 4, 17);
                    line(canvas, 12, 7, 21, 7);
                    line(canvas, 12, 17, 21, 17);
                    break;
                case "chart":
                    line(canvas, 3, 21, 21, 21);
                    line(canvas, 3, 21, 3, 4);
                    canvas.drawRoundRect(6, 13, 10, 20, 1, 1, paint);
                    canvas.drawRoundRect(12, 9, 16, 20, 1, 1, paint);
                    canvas.drawRoundRect(18, 5, 21, 20, 1, 1, paint);
                    break;
                case "plus":
                    line(canvas, 12, 4, 12, 20);
                    line(canvas, 4, 12, 20, 12);
                    break;
                case "sparkle":
                    path.reset();
                    path.moveTo(12, 2);
                    path.quadTo(13, 10, 22, 12);
                    path.quadTo(13, 13, 12, 22);
                    path.quadTo(11, 13, 2, 12);
                    path.quadTo(11, 10, 12, 2);
                    canvas.drawPath(path, paint);
                    break;
                case "calendar":
                    canvas.drawRoundRect(3, 5, 21, 21, 2, 2, paint);
                    line(canvas, 7, 3, 7, 7);
                    line(canvas, 17, 3, 17, 7);
                    line(canvas, 3, 9, 21, 9);
                    line(canvas, 8, 13, 8, 13);
                    line(canvas, 12, 13, 12, 13);
                    line(canvas, 16, 13, 16, 13);
                    line(canvas, 8, 17, 8, 17);
                    line(canvas, 12, 17, 12, 17);
                    break;
            }
            canvas.restore();
        }
    }
}
