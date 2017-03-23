package org.geogebra.web.html5.euclidian;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.euclidian.event.PointerEventType;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.event.HasOffsets;
import org.geogebra.web.html5.event.PointerEvent;
import org.geogebra.web.html5.event.ZeroOffset;

import com.google.gwt.dom.client.Element;

public class MsZoomer {

	private final IsEuclidianController tc;
	private HasOffsets off;

	private static class MsOffset extends ZeroOffset {

		private IsEuclidianController ec;

		public MsOffset(IsEuclidianController ec) {
			this.ec = ec;
		}

		@Override
		public int mouseEventX(int clientX) {
			return clientX + (zoom() - 1);
		}

		private native int zoom() /*-{
			return $wnd.screen.deviceXDPI ? $wnd.screen.deviceXDPI
					/ $wnd.screen.logicalXDPI : 1;
		}-*/;

		@Override
		public int mouseEventY(int clientY) {
			return clientY + (zoom() - 1);
		}

		@Override
		public int touchEventX(int clientX) {
			return mouseEventX(clientX);
		}

		@Override
		public int touchEventY(int clientY) {
			return mouseEventY(clientY);
		}

		@Override
		public int getEvID() {
			return ec.getEvNo();
		}

	}

	public MsZoomer(IsEuclidianController tc, HasOffsets off) {
		this.tc = tc;
		Log.debug("Zoomer for" + off);
		// this.off = (HasOffsets)this.tc;
		this.off = off == null ? new MsOffset(tc) : off;
	}

	private void pointersUp() {
		this.tc.getLongTouchManager().cancelTimer();
		this.tc.setExternalHandling(false);
	}

	private void twoPointersDown(double x1, double y1, double x2, double y2) {
		this.tc.setExternalHandling(true);
		this.tc.twoTouchStart(off.touchEventX((int) x1),
				off.touchEventY((int) y1), off.touchEventX((int) x2),
				off.touchEventY((int) y2));
	}

	private void twoPointersMove(double x1, double y1, double x2, double y2) {
		this.tc.twoTouchMove(off.touchEventX((int) x1),
				off.touchEventY((int) y1), off.touchEventX((int) x2),
				off.touchEventY((int) y2));
	}

	private void singleDown(double x, double y, int type, int modifiers) {
		tc.getOffsets().closePopups();
		PointerEvent e = new PointerEvent(x, y, types[type], off,
				false);
		adjust(e, modifiers);
		this.tc.onPointerEventStart(e);
	}

	private static void adjust(PointerEvent e, int modifiers) {
		if ((modifiers & 8) > 0) {
			e.setAlt(true);
		}
		if ((modifiers & 4) > 0) {
			e.setShift(true);
		}
		if ((modifiers & 2) > 0) {
			e.setControl(true);
		}
		if ((modifiers & 1) > 0) {
			e.setIsRightClick(true);
		}

	}

	private void singleMove(double x, double y, int type, int modifiers) {
		PointerEvent e = new PointerEvent(x, y, types[type], off,
				false);
		adjust(e, modifiers);
		this.tc.onPointerEventMove(e);
	}

	private void singleUp(double x, double y, int type, int modifiers) {
		this.tc.getLongTouchManager().cancelTimer();
		PointerEvent e = new PointerEvent(x, y, types[type], off,
				false);
		adjust(e, modifiers);
		this.tc.onPointerEventEnd(e);
	}

	private final PointerEventType[] types = new PointerEventType[] {
			PointerEventType.MOUSE, PointerEventType.TOUCH,
			PointerEventType.PEN };
	private void setPointerType(int i) {
		if (i == 0) {
			Log.printStacktrace("switch to event type " + types[i]);
		}
		this.tc.setDefaultEventType(types[i]);
	}

	private void startLongTouch(int x, int y) {
		if (this.tc.getMode() == EuclidianConstants.MODE_MOVE) {
			this.tc.getLongTouchManager().scheduleTimer(tc, off.touchEventX(x),
					off.touchEventY(y));
		}
	}

	private void checkMoveLongTouch() {
		if (!tc.isDraggingBeyondThreshold()) {
			/*
			 * this.tc.getLongTouchManager().rescheduleTimerIfRunning(tc, x, y,
			 * false);
			 */
		} else {
			this.tc.getLongTouchManager().cancelTimer();
		}

	}

