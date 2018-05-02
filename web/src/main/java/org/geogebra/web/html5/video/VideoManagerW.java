package org.geogebra.web.html5.video;

import java.util.HashMap;
import java.util.Map;

import org.geogebra.common.awt.MyImage;
import org.geogebra.common.kernel.geos.GeoVideo;
import org.geogebra.common.main.App;
import org.geogebra.common.sound.VideoManager;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.css.GuiResourcesSimple;
import org.geogebra.web.html5.gui.GeoGebraFrameW;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.main.MyImageW;
import org.geogebra.web.resources.JavaScriptInjector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Class for managing audio content.
 * 
 * @author laszlo
 *
 */
public class VideoManagerW implements VideoManager {
	private static boolean youTubeAPI = false;
	private AsyncOperation<Boolean> urlCallback = null;
	/**
	 * Head of a regular YouTube URL.
	 */
	public static final String YOUTUBE = "youtube.com/";
	/**
	 * Head of a short form of YouTube URL.
	 */
	public static final String YOUTUBE_SHORT = "youtu.be/";

	/**
	 * regular start of YouTube ID
	 */
	public static final String ID_PARAM_1 = "v=";
	/**
	 * alternative start of YouTube ID
	 */
	public static final String ID_PARAM_2 = "v/";
	/**
	 * embed start of YouTube ID
	 */
	public static final String EMBED = "embed/";

	/**
	 * true if only preview images are needed (i.e. for image export)
	 */
	private boolean previewOnly = false;

	private Map<String, VideoPlayer> players = new HashMap<>();

	/**
	 * Constructor
	 */
	public VideoManagerW() {
		JavaScriptInjector.inject(GuiResourcesSimple.INSTANCE.youtube());
		loadYouTubeApi();
	}

	@Override
	public void loadGeoVideo(GeoVideo geo) {
		addPlayer(geo);
		updatePlayer(geo);
	}

	@Override
	public void checkURL(String url, AsyncOperation<Boolean> callback) {
		urlCallback = callback;
		checkVideo(url);
	}

	private void checkVideo(String url) {
		if (getYouTubeId(url) == null || "".equals(getYouTubeId(url))) {
			onUrlError();
		} else {
			onUrlOK();
		}
	}

	private void onUrlError() {
		if (urlCallback != null) {
			urlCallback.callback(Boolean.FALSE);
		}
	}

	private void onUrlOK() {
		if (urlCallback != null) {
			urlCallback.callback(Boolean.TRUE);
		}
	}

	@Override
	public void play(GeoVideo video) {
		if (video == null) {
			return;
		}
		if (!(hasPlayer(video)
				&& isPlayerValid(getPlayer(video).getElement()))) {
			addPlayer(video);
		}

		getPlayer(video).play();
	}

	@Override
	public void pause(GeoVideo video) {
		if (video == null || !hasPlayer(video)) {
			return;
		}
		getPlayer(video).pause();
		video.getKernel().getApplication().getActiveEuclidianView().repaintView();
	}

	@Override
	public void background(GeoVideo video) {
		if (video == null || !hasPlayer(video)) {
			return;
		}
		video.setBackground(true);
		updatePlayer(video);
		video.getKernel().getApplication().getActiveEuclidianView().repaintView();
	}

	@Override
	public String getYouTubeId(String url) {
		String id = null;
		int startIdx;
		String subString = null;

		if (url.contains(YOUTUBE)) {
			if (url.contains(ID_PARAM_1) || url.contains(ID_PARAM_2)) {
				startIdx = url.indexOf(ID_PARAM_1) != -1
						? url.indexOf(ID_PARAM_1) : url.indexOf(ID_PARAM_2);
				subString = url.substring(startIdx + ID_PARAM_1.length());

			} else if (url.contains(EMBED)) {
				startIdx = url.indexOf(EMBED);
				subString = url.substring(startIdx + EMBED.length());
			}
		} else if (url.contains(YOUTUBE_SHORT)) {
			startIdx = url.indexOf(YOUTUBE_SHORT);
			subString = url.substring(startIdx + YOUTUBE_SHORT.length());
		}

		if (subString != null) {
			int endIdx = subString.indexOf("?") != -1 ? subString.indexOf("?")
					: (subString.indexOf("&") != -1 ? subString.indexOf("&")
							: (subString.indexOf("\"") != -1
									? subString.indexOf("\"") : -1));
			if (endIdx != -1) {
				id = subString.substring(0, endIdx);
			} else {
				id = subString;
			}
		}
		return id;
	}

