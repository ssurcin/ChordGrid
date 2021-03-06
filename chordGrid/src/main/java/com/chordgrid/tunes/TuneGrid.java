package com.chordgrid.tunes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.chordgrid.model.Line;
import com.chordgrid.model.Measure;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TunePart;
import com.chordgrid.util.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.view.GestureDetector.SimpleOnGestureListener;

public class TuneGrid extends View {

    private static final String TAG = "TuneGrid";
    private static final float REPEAT_BORDER_GAP = 2.0f;
    private static final int REPEAT_PADDING = 10;
    private static final float REPEAT_DOT_RADIUS = 5.0f;
    /**
     * The usual number of measures per line.
     */
    private static final int USUAL_MEASURES_PER_LINE = 8;
    private static Paint paintBorder = new Paint();
    private static Paint paintWhiteFill = new Paint();
    private static Paint paintGreyFill = new Paint();
    private static Paint paintLabel = new Paint();
    private static Paint paintLabelFill = new Paint();
    private static Typeface tfLabel = Typeface.DEFAULT_BOLD;
    private static Paint paintChord = new Paint();

    static {
        paintBorder.setColor(Color.BLACK);

        paintWhiteFill.setColor(Color.WHITE);
        paintGreyFill.setColor(Color.GRAY);

        paintLabel.setStyle(Style.STROKE);
        paintLabel.setColor(Color.BLACK);
        paintLabel.setTypeface(tfLabel);
        paintLabel.setTextAlign(Align.CENTER);

        paintLabelFill.setStyle(Style.FILL);

        paintChord.setAntiAlias(true);
        paintChord.setColor(Color.BLACK);
        paintChord.setTypeface(Typeface.DEFAULT_BOLD);
        paintChord.setTextAlign(Align.CENTER);
    }

    private final int MIN_TEXT_SIZE = 12;
    private final HashMap<Rect, TunePart> mPartLabelAreas = new HashMap<Rect, TunePart>();
    private final HashMap<Rect, ContextLine> mLineAreas = new HashMap<Rect, ContextLine>();
    private final HashMap<Rect, ContextMeasure> mMeasureAreas = new HashMap<Rect, ContextMeasure>();
    private final Point mMeasureOrigin = new Point();
    private int maxMeasuresPerLine = USUAL_MEASURES_PER_LINE;
    /**
     * The displayed tune.
     */
    private Tune tune;
    private int viewWidth;
    private int viewHeight;
    private int measureWidth;
    private float chordUsableMeasureWidth;
    private int labelAreaWidth;
    private float labelTextSize;
    private Rect textBounds = new Rect();
    private OnSelectMeasureHandler mSelectMeasureHandler;
    private OnSelectPartHandler mSelectPartHandler;

    private boolean mPartialRedraw;
    private final Rect mClipRedraw = new Rect();

    /**
     * A gesture detector to handle presses and gestures over the screen.
     * In particular here we handle long presses to select a tune's components.
     */
    private final GestureDetector mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            TunePart part = isOnPartLabel(e);
            if (part != null) {
                Log.d(TAG, String.format("Long press on part label %s", part.getLabel()));
                if (mSelectPartHandler != null)
                    mSelectPartHandler.selectPart(part.getLabel());
                return;
            }

            ContextMeasure contextMeasure = isOnMeasureBox(e);
            if (contextMeasure != null) {
                Log.d(TAG, String.format("Long press on measure box '%s'", contextMeasure.getMeasure()));

                Rect surroundingMeasureBox = contextMeasure.getSurroundingMeasureBox();
                if (surroundingMeasureBox != null) {
                    invalidate(surroundingMeasureBox.left, surroundingMeasureBox.top, surroundingMeasureBox.right, surroundingMeasureBox.bottom);
                    mClipRedraw.set(surroundingMeasureBox);
                    mPartialRedraw = true;
                }

                if (mSelectMeasureHandler != null) {
                    mSelectMeasureHandler.selectMeasure(contextMeasure.getPartIndex(), contextMeasure.getLineIndex(), contextMeasure.getMeasureIndex());
                }
                return;
            }