	public native void reset()/*-{
		$wnd.first = {
			id : -1
		};
		$wnd.second = {
			id : -1
		};
	}-*/;

	public static native void attachTo(Element element, MsZoomer zoomer,
			boolean override) /*-{
		$wnd.first = {
			id : -1
		};
		$wnd.second = {
			id : -1
		};
		var fix = function(name) {
			return $wnd.PointerEvent ? name.toLowerCase() : "MS" + name;
		};
		var getType = function(e){
			if(e.pointerType == 2 || e.pointerType == "touch"){
				return 1;
			}
			if(e.pointerType == "pen" && override){
				return 2;
			}
			return 0;
		};
		
		var getModifiers = function(e){
			var mod = 0;
			if(e.altKey){
				mod+=8;
			}
			if(e.shiftKey){
				mod+=4;
			}
			if(e.ctrlKey){
				mod+=2;
			}
			if(e.button == 2){
				mod+=1;
			}
			return mod;
		};
		element
				.addEventListener(
						fix("PointerMove"),
						function(e) {
							
						//	zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::setPointerType(I)(getType(e));
							if ($wnd.first.id >= 0 && $wnd.second.id >= 0) {
								if ($wnd.second.id === e.pointerId) {
									$wnd.second.x = e.x;
									$wnd.second.y = e.y;
									zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::twoPointersMove(DDDD)($wnd.first.x,
													$wnd.first.y,
													$wnd.second.x,
													$wnd.second.y);
								} else {
									$wnd.first.x = e.x;
									$wnd.first.y = e.y;
								}

							}else{
								if(override){
									e.preventDefault();
									zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::singleMove(DDII)(e.x, e.y, getType(e), getModifiers(e));
								}
							}
							if(override){
								e.preventDefault();
							}
							zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::checkMoveLongTouch()();
							
						});

		element
				.addEventListener(
						fix("PointerDown"),
						function(e) {
							$wnd.pointerCapture = element;
							if ($wnd.first.id >= 0 && $wnd.second.id >= 0) {
								return;
							}
							if ($wnd.first.id >= 0) {
								$wnd.second.id = e.pointerId;
								$wnd.second.x = e.x;
								$wnd.second.y = e.y;
							} else {
								$wnd.first.id = e.pointerId;
								$wnd.first.x = e.x;
								$wnd.first.y = e.y;
							}
							//prevent touch but not mouse: make sure focus is moved
							if(override && getType(e) != 0){
								e.preventDefault();
							}
							zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::setPointerType(I)(getType(e));
							if ($wnd.first.id >= 0 && $wnd.second.id >= 0) {
								zoomer
										.@org.geogebra.web.html5.euclidian.MsZoomer::twoPointersDown(DDDD)($wnd.first.x,
												$wnd.first.y, $wnd.second.x,
												$wnd.second.y);
							} else if(override){
								zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::singleDown(DDII)(e.x, e.y, getType(e), getModifiers(e));
							}
							if (e.pointerType == 2 || e.pointerType == "touch") {
								zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::startLongTouch(II)($wnd.first.x, $wnd.first.y);
							}
							

						});
		function removePointer(out, stopPropagation){
			return function(e) {
				if(!out){
					$wnd.console.log(e);
				}
				if($wnd.pointerCapture != element && !out){
					$wnd.console.log("pointer up no capture");
					return;
				}			
			if ($wnd.first.id == e.pointerId) {
				$wnd.first.id = -1;
			} else {
				$wnd.second.id = -1;
			}
			if(!out && $wnd.second.id < 0 && $wnd.first.id < 0){
				$wnd.pointerCapture = null;
			}
			if(override){
				if(stopPropagation){
					e.stopPropagation();
				}
				if(!out){
					zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::singleUp(DDII)(e.x, e.y, getType(e), getModifiers(e));
				}
			} else {
				zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::pointersUp()();
			}
			zoomer.@org.geogebra.web.html5.euclidian.MsZoomer::setPointerType(I)(getType(e));
		};
		}
				
		element.addEventListener(fix("PointerOut"), removePointer(true));
		if(override){
			$wnd.addEventListener(fix("PointerUp"), removePointer(false));
		}else{
			element.addEventListener(fix("PointerUp"), removePointer(false));	
		}
	}-*/;

}
