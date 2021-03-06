package org.geogebra.web.full.html5;

import org.geogebra.web.full.javax.swing.GCheckmarkMenuItem;
import org.geogebra.web.html5.gui.util.AriaMenuItem;
import org.geogebra.web.resources.SVGResource;

public class GCheckmarkMenuItemMock extends GCheckmarkMenuItem {
	private final String originalTitle;
	private AriaMenuCheckMock menuCheck;

	public GCheckmarkMenuItemMock(String title, SVGResource checkmark, boolean checked) {
		super(title, checkmark, checked);
		originalTitle = title;
		setChecked(checked);
	}

	@Override
	protected AriaMenuItem newMenuItem() {
		menuCheck = new AriaMenuCheckMock(getHTML());
		return menuCheck;
	}

	@Override
	public void setChecked(boolean value) {
		super.setChecked(value);
		menuCheck.setChecked(value);
	}

	@Override
	public boolean isChecked() {
		return menuCheck.isChecked();
	}
}