	@Override
	public void createPreview(GeoVideo geo, final AsyncOperation<MyImage> cb) {
		final Image img = new Image();
		img.getElement().setAttribute("crossorigin", "anonymous");
		img.setUrl(geo.getPreviewUrl());
		img.addLoadHandler(new LoadHandler() {

			@Override
			public void onLoad(LoadEvent event) {
				final MyImage prev = new MyImageW(ImageElement.as(img.getElement()), false);
				cb.callback(prev);
				RootPanel.get().remove(img);
			}
		});
		RootPanel.get().add(img);
	}

	@Override
	public void setPreviewOnly(boolean preview) {
		previewOnly = preview;
	}

	@Override
	public boolean isPreviewOnly() {
		return previewOnly;
	}

	@Override
	public void addPlayer(final GeoVideo video) {
		if (!youTubeAPI) {
			Log.warn("No YouTube API!");
			return;
		}

		AppW app = (AppW) video.getKernel().getApplication();
		GeoGebraFrameW appFrame = (GeoGebraFrameW) app.getAppletFrame();
		final VideoPlayer player = new VideoPlayer(video);
		players.put(video.getYouTubeId(), player);
		appFrame.add(player);
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				player.setYouTubePlayer(createYouTubePlayer(video.getYouTubeId()));
			}
		});
	}

	@Override
	public void removePlayer(final GeoVideo video) {
		if (!hasPlayer(video)) {
			return;
		}
		getPlayer(video).removeFromParent();
	}

	@Override
	public boolean hasPlayer(GeoVideo video) {
		return players.containsKey(video.getYouTubeId());
	}

	private VideoPlayer getPlayer(GeoVideo video) {
		return players.get(video.getYouTubeId());
	}

	@Override
	public void updatePlayer(GeoVideo video) {
		if (!hasPlayer(video) || !video.hasChanged()) {
			return;
		}
		getPlayer(video).update();
	}

	@Override
	public void removePlayers() {
		for (VideoPlayer player : players.values()) {
			player.removeFromParent();
		}
	}

	@Override
	public boolean isOnline(GeoVideo video) {
		return ((AppW) video.getKernel().getApplication()).getNetworkOperation().isOnline();
	}

	private native boolean isPlayerValid(JavaScriptObject player) /*-{
		return player.contentWindow != null;
	}-*/;

	/**
	 * Called after video specified by its id is loaded.
	 * 
	 * @param id
	 *            the YouTube id of the video.
	 */
	public void onPlayerReady(String id) {
		VideoPlayer player = players.get(id);
		player.getVideo().setBackground(true);
		player.update();
	}

	private static void onPlayerStateChange() {
		// implement later;
	}

	private static void onAPIReady() {
		youTubeAPI = true;
	}

	private static native void loadYouTubeApi() /*-{
		$wnd.youtube_api_ready = function() {
			@org.geogebra.web.html5.video.VideoManagerW::onAPIReady()();
		}

		var tag = document.createElement('script');
		tag.id = 'youtube-iframe';
		tag.src = 'https://www.youtube.com/iframe_api';
		var firstScriptTag = $doc.getElementsByTagName('script')[0];
		firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
	}-*/;

	private native JavaScriptObject createYouTubePlayer(String youtubeId) /*-{
		var that = this;
		var ytPlayer = new $wnd.YT.Player(
				youtubeId,
				{
					events : {
						'onReady' : function(event) {
							that.@org.geogebra.web.html5.video.VideoManagerW::onPlayerReady(Ljava/lang/String;)(youtubeId);
						}
					}
				});
		return ytPlayer;
	}-*/;

	//
	@Override
	public void backgroundAll() {
		if (players.isEmpty()) {
			return;
		}
		App app = null;
		for (VideoPlayer player : players.values()) {
			background(player.getVideo());
			if (app == null) {
				app = player.getVideo().getKernel().getApplication();
			}
		}

		if (app != null) {
			app.getActiveEuclidianView().repaintView();
		}
	}
}
