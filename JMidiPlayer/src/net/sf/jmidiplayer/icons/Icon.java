package net.sf.jmidiplayer.icons;

import java.awt.Image;

import javax.swing.ImageIcon;

/**
 * Enumerates all the icons available for use on the application.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 */
public enum Icon {

	/* Available icons */

	DIALOG_ERROR("dialog-error_48.png"), ERROR("dialog-error_22.png"), FOLDER(
			"folder.png"), LOOP("view-refresh.png"), MIDI_FILE("audio-midi.png"), MIDI_PLAYER(
			"midi-player.png"), MUTED("audio-volume-muted.png"), NO_SOLO(
			"media-record.png"), PAUSE("media-playback-pause.png"), PITCH(
			"player-volume.png"), PLAY("media-playback-start.png"), SOLO(
			"user-online.png"), STOP("media-playback-stop.png"), TEMPO(
			"multimedia-volume-control.png"), VOLUME_HIGH(
			"audio-volume-high.png"), VOLUME_LOW("audio-volume-low.png"), VOLUME_MEDIUM(
			"audio-volume-medium.png");

	protected static final String ICONS_PACKAGE = "/net/sf/jmidiplayer/icons/";

	private String path;

	private Icon(String path) {
		this.path = ICONS_PACKAGE + path;
	}

	@Override
	public String toString() {
		return path;
	}

	/**
	 * Returns an <code>ImageIcon</code> with the specified icon.
	 * 
	 * @param icon
	 *            the icon that has to be drawn, one of those enumerated by the
	 *            <code>Icon</code> enum.
	 * @return an <code>ImageIcon</code> with the specified icon.
	 * @see ImageIcon#ImageIcon(java.net.URL)
	 * @see Class#getResource(String)
	 */
	public static ImageIcon get(Icon icon) {
		return new ImageIcon(Icon.class.getResource(icon.path));
	}

	/**
	 * Returns an <code>Image</code> with the specified icon.
	 * 
	 * @param icon
	 *            the icon that has to be drawn, one of those enumerated by the
	 *            <code>Icon</code> enum.
	 * @return an <code>Image</code> with the specified icon.
	 * @see #get(Icon)
	 */
	public static Image getAsImage(Icon icon) {
		return get(icon).getImage();
	}

}