            ContextLine contextLine = isOnLineDelimiter(e);
            if (contextLine != null) {
                Log.d(TAG, String.format("Long press on line delimiter %s", contextLine.line));
                return;
            }

            super.onLongPress(e);
        }
    }
    );

    public TuneGrid(Context context) {
        super(context);
    }

    public TuneGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Point getRelativePosition(View v, MotionEvent event) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        float screenX = event.getRawX();
        float screenY = event.getRawY();
        float viewX = screenX - location[0];
        float viewY = screenY - location[1];
        return new Point((int) viewX, (int) viewY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private void clearAreaMaps() {
        mPartLabelAreas.clear();
        mLineAreas.clear();
        mMeasureAreas.clear();
    }

    public void setOnSelectMeasureHandler(OnSelectMeasureHandler onSelectMeasureHandler) {
        mSelectMeasureHandler = onSelectMeasureHandler;
    }

    public void setOnSelectPartHandler(OnSelectPartHandler onSelectPartHandler) {
        mSelectPartHandler = onSelectPartHandler;
    }

    public Tune getTune() {
        return tune;
    }

    public void setTune(Tune tune) {
        this.tune = tune;
        maxMeasuresPerLine = Math.max(USUAL_MEASURES_PER_LINE, tune.getMaxMeasuresPerLine());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        WindowManager wm = (WindowManager) this.getContext().getSystemService(
                Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;

        if (widthSpecMode == MeasureSpec.EXACTLY)
            width = widthSpecSize;
        else {
            width = display.getWidth();
            if (widthSpecMode == MeasureSpec.AT_MOST)
                width = Math.min(widthSpecSize, width);
        }

        int labelWidth = Math.max(20, width / 20);
        int barWidth = (width - labelWidth) / maxMeasuresPerLine;

        if (heightSpecMode == MeasureSpec.EXACTLY)
            height = heightSpecSize;
        else {
            int nbLines = getTune() == null ? 0 : getTune().countTotalLines();
            height = nbLines * barWidth + getPaddingTop() + getPaddingBottom();
            if (heightSpecMode == MeasureSpec.AT_MOST)
                height = Math.min(height, heightSpecSize);
        }

        setMeasuredDimension(width, height);
        Log.d(LogUtils.getTag(), String.format("TuneGrid dimensions = %dx%d", width, height));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The width of the area designated for the part labels
        // is 5% of the view width (20dp minimum).
        int usableWidth = viewWidth - getPaddingLeft() - getPaddingRight() - 1;

        labelAreaWidth = Math.max(20, usableWidth / 20);
        labelTextSize = Math.max(20, labelAreaWidth / 2);
        paintLabel.setTextSize(labelTextSize);

        Tune tune = getTune();
        if (tune != null) {
            measureWidth = (usableWidth - labelAreaWidth) / maxMeasuresPerLine;
            chordUsableMeasureWidth = measureWidth - 2 * (REPEAT_PADDING + REPEAT_DOT_RADIUS + 2);

            if (mPartialRedraw) {
                for (ContextMeasure contextMeasure : getAllMeasuresInRect(mClipRedraw)) {
                    Rect boundingRect = contextMeasure.getSurroundingMeasureBox();
                    mMeasureOrigin.set(boundingRect.left, boundingRect.top);
                    drawMeasure(canvas, contextMeasure);
                }
            } else {
                mMeasureOrigin.set(getPaddingLeft() + labelAreaWidth, getPaddingTop());

                clearAreaMaps();

                int countParts = tune.countParts();
                for (int partIndex = 0; partIndex < countParts; partIndex++) {
                    TunePart part = tune.getPart(partIndex);
                    drawPartLabel(canvas, part);

                    int countLines = part.countLines();
                    for (int lineIndex = 0; lineIndex < countLines; lineIndex++) {
                        Line line = part.getLine(lineIndex);

                        float lineX = mMeasureOrigin.x, lineY = mMeasureOrigin.y;

                        List<Measure> measures = line.getMeasures();
                        int countMeasures = measures.size();
                        for (int i = 0; i < countMeasures; i++) {
                            drawMeasure(canvas, tune, partIndex, lineIndex, i, true);
                        }
                        mMeasureOrigin.x = getPaddingLeft() + labelAreaWidth;
                        mMeasureOrigin.y += measureWidth;

                        // Remember this line's area in relative coordinates
                        mLineAreas.put(new Rect((int) lineX, (int) lineY, (int) (lineX + measureWidth), (int) (lineY + measureWidth)), new ContextLine(part, line));
                    }
                }
            }
        }

        mPartialRedraw = false;

        super.onDraw(canvas);
    }

    private ContextLine isOnLineDelimiter(MotionEvent e) {
        Point p = getRelativePosition(this, e);
        for (Rect rect : mLineAreas.keySet()) {
            if (rect.contains(p.x, p.y)) {
                if (p.x >= rect.left && p.x < rect.left + 2)
                    return mLineAreas.get(rect);
                if (p.x <= rect.right && p.x > rect.right - 2)
                    return mLineAreas.get(rect);
                return null;
            }
        }
        return null;
    }

    private void drawPartLabel(Canvas canvas, TunePart tunePart) {
        paintLabelFill.setColor(Color.rgb(0xd3, 0xfa, 0x66));
        float radius = labelAreaWidth / 2 - 2;
        float xCenter = getPaddingLeft() + radius + 2;
        float yCenter = mMeasureOrigin.y + measureWidth / 2.0f;
        canvas.drawCircle(xCenter, yCenter, radius, paintLabelFill);
        canvas.drawCircle(xCenter, yCenter, radius, paintLabel);
        float x1 = xCenter - radius, y1 = yCenter - radius;
        float x2 = xCenter + radius, y2 = yCenter + radius;
        drawCenteredText(canvas, tunePart.getLabel(), x1, y1, x2, y2, paintLabel);

        // Remember which area is devoted to this part's label (in relative coordinates)
        mPartLabelAreas.put(new Rect((int) x1, (int) y1, (int) x2, (int) y2), tunePart);
    }

    private TunePart isOnPartLabel(MotionEvent e) {
        Point p = getRelativePosition(this, e);
        for (Rect rect : mPartLabelAreas.keySet()) {
            if (rect.contains(p.x, p.y))
                return mPartLabelAreas.get(rect);
        }
        return null;
    }

    private void drawMeasure(Canvas canvas, Tune tune, int partIndex, int lineIndex, int measureIndex, boolean recordContextMeasure) {

        drawMeasureBox(canvas, tune, partIndex, lineIndex, measureIndex, recordContextMeasure);

        Measure measure = tune.getPart(partIndex).getLine(lineIndex).getMeasure(measureIndex);
        switch (measure.countChords()) {
            case 1:
                drawChord1(measure, canvas);
                break;
            case 2:
                drawChord2(measure, canvas);
                break;
            case 3:
            case 4:
                drawChord4(measure, canvas);
                break;
        }

        mMeasureOrigin.x += measureWidth;
    }

    private void drawMeasure(Canvas canvas, ContextMeasure contextMeasure) {
        drawMeasure(canvas, contextMeasure.getTune(), contextMeasure.getPartIndex(), contextMeasure.getLineIndex(), contextMeasure.getMeasureIndex(), false);
    }

    private void drawMeasureBox(Canvas canvas, Tune tune, int partIndex, int lineIndex, int measureIndex, boolean recordContextMeasure) {
        float x1 = mMeasureOrigin.x;
        float y1 = mMeasureOrigin.y;
        float x2 = x1 + measureWidth;
        float y2 = y1 + measureWidth;
        float yCenter = y1 + measureWidth / 2.0f;

        canvas.drawRect(x1, y1, x2, y2, paintWhiteFill);
        canvas.drawLine(x1, y1, x2, y1, paintBorder);
        canvas.drawLine(x2, y2, x1, y2, paintBorder);

        float defaultStrokeWidth = paintLabel.getStrokeWidth();

        float xInnerLine;
        float xr, yr;

        // Remember the measure box's relative coordinates
        Rect measureBox = new Rect((int) x1 + 2, (int) y1 + 2, (int) x2 - 2, (int) y2 - 2);
        ContextMeasure contextMeasure = new ContextMeasure(tune, partIndex, lineIndex, measureIndex, measureBox);
        if (recordContextMeasure)
            mMeasureAreas.put(measureBox, contextMeasure);

        switch (contextMeasure.getMeasureStyle()) {
            case REPEAT_LEFT:
                paintBorder.setStrokeWidth(REPEAT_BORDER_GAP);
                canvas.drawLine(x1, y1, x1, y2, paintBorder);
                paintBorder.setStrokeWidth(defaultStrokeWidth);
                xInnerLine = x1 + 2 * REPEAT_BORDER_GAP;
                canvas.drawLine(xInnerLine, y1, xInnerLine, y2, paintBorder);

                canvas.drawLine(x2, y1, x2, y2, paintBorder);

                paintBorder.setStyle(Style.FILL);
                xr = x1 + REPEAT_PADDING;
                yr = yCenter - REPEAT_DOT_RADIUS * 2;
                canvas.drawCircle(xr, yr, REPEAT_DOT_RADIUS, paintBorder);
                yr = yCenter + REPEAT_DOT_RADIUS * 2;
                canvas.drawCircle(xr, yr, REPEAT_DOT_RADIUS, paintBorder);
                paintBorder.setStyle(Style.STROKE);
                break;

            case REPEAT_RIGHT:
                paintBorder.setStrokeWidth(REPEAT_BORDER_GAP);
                canvas.drawLine(x2, y1, x2, y2, paintBorder);
                paintBorder.setStrokeWidth(defaultStrokeWidth);
                xInnerLine = x2 - 2 * REPEAT_BORDER_GAP;
                canvas.drawLine(xInnerLine, y1, xInnerLine, y2, paintBorder);

                canvas.drawLine(x1, y2, x1, y1, paintBorder);

                paintBorder.setStyle(Style.FILL);
                xr = x2 - REPEAT_PADDING;
                yr = yCenter - REPEAT_DOT_RADIUS * 2;
                canvas.drawCircle(xr, yr, REPEAT_DOT_RADIUS, paintBorder);
                yr = yCenter + REPEAT_DOT_RADIUS * 2;
                canvas.drawCircle(xr, yr, REPEAT_DOT_RADIUS, paintBorder);
                paintBorder.setStyle(Style.STROKE);
                break;

            default:
                paintBorder.setStyle(Style.STROKE);
                paintBorder.setStrokeWidth(defaultStrokeWidth);
                canvas.drawLine(x2, y1, x2, y2, paintBorder);
                canvas.drawLine(x1, y2, x1, y1, paintBorder);
                break;
        }
    }

    private ContextMeasure isOnMeasureBox(MotionEvent e) {
        Point p = getRelativePosition(this, e);
        for (Rect rect : mMeasureAreas.keySet()) {
            if (rect.contains(p.x, p.y))
                return mMeasureAreas.get(rect);
        }
        return null;
    }

    private List<ContextMeasure> getAllMeasuresInRect(Rect rect) {
        ArrayList<ContextMeasure> measures = new ArrayList<ContextMeasure>();
        for (Rect measureRect : mMeasureAreas.keySet()) {
            if (rect.intersect(measureRect))
                measures.add(mMeasureAreas.get(measureRect));
        }
        return measures;
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y,
                                  float x2, float y2, Paint paint) {
        float centeredX = (x + x2) / 2.0f;
        paint.getTextBounds(text, 0, text.length(), textBounds);
        float centeredY = (y + y2) / 2.0f - (textBounds.bottom + textBounds.top)
                / 2.0f;
        canvas.drawText(text, centeredX, centeredY, paint);
    }

    private void drawCenteredText(Canvas canvas, String text, RectF rect, Paint paint) {
        drawCenteredText(canvas, text, rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    private void drawVerticallyCenteredText(Canvas canvas, String text,
                                            float x, float y, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        float centeredY = y - (textBounds.bottom + textBounds.top) / 2.0f;
        canvas.drawText(text, x, centeredY, paint);
    }

    private RectF getUsableChordRect() {
        float offset = REPEAT_PADDING + REPEAT_DOT_RADIUS + 2;
        return new RectF(mMeasureOrigin.x + offset, mMeasureOrigin.y + offset, mMeasureOrigin.x + measureWidth - offset, mMeasureOrigin.y + measureWidth - offset);
    }

    private void drawChord1(Measure measure, Canvas canvas) {
        paintChord.setTextSize(chordUsableMeasureWidth / 2);
        String chord = measure.getChords().get(0).getValue();
        drawCenteredText(canvas, chord, getUsableChordRect(), paintChord);
    }

    private float getSuitableTextSizeChord2(Measure measure) {
        String largestChord = measure.getLargestChordText();
        int n = largestChord.length();

        float savedTextSize = paintChord.getTextSize();

        float textSize = chordUsableMeasureWidth / 2;
        float l = 3.0f * measureWidth / 4;
        Rect bounds = new Rect();
        do {
            paintChord.setTextSize(textSize);
            paintChord.getTextBounds(largestChord, 0, n, bounds);
            float maxWidth = l - bounds.height();
            if (bounds.width() >= maxWidth)
                textSize--;
            else
                break;
        } while (textSize > MIN_TEXT_SIZE);

        paintChord.setTextSize(savedTextSize);

        return textSize;
    }

    private void drawChord2(Measure measure, Canvas canvas) {
        paintChord.setTextSize(getSuitableTextSizeChord2(measure));

        // Draw the line splitting the measure in 2
        float x = mMeasureOrigin.x;
        float y = mMeasureOrigin.y;
        float x2 = x + measureWidth;
        float y2 = y + measureWidth;
        canvas.drawLine(x2, y, x, y2, paintBorder);

        // Draw the first chord on the upper left side
        String chord1 = measure.getChords().get(0).getValue();
        float tx1 = x + 3.0f * measureWidth / 8.0f;
        float ty1 = y + measureWidth / 4.0f;
        drawVerticallyCenteredText(canvas, chord1, tx1, ty1, paintChord);

        String chord2 = measure.getChords().get(1).getValue();
        float tx2 = x2 - 3.0f * measureWidth / 8.0f;
        float ty2 = y2 - measureWidth / 4.0f;
        drawVerticallyCenteredText(canvas, chord2, tx2, ty2, paintChord);
    }

    private float getSuitableTextSizeChord4(Measure measure) {
        String largestChord = measure.getLargestChordText();
        int n = largestChord.length();

        float savedTextSize = paintChord.getTextSize();

        float textSize = chordUsableMeasureWidth / 2.0f;
        float l = 2.0f * measureWidth / 3;
        Rect bounds = new Rect();
        do {
            paintChord.setTextSize(textSize);
            paintChord.getTextBounds(largestChord, 0, n, bounds);
            float maxWidth = l - bounds.height();
            if (bounds.width() >= maxWidth)
                textSize--;
            else
                break;
        } while (textSize > MIN_TEXT_SIZE);

        paintChord.setTextSize(savedTextSize);

        return textSize;
    }

    private void drawChord4(Measure measure, Canvas canvas) {
        paintChord.setTextSize(getSuitableTextSizeChord4(measure));

        // Draw the lines splitting the measure in 4
        float x = mMeasureOrigin.x;
        float y = mMeasureOrigin.y;
        float x2 = x + measureWidth;
        float y2 = y + measureWidth;
        canvas.drawLine(x2, y, x, y2, paintBorder);
        canvas.drawLine(x, y, x2, y2, paintBorder);

        String chord1 = measure.getChords().get(0).getValue();
        float tx1 = x + measureWidth / 4.0f;
        float ty1 = y + measureWidth / 2.0f;
        drawVerticallyCenteredText(canvas, chord1, tx1, ty1, paintChord);

        String chord2 = measure.getChords().get(1).getValue();
        float tx2 = x + measureWidth / 2.0f;
        float ty2 = y + measureWidth / 6.0f;
        drawVerticallyCenteredText(canvas, chord2, tx2, ty2, paintChord);

        String chord3 = measure.getChords().get(2).getValue();
        float tx3 = x2 - measureWidth / 4.0f;
        float ty3 = y + measureWidth / 2.0f;
        drawVerticallyCenteredText(canvas, chord3, tx3, ty3, paintChord);

        if (measure.countChords() > 3) {
            String chord4 = measure.getChords().get(3).getValue();
            float tx4 = x + measureWidth / 2.0f;
            float ty4 = y2 - measureWidth / 6.0f;
            drawVerticallyCenteredText(canvas, chord4, tx4, ty4, paintChord);
        }
    }

    private enum MeasureStyle {
        NORMAL, REPEAT_LEFT, REPEAT_RIGHT
    }

    public interface OnSelectPartHandler {
        void selectPart(String partLabel);
    }

    public interface OnSelectMeasureHandler {
        void selectMeasure(int partIndex, int lineIndex, int measureIndex);
    }

    private class ContextLine {
        public TunePart part;
        public Line line;

        public ContextLine(TunePart part, Line line) {
            this.part = part;
            this.line = line;
        }
    }

    private class ContextMeasure {
        private Tune mTune;
        private int mPartIndex;
        private int mLineIndex;
        private int mMeasureIndex;
        private Rect mMeasureBox;

        public ContextMeasure(Tune tune, int partIndex, int lineIndex, int measureIndex, Rect measureBox) {
            mTune = tune;
            mPartIndex = partIndex;
            mLineIndex = lineIndex;
            mMeasureIndex = measureIndex;
            mMeasureBox = measureBox;
        }

        public Tune getTune() {
            return mTune;
        }

        public int getPartIndex() {
            return mPartIndex;
        }

        public int getLineIndex() {
            return mLineIndex;
        }

        public int getMeasureIndex() {
            return mMeasureIndex;
        }

        public TunePart getPart() {
            return mTune.getPart(mPartIndex);
        }

        public Line getLine() {
            return getPart().getLine(mLineIndex);
        }

        public Measure getMeasure() {
            return getLine().getMeasure(mMeasureIndex);
        }

        public Rect getMeasureBox() {
            return mMeasureBox;
        }

        public Rect getSurroundingMeasureBox() {
            if (mMeasureBox == null)
                return null;
            return new Rect(mMeasureBox.left - 2, mMeasureBox.top - 2, mMeasureBox.right + 2, mMeasureBox.bottom + 2);
        }

        public MeasureStyle getMeasureStyle() {
            Line line = getLine();
            boolean hasRepetition = line.hasRepetition();
            if (mMeasureIndex == 0 && hasRepetition)
                return MeasureStyle.REPEAT_LEFT;
            else if (mMeasureIndex == line.countMeasures() - 1 && hasRepetition)
                return MeasureStyle.REPEAT_RIGHT;
            return MeasureStyle.NORMAL;
        }
    }
}
