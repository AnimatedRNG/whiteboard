package com.animated.rng.whiteboard.update;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.WhiteboardWindow;
import com.animated.rng.whiteboard.util.Log;
import com.animated.rng.whiteboard.util.ScaledPoint;
import com.animated.rng.whiteboard.util.TextMark;

/**
 * Updates {@link Text} fields and {@link TextMark}s on {@link Whiteboard}.
 * 
 * @author Srinivas Kaza
 */
public class TextUpdate extends WhiteboardUpdate {

	private TextMark textMark;
	private boolean redraw;
	private boolean delete;
	
	// Stops programmatic updates to text fields from triggering modifylisteners
	// because that causes a network feedback loop
	private static boolean ignore;
	
	public TextUpdate() {
		
	}
	
	/**
	 * @param mark {@link TextMark} to update
	 * @param redraw whether or not to redraw text
	 * @param delete whether or not to delete text
	 */
	public TextUpdate(TextMark mark, boolean redraw, boolean delete) {
		this.textMark = mark;
		this.redraw = redraw;
		this.delete = delete;
		this.setUsesGUI(true);
	}
	
	/**
	 * @return whether or not to redraw text
	 */
	public boolean isRedraw() {
		return redraw;
	}

	/**
	 * @param redraw whether or not to redraw text
	 */
	public void setRedraw(boolean redraw) {
		this.redraw = redraw;
	}

	@Override
	public void onUpdate(Whiteboard whiteboard) {
		if (delete)
		{
			whiteboard.deleteText((Long) textMark.id);
			return;
		}
		
		boolean updated = false;
		List<TextMark> textMarks = whiteboard.getTextMarks();
		
		// In retrospect, I probably should have stored the Whiteboard's TextMarks in 
		// a Map<Long, TextMark> rather than a List<TextMark>, as they are demarcated by
		// unique IDs. Fortunately there aren't too many TextMarks, so O(n) isn't too bad.
		for (int i = 0; i < textMarks.size(); i++)
		{
			TextMark mark = textMarks.get(i);
			if (mark.id.equals(textMark.id))
			{
				if (redraw)
					updateText(textMark, whiteboard);
				textMarks.set(i, textMark);
				updated = true;
			}
		}
		
		// If the TextMark already existed and we updated it, then just update the list in Whiteboard
		if (updated)
			whiteboard.setTextMarks(textMarks);
		// Otherwise add a new TextMark to Whiteboard
		else
		{
			textMarks.add(textMark);
			whiteboard.setTextMarks(textMarks);
			makeText(textMark, whiteboard);
		}
	}
	
	/**
	 * Add new {@link TextMark} to {@link Whiteboard}
	 * 
	 * @param mark {@code TextMark} to add to {@code Whiteboard}
	 * @param whiteboard {@code Whiteboard} to put {@code TextMark} upon
	 */
	private void makeText(TextMark mark, Whiteboard whiteboard) {
		Text text = new Text(whiteboard.getCanvas(), SWT.MULTI | SWT.BORDER);
		Point currentDimensions = whiteboard.getCanvasSize();
		Point unscaledBoundaries = mark.getUnscaledBoundaries(currentDimensions);
		Point position = ScaledPoint.toSWTPoint(currentDimensions, mark.getPosition());
		text.setFont(TextMark.changeFontSize(WhiteboardWindow.font, 
				mark.getFontSize(currentDimensions, whiteboard.getCanvas().getDisplay())));
		text.setBounds(position.x, position.y, unscaledBoundaries.x, unscaledBoundaries.y);
		text.setData(mark.id);
		text.setText(mark.getText());
		text.setFocus();
		text.addModifyListener(new EditListener(whiteboard));
		text.addFocusListener(new TextboxFocusListener(whiteboard));
		whiteboard.addTextField(text);
		whiteboard.updateWhiteboard();
	}
	
	/**
	 * @param mark {@link TextMark} to update
	 * @param whiteboard {@link Whiteboard} upon which our {@code TextMark} is updated
	 */
	private void updateText(TextMark mark, Whiteboard whiteboard) {
		List<Text> textFields = whiteboard.getTextFields();
		for (Text field : textFields) {
			if (field.getData().equals(mark.id))
			{
				// Ignore is set to true to avoid tripping the ModifyListener
				ignore = true;
				Point currentDimensions = whiteboard.getCanvasSize();
				Point unscaledBoundaries = mark.getUnscaledBoundaries(currentDimensions);
				Point position = ScaledPoint.toSWTPoint(currentDimensions, mark.getPosition());
				field.setFont(TextMark.changeFontSize(WhiteboardWindow.font, 
						mark.getFontSize(currentDimensions, whiteboard.getCanvas().getDisplay())));
				field.setBounds(position.x, position.y, unscaledBoundaries.x, unscaledBoundaries.y);
				if (!mark.getText().equals(field.getText()))
					field.setText(mark.getText());
				whiteboard.updateWhiteboard();
				ignore = false;
				break;
			}
		}
	}
	
	/**
	 * Listener that checks if the user deleted all the text in a {@link Text} and unfocused, and then deletes
	 * that {@code Text}
	 * 
	 * @author Srinivas Kaza
	 */
	private class TextboxFocusListener implements FocusListener {
		private Whiteboard whiteboard;
		
		public TextboxFocusListener(Whiteboard whiteboard) {
			this.whiteboard = whiteboard;
		}
		
		@Override
		public void focusGained(FocusEvent e) {
			
		}
		
		@Override
		public void focusLost(FocusEvent e) {
			Text field = (Text) e.widget;
			if (field.getText().isEmpty())
				whiteboard.addUpdate(new TextUpdate(this.whiteboard.getTextMark((Long) field.getData()), false, true), true);
		}
	}
	
	/**
	 * Checks for edits to {@link Text} fields and then sends updates.
	 * 
	 * @author Srinivas Kaza
	 */
	private class EditListener implements ModifyListener {

		private Whiteboard whiteboard;
		private String lastText;
		
		public EditListener(Whiteboard whiteboard) {
			this.whiteboard = whiteboard;
			this.lastText = new String();
		}
		
		@Override
		public void modifyText(ModifyEvent e) {
			if (whiteboard == null)
				Log.warn("ForwardingListener", "Whiteboard isn't stored when modifying text! Ignoring update.");
			else
			{
				if (ignore)
					return;
				
				Point currentDimensions = this.whiteboard.getCanvasSize();
				Text modifiedText = (Text) e.widget;
				TextMark originalMark = whiteboard.getTextMark((Long) modifiedText.getData());
				
				// Padding is needed to get rid of the annoying shaking effect
				int padding = (int) (TextMark.PADDING * currentDimensions.y);
				int height = modifiedText.getLineCount() * modifiedText.getLineHeight() + padding;
				modifiedText.setSize(modifiedText.getSize().x, height);
				Point boundaries = modifiedText.getSize();
				
				if (!this.lastText.equals(modifiedText.getText()))
					this.whiteboard.addUpdate(new TextUpdate(new TextMark(originalMark.getPosition(), modifiedText.getText(), 
							ScaledPoint.toScaledPoint(currentDimensions, new Point(boundaries.x, boundaries.y)),
							/*originalMark.getBoundaries(),*/ (Long) modifiedText.getData()), false, false), true);
				
				this.lastText = modifiedText.getText();
			}
		}
	}
}
