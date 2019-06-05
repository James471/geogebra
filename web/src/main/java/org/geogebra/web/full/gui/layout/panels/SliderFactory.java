package org.geogebra.web.full.gui.layout.panels;

import org.geogebra.web.html5.util.sliderPanel.SliderW;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

/**
 * Set of hidden widgets for navigating the construction with Voiceover (iOS)
 * 
 * @author Zbynek
 */
public class SliderFactory {

	/**
	 * For sliders we want more restrictive hide method than
	 * {@link #hideUIElement(Widget)}
	 */
	private static void hideSlider(Widget ui) {
		Style style = ui.getElement().getStyle();
		style.setOpacity(.01);
		style.setPosition(Position.FIXED);
		style.setWidth(1, Unit.PX);
		style.setHeight(1, Unit.PX);
		style.setOverflow(Overflow.HIDDEN);
	}

	/**
	 * @param index  slider identifier in case listener has more sliders
	 * @param source listener
	 * @return slider
	 */
	public SliderW makeSlider(final int index, final HasSliders source) {
		final SliderW range = new SliderW(0, 10);
		hideSlider(range);
		range.getElement().addClassName("slider");
		range.addValueChangeHandler(new ValueChangeHandler<Double>() {

			@Override
			public void onValueChange(ValueChangeEvent<Double> event) {
				source.onValueChange(index, event.getValue());

			}
		});
		range.getElement().setTabIndex(5000);
		return range;
	}

}
