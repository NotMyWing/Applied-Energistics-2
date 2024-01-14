package appeng.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/**
 * Different implementation of a text field that wraps instead of extends
 * MC's {@link GuiTextField}. This is necessary because of deobfuscated name
 * collision between {@link ITooltip} and GuiTextField, which would cause
 * crashes in an obfuscated environment. Additionally, since we are not extending that
 * class, we can construct this object differently and allow its position to be
 * mutable like most other widgets.
 */
public class MEGuiTooltipTextField implements ITooltip {

    protected GuiTextField field;

    private static final int PADDING = 2;
    private static boolean previousKeyboardRepeatEnabled;
    private static MEGuiTooltipTextField previousKeyboardRepeatEnabledField;
    private String tooltip;
    private int fontPad;

    public int x;
    public int y;
    public int w;
    public int h;

    /**
     * Uses the values to instantiate a padded version of a text field. Pays attention to the '_' caret.
     *
     * @param width   absolute width
     * @param height  absolute height
     * @param tooltip tooltip message
     */
    public MEGuiTooltipTextField(final int width, final int height, final String tooltip) {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        field = new GuiTextField(0, fontRenderer, 0, 0, 0, 0);

        w = width;
        h = height;

        setMessage(tooltip);

        this.fontPad = fontRenderer.getCharWidth('_');

        setDimensionsAndColor();
    }

    public MEGuiTooltipTextField(final int width, final int height) {
        this(width, height, "");
    }

    public MEGuiTooltipTextField() {
        this(0, 0);
    }

    protected void setDimensionsAndColor() {
        field.x = this.x + PADDING;
        field.y = this.y + PADDING;
        field.width = this.w - PADDING * 2 - this.fontPad;
        field.height = this.h - PADDING * 2;
    }

    public void onTextChange(final String oldText) {}

    public void mouseClicked(final int xPos, final int yPos, final int button) {

        if (!this.isMouseIn(xPos, yPos)) {
            setFocused(false);
            return;
        }

        field.setCanLoseFocus(false);
        setFocused(true);

        if (button == 1) {
            setText("");
        } else {
            field.mouseClicked(xPos, yPos, button);
        }

        field.setCanLoseFocus(true);
    }

    /**
     * Checks if the mouse is within the element
     *
     * @param xCoord current x coord of the mouse
     * @param yCoord current y coord of the mouse
     * @return true if mouse position is within the getText field area
     */
    public boolean isMouseIn(final int xCoord, final int yCoord) {
        final boolean withinXRange = this.x <= xCoord && xCoord < this.x + this.w;
        final boolean withinYRange = this.y <= yCoord && yCoord < this.y + this.h;

        return withinXRange && withinYRange;
    }

    public boolean textboxKeyTyped(final char keyChar, final int keyID) {
        if (!isFocused()) {
            return false;
        }

        final String oldText = getText();
        boolean handled = field.textboxKeyTyped(keyChar, keyID);

        if (!handled && (keyID == Keyboard.KEY_RETURN || keyID == Keyboard.KEY_NUMPADENTER
                || keyID == Keyboard.KEY_ESCAPE)) {
            setFocused(false);
        }

        if (handled) {
            onTextChange(oldText);
        }

        return handled;
    }

    public void drawTextBox() {
        if (field.getVisible()) {
            setDimensionsAndColor();
            GuiTextField.drawRect(
                    this.x + 1,
                    this.y + 1,
                    this.x + this.w - 1,
                    this.y + this.h - 1,
                    isFocused() ? 0xFF606060 : 0xFFA8A8A8);
            field.drawTextBox();
        }
    }

    public void setText(String text, boolean ignoreTrigger) {
        final String oldText = getText();

        int currentCursorPos = field.getCursorPosition();
        field.setText(text);
        field.setCursorPosition(currentCursorPos);

        if (!ignoreTrigger) {
            onTextChange(oldText);
        }
    }

    public void setText(String text) {
        setText(text, false);
    }

    public void setCursorPositionEnd() {
        field.setCursorPositionEnd();
    }

    public void setFocused(boolean focus) {
        if (field.isFocused() == focus) {
            return;
        }

        field.setFocused(focus);

        if (focus) {

            if (previousKeyboardRepeatEnabledField == null) {
                previousKeyboardRepeatEnabled = Keyboard.areRepeatEventsEnabled();
            }

            previousKeyboardRepeatEnabledField = this;
            Keyboard.enableRepeatEvents(true);
        } else {

            if (previousKeyboardRepeatEnabledField == this) {
                previousKeyboardRepeatEnabledField = null;
                Keyboard.enableRepeatEvents(previousKeyboardRepeatEnabled);
            }
        }
    }

    public void setMaxStringLength(final int size) {
        field.setMaxStringLength(size);
    }

    public void setEnableBackgroundDrawing(final boolean b) {
        field.setEnableBackgroundDrawing(b);
    }

    public void setTextColor(final int color) {
        field.setTextColor(color);
    }

    public void setCursorPositionZero() {
        field.setCursorPositionZero();
    }

    public boolean isFocused() {
        return field.isFocused();
    }

    public String getText() {
        return field.getText();
    }

    public void setMessage(String t) {
        tooltip = t;
    }

    @Override
    public String getMessage() {
        return tooltip;
    }

    @Override
    public boolean isVisible() {
        return field.getVisible();
    }

    @Override
    public int xPos() {
        return x;
    }

    @Override
    public int yPos() {
        return y;
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }
}
