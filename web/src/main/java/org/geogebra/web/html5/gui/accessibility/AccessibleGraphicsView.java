package org.geogebra.web.html5.gui.accessibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geogebra.common.gui.SliderInput;
import org.geogebra.common.main.App;
import org.geogebra.web.full.gui.layout.panels.HasSliders;
import org.geogebra.web.full.gui.layout.panels.SliderFactory;
import org.geogebra.web.html5.util.sliderPanel.SliderW;

/**
 * Accessibility wrapper for graphics view controls
 */
public class AccessibleGraphicsView implements AccessibleWidget, HasSliders {

	private AccessibilityView view;
	private App app;
	private List<SliderInput> sliderTypes = new ArrayList<>(Arrays.asList(SliderInput.ROTATE_Z, SliderInput.TILT));
	private List<SliderW> sliders = new ArrayList<>();
	// TODO add toggle button for animation, fullscreen button

	/**
	 * @param app           application
	 * @param sliderFactory slider factory
	 * @param view          accessibility view
	 */
	public AccessibleGraphicsView(App app, SliderFactory sliderFactory,
			final AccessibilityView view) {
		this.app = app;
		this.view = view;
		for (int i = 0; i < sliderTypes.size(); i++) {
			sliders.add(sliderFactory.makeSlider(i, this));
		}
		update();
	}

	@Override
	public List<SliderW> getControl() {
		return sliders;
	}

	private void updateNumericRange(SliderW range, SliderInput input) {
		range.setMinimum(input.getMin());
		range.setMaximum(input.getMax());
		range.setStep(1);
		double value = getInitialValue(input);
		range.setValue(value);
		view.updateValueText(range, value, "degrees");
	}

	private double getInitialValue(SliderInput slider) {
		if (slider == SliderInput.ROTATE_Z) {
			return app.getEuclidianView3D().getAngleA();
		}
		if (slider == SliderInput.TILT) {
			return app.getEuclidianView3D().getAngleB();
		}
		return 0;
	}

	@Override
	public void onValueChange(int index, double value) {
		SliderInput input = sliderTypes.get(index);
		app.getAccessibilityManager().sliderChange(value - getInitialValue(input), input);
		sliders.get(index).getElement().focus();
		view.updateValueText(sliders.get(index), value, "degrees");
	}

	@Override
	public void update() {
		for (int i = 0; i < sliderTypes.size(); i++) {
			updateNumericRange(sliders.get(i), sliderTypes.get(i));
		}
	}

}
