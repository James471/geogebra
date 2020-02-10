package org.geogebra.web.full.gui.fontmenu;

import org.geogebra.common.euclidian.text.InlineTextController;
import org.geogebra.common.main.App;

import com.google.gwt.core.client.Scheduler;

public class FontCommand implements Scheduler.ScheduledCommand {
	private App app;
	private final InlineTextController textController;
	private final String font;

	public FontCommand(App app, InlineTextController textController, String font) {
		this.app = app;
		this.textController = textController;
		this.font = font;
	}

	@Override
	public void execute() {
		textController.format("font", font);
		app.storeUndoInfo();
	}
}
